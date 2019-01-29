package inaka.com.mangosta.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jxmpp.jid.Jid;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.microblogging.elements.PublishPostExtension;

public class CreateBlogActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.createBlogFloatingButton)
    FloatingActionButton createBlogFloatingButton;

    @BindView(R.id.createBlogText)
    EditText createBlogText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_blog);

        unbinder = ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        createBlogFloatingButton.setIcon(R.drawable.ic_action_send_dark);
        createBlogFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!createBlogText.getText().toString().isEmpty()) {
                    publishBlogPost();
                }
            }
        });

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

    private void publishBlogPost() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.blog_post_publishing), getString(R.string.loading), true);

        Tasks.executeInBackground(this, new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                Jid jid = XMPPSession.getInstance().getUser().asEntityBareJid();

                // create stanza
                PublishPostExtension publishPostExtension = new PublishPostExtension(jid, createBlogText.getText().toString());
                PubSub publishPostPubSub = PubSub.createPubsubPacket(jid, IQ.Type.set, publishPostExtension, null);

                // send stanza
                XMPPSession.getInstance().sendStanza(publishPostPubSub);

                // allow comments
                XMPPSession.getInstance().createNodeToAllowComments(publishPostExtension.getId());

                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                progress.dismiss();
                createBlogText.setText("");
                Toast.makeText(CreateBlogActivity.this, getString(R.string.blog_post_published), Toast.LENGTH_SHORT).show();
                new Event(Event.Type.BLOG_POST_CREATED).post();
                finish();
            }

            @Override
            public void onError(Context context, Exception e) {
                progress.dismiss();
                Toast.makeText(CreateBlogActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

}
