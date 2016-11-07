package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.utils.TimeCalculation;
import inaka.com.mangosta.xmpp.XMPPUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class BlogPostDetailsActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(BlogPostDetailsActivity.class, true, false);

    private BlogPost mBlogPost;

    @Before
    public void setUp() {
        super.setUp();
        launchActivity();
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), BlogPostDetailsActivity.class);

        mBlogPost = new BlogPost("sarasa001", "sarasa@erlang-solutions.com", null, "blog post content", new Date(), new Date());

        Bundle bundle = new Bundle();
        bundle.putParcelable(BlogPostDetailsActivity.BLOG_POST_PARAMETER, mBlogPost);
        intent.putExtras(bundle);

        mActivityTestRule.launchActivity(intent);
    }

    @Test
    public void blogPostDetails() throws Exception {
        onView(withId(R.id.textBlogPostOwnerName))
                .check(matches(isDisplayed()))
                .check(matches(withText(XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()))));

        onView(withId(R.id.textBlogPostDate))
                .check(matches(isDisplayed()))
                .check(matches(withText(TimeCalculation.getTimeStringAgoSinceDate(getContext(), mBlogPost.getUpdated()))));

        onView(withId(R.id.textBlogPostTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText(mBlogPost.getContent())));
    }

}
