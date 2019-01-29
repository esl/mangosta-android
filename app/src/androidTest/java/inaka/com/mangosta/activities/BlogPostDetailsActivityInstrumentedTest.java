package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.rule.ActivityTestRule;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;


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

    @Test
    public void blogPostDetails() throws Exception {
        String dateString = TimeCalculation.getTimeStringAgoSinceDate(getContext(), mBlogPost.getUpdated());
        onView(withId(R.id.textBlogPostDate))
                .check(matches(isDisplayed()))
                .check(matches(withText(dateString)));

        onView(withId(R.id.textBlogPostOwnerName))
                .check(matches(isDisplayed()))
                .check(matches(withText(XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()))));

        onView(withId(R.id.textBlogPostTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText(mBlogPost.getContent())));
    }

    @Test
    public void blogPostCommentsList() throws Exception {
        onView(withId(R.id.recyclerviewComments))
                .check(matches(isDisplayed()));
        checkBlogPostsRecyclerViewContent(mBlogPostCommentsContent);
    }

    @Test
    public void tryToAddCommentWithoutText() throws Exception {
        onView(withId(R.id.textNewComment))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()));

        // Try to send comment
        onView(withId(R.id.buttonSendComment))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        // Empty message error
        isToastMessageDisplayed(R.string.empty_message);
    }

    @Test
    public void addNewComment() throws Exception {
        // Enter comment text
        String commentText = "Testing with a new comment";
        onView(withId(R.id.textNewComment))
                .perform(typeText(commentText), closeSoftKeyboard())
                .check(matches(withText(commentText)));

        // mocking new response
        addNewCommentToMockResponse(commentText);

        // Send comment
        onView(withId(R.id.buttonSendComment))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        // Check comments list
        checkBlogPostsRecyclerViewContent(mBlogPostCommentsContent);
    }

    private void initBlogPostsComments() {
        Date date = getDate(2016, 10, 25, 22, 14, 53);
        mBlogPost = new BlogPost("sarasa001",
                "sarasa@erlang-solutions.com",
                null,
                "blog post content",
                date,
                date);

        mBlogPostComments = new ArrayList<>();
        mBlogPostCommentsContent = new ArrayList<>();

        BlogPostComment comment1 =
                new BlogPostComment("101",
                        mBlogPost.getId(),
                        "comment1",
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        new Date());

        BlogPostComment comment2 =
                new BlogPostComment("102",
                        mBlogPost.getId(),
                        "comment2",
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        new Date());

        BlogPostComment comment3 =
                new BlogPostComment("103",
                        mBlogPost.getId(),
                        "comment3",
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        new Date());

        BlogPostComment comment4 =
                new BlogPostComment("104",
                        mBlogPost.getId(),
                        "comment4",
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        new Date());

        mBlogPostComments.add(comment1);
        mBlogPostComments.add(comment2);
        mBlogPostComments.add(comment3);
        mBlogPostComments.add(comment4);

        mBlogPostCommentsContent.add(comment1.getContent());
        mBlogPostCommentsContent.add(comment2.getContent());
        mBlogPostCommentsContent.add(comment3.getContent());
        mBlogPostCommentsContent.add(comment4.getContent());

        when(mRealmManagerMock.getBlogPostComments(mBlogPost.getId())).thenReturn(mBlogPostComments);
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), BlogPostDetailsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(BlogPostDetailsActivity.BLOG_POST_PARAMETER, mBlogPost);
        intent.putExtras(bundle);
        mActivityTestRule.launchActivity(intent);
    }

    private void checkBlogPostsRecyclerViewContent(final List<String> comments) {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.recyclerviewComments), isDisplayed()))
                .withItems(comments)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String comment, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(comment))).check(view, e);
                    }
                });
    }

    private void addNewCommentToMockResponse(String commentText) {
        mBlogPostComments.add(
                new BlogPostComment("105",
                        mBlogPost.getId(),
                        commentText,
                        XMPPUtils.fromJIDToUserName(mBlogPost.getOwnerJid()),
                        mBlogPost.getOwnerJid(),
                        new Date()));
        mBlogPostCommentsContent.add(commentText);
        when(mRealmManagerMock.getBlogPostComments(mBlogPost.getId())).thenReturn(mBlogPostComments);
    }

}
