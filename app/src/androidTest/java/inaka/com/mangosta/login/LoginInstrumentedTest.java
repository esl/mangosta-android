package inaka.com.mangosta.login;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jxmpp.jid.Jid;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.SplashActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;


@RunWith(AndroidJUnit4.class)
public class LoginInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<SplashActivity> mSplashActivityTestRule =
            new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void checkXMPPServerAndServiceInLogin() throws Exception {
        Assume.assumeFalse(Preferences.getInstance().isLoggedIn());

        IdlingResource resource = startTiming(mSplashActivityTestRule.getActivity().WAIT_TIME);

        Espresso.onView(ViewMatchers.withId(R.id.loginJidCompletionEditText))
                .check(ViewAssertions.matches(ViewMatchers.withText("@" + XMPPSession.SERVICE_NAME)));

        Espresso.onView(ViewMatchers.withId(R.id.loginServerEditText))
                .check(ViewAssertions.matches(ViewMatchers.withText(XMPPSession.SERVER_NAME)));

        stopTiming(resource);
    }

    @Test
    public void checkXMPPLoggedUserSaved() throws Exception {
        Assume.assumeTrue(Preferences.getInstance().isLoggedIn());

        IdlingResource resource = startTiming(mSplashActivityTestRule.getActivity().WAIT_TIME);

        Assume.assumeNotNull(XMPPSession.getInstance().getXMPPConnection());
        Assume.assumeNotNull(XMPPSession.getInstance().getXMPPConnection().getUser());

        Jid jid = XMPPSession.getInstance().getXMPPConnection().getUser().asBareJid();
        Assert.assertTrue(XMPPUtils.isAutenticatedJid(jid));

        String userName = XMPPUtils.fromJIDToUserName(jid.toString());
        Assert.assertTrue(XMPPUtils.isAutenticatedUser(userName));

        stopTiming(resource);
    }

}
