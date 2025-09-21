package com.gnugu.secretbox;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity; // Corrected import
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

/**
 * Created by rhodul on 2014-04-29.
 */
public class EditActivity extends AppCompatActivity { // Corrected base class
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static final String ARG_TEXT = "text";

    private String mOriginalText;
    private EditText mText;
    private TextWatcher mTextWatcher;
    private MenuItem mOk;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        if (getSupportActionBar() != null) { // Good practice to check for null
            getSupportActionBar().setIcon(null);
            this.setTitle(null);
            getSupportActionBar().setIcon(R.drawable.ic_action_sbox);
        }

        mOriginalText = getIntent().getStringExtra(ARG_TEXT);
        mText = (EditText) findViewById(R.id.text);
        mText.setText(mOriginalText);
        mText.setHint(mOriginalText);

        mTextWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int s, int b, int c) {
                if (mText.getText().length() > 0 && mOk != null) { // Check mOk for null
                    mOk.setEnabled(true);
                } else if (mOk != null) { // Check mOk for null
                    mOk.setEnabled(false);
                }
            }
            public void afterTextChanged(Editable editable) { }
            public void beforeTextChanged(CharSequence cs, int i, int j, int k) { }
        };
        // It's good practice to add the TextWatcher after mOk is initialized
        // or ensure mOk is initialized before any text change can occur.
        // For now, will add it here, assuming mOk will be initialized in onCreateOptionsMenu soon enough.
        mText.addTextChangedListener(mTextWatcher);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu);

        mOk = menu.findItem(R.id.action_done);
        // Initialize mOk's state based on current text, as onTextChanged might not have run with mOk initialized.
        if (mText != null && mOk != null) {
             mOk.setEnabled(mText.getText().length() > 0);
        }

        MenuItem cancel = menu.findItem(R.id.action_cancel);
        cancel.setTitle(cancel.getTitle().toString().toUpperCase());

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId(); // Use a local variable for switch
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
        }
        return super.onOptionsItemSelected(item);
    }

}
