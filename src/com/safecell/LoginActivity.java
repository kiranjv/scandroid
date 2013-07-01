package com.safecell;

import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.safecell.dataaccess.AccountRepository;
import com.safecell.dataaccess.ProfilesRepository;
import com.safecell.model.SCAccount;
import com.safecell.model.SCProfile;
import com.safecell.networking.EmergencyHandler;
import com.safecell.networking.NetWork_Information;
import com.safecell.networking.RetriveProfiles;
import com.safecell.networking.RetriveTripsOfProfile;
import com.safecell.networking.UpdateAccountDetails;
import com.safecell.networking.UpdateAccountsDetailsResponseHandler;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.FlurryUtils;
import com.safecell.utilities.TAGS;
import com.safecell.utilities.TrailCheck;
import com.safecell.utilities.UIUtils;

public class LoginActivity extends Activity {
	EditText etUserName;
	EditText etPassword;
	Button btnRetrivaProfiles;
	String uName;
	String callingActivity = "";
	String pwd;
	String loginResponce = "";
	String[] profileName = {};
	int profileIDArray[] = {};
	JSONArray profilesJA;
	JSONObject selectedProfile;
	JSONObject profileJO;
	JSONObject accountJO;
	ProgressThread mThread;
	ProgressThread1 mThread1;
	Handler handler;
	Handler handler1;
	Context context;
	ProgressDialog progressDialog;
	private SCAccount scAccount;
	private SCProfile scProfile;
	private String message;
	private boolean cancelExisitingProfileProgressDialog = false;
	private boolean cancelSelectProfile = false;
	private String versionName;

	private final String TAG = LoginActivity.class.getSimpleName();
	private WebView wv;
	private AlertDialog alertDialogForTermsConditions;

