/*******************************************************************************
 * OutgoingEmailDetector.java.java, Created: May 7, 2012
 *
 * Part of Muni Project
 *
 * Copyright (c) 2012 : NDS Limited
 *
 * P R O P R I E T A R Y &amp; C O N F I D E N T I A L
 *
 * The copyright of this code and related documentation together with any
 * other associated intellectual property rights are vested in NDS Limited
 * and may not be used except in accordance with the terms of the licence
 * that you have entered into with NDS Limited. Use of this material without
 * an express licence from NDS Limited shall be an infringement of copyright
 * and any other intellectual property rights that may be incorporated with
 * this material.
 *
 * ******************************************************************************
 * ******     Please Check GIT for revision/modification history    *******
 * ******************************************************************************
 */
package com.safecell;

import com.safecell.model.SCInterruption;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * @author uttama
 * 
 */
public class OutgoingEmailDetector extends ContentObserver {

	private Context mContext;

	private static final String CONTENT_EMAIL = "content://com.android.email.provider";
	//private static final String CONTENT_EMAIL = "content://com.android.email";

	private final String TAG = OutgoingEmailDetector.class.getSimpleName();

	public static boolean showtoast;

	/**
	 * @param handler
	 */
	public OutgoingEmailDetector(Handler handler, Context context) {
		super(handler);
		this.mContext = context;
	}

	public void start() {
		ContentResolver contentResolver = mContext.getContentResolver();
		contentResolver.registerContentObserver(Uri.parse(CONTENT_EMAIL), true,
				this);
	}

	public void stop() {
		ContentResolver contentResolver = mContext.getContentResolver();
		contentResolver.unregisterContentObserver(this);
	}

	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Log.v(TAG, "Self change value: " + selfChange);
		if (showtoast) {
			if (!new ConfigurePreferences(mContext).isTripAbandon()) {
				Toast.makeText(mContext, "Email interruption raised",
						Toast.LENGTH_LONG).show();
				
				showtoast = false;
			}
		}

	}
}
