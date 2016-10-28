package inaka.com.mangosta.menu;

import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.rule.ActivityTestRule;
import android.support.test.espresso.core.deps.guava.base.Optional;

import org.junit.Rule;
import org.junit.Test;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.actionWithAssertions;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class MainFloatingButtonsTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<MainMenuActivity> mMainMenuActivityActivityTestRule =
            new ActivityTestRule<>(MainMenuActivity.class);

    @Test
    public void checkFloatingButtonsAvailability() throws Exception {
        onView(withId(R.id.multipleActions))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.createNewBlogFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()));

        onView(withId(R.id.createNewChatFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()));

        onView(withId(R.id.manageFriendsFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()));
    }

    @Test
    public void goToCreateBlog() throws Exception {
        onView(withId(R.id.multipleActions))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.createNewBlogFloatingButton))
                .check(matches(isDisplayed()))
                .perform(customClick());

//        onView(withId(R.id.createBlogText))
//                .check(matches(isDisplayed()));
//
//        pressBack();
//
//        onView(withId(R.id.createNewBlogFloatingButton))
//                .check(matches(isDisplayed()));
    }

    @Test
    public void goToCreateChat() throws Exception {

    }

    @Test
    public void goToManageFriends() throws Exception {

    }

    public static ViewAction customClick() {
        return actionWithAssertions(
                new GeneralClickAction(Tap.SINGLE, GeneralLocation.VISIBLE_CENTER, Press.FINGER));
    }
}
