package inaka.com.mangosta.menu;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.activities.SplashActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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

        onView(withId(R.id.actionManageContacts))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .check(matches(isClickable()));

        onView(withId(R.id.actionBlockUsers))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .check(matches(isClickable()));

        // click to see hided menu items
        openActionBarOverflowOrOptionsMenu(getContext());

        onView(withText(R.string.action_about))
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
    }

    @Test
    public void goToManageContactsWithMenuItem() throws Exception {
        assumeTrue(isUserLoggedIn());

        onView(withId(R.id.actionManageContacts))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .perform(click())
                .check(doesNotExist());

        onView(withId(R.id.manageContactsSearchUserLayout))
                .check(matches(isDisplayed()));

        pressBack();
    }

    @Test
    public void goToBlockingPageWithMenuItem() throws Exception {
        onView(withId(R.id.actionBlockUsers))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .perform(click())
                .check(doesNotExist());

        onView(withId(R.id.blockSearchResultRecyclerView))
                .check(matches(isDisplayed()));

        pressBack();
    }

    @Test
    public void goToAboutPageWithMenuItem() throws Exception {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

        onView(withText(R.string.action_about))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .perform(click());

        onView(withId(R.id.content_about))
                .check(matches(isDisplayed()));

        pressBack();
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
