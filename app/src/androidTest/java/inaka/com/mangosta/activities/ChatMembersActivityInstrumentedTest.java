package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.RecyclerViewInteraction;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.realm.Realm;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;

public class ChatMembersActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(ChatMembersActivity.class, true, false);

    List<String> mUsers;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        mUsers = new ArrayList<>();
        mUsers.add("user1@erlang-solutions.com");
        mUsers.add("user2@erlang-solutions.com");
        mUsers.add("user3@erlang-solutions.com");
        mUsers.add("user4@erlang-solutions.com");

        Chat chat = new Chat();
        chat.setType(Chat.TYPE_MUC_LIGHT);

        try {
            doReturn(mUsers).when(mRoomManagerMock).loadMUCLightMembers(any(String.class));
            doReturn(chat).when(mRealmManagerMock).getChatFromRealm(any(Realm.class), any(String.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchActivity(boolean asAdmin) {
        Intent intent = new Intent(getContext(), ChatMembersActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(ChatMembersActivity.IS_ADMIN_PARAMETER, asAdmin);
        bundle.putString(ChatMembersActivity.ROOM_JID_PARAMETER, "");
        intent.putExtras(bundle);
        mActivityTestRule.launchActivity(intent);
    }

    private void checkRecyclerViewContent(final List<String> users) {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.chatMembersRecyclerView), ViewMatchers.isDisplayed()))
                .withItems(users)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String user, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(XMPPUtils.fromJIDToUserName(user)))).check(view, e);
                    }
                });
    }

    @Test
    public void loadMembersAsAdminAndGoToEdit() throws Exception {
        launchActivity(true);

        checkRecyclerViewContent(mUsers);

        onView(withId(R.id.editChatMembersFloatingButton))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.title_activity_edit_room_members))
                .check(matches(isDisplayed()));

        pressBack();
    }

    @Test
    public void loadMembersNotAsAdmin() throws Exception {
        launchActivity(false);

        checkRecyclerViewContent(mUsers);

        onView(withId(R.id.editChatMembersFloatingButton))
                .check(matches(not(isDisplayed())));
    }

}
