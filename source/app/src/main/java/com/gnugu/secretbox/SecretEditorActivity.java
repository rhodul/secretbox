package com.gnugu.secretbox;

import java.util.Locale;

import android.content.Intent;
import android.database.Cursor;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Added for Toolbar
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
// androidx.fragment.app.FragmentPagerAdapter; // Removed
// import androidx.viewpager.widget.ViewPager; // Removed
// import androidx.appcompat.app.ActionBar; // No longer needed for tabs
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

// Removed: implements ActionBar.TabListener
public class SecretEditorActivity extends AppCompatActivity {

    public static final String ARG_COMPARTMENT_ID = "compartment_id";
    public static final String ARG_SECRET_ID = "secret_id";
    public static final String ARG_EDIT_MODE = "edit_mode";

    private SecretEditorFragment mEditorFragment;

    private boolean mEditMode = false;
    private long mCompartmentId;
    private long mSecretId;
    private DataAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_editor);

        Toolbar toolbar = findViewById(R.id.toolbar_secret_editor);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Optionally set title based on edit mode
            // if (mEditMode) { // mEditMode is not yet initialized here
            //    getSupportActionBar().setTitle(R.string.title_activity_secret_editor_edit);
            // } else {
            //    getSupportActionBar().setTitle(R.string.title_activity_secret_editor_new);
            // }
        }

        Intent intent = getIntent();
        mEditMode = intent.getBooleanExtra(ARG_EDIT_MODE, false);
        mCompartmentId = intent.getLongExtra(ARG_COMPARTMENT_ID, -1);
        mSecretId = intent.getLongExtra(ARG_SECRET_ID, -1);

        // Set title after mEditMode is initialized
        if (getSupportActionBar() != null) {
            if (mEditMode) {
                getSupportActionBar().setTitle(R.string.edit);
            } else {
                getSupportActionBar().setTitle(R.string.insert);
            }
        }

        Application application = (Application) getApplication();
        mAdapter = application.getDataAdapter(this);

        if (savedInstanceState == null) {
            String title = null;
            String body = null;
            if (mSecretId != -1 && mAdapter != null) {
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
            mEditorFragment = new SecretEditorFragment(title, body, mEditMode);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.secret_editor_fragment_container, mEditorFragment)
                    .commit();
        } else {
            mEditorFragment = (SecretEditorFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.secret_editor_fragment_container);
            if (mEditorFragment != null) {
                mEditorFragment.setEditMode(mEditMode); // Ensure mode is set on restored fragment
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Always inflate the 'edit' menu which should contain 'Done'
        // For 'new' mode, 'Cancel' might be handled by Up button or you might remove it.
        // For 'edit' mode, 'Cancel' and 'Delete' are relevant.
        getMenuInflater().inflate(R.menu.edit, menu);
        MenuItem cancelItem = menu.findItem(R.id.action_cancel);
        MenuItem deleteItem = menu.findItem(R.id.action_delete_secret);

        if (mEditMode) {
            if (cancelItem != null) {
                cancelItem.setTitle(cancelItem.getTitle().toString().toUpperCase());
                cancelItem.setVisible(true);
            }
            if (deleteItem != null) {
                deleteItem.setVisible(true); // Show delete only in edit mode for an existing secret
            }
        } else {
            // In 'new' mode (not mEditMode)
            if (cancelItem != null) {
                cancelItem.setVisible(false); // Hide 'Cancel' if Up button is preferred
            }
            if (deleteItem != null) {
                deleteItem.setVisible(false); // Hide 'Delete' for a new secret
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_CANCELED);
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
            if (mSecretId != -1) { // Ensure there's a secret to delete
                deleteSecret();
            }
            return true;
        } else if (itemId == R.id.action_edit_secret) {
            // This logic might be redundant if the activity always loads the fragment in the correct mode.
            // If you intend to switch an already displayed secret to edit mode from a viewer mode (not implemented here):
            setEditMode(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setEditMode(boolean editMode) {
        mEditMode = editMode;
        invalidateOptionsMenu(); // Re-create options menu to show/hide items
        if (mEditorFragment != null) {
            mEditorFragment.setEditMode(mEditMode);
        }
        if (getSupportActionBar() != null) {
            if (mEditMode) {
                getSupportActionBar().setTitle(R.string.edit);
            } else {
                getSupportActionBar().setTitle(R.string.insert);
            }
        }
    }

    private boolean writeSecret() {
        if (mEditorFragment == null) {
            return false;
        }

        String title = mEditorFragment.getTitle();
        String body = mEditorFragment.getBody();

        if (!TextUtils.isEmpty(title) && mAdapter != null) {
            if (mSecretId == -1) {
                mSecretId = mAdapter.createSecret(mCompartmentId, title, body);
            } else {
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
