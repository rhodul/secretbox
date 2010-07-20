/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Edits Compartment.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public class CompartmentEditor extends Activity {
	private final static long NEW_RECORD = -1;
	private long _id = NEW_RECORD;
	private DataAdapter _adapter;
	private EditText _name;
	private Button _ok;
	private boolean _wasOkClicked = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.compartment_editor);

		_adapter = ((Application) this.getApplication()).getDataAdapter();

		// decide whether to insert or update
		Intent intent = this.getIntent();
		if (intent.getAction().equals(Intent.ACTION_INSERT)) {
			// inserting new, nothing to do other then title
			this.setTitle(R.string.insert);
		} else {
			// editing existing, so let's get the row ID
			_id = intent.getLongExtra(DataAdapter.Compartment._ID, NEW_RECORD);

			this.setTitle(R.string.edit);
		}

		_ok = (Button) findViewById(R.id.titleOk);
		_ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// when the user clicks, just finish this activity;
				// onPause will be called, and we save our data there
				CompartmentEditor.this._wasOkClicked = true;
				CompartmentEditor.this.setResult(RESULT_OK);
				CompartmentEditor.this.finish();
			}
		});

		_name = (EditText) this.findViewById(R.id.titleText);
		_name.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// make sure the name is not empty
				if (TextUtils.isEmpty(_name.getText())) {
					_ok.setEnabled(false);
				} else {
					_ok.setEnabled(true);
				}
			}

		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (_id != NEW_RECORD) {
			Cursor c = _adapter.getCompartment(_id);
			String encryptedName = c.getString(c.getColumnIndex(DataAdapter.Compartment.NAME));
			c.close();
			_name.setText(_adapter.decrypt(encryptedName));
		}

		// disable OK button if text is not there
		_ok.setEnabled(!TextUtils.isEmpty(_name.getText()));
	}

	@Override
	protected void onPause() {
		super.onPause();
		// don't do nothing for empty text
		// and do only if OK was clicked
		if (!TextUtils.isEmpty(_name.getText())
				&& _wasOkClicked) {
			if (_id == NEW_RECORD) {
				// insert new
				_id = _adapter.createCompartment(_name.getText().toString());
			} else {
				// update existing
				_adapter.updateCompartment(_id, _name.getText().toString());
			}
		}
	}
}
