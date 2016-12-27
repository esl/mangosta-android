package inaka.com.mangosta.context;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import inaka.com.mangosta.chat.RoomManager;
import inaka.com.mangosta.chat.RoomsListManager;
import inaka.com.mangosta.interfaces.MongooseService;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.models.MongooseServiceMock;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.network.MongooseAPI;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.RosterManager;
import inaka.com.mangosta.xmpp.XMPPSession;
import io.realm.Realm;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.runner.lifecycle.Stage.RESUMED;
import static inaka.com.mangosta.models.MyViewMatchers.isToast;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseInstrumentedTest {

    private Activity mCurrentActivity;

    protected Realm mRealmMock;
    protected RealmManager mRealmManagerMock;
    protected XMPPSession mXMPPSessionMock;
    protected RosterManager mRosterManagerMock;
    protected RoomsListManager mRoomListManagerMock;
    protected RoomManager mRoomManagerMock;
    private final NetworkBehavior behavior = NetworkBehavior.create();
    protected MongooseService mMongooseServiceMock;

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
        mRealmMock = Realm.getDefaultInstance();

        mRealmManagerMock = mock(RealmManager.class);

        when(mRealmManagerMock.getRealm()).thenReturn(mRealmMock);
        doReturn(new Chat()).when(mRealmManagerMock).getChatFromRealm(any(Realm.class), any(String.class));
        doReturn(UUID.randomUUID().toString()).when(mRealmManagerMock)
                .saveMessageLocally(any(Chat.class), any(String.class), any(String.class), any(int.class));
        doReturn(mRealmMock.where(ChatMessage.class).findAll())
                .when(mRealmManagerMock)
                .getMessagesForChat(any(Realm.class), any(String.class));
        doNothing().when(mRealmManagerMock).deleteAll();

        RealmManager.setSpecialInstanceForTesting(mRealmManagerMock);
    }

    protected void mockXMPPSession() {
        mXMPPSessionMock = mock(XMPPSession.class);

        try {
            doNothing().when(mXMPPSessionMock).sendStanza(any(Stanza.class));
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }

        doNothing().when(mXMPPSessionMock).createNodeToAllowComments(any(String.class));

        try {
            doReturn(null).when(mXMPPSessionMock).getPubSubService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            doNothing().when(mXMPPSessionMock).blockContacts(anyList());
            doNothing().when(mXMPPSessionMock).unblockContacts(anyList());
            doReturn(new ArrayList<>()).when(mXMPPSessionMock).getBlockList();
            doNothing().when(mXMPPSessionMock).unblockAll();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            EntityBareJid jid = JidCreate.entityBareFrom(Preferences.getInstance().getUserXMPPJid());
            doReturn(jid).when(mXMPPSessionMock).getUser();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        XMPPSession.setSpecialInstanceForTesting(mXMPPSessionMock);
    }

    private void mockRosterManager() {
        mRosterManagerMock = mock(RosterManager.class);
        RosterManager.setSpecialInstanceForTesting(mRosterManagerMock);

        try {
            doNothing().when(mRosterManagerMock).addToBuddies(any(User.class));
            doNothing().when(mRosterManagerMock).removeFromBuddies(any(User.class));
            doReturn(new HashMap<>()).when(mRosterManagerMock).getBuddies();
            doNothing().when(mRosterManagerMock).removeAllFriends();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mockRoomListManager() {
        mRoomListManagerMock = mock(RoomsListManager.class);
        RoomsListManager.setSpecialInstanceForTesting(mRoomListManagerMock);

        doNothing().when(mRoomListManagerMock).createCommonChat(any(String.class));

        doReturn(null).when(mRoomListManagerMock).createMUCLight(anyList(), any(String.class));

        doNothing().when(mRoomListManagerMock).setShowChat(any(Realm.class), any(Chat.class));

        doNothing().when(mRoomListManagerMock)
                .manageNewChat(any(Chat.class), any(Realm.class), any(String.class), any(String.class));
    }

    private void mockRoomManager() {
        mRoomManagerMock = mock(RoomManager.class);
        RoomManager.setSpecialInstanceForTesting(mRoomManagerMock);
        doNothing().when(mRoomManagerMock).updateTypingStatus(any(ChatState.class), any(String.class), any(int.class));
        doNothing().when(mRoomManagerMock).addToMUCLight(any(User.class), any(String.class));
        doNothing().when(mRoomManagerMock).removeFromMUCLight(any(User.class), any(String.class));
    }

    private void mockRestApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(MongooseAPI.BASE_URL).build();

        MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).networkBehavior(behavior).build();

        final BehaviorDelegate<MongooseService> delegate = mockRetrofit.create(MongooseService.class);

        mMongooseServiceMock = new MongooseServiceMock(delegate);

        MongooseAPI mongooseAPI = mock(MongooseAPI.class);
        doReturn(mMongooseServiceMock).when(mongooseAPI).getAuthenticatedService();
        MongooseAPI.setSpecialInstanceForTesting(mongooseAPI);
    }

    protected void setUpTest() {
        Preferences.setTest();
        mockRestApi();
    }

    protected void setUp() {
        setUpTest();
        setUpRealmTestContext();
        mockXMPPSession();
        mockRosterManager();
        mockRoomListManager();
        mockRoomManager();
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
