package inaka.com.mangosta.menu;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.activities.SplashActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;

@RunWith(AndroidJUnit4.class)
public class MainMenuInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<MainMenuActivity> mMainMenuActivityActivityTestRule =
            new ActivityTestRule<>(MainMenuActivity.class);

    @Test
    public void menuItemsAvailability() throws Exception {
        Espresso.onView(ViewMatchers.withId(R.id.actionUserOptions))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                .check(ViewAssertions.matches(ViewMatchers.isClickable()));

        // click to see hided menu items
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        Espresso.onView(ViewMatchers.withText(R.string.action_block_users))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()));

        Espresso.onView(ViewMatchers.withText(R.string.action_signout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()));
    }

    @Test
    public void goToUserProfileWithMenuItem() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        Espresso.onView(ViewMatchers.withId(R.id.actionUserOptions))
                .check(ViewAssertions.matches(ViewMatchers.isClickable()))
                .perform(ViewActions.click())
                .check(ViewAssertions.doesNotExist());

        Espresso.onView(ViewMatchers.withId(R.id.layoutNameUserOptions))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.pressBack();

        Espresso.onView(ViewMatchers.withId(R.id.actionUserOptions))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void goToBlockingPageWithMenuItem() throws Exception {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        Espresso.onView(ViewMatchers.withText(R.string.action_block_users))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.blockSearchResultRecyclerView))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.pressBack();

        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        Espresso.onView(ViewMatchers.withText(R.string.action_block_users))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void logoutWithMenuItem() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        Espresso.onView(ViewMatchers.withText(R.string.action_signout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                .perform(ViewActions.click());

        IdlingResource resource = startTiming(SplashActivity.WAIT_TIME);
        Espresso.onView(ViewMatchers.withId(R.id.loginButton))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        stopTiming(resource);
    }

}
