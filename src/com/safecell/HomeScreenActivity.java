package com.safecell;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.safecell.dataaccess.InteruptionRepository;
import com.safecell.dataaccess.ProfilesRepository;
import com.safecell.dataaccess.TempTripJourneyWayPointsRepository;
import com.safecell.dataaccess.TripJourneysRepository;
import com.safecell.dataaccess.TripRepository;
import com.safecell.model.SCProfile;
import com.safecell.model.Emergency.Emergencies;
import com.safecell.networking.ConfigurationHandler;
import com.safecell.networking.EmergencyHandler;
import com.safecell.networking.SigninHanlder;
import com.safecell.receiver.BlockSMSService;
import com.safecell.receiver.LockKeyPadService;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.DateUtils;
import com.safecell.utilities.FlurryUtils;
import com.safecell.utilities.InformatonUtils;
import com.safecell.utilities.LocationSP;
import com.safecell.utilities.StateAddress;
import com.safecell.utilities.TAGS;
import com.safecell.utilities.TrailCheck;
import com.safecell.utilities.UIUtils;
import com.safecell.utilities.URLs;
import com.safecell.utilities.Util;

public class HomeScreenActivity extends ListActivity {
	/** Called when the activity is first created. **/
	Button startNewTripButton, homeButton, btnMyTrips, rulesButton, faxButton;
	TextView tvTotalTrips, tvGrade, tvTotalMiles;
	TextView tvUserName, tvUserLevel;
	public static TextView tvLocation;
	StringBuilder strFile;
	static String[][] pointInfo;
	ImageView profileImageView;
	Uri outputFileUri;;
	String overallTotalPoints, overallTotalMiles;
	int arrayIndex = 0;
	String tripNameArray[] = new String[] {};
	int pointsArray[] = new int[] {};
	String milesArray[] = new String[] {};
	String tripRecordedDateArray[] = new String[] {};
	private int totalTrips, totalGrade;
	static Context contextHomeScreenActivity;
	public static boolean KEYPAD_LOCK_DESTROY = false;
	private ProfilesRepository profilesRepository;
	private boolean isgameplay;
	private StateAddress stateAddress;
	int[] tripIdArray = new int[] {};
	private TextView noTripsSavedTextView;
	SharedPreferences sharedPreferences;
	private ServiceConnection mConnection;

	private Handler gpsCheckTimerHandler;
	private Runnable gpsCheckTimerHandlerTask;

	private static final int GPS_CHECK_TIMER_INTERVAL = 2; // seconds

	static final int AUTO_SAVE_DELAY_MINUTE = 5;

	private LinearLayout gradeLinearLayout, pointsLinearLayout;

	private NotificationManager mManager;

	private static final int APP_ID = 0;

	private String TAG = HomeScreenActivity.class.getSimpleName();
	private TabControler tabControler;
	private AlertDialog alertDialogForTermsConditions;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// android:background="@drawable/stop_button"

		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Log.v(TAG, "on create");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setWindowAnimations(R.anim.null_animation);

		contextHomeScreenActivity = HomeScreenActivity.this;

		mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		profilesRepository = new ProfilesRepository(contextHomeScreenActivity);
		isgameplay = this.GamplayOnOff();
		if (!InformatonUtils.isServiceRunning(this)) {
			startService();
		}
		// ServiceHandler.getInstance(this).bindService();

		this.initUI();
		Log.v(TAG, "Emergencies.Inbound_Details Size:"
				+ Emergencies.Inbound_Details.size());

		// Request for Emergency numbers
		new EmergencyHandler(contextHomeScreenActivity,
				profilesRepository.getId()).execute();

		IsTripPaused();
		IsTripSaved();

		Log.d(TAG,
				"Setings = "
						+ getSharedPreferences("SETTINGS", MODE_WORLD_READABLE)
								.getBoolean("isDisabled", false));

		sharedPreferences = getSharedPreferences("TRIP", MODE_WORLD_READABLE);

		this.recentTripLog();

		deleteFile(WayPointStore.WAY_POINT_FILE);
		deleteFile(InterruptionStore.INTERRUPTION_POINT_FILE);

		// setListAdapter(new recentTripAdapater(HomeScreenActivity.this));

		if (isAppTermited())

		{
			deleteLastTempTrip();
			Intent mIntent = new Intent(HomeScreenActivity.this,
					TrackingScreenActivity.class);
			mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			Log.v("HomeScreenActivity", "AppTerminated");
			startActivity(mIntent);

			finish();
			// clearTrackingScreenPref();
		}

