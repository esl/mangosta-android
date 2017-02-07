package inaka.com.mangosta.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.BlogPostCommentsListAdapter;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.BlogPostComment;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.TimeCalculation;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.microblogging.elements.PostEntryExtension;

public class BlogPostDetailsActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.textBlogPostOwnerName)
    TextView textBlogPostOwnerName;

    @Bind(R.id.textBlogPostDate)
    TextView textBlogPostDate;

    @Bind(R.id.textBlogPostTitle)
    TextView textBlogPostTitle;

    @Bind(R.id.recyclerviewComments)
    RecyclerView recyclerviewComments;

    @Bind(R.id.textNewComment)
    EditText textNewComment;

    @Bind(R.id.buttonSendComment)
    ImageButton buttonSendComment;

    BlogPost mBlogPost;
    List<BlogPostComment> mBlogPostComments;

    private BlogPostCommentsListAdapter commentsListAdapter;
    public static String BLOG_POST_PARAMETER = "blogPost";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_blog_post_details);

        ButterKnife.bind(this);

        Bundle bundle = getIntent().getExtras();
        mBlogPost = bundle.getParcelable(BLOG_POST_PARAMETER);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setTitle(mBlogPost.getContent());
        setSupportActionBar(toolbar);

        textBlogPostOwnerName.setText(XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()));
        textBlogPostDate.setText(TimeCalculation.getTimeStringAgoSinceDate(this, mBlogPost.getUpdated()));
        textBlogPostTitle.setText(mBlogPost.getContent());

        recyclerviewComments.setHasFixedSize(true);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerviewComments.setLayoutManager(layoutManager);

        mBlogPostComments = new ArrayList<>();

        commentsListAdapter = new BlogPostCommentsListAdapter(mBlogPostComments, this);
        recyclerviewComments.setAdapter(commentsListAdapter);

        buttonSendComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newComment = textNewComment.getText().toString();

                if (newComment.length() > 0) {
                    final ProgressDialog progressSendComment = ProgressDialog.show(BlogPostDetailsActivity.this,
                            getResources().getString(R.string.loading), getResources().getString(R.string.sending_request), true);

                    try {
                        BlogPostComment blogPostComment = sendBlogPostComment(newComment, mBlogPost);
                        RealmManager.getInstance().saveBlogPostComment(blogPostComment);

                        Toast.makeText(getApplicationContext(), BlogPostDetailsActivity.this.getResources()
                                .getString(R.string.comment_created), Toast.LENGTH_LONG).show();
                        textNewComment.setText("");

                        loadBlogPostComments();
                        progressSendComment.dismiss();

                    } catch (Throwable e) {
                        e.printStackTrace();
                        progressSendComment.dismiss();
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), BlogPostDetailsActivity.this.getResources()
                            .getString(R.string.empty_message), Toast.LENGTH_SHORT).show();
                }

            }

        });

        loadBlogPostComments();
    }

    public BlogPostComment sendBlogPostComment(String content, BlogPost blogPost)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jid jid = XMPPSession.getInstance().getUser().asEntityBareJid();
        String userName = XMPPUtils.fromJIDToUserName(jid.toString());

        // create stanza
        PostEntryExtension commentExtension = new PostEntryExtension(content, blogPost.getId(), new Date(), null, userName, jid);

        PubSubManager pubSubManager = XMPPSession.getInstance().getPubSubManager();
        LeafNode node = pubSubManager.getNode(PostEntryExtension.COMMENTS_NODE + "/" + blogPost.getId());
        node.send(new PayloadItem<>(commentExtension));

        return new BlogPostComment(commentExtension.getId(),
                blogPost.getId(),
                content,
                userName,
                jid.toString(),
                commentExtension.getPublished());
    }

    private void loadBlogPostComments() {
        mBlogPostComments.clear();
        mBlogPostComments.addAll(RealmManager.getInstance().getBlogPostComments(mBlogPost.getId()));
        commentsListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

}
