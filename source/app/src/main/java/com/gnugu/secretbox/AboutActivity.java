package com.gnugu.secretbox;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.NavUtils; // Corrected import
import androidx.appcompat.app.AppCompatActivity; // Corrected import
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity { // Corrected base class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        // Show the Up button in the action bar.
        if (getSupportActionBar() != null) { // Good practice to check for null
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String version = "";
        try {
            PackageInfo pi = this.getPackageManager().getPackageInfo(
                    this.getPackageName(), 0);
            version = pi.versionName;
            // Html.fromHtml(String) is deprecated in API 24. For now, keeping original logic.
            // Modern usage: Html.fromHtml(getString(R.string.copyrightText, version), Html.FROM_HTML_MODE_LEGACY)
            ((TextView) this.findViewById(R.id.copyright)).setText(Html.fromHtml(this
                    .getString(R.string.copyrightText, version)));

            ((TextView) this.findViewById(R.id.credtits)).setText(Html.fromHtml(this
                    .getString(R.string.aboutText)));

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(AboutActivity.class.getName(), "package info", e);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
