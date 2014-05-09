package com.gnugu.secretbox;

import java.util.Locale;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class SecretEditorActivity extends ActionBarActivity implements ActionBar.TabListener {

    public static final String ARG_COMPARTMENT_ID = "compartment_id";
    public static final String ARG_SECRET_ID = "secret_id";
    public static final String ARG_EDIT_MODE = "edit_mode";


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
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
        mAdapter = application.getDataAdapter(this);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mTabsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mTabsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mTabsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
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
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel:
                this.setResult(RESULT_CANCELED);
                this.finish();
                return true;
            case R.id.action_done:
                writeSecret();
                this.setResult(RESULT_OK);
                this.finish();
                return true;
            case R.id.action_delete_secret:
                deleteSecret();
                return true;
            case R.id.action_edit_secret:
                setEditMode(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setEditMode(boolean editMode) {
        mEditMode = editMode;
        supportInvalidateOptionsMenu();
        mEditorFragment.setEditMode(mEditMode);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
        // also if in edit mode, change menus between tabs
        if (mEditMode) {
            supportInvalidateOptionsMenu();
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
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                if (mEditorFragment == null) {
                    String title = null;
                    String body = null;
                    if (mSecretId != -1) {
                        Cursor c = mAdapter.getSecret(mSecretId);
                        String encryptedTitle = c.getString(c
                                .getColumnIndex(DataAdapter.Secret.TITLE));
                        String encryptedBody = c.getString(c
                                .getColumnIndex(DataAdapter.Secret.BODY));
                        c.close();
                        title = mAdapter.decrypt(encryptedTitle);
                        body = mAdapter.decrypt(encryptedBody);
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
        // get secret title
        Cursor cursor = mAdapter.getSecret(mSecretId);
        String encryptedTitle = cursor.getString(cursor
                .getColumnIndex(DataAdapter.Secret.TITLE));
        cursor.close();
        String secretTitle = mAdapter.decrypt(encryptedTitle);

        // this is only defined so the anonymous OnClickHandler
        // can see it
        final long secretToDelete = mSecretId;

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapter.deleteSecret(secretToDelete);

                Toast.makeText(SecretEditorActivity.this, R.string.secretDeleted,
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        };

        Utilities.showYesNoAlertDialog(this,
                this.getString(R.string.delete) + "?",
                Html.fromHtml(this.getString(R.string.deleteSecret, TextUtils
                        .htmlEncode(secretTitle))),
                onClickListener,
                null
        );
    }
}
