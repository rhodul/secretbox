/**
 * Copyright � 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

/**
 * @author HERA Consulting Ltd.
 * 
 */
public class SecretImages extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.secret_images);

		TextView empty = ((TextView) this.findViewById(R.id.emptyText));
		empty.setText(Html.fromHtml(this.getString(R.string.noPictures)));
		empty.setVisibility(TextView.VISIBLE);
	}

}
