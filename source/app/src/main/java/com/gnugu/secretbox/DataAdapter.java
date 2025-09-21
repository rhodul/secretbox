/**
 * Copyright 2009 HERA Consulting Ltd.
 */
package com.gnugu.secretbox;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;

import org.apache.commons.codec.binary.Base64;

import android.content.ContentValues;
import android.content.Context; // Added Context import
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
    private static final String DATABASE_NAME = "secretbox.db";
    private static final String SECRETS_SUBDIR = "secrets"; // For picture attachments, within app-specific external files

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
                + SELECT_BODY + " where " + _ID + " = ?";
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
    public static final class Variable implements BaseColumns {
        private static final String TABLE = "variable";
        /**
         * Variable name.
         * <P>
         * Type: TEXT
         * </P>
         */
        private static final String NAME = "name";
        /**
         * Variable value.
         * <P>
         * Type: TEXT
         * </P>
         */
        private static final String VALUE = "value";
        /**
         * Salt variable.
         * <P>
         * Type: TEXT
         * </P>
         */
        private static final String VARIABLE_SALT = "salt";
        /**
         * Key variable.
         * <P>
         * Type: TEXT
         * </P>
         */
        private static final String VARIABLE_KEY = "key";

        /**
         * Variable that indicates which app version news were the last to be
         * forcet to the user.
         * <P>
         * Type: TEXT
         * </P>
         */
        private static final String VARIABLE_LAST_NEWS = "lastNews";

        /**
         * Value that indicates that news were never
         * forced on the user.
         * <P>
         * Type: INT
         * </P>
         */
        public static final int VALUE_LAST_NEWS_NEVER = -1;
    }

    // Removed static DATABASE_FILE_PATH and DATABASE_FILE.
    // They will now be determined using Context.

    /**
     * Current database version.
     */
    public static final int DB_VERSION = 1;

    private SQLiteDatabase _db;
    private Crypto _crypto;
    private Context _context; // Added Context field
    private File _databaseFile; // Added File field for the database path

    /**
     * Instantiates an object of this class.
     *
     * @param context Context for accessing app-specific storage.
     * @param password Password to unlock the encryption.
     * @throws IOException
     * @throws BadPaddingException
     */
    public DataAdapter(Context context, String password) throws IOException, BadPaddingException, InvalidKeyException {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        this._context = context.getApplicationContext(); // Use application context

        if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException(
                    "password cannot be null or empty.");
        }
        
        // Determine database path using app-specific external storage
        File dbDir = this._context.getExternalFilesDir(null);
        if (dbDir == null) {
            // This can happen if external storage is not mounted or some other edge case
            throw new IOException("External app-specific directory not available.");
        }
        if (!dbDir.exists()) {
            dbDir.mkdirs(); // Ensure the directory exists
        }
        this._databaseFile = new File(dbDir, DATABASE_NAME);

        // do we have media card? (Still a good check for external storage availability)
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new IOException("Media card is not ready.");
        }

        // do we have DB?
        // Pass context to the static databaseExists method
        if (!DataAdapter.databaseExists(this._context)) {
            throw new IOException("Database doesn\'t exist at: " + this._databaseFile.getAbsolutePath());
        }

        this.openDatabaseAndCrypto(password);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            this.close();
        } finally {
            super.finalize();
        }
    }

    private void openDatabaseAndCrypto(String password) throws BadPaddingException, InvalidKeyException {
        try {
            // Use the instance field _databaseFile
            _db = SQLiteDatabase.openOrCreateDatabase(this._databaseFile, null);
        } catch (Exception e) {
            Log.e(this.getClass().getName(), Log.getStackTraceString(e));
            throw new RuntimeException(e);
        }

        DataAdapter.ensureDbVersion(_db);
        this.createCrypto(password);
    }

    /**
     * Closes the adapter.
     */
    public void close() {
        if (_db != null && _db.isOpen()) {
            _db.close();
        }
        _db = null;
        _crypto = null;
    }

    private static void ensureDbVersion(SQLiteDatabase db) {
        int version = db.getVersion();
        if (version != DB_VERSION) {
            db.beginTransaction();
            try {
                if (version == 0) {
                    DataAdapter.createDatabaseSchema(db);
                } else {
                    // For now, only supports upgrading from version 0.
                    // If oldVersion > DB_VERSION, this would be a downgrade, which SQLiteOpenHelper handles.
                    // If 0 < oldVersion < DB_VERSION, upgradeDatabaseSchema should handle it.
                     DataAdapter.upgradeDatabaseSchema(db, version);
                }
                db.setVersion(DB_VERSION);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private static void createDatabaseSchema(SQLiteDatabase db) {
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
    private static void upgradeDatabaseSchema(SQLiteDatabase db, int oldVersion) {
        // Handle database schema upgrades here if DB_VERSION is ever increased.
        // For example, if oldVersion is 1 and new DB_VERSION is 2:
        // if (oldVersion < 2) {
        //     db.execSQL("ALTER TABLE " + SomeTable.TABLE + " ADD COLUMN new_column TEXT;");
        // }
        // if (oldVersion < 3) {
        //     // upgrades for version 3
        // }
        // For now, this app doesn't have upgrades beyond initial creation.
        Log.w(DataAdapter.class.getName(), "Upgrading database from version " + oldVersion + " to " + DB_VERSION + ", but no upgrade path is defined.");
        // If there's no specific upgrade path, and oldVersion > 0,
        // you might choose to drop tables and recreate, or throw an exception if data loss is unacceptable.
        // For this scenario, if ensureDbVersion is called with oldVersion != 0 and oldVersion != DB_VERSION,
        // it implies an unexpected state. However, SQLiteOpenHelper typically handles the direct downgrade error.
        // Our ensureDbVersion logic is a simplified version.
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
        DataAdapter.saveEncryptionKey(theKey, newPassword, _db);
    }

    // gets the variable from variable table in DB
    private byte[] getVariable(String variable) {
        String[] columns = { Variable.VALUE };
        Cursor cursor = null;
        try {
            cursor = _db.query(true, Variable.TABLE, columns, Variable.NAME
                    + "=\'" + variable + "\'", null, null, null, null, null);
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

    private void setStringVariable(String variable, String value) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(Variable.NAME, variable);
        initialValues.put(Variable.VALUE, value);
        _db.replace(Variable.TABLE, null, initialValues);
    }

    private String getStringVariable(String variable) {
        String[] columns = { Variable.VALUE };
        Cursor cursor = null;
        try {
            cursor = _db.query(true, Variable.TABLE, columns, Variable.NAME
                    + "=\'" + variable + "\'", null, null, null, null, null);
            if (!cursor.moveToFirst()) {
                return "";
            }
            return cursor.getString(cursor.getColumnIndex(Variable.VALUE));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets the version of application when the last news was imposed on the
     * user.
     */
    public int getLastNewsVersion() {
        String value = this.getStringVariable(Variable.VARIABLE_LAST_NEWS);

        if (TextUtils.isEmpty(value)) {
            return Variable.VALUE_LAST_NEWS_NEVER;
        } else {
            return Integer.parseInt(value);
        }
    }

    /**
     * Sets the version of application when the last news was imposed on the
     * user.
     */
    public void setLastNewsVersion(int version) {
        this.setStringVariable(Variable.VARIABLE_LAST_NEWS, String.valueOf(version));
    }

    private void ThrowSQLiteDatabaseCorruptException() {
        throw new SQLiteDatabaseCorruptException("Database corrupted.");
    }

    // gets the encryption key from DB
    private byte[] getKey(String password)
            throws BadPaddingException {
        // instantiate local crypto with pwd and the salt from DB
        Crypto crypto = new Crypto(password, this.getVariable(
                Variable.VARIABLE_SALT));

        // decrypt the key stored in DB
        return crypto.decrypt(this.getVariable(Variable.VARIABLE_KEY));
    }

    private void createCrypto(String pwd) throws BadPaddingException, InvalidKeyException {
        // ask user for pwd
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
    private static void createNewEncryptionKey(String pwd, SQLiteDatabase db) {
        byte[] theKey = Crypto.generateKey();
        DataAdapter.saveEncryptionKey(theKey, pwd, db);
    }

    // encrypts and saves the key in DB
    private static void saveEncryptionKey(byte[] key, String password,
            SQLiteDatabase db) {
        byte[] salt = Crypto.generateSalt();
        Crypto crypto = new Crypto(password, salt);
        byte[] encryptedKey = crypto.encrypt(key);

        String base64 = new String(Base64.encodeBase64(encryptedKey));
        ContentValues initialValues = new ContentValues();
        initialValues.put(Variable.NAME, Variable.VARIABLE_KEY);
        initialValues.put(Variable.VALUE, base64);
        db.replace(Variable.TABLE, null, initialValues);

        base64 = new String(Base64.encodeBase64(salt));
        initialValues = new ContentValues();
        initialValues.put(Variable.NAME, Variable.VARIABLE_SALT);
        initialValues.put(Variable.VALUE, base64);
        db.replace(Variable.TABLE, null, initialValues);
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
     * @param rowId
     *            Id of the compartment to get.
     * @return Cursor with single compartment in it.
     */
    public Cursor getCompartment(long rowId) {
        Cursor cursor = _db.rawQuery(Compartment.SELECT_ONE,
                new String[] { String.valueOf(rowId) });
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
        return _db.update(Secret.TABLE, values, Secret._ID + "=" + rowId,
                null) > 0;
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
        return _db.query(Secret.TABLE, new String[] { Secret._ID,
                Secret.TITLE, Secret.BODY }, Secret.COMPARTMENT_ID + "=?",
                new String[] { Long.toString(compartmentId) }, null, null,
                null);
    }

    /**
     * Retrieves individual secret.
     *
     * @param rowId
     *            Id of the secret to get.
     * @return Cursor with single secret in it.
     */
    public Cursor getSecret(long rowId) {
        Cursor cursor = _db.query(Secret.TABLE, new String[] { Secret._ID,
                Secret.TITLE, Secret.BODY }, Secret._ID + "=?",
                new String[] { Long.toString(rowId) }, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * Gets the directory where the files for secret (e.g., pictures) are stored.
     * These are stored in a subdirectory within the app's external files directory.
     *
     * @param context Context to resolve the path.
     * @param secretRowId Row id of the secret the picture will belong to.
     * @return File object for the secret's attachment directory.
     * @throws IOException If the external storage is not available.
     */
    public static File getSecretFileDirectory(Context context, long secretRowId) throws IOException {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null for getSecretFileDirectory.");
        }
        File baseDir = context.getExternalFilesDir(SECRETS_SUBDIR); // Store in a "secrets" subdirectory
        if (baseDir == null) {
            throw new IOException("External app-specific directory for secrets not available.");
        }
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        File secretDir = new File(baseDir, String.valueOf(secretRowId));
        if (!secretDir.exists()) {
            secretDir.mkdirs();
        }
        return secretDir;
    }


    /**
     * Checks if database exists in app-specific external storage.
     *
     * @param context Context to resolve the path.
     * @return True if database exists; otherwise false.
     * @throws IOException If media card is not ready.
     */
    public static boolean databaseExists(Context context) throws IOException {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null for databaseExists.");
        }
        final String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            File dbDir = context.getExternalFilesDir(null);
            if (dbDir == null) {
                 // This implies external storage is mounted but getExternalFilesDir returned null,
                 // which is highly unlikely but theoretically possible.
                throw new IOException("External app-specific directory is null despite media being mounted.");
            }
            // Ensure the base directory for the database exists
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            File databaseFile = new File(dbDir, DATABASE_NAME);
            return databaseFile.exists();
        } else {
            throw new IOException("Media card is not ready.");
        }
    }

    /**
     * Creates new database if it already doesn't exist in app-specific external storage.
     *
     * @param context Context to resolve the path.
     * @param password Encryption password.
     * @throws IOException
     */
    public static void createDatabase(Context context, String password) throws IOException {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null for createDatabase.");
        }
        if (!DataAdapter.databaseExists(context)) {
            if (TextUtils.isEmpty(password)) {
                throw new IllegalArgumentException(
                        "password cannot be null or empty.");
            }

            SQLiteDatabase db = null;
            try {
                File dbDir = context.getExternalFilesDir(null);
                if (dbDir == null) {
                     throw new IOException("External app-specific directory not available for database creation.");
                }
                if (!dbDir.exists()) {
                    dbDir.mkdirs();
                }
                File databaseFile = new File(dbDir, DATABASE_NAME);

                db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
                DataAdapter.ensureDbVersion(db);
                DataAdapter.createNewEncryptionKey(password, db);
            } catch (Exception e) {
                Log.e(DataAdapter.class.getName(), Log.getStackTraceString(e));
                throw new RuntimeException("Failed to create database: " + e.getMessage(), e);
            } finally {
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }
        }
    }
}
