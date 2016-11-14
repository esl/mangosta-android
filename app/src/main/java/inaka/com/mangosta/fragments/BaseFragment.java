package inaka.com.mangosta.fragments;


import android.content.Context;
import android.support.v4.app.Fragment;

import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import io.realm.Realm;

public class BaseFragment extends Fragment {

    private Realm mRealm;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mRealm = RealmManager.getInstance().getRealm();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mRealm != null && !Preferences.isTesting()) {
            mRealm.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected Realm getRealm() {
        try {
            if (mRealm.isClosed()) {
                mRealm = RealmManager.getInstance().getRealm();
            }
        } catch (Throwable e) {
            mRealm = RealmManager.getInstance().getRealm();
        }
        return mRealm;
    }
}
