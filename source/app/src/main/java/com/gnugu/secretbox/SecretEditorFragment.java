package com.gnugu.secretbox;

import android.os.Bundle;
import androidx.fragment.app.Fragment; // Corrected import
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by rhodul on 2014-05-02.
 */
public class SecretEditorFragment extends Fragment {

    private final String mTitle;
    private final String mBody;
    private boolean mEditMode;
    private View mRoot;
    private EditText mTitleView;
    private EditText mBodyView;

    public SecretEditorFragment(String title, String body, boolean editMode) {
        mTitle = title;
        mBody = body;
        mEditMode = editMode;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRoot = inflater.inflate(R.layout.fragment_secret_editor, container, false);

        mTitleView = (EditText) mRoot.findViewById(R.id.secret_edit_title);
        mTitleView.setText(mTitle);
        mBodyView = (EditText) mRoot.findViewById(R.id.secret_edit_body);
        mBodyView.setText(mBody);

        ((TextView)mRoot.findViewById(R.id.secret_view_title)).setText(mTitle);
        ((TextView)mRoot.findViewById(R.id.secret_view_body)).setText(mBody);

        setEditMode(mEditMode);

        return mRoot;
    }

    public String getTitle() {
        if (mTitleView != null) {
            return mTitleView.getText().toString();
        }
        return null;
    }

    public String getBody() {
        if (mBodyView != null) {
            return mBodyView.getText().toString();
        }
        return null;
    }

    public void setEditMode(boolean editMode) {
        mEditMode = editMode;
        if (editMode) {
            mRoot.findViewById(R.id.editor).setVisibility(View.VISIBLE);
            mRoot.findViewById(R.id.viewer).setVisibility(View.GONE);
        } else {
            mRoot.findViewById(R.id.editor).setVisibility(View.GONE);
            mRoot.findViewById(R.id.viewer).setVisibility(View.VISIBLE);
        }
    }

}
