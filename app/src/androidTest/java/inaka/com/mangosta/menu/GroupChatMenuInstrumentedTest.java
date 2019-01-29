package inaka.com.mangosta.menu;

import android.content.Intent;
import android.os.Bundle;
import androidx.test.rule.ActivityTestRule;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;

import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.ChatActivity;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.realm.RealmManager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assume.assumeTrue;

public class GroupChatMenuInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(ChatActivity.class, true, false);

    private static String mTestMUCLightJID = "muclightsample@erlang-solutions.com";
    private static String mTestMUCLightName = "MUC Light sample";

    @Override
    @Before
    public void setUp() {
        setUpTest();
        assumeTrue(isUserLoggedIn());
    }

    @BeforeClass
    public static void beforeAllTests() {
        Chat muclightChat = new Chat();
        muclightChat.setType(Chat.TYPE_MUC_LIGHT);
        muclightChat.setShow(true);
        muclightChat.setName(mTestMUCLightName);
        muclightChat.setJid(mTestMUCLightJID);
        muclightChat.setDateCreated(new Date());
        RealmManager.getInstance().saveChat(muclightChat);
    }

    @AfterClass
    public static void afterTest() {
        RealmManager.getInstance().deleteChatAndItsMessages(mTestMUCLightJID);
    }

    private void launchActivityWithMUCLight() {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.CHAT_JID_PARAMETER, mTestMUCLightJID);
        bundle.putString(ChatActivity.CHAT_NAME_PARAMETER, mTestMUCLightName);
        intent.putExtras(bundle);
        mActivityTestRule.launchActivity(intent);
    }

    @Test
    public void goToRoomMembers() throws Exception {
        launchActivityWithMUCLight();

        openActionBarOverflowOrOptionsMenu(getContext());

        onView(withText(R.string.action_chat_members))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.title_activity_chat_members))
                .check(matches(isDisplayed()));

        pressBack();
    }

    @Test
    public void changeRoomName() throws Exception {
        launchActivityWithMUCLight();

        onView(withText(mTestMUCLightName))
                .check(matches(isDisplayed()));

        openActionBarOverflowOrOptionsMenu(getContext());

        onView(withText(R.string.action_change_room_name))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.room_name))
                .check(matches(isDisplayed()));

        onView(withText(mTestMUCLightName))
                .perform(clearText());

        onView(withHint(R.string.enter_room_name_hint))
                .perform(typeText("new room name"));

        onView(withText(android.R.string.yes))
                .perform(click());

        isToastMessageDisplayed(R.string.room_name_changed);

        onView(withText("new room name"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void changeRoomSubject() throws Exception {
        launchActivityWithMUCLight();

        openActionBarOverflowOrOptionsMenu(getContext());

        onView(withText(R.string.action_change_room_subject))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.room_subject))
                .check(matches(isDisplayed()));

        onView(withHint(R.string.enter_room_subject_hint))
                .perform(clearText(), typeText("new room subject"));

        onView(withText(android.R.string.yes))
                .perform(click());

        isToastMessageDisplayed(R.string.room_subject_changed);

        onView(withText("new room subject"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void leaveChat() throws Exception {
        launchActivityWithMUCLight();

        openActionBarOverflowOrOptionsMenu(getContext());

        onView(withText(R.string.action_leave_chat))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.want_to_leave_chat))
                .check(matches(isDisplayed()));

        onView(withText(R.string.action_leave_chat))
                .perform(click());
    }

}
