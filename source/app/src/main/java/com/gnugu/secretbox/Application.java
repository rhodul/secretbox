/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import java.io.IOException;
import androidx.fragment.app.FragmentActivity;
import android.content.Intent;
import android.view.View;
import android.util.Log;

/**
 * Defines entire application.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public final class Application extends android.app.Application {

	private DataAdapter mAdapter = null;
	
	/**
	 * Called when the application is stopping.
	 */
	@Override
	public void onTerminate() {
		this.destroyDataAdapter();
		super.onTerminate();
	}

	/**
	 * Sets the data adapter for all Activities to enjoy.
	 * 
	 * @param adapter
	 *            DataAdapter.
	 */
	void setDataAdapter(DataAdapter adapter) {
		mAdapter = adapter;
	}

	/**
	 * Gets the data adapter for all Activities.
     * If the adapter doesn't exist, this method will attempt to initiate
     * the creation sequence by starting the appropriate Activity.
     *
     * @param parentActivity Needs handler to parent activity to start other activities
     *                       (NewSecretBoxActivity or LoginActivity) if the adapter isn't initialized.
	 * 
	 * @return DataAdapter or null (if initialization is pending via a new Activity).
	 */
	public DataAdapter getDataAdapter(FragmentActivity parentActivity) {
        if (mAdapter == null) {
            // This will start NewSecretBoxActivity or LoginActivity if needed.
            // It does not synchronously create and return the adapter.
            // The adapter will be set later by setDataAdapter() once the setup activity completes.
            Log.d("Application", "mAdapter is null, calling createDataAdapter.");
            createDataAdapter(parentActivity);
        }
		return mAdapter; // This might still be null after createDataAdapter returns.
	}

	/**
	 * Destroys DataAdapter. Called from MainActivity.onDestroy typically.
	 */
	public void destroyDataAdapter() {
		if (mAdapter != null) {
			mAdapter.close();
			mAdapter = null;
            Log.d("Application", "DataAdapter destroyed.");
		}
	}

	/**
	 * Initiates the sequence to make a DataAdapter available by starting
     * NewSecretBoxActivity (if no DB) or LoginActivity (if DB exists).
     * This method itself does not create the DataAdapter instance.
	 */
	private void createDataAdapter(final FragmentActivity parentActivity) {
		try {
            Log.d("Application", "createDataAdapter called. Checking if database exists.");
            // Pass parentActivity (which is a Context) to databaseExists
			boolean databaseExists = DataAdapter.databaseExists(parentActivity);
			if (!databaseExists) {
                Log.d("Application", "Database does NOT exist. Starting NewSecretBoxActivity.");
                parentActivity.startActivityForResult(new Intent(parentActivity,
                        NewSecretBoxActivity.class), NewSecretBoxActivity.ACTIVITY_NEW_SECRET_BOX);
			} else {
                Log.d("Application", "Database EXISTS. Starting LoginActivity.");
                parentActivity.startActivityForResult(new Intent(parentActivity,
                        LoginActivity.class), LoginActivity.ACTIVITY_LOGIN);
			}
		} catch (IOException e) {
            Log.e("Application", "IOException in createDataAdapter: " + e.getMessage(), e);
			Utilities.showNoMediaCardDialog(parentActivity, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    parentActivity.finish();
                }
            });
		}
	}

}
