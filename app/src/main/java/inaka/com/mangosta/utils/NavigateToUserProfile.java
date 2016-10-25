package inaka.com.mangosta.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import inaka.com.mangosta.activities.UserProfileActivity;
import inaka.com.mangosta.models.User;
import inaka.com.mangosta.xmpp.XMPPUtils;

public class NavigateToUserProfile {

    private static void openUser(Context context, User user, boolean isAutheticadedUser) {
        Intent userOptionsActivityIntent = new Intent(context, UserProfileActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(UserProfileActivity.AUTH_USER_PARAMETER, isAutheticadedUser);
        bundle.putParcelable(UserProfileActivity.USER_PARAMETER, user);
        userOptionsActivityIntent.putExtras(bundle);
        context.startActivity(userOptionsActivityIntent);
    }

    public static void go(User user, Context context) {
        boolean isAuthenticatedUser = XMPPUtils.isAutenticatedUser(user);
        NavigateToUserProfile.openUser(context, user, isAuthenticatedUser);
    }

}
