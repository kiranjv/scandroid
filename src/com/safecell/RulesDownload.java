package com.safecell;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.safecell.dataaccess.ProfilesRepository;
import com.safecell.dataaccess.RulesRepository;
import com.safecell.model.SCRule;
import com.safecell.networking.ConfigurationHandler;
import com.safecell.networking.GetSchools;
import com.safecell.networking.RulesAccountRequest;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.DistanceAndTimeUtils;
import com.safecell.utilities.Util;

public class RulesDownload {

	private String TAG = "RulesDownload";
	private Location lastRulesDownloadlocation;
	private final double RULE_UPDATE_RADIUS = 5;
	private double distanceSinceLastDownload;
	private RulesAccountRequest rulesAccountRequest;
	public ArrayList<SCRule> scRules = new ArrayList<SCRule>();

	private boolean isPhoneActive = false;
	private boolean isSmsActive = false;
	private SCRule scRule;
	private Context context;
	private RulesRepository rulesRepository;
	private ProfilesRepository profilesRepository;
	private String currentProfileLicenseKey;
	private boolean ruleDownloadFailed = false;

	public static boolean RULES_REQUEST = false;

	public RulesDownload(Context context) {
		this.context = context;
		rulesRepository = new RulesRepository(context);
		profilesRepository = new ProfilesRepository(context);
		currentProfileLicenseKey = profilesRepository.getLicenseKey();
		// scRules = new ArrayList<SCRule>();
	}

	public void loactionChangedForRule(Location location, Context context) {
		if (lastRulesDownloadlocation != null) {

			double distance = DistanceAndTimeUtils.distFrom(
					lastRulesDownloadlocation.getLatitude(),
					lastRulesDownloadlocation.getLongitude(),
					location.getLatitude(), location.getLongitude());
			// Log.v("Safecell :"+TAG + "distance", distance + "");

			distanceSinceLastDownload += distance;
			Log.d(TAG, "Rules: distanceSinceLastDownload: "
					+ distanceSinceLastDownload);
			if (distanceSinceLastDownload >= RULE_UPDATE_RADIUS) {
				Log.v(TAG, "Distance more than rule radius.");
				distanceSinceLastDownload = 0;
				lastRulesDownloadlocation = location;
				startDownloadThread();

				// downloadRule();

			}

			if (ruleDownloadFailed == true) {
				distanceSinceLastDownload = 0;
				lastRulesDownloadlocation = location;
				startDownloadThread();
				ruleDownloadFailed = false;
			}

		} else {

			distanceSinceLastDownload = 0;
			lastRulesDownloadlocation = location;
			startDownloadThread();

			// downloadRule();

		}
	}

