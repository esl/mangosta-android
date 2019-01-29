package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.xmpp.XMPPUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class UserProfileActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule = new ActivityTestRule<>(UserProfileActivity.class, true, false);

    private User mUser;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        launchActivity();
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), UserProfileActivity.class);

        mUser = new User();
        mUser.setLogin("sarasa");

        Bundle bundle = new Bundle();
        bundle.putParcelable(UserProfileActivity.USER_PARAMETER, mUser);
        intent.putExtras(bundle);

        mActivityTestRule.launchActivity(intent);
    }

    @Test
    public void userData() throws Exception {
        onView(withId(R.id.textLoginUserProfile))
                .check(matches(isDisplayed()))
                .check(matches(withText(mUser.getLogin())));

        onView(withId(R.id.textNameUserProfile))
                .check(matches(isDisplayed()))
                .check(matches(withText(XMPPUtils.fromUserNameToJID(mUser.getLogin()))));
    }

    @Test
    public void openChat() throws Exception {
        onView(withId(R.id.actionOpenChat))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .check(matches(isClickable()))
                .perform(click());

        onView(withId(R.id.chatSendMessageEditText))
                .check(matches(isDisplayed()));
    }

}
