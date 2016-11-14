package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.realm.RealmManager;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class ChatActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(ChatActivity.class, true, false);

    private static String mTestChatJID = "user@erlang-solutions.com";
    private static String mTestChatName = "Chat with user";
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
    }

    @AfterClass
    public static void afterAllTests() {
        RealmManager.getInstance().deleteChatAndItsMessages(mTestChatJID);
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

    private void clickSendTextMessage() {
        onView(withId(R.id.chatSendMessageButton))
                .check(matches(isDisplayed()))
                .perform(click());
        mMessagesCount++;
    }

    private void composeMessage(String message) {
        onView(withId(R.id.chatSendMessageEditText))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(typeText(message));
    }

    private void checkMessagesCount(final int count) throws Throwable {
        onView(withId(R.id.chatMessagesRecyclerView))
                .check(matches(isDisplayed()));

        final RecyclerView chatMessagesRecyclerView =
                (RecyclerView) getCurrentActivity().findViewById(R.id.chatMessagesRecyclerView);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(count, chatMessagesRecyclerView.getAdapter().getItemCount());
            }
        });
    }

    private void sendSticker() {
        onView(withId(R.id.stickersMenuImageButton))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .perform(click());

        onView(withId(R.id.stickersRecyclerView))
                .check(matches(isDisplayed()));

        onView(atPositionOnRecyclerView(R.id.stickersRecyclerView, 0, -1))
                .perform(click());

        mMessagesCount++;
    }

    @Test
    public void sendTextMessage() throws Throwable {
        launchActivityWithChat();

        composeMessage("test message");
        clickSendTextMessage();

        IdlingResource resource = startTiming(5000);
        checkMessagesCount(mMessagesCount);
        stopTiming(resource);
    }

    @Test
    public void sendStickerMessage() throws Throwable {
        launchActivityWithChat();

        sendSticker();
        checkMessagesCount(mMessagesCount);
    }

    @Test
    public void fixTextMessageSent() throws Throwable {
        launchActivityWithChat();

        composeMessage("test message to be fixed");
        clickSendTextMessage();

        IdlingResource resource = startTiming(5000);

        checkMessagesCount(mMessagesCount);

        onView(atPositionOnRecyclerView(R.id.chatMessagesRecyclerView, mMessagesCount - 1, R.id.imageEditMessage))
                .perform(click());

        onView(withText(R.string.correct_message))
                .check(matches(isDisplayed()));

        onView(withText("test message to be fixed"))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(clearText());

        onView(withHint(R.string.hint_edit_text))
                .perform(typeText("fixed message"));

        onView(withText(android.R.string.ok))
                .perform(click());

        onView(atPositionOnRecyclerView(R.id.chatMessagesRecyclerView, mMessagesCount - 1, R.id.messageContentTextView))
                .check(matches(isDisplayed()))
                .check(matches(withText("fixed message")));

        stopTiming(resource);
    }

}
