package com.safecell;

import com.safecell.dataaccess.AccountRepository;
import com.safecell.dataaccess.DBAdapter;
import com.safecell.dataaccess.InteruptionRepository;
import com.safecell.dataaccess.ProfilesRepository;
import com.safecell.dataaccess.SMSRepository;
import com.safecell.dataaccess.TempTripJourneyWayPointsRepository;
import com.safecell.dataaccess.TripJsonRepository;
import com.safecell.model.SCProfile;
import com.safecell.model.SCSms;
import com.safecell.model.SCWayPoint;
import com.safecell.networking.ConfigurationHandler;
import com.safecell.networking.ExistingTripJsonHandler;
import com.safecell.networking.NetWork_Information;
import com.safecell.networking.SubmitNewTripJourney;
import com.safecell.networking.SubmitNewTripJourneyResponceHandler;
import com.safecell.networking.TripSyncHandler;
import com.safecell.receiver.BlockSMSService;
import com.safecell.receiver.LockKeyPadService;
import com.safecell.receiver.LockReceiver;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.DateUtils;
import com.safecell.utilities.DistanceAndTimeUtils;
import com.safecell.utilities.InformatonUtils;
import com.safecell.utilities.LocationSP;
import com.safecell.utilities.StateAddress;
import com.safecell.utilities.TAGS;
import com.safecell.utilities.UIUtils;
import com.safecell.utilities.Util;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.SumPathEffect;
import android.location.Criteria;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class TrackingService extends Service implements LocationListener,
		NmeaListener {

	static private final Logger logger = LoggerFactory
			.getLogger(TrackingService.class);

	public static final String GPS_PROVIDER_DISPLAY_TEXT = "GPS Satellite";
	public static final String NETWORK_PROVIDER_DISPLAY_TEXT = "Network";
	public static final String UNAVAILABLE_PROVIDER_DISPLAY_TEXT = "Unavailable";
	public static ArrayList<String> incomingNumberArrayList = new ArrayList<String>();
	public static final double WAYPOINT_TIME_CUT_OFF = 0.000833;
	public static String TAG = TrackingService.class.getSimpleName();
	public static TrackingService context;
	public static Location LOCATION_FOR_RULE = null;
	public static final int RESPONSE_NULL_WHILE_SAVING = 1;
	public static final int EXCEPTION_WHILE_SAVING = 2;
	public static String SELECTED_PROVIDER = null;
	private static int LOCATION_UPDATE_TIME_INTERVAL = 500; // 0 MilliSEC
	private static int LOCATION_UPDATE_DISTANCE_INTERVAL = 5; // 2 METERS
	private static Double latitude = 0.0, longitude = 0.0;
	private static Location currentLocation;

	static boolean isTripDeleted;
	private TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository;
	double speed = 0.0;
	private float lastDistanceInMiles = 0;
	private static int TRIP_CUT_OFF = 5;
	public static TrackingScreenActivity trackingScreenActivity = null;
	public static Context trackingScreenContext = null;
	private ArrayList<SCSms> smsArrayList = new ArrayList<SCSms>();
	private SMSRepository smsRepository;
	static boolean isTempTripDeleted = false;
	public static HomeScreenActivity homeScreenActivity;
	public static AddTripActivity addTripActivity = null;
	private boolean resultFlag;
	private ConfigurationRunnable configRunnable;

	public static boolean isTripRunning = false;
	private static final int IGNORE_WAYPOINT_SPEED_LIMIT = 150;
	SharedPreferences sharedPreferences;
	Handler handler;

	private static Handler handlerTimerTask;
	private static Runnable timerTaskRunner;

	private static Handler autoStartTripTimer;
	private static Runnable autoStarTripTimerRunner;

	private static Handler idleMonitorTimerHandler = null;
	private static Runnable idleMonitorTimerRunner = null;

	private static Timer weekOfDayMonitorTimer = null;
	private static TimerTask weekOfDayMonitorTimerTask = null;

	private static Handler idlePointsTimerHandler = null;
	private static Runnable idlePointsTimerRunner = null;
	private static int IDLE_POINTS_CLEAR_TIME = 2; // mints

	InteruptionRepository ir;

	private static final int AUTO_TRIP_START_SPEED_MONITOR_PERIOD = 5; // In
	// seconds
	private static final int AUTO_TRIP_START_REQUIRED_MIN_UPDATE_INTERVAL = 6; // In
	// seconds
	private static final int WAYPOINT_FILTER_TIME = 3; // In seconds use -1 to
	// turn this filtering
	// off

	private static long lastUpdateReceivedOn = 0;

	static int AUTO_SAVE_DELAY_MINUTE = 5;

	private int show_dialog_counter = 0;

	public static double currentPDOP = -1;
	public static double currentHDOP = -1;
	public double currentVDOP;
	private static final String NMEA_DOP_SENTENCE = "$GPGSA";
	public static boolean ABANDONFLAG = false;
	public static String gpsSource = UNAVAILABLE_PROVIDER_DISPLAY_TEXT;

	LocationSP locationSP;
	StateAddress stateAddress;

	public static boolean ignoreLocationUpdates = false;

	private static LocationManager locationManager;
	public static ArrayList<String> incomingSMSNumberArrayList = new ArrayList<String>();;

	private LockReceiver receiverScreenOff;

	private OutgoingSMSDetector mSmsDetector;

	private OutgoingWebDetector mWebDetector;

	private OutgoingEmailDetector mEmailDetector;
	private SchoolsDownload schoolsdownload = new SchoolsDownload();
	RulesDownload rulesdownload;
	private boolean APPLY_RULES_NEED = true;
	private MediaPlayer mediaPlayer;

	private static boolean NEED_RULES_DOWNLOAD = false;

	public static boolean iSpeechStarted = false;
	public static boolean AccountActive = true;

	private Handler BateryTimerHandler = new Handler();
	private Timer baterytimer = new Timer();
	private final int BATEERY_THRESHOLD_LOW = 8;

	IBinder mBinder = new LocalBinder();
	private Intent BATTERYintent = null;
	private boolean isTripStarted;

	private boolean isTripAbandoned = false;

	private boolean isWeekOfTimerSplashShow = false;
	public static boolean NO_INTERNET_SAVE = false;

	private static boolean isLockActivated = false;

	@Override
	public IBinder onBind(Intent intent) {
		// Log.d(TAG, "Bind Service Called");
		// // starting the tracking service
		// //ConfigurationHandler.getInstance().readResponse();
		// AUTO_SAVE_DELAY_MINUTE =
		// ConfigurationHandler.getInstance().getConfiguration().getTripStopTime();
		// TrackingService.ignoreLocationUpdates = false;
		// //setting the auto timer always on
		// SharedPreferences preferences = getSharedPreferences("TripCheckBox",
		// MODE_WORLD_WRITEABLE);
		// SharedPreferences.Editor editor= preferences.edit();
		// editor.putBoolean("isbackgroundtrip", true);
		// editor.commit();
		return mBinder;
	}

	public class LocalBinder extends Binder {
		public TrackingService getServerInstance() {
			return TrackingService.this;
		}
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "OnCreate");
		context = TrackingService.this;
		int count = 0;
		while (!NetWork_Information.isNetworkAvailable(context)) {
			Log.v(TAG, "Waiting for mobile network");
			if (count < 10) {
				waiting(2 * 1000);
				count++;
			} else {
				break;
			}
		}
		Log.v(TAG, "BootReceiver.SHUTDOWNSAVE = " + BootReceiver.SHUTDOWNSAVE);
		if (BootReceiver.SHUTDOWNSAVE) {
			Log.v(TAG,
					"SHUTDOWN save is active saving previous shutdown trip data");
			logger.debug("SHUTDOWN save is active saving previous shutdown trip data");
			TAGS.CURRENT_TRIPNAME = "Trip on " + getTodaysDate();
			Log.v(TAG, "Shutdown trip name: " + TAGS.CURRENT_TRIPNAME);
			Toast.makeText(context,
					"Shutdown trip name: " + TAGS.CURRENT_TRIPNAME,
					Toast.LENGTH_LONG).show();
			try {
				// DBAdapter dbAdapter = new DBAdapter(context);
				// dbAdapter.closeDatabase();
				// dbAdapter.open();
				SaveTrip saveTrip = new SaveTrip();
				saveTrip.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		// put logcat moniter service. The service will creates new log file
		// every time it started.

		configureLocationManager();

		smsRepository = new SMSRepository(context);

		handler = new Handler(Looper.getMainLooper());
		ir = new InteruptionRepository(TrackingService.this);
		handlerTimerTask = new Handler();

		HomeScreenActivity.genereateTripUniqueID();

		addIntentFilterForScreenOff();

		rulesdownload = new RulesDownload(context);

	}

	private void addIntentFilterForScreenOff() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		receiverScreenOff = new LockReceiver();
		registerReceiver(receiverScreenOff, filter);
	}

	public static void deleteLastInterruption() {
		if (context == null) {
			// Log.v("Safecell",
			// "deleteLastInterruption falied : Tracking service static context is null");
			return;
		}

		InteruptionRepository interuptionRepository = new InteruptionRepository(
				context);
		interuptionRepository.deleteLastInterruption();
	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		unregisterReceiver(receiverScreenOff);
		stopForeground(true);
		locationManager.removeUpdates(this);
		locationManager.removeNmeaListener(this);
		if (mSmsDetector != null) {
			mSmsDetector.stop();
		}
		if (mWebDetector != null) {
			mWebDetector.stop();
		}
		if (mEmailDetector != null) {
			mEmailDetector.stop();
		}

	}

	@Override
	public void onStart(Intent intent, int startId) {
		BATTERYintent = registerReceiver(null, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));
		sharedPreferences = getSharedPreferences("TRIP", MODE_PRIVATE);
		tempTripJourneyWayPointsRepository = new TempTripJourneyWayPointsRepository(
				TrackingService.this);
		context = TrackingService.this;
		AddTripActivity.trackingService = TrackingService.this;
		Log.d(TAG, "On Start called");
		if (currentLocation != null) {
			// Log.v("Tracking service", "curren Location is not Null");
			latitude = currentLocation.getLatitude();
			longitude = currentLocation.getLongitude();

			LOCATION_FOR_RULE = currentLocation;

			locationSP = new LocationSP(currentLocation, TrackingService.this);
			if (locationSP != null) {
				locationSP.setAddressLine();
				stateAddress = new StateAddress();
				stateAddress.setTextLocation();
			}
		}

		// sms detector initialization
		mSmsDetector = new OutgoingSMSDetector(new Handler(),
				getApplicationContext());
		mWebDetector = new OutgoingWebDetector(new Handler(),
				getApplicationContext());
		mEmailDetector = new OutgoingEmailDetector(new Handler(),
				getApplicationContext());

		startTrip();

		super.onStart(intent, startId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		foregroundService((String) getText(R.string.safe_cell));
		return START_STICKY;
	}

	/**
     * 
     */
	private void foregroundService(String text) {

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.launch_icon,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		/*
		 * PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new
		 * Intent(this, HomeScreenActivity.class), 0);
		 */
		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.safe_cell),
				text, contentIntent);

		startForeground(R.string.safe_cell, notification);
	}

	private void autoStartTrip() {
		Log.d(TAG, "Auto Start Trip Started ");
		logger.debug("Trip started.");
		// foregroundService("Auto trip started");
		// Toast.makeText(getApplicationContext(), "Auto Start Trip Started",
		// Toast.LENGTH_LONG).show();

		String tripName = "";
		tripName = "Trip On " + getTodaysDate();
		TAGS.CURRENT_TRIPNAME = tripName;
		Log.v(TAG, "Setting ShutDown Flag");
		new ConfigurePreferences(context).isShutDown(true);

		// tempTripJourneyWayPointsRepository.deleteTrip();
		// ir.deleteInteruptions();

		String timestamp = String.valueOf(System.currentTimeMillis());
		String key = SCProfile.newUniqueDeviceKey();

		new ConfigurePreferences(context).setOrgonizerToken((timestamp + key));

		/** change unique ID for trip saving **/
		if (HomeScreenActivity.genereateTripUniqueID().equalsIgnoreCase("")) {

			// String timestamp = String.valueOf(System.currentTimeMillis());
			// String key = SCProfile.newUniqueDeviceKey();
			// String substr = uniqueTripId.substring(timestamp.length(),
			// uniqueTripId.length());
			// String unique_timeTripID = timestamp + substr;
			HomeScreenActivity.editGenereateTripUniqueID(timestamp + key);
		}

		new TempTripJourneyWayPointsRepository(context).deleteTripWaypoints();
		new InteruptionRepository(context).deleteInteruptions();

		SharedPreferences.Editor editor = getSharedPreferences("TRIP",
				MODE_WORLD_READABLE).edit();
		editor.putBoolean("isTripStarted", true);
		editor.commit();

		TrackingService.isTripRunning = true;

		// activate lock if trip not abandon

		if (!new ConfigurePreferences(context).isTripAbandon()) {

			// activate locks
			Log.v(TAG, "phone locks activated...");
			logger.debug("phone locks activated...");
			activatePhoneLocks();

		} else {
			TrackingService.isLockActivated = false;
			Log.v(TAG, "Abandon trip no locks activated...");
			logger.debug("Abandon trip no locks activated...");
		}

		Log.v("Safecell", "**autoStartTrip");

	}

	private void activatePhoneLocks() {

		// start the observer based on configuration
		if (mSmsDetector != null) {

			if (TAGS.disableTexting && !BlockSMSService.isSMSBlocked()
					&& !TAGS.SHOW_SPLASH && TAGS.keypadLock) {
				Log.v(TAG, "Activationg SMS blocking service");
				BlockSMSService.activateSMSBlock(context, true);
			}
			Log.v(TAG, "Activating SMS Dectector");
			mSmsDetector.start();
		}
		if (!ConfigurationHandler.getInstance().getConfiguration()
				.isDisableWeb()
				&& mWebDetector != null) {
			Log.v(TAG, "Activating WEB Dectector");
			mWebDetector.start();
		}
		if (!ConfigurationHandler.getInstance().getConfiguration()
				.isDisableEmail()
				&& mEmailDetector != null) {
			Log.v(TAG, "Activating EMail Dectector");
			mEmailDetector.start();
		}

		new ConfigurePreferences(context).setSAVETRIP(true);
		new ConfigurePreferences(context).isTripAbandon(false);

		if (ConfigurationHandler.getInstance().getConfiguration()
				.isDisableWeb()) {
			// Disable Mobile network
			Log.v(TAG, "Disabling mobile network");
			Util.setMobileDataEnabled(context, false);
		}

		// Lock the keypad buttons
		LockKeyPadService.activateKeyPadLock(context, true);
		Log.v(TAG, "Keypad lock service started...");
		TrackingService.isLockActivated = true;

	}

	private static void cancelAutoTripStartTimer() {
		if (autoStartTripTimer != null) {
			autoStartTripTimer.removeCallbacks(autoStarTripTimerRunner);
			autoStartTripTimer = null;
			Log.v("Safecell", "**Trip Autostart cancelled");

		}
	}

	private void setAutoStartTripTimer() {
		autoStarTripTimerRunner = new Runnable() {
			public void run() {
				if (lastUpdateReceivedOn == 0) {
					Log.e(TAG, "Last recieved on time = "
							+ lastUpdateReceivedOn);
					// "WARNING: lastUpdateReceivedOn was zero. This should never reach here.");
					autoStartTripTimer = null;
					return;
				}

				else {
					long currentTime = new Date().getTime();
					if ((currentTime - lastUpdateReceivedOn) >= (AUTO_TRIP_START_REQUIRED_MIN_UPDATE_INTERVAL * 1000)) {
						// Log.d(TAG,
						// "Auto Trip Start Timer fired but trip was not started: last update time too big.");
						//
						// autoStartTripTimer = null;
						// return;
					}
					// else if (!tempTripJourneyWayPointsRepository
					// .isAvarageTimeDiffFeasible()) {
					// Log.d(TAG,
					// "Auto Trip Start Timer fired but trip was not started: isAvarageTimeDiffFeasible is false.");
					// autoStartTripTimer = null;
					// return;
					// }
				}

				// // put the time difference check.
				TempTripJourneyWayPointsRepository waypointRepo = new TempTripJourneyWayPointsRepository(
						context);
				double firstWayPointTimeDiffernce = waypointRepo
						.getFirstWayPointTimeDiffernce(System
								.currentTimeMillis());

				if (firstWayPointTimeDiffernce < TAGS.FALSE_TRIP_TIME_THRESHOLD) {

					// check day of week
					if (checkDayOfWeek()) {

						if (!isTripTimeAllowed()) {

							Log.e(TAG, "WeekOfDay trip not allowed");

							// Disable SAVE TRIP Flag.
							new ConfigurePreferences(context)
									.setSAVETRIP(false);
							Log.v(TAG, "SAVE TRIP Disabled ");
							Toast.makeText(context, "SAVE TRIP Disabled",
									Toast.LENGTH_LONG).show();

							// Set abandon flag in preferences
							new ConfigurePreferences(context)
									.isTripAbandon(true);
							Log.v(TAG, "Trip is Abandoned");
							Toast.makeText(context, "Trip is Abandoned",
									Toast.LENGTH_LONG).show();
							TrackingService.ABANDONFLAG = true;

							// create a week of day timer

							/* Create timer to monitor device idle conditions. */
							isWeekOfTimerSplashShow = false;
							setWeekOfDaySplashTimer();

						}

					}
					// normal day
					autoStartTrip();

					Log.v(TAG, "Auto Trip Started. Time Difference: "
							+ firstWayPointTimeDiffernce + " mins");
					logger.info("Auto Trip Started. Time Difference: "
							+ firstWayPointTimeDiffernce + " mins");
				} else {
					Log.v(TAG, "FALSE TRIP IDENTIFIED. Time Difference: "
							+ firstWayPointTimeDiffernce + " mins");
					logger.info("FALSE TRIP IDENTIFIED. Time Difference: "
							+ firstWayPointTimeDiffernce + " mins");

					// delete the waypoints
					waypointRepo.deleteTripWaypoints();

					// start the service again
					startTrip();
				}

				autoStartTripTimer = null;
			}

		};

		if (autoStartTripTimer != null) {
			cancelAutoTripStartTimer();
		}

		autoStartTripTimer = new Handler();
		autoStartTripTimer.postDelayed(autoStarTripTimerRunner,
				Util.minitToSeconds(AUTO_TRIP_START_SPEED_MONITOR_PERIOD));
		Log.v("Safecell", "**Trip Autostart Monitering started");
		// Toast.makeText(context, "Start trip timmer activated.",
		// Toast.LENGTH_LONG).show();
	}

	/** Set timer to auto stop trip **/

	private void setAutostopTripTimer() {
		Log.d(TAG, "Auto time stop ");
		timerTaskRunner = new Runnable() {
			public void run() {
				try {
					Log.v(TAG, "Auto Save Trip Called for timer id "
							+ timerTaskRunner.hashCode());
					saveTrip(getApplicationContext());
				} catch (Exception e) {
					e.printStackTrace();
				}
				handlerTimerTask = null;
			}
		};

		if (handlerTimerTask != null) {
			handlerTimerTask.removeCallbacks(timerTaskRunner);
			handlerTimerTask = null;
			// Log.v("Safecell", "**Auostop Timer Canceled");
		}
		handlerTimerTask = new Handler();
		// handlerTimerTask.postDelayed(timerTaskRunner, AUTO_SAVE_DELAY_MINUTE
		// * 60 * 1000 - 2 * 1000);
		handlerTimerTask.postDelayed(timerTaskRunner,
				Util.minitToMilliSeconds(AUTO_SAVE_DELAY_MINUTE));
		Log.v("Tracking Service", "**Auto Stop Timer Started timer id "
				+ timerTaskRunner.hashCode());
		// Toast.makeText(context, "Stop trip timmer activated.",
		// Toast.LENGTH_LONG).show();

	}

	public void waiting(int n) {
		long t0, t1;
		t0 = System.currentTimeMillis();
		do {
			t1 = System.currentTimeMillis();
		} while (t1 - t0 < n);
	}

	public static void cancelTripStopTimer() {
		if (handlerTimerTask != null) {
			handlerTimerTask.removeCallbacks(timerTaskRunner);
			handlerTimerTask = null;
			Log.v("Tracking Service", "**Timer Cancel");

		}
	}

	private void setDeviceIdleTimer() {
		Log.d("IdleTimer:", "Creating idle moniter timer..");
		idleMonitorTimerRunner = new Runnable() {

			@Override
			public void run() {
				checkDeviceIdleForLong();
			}
		};

		clearDeviceIdleTimer();
		idleMonitorTimerHandler = new Handler();
		idleMonitorTimerHandler.postDelayed(idleMonitorTimerRunner,
				Util.minitToMilliSeconds(TAGS.tripStopTime));
		Log.d("IdleTimer:",
				"Idle moniter timer created at: "
						+ DateUtils.getTimeStamp(System.currentTimeMillis()));
	}

	private void setWeekOfDaySplashTimer() {
		Log.d(TAG, "Creating week of day moniter timer..");
		logger.debug("Creating week of day moniter timer..");

		weekOfDayMonitorTimerTask = new TimerTask() {

			@Override
			public void run() {
				weekOfDayTimerFireMethod();
			}
		};

		weekOfDayMonitorTimer = new Timer();
		long interval = Util.minitToMilliSeconds(TAGS.recheckInterval);
		weekOfDayMonitorTimer.scheduleAtFixedRate(weekOfDayMonitorTimerTask,
				interval, interval);

		Log.d("IdleTimer:",
				"Idle moniter timer created at: "
						+ DateUtils.getTimeStamp(System.currentTimeMillis()));
	}

	private void clearWeekOfDaySplashTimer() {
		if (weekOfDayMonitorTimer != null) {
			weekOfDayMonitorTimer.cancel();

			Log.d(TAG,
					"Week of day Timer cleared at: "
							+ DateUtils.getTimeStamp(System.currentTimeMillis()));
			logger.debug("Week of day Timer cleared at: "
					+ DateUtils.getTimeStamp(System.currentTimeMillis()));
		}

	}

	private void setPointsIdleTimer() {
		Log.d("IdlePointTimer:", "Creating idle points moniter timer..");
		idlePointsTimerRunner = new Runnable() {

			@Override
			public void run() {
				/* check and clear idle points from database. */

			}
		};
		clearDeviceIdlePointsTimer();
		idlePointsTimerHandler = new Handler();
		idlePointsTimerHandler.postDelayed(idlePointsTimerRunner,
				Util.minitToMilliSeconds(IDLE_POINTS_CLEAR_TIME));
		Log.d("IdlePointTimer:", "Idle points moniter timer created at: "
				+ DateUtils.getTimeStamp(System.currentTimeMillis()));
	}

	private void clearDeviceIdleTimer() {
		if (idleMonitorTimerHandler != null) {
			idleMonitorTimerHandler.removeCallbacks(idleMonitorTimerRunner);
			idleMonitorTimerHandler = null;
			Log.d("IdleTimer:",
					"Idle moniter timer cleared at: "
							+ DateUtils.getTimeStamp(System.currentTimeMillis()));
		}
	}

	private void clearDeviceIdlePointsTimer() {
		if (idlePointsTimerHandler != null) {
			idlePointsTimerHandler.removeCallbacks(idlePointsTimerRunner);
			idlePointsTimerHandler = null;
			Log.d("IdlePointTimer:", "Idle points moniter timer cleared at: "
					+ DateUtils.getTimeStamp(System.currentTimeMillis()));

		}
	}

	protected void checkDeviceIdleForLong() {
		// clear idle timer
		clearDeviceIdleTimer();

		Log.d("IdleTimer:",
				"Idle moniter timer activated at: "
						+ DateUtils.getTimeStamp(System.currentTimeMillis()));
		Log.d("IdleTimer:", "Checking device idle time....");
		TempTripJourneyWayPointsRepository waypoint_store = new TempTripJourneyWayPointsRepository(
				context);

		long timeDiffernce = waypoint_store
				.getLastWaypointTimeDifference(System.currentTimeMillis());
		long seconds = Util.milliToSeconds(timeDiffernce);
		long minits = Util.milliToMinits(timeDiffernce);
		Log.d("IdleTimer:", "Idle time difference milli sec's: "
				+ timeDiffernce + " , Sec's: " + seconds + " , minits: "
				+ minits);

		if (minits >= TAGS.tripStopTime) {
			Log.d("IdleTimer:", "Call trip save method");
			saveTrip(context);
		} else {
			Log.d("IdleTimer:", "Need to continue trip start timer again");
			if (new ConfigurePreferences(context).getTripStrated()
					&& idleMonitorTimerHandler == null) {
				Log.d("IdleTimer:", "Setting another timer");
				setDeviceIdleTimer();
			}
		}

	}

	public void saveTripAsyncTask(final Context frontScreenActivity) {

		/*
		 * SharedPreferences.Editor editor = sharedPreferences.edit();
		 * editor.putBoolean("isTripPaused", false);
		 * editor.putBoolean("isTripStarted", false); editor.commit();
		 */
		new ConfigurePreferences(frontScreenActivity).setTripStrated(false);
		new ConfigurePreferences(frontScreenActivity).setTripPaused(false);

		String apiKey = "";
		int accountID = 0;
		int profileID = 0;

		int totalMiles = getTotalDistance();
		HashMap<Object, Object> ApikeyAndAccountId = new HashMap<Object, Object>();

		AccountRepository accountRepository = new AccountRepository(
				TrackingService.this);
		ApikeyAndAccountId = accountRepository.selectApiKeyAndAccountID();
		apiKey = (String) ApikeyAndAccountId.get("ApiKey");
		accountID = Integer.valueOf(ApikeyAndAccountId.get("AccountId")
				.toString());

		ProfilesRepository profilesRepository = new ProfilesRepository(
				TrackingService.this);
		profileID = profilesRepository.getId();

		TempTripJourneyWayPointsRepository tempWayPointRepo = new TempTripJourneyWayPointsRepository(
				TrackingService.this);
		InteruptionRepository interuptionRepository = new InteruptionRepository(
				TrackingService.this);

		SCProfile currentUser = profilesRepository.getCurrentProfile();
		String name = currentUser.getFirstName() + " "
				+ currentUser.getLastName();

		int TotalDistatnce = (int) tempWayPointRepo.getTotalDistance();

		Log.v(TAG, "Trip distance traveled: " + TotalDistatnce);

		double current_distance = TotalDistatnce
				+ new ConfigurePreferences(context).getPrevSyncMiles(); // TAGS.PREV_SYNC_MILES;

		Log.e(TAG, "Trip Current Final Distance: " + current_distance);
		logger.debug("Trip distance: " + TotalDistatnce
				+ " Trip current final distance: " + current_distance);
		if (BootReceiver.SHUTDOWNSAVE
				|| (!(current_distance < 1) && ConfigurationHandler
						.getInstance().getConfiguration().isLogWayPoints())) {

			Log.v(TAG, "Trip distance is more than 1km");

			logger.debug("Trip distance " + current_distance
					+ " is more than 1km.");
			TrackingScreenActivity.isTripSavingInProgress = true;

			insertIntoSMS();

			if (LockReceiver.wasLoacked) {
				TrackingService.deleteLastInterruption();
				// Log.v("SafeCell: ",
				// "LockReceiver.wasLoacked : Yes - Deleted the last interruption before save trip");
			}

			String firstInterruptionTime = interuptionRepository
					.firstInterruptionStartTime();
			long tripStartTime = tempWayPointRepo.tripStartTime();

			if (firstInterruptionTime != null) {

				String tripStartTimeStr = DateUtils.getTimeStamp(tripStartTime);

				if (firstInterruptionTime.equalsIgnoreCase(tripStartTimeStr)) {
					interuptionRepository.deleteFirstInterruption();
				}
			}

			SubmitNewTripJourney submitNewTripJourney = new SubmitNewTripJourney(
					getApplicationContext(), accountID, profileID, apiKey,
					TAGS.CURRENT_TRIPNAME, totalMiles, name);

			long createJsonStart = System.currentTimeMillis();
			// create trip logs json
			submitNewTripJourney.createJson();

			long createJsonEnd = System.currentTimeMillis();
			long createJsonTime = (createJsonEnd - createJsonStart) / 1000;
			Log.e(TAG, "Json create time: " + createJsonTime);
			logger.debug("Json creation time: " + createJsonTime);
			if (NO_INTERNET_SAVE) {
				Log.e(TAG, "NO_INTERNET_SAVE Json created: " + createJsonTime);
				TripJsonRepository tripJsonRepository = new TripJsonRepository(
						context);
				tripJsonRepository.saveJSON(submitNewTripJourney
						.getOuterJsonObject());

				int json_no = tripJsonRepository.getNumberOfTripJsons();
				Log.e(TAG, "TOTAL JSON IN DATABASE: " + json_no);

			}

			long sendHTTPStart = System.currentTimeMillis();

			// while (true) {
			// Log.v(TAG, "Checking network status");
			// if (NetWork_Information.isNetworkAvailable(context)) {
			// break;
			// }
			// }

			HttpResponse httpResponse = httpResponse = submitNewTripJourney
					.postRequest();
			long sendHTTPEnd = System.currentTimeMillis();
			long serverTime = (sendHTTPEnd - sendHTTPStart) / 1000;
			Log.e(TAG, "HTTP Process time: " + serverTime);
			Log.v(TAG, "IhttpResponse: " + httpResponse);
			logger.debug("Server process time: " + serverTime);
			if (httpResponse == null) {

				Log.e(TAG,
						"Unexepted error occure while saving the trip. Response:"
								+ httpResponse);
				logger.error("Server response is null");
				// showNotification("Unexepted error occure while saving the trip.");
				// tripNotSaveDialog(frontScreenActivity,
				// RESPONSE_NULL_WHILE_SAVING);
				resultFlag = false;

			} else {
				Log.v(TAG, " Response code:"
						+ httpResponse.getStatusLine().toString());

				logger.debug("Server response code: "
						+ httpResponse.getStatusLine().toString());
				SubmitNewTripJourneyResponceHandler submitNewTripJourneyResponceHandler = new SubmitNewTripJourneyResponceHandler(
						TrackingService.this);
				try {
					if (new ConfigurePreferences(context).isTripAbandon()) {
						Log.v(TAG, "Ignoring abandon trip save response");
						logger.debug("Ignoring abandon trip save response");
					} else {
						Log.d(TAG, "Parsing trip save response");
						logger.debug("Parsing trip save response");
						long parseRespStart = System.currentTimeMillis();
						submitNewTripJourneyResponceHandler
								.readResponce(httpResponse);
						long parseRespEnd = System.currentTimeMillis();
						long parseRespTime = (parseRespEnd - parseRespStart) / 1000;
						Log.e(TAG, "Parse response time: " + parseRespTime);
						logger.debug("Parse server response time: "
								+ parseRespTime);
					}

					resultFlag = true;
					tempTripJourneyWayPointsRepository.deleteTripWaypoints();
					Log.d(TAG, "Trip Saved ");
				} catch (Exception e) {
					resultFlag = false;
					e.printStackTrace();
				}

			}

		} else // when trip is small and timer fires
		{
			resultFlag = false;
			Log.d(TAG, "Trip too small to save");
			logger.debug("Trip too small to save");
			// // showNotification("Auto Save: Trip is small to save");
			SharedPreferences.Editor editor1 = sharedPreferences.edit();
			editor1.putBoolean("isTripPaused", false);
			editor1.putBoolean("isTripStarted", false);
			editor1.commit();
		}

	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean result = false;
		result = cm.getActiveNetworkInfo().isAvailable();
		// Log.v("result", "result =" + result);
		return result;

	}

	// @SuppressWarnings("unchecked")
	public void saveTrip(final Context context) {

		if (mSmsDetector != null) {
			mSmsDetector.stop();
		}
		if (mWebDetector != null) {
			mWebDetector.stop();
		}
		if (mEmailDetector != null) {
			mEmailDetector.stop();
		}
		removeLocationUpdates();

		Log.v(TAG, "Background service. Clear Idle moniter timer");
		clearDeviceIdleTimer();

		// Enable mobile network
		Log.v(TAG, "Enabling mobile network");
		Util.setMobileDataEnabled(context, true);
		int count = 0;
		Log.d(TAG,
				"Network Connections "
						+ NetWork_Information.isNetworkAvailable(context));

		/* Check network availability for 1 min */
		while (!NetWork_Information.isNetworkAvailable(context)) {
			if (count < 30) {
				waiting(2 * 1000);
				count++;
			} else {
				break;
			}
		}

		/* Activate flag after 1 min time. */
		if (!NetWork_Information.isNetworkAvailable(context)) {
			Log.e(TAG, "Enableing NO_INTERNET_SAVE flag");
			NO_INTERNET_SAVE = true;
		}
		// do {
		// waiting(2*1000);
		// } while (!NetWork_Information.isNetworkAvailable(context));

		if (TAGS.SHOW_SPLASH && TrackingScreenActivity.isBackground) {
			Log.v(TAG, "Getting Tracking screen into Focus");
			Intent intent = new Intent(TrackingService.this,
					TrackingScreenActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}

		if (NetWork_Information.isNetworkAvailable(context)) {
			Log.d(TAG, "Trip Saving");
			logger.debug("Trip saving.");
			if (!new ConfigurePreferences(context).isTripAbandon()) {
				Toast.makeText(TrackingService.context, "Saving Trip",
						Toast.LENGTH_LONG).show();
			}
			SaveTrip saveTrip = new SaveTrip();
			saveTrip.execute();
			// saveTripAsyncTask(context);
			// startTrip();
		} else {
			if (NO_INTERNET_SAVE) {
				Log.d(TAG, "Trip Saving withouot internet");
				logger.debug("Trip saving without internet.");
				if (!new ConfigurePreferences(context).isTripAbandon()) {
					Toast.makeText(TrackingService.context,
							"Trip Saving withouot internet", Toast.LENGTH_LONG)
							.show();
				}
				SaveTrip saveTrip = new SaveTrip();
				saveTrip.execute();
			}
		}

	}

	public static void showNotification(String msg) {
		if (TrackingService.homeScreenActivity != null) {
			homeScreenActivity.showNotification(msg);
		}
	}

	String getTodaysDate() {
		String today = "";
		SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy");
		Date currentTime_1 = new Date();
		today = formatter.format(currentTime_1);
		return today;
	}

	int getTotalDistance() {
		double totalDistance = 0d;
		TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository = new TempTripJourneyWayPointsRepository(
				TrackingService.this);
		totalDistance = tempTripJourneyWayPointsRepository.getTotalDistance();

		return (int) totalDistance;
	}

	private void reset() {
		Log.d(TAG, "reset Service");
		TrackingScreenActivity.isTripSavingInProgress = false;
		TrackingService.ignoreLocationUpdates = false;
		TrackingScreenActivity.IS_TRIP_PAUSED = false;
		TrackingService.isTripRunning = false;
		cancelAutoTripStartTimer();
		cancelTripStopTimer();
		mSmsDetector.stop();
		mWebDetector.stop();
		mEmailDetector.stop();
		System.gc();

	}

	private void set() {
		Log.d(TAG, "set Called ");
		ProfilesRepository profilesRepository = new ProfilesRepository(
				TrackingService.this);
		int profile_id = profilesRepository.getId();
		Log.d(TAG, "profile id : " + profile_id);

		// configRunnable = new ConfigurationRunnable(profile_id);
		// handler.postDelayed(configRunnable,2000);

		// download configuration form back end
		Log.d(TAG, "Downloading configuration..");
		new ConfigurationRunnable(profile_id).execute();

		TrackingService.ignoreLocationUpdates = false;
		// setting the auto timer always on
		SharedPreferences preferences = getSharedPreferences("TripCheckBox",
				MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("isbackgroundtrip", true);
		editor.commit();

	}

	@Override
	public boolean onUnbind(Intent intent) {
		// Log.d(TAG, "onUnbind Service");
		// TrackingScreenActivity.isTripSavingInProgress = false;
		// TrackingService.ignoreLocationUpdates = false;
		// TrackingScreenActivity.IS_TRIP_PAUSED = false;
		// cancelAutoTripStartTimer();
		// cancelTripStopTimer();
		// System.gc();
		return super.onUnbind(intent);
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public void onLocationChanged(Location location) {

		if (!TrackingService.AccountActive) {
			Log.d(TAG, "Account inactive ignore waypoints");
			return;
		}

		Log.d(TAG, "--------------------------------------------");
		Log.d(TAG, "Location Changed");
		Log.d(TAG, "Longitude  " + location.getLongitude() + " Latitude "
				+ location.getLatitude());
		logger.debug("-------------------------------------------");
		logger.debug("Longitude  " + location.getLongitude() + " Latitude "
				+ location.getLatitude() + " Time: "
				+ DateUtils.getTimeStamp(System.currentTimeMillis()));
		float accuracy = location.getAccuracy();

		Log.e(TAG, "Accuracy: " + accuracy + " Speed: " + location.getSpeed());
		logger.debug("Accuracy: " + accuracy + " Speed: " + location.getSpeed());

		
		// Ignore waypoints with accuracy more than 20
		if(accuracy > 20) {
			Log.d(TAG, "Igonoring way-point accuracy is more than 20..");
			logger.debug("Igonoring way-point accuracy is more than 20..");
			return;
		}
		
		
		// Ignore way point Emergency call in active and trip not started
		if (new ConfigurePreferences(context).getEmergencyTRIPSAVE()
				&& !new ConfigurePreferences(context).getTripStrated()) {
			Log.v(TAG, "EMERGENCY CALL IN ACTIVE - IGNORE WAY POINT");
			// Toast.makeText(
			// context,
			// "Emergency Call is Active. Ignoring location.",
			// Toast.LENGTH_SHORT)
			// .show();
			return;
		}

		// Change trip to abandon trip
		if (new ConfigurePreferences(context).isTripAbandon()) {
			// saveTrip(context);
			if (ABANDONFLAG) {
				Log.v(TAG, "Trip Abondon Activated. ");
				// foregroundService("Trip is Abondoned.");
				if (isLockActivated)
					makeTripAbandon(context);
				isLockActivated = false;
				ABANDONFLAG = false;

			}

		}

		// Emergency Trip Save Activated
		if (new ConfigurePreferences(context).getEmergencyTRIPSAVE()) {
			// Log.v(TAG, "Emergency Save Trip Activated. ");
			// saveTrip(context);
		}

		// check web disable. download rules & apply
		if (ConfigurationHandler.getInstance().getConfiguration()
				.isDisableWeb()
				&& NEED_RULES_DOWNLOAD) {
			enableWEB_Rule();
			if (TrackingService.isTripRunning) {
				NEED_RULES_DOWNLOAD = false;
			}
		}

		long currentTime = new Date().getTime();
		if (WAYPOINT_FILTER_TIME > 0) {
			if ((currentTime - lastUpdateReceivedOn) < (WAYPOINT_FILTER_TIME * 1000)) {
				Log.v(TAG, "Skipping the location " + location.getLongitude()
						+ " " + location.getAltitude());
				return;
			}
		}

		Log.v(TAG, "last recieved time = " + lastUpdateReceivedOn);
		lastUpdateReceivedOn = currentTime;

		if (TrackingService.ignoreLocationUpdates
				|| TrackingScreenActivity.isTripSavingInProgress) {
			Log.v(TAG, "Ignored Location Update");
			return;
		}

		if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
			gpsSource = GPS_PROVIDER_DISPLAY_TEXT;
		} else {
			gpsSource = NETWORK_PROVIDER_DISPLAY_TEXT;
		}

		Log.v(TAG, "location provider = " + gpsSource);

		if (lastDistanceInMiles == 0 || lastDistanceInMiles >= 1600) {

			locationSP = new LocationSP(location, TrackingService.this);
			if (locationSP != null) {
				locationSP.setAddressLine();
				stateAddress = new StateAddress();
				stateAddress.setTextLocation();
			}

			lastDistanceInMiles = 0;
		}
		if (currentLocation != null) {
			// distance from current location and prev location
			double curt_prev_distance = DistanceAndTimeUtils.distFrom(
					currentLocation.getLatitude(),
					currentLocation.getLongitude(), location.getLatitude(),
					location.getLongitude());

			Log.d(TAG, "current and prev location distance: "
					+ curt_prev_distance + " miles");
			logger.debug("current and prev location distance: "
					+ curt_prev_distance + " miles");

			if (curt_prev_distance >= TAGS.LOCATION_DISTANCE_THRESHOLD) {
				Log.d(TAG, "Ignoring sudden location update. ");
				logger.debug("Ignoring sudden location update. ");
			}

			lastDistanceInMiles += (location.distanceTo(currentLocation));
		}

		currentLocation = location;
		LOCATION_FOR_RULE = location;

		try {
			if (!ConfigurationHandler.getInstance().getConfiguration()
					.isDisableWeb()) {
				Log.v(TAG,
						"Disable web is false, Downloading rules while trip running");
				// download the rules
				if (new ConfigurePreferences(context).getTripStrated()
						&& trackingScreenActivity != null) {

					// Check rules for school zone

					schoolsdownload.locationChangedForSchool(LOCATION_FOR_RULE,
							context);
					boolean schoolZoneActive = schoolsdownload
							.schoolZoneActive(LOCATION_FOR_RULE);

					TrackingScreenActivity.updateSchoolUI(schoolZoneActive);

					// Check location rules

					rulesdownload.loactionChangedForRule(LOCATION_FOR_RULE,
							context);
					rulesdownload
							.updateRulesStatusAsPerSchoolZone(schoolZoneActive);

				}
			} else {
				Log.v(TAG,
						"Disable web is true, No rule download while trip running");
				// check already download rules applied on screen or.
				if (APPLY_RULES_NEED
						&& new ConfigurePreferences(context).getTripStrated()
						&& TrackingService.trackingScreenActivity != null) {
					Log.v(TAG, "Trip is started and apply default rules");
					rulesdownload.updateRulesStatusAsPerSchoolZone(false);
					APPLY_RULES_NEED = false;
				}
			}
		} catch (Exception e) {
			Log.e(TAG,
					"Exception while downloading rules in onlocation change: "
							+ e.getMessage());
			e.printStackTrace();
		}
		if (!TrackingScreenActivity.IS_TRIP_PAUSED
				|| !TrackingScreenActivity.isTripSavingInProgress) {
			try {
				insertWaypoint(location);
			} catch (Exception e) {
				Log.e(TAG, "Exception raised while insert waypoint");
				e.printStackTrace();
			}
		} else {
			Log.v(TAG, "InsertWayPoint *Trip Paused or Saving*");
		}

	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.e(TAG, "onProviderDisabled: SELECTED_PROVIDER: "
				+ SELECTED_PROVIDER + " provider " + provider);
		logger.info(" provider " + provider + " disabled");
		selectBestLocationProvider();

	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.e(TAG, "onProviderEnabled: SELECTED_PROVIDER: " + SELECTED_PROVIDER
				+ " provider " + provider);
		logger.info(" provider " + provider + " enabled");
		selectBestLocationProvider();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.e(TAG, "onStatusChanged: SELECTED_PROVIDER: " + SELECTED_PROVIDER
				+ " provider " + provider);
		if (provider.equals(SELECTED_PROVIDER)) {
			switch (status) {
			case LocationProvider.OUT_OF_SERVICE:
				Log.e(TAG, " provider " + provider + " out of service");
				logger.info(" provider " + provider + " out of service");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Log.e(TAG, " provider " + provider + " unavailable");
				logger.info(" provider " + provider + " unavailable");
				// showNotification("GPS updates are unavailable.");
				break;
			case LocationProvider.AVAILABLE:
				logger.info(" provider " + provider + " available");
				if (!InformatonUtils.isServiceRunning(getApplicationContext())) {
					Log.d(TAG, "Configuration");
					configureLocationManager();
					startTrip();
				}
				break;
			}
		}
	}

	private static void noGPSProviderAvailable() {
		SELECTED_PROVIDER = null;
		Log.v(TAG,
				"Location updates are disabled on your phone. Please enable location updates to use Safecell App.");
		// showNotification("Location updates are disabled on your phone. Please enable location updates to use Safecell App.");
	}

	public static void selectBestLocationProvider() {
		Log.v(TAG, "selecting the best provider");
		if (context == null) {
			return;
		}
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			requestGPSUpdates();
		} else if (locationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			Log.e(TAG, "NETWORK PROVIDER LOCATION REQUEST");
			requestNetworkLocationUpdates();
		} else {
			Log.e(TAG, "NO NETWORK PROVIDER AVAILABLE");
			noGPSProviderAvailable();
		}
	}

	private static void processLastKnownLocation() {
		currentLocation = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (currentLocation != null) {
			// Log.v("Tracking service", "curren Location is not Null");
			latitude = currentLocation.getLatitude();
			longitude = currentLocation.getLongitude();

			LOCATION_FOR_RULE = currentLocation;
			// Log.v("current location", currentLocation.toString());

		}
	}

	private static void requestGPSUpdates() {
		if (LocationManager.GPS_PROVIDER.equals(SELECTED_PROVIDER)) {
			return;
		} else if (SELECTED_PROVIDER != null) {
			locationManager.removeUpdates(context);
		}

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				LOCATION_UPDATE_TIME_INTERVAL,
				LOCATION_UPDATE_DISTANCE_INTERVAL, context);

		// locationManager.requestLocationUpdates(LOCATION_UPDATE_TIME_INTERVAL,
		// LOCATION_UPDATE_DISTANCE_INTERVAL, criteria, context, null);
		SELECTED_PROVIDER = LocationManager.GPS_PROVIDER;
		// Log.v("Safecell", "SELECTED_PROVIDER : " + SELECTED_PROVIDER);

		processLastKnownLocation();
	}

	private static void requestNetworkLocationUpdates() {
		if (LocationManager.NETWORK_PROVIDER.equals(SELECTED_PROVIDER)) {
			return;
		} else if (SELECTED_PROVIDER != null) {
			locationManager.removeUpdates(context);
		}

		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER,
				LOCATION_UPDATE_TIME_INTERVAL,
				LOCATION_UPDATE_DISTANCE_INTERVAL, context);

		currentLocation = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		SELECTED_PROVIDER = LocationManager.NETWORK_PROVIDER;
		// Log.v("Safecell", "SELECTED_PROVIDER : " + SELECTED_PROVIDER);

		processLastKnownLocation();
	}

	private void configureLocationManager() {

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.addNmeaListener(this);

		// Criteria criteria = new Criteria();
		// criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		// criteria.setCostAllowed(true);
		// criteria.setAccuracy(Criteria.ACCURACY_FINE);

		// set accuracy details
		// All your normal criteria setup
		Criteria criteria = new Criteria();
		// Use FINE or COARSE (or NO_REQUIREMENT) here
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		criteria.setAltitudeRequired(true);
		criteria.setSpeedRequired(true);
		criteria.setCostAllowed(true);
		criteria.setBearingRequired(true);

		// API level 9 and up
		criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
		criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
		criteria.setBearingAccuracy(Criteria.ACCURACY_LOW);
		criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);

		// String bestProvider = locationManager.getBestProvider(criteria,
		// true);

		selectBestLocationProvider();
	}

	/**
	 * Removing the update listener
	 * 
	 */
	private void removeLocationUpdates() {
		if (locationManager != null && context != null) {
			locationManager.removeUpdates(context);
			locationManager.removeNmeaListener(context);
			SELECTED_PROVIDER = null;
		}
	}

	private void insertWaypoint(Location location) {

		/* Check previous trip json's exist in database = */
		if (NetWork_Information.isNetworkAvailable(context)
				&& !ExistingTripJsonHandler.isInProgress) {
			TripJsonRepository json_repo = new TripJsonRepository(context);
			int num_trips = json_repo.getNumberOfTripJsons();
			if (num_trips > 0) {
				Log.d(TAG, "Previous trips exist's. Saving the trip.");
				logger.debug("Previous trips exist's. Saving the trip.");
				new ExistingTripJsonHandler(context).postAllTripJsons();
			}
		}

		SharedPreferences sharedPreferences = getSharedPreferences("TRIP",
				MODE_WORLD_READABLE);
		isTripStarted = sharedPreferences.getBoolean("isTripStarted", false);
		Log.d(TAG, "is Trip Started " + isTripStarted);

		TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository = new TempTripJourneyWayPointsRepository(
				this);
		int current_locations = tempTripJourneyWayPointsRepository
				.getTotalWaypoints();
		Log.v(TAG, "Current locations: " + current_locations);
		logger.debug("Current local locations: " + current_locations);
		if (!isTripStarted && current_locations <= 15) {
			Toast.makeText(context, "Locations in DB: " + current_locations,
					Toast.LENGTH_SHORT).show();
			SCWayPoint wayPoint = new SCWayPoint(0, 0,
					DateUtils.getTimeStamp(System.currentTimeMillis()),
					location.getLatitude(), location.getLongitude(), speed,
					false);
			Log.d(TAG, "Inserting way points");
			tempTripJourneyWayPointsRepository.intsertWaypoint(wayPoint);
			return;
		}

		double distanceInMiles = tempTripJourneyWayPointsRepository
				.getDistanceDifference(location);
		long currentTime = new Date().getTime();

		double timeDifference = tempTripJourneyWayPointsRepository
				.getTimeDiffernce(currentTime);

		Log.v(TAG, "Time difference " + timeDifference);

		double avarageSpeed = 0;
		if (!isTripStarted) {
			avarageSpeed = tempTripJourneyWayPointsRepository
					.getMyAvarageEstimatedSpeedForAutoTripStart();

		} else {
			avarageSpeed = tempTripJourneyWayPointsRepository
					.getAvarageEstimatedSpeedForAutoTripStart();

		}

		double total_distance = tempTripJourneyWayPointsRepository
				.getTotalDistance();

		if (timeDifference != 0) {
			speed = distanceInMiles / timeDifference;
		}
		Log.d(TAG, "Average Speed " + avarageSpeed);
		Log.d(TAG, "Distance Travelled = " + total_distance);
		Log.d(TAG, "Speed = " + speed);
		logger.debug("Average Speed " + avarageSpeed + " Distance Travelled = "
				+ total_distance + " Speed = " + speed);

		if (!new ConfigurePreferences(context).isTripAbandon()) {
			// Toast.makeText(context, "Avg speed: " +
			// avarageSpeed+" speed:"+speed,
			// 300).show();
		}

		int dist = (int) total_distance;

		// if (total_distance >= 0.2 && isTripStarted) {
		// Log.v(TAG, "Mannual save call");
		// TrackingScreenActivity.isTripSavingInProgress = true;
		// saveTrip(context);
		// }

		logger.debug("Current provider: " + SELECTED_PROVIDER);

		// start trip avg speed more else clear waypoints...

		if (avarageSpeed >= TRIP_CUT_OFF && isTripStarted == false) {
			// start the service again
			Toast.makeText(context, " Starting trip. " + current_locations,
					Toast.LENGTH_SHORT).show();
			if (!TAGS.IS_EMERGENCY_HALT_ACTIVATED)
				//autoStartTrip();
			checkDayAndAutoTripStart();

			else {

				// clear waypoints on emergency halt activated...
				new TempTripJourneyWayPointsRepository(TrackingService.this)
						.deleteTripWaypoints();
				new InteruptionRepository(context).deleteInteruptions();
			}
		} else {
			// clear waypoints only
			//new TempTripJourneyWayPointsRepository(TrackingService.this)
			//		.deleteTripWaypoints();
		}

		/*
		 * if (avarageSpeed >= TRIP_CUT_OFF && isTripStarted == false &&
		 * autoStartTripTimer == null && total_distance > 0.3) {
		 * SharedPreferences tripAutoStartSharedPref = getSharedPreferences(
		 * "TripCheckBox", MODE_WORLD_READABLE); boolean startAutoTrips =
		 * tripAutoStartSharedPref.getBoolean( "isbackgroundtrip", true);
		 * Log.d(TAG, "Start Auto Trips = " + startAutoTrips);
		 * 
		 * if (startAutoTrips && (SELECTED_PROVIDER ==
		 * LocationManager.GPS_PROVIDER)) { Log.d(TAG, "Starting Trip Timer.");
		 * setAutoStartTripTimer(); } else { Log.e(TAG,
		 * "AutoTrip start Timer not started " + SELECTED_PROVIDER +
		 * " GPS Provider = " + LocationManager.GPS_PROVIDER); }
		 * 
		 * } else if (avarageSpeed < TRIP_CUT_OFF && isTripStarted == false &&
		 * autoStartTripTimer != null) { Log.i(TAG,
		 * "Canceling the start timer for average speed =" + avarageSpeed);
		 * cancelAutoTripStartTimer(); }
		 */

		if (speed > IGNORE_WAYPOINT_SPEED_LIMIT && timeDifference != 0) {
			Log.i(TAG, "Speed is too belond waypoint speed =" + speed);
			return;
		}

		boolean background = false;

		if (TrackingScreenActivity.isBackground && !(LockReceiver.wasLoacked)) {
			background = true;
		}

		if (!isTripStarted) {

			SCWayPoint wayPoint = new SCWayPoint(0, 0,
					DateUtils.getTimeStamp(System.currentTimeMillis()),
					location.getLatitude(), location.getLongitude(), speed,
					background);
			Log.d(TAG, "Inserting way points for journey = " + 0);
			tempTripJourneyWayPointsRepository.intsertWaypoint(wayPoint);
		}

		/* Create timer to clear idle locations from repo */
		/*
		 * if(!isTripStarted) { if(idlePointsTimerHandler == null) { // set new
		 * moniter timer setPointsIdleTimer(); } else { // reset existing new
		 * timer idlePointsTimerHandler.removeCallbacks(idlePointsTimerRunner);
		 * idlePointsTimerHandler.postDelayed(idlePointsTimerRunner,
		 * Util.minitToMilliSeconds(IDLE_POINTS_CLEAR_TIME)); } }
		 */

		/* Create timer to monitor device idle conditions. */
		if (isTripStarted) {
			if (idleMonitorTimerHandler == null) {
				// set new moniter timer
				setDeviceIdleTimer();
			} else {
				// reset existing new timer
				idleMonitorTimerHandler.removeCallbacks(idleMonitorTimerRunner);
				idleMonitorTimerHandler.postDelayed(idleMonitorTimerRunner,
						Util.minitToMilliSeconds(TAGS.tripStopTime));
			}
		}

		/* Sync trip data when when 5 miles traveled. */
		if (isTripStarted && (total_distance > TAGS.TRIP_SYNC_DISTANCE)
				&& !TripSyncHandler.isPreviousSyncFail) {
			logger.debug("Trip data is syncing.");
			new TripSyncHandler(context).execute();
		}

		/* Display trip recording screen if it is not started. */
		if (isTripStarted) {
			Log.d("SafeCellDebug", "trackingScreenActivity: "
					+ trackingScreenActivity + " ,isTripStarted:"
					+ isTripStarted);
			if (isWeekOfTimerSplashShow
					|| (!new ConfigurePreferences(context).isTripAbandon()
							&& TAGS.SHOW_SPLASH && trackingScreenActivity == null)) {
				isWeekOfTimerSplashShow = false;
				Log.v(TAG, "Starting tracking screen activity");
				Intent mIntent = new Intent(TrackingService.this,
						TrackingScreenActivity.class);
				mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(mIntent);
			}
		}

		if (isTripStarted) {
			Log.d(TAG, "Putting phone on silent mode.");
			AudioManager aManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			if (!iSpeechStarted
					&& !new ConfigurePreferences(context).isTripAbandon()) {
				aManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			} else {
				aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
			}

			// Save trip based on Time Difference
			if (timeDifference * 60 >= AUTO_SAVE_DELAY_MINUTE) {
				Log.i(TAG, "Saving the trip for as time diffrence is "
						+ timeDifference);
				saveTrip(context);
				return;
			}

			SCWayPoint wayPoint = new SCWayPoint(0, 0,
					DateUtils.getTimeStamp(currentTime),
					location.getLatitude(), location.getLongitude(), speed,
					background);

			Log.d(TAG, "Inserting way points for journey = " + 0);
			tempTripJourneyWayPointsRepository.intsertWaypoint(wayPoint);

			// Save Trip based on speed monitor timer
			if (avarageSpeed < TRIP_CUT_OFF) {
				if (handlerTimerTask == null) {
					setAutostopTripTimer();
					Log.v(TAG, "**** Trip Stop Timer Set: Yes");
				}
			} else {
				if (handlerTimerTask != null) {
					Log.v(TAG, "Average speed is less than cut off speed. "
							+ "Remove the auto trip start timer from handler");
					handlerTimerTask.removeCallbacks(timerTaskRunner);
					handlerTimerTask = null;
					Log.v(TAG, "Trip Stop Timer Cancelled");
				}
			}

		}// if trip started
			// trackingScreenActivity.dismProgressDialog();
	}

	private void insertIntoSMS() {
		Cursor c = context.getContentResolver().query(
				Uri.parse("content://sms"), null, null, null, null);
		c.moveToFirst();

		if (smsRepository == null) {
			// Log.v("Safecell :" + "NULL: ", "smsRepository");
			smsRepository = new SMSRepository(context);
		}

		smsArrayList = smsRepository.scSmsArrayList();

		if (smsArrayList == null) {
			// Log.v("Safecell :" + "NULL: ", "smsArrayList");
			return;
		}

		// Log.v("Safecell :" + "smsArrayList.size()", "size = "
		// + smsArrayList.size());

		boolean smsPresent = false;
		int noOfSmses = smsArrayList.size();

		if (smsArrayList.size() == 0) {
			smsPresent = false;
		} else {

			for (int i = 0; i < smsArrayList.size(); i++) {
				ContentValues values = new ContentValues();

				values.put("thread_id", smsArrayList.get(i).getThread_id());
				values.put("address", smsArrayList.get(i).getAddress());
				values.put("person", smsArrayList.get(i).getPerson());
				values.put("date", smsArrayList.get(i).getDate());
				values.put("protocol", smsArrayList.get(i).getProtocol());
				values.put("read", smsArrayList.get(i).getRead());
				values.put("status", smsArrayList.get(i).getStatus());
				values.put("type", smsArrayList.get(i).getType());
				values.put("reply_path_present", smsArrayList.get(i)
						.getReply_path_present());
				values.put("subject", smsArrayList.get(i).getSubject());
				values.put("body", smsArrayList.get(i).getBody());
				values.put("service_center", smsArrayList.get(i)
						.getService_center());
				values.put("locked", smsArrayList.get(i).getLocked());

				// context.getContentResolver().insert(Uri.parse("content://sms"),
				// values);
			}
			smsRepository.deleteSms();
			smsPresent = true;
		}

		boolean callPresent = false;

		CharSequence contentTitle2 = "Calls during trip";
		CharSequence contentText2 = TrackingScreenActivity.incomingCallCounter
				+ " missed calls during trip.";

		if (TrackingScreenActivity.incomingCallCounter > 0) {
			callPresent = true;
		}

		String sms = (noOfSmses == 1) ? " text " : " texts ";
		String calls = (TrackingScreenActivity.incomingCallCounter == 1) ? " incoming call "
				: " incoming calls ";

		boolean showNotification = false;

		if (smsPresent && callPresent) {
			contentTitle2 = "Blocked texts and Incoming Calls";

			contentText2 = "SafeCellApp blocked " + noOfSmses + sms
					+ "(available in your SMS Inbox) and "
					+ TrackingScreenActivity.incomingCallCounter + calls
					+ "during your trip.";

			showNotification = true;
		} else if (smsPresent) {
			contentTitle2 = "Blocked Texts";
			contentText2 = "SafeCellApp blocked " + noOfSmses + sms
					+ "during your trip, that "
					+ ((noOfSmses == 1) ? "is" : "are")
					+ " now available in your SMS Inbox.";

			showNotification = true;
		} else if (callPresent) {
			contentTitle2 = "Blocked Incoming Calls";

			contentText2 = "SafeCellApp blocked "
					+ TrackingScreenActivity.incomingCallCounter + calls
					+ "during your trip.";

			showNotification = true;
		}

		if (showNotification) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification = new Notification(
					R.drawable.launch_icon, contentTitle2,
					System.currentTimeMillis());

			RemoteViews contentView = new RemoteViews(getPackageName(),
					R.layout.sms_notification);
			contentView.setImageViewResource(R.id.NotificationImage,
					R.drawable.launch_icon);
			contentView.setTextViewText(R.id.SmsTextNotification, contentText2);
			int notification_text_color = android.R.color.white;
			// contentView.setTextColor(R.id.SmsTextNotification,
			// notification_text_color);
			notification.contentView = contentView;
			Intent notificationIntent = new Intent();
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);
			notification.contentIntent = contentIntent;
			notificationManager.notify(0, notification);
		}

	}

	private void insertIntoSMS_Old() {
		Cursor c = context.getContentResolver().query(
				Uri.parse("content://sms"), null, null, null, null);
		c.moveToFirst();

		smsArrayList = smsRepository.scSmsArrayList();
		// Log.v("smsArrayList.size()", "size = " + smsArrayList.size());

		for (int i = 0; i < smsArrayList.size(); i++) {
			ContentValues values = new ContentValues();
			values.put("thread_id", smsArrayList.get(i).getThread_id());
			values.put("address", smsArrayList.get(i).getAddress());
			values.put("person", smsArrayList.get(i).getPerson());
			values.put("date", smsArrayList.get(i).getDate());
			values.put("protocol", smsArrayList.get(i).getProtocol());
			values.put("read", smsArrayList.get(i).getRead());
			values.put("status", smsArrayList.get(i).getStatus());
			values.put("type", smsArrayList.get(i).getType());
			values.put("reply_path_present", smsArrayList.get(i)
					.getReply_path_present());
			values.put("subject", smsArrayList.get(i).getSubject());
			values.put("body", smsArrayList.get(i).getBody());
			values.put("service_center", smsArrayList.get(i)
					.getService_center());
			values.put("locked", smsArrayList.get(i).getLocked());
			context.getContentResolver().insert(Uri.parse("content://sms"),
					values);
		}
		smsRepository.deleteSms();
	}

	class SaveTrip extends AsyncTask<Void, Void, Boolean> {

		private Context context;

		@Override
		protected void onPreExecute() {
			resultFlag = false;
			context = TrackingService.context;

			try {
				/* again clears the idle timmer */
				clearDeviceIdleTimer();

				if (!new ConfigurePreferences(context).isTripAbandon()
						&& trackingScreenActivity != null
						&& !BootReceiver.SHUTDOWNSAVE) {

					trackingScreenActivity.showProgressBar();
					context = TrackingScreenActivity.context;// getApplicationContext();

				} else if (!new ConfigurePreferences(context).isTripAbandon()
						&& addTripActivity != null
						&& !BootReceiver.SHUTDOWNSAVE) {
					addTripActivity.showProgressBar();
					context = AddTripActivity.addTripActivity;

				}
			} catch (Exception e) {
				Log.v(TAG,
						"Exception to show progress dialog. " + e.getMessage());
				e.printStackTrace();
			}
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				Log.v(TAG, "Do In Background");

				// Looper.prepare();
				TrackingScreenActivity.isTripSavingInProgress = true;
				TrackingService.ignoreLocationUpdates = true;

				saveTripAsyncTask(context);
				Log.v(TAG, "Saved On Server");

				// onPostExecute(resultFlag);

			} catch (Exception e) {
				e.printStackTrace();
				resultFlag = false;
				// onPostExecute(false);

			}
			// Looper.loop();

			return resultFlag;
		}

		protected void onPostExecute(Boolean result) {
			Log.v(TAG, "onPostExecute");
			TrackingScreenActivity.isTripSavingInProgress = false;
			TrackingService.ignoreLocationUpdates = false;

			// set trip start to false
			new ConfigurePreferences(TrackingService.context)
					.setTripStrated(false);
			sharedPreferences = getSharedPreferences("TRIP", MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putBoolean("isTripStarted", false);
			editor.commit();

			if (NO_INTERNET_SAVE) {

				// cancel battery timer and flags
				if (new ConfigurePreferences(context).isShutDown()) {
					Log.v(TAG, "DeActivating ShutDown configuration flag");
					new ConfigurePreferences(context).isShutDown(false);
				}

				if (BootReceiver.SHUTDOWNSAVE) {
					BootReceiver.SHUTDOWNSAVE = false;
				}

				Log.v(TAG, "Clearing local database trip data");
				logger.debug("Clearing local trip data");
				TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository = new TempTripJourneyWayPointsRepository(
						TrackingService.this);
				tempTripJourneyWayPointsRepository.deleteTripWaypoints();
				ir.deleteInteruptions();

				// clear prev trip sync distance
				// TAGS.PREV_SYNC_MILES = 0;
				new ConfigurePreferences(context).setPrevSyncMiles(0);
			} else {

				if (!result) {
					Log.d(TAG, "Save failled save result status: " + result);
					new TempTripJourneyWayPointsRepository(context)
							.deleteTripWaypoints();
					new InteruptionRepository(context).deleteInteruptions();
					// try {
					//
					// if (trackingScreenActivity != null) {
					// trackingScreenActivity.dismProgressDialog();
					// // trackingScreenActivity.finish();
					// }
					// if (addTripActivity != null) {
					// addTripActivity.dismProgressDialog();
					// // addTripActivity.finish();
					// }
					//
					if (!new ConfigurePreferences(context).isTripAbandon()) {
						Log.v(TAG, "Trip fail to save");
						logger.debug("Trip failed to save");
						Toast.makeText(TrackingService.context,
								"Trip fail to save", Toast.LENGTH_LONG).show();
						// foregroundService("Trip Save Failed. ");
					}
					// // tripNotSaveDialog(TrackingScreenActivity.context,
					// // EXCEPTION_WHILE_SAVING);
					// } catch (Exception e) {
					// Log.e(TAG, "Exception occured in save preexecute");
					// e.printStackTrace();
					// }
				} else {
					Log.d(TAG, "Trip Saved Sucessfully: " + result);
					logger.debug("Trip saved sucessfully");
					if (!new ConfigurePreferences(context).isTripAbandon()) {
						Log.v(TAG, "Trip saved sucessfully");
						Toast.makeText(TrackingService.context,
								"Trip Saved Sucessfully ", Toast.LENGTH_LONG)
								.show();

						// foregroundService("Trip Saved Sucessfully ");
					}

					// cancel battery timer and flags
					if (new ConfigurePreferences(context).isShutDown()) {
						Log.v(TAG, "DeActivating ShutDown configuration flag");
						new ConfigurePreferences(context).isShutDown(false);
					}

					if (BootReceiver.SHUTDOWNSAVE) {
						BootReceiver.SHUTDOWNSAVE = false;
					}

					Log.v(TAG, "Clearing local database trip data");
					TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository = new TempTripJourneyWayPointsRepository(
							TrackingService.this);
					tempTripJourneyWayPointsRepository.deleteTripWaypoints();
					ir.deleteInteruptions();
					// clear prev trip sync distance
					// TAGS.PREV_SYNC_MILES = 0;
					new ConfigurePreferences(context).setPrevSyncMiles(0);
				}

			}

			// un mute silent mode.
			AudioManager aManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

			callActivityAfterTripSave();
			// start trip again
			startTrip();

		}

	}

	public void callActivityAfterTripSave() {
		try {

			// Setting trip done flag
			new ConfigurePreferences(TrackingService.this).set_isTripDone(true);
			// deactivate keypad lock service
			if (LockKeyPadService.isLockActivated()) {
				LockKeyPadService.deactivateKeyPadLock();
			}

			// deactivate sms blocking service
			if (BlockSMSService.isSMSBlocked()) {
				BlockSMSService.deactivateSMSBlock();
			}

			ConfigurePreferences config_preference = new ConfigurePreferences(
					context);
			if (!new ConfigurePreferences(context).isTripAbandon()
					&& trackingScreenActivity != null) {
				trackingScreenActivity.dismProgressDialog();
				trackingScreenActivity.finish();
				trackingScreenActivity = null;
			}
			if (!new ConfigurePreferences(context).isTripAbandon()
					&& addTripActivity != null) {
				addTripActivity.dismProgressDialog();
				addTripActivity.finish();
			}

			/** change unique ID for trip saving **/
			HomeScreenActivity.editGenereateTripUniqueID("");

			if (homeScreenActivity != null) {
				homeScreenActivity.finish();
			}

			Intent homeScreen = new Intent(TrackingService.this,
					HomeScreenActivity.class);
			homeScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(homeScreen);
			TrackingScreenActivity.isTripSavingInProgress = false;

		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * Starts the service again
	 * 
	 */
	private void startTrip() {

		new ConfigurePreferences(context).isTripAbandon(false);
		new ConfigurePreferences(context).setEmergencyTripSave(false);

		/** change unique ID for trip saving **/
		configureLocationManager();
		HomeScreenActivity.editGenereateTripUniqueID("");
		/*
		 * TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository
		 * = new TempTripJourneyWayPointsRepository( TrackingService.this);
		 * tempTripJourneyWayPointsRepository.deleteTrip();
		 * ir.deleteInteruptions();
		 */
		TrackingScreenActivity.isTripSavingInProgress = false;
		SharedPreferences.Editor editor1 = sharedPreferences.edit();
		editor1.putBoolean("isTripStarted", false);
		editor1.commit();

		TrackingService.isTripRunning = false;

		if (!InformatonUtils.isServiceRunning(this)) {
			Intent mIntent = new Intent(getApplicationContext(),
					TrackingService.class);
			startService(mIntent);
		}

		// Intent mIntent = new Intent(getApplicationContext(),
		// TrackingService.class);
		// stopService(mIntent);
		// startService(mIntent);
		// ServiceHandler.getInstance(this).unBind();
		// ServiceHandler.getInstance(this).bindService();
		reset();
		set();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContextWrapper#stopService(android.content.Intent)
	 */
	@Override
	public boolean stopService(Intent name) {
		Log.d(TAG, "Stopping Service");
		TrackingScreenActivity.isTripSavingInProgress = false;
		TrackingService.ignoreLocationUpdates = false;
		TrackingScreenActivity.IS_TRIP_PAUSED = false;
		cancelAutoTripStartTimer();
		cancelTripStopTimer();
		System.gc();
		return super.stopService(name);
	}

	private void tripNotSaveDialog(final Context frontScreenActivity,
			final int whichBlock) {

		String errorMessage = "Unexepted error occure while saving the trip.";
		if (show_dialog_counter == 4) {
			deleteTripDialog(frontScreenActivity, errorMessage);
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(
				frontScreenActivity);
		final AlertDialog alert;

		builder.setMessage(errorMessage)
				.setCancelable(false)
				.setPositiveButton("Retry",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
								saveTrip(frontScreenActivity);
							}
						})
				//
				.setNegativeButton("Delete Trip",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								/** Delete Temporary Trip **/
								switch (whichBlock) {
								case RESPONSE_NULL_WHILE_SAVING:
									dialog.cancel();
									tempTripJourneyWayPointsRepository
											.deleteTripWaypoints();
									if (addTripActivity != null) {
										addTripActivity.finish();
									}
									break;
								case EXCEPTION_WHILE_SAVING:
									dialog.cancel();
									callActivityAfterTripSave();
									break;

								}

							}
						});

		alert = builder.create();
		alert.show();
		show_dialog_counter += 1;
		if (trackingScreenActivity != null) {
			trackingScreenActivity.dismProgressDialog();
		}
		if (addTripActivity != null) {
			addTripActivity.dismProgressDialog();
		}

	}

	void deleteTripDialog(Context context, String errorMessage) {
		new AlertDialog.Builder(context)
				.setMessage(errorMessage)
				.setPositiveButton("Delete Trip",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
								callActivityAfterTripSave();
								show_dialog_counter = 0;
							}
						}).create().show();
		if (trackingScreenActivity != null) {
			trackingScreenActivity.dismProgressDialog();
		}
		if (addTripActivity != null) {
			addTripActivity.dismProgressDialog();
		}
	}

	@Override
	public void onNmeaReceived(long timestamp, String nmea) {
		// TODO Auto-generated method stub

		if (nmea != null && nmea.length() > 0) {
			// Get rid of checksum
			nmea = nmea.replaceAll("\\*..$", "");

			String[] nmeaSplit = nmea.split(",");

			if (nmeaSplit != null && nmeaSplit.length == 18) {
				if (nmeaSplit[0] != null
						&& nmeaSplit[0].equals(NMEA_DOP_SENTENCE))
					;

				try {
					currentPDOP = Float.parseFloat(nmeaSplit[15]);
				} catch (NumberFormatException nfe) {
					currentPDOP = 0.0;
				}

				try {
					currentHDOP = Float.parseFloat(nmeaSplit[16]);
				} catch (NumberFormatException nfe) {
					currentHDOP = 0.0;
				}

				try {
					currentVDOP = Float.parseFloat(nmeaSplit[17]);
				} catch (NumberFormatException nfe) {
					currentVDOP = 0.0;
				}

				// Log.v("SafecellcurrentPDOP", nmeaSplit[15]);
				// Log.v("SafecellcurrentHDOP", nmeaSplit[16]);
				// Log.v("SafecellcurrentVDOP", nmeaSplit[17]);
			}

		} else {
			// Log.v("Safecell:", "NEMANULL");
		}

	}

	/**
	 * Configuration
	 * 
	 * @author uttama
	 */
	// private class ConfigurationRunnable implements Runnable {
	//
	// private int mProfileId;
	//
	// private ConfigurationRunnable(int mProfileId) {
	// this.mProfileId = mProfileId;
	// }
	//
	// public void run() {
	// // starting the tracking service
	// ConfigurationHandler.getInstance().readResponse(mProfileId);
	// AUTO_SAVE_DELAY_MINUTE =
	// ConfigurationHandler.getInstance().getConfiguration()
	// .getTripStopTime();
	// TRIP_CUT_OFF =
	// ConfigurationHandler.getInstance().getConfiguration().getTripStartSpeed();
	// Log.d(TAG, "Auto Stop Delay = "+AUTO_SAVE_DELAY_MINUTE);
	// Log.d(TAG, "TRIP START SPEED = "+TRIP_CUT_OFF);
	// }
	//
	// }

	/**
	 * Configuration Runnable thread configure the trip.
	 * 
	 * @author uttama
	 */
	private class ConfigurationRunnable extends AsyncTask<Void, Void, Void> {

		private int mProfileId;

		private ConfigurationRunnable(int mProfileId) {
			this.mProfileId = mProfileId;
		}

		@Override
		protected Void doInBackground(Void... params) {
			// Read the configuration from the server
			ConfigurationHandler.getInstance().readResponse(mProfileId);
			ConfigurationHandler.getInstance().readEmergencyNumber(mProfileId);
			AUTO_SAVE_DELAY_MINUTE = ConfigurationHandler.getInstance()
					.getConfiguration().getTripStopTime();
			TRIP_CUT_OFF = ConfigurationHandler.getInstance()
					.getConfiguration().getTripStartSpeed();
			String controll_number = ConfigurationHandler.getInstance()
					.getConfiguration().getController_number();
			if (controll_number.equals("")) {
				Log.v(TAG, "Controller number is null using default number");
				controll_number = "+918971855771";
			}
			TAGS.CONTORL_NUMBER = controll_number;
			TAGS.SHOW_SPLASH = ConfigurationHandler.getInstance()
					.getConfiguration().getSplashShow();
			TAGS.keypadLock = ConfigurationHandler.getInstance()
					.getConfiguration().getKeypadlock();
			TAGS.disableCall = ConfigurationHandler.getInstance()
					.getConfiguration().isDisableCall();

			TAGS.disableTexting = ConfigurationHandler.getInstance()
					.getConfiguration().isDisableTexting();
			TAGS.disableEmail = ConfigurationHandler.getInstance()
					.getConfiguration().isDisableEmail();
			TAGS.disableWeb = ConfigurationHandler.getInstance()
					.getConfiguration().isDisableWeb();
			TAGS.logWayPoints = ConfigurationHandler.getInstance()
					.getConfiguration().isLogWayPoints();
			TAGS.tripStartSpeed = ConfigurationHandler.getInstance()
					.getConfiguration().getTripStartSpeed();
			TAGS.tripStopTime = ConfigurationHandler.getInstance()
					.getConfiguration().getTripStopTime();

			TAGS.startTime = ConfigurationHandler.getInstance()
					.getConfiguration().getStartTime();
			TAGS.endTime = ConfigurationHandler.getInstance()
					.getConfiguration().getEndTime();
			TAGS.isActive = ConfigurationHandler.getInstance()
					.getConfiguration().isActive();
			TAGS.dayOfWeek = ConfigurationHandler.getInstance()
					.getConfiguration().getDayOfWeek();

			Log.d(TAG, "AUTO_SAVE_DELAY_MINUTE:" + AUTO_SAVE_DELAY_MINUTE);
			Log.d(TAG, "TRIP_CUT_OFF: " + TRIP_CUT_OFF);
			Log.d(TAG, "CONTROLLER NUMBER:" + TAGS.CONTORL_NUMBER);
			Log.d(TAG, "isDisableCall:" + TAGS.disableCall);
			Log.d(TAG, "isDisable Text:" + TAGS.disableTexting);
			Log.d(TAG, "isEmail Disable:" + TAGS.disableEmail);

			Log.d(TAG, "isWEB Disable:" + TAGS.disableWeb);
			Log.d(TAG, "SHOW SPLASH:" + TAGS.SHOW_SPLASH);
			Log.d(TAG, "Keypad Lock: " + TAGS.keypadLock);
			Log.d(TAG, "startTime: " + TAGS.startTime);
			Log.d(TAG, "endTime: " + TAGS.endTime);
			Log.d(TAG, "isActive: " + TAGS.isActive);
			Log.d(TAG, "dayOfWeek: " + TAGS.dayOfWeek);

			return null;

		}

		@Override
		protected void onPostExecute(Void result) {
			// check web disable. download rules & apply
			// if (ConfigurationHandler.getInstance().getConfiguration()
			// .isDisableWeb()) {
			NEED_RULES_DOWNLOAD = true;
			APPLY_RULES_NEED = true;
			enableWEB_Rule();

			// }
			super.onPostExecute(result);
		}

	}

	private void enableWEB_Rule() {
		processLastKnownLocation();
		Log.v(TAG, "WebDisable configuration status: "
				+ ConfigurationHandler.getInstance().getConfiguration()
						.isDisableWeb());
		Log.v(TAG, "Downloading and appliing current location rule ");
		if (LOCATION_FOR_RULE == null) {
			Log.v(TAG, "Current location is not resolver yet..");
		} else {
			Log.d(TAG, "Rules are downloading for current location");
			if (!TrackingService.isTripRunning) {
				rulesdownload
						.loactionChangedForRule(LOCATION_FOR_RULE, context);
			}
			if (TrackingService.trackingScreenActivity != null) {
				rulesdownload.updateRulesStatusAsPerSchoolZone(false);

			}
			int rulesnum = rulesdownload.scRules.size();
			Log.d(TAG, "Number of rules downloaded: " + rulesnum);
			if (!TrackingService.isTripRunning && rulesnum != 0) {
				Log.v(TAG, "Trip not running. Do not make rules audible");
				NEED_RULES_DOWNLOAD = false;
				// makeRuleAudible();

			}

		}
	}

	private void makeRuleAudible() {

		Log.v(TAG,
				"Rules download completed & change rule download flag to false(default) ");
		QueueSong queuesong = new QueueSong();
		if (queuesong.isEmpty()) {
			queuesong.enqueue(TrackingScreenActivity.songsFileArray[1]);
		}

		mediaPlayer = new MediaPlayer();
		mediaPlayer = MediaPlayer.create(context, queuesong.peek());
		mediaPlayer.setLooping(false);
		mediaPlayer.start();

		// Log.v("Safecell :" + "Player", "StartPlaying");

		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				mediaPlayer.stop();

			}
		});

		// mediaPlayer = MediaPlayer.create(context,
		// queuesong.peek());
		// CountDownTimer cntr_aCounter = new CountDownTimer(4000, 1000) {
		// public void onTick(long millisUntilFinished) {
		// Log.d(TAG, "Playing audio");
		// // mediaPlayer.setLooping(true);
		// //mediaPlayer.setLooping(false);
		// mediaPlayer.start();
		// }
		//
		// public void onFinish() {
		// try {
		// // code fire after finish
		// mediaPlayer.stop();
		// mediaPlayer.release();
		// Log.v(TAG,
		// "Rules download completed & change rule download flag to false(default) ");
		// NEED_RULES_DOWNLOAD = false;
		//
		// } catch (Exception e) {
		// Log.e(TAG, "Exception raised while stoping media player");
		// e.printStackTrace();
		// }
		// }
		// };
		// cntr_aCounter.start();

	}

	public static Location getCurrentLocation() {
		return currentLocation;
	}

	public static void setCurrentLocation(Location currentLocation) {
		TrackingService.currentLocation = currentLocation;
	}

	/**
	 * Change normal trip to abandon trip.
	 * 
	 * @param context2
	 */
	private void makeTripAbandon(Context context) {
		// Release all applied phone locks.
		removeLocks();

		// Navigate from splash screen to safe cell home screen view
		Intent homeScreen = new Intent(TrackingService.this,
				HomeScreenActivity.class);
		homeScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(homeScreen);

	}

	private void removeLocks() {
		if (mSmsDetector != null) {
			mSmsDetector.stop();
		}
		if (mWebDetector != null) {
			mWebDetector.stop();
		}
		if (mEmailDetector != null) {
			mEmailDetector.stop();
		}
		// Enable mobile network
		Log.v(TAG, "Enabling mobile network");
		Util.setMobileDataEnabled(context, true);

		// deactivate keypad lock service
		if (LockKeyPadService.isLockActivated()) {
			Log.v(TAG, "Removing keypad lock");
			LockKeyPadService.deactivateKeyPadLock();
		}

		// deactivate sms blocking service
		if (BlockSMSService.isSMSBlocked()) {
			Log.v(TAG, "Removing sms block");
			BlockSMSService.deactivateSMSBlock();
		}
	}

	private boolean checkDayOfWeek() {
		String todayDayOfWeek = DateUtils.getTodayDayOfWeek();
		if (todayDayOfWeek.equalsIgnoreCase(TAGS.dayOfWeek))
			return true;
		return false;
	}

	private boolean isTripTimeAllowed() {
		if (TAGS.isActive && DateUtils.isTimeElapsed(TAGS.startTime))
			return true;
		return false;

	}

	private void weekOfDayTimerFireMethod() {
		Log.v(TAG, "weekOfDayTimerFireMethod");
		logger.debug("weekOfDayTimerFireMethod activated...");

		// reset all dayofweek settings
		if (isTripTimeAllowed()) {

			Log.e(TAG, "WeekOfDay trip allowed");

			// Disable SAVE TRIP Flag.
			new ConfigurePreferences(context).setSAVETRIP(true);
			Log.v(TAG, "SAVE TRIP activated ");
			logger.debug("week of day resetting save trip flag");
			// Toast.makeText(context, "SAVE TRIP Activated", Toast.LENGTH_LONG)
			// .show();

			// Set abandon flag in preferences
			new ConfigurePreferences(context).isTripAbandon(false);
			Log.v(TAG, "Trip Abandoned is resetted");
			logger.debug("Trip Abandoned is resetted");
			// Toast.makeText(context, "Trip Abandoned is resetted",
			// Toast.LENGTH_LONG).show();
			isWeekOfTimerSplashShow = true;
			clearWeekOfDaySplashTimer();
		}

	}

