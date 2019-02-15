package inaka.com.mangosta.chat;

import android.util.Log;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import inaka.com.mangosta.database.MangostaDatabase;
import inaka.com.mangosta.models.Chat;
import inaka.com.mangosta.models.ChatMessage;
import inaka.com.mangosta.utils.MangostaApplication;
import inaka.com.mangosta.utils.Preferences;
import inaka.com.mangosta.xmpp.XMPPUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MessageManager {

    private static final String TAG = MessageManager.class.getSimpleName();

    private static MessageManager instance;

    private MangostaDatabase database = MangostaApplication.getInstance().getDatabase();

    public static MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }


}
