package com.safecell.receiver;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.safecell.TrackingScreenActivity;
import com.safecell.TrackingService;
import com.safecell.model.SCInterruption;
import com.safecell.networking.ConfigurationHandler;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.TAGS;
import com.safecell.utilities.Util;

public class BlockSMSService {
	private final static String TAG = BlockSMSService.class.getSimpleName();
	private static boolean lock_status = false;
	private final static long MONITER_TIMER = 500;

	/**
	 * Activate the SMS block based on activate boolean value and trip started
	 * preference value..
	 * 
	 * @param activate
	 *            - true represent active sms block service. false represent
	 *            deactivate the service.
	 */
	public static void activateSMSBlock(final Context context, boolean activate) {
		lock_status = activate;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (lock_status) {
						Thread.sleep(MONITER_TIMER);
						// Log.d(TAG, "SMS Block Service running...");

						ActivityManager imm = (ActivityManager) context
								.getSystemService(Context.ACTIVITY_SERVICE);
						RunningTaskInfo taskInfo = imm.getRunningTasks(100)
								.get(0);

						String taskpackage = taskInfo.baseActivity
								.getPackageName();
						// Log.d(TAG, "Task package = " + taskpackage);

						if (taskpackage.equalsIgnoreCase("com.android.mms") || taskpackage.contains("mms")) {

							if (TAGS.disableTexting) {
								Util.saveInterruption(context,
										SCInterruption.ATTEMPVIOLATION);
								startDisplayActivityAgain(context);
								Log.d(TAG,
										"Got the SMS ATTEMPTED VIOLATION . SMS config is enable");
							}

						}

					}
				} catch (Exception e) {
					Log.e(TAG, "Error in SMS BLOCK service: " + e.getMessage());
					e.printStackTrace();
				}
			}

			private void startDisplayActivityAgain(Context context) {

				if (ConfigurationHandler.getInstance().getConfiguration()
						.getSplashShow()
						&& new ConfigurePreferences(context).getTripStrated()) {
					Log.d(TAG, "Overridding SMS service ");
					Intent intent = new Intent(TrackingService.context,
							TrackingScreenActivity.class);
					// intent.setAction(Intent.ACTION_VIEW);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					// intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					context.startActivity(intent);
				} else {
					Log.v(TAG, "Spalsh show flag is false");
				}

			}

		}).start();
	}

	public static void deactivateSMSBlock() {
		lock_status = false;
	}

	public static boolean isSMSBlocked() {
		return lock_status;
	}

}