	private synchronized void startDownloadThread() {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				downloadRule();

			}
		};

		Thread t = new Thread(runnable);
		t.start();
	}

	private void downloadRule() {
		RULES_REQUEST = true;
//		Log.e(TAG, "Enable network before normal rules download");
//		// enable network
//		Log.e(TAG,
//				"Network status befor rules download "
//						+ Util.getNETWORK_BLOCKED());
//		if (!Util.getNETWORK_BLOCKED()) {
//			// Util.setMobileDataEnabled(context, true);
//		}

		rulesAccountRequest = new RulesAccountRequest(context,
				lastRulesDownloadlocation.getLatitude(),
				lastRulesDownloadlocation.getLongitude(), RULE_UPDATE_RADIUS);
		HttpResponse rulesHttpResponse = rulesAccountRequest.ruleRequest();

		
		if (rulesHttpResponse != null) {
			RULES_REQUEST = false;
			ruleDownloadFailed = false;
			synchronized (scRules) {
				scRules = rulesAccountRequest
						.handleGetResponseSCRule(rulesHttpResponse);
				Log.v(TAG, "Downloaded RULES Size: " + scRules.size());
				// disable network
//				if (Util.getNETWORK_BLOCKED()) {
//					if (!SchoolsDownload.SCHOOLS_RULES_REQUEST
//							&& !RULES_REQUEST) {
//						Log.e(TAG,
//								"disable network after normal rules download");
//						Log.e(TAG, "SCHOOLS_RULES_REQUEST status:"
//								+ SchoolsDownload.SCHOOLS_RULES_REQUEST
//								+ " RULES REQUEST Status: " + RULES_REQUEST);
//						// Util.setMobileDataEnabled(context, false);
//					}
//				}
			}
		} else {
			RULES_REQUEST = false;
			ruleDownloadFailed = true;
			// disable network
//			if (Util.getNETWORK_BLOCKED()) {
//				if (!SchoolsDownload.SCHOOLS_RULES_REQUEST && !RULES_REQUEST) {
//					Log.e(TAG, "disable network after normal rules download");
//					Log.e(TAG, "SCHOOLS_RULES_REQUEST status:"
//							+ SchoolsDownload.SCHOOLS_RULES_REQUEST
//							+ " RULES REQUEST Status: " + RULES_REQUEST);
//					// Util.setMobileDataEnabled(context, false);
//				}
//			}

		}

		rulesInsertionOrUpdation();
	}

	private boolean isSMSOrEmailRule(int index) {
		String rule_type = scRules.get(index).getRule_type();

		if (rule_type.equalsIgnoreCase("sms")
				|| rule_type.equalsIgnoreCase("email")) {

			return true;
		}
		return false;

	}

	private boolean isSchoolZoneOnly(int index) {

		String isSchoolZone = scRules.get(index).getWhen_enforced();

		if (isSchoolZone.equalsIgnoreCase("always")) {
			return true;
		}

		return false;
	}

	private boolean isPhoneRule(int index) {

		String isPhone = scRules.get(index).getRule_type();
		if (isPhone.equalsIgnoreCase("phone")) {
			return true;
		}
		return false;
	}

	public void updateRulesStatusAsPerSchoolZone(boolean schoolZoneActive) {

		Log.d(TAG, "Update Rules Status As Per School Zone value: "
				+ schoolZoneActive);
		synchronized (scRules) {

			if (scRules.size() == 0) {
				Log.d(TAG, "No rules available");
				ruleApplyOnTrackingScreen();
				return;

			} else {
				boolean phoneRuleFound = false;
				boolean smsOrEmailRuleFound = false;
				boolean smsRuleApplicableForAllZones = false;
				boolean phoneRuleApplicableForAllZones = false;

				for (int i = 0; i < scRules.size(); i++) {

					if (!(scRules.get(i)
							.appliesToLicenseClass(currentProfileLicenseKey))) {
						Log.d(TAG, "Rule does not apply to "
								+ scRules.get(i).getLicenses() + " "
								+ currentProfileLicenseKey);
						continue;
					}

					// Log.v("Safecell :"+"Rule_type",
					// scRules.get(i).getRule_type());

					if (isSMSOrEmailRule(i)) {

						smsOrEmailRuleFound = true;
						Log.d(TAG, "SMS or Email Rule Found");

						if (isSchoolZoneOnly(i)) {
							smsRuleApplicableForAllZones = true;
							Log.d(TAG,
									"sms Rule Applicable For All Zones (i.e always)");
						}
					}// end if email

					if (isPhoneRule(i)) {

						phoneRuleFound = true;
						Log.d(TAG, "Phone Rule Found");
						if (isSchoolZoneOnly(i)) {
							phoneRuleApplicableForAllZones = true;
							Log.d(TAG,
									"Phone Rule Applicable For All Zones (i.e always)");
						}
					}// end if phone

				}// end for loop

				if (smsOrEmailRuleFound) {

					if (smsRuleApplicableForAllZones) {
						isSmsActive = true;
						// Log.v("Safecell :"+"RuleSMs",
						// "ApplyToall--smsOrEmailRuleFound");
					} else if ((!smsRuleApplicableForAllZones)
							&& schoolZoneActive) {
						// Log.v("Safecell :"+"RuleSMs",
						// "SchoolZone--smsOrEmailRuleFound");
						isSmsActive = true;
					} else {

						isSmsActive = false;
					}
				} else {
					isSmsActive = false;
					// Log.v("Safecell :"+"sms", "smsRule Not Found");
				}// end smsOrEmailRuleFound

				if (phoneRuleFound) {

					if (phoneRuleApplicableForAllZones) {
						// Log.v("Safecell :"+"RulePhone",
						// "ApplyToall--PhoneRuleFound");
						isPhoneActive = true;
						isSmsActive = true;
						if (!ConfigurationHandler.getInstance()
								.getConfiguration().isDisableCall()) {
							ConfigurationHandler.getInstance()
									.getConfiguration().setDisableCall(true);
							ConfigurationHandler.getInstance()
									.getConfiguration().setDisableTexting(true);

						}
					} else if ((!phoneRuleApplicableForAllZones)
							&& schoolZoneActive) {
						// Log.v("Safecell :"+"RulePhone",
						// "SchoolZone--PhoneRuleFound");
						isPhoneActive = true;
						isSmsActive = true;
						if (!ConfigurationHandler.getInstance()
								.getConfiguration().isDisableCall()) {
							ConfigurationHandler.getInstance()
									.getConfiguration().setDisableCall(true);
							ConfigurationHandler.getInstance()
									.getConfiguration().setDisableTexting(true);

						}
					} else {
						// Log.v("Safecell :"+"phone", "phoneRule Not Found");
						isPhoneActive = false;

					}
				} else {

					isPhoneActive = false;
					// Log.v("Safecell :"+"phone", "phoneRule Not Found");
				}// end phoneRuleFound

				Log.v(TAG, "Appling rules - phone: " + isPhoneActive + " sms: "
						+ isSmsActive + " school: " + schoolZoneActive);
				ruleApplyOnTrackingScreen();

			}// first if else
		}// end synchronized

	}

	/**
	 * UPdate the location rules on tracking screen.
	 */
	public void ruleApplyOnTrackingScreen() {
		TrackingScreenActivity.updateRulesUI(isPhoneActive, isSmsActive);
	}

	public boolean isPhoneActive() {
		return isPhoneActive;
	}

	public void setPhoneActive(boolean isPhoneActive) {
		this.isPhoneActive = isPhoneActive;
	}

	public boolean isSmsActive() {
		return isSmsActive;
	}

	public void setSmsActive(boolean isSmsActive) {
		this.isSmsActive = isSmsActive;
	}

	public synchronized void rulesInsertionOrUpdation() {

		try {
			rulesRepository.updateInActive();
			Log.v(TAG, "Rules: " + scRules);
			boolean ruleIdPresent = false;
			for (int i = 0; i < scRules.size(); i++) {
				
				int ruleID = scRules.get(i).getId();
				ruleIdPresent = rulesRepository.ruleIdPresentInTable(String
						.valueOf(ruleID));

				if (ruleIdPresent)

				{
					rulesRepository.updateRules(scRules.get(i));
					Log.d(TAG, "Updating rule id: " + ruleID);
					// activeRulesArrayList.add(rulesArrayList.get(i));

				} else {

					rulesRepository.insertRules(scRules.get(i));
					// activeRulesArrayList.add(scRules.get(i));
					Log.d(TAG, "Inserting rule id: " + ruleID);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception while rules inserting into reo");
			e.printStackTrace();
		}
	}

	public ArrayList<SCRule> getScRules() {
		return scRules;
	}

	public String responseBody(HttpResponse response) throws ParseException,
			IOException {
		if (response == null) {
			return null;
		}

		String responseBody = EntityUtils.toString(response.getEntity());

		return responseBody;
	}

}
