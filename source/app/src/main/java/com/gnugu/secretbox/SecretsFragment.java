package com.gnugu.secretbox;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.Formatter;

/**
 * Created by rhodul on 2014-04-29.
 */
public class SecretsFragment extends Fragment

{
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_COMPARTMENT_ID = "compartment_id";
    private static final String ARG_COMPARTMENT_NAME = "compartment_name";

    /**
     * Custom ViewBinder for SecretList.
     *
     * @author HERA Consulting Ltd.
     *
     */
    private final class SecretViewBinder implements SimpleCursorAdapter.ViewBinder {

        public SecretViewBinder() {
        }


        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            String columnName = cursor.getColumnName(columnIndex);

            if (columnName.equals(DataAdapter.Secret.TITLE)) {
                TextView secretTitle = (TextView) view
                        .findViewById(R.id.line1);
                secretTitle.setText(mAdapter.decrypt(cursor
                        .getString(columnIndex)));

                return true;
            }
            return false;
        }

    }


    private View mRoot;
    private DataAdapter mAdapter;
    private ListView mSecretsListView;
    private long mComopartmentId;
    private SecretsFragmentCallbacks mCallbacks;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SecretsFragment newInstance(long compartmentId, String compartmentName) {
        SecretsFragment fragment = new SecretsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_COMPARTMENT_ID, compartmentId);
        args.putString(ARG_COMPARTMENT_NAME, compartmentName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getAdapter();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (SecretsFragmentCallbacks) activity;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_secrets, container, false);

        mSecretsListView = (ListView) mRoot.findViewById(R.id.list);
        mSecretsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        mComopartmentId = getArguments().getLong(ARG_COMPARTMENT_ID);

        // setup the ListView adapter (will be populated in fillData())
        // map cursor fields to list item
        // (_ID is there for the picture count)
        String[] from = new String[] { DataAdapter.Secret.TITLE };
        int[] to = new int[] { R.id.line1 };
        SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this.getActivity(),
                R.layout.list_item_simple, null, from, to);
        // set our custom view binder
        cursorAdapter.setViewBinder(new SecretViewBinder());
        mSecretsListView.setAdapter(cursorAdapter);

        fillData();
        return mRoot;
    }

    private void getAdapter() {
        MainActivity activity = (MainActivity) this.getActivity();
        Application application = (Application) activity.getApplication();
        mAdapter = application.getDataAdapter(activity);
    }

    public void fillData() {
        if (mAdapter == null) {
            getAdapter();
        }
        // if still nothing it means we fired up Login activity, so let our mother
        // activity to get back to us
        if (mAdapter == null) {
            return;
        }

        SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) mSecretsListView.getAdapter();

        // get old cursor
        Cursor cursor = cursorAdapter.getCursor();

        if (cursor != null) {
            // if cursor is there simply re-query
            cursor.requery();
        } else {
            // need for new cursor
            cursor = mAdapter.getSecrets(mComopartmentId);
            this.getActivity().startManagingCursor(cursor);
            cursorAdapter.changeCursor(cursor);
        }

        // make the "no items" text visible
        if (cursor.getCount() == 0) {
            mSecretsListView.setVisibility(View.GONE);
            mRoot.findViewById(R.id.nothing).setVisibility(View.VISIBLE);
        } else {
            mSecretsListView.setVisibility(View.VISIBLE);
            mRoot.findViewById(R.id.nothing).setVisibility(View.GONE);
        }
    }

    private void selectItem(int position) {
        if (mSecretsListView != null) {
            mSecretsListView.setItemChecked(position, true);
            if (mCallbacks != null) {
                // get compartment id and name
                if (mSecretsListView != null) {
                    SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) mSecretsListView.getAdapter();
                    Cursor cursor = cursorAdapter.getCursor();
                    if (cursor != null) {
                        cursor.moveToPosition(position);
                        long id = cursor.getLong(cursor.getColumnIndex(DataAdapter.Secret._ID));
                        mCallbacks.onSecretSelected(id);
                    }
                }
            }
        }
    }


    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface SecretsFragmentCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onSecretSelected(long secretId);
    }

}
