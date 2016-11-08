package inaka.com.mangosta.activities;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.doReturn;

public class BlockUsersActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(BlockUsersActivity.class, true, false);

    @Before
    @Override
    public void setUp() {
        super.setUp();
        launchActivity();
        doReturn(true).when(mXMPPSessionMock).userExists("ramabit");
        doReturn(false).when(mXMPPSessionMock).userExists("sarasa");

    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), BlockUsersActivity.class);
        mActivityTestRule.launchActivity(intent);
    }

    @Test
    public void searchUserNotFound() {
        onView(withId(R.id.blockSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasa"));

        onView(withId(R.id.blockSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        onView(withText(R.string.user_not_found))
                .check(matches(isDisplayed()));

        onView(withText(android.R.string.ok))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    @Test
    public void searchUserFound() {
        onView(withId(R.id.blockSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("ramabit"));

        onView(withId(R.id.blockSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        RecyclerView searchResultsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.blockSearchResultRecyclerView);
        Assert.assertEquals(1, searchResultsRecyclerView.getAdapter().getItemCount());

    }

}
