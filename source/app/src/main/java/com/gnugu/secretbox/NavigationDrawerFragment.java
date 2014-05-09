package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Formatter;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {
    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Custom ViewBinder for CompartmentList.
     *
     * @author HERA Consulting Ltd.
     *
     */
    private final class CompartmentViewBinder implements SimpleCursorAdapter.ViewBinder {
        private final StringBuilder mSecretCountBuilder = new StringBuilder();
        private final Formatter mSecretCountFormatter = new Formatter(
                mSecretCountBuilder);
        private final String mSecretCountFormat;

        public CompartmentViewBinder() {
            mSecretCountFormat = getString(R.string.secretCount);
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            String columnName = cursor.getColumnName(columnIndex);

            if (columnName.equals(DataAdapter.Compartment.NAME)) {
                TextView secretCount = (TextView) view
                        .findViewById(R.id.line1);
                secretCount.setText(mAdapter.decrypt(cursor
                        .getString(columnIndex)));
                return true;
            } else if (columnName.equals(DataAdapter.Compartment.SECRET_COUNT)) {
                mSecretCountBuilder.delete(0, mSecretCountBuilder.length());
                mSecretCountFormatter.format(mSecretCountFormat, cursor
                        .getInt(columnIndex));
                TextView secretCount = (TextView) view
                        .findViewById(R.id.line2);
                secretCount.setText(mSecretCountBuilder.toString());
                return true;
            }

            return false;
        }

    }


    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mCompartmentListView;
    private View mFragmentContainerView;
    private View mRoot;
    private DataAdapter mAdapter;
    private EditText mNewCompartmentName;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }

        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedPosition);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);

        getAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        mCompartmentListView = (ListView) mRoot.findViewById(R.id.list);
        mCompartmentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        // setup the ListView adapter (will be populated in fillData())
        // map cursor fields to list item
        String[] from = new String[] { DataAdapter.Compartment.NAME,
                DataAdapter.Compartment.SECRET_COUNT };
        int[] to = new int[] { R.id.line1, R.id.line2 };
        SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this.getActivity(),
                R.layout.list_item, null, from, to);
        // set our custom view binder
        cursorAdapter.setViewBinder(new CompartmentViewBinder());
        mCompartmentListView.setAdapter(cursorAdapter);


        mCompartmentListView.setItemChecked(mCurrentSelectedPosition, true);

        mNewCompartmentName = (EditText) mRoot.findViewById(R.id.new_compartment_name);

        ImageButton add = (ImageButton) mRoot.findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createCompartment();
            }
        });

        return mRoot;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.openDrawer(mFragmentContainerView);

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mCompartmentListView != null) {
            mCompartmentListView.setItemChecked(position, true);
            if (mCallbacks != null) {
                // get compartment id and name
                if (mCompartmentListView != null) {
                    SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) mCompartmentListView.getAdapter();
                    Cursor cursor = cursorAdapter.getCursor();
                    if (cursor != null) {
                        cursor.moveToPosition(position);
                        long id = cursor.getLong(cursor.getColumnIndex(DataAdapter.Compartment._ID));
                        String name = cursor.getString(cursor.getColumnIndex(DataAdapter.Compartment.NAME));
                        mCallbacks.onNavigationDrawerItemSelected(id, mAdapter.decrypt(name));
                    }
                }
            }
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the drawer app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.drawer, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.action_change_password) {
            startActivity(new Intent(this.getActivity(), ChangePassword.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getAdapter() {
        MainActivity activity = (MainActivity) this.getActivity();
        Application application = (Application) activity.getApplication();
        mAdapter = application.getDataAdapter(activity);
    }

    public void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }
    }

    public void fillData() {
        if (mAdapter == null) {
            getAdapter();
        }

        SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) mCompartmentListView.getAdapter();

        // get old cursor
        Cursor cursor = cursorAdapter.getCursor();

        if (cursor != null) {
            // if cursor is there simply re-query
            cursor.requery();
        } else {
            // need for new cursor
            cursor = mAdapter.getCompartments();
            this.getActivity().startManagingCursor(cursor);
            cursorAdapter.changeCursor(cursor);
        }

        // make the "no items" text visible
        if (cursor.getCount() == 0) {
            mCompartmentListView.setVisibility(View.GONE);
            mRoot.findViewById(R.id.nothing).setVisibility(View.VISIBLE);
        } else {
            mCompartmentListView.setVisibility(View.VISIBLE);
            mRoot.findViewById(R.id.nothing).setVisibility(View.GONE);
        }
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the drawer app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.appName);
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    private void createCompartment() {
        if (mNewCompartmentName.getText().length() == 0) {
            return;
        }

        mAdapter.createCompartment(mNewCompartmentName.getText().toString());
        mNewCompartmentName.setText(null);
        Toast.makeText(this.getActivity(), R.string.compartmentCreated,
                Toast.LENGTH_SHORT).show();

        // new compartment, so refresh data
        this.fillData();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(long compartmentId, String compartmentName);
    }
}
