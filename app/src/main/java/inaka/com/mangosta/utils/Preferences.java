package inaka.com.mangosta.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.jxmpp.util.XmppDateTime;

import java.util.Date;

public class Preferences {

    private static final String APP_SHARED_PREFS = Preferences.class.getSimpleName();

    private static final String LOGGED_IN = "logged_in";

    private static final String XMPP_OAUTH_ACCESS_TOKEN = "xmpp_oauth_access_token";
    private static final String XMPP_OAUTH_REFRESH_TOKEN = "xmpp_oauth_refresh_token";
    private static final String DATE_LAST_TOKEN_UPDATE = "date_last_token_update";

    /**
     * User XMPP JID
     */
    private final String USER_XMPP_JID = "user_xmpp_jid";
    private final String USER_XMPP_PASSWORD = "user_xmpp_password";

    private SharedPreferences mPreferences;
    private static Preferences mInstance;
    private static boolean mIsTesting = false;

    private Preferences(Context context) {
        this.mPreferences = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
    }

    public static Preferences getInstance() {
        if (mInstance == null) {
            mInstance = new Preferences(MangostaApplication.getInstance());
        }
        return mInstance;
    }

    public static void setTest() {
        mIsTesting = true;
    }

    public static boolean isTesting() {
        return mIsTesting;
    }

    public String getXmppOauthAccessToken() {
        return mPreferences.getString(XMPP_OAUTH_ACCESS_TOKEN, "");
    }

    public void setXmppOauthAccessToken(String token) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(XMPP_OAUTH_ACCESS_TOKEN, token);
        editor.apply();
        setDateLastTokenUpdate(new Date());
    }

    public String getXmppOauthRefreshToken() {
        return mPreferences.getString(XMPP_OAUTH_REFRESH_TOKEN, "");
    }

    public void setXmppOauthRefreshToken(String token) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(XMPP_OAUTH_REFRESH_TOKEN, token);
        editor.apply();
        setDateLastTokenUpdate(new Date());
    }

    public String getDateLastTokenUpdate() {
        return mPreferences.getString(DATE_LAST_TOKEN_UPDATE, "");
    }

    private void setDateLastTokenUpdate(Date date) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(DATE_LAST_TOKEN_UPDATE, XmppDateTime.formatXEP0082Date(date));
        editor.apply();
    }

    public String getUserXMPPJid() {
        return mPreferences.getString(USER_XMPP_JID, "");
    }

    public void setUserXMPPJid(String userXMPPJid) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(USER_XMPP_JID, userXMPPJid);
        editor.apply();
    }

    public String getUserXMPPPassword() {
        return mPreferences.getString(USER_XMPP_PASSWORD, "");
    }

    public void setUserXMPPPassword(String password) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(USER_XMPP_PASSWORD, password);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return mPreferences.getBoolean(LOGGED_IN, false);
    }

    public void setLoggedIn(boolean logged) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(LOGGED_IN, logged);
        editor.apply();
    }

    public void deleteAll() {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.clear();
        editor.apply();
    }

}