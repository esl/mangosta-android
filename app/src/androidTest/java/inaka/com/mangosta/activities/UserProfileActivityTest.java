package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.RecyclerViewInteraction;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.xmpp.XMPPUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class UserProfileActivityTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule = new ActivityTestRule<>(UserProfileActivity.class, true, false);

    private User mUser;
    private List<String> mBlogPostsContent;

    @Before
    @Override
    public void setUp() {
        super.setUp();

        initBlogPosts();

        mBlogPostsContent = new ArrayList<>();
        for (BlogPost blogPost : mBlogPosts) {
            mBlogPostsContent.add(blogPost.getContent());
        }

        launchActivity();
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), UserProfileActivity.class);

        mUser = new User();
        mUser.setLogin("sarasa");

        Bundle bundle = new Bundle();
        bundle.putParcelable(UserProfileActivity.USER_PARAMETER, mUser);
        bundle.putBoolean(UserProfileActivity.AUTH_USER_PARAMETER, true);
        intent.putExtras(bundle);

        mActivityTestRule.launchActivity(intent);
    }

    @Test
    public void userData() throws Exception {
        onView(withId(R.id.textLoginUserProfile))
                .check(matches(isDisplayed()))
                .check(matches(withText(mUser.getLogin())));

        onView(withId(R.id.textNameUserProfile))
                .check(matches(isDisplayed()))
                .check(matches(withText(XMPPUtils.fromUserNameToJID(mUser.getLogin()))));
    }

    @Test
    public void blogPostsList() throws Exception {
        onView(withId(R.id.blogsRecyclerView))
                .check(matches(isDisplayed()));

        checkBlogPostsRecyclerViewContent(mBlogPostsContent);
    }

    private void checkBlogPostsRecyclerViewContent(final List<String> blogPosts) {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.blogsRecyclerView), ViewMatchers.isDisplayed()))
                .withItems(blogPosts)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String chatName, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(chatName))).check(view, e);
                    }
                });
    }

}
