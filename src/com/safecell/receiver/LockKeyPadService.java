package com.safecell.receiver;

import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LongSparseArray;
import android.widget.Toast;

import com.safecell.HomeScreenActivity;
import com.safecell.PhoneReceiver;
import com.safecell.TrackingScreenActivity;
import com.safecell.TrackingService;
import com.safecell.model.SCInterruption;
import com.safecell.networking.ConfigurationHandler;

import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.TAGS;
import com.safecell.utilities.UIUtils;
import com.safecell.utilities.Util;

/**
 * Keypad pad lock service
 * 
 */
public class LockKeyPadService {
	static private final Logger logger = LoggerFactory
			.getLogger(LockKeyPadService.class);
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
						Log.d(TAG, "Task package = " + taskpackage);
						logger.info("Task package = " + taskpackage);
						if (taskpackage.equalsIgnoreCase(lasttask)) {
							// Log.d(TAG, "This is previous package");
						} else if (taskpackage.equalsIgnoreCase("com.safecell")) {
							Log.d(TAG, "com.safecell task");
						} else if (taskpackage.contains("mms")
								|| taskpackage.contains("messaging")) {
							if (TAGS.disableTexting) {
								Util.saveInterruption(context,
										"ATTEMPTED VIOLATION-MMS");
								startDisplayActivityAgain(context);
								Log.d(TAG,
										"Got the SMS ATTEMPTED VIOLATION . SMS config is enable");
							}
						} else if (taskpackage.contains("browser")
								|| taskpackage.contains("chrome")) {
							Log.d(TAG, "Got browser service.");

							if (TAGS.disableWeb) {
								Util.saveInterruption(context,
										"ATTEMPTED VIOLATION-WEB");
								startDisplayActivityAgain(context);
								Log.d(TAG,
										"Got the WEB service. WEB config is enable. Attempted violation recorded");
								logger.debug("Got the WEB service. WEB config is enable. Attempted violation recorded");
							} else {
								//dconfig false case
								Util.saveInterruption(context,
										"WEB");
								Toast.makeText(mContext, "WEB Interruption.", Toast.LENGTH_SHORT).show();
								Log.d(TAG,
										"WEB Interruption");
								logger.debug("Web interruption recorded");
							}
						}

						else if (taskpackage.contains("email")
								|| taskpackage.contains("motoemail")) {
							Log.d(TAG, "Got email service.");
							Log.d(TAG, "Email config flag status: "
									+ TAGS.disableEmail);
							if (TAGS.disableEmail) {
								Util.saveInterruption(context,
										"ATTEMPTED VIOLATION-EMAIL");
								startDisplayActivityAgain(context);
								Log.d(TAG,
										"Got the email service. email config is enable. Attempted violation recorded");
								logger.debug("Got the email service. email config is enable. Attempted violation recorded");
							} else {

								Log.d(TAG,
										"Got the email service. email config is disable");
								logger.debug("Got the email service. email config is disable. Recording email interruption..");
								new Thread(new Runnable() {

									@Override
									public void run() {
										Looper.prepare();
										if (!new ConfigurePreferences(mContext)
												.isTripAbandon())
											Toast.makeText(
													TrackingService.context,
													"Email interruption",
													Toast.LENGTH_LONG).show();
										Looper.loop();

									}
								}).start();

								Util.saveInterruption(context,
										SCInterruption.EMAIL);

							}
						} else if (taskpackage.contains("contacts")) {

							Log.d(TAG, "Got the contacts service.");
							Log.d(TAG, "Disable call: " + TAGS.disableCall);
							if (TAGS.disableCall) {
								logger.debug("Got contacts service. Disable call enabled. Recording as Attempted violation. ");
								Util.saveInterruption(context,
										"ATTEMPTED VIOLATION-PHONE");
								Log.d(TAG, "Contacts config flag status: "
										+ TAGS.disableCall
										+ "Attempted violation recorded");
								startDisplayActivityAgain(context);
							} else {

							}
						} else if (taskpackage.contains("phone")) {
							Log.d(TAG, "Got the phone service.");
						}

						else if (TAGS.SHOW_SPLASH
								&& taskpackage
										.equalsIgnoreCase("com.google.android.apps.maps")) {

							Log.d(TAG, "Got the phone service.");
							logger.debug("Got MAPS interruption");
						} else if (taskpackage.contains("googlequicksearchbox")) {
							Log.d(TAG, "Got the search service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getKeypadlock()) {
								startDisplayActivityAgain(context);
							} else {

							}

						} else if (taskpackage.contains("launcher")
								|| taskpackage.contains("homescreen")) {

							Log.d(TAG, "Got the launcher service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getKeypadlock()) {
								startDisplayActivityAgain(context);
							} else {

							}

						} else if (taskpackage.contains("twlauncher")) {
							Log.d(TAG, "Got the twlauncher service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getKeypadlock()) {
								startDisplayActivityAgain(context);
							} else {

							}

						} else if (taskpackage
								.equalsIgnoreCase("com.android.launcher")) {
							Log.d(TAG, "Got the launcher service.");
							if (ConfigurationHandler.getInstance()
									.getConfiguration().getKeypadlock()) {
								startDisplayActivityAgain(context);
							} else {

							}

						} else {

							String taskName = Util.getTaskName(taskpackage);
							String violation = SCInterruption.VIOLATION + "-"
									+ taskName;
							Util.saveInterruption(context, violation);

							Log.d(TAG, "Got the VIOLATION interruption:"
									+ violation);
							logger.debug("Got the VIOLATION interruption:"
									+ violation);
							// Log.d(TAG, "Got the other service.");
							// if (ConfigurationHandler.getInstance()
							// .getConfiguration().getSplashShow()
							// && !taskpackage
							// .equalsIgnoreCase("com.android.phone")) {
							// startDisplayActivityAgain(context);
							// }
							// Util.saveInterruption(context,
							// SCInterruption.VIOLATION);

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
					if (TAGS.SHOW_SPLASH
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
					} else if (TAGS.keypadLock) {
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
