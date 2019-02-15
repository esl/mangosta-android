package inaka.com.mangosta.fragments;


import android.content.Context;

import androidx.fragment.app.Fragment;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class BaseFragment extends Fragment {

    private CompositeDisposable disposables;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        disposables = new CompositeDisposable();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        disposables.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

}
