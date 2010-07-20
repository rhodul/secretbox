/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;


/**
 * Defines entire application.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public final class Application extends android.app.Application {
	private DataAdapter _adapter = null;
	
	/**
	 * Sets the data adapter for all Activities to enjoy.
	 * @param adapter DataAdapter.
	 */
	public void setDataAdapter(DataAdapter adapter) {
		/*
		 * Although Google suggests that we don't use
		 * getters and setters for performance reasons
		 * I have proved here AGAIN that having a public
		 * field IS pain in the ass. I was not able to find
		 * out what is setting it to null!
		 */
		_adapter = adapter;
	}
	
	/**
	 * Gets the data adapter for all Activities.
	 * @return DataAdapter or null.
	 */
	public DataAdapter getDataAdapter() {
		/*
		 * Although Google suggests that we don't use
		 * getters and setters for performance reasons
		 * I have proved here AGAIN that having a public
		 * field IS pain in the ass. I was not able to find
		 * out what is setting it to null!
		 */
		return _adapter;
	}

}
