package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.xmpp.XMPPUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class UserProfileActivityTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule = new ActivityTestRule<>(UserProfileActivity.class, true, false);

    @Before
    @Override
    public void setUp() {
        super.setUp();
    }

    @Test
    public void userData() throws Exception {
        Intent intent = new Intent(getContext(), UserProfileActivity.class);

        User user = new User();
        user.setLogin("sarasa");

        Bundle bundle = new Bundle();
        bundle.putParcelable(UserProfileActivity.USER_PARAMETER, user);
        bundle.putBoolean(UserProfileActivity.AUTH_USER_PARAMETER, true);
        intent.putExtras(bundle);

        mActivityTestRule.launchActivity(intent);

        onView(withId(R.id.textLoginUserProfile))
                .check(matches(isDisplayed()))
                .check(matches(withText(user.getLogin())));

        onView(withId(R.id.textNameUserProfile))
                .check(matches(isDisplayed()))
                .check(matches(withText(XMPPUtils.fromUserNameToJID(user.getLogin()))));
    }

}