	private static boolean isTermsAcepted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_layout);

		getWindow().setWindowAnimations(R.anim.null_animation);

		InitUi();
		context = LoginActivity.this;
		if (!isTermsAcepted)
			dialogforWebview();

		progressDialog = new ProgressDialog(context);
		mThread = new ProgressThread();
		mThread1 = new ProgressThread1();
		PackageManager pm = getPackageManager();
		try {
			// ---get the package info---
			PackageInfo pi = pm.getPackageInfo("com.safecell", 0);
			// Log.v("Version Code", "Code = "+pi.versionCode);
			// Log.v("Version Name", "Name = "+pi.versionName);
			versionName = pi.versionName;

		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				try {
					progressDialog.dismiss();
				} catch (Exception e) {
					Log.e(TAG, "Exception while dismissing progress dialog");
					e.printStackTrace();
				}
				if (mThread.isAlive()) {
					mThread = new ProgressThread();
				}
			}
		};
		handler1 = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				progressDialog.dismiss();
				if (mThread1.isAlive()) {
					mThread1 = new ProgressThread1();
				}
			}
		};
	}

	void InitUi() {
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		etUserName = (EditText) findViewById(R.id.LoginUserNameEditText);
		etPassword = (EditText) findViewById(R.id.LoginPasswordEditText);
		btnRetrivaProfiles = (Button) findViewById(R.id.LoginRetriveProfileButton);
		btnRetrivaProfiles.setOnClickListener(retriveProfileOnclickListner);

	}

	void dialogforWebview() {

		AlertDialog.Builder builder;

		Context mContext = LoginActivity.this;

		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.private_policy_layout,
				(ViewGroup) findViewById(R.id.layout_root));

		final Activity activity = LoginActivity.this;
		wv = (WebView) layout.findViewById(R.id.webview);

		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

		wv.setWebViewClient(new HelloWebViewClient());
		wv.setWebChromeClient(new WebChromeClient() {

			public void onProgressChanged(WebView view, int newProgress) {
				activity.setProgress(newProgress * 100);

				if (newProgress == 100) {

				}
			};
		});

		wv.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				// Log.v("errorCode", "errorcode "+errorCode + description);
				alertDialogForTermsConditions.cancel();

			}
		});
		// wv.loadUrl(URLs.REMOTE_URL +
		// "api/1/site_setting/terms_of_service.html");
		wv.loadUrl("file:///android_asset/terms_of_service.html");
		builder = new AlertDialog.Builder(mContext);
		builder.setView(layout);
		builder.setTitle("Policy");

		builder.setPositiveButton("Accept",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						isTermsAcepted = true;
						dialog.cancel();
					}
				}).setNegativeButton("Don't Accept",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						isTermsAcepted = false;
						dialog.cancel();
						quitDialog("License",
								"Terms and conditions should accept. ");
					}
				});
		alertDialogForTermsConditions = builder.create();
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		try {
			alertDialogForTermsConditions.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		FlurryUtils.startFlurrySession(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryUtils.endFlurrySession(this);
	}

	protected void onPause() {
		super.onPause();
		Log.v(TAG, "On pause");
		if (progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	};

	protected void onResume() {
		super.onResume();
		Log.v(TAG, "on resume");
	};

	OnClickListener retriveProfileOnclickListner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			/*
			 * uName = etUserName.getText().toString().trim(); pwd =
			 * etPassword.getText().toString().trim();
			 */

			if (!isTermsAcepted)
				dialogforWebview();
			else {
				if (NetWork_Information.isNetworkAvailable(LoginActivity.this)) {
					progressDialog.setMessage("Loading Please Wait");

					progressDialog.show();

					progressDialog
							.setCancelable(cancelExisitingProfileProgressDialog);

					mThread.start();
				} else {

					NetWork_Information
							.noNetworkConnectiondialog(LoginActivity.this);

				}
			}
		}
	};

	private synchronized void varification() {

		Log.d(TAG, "Getting profile details");

		uName = etUserName.getText().toString().trim();
		pwd = etPassword.getText().toString().trim();
		String errorMessage = null;
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("username", uName);
			jsonObject.put("password", pwd);

			JSONObject userSesionJsonObject = new JSONObject();
			userSesionJsonObject.put("user_session", jsonObject);

			RetriveProfiles retriveProfiles = new RetriveProfiles(context,
					userSesionJsonObject);
			loginResponce = retriveProfiles.Retrive();
			Log.i(TAG, "Login response: " + loginResponce);
			int statusCode = retriveProfiles.getStatusCode();
			if (statusCode == 200) {
				Log.d(TAG, "Login sucess");
				// store login cradentials in shared preferences
				storeLoginDetails(uName, pwd);
				showProfileList();

			} else {
				errorMessage = retriveProfiles.getFailureMessage();
				UIUtils.OkDialog(context, errorMessage);
			}

		} catch (Exception e) {
			Log.e(TAG, "Exception raised while login. Error message - "
					+ errorMessage);
			e.printStackTrace();
		}
	}

	/**
	 * Based on the activated filed, validate the account is activated or not.
	 * If the account is activated it should return true otherwise return false.
	 * 
	 * @param loginResponce
	 *            - Represent login response as a string.
	 */
	private boolean validateAccountActive(JSONObject select_profile) {
		boolean isActive = false;
		try {
			String status = select_profile.getString("status");
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

	void showProfileList() {
		try {
			JSONObject loginResponceJsonObject = new JSONObject(loginResponce);

			// Log.v("Safecell:", "JSONObject "
			// +loginResponceJsonObject.toString(4));
			accountJO = loginResponceJsonObject.getJSONObject("account");
			profilesJA = accountJO.getJSONArray("profiles");

			profileName = new String[profilesJA.length()];
			profileIDArray = new int[profilesJA.length()];

			for (int i = 0; i < profilesJA.length(); i++) {
				profileJO = profilesJA.getJSONObject(i);
				profileName[i] = profileJO.getString("first_name") + " "
						+ profileJO.getString("last_name");
				profileIDArray[i] = profileJO.getInt("id");
			}

			selectedProfile = new JSONObject();

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select Profile");
			builder.setItems(profileName,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							if (NetWork_Information
									.isNetworkAvailable(LoginActivity.this)) {

								try {
									selectedProfile = profilesJA
											.getJSONObject(item);
									// check that profile already logged in
									boolean is_app_installed = selectedProfile
											.getBoolean("is_app_installed");
									Log.v(TAG, "is_app_installed: "
											+ is_app_installed);
									if (is_app_installed) {
										Log.e(TAG, "Blocking login...");
										Toast.makeText(
												context,
												"Profile already in use in another device.",
												Toast.LENGTH_LONG).show();
										quitDialog(context, "Profile in use",
												"Profile already in use in another device.");
									} else {
										Log.e(TAG, "Allowing login...");

										progressDialog
												.setMessage("Loading Please Wait");

										progressDialog.show();

										progressDialog
												.setCancelable(cancelSelectProfile);
										// setting selected index into
										// preferences
										new ConfigurePreferences(context)
												.setProfileIndex(String
														.valueOf(item));
										new ConfigurePreferences(context)
												.setSelectedProfile(selectedProfile
														.toString());
										// Checking profile license information
										// empty or not
										String manager_id = selectedProfile
												.getString("manager_id");
										String start_date = selectedProfile
												.getString("license_startdate");
										String subscription = selectedProfile
												.getString("license_subsription");

										// store details in shared preferences
										ConfigurePreferences preferences = new ConfigurePreferences(
												context);
										preferences
												.set_ProfileID(selectedProfile
														.getString("id"));
										preferences.set_AccountID(selectedProfile
												.getString("account_id"));
										preferences.set_ManagerID(manager_id);
										preferences
												.set_LicenseStartDate(start_date);
										preferences
												.set_LicenseSubscription(subscription);

										// check manager account
										if (manager_id.equals("0")) {
											Log.d(TAG, "Manager profile");
											UIUtils.OkDialog(context,
													"Cannot login with Manager Account. Please provide a registered device user.");
											progressDialog.dismiss();
											return;
										}

										// Check account is active or inactive

										// validate account activation
										boolean account_status = validateAccountActive(selectedProfile);
										if (account_status) {
											Log.v(TAG,
													"Account is activated...");
											TrackingService.AccountActive = true;
										} else {
											Log.v(TAG,
													"Account is not activated yet..");
											quitDialog(context, "Activation",
													TAGS.TAG_INACTIVE);
											return;
										}
										// check start date empty
										if (start_date.isEmpty()
												|| start_date
														.equalsIgnoreCase(" ")
												|| start_date == "null"
												|| start_date.equals("null")
												|| subscription.isEmpty()
												|| subscription
														.equalsIgnoreCase(" ")
												|| subscription == "null"
												|| subscription.equals("null")) {
											Log.e(TAG, "Profile license null");
											UIUtils.OkDialog(context,
													"No profile license information in server .");
											progressDialog.dismiss();
											return;
										}

										// check license start date of profile
										boolean start_status = TrailCheck
												.validateStartDate(context,
														start_date);
										Log.d(TAG,
												"start date validation status = "
														+ start_status);
										if (start_status) {
											Log.e(TAG,
													"Your license not started yet");
											String startdate = start_date
													.split("T")[0];
											quitDialog(
													context,
													"Licence",
													"You are authorize to use the application from "
															+ startdate
															+ ". Please login on that date");
											progressDialog.dismiss();
											return;
										}

										Log.d(TAG, "License subscription: "
												+ subscription);
										// check license expire date of profile
										boolean expire = TrailCheck
												.validateExpireOn(context,
														start_date,
														subscription);
										long remain_days = TrailCheck
												.getRemain_days();
										if (expire) {
											String exipredate = TrailCheck.expire_date
													.split(" ")[0];
											Log.e(TAG, "Trail expired");
											quitDialog(
													context,
													TrailCheck.title,
													"You SafeCell license expired on "
															+ exipredate
															+ " .Please log on the www.safecellapp.mobi with your userid and password and renew the license.");
											progressDialog.dismiss();
											return;
										}
										if (remain_days < 30 && !expire) {

											AlertDialog dialog_screen = new AlertDialog.Builder(
													context)
													.setMessage(
															TrailCheck.messsge)
													.setNeutralButton(
															"Ok",
															new DialogInterface.OnClickListener() {

																@Override
																public void onClick(
																		DialogInterface dialog,
																		int which) {

																	dialog.cancel();
																	mThread1.start();

																}
															}).create();
											dialog_screen.show();

										} else {
											mThread1.start();
										}
									}
								} catch (Exception e) {
									Log.d(TAG,
											"Exception while checking license");
									e.printStackTrace();
								}

							} else {

								Log.d(TAG, "No network information available");
								NetWork_Information
										.noNetworkConnectiondialog(LoginActivity.this);

							}

						}
					});
			AlertDialog alert = builder.create();
			alert.show();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public synchronized void SaveProfile() {
		try {

			Log.d(TAG, "Saving profile information");
			String validationCode = accountJO.getString("validation_code");
			int masterProfileId = accountJO.getInt("master_profile_id");
			// accountJO.getString("valid_until");
			String apiKey = accountJO.getString("apikey");
			int MasterAccountId = accountJO.getInt("id");

			int accountId = selectedProfile.getInt("account_id");
			int profileId = selectedProfile.getInt("id");
			String phone = selectedProfile.getString("phone");
			String lastName = selectedProfile.getString("last_name");
			String firstName = selectedProfile.getString("first_name");
			String email = selectedProfile.getString("email");
			String licenses = selectedProfile.getString("license_class_key");
			String deviceKey = selectedProfile.getString("device_key");

			scAccount = new SCAccount();
			scAccount.setAccountCode(validationCode);
			scAccount.setAccountId(MasterAccountId);
			scAccount.setMasterProfileId(masterProfileId);
			scAccount.setApiKey(apiKey);
			scAccount.setChargity_id(accountJO.getString("chargify_id"));
			scAccount.setActivated(accountJO.getBoolean("activated"));
			scAccount.setArchived(accountJO.getBoolean("archived"));
			scAccount.setStatus(accountJO.getString("status"));
			scAccount.setPerksId(accountJO.getString("perks_id"));

			AccountRepository accountRepository = new AccountRepository(
					LoginActivity.this);
			accountRepository.insertAccount(scAccount);

			scProfile = new SCProfile();
			scProfile.setAccountID(accountId);
			scProfile.setProfileId(profileId);
			scProfile.setEmail(email);
			scProfile.setFirstName(firstName);
			scProfile.setLastName(lastName);
			scProfile.setPhone(phone);
			scProfile.setLicenses(licenses);
			scProfile.setDeviceKey(deviceKey);
			scProfile.setDeviceFamily("android");
			scProfile.setExpiresOn(selectedProfile.getString("expires_on"));
			scProfile.setAppVersion(versionName);

			scProfile.setDeviceKey(SCProfile.newUniqueDeviceKey());

			// prepare emergency database for profile
			new EmergencyHandler(context, scProfile.getProfileId()).execute();
			// Update local database with trip details
			updateProfile();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private HttpResponse accountUpdateProfileResponse() {

		HashMap<Object, Object> profileMap = new HashMap<Object, Object>();
		profileMap.put("first_name", scProfile.getFirstName());
		profileMap.put("last_name", scProfile.getLastName());
		profileMap.put("email", scProfile.getEmail());
		profileMap.put("phone", scProfile.getPhone());
		profileMap.put("license_class_key", scProfile.getLicenses());
		profileMap.put("account_id", scProfile.getAccountID());
		profileMap.put("id", scProfile.getProfileId());
		profileMap.put("device_key", scProfile.getDeviceKey());

		HttpResponse profileResponse = null;
		UpdateAccountDetails updateAccountDetails = new UpdateAccountDetails(
				context, profileMap, scAccount.getApiKey(),
				scProfile.getProfileId());
		updateAccountDetails.updateAccountJson();

		profileResponse = updateAccountDetails.putRequest();
		message = updateAccountDetails.getFailureMessage();
		return profileResponse;
	}

	private synchronized void updateProfile() {

		Log.d(TAG, "Getting profile trips from server");
		HttpResponse profileResponse;
		profileResponse = accountUpdateProfileResponse();
		if (profileResponse != null) {

			UpdateAccountsDetailsResponseHandler updateAccountsDetailsResponseHandler = new UpdateAccountsDetailsResponseHandler(
					context);

			updateAccountsDetailsResponseHandler
					.updateAccountResponse(profileResponse);

			RetriveTripsOfProfile retriveTripsOfProfile = new RetriveTripsOfProfile(
					LoginActivity.this, scProfile.getProfileId(),
					scAccount.getApiKey());
			retriveTripsOfProfile.retrive();

			ProfilesRepository profilesRepository = new ProfilesRepository(
					LoginActivity.this);
			profilesRepository.insertProfile(scProfile);

			progressDialog.dismiss();

			Log.d(TAG, "Starting home screen activity");

			Intent mIntent = new Intent(LoginActivity.this,
					HomeScreenActivity.class);
			startActivity(mIntent);
			finish();

		} else {

			new AlertDialog.Builder(context)
					.setMessage(message)
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {

									dialog.cancel();
								}
							}).show();
		}
	}

	private class ProgressThread extends Thread {
		ProgressThread() {
		}

		public void run() {
			try {
				Looper.prepare();
				varification();

			} catch (Exception e) {
				Log.e(TAG, "Exception while getting profile information");
				e.printStackTrace();
			}

			handler.sendEmptyMessage(0);
			Looper.loop();
		}
	}

	private class ProgressThread1 extends Thread {
		ProgressThread1() {
		}

		public void run() {
			try {
				Looper.prepare();

				SaveProfile();

			} catch (Exception e) {
				// TODO: handle exception
			}

			handler1.sendEmptyMessage(0);
			Looper.loop();
		}
	}

	/**
	 * Store the username and password details in shared preferences file.
	 * 
	 * @param uName
	 *            - Login User name
	 * @param pwd
	 *            - Login pass word
	 */
	private void storeLoginDetails(String uName, String pwd) {

		Log.d(TAG, "Storing username and password");
		new ConfigurePreferences(context).setUserName(uName);
		new ConfigurePreferences(context).setPassWord(pwd);
		new ConfigurePreferences(context).setIsLogin(true);
	}

	public void quitDialog(Context context, String title, String message) {
		boolean flag = false;
		new AlertDialog.Builder(context)
				.setMessage(message)
				.setTitle(title)
				.setNeutralButton("Quit",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
								LoginActivity.this.finish();
							}
						}).show();

	}

	// Dialog dialog;
	private class HelloWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			Log.v("Terms URL: ", url);
			return true;
		}
	}

	private void quitDialog(String title, String message) {

		new AlertDialog.Builder(context)
				.setMessage(message)
				.setTitle(title)
				.setNeutralButton("Quit",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {

								dialog.cancel();
								finish();

							}
						}).show();
	}

}
