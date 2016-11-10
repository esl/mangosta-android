package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;

import inaka.com.mangosta.R;
import inaka.com.mangosta.context.BaseInstrumentedTest;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.realm.RealmManager;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static inaka.com.mangosta.models.MyViewMatchers.atPositionOnRecyclerView;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class ChatActivityInstrumentedTest extends BaseInstrumentedTest {

    @Rule
    public ActivityTestRule mActivityTestRule =
            new ActivityTestRule<>(ChatActivity.class, true, false);

    private String mTestChatJID = "sarasa@erlang-solutions.com";
    private String mTestChatName = "Chat with user";
    private int mMessagesCount;

    @Override
    @Before
    public void setUp() {
        assumeTrue(isUserLoggedIn());

        try {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Chat chat = new Chat();
                    chat.setType(Chat.TYPE_1_T0_1);
                    chat.setShow(true);
                    chat.setName(mTestChatName);
                    chat.setJid(mTestChatJID);
                    chat.setDateCreated(new Date());
                    RealmManager.getInstance().saveChat(chat);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }

        launchActivity();

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

    @After
    public void afterTest() {
        try {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RealmManager.getInstance().deleteChatAndItsMessages(mTestChatJID);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void launchActivity() {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.CHAT_JID_PARAMETER, mTestChatJID);
        bundle.putString(ChatActivity.CHAT_NAME_PARAMETER, mTestChatName);
        intent.putExtras(bundle);
        mActivityTestRule.launchActivity(intent);
    }

    private void clickSendMessage() {
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
    }

    @Test
    public void sendTextMessage() throws Throwable {
        composeMessage("test message");
        clickSendMessage();
        checkMessagesCount(mMessagesCount);
    }

    @Test
    public void sendStickerMessage() throws Throwable {
        sendSticker();
        checkMessagesCount(mMessagesCount);
    }

    @Test
    public void fixTextMessageSent() throws Throwable {
//        composeMessage("test message");
//        clickSendMessage();
//        checkMessagesCount(mMessagesCount + 1);

//        onView(atPositionOnRecyclerView(R.id.chatMessagesRecyclerView, 0, ))
    }

    @Test
    public void retrieveMessagesArchive() throws Exception {

    }

}