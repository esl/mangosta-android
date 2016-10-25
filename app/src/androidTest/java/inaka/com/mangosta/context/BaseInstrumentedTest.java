package inaka.com.mangosta.context;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;

public class BaseInstrumentedTest {

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
}
