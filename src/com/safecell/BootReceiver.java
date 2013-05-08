package com.safecell;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.safecell.utilities.ConfigurePreferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

	static private final Logger logger = LoggerFactory
			.getLogger(BootReceiver.class);

	private static final String TAG = "BootReceiver";
	public static boolean SHUTDOWNSAVE = false;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i("BootReceiver", "Got intent " + intent.getAction() + " / data: "
				+ intent.getDataString());

		try {
			Log.v("Safecell :" + "Boot BroadcastReceiver", "Received");
			Log.v("BootReceiver", "isShutDown - "
					+ new ConfigurePreferences(context).isShutDown());
			logger.debug("isShutDown - "
					+ new ConfigurePreferences(context).isShutDown());
			Log.v(TAG, "Clearing emergency call flags");
			new ConfigurePreferences(context).setEmergencyTripSave(false);
			if (new ConfigurePreferences(context).isShutDown()) {
				Log.v("BootReceiver", "shutdown status: " + SHUTDOWNSAVE);
				SHUTDOWNSAVE = true;
			}

			activateNetwork(context, true);

			logger.debug("SHUTDOWN Flag: "+SHUTDOWNSAVE);
			Intent myStarterIntent = new Intent(context,
					SplashScreenActivity.class);
			myStarterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(myStarterIntent);

			// on boot up automatically start the service
			Intent mIntent = new Intent(context, TrackingService.class);
			context.startService(mIntent);
			// ServiceHandler.getInstance(context).bindService();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void activateNetwork(Context context, boolean enabled) {

		final ConnectivityManager conman = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			Class conmanClass = Class.forName(conman.getClass().getName());
			final Field iConnectivityManagerField = conmanClass
					.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			final Object iConnectivityManager = iConnectivityManagerField
					.get(conman);
			final Class iConnectivityManagerClass = Class
					.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass
					.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabledMethod.setAccessible(true);
			Log.d(TAG, "Mobile network = " + enabled);
			setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
		} catch (ClassNotFoundException e) {
			activateNetwork(context, enabled);
			Log.d(TAG, "Exception Occurred : ClassNotFoundException");
		} catch (NoSuchFieldException e) {
			activateNetwork(context, enabled);
			Log.d(TAG, "Exception Occurred : NoSuchFieldException");

		} catch (IllegalArgumentException e) {
			activateNetwork(context, enabled);
			Log.d(TAG, "Exception Occurred : IllegalArgumentException");

		} catch (IllegalAccessException e) {
			activateNetwork(context, enabled);
			Log.d(TAG, "Exception Occurred : IllegalAccessException");

		} catch (NoSuchMethodException e) {
			activateNetwork(context, enabled);
			Log.d(TAG, "Exception Occurred : NoSuchMethodException");

		} catch (InvocationTargetException e) {
			activateNetwork(context, enabled);
			Log.d(TAG, "Exception Occurred : InvocationTargetException");

		}

	}
}