		/*
		 * if(!isAppTermited()) { Log.v("SafeCell: Temp data",
		 * "Delete Last Data"); deleteLastTempTrip(); }
		 */

	}

	public static String genereateTripUniqueID() {
		SharedPreferences preferences = TrackingService.context
				.getSharedPreferences("TripJouneryUID", MODE_WORLD_WRITEABLE);
		String jouneryUniqueID = preferences.getString("UniqueIdForTrip", "");
		// Log.v("SafeCell : HomeScreen","jouneryUniqueID ="+jouneryUniqueID);
		return jouneryUniqueID;
	}

	public static void editGenereateTripUniqueID(String uniqueId) {
		SharedPreferences.Editor editorUniqueID = TrackingService.context
				.getSharedPreferences("TripJouneryUID", MODE_WORLD_WRITEABLE)
				.edit();
		editorUniqueID.putString("UniqueIdForTrip", uniqueId);
		editorUniqueID.commit();
		// Log.v("Safecell : --UniqueIdForTrip", "ID = "+uniqueId);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.v(TAG, "on start");
		HomeScreenActivity.contextHomeScreenActivity = this;
		// skip when trip is running mode.
		if (new ConfigurePreferences(contextHomeScreenActivity)
				.getTripStrated()) {
			Log.v(TAG, "Trip is running skip on start method");
			setListAdapter(new recentTripAdapater(HomeScreenActivity.this));
			return;
		}
		// deactivate keypad lock service
		if (LockKeyPadService.isLockActivated()) {
			LockKeyPadService.deactivateKeyPadLock();
		}
		// deactivate sms blocking service
		if (BlockSMSService.isSMSBlocked()) {
			BlockSMSService.deactivateSMSBlock();
		}
		new ConfigurePreferences(contextHomeScreenActivity).setSAVETRIP(true);
		// Log.v("onStart", "onstart");
		Log.d(TAG, "Checking license again");
		if (new ConfigurePreferences(
				HomeScreenActivity.this.getApplicationContext()).getIsLogin()
				&& !new ConfigurePreferences(HomeScreenActivity.this)
						.get_isTripDone()) {
			Log.d(TAG, "Just now you login");
			new ConfigurePreferences(
					HomeScreenActivity.contextHomeScreenActivity)
					.setIsLogin(false);
		} else if (new ConfigurePreferences(
				HomeScreenActivity.contextHomeScreenActivity).getTripStrated()) {
			Log.v(TAG, "Trip running no license check required");
		} else if (new ConfigurePreferences(
				HomeScreenActivity.contextHomeScreenActivity).get_isTripDone()) {
			Log.d(TAG, "License validatation");
			new ConfigurePreferences(
					HomeScreenActivity.contextHomeScreenActivity)
					.set_isTripDone(false);
			// validate user license status
			validateLicense();
		} else {
			Log.d(TAG, "License validatation");
			// validate user license status
			validateLicense();
		}

		FlurryUtils.startFlurrySession(this);
		if (!isgameplay) {
			gradeLinearLayout.setVisibility(View.GONE);
		} else {
			gradeLinearLayout.setVisibility(View.VISIBLE);
			// initUI();
		}

		setListAdapter(new recentTripAdapater(HomeScreenActivity.this));
	}

	private void validateLicense() {
		Log.d(TAG, "Creating login asysn service");
		try {
			new ASyncLoginHandler().execute();
		} catch (Exception e) {
			Log.e(TAG, "Exception raised while asyncronus login");
			e.printStackTrace();
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.v(TAG, "onStop method");
		FlurryUtils.endFlurrySession(this);
	}

	public void IsTripSaved() {

		// SharedPreferences sharedPreferences = getSharedPreferences("TRIP",
		// MODE_PRIVATE);
		/*
		 * if(sharedPreferences.getBoolean("isTrackingCrashed", false)) {
		 * SharedPreferences.Editor editorTripCrashed =
		 * sharedPreferences.edit();
		 * editorTripCrashed.putBoolean("isTrackingCrashed", false);
		 * editorTripCrashed.commit();
		 * 
		 * Intent mIntent = new Intent(HomeScreenActivity.this,
		 * TrackingScreenActivity.class); startActivity(mIntent);
		 * 
		 * 
		 * }
		 */

		/*
		 * if(!sharedPreferences.getBoolean("isTripSaved", true)){ Intent
		 * mIntent = new Intent(HomeScreenActivity.this, AddTripActivity.class);
		 * startActivity(mIntent); }
		 */
	}

	private void IsTripPaused() {

		SharedPreferences sharedPreferences = getSharedPreferences("TRIP",
				MODE_PRIVATE);
		if (sharedPreferences.getBoolean("isTripPaused", false)) {
			Intent mIntent = new Intent(HomeScreenActivity.this,
					TrackingScreenActivity.class);
			startActivity(mIntent);
			finish();

		}
	}

	private void startService() {

		Intent mIntent = new Intent(this, TrackingService.class);
		startService(mIntent);

	}

	/*
	 * public void checkProvider() { LocationManager locationManager =
	 * (LocationManager) getSystemService(Context.LOCATION_SERVICE); Location
	 * currentLocation = locationManager.getLastKnownLocation("gps");
	 * 
	 * 
	 * String provider = TrackingService.SELECTED_PROVIDER;
	 * Log.v("Safecell: Provider", "provider = "+provider); AlertDialog d;
	 * if(!"gps".equalsIgnoreCase(provider))
	 * 
	 * if (currentLocation == null) { Toast.makeText(HomeScreenActivity.this,
	 * "Not using GPS provider for location updates.",
	 * Toast.LENGTH_LONG).show();
	 * 
	 * final AlertDialog.Builder b = new
	 * AlertDialog.Builder(HomeScreenActivity.this); //final AlertDialog d =new
	 * AlertDialog(this); b.setTitle("Warning");b.setMessage(
	 * "This program requires a GPS provider. As of now your device does not have GPS service. "
	 * + "	Please enable the GPS service and restart the program.");
	 * 
	 * b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) { //
	 * /dismiss(); //HomeScreenActivity.this.finish(); } }); d = b.create();
	 * d.show();
	 * 
	 * } }
	 */

	private void launchGPSOptions() {
		final Intent intent = new Intent(
				Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	void deleteLastTempTrip() {

		TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository = new TempTripJourneyWayPointsRepository(
				HomeScreenActivity.this);
		tempTripJourneyWayPointsRepository.deleteTripWaypoints();

		InteruptionRepository interuptionRepository = new InteruptionRepository(
				HomeScreenActivity.this);
		interuptionRepository.deleteInteruptions();
	}

	private void recentTripLog() {
		TripJourneysRepository tripJourneysRepository = new TripJourneysRepository(
				HomeScreenActivity.this);
		TripRepository tripRepository = new TripRepository(
				HomeScreenActivity.this);

		Cursor cursorTripJounery = tripJourneysRepository.SelectTrip_journeys();
		startManagingCursor(cursorTripJounery);
		tripJourneysRepository.SelectTrip_journeys().close();

		float totalPositivePoints = tripJourneysRepository.getPointsSum();
		// Log.v("totalPositivePoints", ""+totalPositivePoints);
		float totalSafeMilePoints = tripJourneysRepository
				.getSafeMilePointsSum();
		// Log.v("totalSafeMilePoints", ""+totalSafeMilePoints);

		float grade = 0;

		if (totalPositivePoints > 0) {
			grade = totalSafeMilePoints / totalPositivePoints;

		}
		grade = grade * 100;
		// Log.v("grade", "" + grade);
		int ratioInt = Math.round(grade);
		if (ratioInt < 0) {
			ratioInt = 0;
		}

		Cursor cursorTotalPointsMiles = tripJourneysRepository
				.sumOfPointsMiles();
		startManagingCursor(cursorTotalPointsMiles);
		tripJourneysRepository.sumOfPointsMiles().close();
		cursorTotalPointsMiles.moveToFirst();

		ProfilesRepository profileRepository = new ProfilesRepository(
				HomeScreenActivity.this);
		tvUserName.setText(profileRepository.getName() + "");

		if (cursorTotalPointsMiles.getCount() > 0
				&& !cursorTotalPointsMiles.isNull(0)) {
			cursorTotalPointsMiles.moveToFirst();

			overallTotalPoints = cursorTotalPointsMiles.getString(0);

			totalTrips = tripRepository.getTripCount();
			tvTotalMiles.setText("" + (int) totalSafeMilePoints);
			tvTotalTrips.setText("" + totalTrips);
			tvGrade.setText("" + ratioInt + "%");
		}
		cursorTotalPointsMiles.close();

		pointsArray = new int[cursorTripJounery.getCount()];
		milesArray = new String[cursorTripJounery.getCount()];
		tripRecordedDateArray = new String[cursorTripJounery.getCount()];
		tripIdArray = new int[cursorTripJounery.getCount()];

		if (cursorTripJounery.getCount() > 0) {
			noTripsSavedTextView.setVisibility(View.INVISIBLE);
			cursorTripJounery.moveToFirst();
			do {
				int tripIdIndex = cursorTripJounery
						.getColumnIndex("trip_journey_id");
				int milesIndex = cursorTripJounery.getColumnIndex("miles");
				int pointsIndex = cursorTripJounery.getColumnIndex("points");
				int trip_dateIndex = cursorTripJounery
						.getColumnIndex("trip_date");

				int tripId = cursorTripJounery.getInt(tripIdIndex);
				String miles = ""
						+ Math.round(Double.valueOf(cursorTripJounery
								.getString(milesIndex)));
				int points = cursorTripJounery.getInt(pointsIndex);
				long tripDate = cursorTripJounery.getLong(trip_dateIndex);

				
				
				
				String formatTripDate = DateUtils.dateInString(tripDate);
				Log.e(TAG, "(Milli)tripDate: "+tripDate+" formatedTripDate: "+formatTripDate);
				pointsArray[arrayIndex] = points;
				milesArray[arrayIndex] = miles + " Total Miles";
				tripRecordedDateArray[arrayIndex] = formatTripDate;
				tripIdArray[arrayIndex] = tripId;
				arrayIndex = arrayIndex + 1;

			} while (cursorTripJounery.moveToNext());

			cursorTripJounery.close();

			tripNameArray = tripRepository.SelectTripName();

		}

	}

	boolean isAppTermited() {
		boolean isAppTermited;
		InteruptionRepository interuptionRepository = new InteruptionRepository(
				HomeScreenActivity.this);
		isAppTermited = interuptionRepository.isAppTermited();
		return isAppTermited;
	}

	public boolean GamplayOnOff() {
		SharedPreferences sharedPreferences = getSharedPreferences(
				"GamePlayCheckBox", MODE_WORLD_READABLE);
		isgameplay = sharedPreferences.getBoolean("isGameplay", true);
		return isgameplay;

	}

	public void checkLocationProviderStatus() {
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			return; // We have GPS do nothing
		} else if (locationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			showGPSStatusAlert(LocationManager.NETWORK_PROVIDER);
		} else {
			showGPSStatusAlert(null);
		}
	}

	public void showGPSStatusAlert(String provider) {

		// Log.v("Safecell", "" + provider);

		String title = "";
		String message = "";

		if (LocationManager.GPS_PROVIDER.equals(provider)) {
			return;
		}

		if (provider == null) {
			title = "GPS is not enabled.";
			message = "GPS is not enabled. Please enable it.";
		} else {
			title = "GPS is not enabled.";
			message = "GPS is not enabled. Please enable it. \nMeanwhile, background trip tracking will be disabled.";
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(title);
		builder.setMessage(message);
		builder.setCancelable(false);

		builder.setPositiveButton("Launch Settings",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						launchGPSOptions();
					}
				});

		if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
		}

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void initUI() {

		setContentView(R.layout.home_screen_layout);
		// startNewTripButton = (Button)
		// findViewById(R.id.StartNewTripLayout_StartNewButton);

		homeButton = (Button) findViewById(R.id.tabBarHomeButton);
		btnMyTrips = (Button) findViewById(R.id.tabBarMyTripsButton);
		rulesButton = (Button) findViewById(R.id.tabBarRulesButton);
		faxButton = (Button) findViewById(R.id.tabBarFaxButton);
		homeButton.setBackgroundResource(R.drawable.home_clicked);
		noTripsSavedTextView = (TextView) findViewById(R.id.noTripsSavedTextView);
		tvTotalTrips = (TextView) findViewById(R.id.StartNewTripTripsTextView);
		tvGrade = (TextView) findViewById(R.id.StartNewTripGradeTextView);
		tvTotalMiles = (TextView) findViewById(R.id.StartNewTripTotalMilesTextView);
		profileImageView = (ImageView) findViewById(R.id.StartNewTripProileImageView);
		tvUserName = (TextView) findViewById(R.id.HomeScreenUserName);
		tvLocation = (TextView) findViewById(R.id.tabBarCurentLocationTextView);
		tvLocation.setText(LocationSP.LocationSP);

		gradeLinearLayout = (LinearLayout) findViewById(R.id.HomeScreenGradeLinearLayout);
		setProfileImage();
		/*
		 * if (!isgameplay) { gradeLinearLayout.setVisibility(View.GONE); }else
		 * gradeLinearLayout.setVisibility(View.VISIBLE);
		 */

		if (overallTotalMiles == null) {
			overallTotalMiles = "0";
		}
		tvTotalMiles.setText(overallTotalMiles + "");
		tvTotalTrips.setText(totalTrips + "");
		tvGrade.setText(totalGrade + "%");

		tabControler = new TabControler(HomeScreenActivity.this);
		btnMyTrips.setOnClickListener(tabControler.getMyTripsOnClickListner());
		rulesButton.setOnClickListener(tabControler.getRulesOnClickListner());
		faxButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				faxButton.setBackgroundResource(R.drawable.fax_click);
				Activity activity = HomeScreenActivity.this;
				tabControler.dialogforWebviewFax(URLs.FAX_URL, activity, HomeScreenActivity.this);

			}
		});

		// startNewTripButton.setOnClickListener(startTripOnClickListener);
		profileImageView.setOnClickListener(profileImageViewOnclickListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPostResume()
	 */
	@Override
	protected void onPostResume() {
		super.onPostResume();
		// settingsButton.setOnClickListener(tabControler
		// .getSettingOnClickListener());

		Log.d(TAG, "ON Resume");
	}

	private void setProfileImage() {
		if (profilesRepository == null)
			return;
		byte[] profileImage = profilesRepository.getProfileImage();
		if (profileImage != null) {

			ByteArrayInputStream imageStream = new ByteArrayInputStream(
					profileImage);
			Bitmap Image = BitmapFactory.decodeStream(imageStream);
			profileImageView.setImageBitmap(Image);

		}
	}

	private OnClickListener startTripOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository = new TempTripJourneyWayPointsRepository(
					HomeScreenActivity.this);
			InteruptionRepository ir = new InteruptionRepository(
					HomeScreenActivity.this);

			TrackingService.cancelTripStopTimer();
			// Log.v("Safecell",
			// "Manual Trip Start Cancelled Auto Trip Start Timer if Set");

			/** Clear Shared Preference **/
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putBoolean("isTripPaused", false);
			editor.putBoolean("isTripStarted", true);
			editor.commit();

			/** change unique ID for trip saving **/
			editGenereateTripUniqueID(SCProfile.newUniqueDeviceKey());
			// Intent mIntent = new Intent(getApplicationContext(),
			// TrackingService.class);
			// getApplicationContext().stopService(mIntent);
			// getApplicationContext().startService(mIntent);

			ServiceHandler.getInstance(getApplicationContext()).unBind();
			ServiceHandler.getInstance(getApplicationContext()).bindService();

			// Intent callActivity = new Intent(HomeScreenActivity.this,
			// TrackingScreenActivity.class);
			// startActivity(callActivity);
			// finish();

			tempTripJourneyWayPointsRepository.deleteTripWaypoints();
			ir.deleteInteruptions();
		}
	};

	private OnClickListener profileImageViewOnclickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			dialogMessage();

		}
	};// Library
	private WebView wv;

	private void dialogMessage() {
		Dialog dialog = new AlertDialog.Builder(HomeScreenActivity.this)
				.setMessage("Select Profile Picture")
				.setPositiveButton("Photo Library",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								Intent intent = new Intent();
								intent.setType("image/*");
								intent.setAction(Intent.ACTION_GET_CONTENT);
								startActivityForResult(intent, 1);

							}
						})
				.setNegativeButton("New Photo",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								Intent nintent = new Intent(
										MediaStore.ACTION_IMAGE_CAPTURE);
								File file = new File(Environment
										.getExternalStorageDirectory(), String
										.valueOf(System.currentTimeMillis())
										+ ".jpg");

								outputFileUri = Uri.fromFile(file);
								nintent.putExtra(MediaStore.EXTRA_OUTPUT,
										outputFileUri);
								startActivityForResult(nintent, 2);
							}
						}).create();
		dialog.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 1) {

			if (resultCode == Activity.RESULT_OK) {

				Uri selectedImage = data.getData();
				// Log.v("Safecell :" + "selectedImage", "imagePath = "
				// + selectedImage);
				profileImageView.setImageBitmap(getImageFromURI(selectedImage));

			}
		}// //End Request code = 1

		if (requestCode == 2) {
			if (resultCode == -1) {

				Uri selectedImage = Uri.parse(outputFileUri.getPath());
				// Log.v("Safecell :" + "selectedImage", "imagePath = "
				// + selectedImage);
				profileImageView.setImageBitmap(getImageFromURI(selectedImage));

			}
		}

	}// end on result

	Bitmap getImageFromURI(Uri uri) {
		Bitmap resizedBitmap = null;
		String abc = null;
		if (uri != null) {
			String str = uri.toString();
			abc = str.substring(0, 1);
			// Log.v("Safecell :" + "abc", str);
		}

		if (uri != null && abc.equalsIgnoreCase("c")) {
			Uri selectedImage = uri;
			// Log.v("Safecell :" + "Uri", selectedImage.toString());

			String[] proj = { MediaColumns.DATA };
			Cursor cursor = managedQuery(selectedImage, proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
			cursor.moveToFirst();

			String path = cursor.getString(column_index);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			resizedBitmap = BitmapFactory.decodeFile(path, options);
			imageStoreInDatabase(resizedBitmap);

			cursor.close();

			return resizedBitmap;
		} else if (uri != null && abc.equalsIgnoreCase("/")) {

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			resizedBitmap = BitmapFactory.decodeFile(uri.getPath(), options);
			imageStoreInDatabase(resizedBitmap);
			return resizedBitmap;

		}
		return resizedBitmap;

	}

	public void imageStoreInDatabase(Bitmap imageBitmap) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] b = baos.toByteArray();
		profilesRepository.updateProfileImage(b);

	}

	/*
	 * private void cancelGPSCheckTimer() { if (gpsCheckTimerHandler != null) {
	 * gpsCheckTimerHandler.removeCallbacks(gpsCheckTimerHandlerTask);
	 * gpsCheckTimerHandler = null; Log.v("Safecell",
	 * "**GPSCheckTimer cancelled"); } }
	 * 
	 * private void createGPSCheckTimer() { cancelGPSCheckTimer();
	 * gpsCheckTimerHandler = new Handler();
	 * gpsCheckTimerHandler.postDelayed(gpsCheckTimerHandlerTask,
	 * GPS_CHECK_TIMER_INTERVAL * 1000); Log.v("Safecell",
	 * "**GPSCheckTimer started"); }
	 * 
	 * private void setGPSCheckTimer() { gpsCheckTimerHandlerTask = new
	 * Runnable() { public void run() {
	 * 
	 * if(TrackingService.context == null) { Log.v("Safecell",
	 * "**static_this is null : GPSCheckTimer will check again");
	 * createGPSCheckTimer(); return; }
	 * 
	 * cancelGPSCheckTimer();
	 * 
	 * showGPSStatusAlert(TrackingService.SELECTED_PROVIDER); } };
	 * 
	 * if (gpsCheckTimerHandler != null) { cancelGPSCheckTimer(); }
	 * 
	 * if(TrackingService.context == null) { createGPSCheckTimer(); } else {
	 * Log.v("Safecell", "Traking service ready. No Timer.");
	 * showGPSStatusAlert(TrackingService.SELECTED_PROVIDER); } }
	 */

	class recentTripAdapater extends ArrayAdapter<Object> {

		Activity context;

		recentTripAdapater(Activity context) {
			super(context, R.layout.start_new_trip_listrow, milesArray);
			this.context = context;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			LayoutInflater inflater = context.getLayoutInflater();
			View row = inflater.inflate(R.layout.start_new_trip_listrow, null);
			TextView pointsNumberTextView = (TextView) row
					.findViewById(R.id.StartNewTripRowPointsNumber);
			TextView totalMilesTextView = (TextView) row
					.findViewById(R.id.StartNewTripRowTotalMilesTextView);
			TextView dateTextView = (TextView) row
					.findViewById(R.id.StartNewTripRowDateTimeTextView);
			TextView tripNameTextView = (TextView) row
					.findViewById(R.id.StartNewTripRowTripNameTextView);
			TextView pointsLabelTextView = (TextView) row
					.findViewById(R.id.StartNewTripRowPointsText);

			pointsLinearLayout = (LinearLayout) row
					.findViewById(R.id.StartNewTripPointsLinearLayout);

			if (!isgameplay) {
				pointsLinearLayout.setVisibility(View.GONE);

			}
			switch (position) {
			case 0:
				pointsNumberTextView.setText(String
						.valueOf(pointsArray[position]));
				if (pointsArray[position] < 0) {
					pointsNumberTextView.setTextColor(Color.RED);
					pointsLabelTextView.setTextColor(Color.RED);
				}
				totalMilesTextView.setText(milesArray[position]);
				dateTextView.setText(tripRecordedDateArray[position]);
				tripNameTextView.setText(tripNameArray[position]);
				break;
			case 1:

				pointsNumberTextView.setText(String
						.valueOf(pointsArray[position]));
				if (pointsArray[position] < 0) {
					pointsNumberTextView.setTextColor(Color.RED);
					pointsLabelTextView.setTextColor(Color.RED);
				}
				totalMilesTextView.setText(milesArray[position]);
				dateTextView.setText(tripRecordedDateArray[position]);
				tripNameTextView.setText(tripNameArray[position]);
				break;
			case 2:

				pointsNumberTextView.setText(String
						.valueOf(pointsArray[position]));
				if (pointsArray[position] < 0) {
					pointsNumberTextView.setTextColor(Color.RED);
					pointsLabelTextView.setTextColor(Color.RED);
				}
				totalMilesTextView.setText(milesArray[position]);
				dateTextView.setText(tripRecordedDateArray[position]);
				tripNameTextView.setText(tripNameArray[position]);
				break;
			case 3:

				pointsNumberTextView.setText(String
						.valueOf(pointsArray[position]));
				if (pointsArray[position] < 0) {
					pointsNumberTextView.setTextColor(Color.RED);
					pointsLabelTextView.setTextColor(Color.RED);
				}
				totalMilesTextView.setText(milesArray[position]);
				dateTextView.setText(tripRecordedDateArray[position]);
				tripNameTextView.setText(tripNameArray[position]);
				break;
			case 4:

				pointsNumberTextView.setText(String
						.valueOf(pointsArray[position]));
				if (pointsArray[position] < 0) {
					pointsNumberTextView.setTextColor(Color.RED);
					pointsLabelTextView.setTextColor(Color.RED);
				}
				totalMilesTextView.setText(milesArray[position]);
				dateTextView.setText(tripRecordedDateArray[position]);

				break;

			}

			return (row);
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		int TripId = tripIdArray[position];

		Intent mIntent = new Intent(HomeScreenActivity.this,
				MyTripDiscriptionActivity.class);
		mIntent.putExtra("TripId", TripId);
		mIntent.putExtra("CallingActivity", "HomeScreenActivity");
		startActivityForResult(mIntent, 17);

		super.onListItemClick(l, v, position, id);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		return super.onKeyDown(keyCode, event);

	}

	// @Override
	// public boolean onKeyDown(int keyCode, KeyEvent event) {
	// if (keyCode == KeyEvent.KEYCODE_BACK) {
	// AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
	// HomeScreenActivity.this);
	// dialogBuilder.setMessage("Are you sure you want to exit?")
	// .setCancelable(false).setPositiveButton("Yes",
	// new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog,
	// int id) {
	//
	// // SharedPreferences sharedPreferences = getSharedPreferences(
	// // "TRIP", MODE_WORLD_READABLE);
	// // SharedPreferences.Editor editor = sharedPreferences
	// // .edit();
	// // editor.putBoolean("isTripStarted", false);
	// // editor.commit();
	// //
	// // TempTripJourneyWayPointsRepository tempTripJourneyWayPointsRepository
	// = new TempTripJourneyWayPointsRepository(
	// // HomeScreenActivity.this);
	// // tempTripJourneyWayPointsRepository
	// // .deleteTrip();
	//
	// HomeScreenActivity.this.finish();
	// }
	// }).setNegativeButton("No",
	// new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog,
	// int id) {
	// dialog.cancel();
	// }
	// });
	// if (!dialogBuilder.create().isShowing())
	// dialogBuilder.create().show();
	// return false;
	// }
	// return super.onKeyDown(keyCode, event);
	// }

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		// LocationService.setAct = this;
		super.onResume();
		TrackingService.homeScreenActivity = HomeScreenActivity.this;
		StateAddress.currentActivity = this;
		Log.v(TAG, "on resume");

		TrackingService.selectBestLocationProvider();

		checkLocationProviderStatus();

		// this.recentTripLog();
		// setListAdapter(new recentTripAdapater(HomeScreenActivity.this));
		// setListAdapter(new recentTripAdapater(HomeScreenActivity.this));
		isgameplay = this.GamplayOnOff();
		if (!isgameplay && (gradeLinearLayout.getVisibility() == View.VISIBLE)) {
			gradeLinearLayout.setVisibility(View.GONE);
		}

		TrackingService.ignoreLocationUpdates = false;
		// Log.v("Safecell", "startIgnoringLocationUpdates = false");

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "on destroy");
		if (HomeScreenActivity.KEYPAD_LOCK_DESTROY) {
			Log.v(TAG, "Over ride ondestroy");
			KEYPAD_LOCK_DESTROY = false;
			return;
		}

		TrackingService.homeScreenActivity = null;

		// ServiceHandler.getInstance(this).unBind();
	}

	public void showNotification(String msg) {
		if (!new ConfigurePreferences(contextHomeScreenActivity)
				.getTripStrated()) {
			Notification notification = new Notification(
					R.drawable.launch_icon, "Notify",
					System.currentTimeMillis());
			notification.setLatestEventInfo(HomeScreenActivity.this,
					"SafeCell", msg, PendingIntent.getActivity(
							HomeScreenActivity.this.getBaseContext(), 0, null,
							PendingIntent.FLAG_CANCEL_CURRENT));
			mManager.notify(APP_ID, notification);
		}

	}

	private class ASyncLoginHandler extends AsyncTask<Void, Void, Integer> {
		String loginResponce = null;
		String failureMessage = null;

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(Void... params) {

			JSONObject jsonObject = new JSONObject();
			ConfigurePreferences preference = new ConfigurePreferences(
					HomeScreenActivity.contextHomeScreenActivity);
			try {
				jsonObject.put("username", preference.getUserName());
				jsonObject.put("password", preference.getPassWord());

				JSONObject userSesionJsonObject = new JSONObject();
				userSesionJsonObject.put("user_session", jsonObject);
				SigninHanlder retriveProfiles = new SigninHanlder(
						HomeScreenActivity.contextHomeScreenActivity,
						userSesionJsonObject);
				loginResponce = retriveProfiles.accountLogin();
				int statusCode = retriveProfiles.getStatusCode();
				Log.d(TAG, "Login response code - " + statusCode);
				Log.d(TAG, "Login response string - " + loginResponce);
				if (statusCode != 200)
					failureMessage = retriveProfiles.getFailureMessage();
				return statusCode;

			} catch (JSONException e) {
				Log.e(TAG, "Exception while license validating login details");
				e.printStackTrace();
				return null;
			}

		}

		@Override
		protected void onPostExecute(Integer statuscode) {
			try {

				if (statuscode == 200) {
					Log.d(TAG, "Login sucess");

					// Check account is active or inactive

					// validate account activation
					boolean account_status = validateAccountActive(loginResponce);
					if (account_status) {
						Log.v(TAG, "Account is activated...");
						TrackingService.AccountActive = true;
					} else {
						Log.v(TAG, "Account is not activated yet..");
						quitDialog("Activation", TAGS.TAG_INACTIVE);
						return;
					}

					Log.d(TAG, "Checking license expirity");

					// check license expire date of profile
					String values[] = parseLicense(loginResponce);
					String start_date = values[0];
					String subscription = values[1];
					// check start date empty
					if (start_date.isEmpty()
							|| start_date.equalsIgnoreCase(" ")
							|| start_date == "null"
							|| start_date.equals("null")
							|| subscription.isEmpty()
							|| subscription.equalsIgnoreCase(" ")
							|| subscription == "null"
							|| subscription.equals("null")) {
						Log.e(TAG, "Profile license null");
						UIUtils.OkDialog(
								HomeScreenActivity.contextHomeScreenActivity,
								"No profile license information in server .");
						return;
					}
					boolean expire = TrailCheck.validateExpireOn(
							HomeScreenActivity.contextHomeScreenActivity,
							start_date, subscription);
					long remain_days = TrailCheck.getRemain_days();
					if (expire) {
						Log.d(TAG, "Trail expired");
						String exipredate = TrailCheck.expire_date.split(" ")[0];
						Log.d(TAG, "Trail expired");
						quitDialog(
								TrailCheck.title,
								"You SafeCell license expired on "
										+ exipredate
										+ " .Please log on the www.safecellapp.mobi with your userid and password and renew the license.");
					}
					if (remain_days < 30 && !expire) {
						if (remain_days < 2) {
							UIUtils.OkDialog(
									HomeScreenActivity.contextHomeScreenActivity,
									"You SafeCell license is about to expire on"
											+ TrailCheck.expire_date.split(" ")[0]
											+ ". Please log on the www.safecellapp.mobi with your userid and password and extend the license, otherwise you will not be able to use this application");
							Toast.makeText(
									HomeScreenActivity.contextHomeScreenActivity,
									"You SafeCell license is about to expire on"
											+ TrailCheck.expire_date.split(" ")[0]
											+ ". Please log on the www.safecellapp.mobi with your userid and password and extend the license, otherwise you will not be able to use this application",
									Toast.LENGTH_LONG).show();
						} else {
							UIUtils.OkDialog(
									HomeScreenActivity.contextHomeScreenActivity,
									TrailCheck.messsge);
							Toast.makeText(
									HomeScreenActivity.contextHomeScreenActivity,
									TrailCheck.messsge, Toast.LENGTH_LONG)
									.show();
						}

					}

				} else {
					Log.d(TAG, "Login Failled - " + failureMessage);
					// UIUtils.OkDialog(
					// HomeScreenActivity.contextHomeScreenActivity,
					// failureMessage);
				}

			} catch (Exception e) {
				Log.e(TAG, "Exception while license validating license details");
				e.printStackTrace();
			}

			super.onPostExecute(statuscode);
		}

	}

	public void quitDialog(String title, String message) {
		boolean flag = false;
		new AlertDialog.Builder(HomeScreenActivity.this)
				.setMessage(message)
				.setTitle(title)
				.setNeutralButton("Quit",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								TrackingService.AccountActive = false;
								dialog.cancel();
								finish();
							}
						}).show();

	}

	private String[] parseLicense(String loginResponse) {
		String dates[] = new String[2];
		try {

			JSONObject loginResponceJsonObject = new JSONObject(loginResponse);
			JSONArray profilesJA = loginResponceJsonObject.getJSONObject(
					"account").getJSONArray("profiles");

			JSONObject selectedProfile = profilesJA.getJSONObject(Integer
					.parseInt(new ConfigurePreferences(
							HomeScreenActivity.contextHomeScreenActivity)
							.getProfileIndex()));

			String start_date = selectedProfile.getString("license_startdate");
			String subscription = selectedProfile
					.getString("license_subsription");
			dates[0] = start_date;
			dates[1] = subscription;

			return dates;

		} catch (Exception e) {

			Log.e(TAG, "Error while reading login response");
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Based on the activated filed, validate the account is activated or not.
	 * If the account is activated it should return true otherwise return false.
	 * 
	 * @param loginResponce
	 *            - Represent login response as a string.
	 */
	private boolean validateAccountActive(String loginresponse) {
		boolean isActive = false;
		try {
			JSONObject loginResponceJsonObject = new JSONObject(loginresponse);

			JSONObject accountJO = loginResponceJsonObject
					.getJSONObject("account");
			JSONArray profilesJA = accountJO.getJSONArray("profiles");
			JSONObject selectedProfile = profilesJA.getJSONObject(Integer
					.parseInt(new ConfigurePreferences(
							HomeScreenActivity.contextHomeScreenActivity)
							.getProfileIndex()));

			String status = selectedProfile.getString("status");
			if (status.equalsIgnoreCase("open")) {
				isActive = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		finally {
			return isActive;
		}

	}

	
}// end

