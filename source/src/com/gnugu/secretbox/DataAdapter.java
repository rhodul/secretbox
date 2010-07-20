/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */
package com.gnugu.secretbox;

import java.io.File;
import java.io.IOException;

import javax.crypto.BadPaddingException;

import org.apache.commons.codec.binary.Base64;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Environment;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * Provides access to secrets.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public final class DataAdapter {
	/**
	 * Compartment table.
	 */
	public static final class Compartment implements BaseColumns {
		private static final String TABLE = "compartment";
		/**
		 * Compartment name.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";

		/**
		 * Number of secrets in compartment.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String SECRET_COUNT = "secretCount";

		// we glue the select together here
		private static final String SELECT_BODY = _ID + ", " + NAME
		+ ", (select count(" + Secret._ID + ") from " + Secret.TABLE
		+ " where " + Secret.TABLE + "." + Secret.COMPARTMENT_ID
		+ " = " + TABLE + "." + _ID + ") as " + SECRET_COUNT + " from "
		+ TABLE;

		// select all compartments
		private static final String SELECT_ALL = "select " + SELECT_BODY;

		// select only one record
		private static final String SELECT_ONE = "select distinct "
			+ SELECT_BODY
			+ " where " + _ID + " = ?";
	}

	/**
	 * Secret table.
	 */
	public static final class Secret implements BaseColumns {
		private static final String TABLE = "secret";
		private static final String COMPARTMENT_ID = "compartment_id";
		/**
		 * Secret title.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String TITLE = "title";
		/**
		 * Secret body.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String BODY = "body";
	}

	/**
	 * Variable table.
	 */
	private static final class Variable implements BaseColumns {
		private static final String TABLE = "variable";
		/**
		 * Variable name.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";
		/**
		 * Variable value.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String VALUE = "value";
		/**
		 * Salt variable.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String VARIABLE_SALT = "salt";
		/**
		 * Key variable.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String VARIABLE_KEY = "key";
	}

	/**
	 * Interface definition for a callback to be invoked when the brand new
	 * database is being created.
	 * 
	 * @author HERA Consulting Ltd.
	 * 
	 */
	public abstract static interface OnNewDatabaseListener {
		/**
		 * Called when a new password for encryption is required.
		 * 
		 * @return Password for data encryption.
		 */
		public abstract String onGetPassword();
	}

	/**
	 * Interface definition for a callback to be invoked when user needs to
	 * login.
	 * 
	 * @author HERA Consulting Ltd.
	 * 
	 */
	public abstract static interface OnLoginListener {
		/**
		 * Called when user needs to login.
		 * 
		 * @return Password for data encryption.
		 */
		public abstract String onGetPassword();
	}

	/**
	 * Path where the file with secrets is stored.
	 */
	public static final File FILE_PATH;

	/**
	 * File where the secrets are stored.
	 */
	public static final String FILE_NAME = "secretbox.db";

	/**
	 * Current database version.
	 */
	public static final int DB_VERSION = 1;

	private OnNewDatabaseListener _onNewDatabaseListener;
	private OnLoginListener _onLoginListener;

	private SQLiteDatabase _db;
	private Crypto _crypto;

	static {
		FILE_PATH = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "secretbox");
	}

	/**
	 * Instantiates an object of this class. Will call either OnNewFileListener
	 * or OnOpenFileListener.
	 * 
	 * @param onNewDatabaseListener
	 *            Callback to be invoked when the brand new database is being
	 *            created.
	 * @param onRetrieveEncryptionKeyListener
	 *            Callback to be invoked when user needs to login.
	 * @throws IOException
	 * @throws BadPaddingException
	 */
	public DataAdapter(OnNewDatabaseListener onNewDatabaseListener,
			OnLoginListener onLoginListener) throws IOException,
			BadPaddingException {

		if (onNewDatabaseListener == null || onLoginListener == null) {
			throw new NullPointerException(
					"onNewDatabaseListener and onLoginListener cannot be null.");
		}

		_onNewDatabaseListener = onNewDatabaseListener;
		_onLoginListener = onLoginListener;

		File file;
		String storageState = Environment.getExternalStorageState();
		if (storageState.equals(Environment.MEDIA_MOUNTED)) {
			// ensure the path
			FILE_PATH.mkdirs();
			file = new File(DataAdapter.FILE_PATH, DataAdapter.FILE_NAME);
		} else {
			throw new IOException("Media card is not ready.");
		}

		this.openDatabase(file);
	}

	private void openDatabase(File file) throws BadPaddingException {
		// check if the file exists
		boolean isBranNewDb = !file.exists();
		String pwd = null;
		
		// prompt for new pwd to base encryption on
		if (isBranNewDb) {
			pwd = _onNewDatabaseListener.onGetPassword();
			if (TextUtils.isEmpty(pwd)) {
				throw new IllegalArgumentException(
						"pwd cannot be null or empty.");
			}
		}

		try {
			_db = SQLiteDatabase.openOrCreateDatabase(file, null);
		} catch (Exception e) {
			Log.e(this.getClass().getName(), Log.getStackTraceString(e));
			throw new RuntimeException(e);
		}

		this.ensureDbVersion(_db);

		if (isBranNewDb) {
			// we need to make new encryption key
			this.createNewEncryptionKey(pwd);
		} else {
			// get the key from DB
			this.retrieveEncryptionKey();
		}
	}

	private void ensureDbVersion(SQLiteDatabase db) {
		int version = db.getVersion();
		if (version != DB_VERSION) {
			db.beginTransaction();
			try {
				if (version == 0) {
					this.createDatabase(db);
				} else {
					this.upgradeDatabase(db, version);
				}
				db.setVersion(DB_VERSION);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}
	}

	private void createDatabase(SQLiteDatabase db) {
		db.execSQL("create table " + Variable.TABLE + " (" + Variable.NAME
				+ " text primary key, " + Variable.VALUE + " text not null);");

		db.execSQL("create table " + Compartment.TABLE + " (" + Compartment._ID
				+ " integer primary key autoincrement, " + Compartment.NAME
				+ " text not null);");

		db.execSQL("create table " + Secret.TABLE + " (" + Secret._ID
				+ " integer primary key autoincrement, "
				+ Secret.COMPARTMENT_ID + " integer references "
				+ Compartment.TABLE + " (" + Compartment._ID
				+ ") on delete cascade, " + Secret.TITLE + " text not null, "
				+ Secret.BODY + " text not null);");
	}

	// this will upgrade database
	private void upgradeDatabase(SQLiteDatabase db, int oldVersion) {
		// nothing to do in this version
	}

	/**
	 * Changes the encryption password.
	 * 
	 * @param oldPassword
	 *            Password for existing encryption.
	 * @param newPassword
	 *            Password for new encryption.
	 * @throws BadPaddingException
	 */
	public void setNewEncryptionPassword(String oldPassword, String newPassword)
			throws BadPaddingException {
		// get the key from DB
		// using old pwd
		byte[] theKey = this.getKey(oldPassword);

		// store newly encrypted key in DB
		this.saveEncryptionKey(theKey, newPassword);
	}

	// gets the variable from variable table in DB
	private byte[] getVariable(String variable) {
		String[] columns = { Variable.VALUE };
		Cursor cursor = null;
		try {
			cursor = _db.query(true, Variable.TABLE, columns, Variable.NAME
					+ "='" + variable + "'", null, null, null, null, null);
			if (!cursor.moveToFirst()) {
				this.ThrowSQLiteDatabaseCorruptException();
			}
			return Base64.decodeBase64(cursor.getString(
					cursor.getColumnIndex(Variable.VALUE)).getBytes());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void ThrowSQLiteDatabaseCorruptException() {
		throw new SQLiteDatabaseCorruptException("Database corrupted.");
	}

	// gets the encryption key from DB
	private byte[] getKey(String password) throws BadPaddingException {
		// instantiate local crypto with pwd and the salt from DB
		Crypto crypto = new Crypto(password, this
				.getVariable(Variable.VARIABLE_SALT));

		// decrypt the key stored in DB
		return crypto.decrypt(this.getVariable(Variable.VARIABLE_KEY));
	}

	private void retrieveEncryptionKey() throws BadPaddingException {
		// ask user for pwd
		String pwd = _onLoginListener.onGetPassword();
		if (TextUtils.isEmpty(pwd)) {
			throw new IllegalArgumentException("pwd cannot be null or empty.");
		}

		// get the key from DB
		byte[] theKey = this.getKey(pwd);

		// instantiate _crypto with the key from DB
		_crypto = new Crypto(theKey);
	}

	// this should only be called once when brand new DB
	// is created
	private void createNewEncryptionKey(String pwd) {
		// we will encrypt theKey (generated below) with another key based on
		// user's pwd and store it encrypted in DB;
		// when the user decides to change the password all we need
		// to re-encrypt is theKey which will be simply done by calling this
		// method

		// generate random key
		byte[] theKey = Crypto.generateKey();

		// we can now instantiate Crypto with this key;
		// random key is the one used to encrypt/decrypt
		// data
		_crypto = new Crypto(theKey);

		// we will now encrypt theKey with another key based on user's pwd
		// and store it encrypted in DB;
		// when the user decides to change the password all we need
		// to re-encrypt is theKey which will be simply done by calling this
		// method.
		this.saveEncryptionKey(theKey, pwd);
	}

	// encrypts and saves the key in DB
	private void saveEncryptionKey(byte[] key, String password) {
		// we need new salt
		byte[] salt = Crypto.generateSalt();

		// and a new crypto to encrypt the key
		Crypto crypto = new Crypto(password, salt);

		// encrypt
		byte[] encryptedKey = crypto.encrypt(key);

		// save the key in DB
		String base64 = new String(Base64.encodeBase64(encryptedKey));
		ContentValues initialValues = new ContentValues();
		initialValues.put(Variable.NAME, Variable.VARIABLE_KEY);
		initialValues.put(Variable.VALUE, base64);
		_db.replace(Variable.TABLE, null, initialValues);

		// save salt in DB
		base64 = new String(Base64.encodeBase64(salt));
		initialValues = new ContentValues();
		initialValues.put(Variable.NAME, Variable.VARIABLE_SALT);
		initialValues.put(Variable.VALUE, base64);
		_db.replace(Variable.TABLE, null, initialValues);
	}

	/**
	 * Decrypts data.
	 * 
	 * @param data
	 *            string to decrypt.
	 * @return Human readable string.
	 */
	public String decrypt(String data) {
		try {
			return _crypto
					.decryptToString(Base64.decodeBase64(data.getBytes()));
		} catch (BadPaddingException e) {
			Log.e(this.getClass().getName(), Log.getStackTraceString(e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates new compartment.
	 * 
	 * @param name
	 *            Compartment name.
	 * @return Compartment rowId.
	 */
	public long createCompartment(String name) {
		if (TextUtils.isEmpty(name)) {
			throw new IllegalArgumentException("name cannot be null or empty.");
		}

		ContentValues values = new ContentValues();
		values.put(Compartment.NAME, new String(Base64.encodeBase64(_crypto
				.encrypt(name))));

		return _db.insert(Compartment.TABLE, null, values);
	}

	/**
	 * Deletes compartment.
	 * 
	 * @param rowId
	 *            Compartment id to delete.
	 * @return true if deleted, false otherwise.
	 */
	public boolean deleteCompartment(long rowId) {
		return _db.delete(Compartment.TABLE, Compartment._ID + "=" + rowId,
				null) > 0;
	}

	/**
	 * Updates compartment.
	 * 
	 * @param rowId
	 *            Compartment to update.
	 * @param name
	 *            New name.
	 * @return true if updated, false otherwise.
	 */
	public boolean updateCompartment(long rowId, String name) {
		if (TextUtils.isEmpty(name)) {
			throw new IllegalArgumentException("name cannot be null or empty.");
		}

		ContentValues values = new ContentValues();
		values.put(Compartment.NAME, new String(Base64.encodeBase64(_crypto
				.encrypt(name))));

		return _db.update(Compartment.TABLE, values, Compartment._ID + "="
				+ rowId, null) > 0;
	}

	/**
	 * Retrieves all compartments.
	 * 
	 * @return Cursor with compartments.
	 */
	public Cursor getCompartments() {
		return _db.rawQuery(Compartment.SELECT_ALL, null);
	}

	/**
	 * Retrieves individual compartment.
	 *  
	 * @param rowId Id of the compartment to get.
	 * @return Cursor with single compartment in it.
	 */
    public Cursor getCompartment(long rowId) {
    	Cursor cursor = _db.rawQuery(Compartment.SELECT_ONE, new String[] {String.valueOf(rowId)});
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

	/**
	 * Creates new secret.
	 * 
	 * @param compartmentId
	 *            Id of the compartment this secret belongs to.
	 * @param title
	 *            Secret title.
	 * @param body
	 *            Secret body.
	 * @return Secret rowId.
	 */
	public long createSecret(long compartmentId, String title, String body) {
		if (TextUtils.isEmpty(title)) {
			throw new IllegalArgumentException("title cannot be null or empty.");
		}
		// alow empty body
		if (TextUtils.isEmpty(body)) {
			body = "";
		}

		ContentValues values = new ContentValues();
		values.put(Secret.COMPARTMENT_ID, compartmentId);
		values.put(Secret.TITLE, new String(Base64.encodeBase64(_crypto
				.encrypt(title))));
		values.put(Secret.BODY, new String(Base64.encodeBase64(_crypto
				.encrypt(body))));

		return _db.insert(Secret.TABLE, null, values);
	}

	/**
	 * Deletes secret.
	 * 
	 * @param rowId
	 *            Secret id to delete.
	 * @return true if deleted, false otherwise.
	 */
	public boolean deleteSecret(long rowId) {
		return _db.delete(Secret.TABLE, Secret._ID + "=" + rowId, null) > 0;
	}

	/**
	 * Updates secret.
	 * 
	 * @param rowId
	 *            Secret to update.
	 * @param title
	 *            New title.
	 * @param body
	 *            New body.
	 * @return true if updated, false otherwise.
	 */
	public boolean updateSecret(long rowId, String title, String body) {
		if (TextUtils.isEmpty(title)) {
			throw new IllegalArgumentException("title cannot be null or empty.");
		}
		// alow empty body
		if (TextUtils.isEmpty(body)) {
			body = "";
		}

		ContentValues values = new ContentValues();
		values.put(Secret.TITLE, new String(Base64.encodeBase64(_crypto
				.encrypt(title))));
		values.put(Secret.BODY, new String(Base64.encodeBase64(_crypto
				.encrypt(body))));

		return _db.update(Secret.TABLE, values, Secret._ID + "=" + rowId, null) > 0;
	}

	/**
	 * Retrieves all secrets.
	 * 
	 * @param compartmentId
	 *            Id of the compartment from which the secrets will be
	 *            retrieved.
	 * @return Cursor with secrets.
	 */
	public Cursor getSecrets(long compartmentId) {
		return _db
				.query(Secret.TABLE, new String[] { Secret._ID, Secret.TITLE,
						Secret.BODY }, Secret.COMPARTMENT_ID + "=?",
						new String[] { Long.toString(compartmentId) }, null,
						null, null);
	}

	/**
	 * Retrieves individual secret. 
	 * @param rowId Id of the secret to get.
	 * @return Cursor with single secret in it.
	 */
    public Cursor getSecret(long rowId) {
    	Cursor cursor = _db.query(Secret.TABLE, new String[] { Secret._ID, Secret.TITLE,
				Secret.BODY }, Secret._ID + "=?",
				new String[] { Long.toString(rowId) }, null,
				null, null);
    	
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }
}
