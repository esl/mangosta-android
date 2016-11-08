package inaka.com.mangosta.activities;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;

import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.ArrayList;
import java.util.List;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static inaka.com.mangosta.models.MyViewMatchers.atPositionOnRecyclerView;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doReturn;

public class ManageFriendsActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(ManageFriendsActivity.class, true, false);

    private List<Jid> mFriends;

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
        Intent intent = new Intent(getContext(), ManageFriendsActivity.class);
        mActivityTestRule.launchActivity(intent);
    }

    private void setFriends() {
        mFriends = new ArrayList<>();
        try {
            mFriends.add(JidCreate.from("friend1@sarasa.com"));
            mFriends.add(JidCreate.from("friend2@sarasa.com"));
            mFriends.add(JidCreate.from("friend3@sarasa.com"));
            doReturn(mFriends).when(mRosterManagerMock).getBuddies();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getSearchedUsersCount() {
        RecyclerView searchResultsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.manageFriendsSearchResultRecyclerView);
        return searchResultsRecyclerView.getAdapter().getItemCount();
    }

    private int getFriendsCount() {
        RecyclerView friendsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.manageFriendsUsersRecyclerView);
        return friendsRecyclerView.getAdapter().getItemCount();
    }

    @Test
    public void searchUserNotFound() throws Exception {
        onView(withId(R.id.manageFriendsSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaFalse"));

        onView(withId(R.id.manageFriendsSearchUserButton))
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
        onView(withId(R.id.manageFriendsSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaTrue"));

        onView(withId(R.id.manageFriendsSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        Assert.assertEquals(1, getSearchedUsersCount());
    }

    @Test
    public void searchUserAndMakeItAFriend() throws Exception {
        onView(withId(R.id.manageFriendsSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaTrue"));

        onView(withId(R.id.manageFriendsSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        Assert.assertEquals(1, getSearchedUsersCount());

        onView(allOf(withId(R.id.addUserButton), isDisplayed()))
                .perform(click());

        Assert.assertEquals(0, getSearchedUsersCount());
        Assert.assertEquals(mFriends.size() + 1, getFriendsCount());
    }

    @Test
    public void unfriendUser() throws Exception {
        onView(atPositionOnRecyclerView(R.id.manageFriendsUsersRecyclerView, 0, R.id.removeUserButton))
                .check(matches(isDisplayed()))
                .perform(click());
        Assert.assertEquals(mFriends.size() - 1, getFriendsCount());
    }

    @Test
    public void unfriendAll() throws Exception {
        onView(withId(R.id.manageFriendsUsersUnfriendAllButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        Assert.assertEquals(0, getFriendsCount());

        onView(withId(R.id.manageFriendsUsersUnfriendAllButton))
                .check(matches(not(isDisplayed())));
    }

}
