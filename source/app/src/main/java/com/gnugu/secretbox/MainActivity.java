package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        SecretsFragment.SecretsFragmentCallbacks {

    /**
     * Activities may choose to wait for the activity result.
     * This is the activity code.
     */
    public static final int ACTIVITY_EDIT_COMPARTMENT = 100;
    // this is multipurpose activity that will handle new/view/edit
    public static final int ACTIVITY_SECRET_DETAIL = 101;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private SecretsFragment mSecretsFragment = null;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mCompartmentName = null;
    private long mCompartmentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setIcon(R.drawable.ic_action_sbox);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onDestroy() {
        // after user exits the activity
        // we want him to login when he comes back
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
        } else if (mSecretsFragment != null) {
            transaction.remove(mSecretsFragment);
        }
        transaction.commit();
    }



    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mCompartmentName == null ? getTitle(): mCompartmentName);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // hide or show edit menus based on selected item
        menu.setGroupVisible(R.id.compartment_menu, mCompartmentId != -1);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_delete_compartment:
                deleteCompartment();
                return true;
            case R.id.action_edit_compartment:
                Intent intent = new Intent(this, EditActivity.class);
                intent.putExtra(EditActivity.ARG_TEXT, mCompartmentName);
                startActivityForResult(intent, ACTIVITY_EDIT_COMPARTMENT);
                return true;
            case R.id.action_new_secret:
                startEditorActivity(true, -1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case NewSecretBoxActivity.ACTIVITY_NEW_SECRET_BOX:
            case LoginActivity.ACTIVITY_LOGIN:
                // check if OK was pressed
                if (resultCode == Activity.RESULT_OK) {
                    mNavigationDrawerFragment.fillData();
                } else {
                    // user didn't create new db, let's get out of here
                    this.finish();
                }
                break;
            case ACTIVITY_EDIT_COMPARTMENT:
                if (resultCode == Activity.RESULT_OK) {
                    updateCompartment(data.getStringExtra(EditActivity.ARG_TEXT));
                }
            case ACTIVITY_SECRET_DETAIL:
                if (resultCode == Activity.RESULT_OK) {
                    mNavigationDrawerFragment.fillData();
                    mSecretsFragment.fillData();
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateCompartment(String newName) {
        DataAdapter adapter = ((Application)getApplication()).getDataAdapter(this);
        adapter.updateCompartment(mCompartmentId, newName );
        mCompartmentName = newName;
        restoreActionBar();
        mNavigationDrawerFragment.fillData();
    }

    private void deleteCompartment() {
        DataAdapter adapter = ((Application)getApplication()).getDataAdapter(this);
        // check if the compartment is empty
        Cursor cursor = adapter.getCompartment(mCompartmentId);
        int secretCount = cursor.getInt(cursor
                .getColumnIndex(DataAdapter.Compartment.SECRET_COUNT));
        cursor.close();

        if (secretCount > 0) {
            // inform user
            Utilities.showAlertDialog(this,
                    getString(R.string.cannotDeleteCopmartment),
                    null);
        } else {
            // delete
            adapter.deleteCompartment(mCompartmentId);
            // let's make a toast
            Toast.makeText(this, R.string.compartmentDeleted,
                    Toast.LENGTH_SHORT).show();
            // and refresh
            mNavigationDrawerFragment.fillData();
            onNavigationDrawerItemSelected(-1, null);

            mNavigationDrawerFragment.openDrawer();
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
