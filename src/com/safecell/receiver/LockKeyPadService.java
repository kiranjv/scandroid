package com.safecell.receiver;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.safecell.HomeScreenActivity;
import com.safecell.PhoneReceiver;
import com.safecell.TrackingScreenActivity;
import com.safecell.TrackingService;
import com.safecell.model.SCInterruption;
import com.safecell.networking.ConfigurationHandler;

import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.UIUtils;
import com.safecell.utilities.Util;

/**
 * Keypad pad lock service
 * 
 */
public class LockKeyPadService {

	private final static String TAG = LockKeyPadService.class.getSimpleName();
	private static boolean lock_status = false;
	private static Context mContext;
	private final static long MONITER_TIMER = 500;

	/**
	 * Activate the keypad lock based on activate boolean value and trip started
	 * preference value..
	 * 
	 * @param activate
	 *            - true represent active locking service. false represent
	 *            deactivate the service.
	 */
	public static void activateKeyPadLock(final Context context,
			boolean activate) {
		mContext = context;
		lock_status = activate;
		new Thread(new Runnable() {
			private boolean SHOW_SCREEN = true;
			private String lasttask = "";

			@Override
			public void run() {
				try {
					while (lock_status) {
						Thread.sleep(MONITER_TIMER);
						ActivityManager imm = (ActivityManager) context
								.getSystemService(Context.ACTIVITY_SERVICE);
						RunningTaskInfo taskInfo = imm.getRunningTasks(100)
								.get(0);
						String taskpackage = taskInfo.baseActivity
								.getPackageName();
//						Log.d(TAG, "Task package = " + taskpackage);

						if (taskpackage.equalsIgnoreCase(lasttask)) {
							// Log.d(TAG, "This is previous package");
						} else if (taskpackage
								.equalsIgnoreCase("com.android.mms")) {
							if (ConfigurationHandler.getInstance()
									.getConfiguration().isDisableTexting()) {
								startDisplayActivityAgain(context);
								Log.d(TAG,
										"Got the SMS service. SMS config is enable");
							}
						} else if (taskpackage
								.equalsIgnoreCase("com.android.browser")) {
							if (ConfigurationHandler.getInstance()
									.getConfiguration().isDisableWeb()) {
								startDisplayActivityAgain(context);
								Log.d(TAG,
										"Got the WEB service. WEB config is enable");
							}
						}

						else if (taskpackage
								.equalsIgnoreCase("com.android.email")) {

							if (ConfigurationHandler.getInstance()
									.getConfiguration().isDisableEmail()) {
								startDisplayActivityAgain(context);
								Log.d(TAG,
										"Got the email service. email config is enable");
							} else {

								Log.d(TAG,
										"Got the email service. email config is disable");

								new Thread(new Runnable() {

									@Override
									public void run() {
										Looper.prepare();
										if(!new ConfigurePreferences(mContext).isTripAbandon())
										Toast.makeText(TrackingService.context,
												"Email interruption",
												Toast.LENGTH_LONG).show();
										Looper.loop();

									}
								}).start();

								Util.saveInterruption(context,
										SCInterruption.EMAIL);

							}
						} else if (taskpackage
								.equalsIgnoreCase("com.android.contacts")) {
							Log.d(TAG, "Got the contacts service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().isDisableCall()) {
								startDisplayActivityAgain(context);
							} else {
								Util.saveInterruption(context,
										SCInterruption.VIOLATION);
							}
						} else if (taskpackage
								.equalsIgnoreCase("com.google.android.googlequicksearchbox")) {
							Log.d(TAG, "Got the search service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getKeypadlock()) {
								startDisplayActivityAgain(context);
							} else {
								Util.saveInterruption(context,
										SCInterruption.VIOLATION);
							}

						} else if (taskpackage
								.equalsIgnoreCase("com.htc.launcher")) {
						
							Log.d(TAG, "Got the launcher service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getKeypadlock()) {
								startDisplayActivityAgain(context);
							} else {
								Util.saveInterruption(context,
										SCInterruption.VIOLATION);
							}

						} else if (taskpackage
								.equalsIgnoreCase("com.sec.android.app.twlauncher")) {
							Log.d(TAG, "Got the twlauncher service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getKeypadlock()) {
								startDisplayActivityAgain(context);
							} else {
								Util.saveInterruption(context,
										SCInterruption.VIOLATION);
							}

						}
						else if (taskpackage
								.equalsIgnoreCase("com.android.launcher")) {
							Log.d(TAG, "Got the launcher service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getKeypadlock()) {
								startDisplayActivityAgain(context);
							} else {
								Util.saveInterruption(context,
										SCInterruption.VIOLATION);
							}

						} 
						else {

							// Log.d(TAG, "Got the other service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getSplashShow()
									&& !taskpackage
											.equalsIgnoreCase("com.android.phone")) {
								//startDisplayActivityAgain(context);
							}
//							Util.saveInterruption(context,
//									SCInterruption.VIOLATION);

						}
						lasttask = taskpackage;

					}
				} catch (Exception e) {
					Log.e(TAG,
							"Error in Keypad lock service: " + e.getMessage());
					e.printStackTrace();
				}
			}

			private void startDisplayActivityAgain(Context context) {

				// Intent intent = new Intent(TrackingService.context,
				// TrackingScreenActivity.class);
				// intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
				// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// TrackingService.context.startActivity(intent);

				if (!PhoneReceiver.EMERGENCYCALL_ACTIVE) {
					if (ConfigurationHandler.getInstance().getConfiguration()
							.getSplashShow()
							&& new ConfigurePreferences(context)
									.getTripStrated()) {
						Log.d(TAG, "Displaying tracking activity again..");
						TrackingScreenActivity.KEYPAD_LOCK_DESTROY = true;
						Intent intent = new Intent(TrackingService.context,
								TrackingScreenActivity.class);
						// intent.setAction(Intent.ACTION_VIEW);

						// intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
						// intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						TrackingService.context.startActivity(intent);
					} else if(ConfigurationHandler.getInstance().getConfiguration().getKeypadlock()) {
						Log.d(TAG, "Displaying home screen activity again..");
						HomeScreenActivity.KEYPAD_LOCK_DESTROY = true;
						Intent intent = new Intent(TrackingService.context,
								HomeScreenActivity.class);

						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						TrackingService.context.startActivity(intent);
					}

				} else {
					PhoneReceiver.EMERGENCYCALL_ACTIVE = false;
				}

			}

		}).start();
	}

	public static void deactivateKeyPadLock() {
		lock_status = false;
	}

	public static boolean isLockActivated() {
		return lock_status;
	}

}
