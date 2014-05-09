package com.gnugu.secretbox;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by rhodul on 2014-05-01.
 */
public class YesNoAlertDialogFragment extends DialogFragment {
    private final CharSequence mTitle;
    private final CharSequence mMessage;
    private final View.OnClickListener mOkClickListener;
    private final View.OnClickListener mCancelClickListener;

    /**
     *
     * @param title
     * @param message
     * @param okClickListener What has to happen when OK is clicked.
     * @param cancelClickListener What has to happen when Cancel is clicked.
     */
    public YesNoAlertDialogFragment(CharSequence title, CharSequence message,
                                    View.OnClickListener okClickListener,
                                    View.OnClickListener cancelClickListener) {
        mTitle = title;
        mMessage = message;
        mOkClickListener = okClickListener;
        mCancelClickListener = cancelClickListener;

        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.alert_dialog_yes_no, container);

        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(mTitle);

        TextView message = (TextView) view.findViewById(R.id.message);
        message.setText(mMessage);

        // we need to dismiss the dialog when clicked, so let's do so
        // by wrapping their onClickListener in ours
        Button ok = (Button) view.findViewById(R.id.ok);
        View.OnClickListener okListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOkClickListener != null) {
                    mOkClickListener.onClick(view);
                }
                dismiss();
            }
        };
        ok.setOnClickListener(okListener);

        Button cancel = (Button) view.findViewById(R.id.cancel);
        View.OnClickListener cancelListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCancelClickListener != null) {
                    mCancelClickListener.onClick(view);
                }
                dismiss();
            }
        };
        cancel.setOnClickListener(cancelListener);


        this.setCancelable(false);

        return view;
    }

}
