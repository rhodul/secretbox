/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import java.io.IOException;

import javax.crypto.BadPaddingException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.gnugu.secretbox.DataAdapter.OnLoginListener;
import com.gnugu.secretbox.DataAdapter.OnNewDatabaseListener;

/**
 * Since dialogs are NON blocking in Android and we need to prompt user for few
 * things when the application starts we need to do so in another thread and
 * communicate back via callbacks.
 * 
 * @author HERA Consulting Ltd.
 * 
 */
final class StartupThread extends Thread {
	private final Handler _handler;
	private DataAdapter _adapter;
	private StringBuilder _pwd;
	private volatile boolean _shouldDie = false;

	/**
	 * This is used as a key in the bundle for passing the messages.
	 */
	public static final String VARIABLE_MSG = "msg";
	/**
	 * VARIABLE_MSG will have this value if no sdcard is mounted.
	 */
	public static final int MSG_NO_SDCARD = 0;
	/**
	 * VARIABLE_MSG will have this value if the application runs for the very
	 * first time.
	 */
	public static final int MSG_NEW_SECRET_BOX = 1;
	/**
	 * VARIABLE_MSG will have this value if the login is required.
	 */
	public static final int MSG_LOGIN = 2;
	/**
	 * VARIABLE_MSG will have this value if wrong password was provided.
	 */
	public static final int MSG_WRONG_PASSWORD = 3;
	/**
	 * VARIABLE_MSG will have this value when DataAdapter has been instantiated.
	 */
	public static final int MSG_DATA_ADAPTER_READY = 4;
	/**
	 * VARIABLE_MSG will have this value if there was an exception. The thread
	 * will die.
	 */
	public static final int MSG_EXCEPTION = -1;
	/**
	 * This is used as a key in the bundle for passing the exception.
	 */
	public static final String VARIABLE_EXCEPTION = "exception";

	StartupThread(Handler handler) {
		if (handler == null) {
			throw new NullPointerException();
		}
		_handler = handler;

		_pwd = new StringBuilder();
	}

	@Override
	public void run() {
		try {
			_adapter = new DataAdapter(new OnNewDatabaseListener() {
				@Override
				public String onGetPassword() {
					// we can't declare msg and data at the beginning
					// of this method as final because then we would be
					// reusing the message and we would get "This message
					// has alredy been posted." error every now and then!
					Message msg = _handler.obtainMessage();
					Bundle data = new Bundle();
					data.putInt(VARIABLE_MSG, MSG_NEW_SECRET_BOX);
					msg.setData(data);
					_handler.sendMessage(msg);
					try {
						// wait for the UI thread to supply pwd
						synchronized (_pwd) {
							while (_pwd.length() == 0) {
								_pwd.wait();
								// let's say user pressed Cancel button
								// on password challenge
								if (_shouldDie) {
									return null;
								}
							}
						}
					} catch (Exception e) {
						Log.e(this.getClass().getName(), Log
								.getStackTraceString(e));
					}
					return _pwd.toString();
				}
			}, new OnLoginListener() {
				@Override
				public String onGetPassword() {
					Message msg = _handler.obtainMessage();
					Bundle data = new Bundle();
					data.putInt(VARIABLE_MSG, MSG_LOGIN);
					msg.setData(data);
					_handler.sendMessage(msg);
					try {
						// wait for the UI thread to supply pwd
						synchronized (_pwd) {
							while (_pwd.length() == 0) {
								_pwd.wait();
								// let's say user pressed Cancel button
								// on password challenge
								if (_shouldDie) {
									return null;
								}
							}
						}
					} catch (Exception e) {
						Log.e(this.getClass().getName(), Log
								.getStackTraceString(e));
					}
					return _pwd.toString();
				}
			});
		} catch (BadPaddingException b) {
			// wrong pwd
			Message msg = _handler.obtainMessage();
			Bundle data = new Bundle();
			data.putInt(VARIABLE_MSG, MSG_WRONG_PASSWORD);
			msg.setData(data);
			_handler.sendMessage(msg);
		} catch (IllegalArgumentException i) {
			// let's say user pressed Cancel button
			// on password challenge
			if (_shouldDie) {
				return;
			} else {
				// seems like user provided blank pwd
				throw new IllegalArgumentException(i);
			}
		} catch (IOException e) {
			Message msg = _handler.obtainMessage();
			Bundle data = new Bundle();
			data.putInt(VARIABLE_MSG, MSG_NO_SDCARD);
			msg.setData(data);
			_handler.sendMessage(msg);
		} catch (Exception x) {
			Message msg = _handler.obtainMessage();
			Bundle data = new Bundle();
			data.putInt(VARIABLE_MSG, MSG_EXCEPTION);
			data.putSerializable(VARIABLE_EXCEPTION, x);
			msg.setData(data);
			_handler.sendMessage(msg);
		}

		// return the adapter via handler now when
		// the object has been constructed
		if (_adapter != null) {
			Message msg = _handler.obtainMessage();
			Bundle data = new Bundle();
			data.putInt(VARIABLE_MSG, MSG_DATA_ADAPTER_READY);
			msg.setData(data);
			_handler.sendMessage(msg);

		}
	}

	/**
	 * Notifies the thread that it should die.
	 */
	public void setShouldDie() {
		_shouldDie = true;
		// unlock the sleepers
		synchronized (_pwd) {
			_pwd.notify();
		}
	}

	/**
	 * Sets the password for DataAdapter.
	 * 
	 * @param pwd
	 *            Password as collected from the user.
	 */
	public void setPassword(String pwd) {
		if (TextUtils.isEmpty(pwd)) {
			throw new IllegalArgumentException("pwd cannot be null or empty.");
		}
		synchronized (_pwd) {
			_pwd.append(pwd);
			_pwd.notify();
		}
	}

	/**
	 * After the calling thread has been notified that the adapter is ready it
	 * can grab it via this method.
	 * 
	 * @return DataAdapter.
	 */
	public synchronized DataAdapter getDataAdapter() {
		return _adapter;
	}
}
