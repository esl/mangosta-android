package inaka.com.mangosta.login;

import androidx.test.espresso.IdlingResource;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jxmpp.jid.Jid;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.SplashActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.xmpp.XMPPSession;
import inaka.com.mangosta.xmpp.XMPPUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;


@RunWith(AndroidJUnit4.class)
public class LoginInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<SplashActivity> mSplashActivityTestRule =
            new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void checkXMPPServerAndServiceInLogin() throws Exception {
        assumeFalse(isUserLoggedIn());

        IdlingResource resource = startTiming(SplashActivity.WAIT_TIME);

        onView(withId(R.id.loginJidCompletionEditText))
                .check(matches(withText("@" + XMPPSession.SERVICE_NAME)));

        onView(withId(R.id.loginServerEditText))
                .check(matches(withText(XMPPSession.SERVER_NAME)));

        stopTiming(resource);
    }

    @Test
    public void checkXMPPLoggedUserSaved() throws Exception {
        assumeTrue(isUserLoggedIn());

        IdlingResource resource = startTiming(SplashActivity.WAIT_TIME);

        onView(withId(R.id.viewpagerMainMenu))
                .check(matches(isDisplayed()));

        assumeNotNull(XMPPSession.getInstance().getXMPPConnection());
        assumeNotNull(XMPPSession.getInstance().getXMPPConnection().getUser());

        Jid jid = XMPPSession.getInstance().getXMPPConnection().getUser().asBareJid();
        assertTrue(XMPPUtils.isAutenticatedJid(jid));

        String userName = XMPPUtils.fromJIDToUserName(jid.toString());
        assertTrue(XMPPUtils.isAutenticatedUser(userName));

        stopTiming(resource);
    }

}
