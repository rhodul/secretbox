package com.gnugu.secretbox;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Added import
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText; // TextInputEditText is a subclass of EditText

import java.util.Locale; // Added import

public class EditActivity extends AppCompatActivity {
    public static final String ARG_TEXT = "text";

    private String mOriginalText;
    private EditText mText; // This is actually a TextInputEditText from the layout
    private TextWatcher mTextWatcher;
    private MenuItem mOk;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        Toolbar toolbar = findViewById(R.id.toolbar_edit_activity);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setIcon(R.drawable.ic_action_sbox);
            getSupportActionBar().setTitle(R.string.edit); // "Edit"
        }

        mOriginalText = getIntent().getStringExtra(ARG_TEXT);
        mText = findViewById(R.id.text); // No need to cast if using EditText as base type
        mText.setText(mOriginalText);
        // mText.setHint(mOriginalText); // Hint is now in TextInputLayout in XML

        mTextWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int s, int b, int c) {
                if (mOk != null) { // Check mOk for null before using
                    mOk.setEnabled(mText.getText().length() > 0);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) { }
            @Override
            public void beforeTextChanged(CharSequence cs, int i, int j, int k) { }
        };
        mText.addTextChangedListener(mTextWatcher);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu); // edit.xml now includes delete

        mOk = menu.findItem(R.id.action_done);
        if (mText != null && mOk != null) { // Ensure mText is also not null
             mOk.setEnabled(mText.getText().length() > 0);
        }

        MenuItem cancel = menu.findItem(R.id.action_cancel);
        if (cancel != null) { // Check if item exists
            cancel.setTitle(cancel.getTitle().toString().toUpperCase(Locale.getDefault()));
        }
        
        // Hide delete action for compartments, as it's handled differently
        MenuItem deleteItem = menu.findItem(R.id.action_delete_secret);
        if (deleteItem != null) {
            deleteItem.setVisible(false); // Compartment deletion is not done from this simple editor
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home || itemId == R.id.action_cancel) {
            this.setResult(RESULT_CANCELED);
            this.finish();
            return true;
        } else if (itemId == R.id.action_done) {
            Bundle data = new Bundle();
            data.putString(ARG_TEXT, mText.getText().toString());

            Intent intent = new Intent();
            intent.putExtras(data);
            this.setResult(RESULT_OK, intent);
            this.finish();
            return true;
        } else if (itemId == R.id.action_delete_secret) {
            // For compartment editing, this action is not used, so treat as cancel or do nothing.
            // As per onCreateOptionsMenu, this item should be hidden.
            // If it were visible, finishing with RESULT_CANCELED is a safe default.
            this.setResult(RESULT_CANCELED);
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
