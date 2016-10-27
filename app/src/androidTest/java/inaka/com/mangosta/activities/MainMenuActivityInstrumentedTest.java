package inaka.com.mangosta.activities;

import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import inaka.com.mangosta.adapters.ViewPagerMainMenuAdapter;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.fragments.ChatsListFragment;
import inaka.com.mangosta.realm.RealmManager;

@RunWith(AndroidJUnit4.class)
public class MainMenuActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule<MainMenuActivity> mMainMenuActivityActivityTestRule =
            new ActivityTestRule<>(MainMenuActivity.class);

    @Test
    public void initializeOneToOneChatsList() throws Exception {
        Assume.assumeTrue(isUserLoggedIn());

        IdlingResource resource = startTiming(10000);

        // Obtain the one to one chats fragment
        ChatsListFragment chatsListFragment = getChatsListFragment(ChatsListFragment.ONE_TO_ONE_CHATS_POSITION);

        // Check if it loads the correct amount of chats
        int chatsCount = chatsListFragment.getChatListAdapter().getItemCount();
        Assert.assertEquals(chatsCount, RealmManager.get1to1Chats().size());

        stopTiming(resource);
    }

    private ChatsListFragment getChatsListFragment(int index) {
        MainMenuActivity mainMenuActivity = mMainMenuActivityActivityTestRule.getActivity();
        ViewPagerMainMenuAdapter adapter = ((ViewPagerMainMenuAdapter) mainMenuActivity.mViewpagerMainMenu.getAdapter());
        return (ChatsListFragment) adapter.mFragmentList[index];
    }

}
