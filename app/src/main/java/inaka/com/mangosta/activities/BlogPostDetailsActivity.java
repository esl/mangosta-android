package inaka.com.mangosta.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.BlogPostCommentsListAdapter;
import inaka.com.mangosta.database.MangostaDatabase;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.BlogPostComment;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.TimeCalculation;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;
import inaka.com.mangosta.xmpp.microblogging.elements.PublishCommentExtension;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BlogPostDetailsActivity extends BaseActivity {

    public static String BLOG_POST_PARAMETER = "blogPost";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.textBlogPostOwnerName)
    TextView textBlogPostOwnerName;

    @BindView(R.id.textBlogPostDate)
    TextView textBlogPostDate;

    @BindView(R.id.textBlogPostTitle)
    TextView textBlogPostTitle;

    @BindView(R.id.recyclerviewComments)
    RecyclerView recyclerviewComments;

    @BindView(R.id.textNewComment)
    EditText textNewComment;

    @BindView(R.id.buttonSendComment)
    ImageButton buttonSendComment;

    BlogPost mBlogPost;
    List<BlogPostComment> mBlogPostComments;

    private BlogPostCommentsListAdapter commentsListAdapter;

    private MangostaDatabase database = MangostaApplication.getInstance().getDatabase();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_blog_post_details);

        unbinder = ButterKnife.bind(this);

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

                        Completable.fromCallable(() -> database.blogPostCommentDao().insert(blogPostComment))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe();

                        Toast.makeText(getApplicationContext(), BlogPostDetailsActivity.this.getResources()
                                .getString(R.string.comment_created), Toast.LENGTH_LONG).show();
                        textNewComment.setText("");

                        progressSendComment.dismiss();

                    } catch (SmackException.NotConnectedException | InterruptedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
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
        Jid pubSubServiceJid = XMPPSession.getInstance().getPubSubService();

        // create stanza
        PublishCommentExtension publishCommentExtension = new PublishCommentExtension(blogPost.getId(), userName, jid, content, new Date());
        PubSub publishCommentPubSub = PubSub.createPubsubPacket(pubSubServiceJid, IQ.Type.set, publishCommentExtension, null);

        // send stanza
        XMPPSession.getInstance().sendStanza(publishCommentPubSub);

        return new BlogPostComment(publishCommentExtension.getId(),
                blogPost.getId(),
                content,
                userName,
                jid.toString(),
                publishCommentExtension.getPublished());
    }

    private void loadBlogPostComments() {
        mBlogPostComments.clear();
        addDisposable(database.blogPostCommentDao().getCommentsForBlogPost(mBlogPost.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(comments -> {}//@todo commentsListAdapter.submitList(comments)
                ));
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
