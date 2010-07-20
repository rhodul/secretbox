/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox.tests;

import java.io.File;
import android.test.ApplicationTestCase;
import com.gnugu.secretbox.Application;

/**
 * @author HERA Consulting Ltd.
 * 
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
	/**
	 * @param applicationClass
	 */
	public ApplicationTest() {
		super(Application.class);
	}

	private File _datFile;

	@Override
	protected void setUp() throws Exception {
		_datFile = DataAdapterTest.getDatFile();

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		_datFile.delete();

		super.tearDown();
	}

	public void testNewDatabase() {
		 if (_datFile.exists()) {
		 _datFile.delete();
		 }
		 this.createApplication();
	}
}
