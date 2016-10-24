package inaka.com.mangosta.fragments;


import android.content.Context;
import android.support.v4.app.Fragment;

import inaka.com.mangosta.realm.RealmManager;
import io.realm.Realm;

public class BaseFragment extends Fragment {

    private Realm mRealm;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mRealm = RealmManager.getRealm();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mRealm != null) {
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
                mRealm = RealmManager.getRealm();
            }
        } catch (Throwable e) {
            mRealm = RealmManager.getRealm();
        }
        return mRealm;
    }
}
