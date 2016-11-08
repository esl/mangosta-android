package inaka.com.mangosta.activities;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import static org.mockito.Mockito.doReturn;

public class BlockUsersActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(BlockUsersActivity.class, true, false);

    private List<Jid> mBlockedUsers;

    @Before
    @Override
    public void setUp() {
        super.setUp();

        // user that exists
        doReturn(true).when(mXMPPSessionMock).userExists("sarasaTrue");

        // user that does not exist
        doReturn(false).when(mXMPPSessionMock).userExists("sarasaFalse");

        // set list of blocked users
        setBlockedUsers();

        launchActivity();
    }

    private void setBlockedUsers() {
        mBlockedUsers = new ArrayList<>();
        try {
            mBlockedUsers.add(JidCreate.from("blocked1@sarasa.com"));
            mBlockedUsers.add(JidCreate.from("blocked2@sarasa.com"));
            mBlockedUsers.add(JidCreate.from("blocked3@sarasa.com"));
            doReturn(mBlockedUsers).when(mXMPPSessionMock).getBlockList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), BlockUsersActivity.class);
        mActivityTestRule.launchActivity(intent);
    }

    private int getSearchedUsersCount() {
        RecyclerView searchResultsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.blockSearchResultRecyclerView);
        return searchResultsRecyclerView.getAdapter().getItemCount();
    }

    private int getBlockedUsersCount() {
        RecyclerView blockedUsersRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.blockedUsersRecyclerView);
        return blockedUsersRecyclerView.getAdapter().getItemCount();
    }

    @Test
    public void searchUserNotFound() throws Exception {
        onView(withId(R.id.blockSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaFalse"));

        onView(withId(R.id.blockSearchUserButton))
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
        onView(withId(R.id.blockSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaTrue"));

        onView(withId(R.id.blockSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        Assert.assertEquals(1, getSearchedUsersCount());
    }

    @Test
    public void searchUserAndBlockIt() throws Exception {
        onView(withId(R.id.blockSearchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("sarasaTrue"));

        onView(withId(R.id.blockSearchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        Assert.assertEquals(1, getSearchedUsersCount());

        onView(allOf(withId(R.id.addUserButton), isDisplayed()))
                .perform(click());

        Assert.assertEquals(0, getSearchedUsersCount());
        Assert.assertEquals(mBlockedUsers.size() + 1, getBlockedUsersCount());
    }

    @Test
    public void unblockUser() throws Exception {
        onView(atPositionOnRecyclerView(R.id.blockedUsersRecyclerView, 0, R.id.removeUserButton))
                .check(matches(isDisplayed()))
                .perform(click());
        Assert.assertEquals(mBlockedUsers.size() - 1, getBlockedUsersCount());
    }

}
