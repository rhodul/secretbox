/**
 * Copyright � 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import java.io.IOException;
import android.support.v4.app.FragmentActivity;
import android.content.Intent;
import android.view.View;

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
		/*
		 * Although Google suggests that we don't use getters and setters for
		 * performance reasons I have proved here AGAIN that having a public
		 * field IS pain in the ass. I was not able to find out what is setting
		 * it to null!
		 */
		mAdapter = adapter;
	}

	/**
	 * Gets the data adapter for all Activities.
     *
     * @param parentActivity Needs handler to parent activity to terminate it
     *                       if the PWD is not OK.
	 * 
	 * @return DataAdapter or null.
	 */
	public DataAdapter getDataAdapter(FragmentActivity parentActivity) {
		/*
		 * Although Google suggests that we don't use getters and setters for
		 * performance reasons I have proved here AGAIN that having a public
		 * field IS pain in the ass. I was not able to find out what is setting
		 * it to null!
		 */

        if (mAdapter == null) {
            createDataAdapter(parentActivity);
        }
		return mAdapter;
	}

	/**
	 * Destroys DataAdapter. CompartmentList activity from onDestroy.
	 */
	public void destroyDataAdapter() {
		if (mAdapter != null) {
			mAdapter.close();
			mAdapter = null;
		}
	}

	/**
	 * Creates the DataAdapter in a separate thread.
	 */
	private void createDataAdapter(final FragmentActivity parentActivity) {
		try {
			boolean databaseExists = DataAdapter.databaseExists();
			if (!databaseExists) {
                parentActivity.startActivityForResult(new Intent(parentActivity,
                        NewSecretBoxActivity.class), NewSecretBoxActivity.ACTIVITY_NEW_SECRET_BOX);
			} else {
                parentActivity.startActivityForResult(new Intent(parentActivity,
                        LoginActivity.class), LoginActivity.ACTIVITY_LOGIN);
			}
		} catch (IOException e) {
			Utilities.showNoMediaCardDialog(parentActivity, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    parentActivity.finish();
                }
            });
		}
	}

}
