package com.gnugu.secretbox;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.fragment.app.Fragment; // Corrected import
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
        // Html.fromHtml(String) is deprecated in API 24. For now, keeping original logic.
        // Modern usage: Html.fromHtml(getString(R.string.noPictures), Html.FROM_HTML_MODE_LEGACY)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            text.setText(Html.fromHtml(this.getString(R.string.noPictures), Html.FROM_HTML_MODE_LEGACY));
        } else {
            text.setText(Html.fromHtml(this.getString(R.string.noPictures)));
        }
        text.setMovementMethod(LinkMovementMethod.getInstance());

        return rootView;
    }
}
