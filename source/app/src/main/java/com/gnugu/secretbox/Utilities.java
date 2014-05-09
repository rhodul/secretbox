/**
 * Copyright 2009 HERA Consulting Ltd.
 */

package com.gnugu.secretbox;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

/**
 * @author HERA Consulting Ltd. Various utilities.
 */
final class Utilities {

    /**
     * Creates alert dialog.
     *
     * @param activity         Parent.
     * @param message Dialog message.
     * @param onClickListener Action to take when OK button is clicked.
     */
    public static void showAlertDialog(FragmentActivity activity,
                                           CharSequence message,
                                           View.OnClickListener onClickListener) {

        showDialog(activity, message, onClickListener);
    }

    /**
     * Shows the no media card dialog.
     *
     * @param onClickListener Action to take when OK button is clicked.
     */
    public static void showNoMediaCardDialog(FragmentActivity activity, View.OnClickListener onClickListener) {
        Utilities.showAlertDialog(activity,
                activity.getString(R.string.noMediaCard), onClickListener);
    }

    private static void showDialog(FragmentActivity activity,
                                       CharSequence message,
                                       View.OnClickListener onClickListener) {
        FragmentManager fm = activity.getSupportFragmentManager();
        AlertDialogFragment dialog = new AlertDialogFragment(message, onClickListener);
        dialog.show(fm, "alert_dialog");
    }

    /**
     *
     * @param activity
     * @param title
     * @param message
     * @param okClickListener When OK clicked.
     * @param cancelClickListener When Cancel clicked.
     */
    public static void showYesNoAlertDialog(FragmentActivity activity,
                                       CharSequence title,
                                       CharSequence message,
                                       View.OnClickListener okClickListener,
                                       View.OnClickListener cancelClickListener) {

        FragmentManager fm = activity.getSupportFragmentManager();
        YesNoAlertDialogFragment dialog = new YesNoAlertDialogFragment(title, message, okClickListener, cancelClickListener);
        dialog.show(fm, "alert_dialog");
    }

}
