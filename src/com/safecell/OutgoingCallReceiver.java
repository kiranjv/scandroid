/*******************************************************************************
 * OutgoingCallReceiver.java.java, Created: Apr 25, 2012
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

import com.android.internal.telephony.ITelephony;

import com.safecell.dataaccess.ContactRepository;
import com.safecell.model.Emergency.Emergencies;
import com.safecell.model.SCInterruption;
import com.safecell.networking.ConfigurationHandler;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.Util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

/**
 * @author uttama
 * 
 */
public class OutgoingCallReceiver extends BroadcastReceiver {

	private Context mContext;

	private TelephonyManager telephonyManager;

	private ITelephony telephonyService;

	private String TAG = OutgoingCallReceiver.class.getSimpleName();

	public static boolean EMERGENCY_SAVE_TRIP = false;

	@Override
	public void onReceive(Context context, Intent intent) {

		Bundle bundle = intent.getExtras();

		if (null == bundle)
			return;
		this.mContext = context;

		Log.d(TAG, "Intent action: " + intent.getAction());
		String phonenumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
		Log.d(TAG, "Outgoing Call = " + phonenumber);
		SharedPreferences preferences = context.getSharedPreferences("TRIP", 1);

		boolean isTripStarted = preferences.getBoolean("isTripStarted", false);
		if(isTripStarted && new ConfigurePreferences(mContext).isTripAbandon()) {
			Log.v(TAG, "Outgoing call received when trip abandon");
			return;
		}
		if (isTripStarted && !TrackingService.ignoreLocationUpdates) {

			if (ConfigurationHandler.getInstance().getConfiguration()
					.isDisableCall()) {
				telephonyManager = (TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
				connectToTelephonyService();
				if (intent.getAction().equals(
						"android.intent.action.NEW_OUTGOING_CALL")) {
					if (!isEmergencyNumber(phonenumber)) {

						try {
							Log.d(TAG, "Ending Outgoing Call");
							setResultData(null);
							telephonyService.endCall();
						} catch (RemoteException e) {
							e.printStackTrace();
						}

					} else {
						Log.v(TAG,
								"Allowing emergency outgoing call & activate emergency trip save");
						// Activate trip save as background
						new ConfigurePreferences(context)
								.setEmergencyTripSave(true);
						// Util.saveInterruption(context, SCInterruption.EMEO);
					}
				}
			} else {
				if (isEmergencyNumber(phonenumber)) {
					Log.v(TAG,
							"Allowing emergency outgoing call & activate emergency trip save");
					// Activate trip save as background

					new ConfigurePreferences(context)
							.setEmergencyTripSave(true);
					// Util.saveInterruption(context, SCInterruption.EMEO);

				} else {
					if (!new ConfigurePreferences(mContext).isTripAbandon()) {
						Toast.makeText(mContext, "Outgoing call interruption",
								Toast.LENGTH_LONG).show();
						Util.saveInterruption(context, SCInterruption.CALL);
					}
				}

			}
		}
	}

	@SuppressWarnings("unchecked")
	private void connectToTelephonyService() {
		try {
			Class c = Class.forName(telephonyManager.getClass().getName());
			Method m = c.getDeclaredMethod("getITelephony");
			m.setAccessible(true);
			// telephonyService = (ITelephony)m.invoke(tm);
			telephonyService = (ITelephony) m.invoke(telephonyManager);

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("call prompt",
					"FATAL ERROR: could not connect to telephony subsystem");
			Log.e("call prompt", "Exception object: " + e);
			// finish();
		}
	}

	public boolean isEmergencyNumber(String number) {
		boolean isEmergencyNumber = false;
		Cursor cursor = mContext.getContentResolver().query(
				Emergencies.CONTENT_URI, new String[] { Emergencies.NUMBER },
				null, null, null);

		if (cursor == null) {
			Log.d(TAG, "Contact provider cursor is null");
			ContactRepository contactrepo = new ContactRepository(mContext);
			cursor = contactrepo.SelectContacts();
		}
		if (number != null && cursor != null) {
			cursor.moveToFirst();
			for (int i = 0; cursor != null && i < cursor.getCount(); i++) {
				String emergency_number = cursor.getString(cursor
						.getColumnIndex(Emergencies.NUMBER));
				Log.d(TAG, "**** Emergency number " + i + " = "
						+ emergency_number + " and normal number is " + number);
				if (number.equals(emergency_number)) {
					isEmergencyNumber = true;
					break;
				}
				cursor.moveToNext();
			}
			cursor.close();
		}
		return isEmergencyNumber;

	}

}
