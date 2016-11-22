package inaka.com.mangosta.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import inaka.com.mangosta.activities.UserProfileActivity;
import inaka.com.mangosta.models.User;

public class NavigateToUserProfile {

    private static void openUser(Context context, User user) {
        Intent userOptionsActivityIntent = new Intent(context, UserProfileActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(UserProfileActivity.USER_PARAMETER, user);
        userOptionsActivityIntent.putExtras(bundle);
        context.startActivity(userOptionsActivityIntent);
    }

    public static void go(User user, Context context) {
        NavigateToUserProfile.openUser(context, user);
    }

}
