package inaka.com.mangosta.context;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import org.mockito.Mockito;

import java.util.Collection;

import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import io.realm.Realm;

import static android.support.test.runner.lifecycle.Stage.RESUMED;

public class BaseInstrumentedTest {

    private Activity mCurrentActivity;
    private Realm mRealmMock;
    protected RealmManager mRealmManagerMock;

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

    protected void setUpRealmTestContext() {
        Realm.init(getContext());
        mRealmMock = Realm.getDefaultInstance();

        mRealmManagerMock = Mockito.mock(RealmManager.class);
        Mockito.when(mRealmManagerMock.getRealm()).thenReturn(mRealmMock);
        RealmManager.setSpecialInstanceForTesting(mRealmManagerMock);
    }

}
