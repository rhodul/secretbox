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
public class AlertDialogFragment extends DialogFragment {
    private final CharSequence mMessage;
    private final View.OnClickListener mOnClickListener;

    /**
     *
     * @param message
     * @param onClickListener What has to happen when OK is clicked.
     */
    public AlertDialogFragment(CharSequence message, View.OnClickListener onClickListener) {
        mMessage = message;
        mOnClickListener = onClickListener;

        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.alert_dialog, container);

        TextView message = (TextView) view.findViewById(R.id.message);
        message.setText(mMessage);

        // we need to dismiss the dialog when clicked, so let's do so
        // by wrapping their onClickListener in ours
        Button ok = (Button) view.findViewById(R.id.ok);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(view);
                }
                dismiss();
            }
        };
        ok.setOnClickListener(listener);

        this.setCancelable(false);

        return view;
    }

}
