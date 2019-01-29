package inaka.com.mangosta.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import inaka.com.mangosta.BuildConfig;
import inaka.com.mangosta.R;

public class AboutActivity extends BaseActivity {

    @BindView(R.id.aboutVersionNumberTextView)
    TextView aboutVersionNumberTextView;

    @BindView(R.id.mangostaLinkButton)
    Button mangostaLinkButton;

    @BindView(R.id.mongooseimLinkButton)
    Button mongooseimLinkButton;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        unbinder = ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        String version = String.format(Locale.getDefault(), getString(R.string.version), BuildConfig.VERSION_NAME);
        aboutVersionNumberTextView.setText(version);

        mangostaLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBrowserWithUrl(getString(R.string.mangosta_link));
            }
        });

        mongooseimLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBrowserWithUrl(getString(R.string.mongooseim_link));
            }
        });

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

    private void openBrowserWithUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

}
