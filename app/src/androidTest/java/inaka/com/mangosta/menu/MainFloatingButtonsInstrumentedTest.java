package inaka.com.mangosta.menu;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class MainFloatingButtonsInstrumentedTest extends BaseInstrumentedTest {

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
    }

}
