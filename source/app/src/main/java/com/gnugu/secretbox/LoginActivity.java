package com.gnugu.secretbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;


/**
 * A login screen that offers login via email/password.

 */
public class LoginActivity extends ActionBarActivity {


    /**
     * Activities may choose to wait for the activity result.
     * This is the activity code.
     */
    public static final int ACTIVITY_LOGIN = 999;

    private EditText mPassword;
    private TextWatcher mTextWatcher;
    private Button mOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mTextWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int s, int b, int c) {
                if (mPassword.getText().length() > 0) {
                    mOk.setEnabled(true);
                } else {
                    mOk.setEnabled(false);
                }
            }
            public void afterTextChanged(Editable editable) { }
            public void beforeTextChanged(CharSequence cs, int i, int j, int k) { }
        };


        mPassword = (EditText) findViewById(R.id.password);
        mPassword.addTextChangedListener(mTextWatcher);
        mPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mOk = (Button) findViewById(R.id.ok);
        mOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.setResult(RESULT_CANCELED);
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        try {
            Application app = (Application) this.getApplication();
            if (app != null) {
                app.setDataAdapter(new DataAdapter(
                        mPassword.getText().toString()));
                this.setResult(RESULT_OK, new Intent());
                this.finish();
            }
        } catch (IllegalArgumentException e) {
            // empty pwd is inherently wrong pwd
            showLoginFailed();
        } catch (BadPaddingException e) {
            // wrong pwd
            showLoginFailed();
        } catch (InvalidKeyException e) {
            // pwd too short?
            showLoginFailed();
        } catch (IOException e) {
            Utilities.showNoMediaCardDialog(this, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }
    }

    private void showLoginFailed() {
        Utilities.showAlertDialog(this, getString(R.string.wrongPassword), null);
    }

}



