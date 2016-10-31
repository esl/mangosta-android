package inaka.com.mangosta.activities;

import android.app.Activity;
import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.widget.EditText;

import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.junit.Assume.assumeTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.realm.RealmManager;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class CreateBlogActivityTest extends BaseInstrumentedTest {

//    @Rule
//    public ActivityTestRule<CreateBlogActivity> mCreateBlogActivityActivityTestRule =
//            new ActivityTestRule<>(CreateBlogActivity.class);

    @Rule
    public ActivityTestRule<MainMenuActivity> mMainMenuActivityActivityTestRule =
            new ActivityTestRule<>(MainMenuActivity.class);

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = mMainMenuActivityActivityTestRule.getActivity();
    }

    @Test
    public void enterBlogPostContent() throws Exception {
        assumeTrue(isUserLoggedIn());

        mActivity.startActivity(new Intent(mActivity, CreateBlogActivity.class));

        onView(withId(R.id.createBlogText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("Running a test"))
                .check(matches(hasFocus()));
    }

    @Test
    public void createBlogPost() throws Exception {
        assumeTrue(isUserLoggedIn());

        int blogPostsCount = getBlogPostsCount();

        mActivity.startActivity(new Intent(mActivity, CreateBlogActivity.class));

        onView(withId(R.id.createBlogFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        // nothing happens
        // ...

        onView(withId(R.id.createBlogText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("Running a test"))
                .check(matches(hasFocus()))
                .check(matches(withText("Running a test")));

        onView(withId(R.id.createBlogFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        onView(withId(R.id.blogsRecyclerView))
                .check(matches(isDisplayed()));

        pressBack();

        assertEquals(blogPostsCount + 1, getBlogPostsCount());

    }

    private int getBlogPostsCount() {
        return RealmManager.getBlogPosts().size();
    }

}
