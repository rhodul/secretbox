package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Added Toolbar import
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
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        getAdapter();
        if (mFromSavedInstanceState || mCurrentSelectedPosition != 0) {
             selectItem(mCurrentSelectedPosition);
        }
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

        String[] from = new String[] { DataAdapter.Compartment.NAME,
                DataAdapter.Compartment.SECRET_COUNT };
        int[] to = new int[] { R.id.line1, R.id.line2 };
        SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(requireActivity(),
                R.layout.list_item, null, from, to, 0);
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
     * @param toolbar      The Toolbar from the host Activity.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout, Toolbar toolbar) { // Added toolbar parameter
        mFragmentContainerView = requireActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                requireActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                toolbar,                          /* Toolbar */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }
                requireActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }
                requireActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mCompartmentListView != null) {
            mCompartmentListView.setItemChecked(position, true);
            if (mCallbacks != null) {
                if (mCompartmentListView.getAdapter() instanceof SimpleCursorAdapter) {
                    SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) mCompartmentListView.getAdapter();
                    Cursor cursor = cursorAdapter.getCursor();
                    if (cursor != null && cursor.moveToPosition(position)) {
                        int idColumnIndex = cursor.getColumnIndex(DataAdapter.Compartment._ID);
                        int nameColumnIndex = cursor.getColumnIndex(DataAdapter.Compartment.NAME);
                        if (idColumnIndex != -1 && nameColumnIndex != -1) {
                            long id = cursor.getLong(idColumnIndex);
                            String name = cursor.getString(nameColumnIndex);
                            mCallbacks.onNavigationDrawerItemSelected(id, mAdapter.decrypt(name));
                        } else {
                            // Log error or handle missing columns
                        }
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
            throw new ClassCastException(activity.toString()
                    + " must implement NavigationDrawerCallbacks");
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
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    private void getAdapter() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            Application application = (Application) activity.getApplication();
            mAdapter = application.getDataAdapter(activity);
        }
    }

    public void openDrawer() {
        if (mDrawerLayout != null && mFragmentContainerView != null) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }
    }

    public void fillData() {
        if (mAdapter == null) {
            getAdapter();
        }
        if (mAdapter == null) {
            return;
        }

        if (mCompartmentListView.getAdapter() instanceof SimpleCursorAdapter) {
            SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) mCompartmentListView.getAdapter();
            Cursor newCursor = mAdapter.getCompartments();
            cursorAdapter.changeCursor(newCursor);

            if (newCursor == null || newCursor.getCount() == 0) {
                mCompartmentListView.setVisibility(View.GONE);
                mRoot.findViewById(R.id.nothing).setVisibility(View.VISIBLE);
            } else {
                mCompartmentListView.setVisibility(View.VISIBLE);
                mRoot.findViewById(R.id.nothing).setVisibility(View.GONE);
            }
        }
    }

    private void showGlobalContextActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.appName);
        }
    }

    private ActionBar getSupportActionBar() {
        if (getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        }
        return null;
    }

    private void createCompartment() {
        if (mNewCompartmentName.getText().length() == 0) {
            return;
        }
        if (mAdapter != null) {
             mAdapter.createCompartment(mNewCompartmentName.getText().toString());
             mNewCompartmentName.setText(null);
             Toast.makeText(requireActivity(), R.string.compartmentCreated,
                Toast.LENGTH_SHORT).show();
             this.fillData();
        } else {
            // Handle case where mAdapter is null, maybe show a Toast
            Toast.makeText(requireActivity(), "Error: Adapter not initialized", Toast.LENGTH_SHORT).show();
        }
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
