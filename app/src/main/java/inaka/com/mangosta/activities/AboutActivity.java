package inaka.com.mangosta.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import inaka.com.mangosta.BuildConfig;
import inaka.com.mangosta.R;

public class AboutActivity extends BaseActivity {

    @Bind(R.id.aboutVersionNumberTextView)
    TextView aboutVersionNumberTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        String version = String.format(Locale.getDefault(), getString(R.string.version), BuildConfig.VERSION_NAME);
        aboutVersionNumberTextView.setText(version);
    }

}
