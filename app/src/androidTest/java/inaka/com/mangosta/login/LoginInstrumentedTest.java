package inaka.com.mangosta.login;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.SplashActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;

@RunWith(AndroidJUnit4.class)
public class LoginInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<SplashActivity> mSplashActivityTestRule =
            new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void checkXMPPServerAndService() throws Exception {

        // if the user is not logged in
        if (!Preferences.getInstance().isLoggedIn()) {

            IdlingResource resource = startTiming(mSplashActivityTestRule.getActivity().WAIT_TIME);

            Espresso.onView(ViewMatchers.withId(R.id.loginJidCompletionEditText))
                    .check(ViewAssertions.matches(ViewMatchers.withText("@" + XMPPSession.SERVICE_NAME)));

            Espresso.onView(ViewMatchers.withId(R.id.loginServerEditText))
                    .check(ViewAssertions.matches(ViewMatchers.withText(XMPPSession.SERVER_NAME)));

            stopTiming(resource);

        }

    }

}
