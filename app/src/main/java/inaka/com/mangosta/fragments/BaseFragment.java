package inaka.com.mangosta.fragments;


import android.content.Context;
import androidx.fragment.app.Fragment;

import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.utils.Preferences;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;

public class BaseFragment extends Fragment {

    private Realm mRealm;
    private CompositeDisposable disposables;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mRealm = RealmManager.getInstance().getRealm();
        disposables = new CompositeDisposable();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mRealm != null && !Preferences.isTesting()) {
            mRealm.close();
        }
        disposables.dispose();
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

    protected void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

}
