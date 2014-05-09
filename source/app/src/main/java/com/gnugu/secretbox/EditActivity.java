package com.gnugu.secretbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

/**
 * Created by rhodul on 2014-04-29.
 */
public class EditActivity extends ActionBarActivity {
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
        getSupportActionBar().setIcon(null);

        this.setTitle(null);
        getSupportActionBar().setIcon(R.drawable.ic_action_sbox);

        mOriginalText = getIntent().getStringExtra(ARG_TEXT);
        mText = (EditText) findViewById(R.id.text);
        mText.setText(mOriginalText);
        mText.setHint(mOriginalText);

        mTextWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int s, int b, int c) {
                if (mText.getText().length() > 0) {
                    mOk.setEnabled(true);
                } else {
                    mOk.setEnabled(false);
                }
            }
            public void afterTextChanged(Editable editable) { }
            public void beforeTextChanged(CharSequence cs, int i, int j, int k) { }
        };

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu);

        mOk = menu.findItem(R.id.action_done);

        MenuItem cancel = menu.findItem(R.id.action_cancel);
        cancel.setTitle(cancel.getTitle().toString().toUpperCase());

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel:
                this.setResult(RESULT_CANCELED);
                this.finish();
                return true;
            case R.id.action_done:
                Bundle data = new Bundle();
                data.putString(ARG_TEXT, mText.getText().toString());

                Intent intent = new Intent();
                intent.putExtras(data);
                this.setResult(RESULT_OK, intent);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
