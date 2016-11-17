package inaka.com.mangosta.menu;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.activities.SplashActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class MainMenuInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<MainMenuActivity> mMainMenuActivityActivityTestRule =
            new ActivityTestRule<>(MainMenuActivity.class);

    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void menuItemsAvailability() throws Exception {
        onView(withId(R.id.actionUserOptions))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .check(matches(isClickable()));

        // click to see hided menu items
        openActionBarOverflowOrOptionsMenu(getContext());

        onView(withText(R.string.action_manage_friends))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()));

        onView(withText(R.string.action_block_users))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()));

        onView(withText(R.string.action_signout))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()));
    }

    @Test
    public void goToUserProfileWithMenuItem() throws Exception {
        assumeTrue(isUserLoggedIn());

        onView(withId(R.id.actionUserOptions))
                .check(matches(isClickable()))
                .perform(click())
                .check(doesNotExist());

        onView(withId(R.id.layoutNameUserOptions))
                .check(matches(isDisplayed()));

        pressBack();

        onView(withId(R.id.actionUserOptions))
                .check(matches(isDisplayed()));
    }

    @Test
    public void goToManageFriendsWithMenuItem() throws Exception {
        assumeTrue(isUserLoggedIn());

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        onView(withId(R.id.actionManageFriends))
                .check(matches(isClickable()))
                .perform(click())
                .check(doesNotExist());

        onView(withId(R.id.manageFriendsSearchUserLayout))
                .check(matches(isDisplayed()));

        pressBack();

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        onView(withId(R.id.actionManageFriends))
                .check(matches(isDisplayed()));
    }

    @Test
    public void goToBlockingPageWithMenuItem() throws Exception {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        onView(withText(R.string.action_block_users))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .perform(click());

        onView(withId(R.id.blockSearchResultRecyclerView))
                .check(matches(isDisplayed()));

        pressBack();

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        onView(withText(R.string.action_block_users))
                .check(matches(isDisplayed()));
    }

    @Test
    public void logoutWithMenuItem() throws Exception {
        assumeTrue(isUserLoggedIn());

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        onView(withText(R.string.action_signout))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .perform(click());

        IdlingResource resource = startTiming(SplashActivity.WAIT_TIME);
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
        stopTiming(resource);
    }

}
