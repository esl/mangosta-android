package inaka.com.mangosta.activities;

import android.app.Activity;
import android.content.Intent;
import androidx.test.espresso.IdlingResource;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class CreateBlogActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule = new ActivityTestRule<>(MainMenuActivity.class);

    private Activity mActivity;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        mActivity = mActivityTestRule.getActivity();
        initBlogPosts();
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

        // nothing happens because the content is empty

        String newBlogPostContent = "Blog post test";

        // now I complete the content
        onView(withId(R.id.createBlogText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText(newBlogPostContent))
                .check(matches(hasFocus()))
                .check(matches(withText(newBlogPostContent)));

        // save it in mock
        addBlogPostToMockResponse(newBlogPostContent);

        // create blog post
        onView(withId(R.id.createBlogFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        IdlingResource resource = startTiming(2000);

        onView(withId(R.id.blogsRecyclerView))
                .check(matches(isDisplayed()));

        assertEquals(blogPostsCount + 1, getBlogPostsCount());

        stopTiming(resource);
    }

    private void addBlogPostToMockResponse(String newBlogPostContent) {
        BlogPost blogPost4 = new BlogPost("004",
                Preferences.getInstance().getUserXMPPJid(),
                null,
                newBlogPostContent,
                new Date(),
                new Date());
        mBlogPosts.add(blogPost4);
        Mockito.when(mRealmManagerMock.getBlogPosts()).thenReturn(mBlogPosts);
    }

    private int getBlogPostsCount() {
        return RealmManager.getInstance().getBlogPosts().size();
    }

}
