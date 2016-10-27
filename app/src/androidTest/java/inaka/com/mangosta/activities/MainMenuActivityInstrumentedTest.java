package inaka.com.mangosta.activities;

import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;


import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import inaka.com.mangosta.R;
import inaka.com.mangosta.adapters.ViewPagerMainMenuAdapter;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.fragments.ChatsListFragment;
import inaka.com.mangosta.realm.RealmManager;

import static org.hamcrest.Matchers.allOf;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainMenuActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<MainMenuActivity> mMainMenuActivityActivityTestRule =
            new ActivityTestRule<>(MainMenuActivity.class);

    @Test
    public void chatListVisibility() throws Exception {

        IdlingResource resource = startTiming(10000);

        // check chat list visibility in 1st tab
//        onView(allOf(withId(R.id.button), isDescendantOfA(firstChildOf(withId(R.id.viewpager))))
//                withId(R.id.chatListRecyclerView))
//                .check(matches(isDisplayed()));

        // move to the 2nd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft());

        // check chat list visibility in 2nd tab
        onView(withId(R.id.chatListRecyclerView))
                .check(matches(isDisplayed()));

        // move to the 3rd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft());

        // check chat list visibility in 3rd tab
        onView(withId(R.id.chatListRecyclerView))
                .check(matches(isDisplayed()));

        stopTiming(resource);
    }

    @Test
    public void initializeOneToOneChatsList() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        IdlingResource resource = startTiming(10000);

        // Obtain the one to one chats fragment
        ChatsListFragment chatsListFragment = getChatsListFragment(ChatsListFragment.ONE_TO_ONE_CHATS_POSITION);

        // Check if it loads the correct amount of chats
        Assert.assertEquals(getChatsCount(chatsListFragment), RealmManager.get1to1Chats().size());

        stopTiming(resource);
    }

    @Test
    public void initializeMUCLightList() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        // move to the 2nd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft());

        IdlingResource resource = startTiming(10000);

        // Obtain the one to one chats fragment
        ChatsListFragment chatsListFragment = getChatsListFragment(ChatsListFragment.MUC_LIGHT_CHATS_POSITION);

        // Check if it loads the correct amount of chats
        Assert.assertEquals(getChatsCount(chatsListFragment), RealmManager.getMUCLights().size());

        stopTiming(resource);
    }

    @Test
    public void initializeMUCList() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        // move to the 3rd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft())
                .perform(swipeLeft());

        IdlingResource resource = startTiming(10000);

        // Obtain the one to one chats fragment
        ChatsListFragment chatsListFragment = getChatsListFragment(ChatsListFragment.MUC_CHATS_POSITION);

        // Check if it loads the correct amount of chats
        Assert.assertEquals(getChatsCount(chatsListFragment), RealmManager.getMUCs().size());

        stopTiming(resource);
    }

    private int getChatsCount(ChatsListFragment chatsListFragment) {
        return chatsListFragment.getChatListAdapter().getItemCount();
    }

    private ChatsListFragment getChatsListFragment(int index) {
        MainMenuActivity mainMenuActivity = mMainMenuActivityActivityTestRule.getActivity();
        ViewPagerMainMenuAdapter adapter = ((ViewPagerMainMenuAdapter) mainMenuActivity.mViewpagerMainMenu.getAdapter());
        return (ChatsListFragment) adapter.mFragmentList[index];
    }

}