/*	private void checksBeforeAutoTripStart_old() {
		// // put the time difference check.
		TempTripJourneyWayPointsRepository waypointRepo = new TempTripJourneyWayPointsRepository(
				context);
		double firstWayPointTimeDiffernce = waypointRepo
				.getFirstWayPointTimeDiffernce(System.currentTimeMillis());

		// TAGS.PREV_SYNC_MILES = 0;
		new ConfigurePreferences(context).setPrevSyncMiles(0);

		if (firstWayPointTimeDiffernce < TAGS.FALSE_TRIP_TIME_THRESHOLD) {

			// check day of week
			if (checkDayOfWeek()) {

				if (!isTripTimeAllowed()) {

					Log.e(TAG, "WeekOfDay trip not allowed");

					// Disable SAVE TRIP Flag.
					new ConfigurePreferences(context).setSAVETRIP(false);
					Log.v(TAG, "SAVE TRIP Disabled ");
					Toast.makeText(context, "SAVE TRIP Disabled",
							Toast.LENGTH_LONG).show();

					// Set abandon flag in preferences
					new ConfigurePreferences(context).isTripAbandon(true);
					Log.v(TAG, "Trip is Abandoned");
					Toast.makeText(context, "Trip is Abandoned",
							Toast.LENGTH_LONG).show();
					TrackingService.ABANDONFLAG = true;

					// create a week of day timer

					 Create timer to monitor device idle conditions. 
					isWeekOfTimerSplashShow = false;
					setWeekOfDaySplashTimer();

				}

			}
			// normal day
			autoStartTrip();

			Log.v(TAG, "Auto Trip Started. Time Difference: "
					+ firstWayPointTimeDiffernce + " mins");
			logger.info("Auto Trip Started. Time Difference: "
					+ firstWayPointTimeDiffernce + " mins");
		} else {
			Log.v(TAG, "FALSE TRIP IDENTIFIED. Time Difference: "
					+ firstWayPointTimeDiffernce + " mins");
			logger.info("FALSE TRIP IDENTIFIED. Time Difference: "
					+ firstWayPointTimeDiffernce + " mins");

			// delete the waypoints
			waypointRepo.deleteTripWaypoints();

			// start the service again
			startTrip();
		}
	}
*/
	
	
	private void checkDayAndAutoTripStart() {
		// // put the time difference check.
		TempTripJourneyWayPointsRepository waypointRepo = new TempTripJourneyWayPointsRepository(
				context);
		double firstWayPointTimeDiffernce = waypointRepo
				.getFirstWayPointTimeDiffernce(System.currentTimeMillis());

		
		new ConfigurePreferences(context).setPrevSyncMiles(0);

		if (firstWayPointTimeDiffernce < TAGS.FALSE_TRIP_TIME_THRESHOLD) {

			// check day of week
			// if (checkDayOfWeek()) {
			//
			// if (!isTripTimeAllowed()) {
			//
			// Log.e(TAG, "WeekOfDay trip not allowed");
			//
			// // Disable SAVE TRIP Flag.
			// new ConfigurePreferences(context).setSAVETRIP(false);
			// Log.v(TAG, "SAVE TRIP Disabled ");
			// Toast.makeText(context, "SAVE TRIP Disabled",
			// Toast.LENGTH_LONG).show();
			//
			// // Set abandon flag in preferences
			// new ConfigurePreferences(context).isTripAbandon(true);
			// Log.v(TAG, "Trip is Abandoned");
			// Toast.makeText(context, "Trip is Abandoned",
			// Toast.LENGTH_LONG).show();
			// TrackingService.ABANDONFLAG = true;
			//
			// // create a week of day timer
			//
			// /* Create timer to monitor device idle conditions. */
			// isWeekOfTimerSplashShow = false;
			// setWeekOfDaySplashTimer();
			//
			// }
			//
			// } else {
			// normal day
			
			autoStartTrip();
			

			Log.v(TAG, "Auto Trip Started. Time Difference: "
					+ firstWayPointTimeDiffernce + " mins");
			logger.info("Auto Trip Started. Time Difference: "
					+ firstWayPointTimeDiffernce + " mins");
			// }
		} else {
			Log.v(TAG, "FALSE TRIP IDENTIFIED. Time Difference: "
					+ firstWayPointTimeDiffernce + " mins");
			logger.info("FALSE TRIP IDENTIFIED. Time Difference: "
					+ firstWayPointTimeDiffernce + " mins");

			// delete the waypoints
			waypointRepo.deleteTripWaypoints();

			// start the service again
			startTrip();
		}
	}

	private void logicToNewRequirement() {
		/*
		 * if(dayofweek) {
		 * 
		 * if(checkTime()) { // trip allowed if(statusActive) { //start normal
		 * trip } else { // start the abandon trip } } else { // start the timer
		 * with interval time and recall same method..
		 * 
		 * }
		 * 
		 * } else { // normal day case. start trip normally }
		 */
	}

}
