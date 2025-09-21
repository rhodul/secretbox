/**
 * Copyright 2009 HERA Consulting Ltd.
 */

package com.gnugu.secretbox;

import java.io.IOException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity; // Corrected import
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.gnugu.secretbox.NewPassword.OnPasswordEditedListener;

/**
 * When the application is used for the first time this activity will prompt for
 * new password.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public final class NewSecretBoxActivity extends AppCompatActivity { // Corrected base class
	Button _ok;
	NewPassword _newPwd;
	
	/**
	 * Activities may choose to wait for the activity result.
	 * This is the activity code.
	 */
	public static final int ACTIVITY_NEW_SECRET_BOX = 666;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_new_secret_box);

		_ok = (Button) this.findViewById(R.id.newSecretBoxOk);
		_ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String password = (String) _newPwd.getPassword(); 
				// create new DB
				try {
					DataAdapter.createDatabase(NewSecretBoxActivity.this, password);
				} catch (IOException e) {
					Log.e(this.getClass().getName(), Log.getStackTraceString(e));
					throw new RuntimeException(e);
				}
				// also instantiate the object
				Application app = (Application) NewSecretBoxActivity.this.getApplication();
				try {
					app.setDataAdapter(new DataAdapter(NewSecretBoxActivity.this, password));
				} catch (BadPaddingException e) {
					Log.e(this.getClass().getName(), Log.getStackTraceString(e));
					throw new RuntimeException(e);
                } catch (InvalidKeyException e) {
                    Log.e(this.getClass().getName(), Log.getStackTraceString(e));
                    throw new RuntimeException(e);
				} catch (IOException e) {
					Log.e(this.getClass().getName(), Log.getStackTraceString(e));
					throw new RuntimeException(e);
				}
		        NewSecretBoxActivity.this.setResult(RESULT_OK, new Intent());
				NewSecretBoxActivity.this.finish();
			}
		});

		_newPwd = (NewPassword) this.findViewById(R.id.newSecretBoxPwd);
		_newPwd.setOnPasswordEditedListener(new OnPasswordEditedListener() {

			@Override
			public void onPasswordsNotEqual() {
				_ok.setEnabled(false);
			}

			@Override
			public void onPasswordsEqual() {
				_ok.setEnabled(true);
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

}
