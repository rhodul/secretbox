/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import javax.crypto.BadPaddingException;
import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.gnugu.secretbox.Application;
import com.gnugu.secretbox.NewPassword.OnPasswordEditedListener;

/**
 * When the application is used for the first time this activity will prompt for
 * new password.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public final class ChangePassword extends Activity {
	Button _ok;
	NewPassword _newPwd;
	EditText _oldPwd;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.change_password);

		_oldPwd = (EditText) this.findViewById(R.id.oldPassword);
		// when we use android:password attribute in the layout file
		// then the EditText's hint font has larger spacing for some reason,
		// so we do it here which doesn't change the spacing
		_oldPwd.setTransformationMethod(new PasswordTransformationMethod());
		_oldPwd.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);

		Button cancel = (Button) this.findViewById(R.id.changePasswordCancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ChangePassword.this.setResult(RESULT_CANCELED);
				ChangePassword.this.finish();
			}
		});

		_ok = (Button) this.findViewById(R.id.changePasswordOk);
		_ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					DataAdapter adapter = ((Application) ChangePassword.this.getApplication()).getDataAdapter();
					adapter.setNewEncryptionPassword(ChangePassword.this._oldPwd.getText().toString(), _newPwd.getPassword().toString());

					Toast.makeText(ChangePassword.this, R.string.passwordChanged,
							Toast.LENGTH_SHORT).show();
			        
					ChangePassword.this.setResult(RESULT_OK);
					ChangePassword.this.finish();
				} catch(BadPaddingException ex) {
					Toast.makeText(ChangePassword.this, R.string.wrongOldPassword,
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		

		_newPwd = (NewPassword) this.findViewById(R.id.newPassword);
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
