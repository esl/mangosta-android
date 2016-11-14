package inaka.com.mangosta.activities;

import android.content.Intent;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static inaka.com.mangosta.models.MyViewMatchers.atPositionOnRecyclerView;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

public class CreateChatActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(CreateChatActivity.class, true, false);

    @Before
    @Override
    public void setUp() {
        super.setUp();

        // users that exist
        doReturn(true).when(mXMPPSessionMock).userExists("user1");
        doReturn(true).when(mXMPPSessionMock).userExists("user2");

        // user that does not exist
        doReturn(false).when(mXMPPSessionMock).userExists("falseUser");

        launchActivity();
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), CreateChatActivity.class);
        mActivityTestRule.launchActivity(intent);
    }

    private int getSearchedUsersCount() {
        RecyclerView searchResultsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.searchResultRecyclerView);
        return searchResultsRecyclerView.getAdapter().getItemCount();
    }

    private int getUsersForChatCount() {
        RecyclerView searchResultsRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.membersRecyclerView);
        return searchResultsRecyclerView.getAdapter().getItemCount();
    }

    private void addUser1() {
        onView(withId(R.id.searchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(clearText())
                .perform(typeText("user1"));

        onView(withId(R.id.searchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        assertEquals(1, getSearchedUsersCount());

        onView(allOf(withId(R.id.addUserButton), isDisplayed()))
                .perform(click());

        assertEquals(0, getSearchedUsersCount());
        assertEquals(1, getUsersForChatCount());
    }

    private void addUser2() {
        onView(withId(R.id.searchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(clearText())
                .perform(typeText("user2"));

        onView(withId(R.id.searchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        assertEquals(1, getSearchedUsersCount());

        onView(allOf(withId(R.id.addUserButton), isDisplayed()))
                .perform(click());

        assertEquals(0, getSearchedUsersCount());
        assertEquals(2, getUsersForChatCount());
    }

    private void removeFirstUser() {
        onView(atPositionOnRecyclerView(R.id.membersRecyclerView, 0, R.id.removeUserButton))
                .check(matches(isDisplayed()))
                .perform(click());
        assertEquals(0, getUsersForChatCount());
    }

    private void pressButtonToCreateChat() {
        onView(withId(R.id.continueFloatingButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());
    }

    private void verifyEnteredToAChat() {
        onView(withId(R.id.chatSendMessageEditText))
                .check(matches(isDisplayed()));
    }

    private void enterGroupChatName(String name) {
        onView(withHint(R.string.enter_room_name_hint))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText(name));
    }

    private void createGroupChat() {
        onView(withText(android.R.string.yes))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    private void selectMUCType() {
        onView(withText(R.string.muc_chat_type))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    private void selectMUCLightType() {
        onView(withText(R.string.muc_light_chat_type))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    private void checkDialogToSelectRoomNameIsOpened() {
        onView(withText(R.string.room_name))
                .check(matches(isDisplayed()));
    }

    @Test
    public void searchUserNotFound() throws Exception {
        onView(withId(R.id.searchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("falseUser"));

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
        onView(withId(R.id.searchUserEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText("user1"));

        onView(withId(R.id.searchUserButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        Assert.assertEquals(1, getSearchedUsersCount());
    }

    @Test
    public void searchUserAndAddItToAChat() throws Exception {
        addUser1();
    }

    @Test
    public void addAndRemoveUsers() throws Exception {
        addUser1();

        removeFirstUser();

        addUser1();
        addUser2();

        onView(atPositionOnRecyclerView(R.id.membersRecyclerView, 1, R.id.removeUserButton))
                .check(matches(isDisplayed()))
                .perform(click());
        assertEquals(1, getUsersForChatCount());
    }

    @Test
    public void tryToCreateChatWithoutAddingUsersFirst() throws Exception {
        pressButtonToCreateChat();
        isToastMessageDisplayed(R.string.add_people_to_create_chat);

        addUser1();
        removeFirstUser();
        pressButtonToCreateChat();
        isToastMessageDisplayed(R.string.add_people_to_create_chat);
    }

    @Test
    public void create1to1Chat() throws Exception {
        addUser1();
        pressButtonToCreateChat();
        verifyEnteredToAChat();
    }

    @Test
    public void createMUC() throws Exception {
        addUser1();
        addUser2();
        pressButtonToCreateChat();
        checkDialogToSelectRoomNameIsOpened();
        selectMUCType();
        enterGroupChatName("test muc");
        createGroupChat();
        verifyEnteredToAChat();
    }


    @Test
    public void createdMUCLight() throws Exception {
        addUser1();
        addUser2();
        pressButtonToCreateChat();
        checkDialogToSelectRoomNameIsOpened();
        selectMUCLightType();
        enterGroupChatName("test muc light");
        createGroupChat();

        IdlingResource resource = startTiming(5000);
        verifyEnteredToAChat();
        stopTiming(resource);
    }

}
