/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */
package com.gnugu.secretbox;

import java.util.Formatter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SimpleCursorAdapter.ViewBinder;
import com.gnugu.secretbox.DataAdapter.Compartment;

/**
 * List all compartments in the secret box.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
public final class CompartmentList extends ListActivity {

	/**
	 * When app is starting up we need to perform some actions and popup some
	 * dialogs. Dialogs are not blocking in Android so we need separate thread
	 * and handler communication.
	 * 
	 * @author HERA Consulting Ltd.
	 * 
	 */
	private final class StartupHandler extends Handler {
		private static final int DIALOG_NO_SDCARD = 0;
		private static final int DIALOG_DB_CORRUPTED = 1;
		private static final int DIALOG_LOGIN = 2;
		private static final int ACTIVITY_NEW_SECRET_BOX = 0;
		private StartupThread _startupThread = null;

		public StartupHandler() {
			this.startStartupThread();
		}

		/**
		 * Stops the thread that handles startup process.
		 */
		private void stopStartupThread() {
			if (_startupThread != null) {
				_startupThread.setShouldDie();
				try {
					// wait for the thread to die
					_startupThread.join();
				} catch (InterruptedException e) {
					Log
							.e(this.getClass().getName(), Log
									.getStackTraceString(e));
					throw new RuntimeException(e);
				}
				_startupThread = null;
			}
		}

		/**
		 * Starts the thread that handles startup process.
		 */
		private void startStartupThread() {
			/**
			 * Since dialogs are NON blocking in Android and we need to prompt
			 * user for few things when the application starts we need to do so
			 * in another thread and communicate back via callbacks.
			 */
			this.stopStartupThread();
			_startupThread = new StartupThread(this);
			_startupThread.start();
		}

		@Override
		public void handleMessage(Message msg) {
			int message = msg.getData().getInt(StartupThread.VARIABLE_MSG);
			switch (message) {
			case StartupThread.MSG_NO_SDCARD:
				this.showNoSdCardDialog();
				break;
			case StartupThread.MSG_NEW_SECRET_BOX:
				this.showNewSecretBoxActivity();
				break;
			case StartupThread.MSG_LOGIN:
				this.showLoginDialog();
				break;
			case StartupThread.MSG_WRONG_PASSWORD:
				this.showLoginFailed();
				break;
			case StartupThread.MSG_DATA_ADAPTER_READY:
				// get the ready to use adapter
				this.receiveDataAdapter();
				break;
			case StartupThread.MSG_EXCEPTION:
				Exception ex = (Exception) msg.getData().getSerializable(
						StartupThread.VARIABLE_EXCEPTION);
				this.receiveException(ex);
				break;
			}
		}

		/**
		 * Receives DataAdapter from StartupThread
		 */
		private void receiveDataAdapter() {
			CompartmentList.this._adapter = _startupThread.getDataAdapter();
			this.stopStartupThread();

			// share
			((Application) CompartmentList.this.getApplication())
					.setDataAdapter(CompartmentList.this._adapter);

			// fill data
			CompartmentList.this.fillData();
		}

		private void receiveException(Exception ex) {
			if (ex instanceof SQLiteDatabaseCorruptException) {
				CompartmentList.this.showDialog(DIALOG_DB_CORRUPTED);
			} else {
				CompartmentList.this.createErrorDialog(ex.getMessage()).show();
			}
		}

		private void showNoSdCardDialog() {
			CompartmentList.this.showDialog(DIALOG_NO_SDCARD);
		}

		private void showNewSecretBoxActivity() {
			CompartmentList.this.startActivityForResult(new Intent(
					CompartmentList.this, NewSecretBox.class),
					ACTIVITY_NEW_SECRET_BOX);
		}

		Dialog createLoginDialog() {
			final EditText pwd = new EditText(CompartmentList.this);
			pwd.setHint(R.string.password);
			pwd.setSingleLine();
			pwd.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			pwd.setTransformationMethod(new PasswordTransformationMethod());
			pwd.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT));

			AlertDialog.Builder builder = new AlertDialog.Builder(
					CompartmentList.this);
			builder.setTitle(R.string.loginText).setCancelable(false)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									try {
										_startupThread.setPassword(pwd
												.getText().toString());
									} catch (IllegalArgumentException e) {
										// empty pwd is inherently wrong pwd
										StartupHandler.this.showLoginFailed();
									}
								}
							}).setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									StartupHandler.this.exitApplication();
								}
							}).setView(pwd);

			return builder.create();
		}

		private void showLoginDialog() {
			CompartmentList.this.showDialog(DIALOG_LOGIN);
		}

		private void showLoginFailed() {
			Toast.makeText(CompartmentList.this, R.string.wrongPassword,
					Toast.LENGTH_SHORT).show();
			// and start the startup thread again to prompt for pwd
			this.startStartupThread();
		}

		void exitApplication() {
			this.stopStartupThread();
			CompartmentList.this.finish();
		}

		private void newSecretBoxActivityResult(int resultCode, Intent data) {
			// check if OK was pressed
			if (resultCode == Activity.RESULT_OK) {
				// get the pwd out
				String newPwd = data.getStringExtra(NewSecretBox.PASSWORD);
				_startupThread.setPassword(newPwd);
			} else {
				// user didn't create new pwd, let's get out of here
				this.exitApplication();
			}
		}

	}

	/**
	 * Custom ViewBinder for CompartmentList.
	 * 
	 * @author HERA Consulting Ltd.
	 * 
	 */
	private final class CompartmentViewBinder implements ViewBinder {
		private final StringBuilder _secretCountBuilder = new StringBuilder();
		private final Formatter _secretCountFormatter = new Formatter(
				_secretCountBuilder);
		private final String _secretCountFormat = CompartmentList.this
				.getString(R.string.secretCount);

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

			if (columnName.equals(DataAdapter.Compartment.NAME)) {
				TextView secretCount = (TextView) view
						.findViewById(R.id.compartmentName);
				secretCount.setText(CompartmentList.this._adapter
						.decrypt(cursor.getString(columnIndex)));
				return true;
			} else if (columnName.equals(DataAdapter.Compartment.SECRET_COUNT)) {
				_secretCountBuilder.delete(0, _secretCountBuilder.length());
				_secretCountFormatter.format(_secretCountFormat, cursor
						.getInt(columnIndex));
				TextView secretCount = (TextView) view
						.findViewById(R.id.secretCount);
				secretCount.setText(_secretCountBuilder.toString());
				return true;
			}

			return false;
		}

	}

	private static final int MENU_NEW = 0;
	private static final int MENU_EDIT = 1;
	private static final int MENU_DELETE = 2;
	private static final int MENU_SHOW_SECRETS = 3;
	private static final int MENU_CHANGE_PWD = 4;
	private static final int MENU_ABOUT = 50;
	private static final int MENU_GROUP_COMPARTMENT_SELECTED = 1;
	private static final int ACTIVITY_NEW_COMPARTMENT = 100;
	private static final int ACTIVITY_EDIT_COMPARTMENT = 101;
	private static final int ACTIVITY_SECRETS = 110;
	private static final int DIALOG_CANNOT_DELETE_COMPARTMENT = 100;

	private StartupHandler _startupHandler = null;
	private DataAdapter _adapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.compartment_list);
		this.setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

		// setup the ListView adapter (will be populated in fillData())
		// map cursor fields to list item
		String[] from = new String[] { Compartment.NAME,
				Compartment.SECRET_COUNT };
		int[] to = new int[] { R.id.compartmentName, R.id.secretCount };
		SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this,
				R.layout.compartment_list_item, null, from, to);
		// set our custom view binder
		cursorAdapter.setViewBinder(new CompartmentViewBinder());
		this.setListAdapter(cursorAdapter);

		_adapter = ((Application) this.getApplication()).getDataAdapter();
		if (_adapter == null) {
			/**
			 * When app is starting up we need to perform some actions and popup
			 * some dialogs. Dialogs are not blocking in Android so we need
			 * separate thread and handler communication.
			 * 
			 * As it seems DataAdapter was not yet instantiated by nobody so we
			 * have to do it now. Once instantiated it will call our fillData()
			 * method.
			 */
			_startupHandler = new StartupHandler();
		} else {
			// the app already went through startup procedure
			this.fillData();
		}

		// inform the list we provide context menus for items
		this.getListView().setOnCreateContextMenuListener(this);
	}

	@Override
	public void onDestroy() {
		// after user exits the activity
		// we want him to login when he comes back
		((Application) this.getApplication()).setDataAdapter(null);

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// new compartment
		menu.add(Menu.NONE, MENU_NEW, 0, R.string.insert).setIcon(
				android.R.drawable.ic_menu_add);

		// (we will hide/show the group as the item is selected or not
		// see onPrepareOptionsMenu())
		// show secrets
		menu.add(MENU_GROUP_COMPARTMENT_SELECTED, MENU_SHOW_SECRETS, 0,
				R.string.showSecrets).setIcon(android.R.drawable.ic_menu_view);

		// edit compartment menu
		menu.add(MENU_GROUP_COMPARTMENT_SELECTED, MENU_EDIT, 0, R.string.edit)
				.setIcon(android.R.drawable.ic_menu_edit);

		// delete compartment
		menu.add(MENU_GROUP_COMPARTMENT_SELECTED, MENU_DELETE, 0,
				R.string.delete).setIcon(android.R.drawable.ic_menu_delete);

		// change pwd
		menu.add(Menu.NONE, MENU_CHANGE_PWD, 0, R.string.changePassword)
				.setIcon(android.R.drawable.ic_menu_manage);

		// about
		menu.add(Menu.NONE, MENU_ABOUT, 0, R.string.aboutMenu).setIcon(
				android.R.drawable.ic_menu_help);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// hide or show edit menus based on selected item
		menu.setGroupVisible(MENU_GROUP_COMPARTMENT_SELECTED, this
				.getSelectedItemId() > 0);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW:
			this.newConmpartment();
			return true;
		case MENU_SHOW_SECRETS:
			this.showSecrets(this.getSelectedItemId());
			return true;
		case MENU_EDIT:
			this.editConmpartment(this.getSelectedItemId());
			return true;
		case MENU_DELETE:
			this.deleteConmpartment(this.getSelectedItemId());
			return true;
		case MENU_CHANGE_PWD:
			this.changePassword();
			return true;
		case MENU_ABOUT:
			this.startActivity(new Intent(this, About.class));
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
				.getColumnIndex(DataAdapter.Compartment.NAME))));

		// add menu item(s)
		// show secrets
		menu.add(Menu.NONE, MENU_SHOW_SECRETS, 0, R.string.showSecrets);
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
		case MENU_SHOW_SECRETS:
			this.showSecrets(info.id);
			return true;
		case MENU_EDIT:
			this.editConmpartment(info.id);
			return true;
		case MENU_DELETE:
			this.deleteConmpartment(info.id);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case StartupHandler.DIALOG_NO_SDCARD:
			dialog = this.createErrorDialog(this
					.getString(R.string.noMediaCard));
			break;
		case StartupHandler.DIALOG_DB_CORRUPTED:
			dialog = this.createErrorDialog(this
					.getString(R.string.dbCorrupted));
			break;
		case StartupHandler.DIALOG_LOGIN:
			// no null check required because this will
			// only happen when startup handler is alive
			dialog = _startupHandler.createLoginDialog();
			break;
		case DIALOG_CANNOT_DELETE_COMPARTMENT:
			dialog = this.createAlertDialog(this.getString(R.string.delete),
					this.getString(R.string.cannotDeleteCopmartment));
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case StartupHandler.ACTIVITY_NEW_SECRET_BOX:
			// safe without null pointer check since this
			// can only be reached when _startupHandler is alive
			_startupHandler.newSecretBoxActivityResult(resultCode, data);
			break;
		case ACTIVITY_NEW_COMPARTMENT:
			// let's make a toast
			if (resultCode == RESULT_OK) {
				Toast.makeText(this, R.string.compartmentCreated,
						Toast.LENGTH_SHORT).show();
				// new compartment, so refresh data
				this.fillData();
			}
			break;
		case ACTIVITY_EDIT_COMPARTMENT:
			// let's make a toast
			Toast.makeText(this, R.string.compartmentChanged,
					Toast.LENGTH_SHORT).show();
			// compartment changed, so refresh data
			this.fillData();
			break;
		case ACTIVITY_SECRETS:
			if (resultCode == RESULT_OK) {
				// when we returned from secrets we may need
				// to refresh data; let's see what secret list
				// has to say about it
				boolean shouldRefresh = data.getBooleanExtra(
						SecretList.VARIABLE_COUNT_CHANGED, true);
				if (shouldRefresh) {
					this.fillData();
				}
			}
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		this.showSecrets(id);
	}

	void fillData() {
		SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) this
				.getListAdapter();

		// get old cursor
		Cursor cursor = cursorAdapter.getCursor();

		if (cursor != null) {
			// if cursor is there simply re-query
			cursor.requery();
		} else {
			// need for new cursor
			cursor = _adapter.getCompartments();
			this.startManagingCursor(cursor);
			cursorAdapter.changeCursor(cursor);
		}

		// make the "no items" text visible
		if (cursor.getCount() == 0) {
			((TextView) this.findViewById(R.id.emptyText))
					.setVisibility(TextView.VISIBLE);
		}
	}

	private void newConmpartment() {
		// launch activity to insert a new compartment
		this.startActivityForResult(new Intent(this, CompartmentEditor.class)
				.setAction(Intent.ACTION_INSERT), ACTIVITY_NEW_COMPARTMENT);
	}

	private void editConmpartment(long compartmentId) {
		// launch activity to edit a compartment
		Intent intent = new Intent(this, CompartmentEditor.class);
		intent.setAction(Intent.ACTION_EDIT);
		// pass in the record ID
		intent.putExtra(DataAdapter.Compartment._ID, compartmentId);

		this.startActivityForResult(intent, ACTIVITY_EDIT_COMPARTMENT);
	}

	private void deleteConmpartment(long compartmentId) {
		// check if the compartment is empty
		Cursor cursor = _adapter.getCompartment(compartmentId);
		int secretCount = cursor.getInt(cursor
				.getColumnIndex(DataAdapter.Compartment.SECRET_COUNT));
		cursor.close();

		if (secretCount > 0) {
			// inform user
			this.showDialog(DIALOG_CANNOT_DELETE_COMPARTMENT);
		} else {
			// delete
			_adapter.deleteCompartment(compartmentId);
			// let's make a toast
			Toast.makeText(this, R.string.compartmentDeleted,
					Toast.LENGTH_SHORT).show();
			// and refresh
			this.fillData();
		}
	}

	Dialog createErrorDialog(CharSequence charSequence) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.error).setMessage(charSequence)
				.setCancelable(false).setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								CompartmentList.this.exit();
							}
						});

		return builder.create();
	}

	Dialog createAlertDialog(CharSequence title, CharSequence message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title).setMessage(message).setIcon(
				android.R.drawable.ic_dialog_alert).setCancelable(false)
				.setPositiveButton(android.R.string.ok, null);

		return builder.create();
	}

	private void showSecrets(long compartmentId) {
		// launch activity to show secrets
		this.startActivityForResult(new Intent(this, SecretList.class)
				.putExtra(Compartment._ID, compartmentId), ACTIVITY_SECRETS);
	}

	private void changePassword() {
		this.startActivity(new Intent(this, ChangePassword.class));
	}

	private void exit() {
		if (_startupHandler != null) {
			_startupHandler.exitApplication();
		} else {
			this.finish();
		}
	}

}
