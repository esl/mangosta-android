package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.BlogPostComment;
import inaka.com.mangosta.models.RecyclerViewInteraction;
import inaka.com.mangosta.utils.TimeCalculation;
import inaka.com.mangosta.xmpp.XMPPUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class BlogPostDetailsActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(BlogPostDetailsActivity.class, true, false);

    private BlogPost mBlogPost;
    private List<BlogPostComment> mBlogPostComments;
    private List<String> mBlogPostCommentsContent;

    @Before
    public void setUp() {
        super.setUp();
        initBlogPostsComments();
        launchActivity();
    }

    private void initBlogPostsComments() {
        mBlogPost = new BlogPost("sarasa001", "sarasa@erlang-solutions.com", null, "blog post content", new Date(), new Date());

        mBlogPostComments = new ArrayList<>();
        mBlogPostCommentsContent = new ArrayList<>();

        BlogPostComment comment1 =
                new BlogPostComment("101",
                        mBlogPost.getId(),
                        "comment1",
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        null,
                        new Date());

        BlogPostComment comment2 =
                new BlogPostComment("101",
                        mBlogPost.getId(),
                        "comment1",
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        null,
                        new Date());

        BlogPostComment comment3 =
                new BlogPostComment("101",
                        mBlogPost.getId(),
                        "comment1",
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        null,
                        new Date());

        BlogPostComment comment4 =
                new BlogPostComment("101",
                        mBlogPost.getId(),
                        "comment1",
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        null,
                        new Date());

        mBlogPostComments.add(comment1);
        mBlogPostComments.add(comment2);
        mBlogPostComments.add(comment3);
        mBlogPostComments.add(comment4);

        mBlogPostCommentsContent.add(comment1.getContent());
        mBlogPostCommentsContent.add(comment2.getContent());
        mBlogPostCommentsContent.add(comment3.getContent());
        mBlogPostCommentsContent.add(comment4.getContent());

        Mockito.when(mRealmManagerMock.getBlogPostComments(mBlogPost.getId())).thenReturn(mBlogPostComments);
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), BlogPostDetailsActivity.class);
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

    @Test
    public void blogPostComments() throws Exception {
        onView(withId(R.id.recyclerviewComments))
                .check(matches(isDisplayed()));

        IdlingResource resource = startTiming(5000);
        checkBlogPostsRecyclerViewContent(mBlogPostCommentsContent);
        stopTiming(resource);
    }

    private void checkBlogPostsRecyclerViewContent(final List<String> comments) {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.recyclerviewComments), ViewMatchers.isDisplayed()))
                .withItems(comments)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String comment, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(comment))).check(view, e);
                    }
                });
    }

}
