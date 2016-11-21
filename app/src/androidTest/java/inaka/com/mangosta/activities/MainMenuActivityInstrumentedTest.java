package inaka.com.mangosta.activities;

import android.content.Intent;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.ViewPagerMainMenuAdapter;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.fragments.ChatsListsFragment;
import inaka.com.mangosta.models.BlogPost;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.RecyclerViewInteraction;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.ChatOrderComparator;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class MainMenuActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<MainMenuActivity> mMainMenuActivityActivityTestRule =
            new ActivityTestRule<>(MainMenuActivity.class, true, false);

    private List<String> mMUCLightNames;
    private List<String> mOneToOneChatNames;
    private List<Chat> mMUCLights;
    private List<Chat> mOneToOneChats;
    private List<String> mBlogPostsContent;

    @Before
    public void beforeTests() {
        obtain1to1Chats();
        obtainMUCLights();
    }

    private void obtainMUCLights() {
        mMUCLights = RealmManager.getInstance().getMUCLights();
        Collections.sort(mMUCLights, new ChatOrderComparator());
        mMUCLightNames = new ArrayList<>();
        for (Chat chat : mMUCLights) {
            mMUCLightNames.add(chat.getName());
        }
    }

    private void obtain1to1Chats() {
        mOneToOneChats = RealmManager.getInstance().get1to1Chats();
        Collections.sort(mOneToOneChats, new ChatOrderComparator());
        mOneToOneChatNames = new ArrayList<>();
        for (Chat chat : mOneToOneChats) {
            mOneToOneChatNames.add(chat.getName());
        }
    }

    private int getGroupChatsCount(ChatsListsFragment chatsListsFragment) {
        return chatsListsFragment.getGroupChatsAdapter().getItemCount();
    }

    private int getOneToOneChatsCount(ChatsListsFragment chatsListsFragment) {
        return chatsListsFragment.getOneToOneChatsAdapter().getItemCount();
    }

    private ChatsListsFragment getChatsListFragment() {
        MainMenuActivity mainMenuActivity = mMainMenuActivityActivityTestRule.getActivity();
        ViewPagerMainMenuAdapter adapter = ((ViewPagerMainMenuAdapter) mainMenuActivity.mViewpagerMainMenu.getAdapter());
        return (ChatsListsFragment) adapter.mFragmentList[0];
    }

    private void checkGroupChatsRecyclerViewContent(final List<String> chatNames) {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.groupChatsRecyclerView), ViewMatchers.isDisplayed()))
                .withItems(chatNames)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String chatName, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(chatName))).check(view, e);
                    }
                });
    }

    private void checkOneToOneChatsRecyclerViewContent(final List<String> chatNames) {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.oneToOneChatsRecyclerView), ViewMatchers.isDisplayed()))
                .withItems(chatNames)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String chatName, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(chatName))).check(view, e);
                    }
                });
    }

    private void checkBlogPostsRecyclerViewContent(final List<String> blogPosts) {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.blogsRecyclerView), ViewMatchers.isDisplayed()))
                .withItems(blogPosts)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String blogPost, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(blogPost))).check(view, e);
                    }
                });
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), MainMenuActivity.class);
        mMainMenuActivityActivityTestRule.launchActivity(intent);
    }

    @Test
    public void initializeOneToOneChatsList() throws Exception {
        launchActivity();

        assumeTrue(isUserLoggedIn());

        IdlingResource resource = startTiming(5000);

        // Obtain the one to one chats fragment
        ChatsListsFragment chatsListsFragment = getChatsListFragment();

        // Check if it loads the correct amount of chats
        assertEquals(getOneToOneChatsCount(chatsListsFragment), mOneToOneChats.size());

        checkOneToOneChatsRecyclerViewContent(mOneToOneChatNames);

        stopTiming(resource);
    }

    @Test
    public void initializeMUCLightList() throws Exception {
        launchActivity();

        assumeTrue(isUserLoggedIn());

        IdlingResource resource = startTiming(5000);

        // Obtain the one to one chats fragment
        ChatsListsFragment chatsListsFragment = getChatsListFragment();

        // Check if it loads the correct amount of chats
        assertEquals(getGroupChatsCount(chatsListsFragment), mMUCLights.size());

        checkGroupChatsRecyclerViewContent(mMUCLightNames);

        stopTiming(resource);
    }

    @Test
    public void blogPostsList() throws Exception {
        super.setUp();

        initBlogPosts();

        mBlogPostsContent = new ArrayList<>();
        for (BlogPost blogPost : mBlogPosts) {
            mBlogPostsContent.add(blogPost.getContent());
        }

        launchActivity();

        // move to the 2nd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft());

        onView(withId(R.id.blogsRecyclerView))
                .check(matches(isDisplayed()));

        checkBlogPostsRecyclerViewContent(mBlogPostsContent);
    }

}
