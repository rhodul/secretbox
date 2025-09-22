package com.gnugu.secretbox;

import java.util.Locale;

import android.content.Intent;
import android.database.Cursor;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
// Removed unnecessary imports like FragmentTransaction, ViewPager, etc.
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class SecretEditorActivity extends AppCompatActivity {

    public static final String ARG_COMPARTMENT_ID = "compartment_id";
    public static final String ARG_SECRET_ID = "secret_id";
    public static final String ARG_EDIT_MODE = "edit_mode";

    private SecretEditorFragment mEditorFragment;

    private boolean mEditMode = false; // Default, will be updated in onCreate
    private long mCompartmentId;
    private long mSecretId;
    private DataAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_editor);

        Toolbar toolbar = findViewById(R.id.toolbar_secret_editor);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        mCompartmentId = intent.getLongExtra(ARG_COMPARTMENT_ID, -1);
        mSecretId = intent.getLongExtra(ARG_SECRET_ID, -1);

        // Determine initial mEditMode for the Activity
        if (mSecretId == -1) { // New secret: activity and fragment start in edit mode
            mEditMode = true;
        } else { // Existing secret: activity mode from intent, fragment mode will match
            mEditMode = intent.getBooleanExtra(ARG_EDIT_MODE, false);
        }

        // Set Toolbar icon and title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setIcon(R.drawable.ic_action_sbox); // Set icon here
            if (mSecretId == -1) { // New Secret title
                getSupportActionBar().setTitle(R.string.insert);
            } else if (mEditMode) { // Edit Existing Secret title
                getSupportActionBar().setTitle(R.string.edit);
            } else { // View Existing Secret title
                getSupportActionBar().setTitle(R.string.secret);
            }
        }

        Application application = (Application) getApplication();
        mAdapter = application.getDataAdapter(this);

        if (savedInstanceState == null) {
            String title = null;
            String body = null;
            if (mSecretId != -1 && mAdapter != null) { // Load data only for existing secrets
                Cursor c = mAdapter.getSecret(mSecretId);
                if (c != null) {
                    if (c.moveToFirst()) {
                        int titleColIndex = c.getColumnIndex(DataAdapter.Secret.TITLE);
                        int bodyColIndex = c.getColumnIndex(DataAdapter.Secret.BODY);
                        if (titleColIndex != -1) {
                            String encryptedTitle = c.getString(titleColIndex);
                            title = mAdapter.decrypt(encryptedTitle);
                        }
                        if (bodyColIndex != -1) {
                            String encryptedBody = c.getString(bodyColIndex);
                            body = mAdapter.decrypt(encryptedBody);
                        }
                    }
                    c.close();
                }
            }
            // Pass the determined mEditMode to the fragment
            mEditorFragment = new SecretEditorFragment(title, body, mEditMode);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.secret_editor_fragment_container, mEditorFragment)
                    .commit();
        } else {
            mEditorFragment = (SecretEditorFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.secret_editor_fragment_container);
            // Ensure fragment's edit mode is consistent with activity's mEditMode after restoration
            if (mEditorFragment != null) {
                mEditorFragment.setEditMode(mEditMode);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear(); // Clear previous menu items
        if (mEditMode) { // Covers New Secret (mSecretId == -1 implies mEditMode = true) and Edit Existing Secret
            getMenuInflater().inflate(R.menu.edit, menu); // Has Done, Cancel, Delete
            MenuItem cancelItem = menu.findItem(R.id.action_cancel);
            if (cancelItem != null) {
                cancelItem.setTitle(cancelItem.getTitle().toString().toUpperCase(Locale.getDefault()));
            }
            MenuItem deleteItem = menu.findItem(R.id.action_delete_secret);
            if (deleteItem != null) {
                if (mSecretId == -1) { // New, unsaved secret
                    deleteItem.setVisible(false); // Cannot delete a new, unsaved secret
                } else {
                    deleteItem.setVisible(true); // Can delete an existing secret being edited
                }
            }
        } else { // View Existing Secret Mode (mSecretId != -1 && !mEditMode)
            getMenuInflater().inflate(R.menu.secret_viewer, menu); // Has Edit, Delete
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_CANCELED); // Or handle based on unsaved changes if necessary
            finish();
            return true;
        } else if (itemId == R.id.action_cancel) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else if (itemId == R.id.action_done) {
            if (writeSecret()) {
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, R.string.error_title_empty, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (itemId == R.id.action_delete_secret) {
            if (mSecretId != -1) { // Ensure there's an existing secret to delete
                deleteSecret();
            }
            return true;
        } else if (itemId == R.id.action_edit_secret) { // From secret_viewer menu
            setEditMode(true); // Switch to edit mode for the current secret
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This method is called to switch an EXISTING secret between view and edit modes
    private void setEditMode(boolean newModeState) {
        if (mSecretId == -1 && !newModeState) {
            // Cannot switch a new secret to "view" mode before it's saved.
            // This scenario should ideally not occur with current logic.
            return;
        }

        mEditMode = newModeState;
        if (mEditorFragment != null) {
            mEditorFragment.setEditMode(mEditMode);
        }

        if (getSupportActionBar() != null) {
            // Title for existing secrets changes based on mEditMode
            if (mSecretId != -1) { // Only change title if it's an existing secret
                if (mEditMode) {
                    getSupportActionBar().setTitle(R.string.edit);
                } else {
                    getSupportActionBar().setTitle(R.string.secret);
                }
            }
            // For new secrets (mSecretId == -1), title is set in onCreate and doesn't change via setEditMode.
        }
        invalidateOptionsMenu(); // Re-create the menu for the new mode
    }

    private boolean writeSecret() {
        if (mEditorFragment == null) {
            return false;
        }
        String title = mEditorFragment.getTitle();
        String body = mEditorFragment.getBody();

        if (!TextUtils.isEmpty(title) && mAdapter != null) {
            if (mSecretId == -1) { // New secret
                mSecretId = mAdapter.createSecret(mCompartmentId, title, body);
                // After saving a new secret, it's now an "existing" secret.
                // It should arguably switch to "View" mode or stay in "Edit" mode.
                // Current behavior: finishes activity. If it stayed, mEditMode might need an update.
            } else { // Update existing
                mAdapter.updateSecret(mSecretId, title, body);
            }
            return true;
        }
        return false;
    }

    private void deleteSecret() {
        if (mAdapter == null || mSecretId == -1) return;

        Cursor cursor = mAdapter.getSecret(mSecretId);
        String secretTitle = "";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int titleColIndex = cursor.getColumnIndex(DataAdapter.Secret.TITLE);
                if (titleColIndex != -1) {
                    String encryptedTitle = cursor.getString(titleColIndex);
                    secretTitle = mAdapter.decrypt(encryptedTitle);
                }
            }
            cursor.close();
        }

        final long secretToDelete = mSecretId;

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdapter != null) {
                    mAdapter.deleteSecret(secretToDelete);
                    Toast.makeText(SecretEditorActivity.this, R.string.secretDeleted,
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }
        };
        
        CharSequence message;
        String alertMessageString = this.getString(R.string.deleteSecret, TextUtils.htmlEncode(secretTitle));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            message = Html.fromHtml(alertMessageString, Html.FROM_HTML_MODE_LEGACY);
        } else {
            message = Html.fromHtml(alertMessageString);
        }

        Utilities.showYesNoAlertDialog(this,
                this.getString(R.string.delete) + "?",
                message,
                onClickListener,
                null
        );
    }
}
