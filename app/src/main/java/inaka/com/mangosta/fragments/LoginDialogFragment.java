package inaka.com.mangosta.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.activities.MainMenuActivity;
import inaka.com.mangosta.xmpp.XMPPSession;

public class LoginDialogFragment extends DialogFragment {

    public static LoginDialogFragment newInstance() {
        return new LoginDialogFragment();
    }

    @Bind(R.id.loginUserNameEditText)
    EditText loginUserNameEditText;

    @Bind(R.id.loginJidCompletionEditText)
    EditText loginJidCompletionEditText;

    @Bind(R.id.loginPasswordEditText)
    EditText loginPasswordEditText;

    @Bind(R.id.loginServerEditText)
    EditText loginServerEditText;

    @Bind(R.id.loginButton)
    Button loginButton;

    // TODO change this
    private String mPassword = "6fsp2u9y";
    private String mUserName = "ramabit";
//    private String mUserName = "gardano";
//    private String mUserName = "griveroa-inaka";

//    private String mUserName = "test.user";
//    private String mPassword = "9xpW9mmUenFgMjay";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_dialog, container, false);
        ButterKnife.bind(this, view);

        loginUserNameEditText.setText(mUserName);
        loginUserNameEditText.setSelection(mUserName.length());
        loginJidCompletionEditText.setText("@" + XMPPSession.SERVICE_NAME);
        loginPasswordEditText.setText(mPassword);
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

        Tasks.executeInBackground(getActivity(), new BackgroundWork<Object>() {
            @Override
            public Object doInBackground() throws Exception {
                XMPPSession.getInstance().login(userName, password);
                return null;
            }
        }, new Completion<Object>() {
            @Override
            public void onSuccess(Context context, Object result) {
                progress.dismiss();
                startApplication();
            }

            @Override
            public void onError(Context context, Exception e) {
                progress.dismiss();
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startApplication() {
        Intent mainMenuIntent = new Intent(getActivity(), MainMenuActivity.class);
        mainMenuIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainMenuIntent);
        getActivity().finish();
    }

}
