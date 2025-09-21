package com.gnugu.secretbox;

import java.util.Locale;

import android.content.Intent;
import android.database.Cursor;
import androidx.appcompat.app.AppCompatActivity; // Corrected import
import androidx.appcompat.app.ActionBar; // Corrected import
import androidx.fragment.app.Fragment; // Corrected import
import androidx.fragment.app.FragmentManager; // Corrected import
import androidx.fragment.app.FragmentTransaction; // Corrected import
import androidx.fragment.app.FragmentPagerAdapter; // Corrected import
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager; // Corrected import
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class SecretEditorActivity extends AppCompatActivity implements ActionBar.TabListener {

    public static final String ARG_COMPARTMENT_ID = "compartment_id";
    public static final String ARG_SECRET_ID = "secret_id";
    public static final String ARG_EDIT_MODE = "edit_mode";


    /**
     * The {@link androidx.viewpager.widget.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link androidx.fragment.app.FragmentStatePagerAdapter}.
     */
    private TabsPagerAdapter mTabsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private SecretEditorFragment mEditorFragment;
    private SecretEditorPicturesFragment mPicturesFragment;

    private boolean mEditMode = false;
    private long mCompartmentId;
    private long mSecretId;
    private DataAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_editor);

        Intent intent = getIntent();
        mEditMode = intent.getBooleanExtra(ARG_EDIT_MODE, false);
        mCompartmentId = intent.getLongExtra(ARG_COMPARTMENT_ID, -1);
        // is it edit or insert?
        mSecretId = intent.getLongExtra(ARG_SECRET_ID, -1);

        Application application = (Application) getApplication();
        // SecretEditorActivity is an AppCompatActivity, which is a FragmentActivity
        mAdapter = application.getDataAdapter(this);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        // actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS); // Deprecated

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mTabsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() { // Use addOnPageChangeListener for ViewPager
            @Override
            public void onPageSelected(int position) {
                if (actionBar != null) {
                    // actionBar.setSelectedNavigationItem(position); // Related to deprecated NAVIGATION_MODE_TABS
                    // If using TabLayout, you would update it here: tabLayout.getTabAt(position).select();
                }
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        // This loop is related to the deprecated NAVIGATION_MODE_TABS
        // If using TabLayout, tabs are typically defined in XML or added to the TabLayout directly.
        /*
        if (actionBar != null) {
            for (int i = 0; i < mTabsPagerAdapter.getCount(); i++) {
                actionBar.addTab(
                        actionBar.newTab()
                                .setText(mTabsPagerAdapter.getPageTitle(i))
                                .setTabListener(this));
            }
        }
        */
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mEditMode) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.edit, menu);

            MenuItem cancel = menu.findItem(R.id.action_cancel);
            cancel.setTitle(cancel.getTitle().toString().toUpperCase());
        } else {
            getMenuInflater().inflate(R.menu.secret_viewer, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home || itemId == R.id.action_cancel) {
            this.setResult(RESULT_CANCELED);
            this.finish();
            return true;
        } else if (itemId == R.id.action_done) {
            writeSecret();
            this.setResult(RESULT_OK);
            this.finish();
            return true;
        } else if (itemId == R.id.action_delete_secret) {
            deleteSecret();
            return true;
        } else if (itemId == R.id.action_edit_secret) {
            setEditMode(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setEditMode(boolean editMode) {
        mEditMode = editMode;
        invalidateOptionsMenu(); // Corrected API call
        if (mEditorFragment != null) { // Check if fragment exists
            mEditorFragment.setEditMode(mEditMode);
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
        // also if in edit mode, change menus between tabs
        if (mEditMode) {
            invalidateOptionsMenu(); // Corrected API call
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // if going into the pictures, we need to save the secret first
        if (mEditMode && tab.getPosition() == 0) {
            // force thumbnails loading only once
            boolean setSecretId = mSecretId == -1;

            // only write if brand new secret
            if (setSecretId && !this.writeSecret()) {
                Toast.makeText(this, R.string.cannotAddPictures, Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class TabsPagerAdapter extends FragmentPagerAdapter {

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT); // Added behavior for modern FragmentPagerAdapter
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                if (mEditorFragment == null) {
                    String title = null;
                    String body = null;
                    if (mSecretId != -1 && mAdapter != null) { // Check mAdapter for null
                        Cursor c = mAdapter.getSecret(mSecretId);
                        if (c != null) { // Check cursor for null
                             if (c.moveToFirst()) { // Ensure cursor has data
                                int titleColIndex = c.getColumnIndex(DataAdapter.Secret.TITLE);
                                int bodyColIndex = c.getColumnIndex(DataAdapter.Secret.BODY);
                                if (titleColIndex != -1) {
                                    String encryptedTitle = c.getString(titleColIndex);
                                    title = mAdapter.decrypt(encryptedTitle);
                                }
                                if (bodyColIndex != -1) {
                                    String encryptedBody = c.getString(bodyColIndex);
                                    body = mAdapter.decrypt(encryptedBody);
                                }
                            }
                            c.close();
                        }
                    }
                    mEditorFragment = new SecretEditorFragment(title, body, mEditMode);
                }
                return mEditorFragment;
            }

            if (mPicturesFragment == null) {
                mPicturesFragment = new SecretEditorPicturesFragment();
            }
            return mPicturesFragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.secret).toUpperCase(l);
                case 1:
                    return getString(R.string.pictures).toUpperCase(l);
            }
            return null;
        }
    }

    // writes secret and returns true if success
    private boolean writeSecret() {
        if (mEditorFragment == null) {
            return false;
        }

        String title = mEditorFragment.getTitle();
        String body = mEditorFragment.getBody();

        // don't do nothing for empty title
        if (!TextUtils.isEmpty(title) && mAdapter != null) {
            if (mSecretId == -1) {
                // insert new
                mSecretId = mAdapter.createSecret(mCompartmentId, title, body);
            } else {
                // update existing
                mAdapter.updateSecret(mSecretId, title, body);
            }
            return true;
        }
        return false;
    }

    private void deleteSecret() {
        if (mAdapter == null) return; // Guard against null adapter
        // get secret title
        Cursor cursor = mAdapter.getSecret(mSecretId);
        String secretTitle = "";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int titleColIndex = cursor.getColumnIndex(DataAdapter.Secret.TITLE);
                if (titleColIndex != -1) {
                    String encryptedTitle = cursor.getString(titleColIndex);
                    secretTitle = mAdapter.decrypt(encryptedTitle);
                }
            }
            cursor.close();
        }

        // this is only defined so the anonymous OnClickHandler
        // can see it
        final long secretToDelete = mSecretId;

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdapter != null) { // Check adapter again in case it was nullified
                    mAdapter.deleteSecret(secretToDelete);
                    Toast.makeText(SecretEditorActivity.this, R.string.secretDeleted,
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }
        };
        
        CharSequence message;
        String alertMessageString = this.getString(R.string.deleteSecret, TextUtils.htmlEncode(secretTitle));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            message = Html.fromHtml(alertMessageString, Html.FROM_HTML_MODE_LEGACY);
        } else {
            message = Html.fromHtml(alertMessageString);
        }

        Utilities.showYesNoAlertDialog(this,
                this.getString(R.string.delete) + "?",
                message,
                onClickListener,
                null
        );
    }
}
