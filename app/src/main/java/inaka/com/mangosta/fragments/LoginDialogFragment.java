package inaka.com.mangosta.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.CreateBlogActivity;
import inaka.com.mangosta.activities.SplashActivity;
import inaka.com.mangosta.models.Event;
import inaka.com.mangosta.xmpp.XMPPSession;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LoginDialogFragment extends DialogFragment {

    public static LoginDialogFragment newInstance() {
        return new LoginDialogFragment();
    }

    @BindView(R.id.loginUserNameEditText)
    EditText loginUserNameEditText;

    @BindView(R.id.loginJidCompletionEditText)
    EditText loginJidCompletionEditText;

    @BindView(R.id.loginPasswordEditText)
    EditText loginPasswordEditText;

    @BindView(R.id.loginServerEditText)
    EditText loginServerEditText;

    @BindView(R.id.loginButton)
    Button loginButton;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_dialog, container, false);
        ButterKnife.bind(this, view);

        toolbar.setTitle(getString(R.string.title_login));

        String userName = XMPPSession.DEFAULT_USER;
        String password = XMPPSession.DEFAULT_PASS;

        loginUserNameEditText.setText(userName);
        loginUserNameEditText.setSelection(userName.length());
        loginJidCompletionEditText.setText("@" + XMPPSession.SERVICE_NAME);

        loginPasswordEditText.setText(password);
        loginServerEditText.setText(XMPPSession.SERVER_NAME);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginAndStart(loginUserNameEditText.getText().toString(), loginPasswordEditText.getText().toString());
            }
        });

        return view;
    }

    private void loginAndStart(final String userName, final String password) {
        final ProgressDialog progress = ProgressDialog.show(getActivity(), getString(R.string.loading), null, true);

        Completable task = Completable.fromCallable(() -> {
            XMPPSession.startService(getActivity());
            ((SplashActivity) getActivity()).getService().login(userName, password);
            Thread.sleep(XMPPSession.REPLY_TIMEOUT);
            return null;
        });

        Disposable d = task
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    progress.dismiss();
                    ((SplashActivity) getActivity()).startApplication();
                }, error -> {
                    progress.dismiss();
                    XMPPSession.getInstance().getXMPPConnection().disconnect();
                    XMPPSession.clearInstance();
                    Toast.makeText(LoginDialogFragment.this.getContext(),
                            getString(R.string.error_login), Toast.LENGTH_SHORT).show();
                });
    }

}
