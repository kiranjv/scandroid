/*******************************************************************************
 * Util.java.java, Created: May 1, 2012
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

package com.safecell.utilities;

import com.safecell.dataaccess.InteruptionRepository;
import com.safecell.dataaccess.TempTripJourneyWayPointsRepository;
import com.safecell.model.Configuration;
import com.safecell.model.SCInterruption;
import com.safecell.networking.ConfigurationHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * @author uttama
 */
public class Util {

	private static String TAG = Util.class.getSimpleName();

	/** Default Network status details */
	private static boolean NETWORK_BLOCKED = true;

	public static boolean getNETWORK_BLOCKED() {
		return NETWORK_BLOCKED;
	}

	public static void setNETWORK_BLOCKED(boolean nETWORK_STATUS) {
		NETWORK_BLOCKED = nETWORK_STATUS;
	}

	/**
	 * Disable and enable the mobile network.
	 */
	public static void setMobileDataEnabled(Context context, boolean enabled) {

		SharedPreferences preferences = context.getSharedPreferences("TRIP", 1);
		boolean isTripStarted = preferences.getBoolean("isTripStarted", false);
		if (isTripStarted) {
			Configuration config = ConfigurationHandler.getInstance()
					.getConfiguration();
			if (config.isDisableWeb()) {
				NETWORK_BLOCKED = true;
				final ConnectivityManager conman = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				try {
					Class conmanClass = Class.forName(conman.getClass()
							.getName());
					final Field iConnectivityManagerField = conmanClass
							.getDeclaredField("mService");
					iConnectivityManagerField.setAccessible(true);
					final Object iConnectivityManager = iConnectivityManagerField
							.get(conman);
					final Class iConnectivityManagerClass = Class
							.forName(iConnectivityManager.getClass().getName());
					final Method setMobileDataEnabledMethod = iConnectivityManagerClass
							.getDeclaredMethod("setMobileDataEnabled",
									Boolean.TYPE);
					setMobileDataEnabledMethod.setAccessible(true);
					Log.d(TAG, "Mobile network = " + enabled);
					setMobileDataEnabledMethod.invoke(iConnectivityManager,
							enabled);
				} catch (ClassNotFoundException e) {
					setMobileDataEnabled(context, enabled);
					Log.d(TAG, "Exception Occurred : ClassNotFoundException");
				} catch (NoSuchFieldException e) {
					setMobileDataEnabled(context, enabled);
					Log.d(TAG, "Exception Occurred : NoSuchFieldException");

				} catch (IllegalArgumentException e) {
					setMobileDataEnabled(context, enabled);
					Log.d(TAG, "Exception Occurred : IllegalArgumentException");

				} catch (IllegalAccessException e) {
					setMobileDataEnabled(context, enabled);
					Log.d(TAG, "Exception Occurred : IllegalAccessException");

				} catch (NoSuchMethodException e) {
					setMobileDataEnabled(context, enabled);
					Log.d(TAG, "Exception Occurred : NoSuchMethodException");

				} catch (InvocationTargetException e) {
					setMobileDataEnabled(context, enabled);
					Log.d(TAG, "Exception Occurred : InvocationTargetException");

				}
			}
		} else {
			NETWORK_BLOCKED = false;
		}
	}

	public synchronized static void saveInterruption(Context context,
			String type) {
		SharedPreferences preferences = context.getSharedPreferences("TRIP", 1);
		boolean isTripStarted = preferences.getBoolean("isTripStarted", false);
		if (isTripStarted) {
			TempTripJourneyWayPointsRepository repos = new TempTripJourneyWayPointsRepository(
					context);
			Cursor cursor = repos.getTrip();
			Log.d(TAG, "Trip count " + cursor.getCount());
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToLast();
				SCInterruption scInterruption = new SCInterruption();
				scInterruption.setStarted_at(DateUtils.getTimeStamp(new Date()
						.getTime()));
				scInterruption
						.setLatitude(Double.toString(cursor.getDouble(2)));
				scInterruption
						.setLongitude(Double.toString(cursor.getDouble(3)));
				scInterruption.setEstimatedSpeed(Double.toString(repos
						.getAvarageEstimatedSpeedForAutoTripStart()));
				scInterruption.setSchooleZoneActive(false);
				scInterruption.setPhoneRuleActive(false);
				scInterruption.setSmsRuleActive(false);
				scInterruption.setType(type);
				InteruptionRepository interuptionRepository = new InteruptionRepository(
						context);
				interuptionRepository.insertInterupt(scInterruption);
				// Toast.makeText(context, "Interruption Captured : Type = " +
				// type, Toast.LENGTH_LONG)
				// .show();

			}
		}

	}

	public static long minitToMilliSeconds(long minits) {
		return (minits * 60000);
	}

	public static long minitToSeconds(long minits) {
		return (minits * 1000);
	}

	public static long milliToMinits(long millisces) {
		return (millisces / 60000);
	}

	public static long milliToSeconds(long millsecs) {
		return (millsecs / 1000);
	}

	public static String getTaskName(String package_name) {
		StringTokenizer tokenizer = new StringTokenizer(package_name);
		String taskname = null;
		while (tokenizer.hasMoreElements()) {
			taskname = tokenizer.nextToken(".");

		}
		System.out.println("Task Name: " + taskname);
		return taskname;
	}
}
