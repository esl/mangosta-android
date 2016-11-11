package inaka.com.mangosta.menu;

import android.content.Intent;
import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;

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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assume.assumeTrue;

public class ChatMenuInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(ChatActivity.class, true, false);

    private static String mTestChatJID = "user@erlang-solutions.com";
    private static String mTestChatName = "Chat with user";
    private static String mTestMUCLightJID = "muclightsample@erlang-solutions.com";
    private static String mTestMUCLightName = "MUC Light sample";
    private int mMessagesCount;

    @Override
    @Before
    public void setUp() {
        setUpTest();
        assumeTrue(isUserLoggedIn());
    }

    @BeforeClass
    public static void beforeAllTests() {
        Chat chat = new Chat();
        chat.setType(Chat.TYPE_1_T0_1);
        chat.setShow(true);
        chat.setName(mTestChatName);
        chat.setJid(mTestChatJID);
        chat.setDateCreated(new Date());
        RealmManager.getInstance().saveChat(chat);

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
        RealmManager.getInstance().deleteChatAndItsMessages(mTestChatJID);
        RealmManager.getInstance().deleteChatAndItsMessages(mTestMUCLightJID);
    }

    private void initMessagesCount() {
        final RecyclerView chatMessagesRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.chatMessagesRecyclerView);
        try {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessagesCount = chatMessagesRecyclerView.getAdapter().getItemCount();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void launchActivityWithChat() {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.CHAT_JID_PARAMETER, mTestChatJID);
        bundle.putString(ChatActivity.CHAT_NAME_PARAMETER, mTestChatName);
        intent.putExtras(bundle);
        mActivityTestRule.launchActivity(intent);
        initMessagesCount();
    }

    private void launchActivityWithMUCLight() {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.CHAT_JID_PARAMETER, mTestMUCLightJID);
        bundle.putString(ChatActivity.CHAT_NAME_PARAMETER, mTestMUCLightName);
        intent.putExtras(bundle);
        mActivityTestRule.launchActivity(intent);
        initMessagesCount();
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

}
