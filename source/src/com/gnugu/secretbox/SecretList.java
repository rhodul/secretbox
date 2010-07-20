/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SimpleCursorAdapter.ViewBinder;
import com.gnugu.secretbox.DataAdapter.Compartment;
import com.gnugu.secretbox.DataAdapter.Secret;

/**
 * @author HERA Consulting Ltd.
 * 
 */
public class SecretList extends ListActivity {

	/**
	 * Custom ViewBinder for SecretList.
	 * 
	 * @author HERA Consulting Ltd.
	 * 
	 */
	private final class SecretViewBinder implements ViewBinder {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.widget.SimpleCursorAdapter.ViewBinder#setViewValue(android
		 * .view.View, android.database.Cursor, int)
		 */
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String columnName = cursor.getColumnName(columnIndex);

			if (columnName.equals(DataAdapter.Secret.TITLE)) {
				TextView secretCount = (TextView) view
						.findViewById(R.id.secretTitle);
				secretCount.setText(SecretList.this._adapter.decrypt(cursor
						.getString(columnIndex)));
				return true;
			}

			return false;
		}

	}

	/**
	 * Indicates back to calling activity whether or not any secret was deleted
	 * or added.
	 */
	public static final String VARIABLE_COUNT_CHANGED = "countChanged";

	private static final int MENU_NEW = 0;
	private static final int MENU_EDIT = 1;
	private static final int MENU_DELETE = 2;
	private static final int MENU_GROUP_SECRET_SELECTED = 1;
	private static final int ACTIVITY_NEW_SECRET = 100;
	private static final int ACTIVITY_EDIT_SECRET = 101;

	private DataAdapter _adapter = null;
	private long _compartmentId;
	private String _compartmentName;
	private boolean _countChanged = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.secret_list);
		this.setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

		_adapter = ((Application) this.getApplication()).getDataAdapter();

		// get the compartment id out of the intend
		_compartmentId = this.getIntent().getLongExtra(Compartment._ID, 1);

		// set the title
		Cursor cursor = _adapter.getCompartment(_compartmentId);
		_compartmentName = _adapter.decrypt(cursor.getString(cursor
				.getColumnIndex(Compartment.NAME)));
		cursor.close();
		this.setTitle(_compartmentName);

		// setup the ListView adapter (will be populated in fillData())
		// map cursor fields to list item
		String[] from = new String[] { Secret.TITLE };
		int[] to = new int[] { R.id.secretTitle };
		SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this,
				R.layout.secret_list_item, null, from, to);
		// set our custom view binder
		cursorAdapter.setViewBinder(new SecretViewBinder());
		this.setListAdapter(cursorAdapter);

		this.fillData();

		// inform the list we provide context menus for items
		this.getListView().setOnCreateContextMenuListener(this);

		// also set the result here so the calling activity
		// gets data even if we don't add/remove secret
		this.setResult();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// new compartment
		menu.add(Menu.NONE, MENU_NEW, 0, R.string.insert).setIcon(
				android.R.drawable.ic_menu_add);

		// (we will hide/show the group as the item is selected or not
		// see onPrepareOptionsMenu())

		// edit secret menu
		menu.add(MENU_GROUP_SECRET_SELECTED, MENU_EDIT, 0, R.string.edit)
				.setIcon(android.R.drawable.ic_menu_edit);

		// delete secret
		menu.add(MENU_GROUP_SECRET_SELECTED, MENU_DELETE, 0, R.string.delete)
				.setIcon(android.R.drawable.ic_menu_delete);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// hide or show edit menus based on selected item
		menu.setGroupVisible(MENU_GROUP_SECRET_SELECTED, this
				.getSelectedItemId() > 0);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW:
			this.newSecret();
			return true;
		case MENU_EDIT:
			this.editSecret(this.getSelectedItemId());
			return true;
		case MENU_DELETE:
			this.deleteSecret(this.getSelectedItemId());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(this.getClass().getName(), "bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) this.getListAdapter().getItem(info.position);
		if (cursor == null) {
			// for some reason the requested item isn't available, do nothing
			return;
		}

		// setup the menu header
		menu.setHeaderTitle(_adapter.decrypt(cursor.getString(cursor
				.getColumnIndex(DataAdapter.Secret.TITLE))));

		// add menu item(s)
		menu.add(Menu.NONE, MENU_EDIT, 0, R.string.edit);
		menu.add(Menu.NONE, MENU_DELETE, 0, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e(this.getClass().getName(), "bad menuInfo", e);
			return false;
		}

		switch (item.getItemId()) {
		case MENU_EDIT:
			this.editSecret(info.id);
			return true;
		case MENU_DELETE:
			this.deleteSecret(info.id);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTIVITY_NEW_SECRET:
			// let's make a toast
			Toast.makeText(this, R.string.secretCreated,
							Toast.LENGTH_SHORT).show();
			// new compartment, so refresh data
			this.fillData();
			// also notify CompartmentList to refresh itself
			_countChanged = true;
			this.setResult();
			break;
		case ACTIVITY_EDIT_SECRET:
			// let's make a toast
			Toast.makeText(this, R.string.secretChanged, Toast.LENGTH_SHORT)
					.show();
			// compartment changed, so refresh data
			this.fillData();
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		this.editSecret(id);
	}

	// call this method when secret added/deleted
	// so the CompartmentList can refresh
	private void setResult() {
		// tell the calling activity if the count has changed
		// so it can decide whether or not to refresh the list
		Intent i = new Intent();
		i.putExtra(VARIABLE_COUNT_CHANGED, _countChanged);
		this.setResult(RESULT_OK, i);
	}

	void fillData() {
		SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter)this.getListAdapter();
		
		// get old cursor
		Cursor cursor = cursorAdapter.getCursor();
		
		if (cursor != null) {
			// if cursor is there simply re-query
			cursor.requery();
		} else {
			// need for new cursor
			cursor = _adapter.getSecrets(_compartmentId);
			this.startManagingCursor(cursor);
			cursorAdapter.changeCursor(cursor);
		}
		
		// make the "no items" text visible
		if (cursor.getCount() == 0) {
			TextView empty = ((TextView) this.findViewById(R.id.emptyText));

			// we have HTML escaped styled format string there
			// so we need to make HTML out of it
			// (and we are making sure that _compartmentName doesn't
			// have no HTML in it)
			empty.setText(Html.fromHtml(this.getString(R.string.noSecrets,
					TextUtils.htmlEncode(_compartmentName))));

			empty.setVisibility(TextView.VISIBLE);
		}
	}

	private void newSecret() {
		// launch activity to insert a new secret
		Intent i = new Intent(this, SecretEditor.class);
		i.setAction(Intent.ACTION_INSERT);
		i.putExtra(SecretEditor.VARIABLE_COMPARTMENT_ID, _compartmentId);
		this.startActivityForResult(i, ACTIVITY_NEW_SECRET);
	}

	private void editSecret(long secretId) {
		// launch activity to edit a secret
		Intent intent = new Intent(this, SecretEditor.class);
		intent.setAction(Intent.ACTION_EDIT);
		// pass in the record ID
		intent.putExtra(DataAdapter.Secret._ID, secretId);

		this.startActivityForResult(intent, ACTIVITY_EDIT_SECRET);
	}

	private void deleteSecret(long secretId) {
		// get secret title
		Cursor cursor = _adapter.getSecret(secretId);
		String encryptedTitle = cursor.getString(cursor
				.getColumnIndex(Secret.TITLE));
		cursor.close();
		String secretTitle = _adapter.decrypt(encryptedTitle);

		// this is only defined so the anonymous OnClickHandler
		// can see it
		final long secretToDelete = secretId;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(this.getString(R.string.delete)).setMessage(
				Html.fromHtml(this.getString(R.string.deleteSecret, TextUtils
						.htmlEncode(secretTitle)))).setIcon(
				android.R.drawable.ic_dialog_alert).setCancelable(false)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SecretList.this._adapter.deleteSecret(secretToDelete);

						Toast.makeText(SecretList.this, R.string.secretDeleted, Toast.LENGTH_SHORT)
						.show();

						SecretList.this.fillData();

						// and set the result
						SecretList.this._countChanged = true;
						SecretList.this.setResult();
					}
				});

		builder.create().show();
	}

}
