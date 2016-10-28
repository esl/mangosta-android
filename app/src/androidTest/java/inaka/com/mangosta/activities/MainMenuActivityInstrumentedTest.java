package inaka.com.mangosta.activities;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Assert;
import org.junit.Assume;
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
import inaka.com.mangosta.context.RecyclerViewInteraction;
import inaka.com.mangosta.fragments.ChatsListFragment;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.ChatOrderComparator;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class MainMenuActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<MainMenuActivity> mMainMenuActivityActivityTestRule =
            new ActivityTestRule<>(MainMenuActivity.class);

    private List<String> mMUCNames;
    private List<String> mMUCLightNames;
    private List<String> mOneToOneChatNames;
    private List<Chat> mMUCs;
    private List<Chat> mMUCLights;
    private List<Chat> mOneToOneChats;

    @Before
    public void beforeTests() {
        obtain1to1Chats();
        obtainMUCLights();
        obtainMUCs();
    }

    private void obtainMUCs() {
        mMUCs = RealmManager.getMUCs();
        Collections.sort(mMUCs, new ChatOrderComparator());
        mMUCNames = new ArrayList<>();
        for (Chat chat : mMUCs) {
            mMUCNames.add(chat.getName());
        }
    }

    private void obtainMUCLights() {
        mMUCLights = RealmManager.getMUCLights();
        Collections.sort(mMUCLights, new ChatOrderComparator());
        mMUCLightNames = new ArrayList<>();
        for (Chat chat : mMUCLights) {
            mMUCLightNames.add(chat.getName());
        }
    }

    private void obtain1to1Chats() {
        mOneToOneChats = RealmManager.get1to1Chats();
        Collections.sort(mOneToOneChats, new ChatOrderComparator());
        mOneToOneChatNames = new ArrayList<>();
        for (Chat chat : mOneToOneChats) {
            mOneToOneChatNames.add(chat.getName());
        }
    }

    @Test
    public void initializeOneToOneChatsList() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        // Obtain the one to one chats fragment
        ChatsListFragment chatsListFragment = getChatsListFragment(ChatsListFragment.ONE_TO_ONE_CHATS_POSITION);

        // Check if it loads the correct amount of chats
        Assert.assertEquals(getChatsCount(chatsListFragment), mOneToOneChats.size());
    }

    @Test
    public void initializeMUCLightList() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        // move to the 2nd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft());

        // Obtain the one to one chats fragment
        ChatsListFragment chatsListFragment = getChatsListFragment(ChatsListFragment.MUC_LIGHT_CHATS_POSITION);

        // Check if it loads the correct amount of chats
        Assert.assertEquals(getChatsCount(chatsListFragment), mMUCLights.size());
    }

    @Test
    public void initializeMUCList() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        // move to the 3rd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft())
                .perform(swipeLeft());

        // Obtain the one to one chats fragment
        ChatsListFragment chatsListFragment = getChatsListFragment(ChatsListFragment.MUC_CHATS_POSITION);

        // Check if it loads the correct amount of chats
        Assert.assertEquals(getChatsCount(chatsListFragment), mMUCs.size());
    }

    private int getChatsCount(ChatsListFragment chatsListFragment) {
        return chatsListFragment.getChatListAdapter().getItemCount();
    }

    private ChatsListFragment getChatsListFragment(int index) {
        MainMenuActivity mainMenuActivity = mMainMenuActivityActivityTestRule.getActivity();
        ViewPagerMainMenuAdapter adapter = ((ViewPagerMainMenuAdapter) mainMenuActivity.mViewpagerMainMenu.getAdapter());
        return (ChatsListFragment) adapter.mFragmentList[index];
    }

    @Test
    public void oneToOneChatsInRecyclerView() throws Throwable {
        Assume.assumeTrue(isUserLoggedIn());
        checkRecyclerViewContent(mOneToOneChatNames);
    }

    @Test
    public void mucLightsInRecyclerView() throws Throwable {
        Assume.assumeTrue(isUserLoggedIn());

        // move to the 2nd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft());

        checkRecyclerViewContent(mMUCLightNames);
    }

    @Test
    public void mucsInRecyclerView() throws Throwable {
        Assume.assumeTrue(isUserLoggedIn());

        // move to the 3rd tab
        onView(withId(R.id.viewpagerMainMenu))
                .perform(swipeLeft())
                .perform(swipeLeft());

        checkRecyclerViewContent(mMUCNames);
    }

    private void checkRecyclerViewContent(final List<String> chatNames) throws Throwable {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.chatListRecyclerView), ViewMatchers.isDisplayed()))
                .withItems(chatNames)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String chatName, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(chatName))).check(view, e);
                    }
                });
    }

}
