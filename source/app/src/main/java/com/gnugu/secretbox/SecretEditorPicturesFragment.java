package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;

/**
 * Created by rhodul on 2014-05-02.
 */
public class SecretEditorPicturesFragment extends Fragment {

    public SecretEditorPicturesFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_secret_editor_pictures, container, false);

        TextView text = (TextView) rootView.findViewById(R.id.text);
        text.setText(Html.fromHtml(this.getString(R.string.noPictures)));
        text.setMovementMethod(LinkMovementMethod.getInstance());

        return rootView;
    }
}
