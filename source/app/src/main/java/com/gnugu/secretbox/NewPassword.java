/**
 * Copyright 2009 HERA Consulting Ltd.
 */

package com.gnugu.secretbox;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * New password collector control.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public final class NewPassword extends LinearLayout {

	/**
	 * Interface definition for a callbacks to be invoked as the password gets
	 * edited.
	 * 
	 * @author HERA Consulting Ltd.
	 * 
	 */
	public abstract static interface OnPasswordEditedListener {
		/**
		 * Called when the password and repeat are equal.
		 */
		public abstract void onPasswordsEqual();

		/**
		 * Called when the password and repeat are not equal.
		 */
		public abstract void onPasswordsNotEqual();
	}

	private OnPasswordEditedListener _onPasswordEditedListener = null;
	private EditText _pwd;
	private EditText _pwdRepeat;
	private TextWatcher _textWatcher;

	public NewPassword(Context context) {
		super(context);

		this.createChildControls(context);
	}

	public NewPassword(Context context, AttributeSet attrs) {
		super(context, attrs);

		this.createChildControls(context);
	}

	/**
	 * Sets the OnPasswordEditedListener.
	 * 
	 * @param listener
	 *            The listener.
	 */
	public void setOnPasswordEditedListener(OnPasswordEditedListener listener) {
		_onPasswordEditedListener = listener;
	}

	private void createChildControls(Context context) {
		this.setOrientation(VERTICAL);
		this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		LayoutParams pwdLayoutParams = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		PasswordTransformationMethod pwdTransformation = new PasswordTransformationMethod();

		// checks if the two passwords are equal
		_textWatcher = new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence cs, int s, int b, int c) {
				// compare the two passwords
				if (_onPasswordEditedListener != null) {
					String pwd = _pwd.getText().toString();
					String pwdRepead = _pwdRepeat.getText().toString();
					if (!TextUtils.isEmpty(pwd)
							&& !TextUtils.isEmpty(pwdRepead)
							&& pwd.equals(pwdRepead)) {
						_onPasswordEditedListener.onPasswordsEqual();
					} else {
						_onPasswordEditedListener.onPasswordsNotEqual();
					}
				}
			}
			public void afterTextChanged(Editable editable) { }
			public void beforeTextChanged(CharSequence cs, int i, int j, int k) { }

		};

		_pwd = new EditText(context);
		_pwd.setHint(R.string.newPassword);
		_pwd.setSingleLine();
		_pwd.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		_pwd.setTransformationMethod(pwdTransformation);
		_pwd.addTextChangedListener(_textWatcher);
		this.addView(_pwd, pwdLayoutParams);

		_pwdRepeat = new EditText(context);
		_pwdRepeat.setHint(R.string.newPasswordRepeat);
		_pwdRepeat.setSingleLine();
		_pwdRepeat.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		_pwdRepeat.setTransformationMethod(pwdTransformation);
		_pwdRepeat.addTextChangedListener(_textWatcher);
		this.addView(_pwdRepeat, pwdLayoutParams);
	}

	/**
	 * Gets the user's new password.
	 * @return New password.
	 */
	public CharSequence getPassword() {
		return _pwd.getText().toString();
	}
}
