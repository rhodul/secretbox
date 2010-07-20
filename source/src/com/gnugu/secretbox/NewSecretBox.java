/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
public final class NewSecretBox extends Activity {
	Button _ok;
	NewPassword _newPwd;
	
	/**
	 * Name of the variable that will be returned from this activity.
	 */
	public static final String PASSWORD = "password";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.new_secret_box);

		Button cancel = (Button) this.findViewById(R.id.newSecretBoxCancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				NewSecretBox.this.setResult(RESULT_CANCELED);
				NewSecretBox.this.finish();
			}
		});

		_ok = (Button) this.findViewById(R.id.newSecretBoxOk);
		_ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        Intent i = new Intent();
		        i.putExtra(NewSecretBox.PASSWORD, _newPwd.getPassword());
		        NewSecretBox.this.setResult(RESULT_OK, i);
				NewSecretBox.this.finish();
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

	
}
