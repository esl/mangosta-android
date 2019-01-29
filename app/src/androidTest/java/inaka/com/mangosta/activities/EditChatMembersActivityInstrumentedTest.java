package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.MyViewMatchers;
import inaka.com.mangosta.models.RecyclerViewInteraction;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.realm.Realm;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

public class EditChatMembersActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(EditChatMemberActivity.class, true, false);

    List<String> mUsers;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        // user that exists
        doReturn(true).when(mXMPPSessionMock).userExists("userExists");

        // user that does not exist
        doReturn(false).when(mXMPPSessionMock).userExists("userNotExists");

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

    private void launchActivity() {
        Intent intent = new Intent(getContext(), EditChatMemberActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(EditChatMemberActivity.CHAT_JID_PARAMETER, "");
        intent.putExtras(bundle);
        mActivityTestRule.launchActivity(intent);
    }

    private void checkRecyclerViewContent(final List<String> users) {
        RecyclerViewInteraction.<String>onRecyclerView(allOf(withId(R.id.membersRecyclerView), ViewMatchers.isDisplayed()))
                .withItems(users)
                .check(new RecyclerViewInteraction.ItemViewAssertion<String>() {
                    @Override
                    public void check(String user, View view, NoMatchingViewException e) {
                        matches(hasDescendant(withText(XMPPUtils.fromJIDToUserName(user)))).check(view, e);
                    }
                });
    }

    private int getSearchedUsersCount() {
        RecyclerView searchResultsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.searchResultRecyclerView);
        return searchResultsRecyclerView.getAdapter().getItemCount();
    }

    private int getMembersCount() {
        RecyclerView blockedUsersRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.membersRecyclerView);
        return blockedUsersRecyclerView.getAdapter().getItemCount();
    }

    @Test
    public void loadMembersList() throws Exception {
        launchActivity();
        checkRecyclerViewContent(mUsers);
    }

    @Test
    public void searchUserNotFound() throws Exception {
        launchActivity();

        onView(withId(R.id.searchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("userNotExists"));

        onView(withId(R.id.searchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        onView(withText(R.string.invite_to_mangosta))
                .check(matches(isDisplayed()));

        onView(withText(android.R.string.ok))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    @Test
    public void searchUserFound() throws Exception {
        launchActivity();

        onView(withId(R.id.searchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("userExists"));

        onView(withId(R.id.searchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        assertEquals(1, getSearchedUsersCount());
    }

    @Test
    public void addUser() throws Exception {
        launchActivity();

        onView(withId(R.id.searchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("userExists"));

        onView(withId(R.id.searchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        assertEquals(1, getSearchedUsersCount());

        onView(MyViewMatchers.atPositionOnRecyclerView(R.id.searchResultRecyclerView, 0, R.id.addUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        // new mocked result
        mUsers.add(XMPPUtils.fromUserNameToJID("userExists"));
        doReturn(mUsers).when(mRoomManagerMock).loadMUCLightMembers(any(String.class));

        assertEquals(0, getSearchedUsersCount());
        assertEquals(mUsers.size(), getMembersCount());
        checkRecyclerViewContent(mUsers);
    }

    @Test
    public void removeUser() throws Exception {
        launchActivity();

        onView(MyViewMatchers.atPositionOnRecyclerView(R.id.membersRecyclerView, 0, R.id.removeUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        // new mocked result
        mUsers.remove(0);
        doReturn(mUsers).when(mRoomManagerMock).loadMUCLightMembers(any(String.class));

        assertEquals(0, getSearchedUsersCount());
        assertEquals(mUsers.size(), getMembersCount());
        checkRecyclerViewContent(mUsers);
    }


}
