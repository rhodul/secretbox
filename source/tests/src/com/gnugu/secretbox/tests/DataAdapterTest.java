/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox.tests;

import java.io.File;
import java.util.Formatter;

import javax.crypto.BadPaddingException;

import junit.framework.Assert;
import android.database.Cursor;
import android.os.Environment;
import android.test.AndroidTestCase;
import com.gnugu.secretbox.DataAdapter;
import com.gnugu.secretbox.DataAdapter.Compartment;
import com.gnugu.secretbox.DataAdapter.OnLoginListener;
import com.gnugu.secretbox.DataAdapter.OnNewDatabaseListener;
import com.gnugu.secretbox.DataAdapter.Secret;

/**
 * Test class for DataAdapter.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public class DataAdapterTest extends AndroidTestCase {

	private boolean _wasOnNewFileCallBack = false;
	private boolean _wasOnOpenFileCallBack = false;
	private File _datFile;
	private static final String _pwd = "ferko";

	public static File getDatFile() {
		String storageState = Environment.getExternalStorageState();
		if (storageState.equals(Environment.MEDIA_MOUNTED)) {
			return new File(DataAdapter.FILE_PATH, DataAdapter.FILE_NAME);
		} else {
			throw new RuntimeException("Media state: " + storageState);
		}
	}

	@Override
	protected void setUp() {
		_datFile = DataAdapterTest.getDatFile();
	}

	@Override
	protected void tearDown() {
		_datFile.delete();
	}

	public void testNewFile() throws BadPaddingException {
		DataAdapter adapter = this.createDummyAdapter(_pwd);

		Assert.assertEquals(true, _wasOnNewFileCallBack);
		Assert.assertEquals(false, _wasOnOpenFileCallBack);

		// also make sure the file has been created
		Assert.assertTrue(_datFile.exists());
	}

	public void testExistingFile() throws BadPaddingException {
		// this will create a file
		DataAdapter adapter = this.createDummyAdapter(_pwd);

		// and this will reopen it
		adapter = this.createDummyAdapter(_pwd);

		Assert.assertEquals(false, _wasOnNewFileCallBack);
		Assert.assertEquals(true, _wasOnOpenFileCallBack);
	}

	public void testChangePassword() throws BadPaddingException {
		final String newPwd = "mrkva";

		DataAdapter adapter = this.createDummyAdapter(_pwd);
		adapter.setNewEncryptionPassword(_pwd, newPwd);

		try {
			// this should throw exception because the handlers
			// below will provide old pwd
			adapter = this.createDummyAdapter(_pwd);
			Assert.fail("Expected exception was not thrown.");
		} catch (BadPaddingException ex) {
			// we are OK
			Assert.assertTrue(true);
		}

		// but this should work just fine
		adapter = this.createDummyAdapter(newPwd);

		Assert.assertEquals(false, _wasOnNewFileCallBack);
		Assert.assertEquals(true, _wasOnOpenFileCallBack);
	}

	public void testCompartment() throws BadPaddingException {
		DataAdapter adapter = this.createDummyAdapter(_pwd);
		final String COMPARTMENT_FORMAT = "Compartment number %d.";
		final String UPDATED_TXT = "This has been updated.";
		final int COMPARTMENT_COUNT = 10;
		StringBuilder builder = new StringBuilder();
		Formatter formatter = new Formatter(builder);

		for (int i = 0; i < COMPARTMENT_COUNT; i++) {
			builder.delete(0, builder.length());
			formatter.format(COMPARTMENT_FORMAT, i);
			adapter.createCompartment(builder.toString());
		}

		// let's re-get the adapter
		adapter = this.createDummyAdapter(_pwd);
		Cursor cursor = adapter.getCompartments();
		try {
			Assert.assertEquals(COMPARTMENT_COUNT, cursor.getCount());

			int x = 0;
			while (cursor.moveToNext()) {
				builder.delete(0, builder.length());
				formatter.format(COMPARTMENT_FORMAT, x);
				Assert.assertEquals(builder.toString(), adapter.decrypt(cursor
						.getString(cursor.getColumnIndex(Compartment.NAME))));
				x++;
			}

			// now update the first record
			cursor.moveToFirst();
			adapter.updateCompartment(cursor.getLong(cursor
					.getColumnIndex(Compartment._ID)), UPDATED_TXT);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// reload
		adapter = this.createDummyAdapter(_pwd);
		cursor = adapter.getCompartments();
		try {
			// check if update worked
			cursor.moveToFirst();
			Assert.assertEquals(UPDATED_TXT, adapter.decrypt(cursor
					.getString(cursor.getColumnIndex(Compartment.NAME))));

			// now delete them all
			while (!cursor.isAfterLast()) {
				adapter.deleteCompartment(cursor.getLong(cursor
						.getColumnIndex(Compartment._ID)));
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// reload again
		adapter = this.createDummyAdapter(_pwd);
		cursor = adapter.getCompartments();
		try {
			// should be empty
			Assert.assertEquals(0, cursor.getCount());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testSecret() throws BadPaddingException {
		DataAdapter adapter = this.createDummyAdapter(_pwd);
		final String SECRET_TITLE_FORMAT = "Secret number %d.";
		final String SECRET_BODY_FORMAT = "Cannot tell you what %d is.";
		final String UPDATED_TITLE = "Updated title.";
		final String UPDATED_BODY = "Updated body.";
		final int SECRET_COUNT = 10;
		StringBuilder titleBuilder = new StringBuilder();
		Formatter titleFormatter = new Formatter(titleBuilder);
		StringBuilder bodyBuilder = new StringBuilder();
		Formatter bodyFormatter = new Formatter(bodyBuilder);
		
		long compartmentId = adapter.createCompartment("Compartment");

		for (int i = 0; i < SECRET_COUNT; i++) {
			titleBuilder.delete(0, titleBuilder.length());
			titleFormatter.format(SECRET_TITLE_FORMAT, i);
			bodyBuilder.delete(0, bodyBuilder.length());
			bodyFormatter.format(SECRET_BODY_FORMAT, i);
			adapter.createSecret(compartmentId, titleBuilder.toString(), bodyBuilder.toString());
		}

		// let's re-get the adapter
		adapter = this.createDummyAdapter(_pwd);
		Cursor cursor = adapter.getSecrets(compartmentId);
		try {
			Assert.assertEquals(SECRET_COUNT, cursor.getCount());

			int x = 0;
			while (cursor.moveToNext()) {
				titleBuilder.delete(0, titleBuilder.length());
				titleFormatter.format(SECRET_TITLE_FORMAT, x);
				bodyBuilder.delete(0, bodyBuilder.length());
				bodyFormatter.format(SECRET_BODY_FORMAT, x);
				Assert.assertEquals(titleBuilder.toString(), adapter.decrypt(cursor
						.getString(cursor.getColumnIndex(Secret.TITLE))));
				Assert.assertEquals(bodyBuilder.toString(), adapter.decrypt(cursor
						.getString(cursor.getColumnIndex(Secret.BODY))));
				x++;
			}

			// now update the first record
			cursor.moveToFirst();
			adapter.updateSecret(cursor.getLong(cursor
					.getColumnIndex(Secret._ID)), UPDATED_TITLE, UPDATED_BODY);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// reload
		adapter = this.createDummyAdapter(_pwd);

		// check if compartment tells us number
		// of secrets
		cursor = adapter.getCompartments();
		cursor.moveToFirst();
		try {
			Assert.assertEquals(SECRET_COUNT, cursor.getLong(cursor.getColumnIndex(Compartment.SECRET_COUNT)));
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		// check the secrets
		cursor = adapter.getSecrets(compartmentId);
		try {
			// check if update worked
			cursor.moveToFirst();
			Assert.assertEquals(UPDATED_TITLE, adapter.decrypt(cursor
					.getString(cursor.getColumnIndex(Secret.TITLE))));
			Assert.assertEquals(UPDATED_BODY, adapter.decrypt(cursor
					.getString(cursor.getColumnIndex(Secret.BODY))));

			// now delete them all
			while (!cursor.isAfterLast()) {
				adapter.deleteSecret(cursor.getLong(cursor
						.getColumnIndex(Secret._ID)));
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// reload again
		adapter = this.createDummyAdapter(_pwd);
		cursor = adapter.getSecrets(compartmentId);
		try {
			// should be empty
			Assert.assertEquals(0, cursor.getCount());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public DataAdapter createDummyAdapter(final String password)
			throws BadPaddingException {
		try {
			return new DataAdapter(new OnNewDatabaseListener() {
				@Override
				public String onGetPassword() {
					_wasOnNewFileCallBack = true;
					_wasOnOpenFileCallBack = false;
					return password;
				}
			}, new OnLoginListener() {
				@Override
				public String onGetPassword() {
					_wasOnOpenFileCallBack = true;
					_wasOnNewFileCallBack = false;
					return password;
				}
			});
		} catch (BadPaddingException b) {
			throw b;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
