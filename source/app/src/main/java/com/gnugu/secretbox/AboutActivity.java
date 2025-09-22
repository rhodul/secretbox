package com.gnugu.secretbox;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Added import
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar_about);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setIcon(R.drawable.ic_action_sbox); // Set icon
            getSupportActionBar().setTitle(R.string.about);          // Set title
        }

        String version = "";
        try {
            PackageInfo pi = this.getPackageManager().getPackageInfo(
                    this.getPackageName(), 0);
            version = pi.versionName;
            String copyrightTemplate = getString(R.string.copyrightText);
            ((TextView) this.findViewById(R.id.copyright)).setText(Html.fromHtml(String.format(copyrightTemplate, version)));

            ((TextView) this.findViewById(R.id.credtits)).setText(Html.fromHtml(this
                    .getString(R.string.aboutText)));

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(AboutActivity.class.getName(), "package info", e);
        } catch (Exception e) { // Catch other potential exceptions from String.format or Html.fromHtml
            Log.e(AboutActivity.class.getName(), "Error setting about text", e);
            // Fallback if formatting or HTML parsing fails
            try {
                ((TextView) this.findViewById(R.id.copyright)).setText(getString(R.string.copyrightText)); // Raw string as fallback
                ((TextView) this.findViewById(R.id.credtits)).setText(getString(R.string.aboutText)); // Raw string as fallback
            } catch (Exception fallbackException) {
                Log.e(AboutActivity.class.getName(), "Error setting fallback about text", fallbackException);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
