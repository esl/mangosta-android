package inaka.com.mangosta.context;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import io.realm.Realm;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.runner.lifecycle.Stage.RESUMED;
import static inaka.com.mangosta.models.MyViewMatchers.isToast;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseInstrumentedTest {

    private Activity mCurrentActivity;
    protected RealmManager mRealmManagerMock;
    protected List<BlogPost> mBlogPosts;

    protected IdlingResource startTiming(long time) {
        IdlingResource idlingResource = new ElapsedTimeIdlingResource(time);
        Espresso.registerIdlingResources(idlingResource);
        return idlingResource;
    }

    protected void stopTiming(IdlingResource idlingResource) {
        Espresso.unregisterIdlingResources(idlingResource);
    }

    private class ElapsedTimeIdlingResource implements IdlingResource {
        private long mStartTime;
        private final long mWaitingTime;
        private ResourceCallback mResourceCallback;

        private ElapsedTimeIdlingResource(long waitingTime) {
            this.mStartTime = System.currentTimeMillis();
            this.mWaitingTime = waitingTime;
        }

        @Override
        public String getName() {
            return ElapsedTimeIdlingResource.class.getName() + ":" + mWaitingTime;
        }

        @Override
        public boolean isIdleNow() {
            long elapsed = System.currentTimeMillis() - mStartTime;
            boolean idle = (elapsed >= mWaitingTime);
            if (idle) {
                mResourceCallback.onTransitionToIdle();
            }
            return idle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
            this.mResourceCallback = resourceCallback;
        }
    }

    protected boolean isUserLoggedIn() {
        return Preferences.getInstance().isLoggedIn();
    }

    protected Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    protected Activity getCurrentActivity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED);
                if (resumedActivities.iterator().hasNext()) {
                    mCurrentActivity = resumedActivities.iterator().next();
                }
            }
        });
        return mCurrentActivity;
    }

    private void setUpRealmTestContext() {
        Realm.init(getContext());
        Realm realmMock = Realm.getDefaultInstance();

        mRealmManagerMock = mock(RealmManager.class);
        when(mRealmManagerMock.getRealm()).thenReturn(realmMock);
        RealmManager.setSpecialInstanceForTesting(mRealmManagerMock);
    }

    private void mockXMPPSession() {
        XMPPSession xmppSession = mock(XMPPSession.class);

        try {
            EntityBareJid jid = JidCreate.entityBareFrom(Preferences.getInstance().getUserXMPPJid());
            when(xmppSession.getUser()).thenReturn(jid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        try {
            doNothing().when(xmppSession).sendStanza(any(Stanza.class));
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }

        doNothing().when(xmppSession).createNodeToAllowComments(any(String.class));

        try {
            doReturn(null).when(xmppSession).getPubSubService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        XMPPSession.setSpecialInstanceForTesting(xmppSession);
    }

    protected void setUp() {
        setUpRealmTestContext();
        mockXMPPSession();
    }

    protected void initBlogPosts() {
        BlogPost blogPost1 = new BlogPost("001",
                Preferences.getInstance().getUserXMPPJid(),
                null,
                "blog post 1",
                new Date(),
                new Date());

        BlogPost blogPost2 = new BlogPost("002",
                Preferences.getInstance().getUserXMPPJid(),
                null,
                "blog post 2",
                new Date(),
                new Date());

        BlogPost blogPost3 = new BlogPost("003",
                Preferences.getInstance().getUserXMPPJid(),
                null,
                "blog post 3",
                new Date(),
                new Date());

        mRealmManagerMock.saveBlogPost(blogPost1);
        mRealmManagerMock.saveBlogPost(blogPost2);
        mRealmManagerMock.saveBlogPost(blogPost3);

        mBlogPosts = new ArrayList<>();
        mBlogPosts.add(blogPost1);
        mBlogPosts.add(blogPost2);
        mBlogPosts.add(blogPost3);

        Mockito.when(mRealmManagerMock.getBlogPosts()).thenReturn(mBlogPosts);
    }

    public void isToastMessageDisplayed(int textId) {
        onView(withText(textId)).inRoot(isToast()).check(matches(isDisplayed()));
    }

    protected static Date getDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
