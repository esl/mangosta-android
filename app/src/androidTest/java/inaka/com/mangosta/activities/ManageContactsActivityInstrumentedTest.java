package inaka.com.mangosta.activities;

import android.content.Intent;
import androidx.test.rule.ActivityTestRule;
import androidx.recyclerview.widget.RecyclerView;

import org.jivesoftware.smack.packet.Presence;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.HashMap;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static inaka.com.mangosta.models.MyViewMatchers.atPositionOnRecyclerView;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

public class ManageContactsActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(ManageContactsActivity.class, true, false);

    private HashMap<Jid, Presence.Type> mFriends;

    @Before
    @Override
    public void setUp() {
        super.setUp();

        // user that exists
        doReturn(true).when(mXMPPSessionMock).userExists("sarasaTrue");

        // user that does not exist
        doReturn(false).when(mXMPPSessionMock).userExists("sarasaFalse");

        setFriends();

        launchActivity();
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), ManageContactsActivity.class);
        mActivityTestRule.launchActivity(intent);
    }

    private void setFriends() {
        mFriends = new HashMap<>();
        try {
            mFriends.put(JidCreate.from("friend1@sarasa.com"), Presence.Type.fromString("available"));
            mFriends.put(JidCreate.from("friend2@sarasa.com"), Presence.Type.fromString("available"));
            mFriends.put(JidCreate.from("friend3@sarasa.com"), Presence.Type.fromString("available"));
            doReturn(mFriends).when(mRosterManagerMock).getContacts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getSearchedUsersCount() {
        RecyclerView searchResultsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.manageContactsSearchResultRecyclerView);
        return searchResultsRecyclerView.getAdapter().getItemCount();
    }

    private int getFriendsCount() {
        RecyclerView friendsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.manageContactsUsersRecyclerView);
        return friendsRecyclerView.getAdapter().getItemCount();
    }

    @Test
    public void searchUserNotFound() throws Exception {
        onView(withId(R.id.manageContactsSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaFalse"));

        onView(withId(R.id.manageContactsSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        onView(withText(R.string.user_not_found))
                .check(matches(isDisplayed()));

        onView(withText(android.R.string.ok))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    @Test
    public void searchUserFound() throws Exception {
        onView(withId(R.id.manageContactsSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaTrue"));

        onView(withId(R.id.manageContactsSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        assertEquals(1, getSearchedUsersCount());
    }

    @Test
    public void searchUserAndMakeItAFriend() throws Exception {
        onView(withId(R.id.manageContactsSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaTrue"));

        onView(withId(R.id.manageContactsSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        assertEquals(1, getSearchedUsersCount());

        onView(allOf(withId(R.id.addUserButton), isDisplayed()))
                .perform(click());

        assertEquals(0, getSearchedUsersCount());
        assertEquals(mFriends.size() + 1, getFriendsCount());
    }

    @Test
    public void unfriendUser() throws Exception {
        onView(atPositionOnRecyclerView(R.id.manageContactsUsersRecyclerView, 0, R.id.removeUserButton))
                .check(matches(isDisplayed()))
                .perform(click());
        assertEquals(mFriends.size() - 1, getFriendsCount());
    }

    @Test
    public void unfriendAll() throws Exception {
        onView(withId(R.id.manageContactsUsersRemoveAllContactsButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        assertEquals(0, getFriendsCount());

        onView(withId(R.id.manageContactsUsersRemoveAllContactsButton))
                .check(matches(not(isDisplayed())));
    }

}
