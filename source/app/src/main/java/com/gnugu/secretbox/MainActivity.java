package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.fragment.app.Fragment; 
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log; 
import android.view.Menu;
import android.view.MenuItem;
import android.view.View; 
import android.view.Window; // For Window.FEATURE_NO_TITLE
import android.widget.Toast;
// Removed: import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        SecretsFragment.SecretsFragmentCallbacks {

    public static final int ACTIVITY_EDIT_COMPARTMENT = 100;
    public static final int ACTIVITY_SECRET_DETAIL = 101;
    private static final String TAG = "MainActivity"; // Logging Tag

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private SecretsFragment mSecretsFragment = null;

    private CharSequence mCompartmentName = null;
    private long mCompartmentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Programmatically ensure no title bar is requested by the window
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.d(TAG, "onCreate started. Requested Window.FEATURE_NO_TITLE.");
        setContentView(R.layout.activity_main);
        Log.d(TAG, "setContentView completed");

        // Log all fragments known to the FragmentManager
        Log.d(TAG, "Listing fragments in FragmentManager:");
        if (getSupportFragmentManager().getFragments().isEmpty()) {
            Log.d(TAG, "FragmentManager has no fragments.");
        } else {
            for (Fragment f : getSupportFragmentManager().getFragments()) {
                if (f != null) {
                    Log.d(TAG, "Found fragment in manager: " + f.getClass().getName() +
                            " with ID: " + Integer.toHexString(f.getId()) + // Log ID as hex
                            " (R.id.navigation_drawer is " + Integer.toHexString(R.id.navigation_drawer) + ")" +
                            " and tag: " + f.getTag());
                } else {
                    Log.d(TAG, "Found a null fragment in manager list");
                }
            }
        }
        
        // Check if the view for the fragment ID exists
        View fragmentXmlView = findViewById(R.id.navigation_drawer);
        if (fragmentXmlView != null) {
            Log.d(TAG, "findViewById(R.id.navigation_drawer) returned a View: " + fragmentXmlView.getClass().getName());
        } else {
            Log.d(TAG, "findViewById(R.id.navigation_drawer) returned NULL for the View");
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        Log.d(TAG, "Toolbar found: " + (toolbar != null));
        setSupportActionBar(toolbar);
        Log.d(TAG, "setSupportActionBar called");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.drawable.ic_action_sbox);
            Log.d(TAG, "SupportActionBar icon set");
        }
        // === DIAGNOSTIC CODE REMOVED ===

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        Log.d(TAG, "mNavigationDrawerFragment is null after findFragmentById: " + (mNavigationDrawerFragment == null));

        if (mNavigationDrawerFragment == null) {
            Log.e(TAG, "FATAL: mNavigationDrawerFragment is null before calling setUp. This will cause a crash.");
            // Let it crash as per original behavior to see if logs provide clues
        }
        
        // Set up the drawer, passing the toolbar
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                toolbar); 
        Log.d(TAG, "mNavigationDrawerFragment.setUp called");
    }

    @Override
    public void onDestroy() {
        ((Application) this.getApplication()).destroyDataAdapter();
        super.onDestroy();
    }

    @Override
    public void onNavigationDrawerItemSelected(long compartmentId, String compartmentName) {
        mCompartmentId = compartmentId;
        mCompartmentName = compartmentName;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (mCompartmentId != -1) {
            mSecretsFragment = SecretsFragment.newInstance(compartmentId, compartmentName);
            transaction.replace(R.id.container, mSecretsFragment);
        } else { // compartmentId is -1
            if (mSecretsFragment != null) {
                transaction.remove(mSecretsFragment);
                mSecretsFragment = null; // Clear the reference
            }
            // Optionally, ensure the drawer is open to prompt user action
            if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
                mNavigationDrawerFragment.openDrawer();
            }
        }
        transaction.commit();
        // Important: Call restoreActionBar here as well to update the title
        restoreActionBar(); 
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mCompartmentName == null ? getTitle(): mCompartmentName);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.setGroupVisible(R.id.compartment_menu, mCompartmentId != -1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        } else if (id == R.id.action_delete_compartment) {
            deleteCompartment();
            return true;
        } else if (id == R.id.action_edit_compartment) {
            Intent intent = new Intent(this, EditActivity.class);
            intent.putExtra(EditActivity.ARG_TEXT, mCompartmentName);
            startActivityForResult(intent, ACTIVITY_EDIT_COMPARTMENT);
            return true;
        } else if (id == R.id.action_new_secret) {
            startEditorActivity(true, -1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case NewSecretBoxActivity.ACTIVITY_NEW_SECRET_BOX:
            case LoginActivity.ACTIVITY_LOGIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (mNavigationDrawerFragment != null) {
                        mNavigationDrawerFragment.fillData(); // This should trigger onNavigationDrawerItemSelected
                    }
                } else {
                    this.finish();
                }
                break;
            case ACTIVITY_EDIT_COMPARTMENT:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    updateCompartment(data.getStringExtra(EditActivity.ARG_TEXT));
                }
                break;
            case ACTIVITY_SECRET_DETAIL:
                if (resultCode == Activity.RESULT_OK) {
                    if (mNavigationDrawerFragment != null) {
                        mNavigationDrawerFragment.fillData(); // This should trigger onNavigationDrawerItemSelected
                    }
                    if (mSecretsFragment != null) {
                        mSecretsFragment.fillData();
                    }
                }
                break;
        }
    }

    private void updateCompartment(String newName) {
        DataAdapter adapter = ((Application)getApplication()).getDataAdapter(this);
        if (adapter != null) {
            adapter.updateCompartment(mCompartmentId, newName );
        }
        mCompartmentName = newName;
        restoreActionBar();
        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.fillData();
        }
    }

    private void deleteCompartment() {
        DataAdapter adapter = ((Application)getApplication()).getDataAdapter(this);
        if (adapter == null) return;
        Cursor cursor = adapter.getCompartment(mCompartmentId);
        if (cursor == null) return;
        int secretCount = 0;
        if (cursor.moveToFirst()) {
            int secretCountColumnIndex = cursor.getColumnIndex(DataAdapter.Compartment.SECRET_COUNT);
            if (secretCountColumnIndex != -1) {
                 secretCount = cursor.getInt(secretCountColumnIndex);
            }
        }
        cursor.close();
        if (secretCount > 0) {
            Utilities.showAlertDialog(this,
                    getString(R.string.cannotDeleteCopmartment),
                    null);
        } else {
            adapter.deleteCompartment(mCompartmentId);
            Toast.makeText(this, R.string.compartmentDeleted,
                    Toast.LENGTH_SHORT).show();
            if (mNavigationDrawerFragment != null) {
                mNavigationDrawerFragment.fillData();
                // onNavigationDrawerItemSelected will be called by fillData's callback
                // and will handle opening the drawer if mCompartmentId is -1
            }
        }
    }

    @Override
    public void onSecretSelected(long secretId) {
        startEditorActivity(false, secretId);
    }

    private void startEditorActivity(boolean editMode, long secretId) {
        Intent newSecretIntent = new Intent(this, SecretEditorActivity.class);
        newSecretIntent.putExtra(SecretEditorActivity.ARG_EDIT_MODE, editMode);
        newSecretIntent.putExtra(SecretEditorActivity.ARG_COMPARTMENT_ID, mCompartmentId);
        newSecretIntent.putExtra(SecretEditorActivity.ARG_SECRET_ID, secretId);
        startActivityForResult(newSecretIntent, ACTIVITY_SECRET_DETAIL);
    }
}
