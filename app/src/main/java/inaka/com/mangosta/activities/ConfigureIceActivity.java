package inaka.com.mangosta.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.attribute.Attribute;
import org.ice4j.attribute.ErrorCodeAttribute;
import org.ice4j.attribute.NonceAttribute;
import org.ice4j.message.Message;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.R;
import inaka.com.mangosta.models.IceConfiguration;
import inaka.com.mangosta.realm.RealmManager;
import inaka.com.mangosta.videostream.IceClient;
import io.realm.Realm;

public class ConfigureIceActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.turnAddress)
    EditText turnAddress;

    @Bind(R.id.turnPort)
    EditText turnPort;

    @Bind(R.id.turnRealm)
    EditText turnRealm;

    @Bind(R.id.turnUsername)
    EditText turnUsername;

    @Bind(R.id.turnPassword)
    EditText turnPassword;

    @Bind(R.id.buttonTestConnection)
    Button testConnection;

    @Bind(R.id.buttonSaveConfiguration)
    Button saveConfiguration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_ice);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);


        saveConfiguration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentConfiguration();
                setResult(RESULT_OK, new Intent());
                ConfigureIceActivity.this.finish();
            }
        });

        testConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                testConnection.setEnabled(false);
                            }
                        });

                        checkIceConnectivity(testConnection);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                testConnection.setEnabled(true);
                            }
                        });

                        return null;
                    }
                };

                task.execute();
            }
        });
    }

    private void checkIceConnectivity(final Button testConnection) {
        try {
            IceConfiguration localConf = new IceConfiguration();
            loadConfigurationFromForm(localConf);
            IceClient iceClient = new IceClient(localConf, null);

            ErrorCodeAttribute e = testAllocate(iceClient, 3);
            if(e == null) {
                toast("Connected successfully to " + localConf.getTurnRealm() + "!");
            } else {
                toast("Error: " + ErrorCodeAttribute.getDefaultReasonPhrase(e.getErrorCode()));
            }
        } catch (Exception e) {
            toast("Error: " + e.toString());
            e.printStackTrace();
        }
    }

    private ErrorCodeAttribute testAllocate(IceClient iceClient, int tries) throws IOException, StunException {
        if(tries <= 0) {
            throw new RuntimeException("unable to negotiate STUN nonce");
        }

        Request request = MessageFactory.createAllocateRequest((byte) 17, false);
        StunMessageEvent event = iceClient.sendRequestAndWaitForResponse(request);
        if(event == null)
            throw new RuntimeException("STUN request timeout");

        Message message = event.getMessage();
        ErrorCodeAttribute e = (ErrorCodeAttribute) message.getAttribute(Attribute.ERROR_CODE);
        if(e != null) {
            NonceAttribute nonce = (NonceAttribute) message.getAttribute(Attribute.NONCE);
            if (nonce != null)
                iceClient.getAuthSession().setNonce(nonce.getNonce());

            if(e.getErrorCode() == ErrorCodeAttribute.STALE_NONCE) {
                return testAllocate(iceClient, tries - 1);
            }
        } else {
            Request cancelAllocation = MessageFactory.createRefreshRequest(0);
            iceClient.sendRequest(cancelAllocation);
        }

        return e;
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ConfigureIceActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadConfigurationFromForm(IceConfiguration conf) {
        conf.setTurnAddress(turnAddress.getText().toString().trim());
        conf.setTurnPort(Integer.parseInt(turnPort.getText().toString().trim()));
        conf.setTurnRealm(turnRealm.getText().toString().trim());
        conf.setTurnUsername(turnUsername.getText().toString().trim());
        conf.setTurnPassword(turnPassword.getText().toString().trim());
    }

    private void saveCurrentConfiguration() {
        try {
            Realm realm = RealmManager.getInstance().getRealm();
            IceConfiguration conf = getConfiguration();

            realm.beginTransaction();

            loadConfigurationFromForm(conf);

            realm.commitTransaction();
            realm.close();

            reloadTextFields();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        reloadTextFields();
    }



    private void reloadTextFields() {
        IceConfiguration conf = getConfiguration();
        turnAddress.setText(conf.getTurnAddress());
        turnPort.setText(String.valueOf(conf.getTurnPort()));
        turnRealm.setText(conf.getTurnRealm());
        turnUsername.setText(conf.getTurnUsername());
        turnPassword.setText(conf.getTurnPassword());
    }

    private IceConfiguration getConfiguration() {
        return RealmManager.getInstance().getIceConfiguration();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

}
