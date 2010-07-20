/**
 * Copyright � 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.gnugu.secretbox.DataAdapter.Secret;

/**
 * Edits Compartment.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public class SecretEditor extends Activity {

	/**
	 * Used to pass compartment id to this activity.
	 */
	public static final String VARIABLE_COMPARTMENT_ID = "compartmentId";

	private static final int MENU_PICTURES = 0;
	private final static long NEW_RECORD = -1;
	private long _compartmentId;
	private long _id = NEW_RECORD;
	private DataAdapter _adapter;
	private EditText _title;
	private EditText _body;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.secret_editor);

		_adapter = ((Application) this.getApplication()).getDataAdapter();

		// decide whether to insert or update
		Intent intent = this.getIntent();
		if (intent.getAction().equals(Intent.ACTION_INSERT)) {
			// get the owner compartment
			_compartmentId = intent.getLongExtra(VARIABLE_COMPARTMENT_ID, NEW_RECORD);

			// set title
			this.setTitle(R.string.insert);
		} else {
			// editing existing, so let's get the row ID
			_id = intent.getLongExtra(DataAdapter.Secret._ID, NEW_RECORD);

			this.setTitle(R.string.edit);
		}

		_title = (EditText) this.findViewById(R.id.secretTitle);
		_body = (EditText) this.findViewById(R.id.secretBody);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// pictures
		menu.add(Menu.NONE, MENU_PICTURES, 0, R.string.pictures).setIcon(
				android.R.drawable.ic_menu_gallery);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PICTURES:
			this.showPictures();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (_id != NEW_RECORD) {
			//TODO: 00. _adapter can be NULL when this activity was at background for a long time
			// (see report in Market publish)
			Cursor c = _adapter.getSecret(_id);
			String encryptedTitle = c.getString(c.getColumnIndex(DataAdapter.Secret.TITLE));
			String encryptedBody = c.getString(c.getColumnIndex(DataAdapter.Secret.BODY));
			c.close();
			_title.setText(_adapter.decrypt(encryptedTitle));
			_body.setText(_adapter.decrypt(encryptedBody));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.writeSecret();
	}

	// writes secret and returns true if success
	private boolean writeSecret() {
		// don't do nothing for empty title
		if (!TextUtils.isEmpty(_title.getText()) && _adapter != null) {
			if (_id == NEW_RECORD) {
				// insert new
				_id = _adapter.createSecret(_compartmentId, _title.getText()
						.toString(), _body.getText().toString());
			} else {
				// update existing
				_adapter.updateSecret(_id, _title.getText().toString(), _body
						.getText().toString());
			}
			return true;
		}
		return false;
	}

	private void showPictures() {
		// don't allow adding images if can't write secret
		if (!this.writeSecret()) {
			Toast.makeText(this, R.string.cannotAddPictures, Toast.LENGTH_LONG)
					.show();
		} else {
			// launch activity to deal with pictures
			Intent i = new Intent(this, SecretImages.class);
			i.putExtra(Secret._ID, _id);
			this.startActivity(i);
		}
	}

}
