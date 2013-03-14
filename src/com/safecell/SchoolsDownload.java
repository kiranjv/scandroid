package com.safecell;

import java.util.ArrayList;

import android.content.Context;
import android.location.Location;
import android.test.suitebuilder.TestSuiteBuilder.FailedToCreateTests;
import android.util.Log;

import com.safecell.model.SCSchool;
import com.safecell.networking.GetSchools;
import com.safecell.networking.GetSchoolsResponseHandler;
import com.safecell.utilities.DistanceAndTimeUtils;
import com.safecell.utilities.Util;

public class SchoolsDownload {

	private String TAG = SchoolsDownload.class.getSimpleName();
	private Location lastSchoolDownLoadLocation;
	private final double SCHOOL_UPDATE_RADIUS = 5;
	private final double SCHOOL_NEAR_RANGE = 300;
	private double distanceSinceLastDownload;

	private GetSchools downloadSchools;
	private GetSchoolsResponseHandler getSchoolsResponseHandler;
	public ArrayList<SCSchool> schools = new ArrayList<SCSchool>();

	private Context context;
	public static boolean SCHOOLS_RULES_REQUEST = false;

	public SchoolsDownload() {
	}

	/**
	 * Downalod the rules based on the current location.
	 * 
	 * @param location
	 * @param context
	 */
	public void locationChangedForSchool(Location location, Context context) {

		this.context = context;

		if (lastSchoolDownLoadLocation != null) {

			double distance = DistanceAndTimeUtils.distFrom(
					lastSchoolDownLoadLocation.getLatitude(),
					lastSchoolDownLoadLocation.getLongitude(),
					location.getLatitude(), location.getLongitude());
			Log.d(TAG, "School: distanceSinceLastDownload: "
					+ distanceSinceLastDownload + " current distance: "
					+ distance);

			distanceSinceLastDownload += distance;

			if (distanceSinceLastDownload >= SCHOOL_UPDATE_RADIUS) {
				Log.v(TAG, "More than school radius.");
				distanceSinceLastDownload = 0;
				lastSchoolDownLoadLocation = location;
				startDownloadThread();

			}

		}

		else {

			Log.d(TAG, "Schools: lastSchoolDownLoadLocation = "
					+ lastSchoolDownLoadLocation);
			distanceSinceLastDownload = 0;
			lastSchoolDownLoadLocation = location;
			startDownloadThread();

		}

	}

	private void startDownloadThread() {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				Log.e(TAG, "Enable network before shcool rules download");
				downloadSchool();
			}
		};

		Thread t = new Thread(runnable);
		t.start();
	}

	/**
	 * Download the school rules from server with in SCHOOL_UPDATE_RADIUS.
	 */
	private void downloadSchool() {
		SCHOOLS_RULES_REQUEST = true;
		// enable network
		// Log.e(TAG,
		// "Network status before school download: "
		// + Util.getNETWORK_BLOCKED());
		// Util.setMobileDataEnabled(context, true);

		Log.d(TAG, "Downloading school rules");
		downloadSchools = new GetSchools(context,
				lastSchoolDownLoadLocation.getLatitude(),
				lastSchoolDownLoadLocation.getLongitude(), SCHOOL_UPDATE_RADIUS);
		String result = downloadSchools.getRequest();

		Log.e(TAG, "School rules download response: " + result);
		if (result != null) {

			getSchoolsResponseHandler = new GetSchoolsResponseHandler(result);
			synchronized (schools) {
				schools = getSchoolsResponseHandler.handleGetSchoolsResponse();
				Log.d(TAG, "Number of schools downloaded: " + schools.size());
			}
			SCHOOLS_RULES_REQUEST = false;
			// disable network
			// if (Util.getNETWORK_BLOCKED()) {
			// if (!RulesDownload.RULES_REQUEST && !SCHOOLS_RULES_REQUEST) {

			// Log.e(TAG, "Disable network after shcool rules download");
			// Log.e(TAG, "SCHOOLS_RULES_REQUEST status:"
			// + SCHOOLS_RULES_REQUEST + " RULES REQUEST: "
			// + RulesDownload.RULES_REQUEST);
			// Util.setMobileDataEnabled(context, false);
			// }
			// }

		} else {
			String message = downloadSchools.getFailureMessage();
			Log.v(TAG, "Rule download fail message:" + message);
			SCHOOLS_RULES_REQUEST = false;
			// disable network
			// if (Util.getNETWORK_BLOCKED()) {
			// if (!RulesDownload.RULES_REQUEST && !SCHOOLS_RULES_REQUEST) {

			// Log.e(TAG, "Disable network after shcool rules download");
			// Util.setMobileDataEnabled(context, false);
		}
		// }
		// }

	}

	/**
	 * Check location is in school zone radius.
	 * 
	 * @param location
	 *            Represent the current GPS Location
	 * @return true if location is in school zone. Otherwise return false.
	 */
	public boolean schoolZoneActive(Location location) {

		synchronized (schools) {
			Log.d(TAG, "Checking school for Zone");

			if (schools.size() > 0) {

				for (SCSchool school : schools) {
					// Log.d(TAG, "id: " + school.getId());
					// Log.d(TAG, "name: " + school.getName());
					// Log.d(TAG, "Latitude: " + school.getLatitude()
					// + " Longitude: " + school.getLongitude());

					double distanceFromSchool = DistanceAndTimeUtils.distFrom(
							location.getLatitude(), location.getLongitude(),
							school.getLatitude(), school.getLongitude());
					double distance = (1609.344) * distanceFromSchool;
					// 1 mile = 1 609.344 meters
					Log.d(TAG, "DistanceFromSchool: " + distance);
					if (distance < SCHOOL_NEAR_RANGE) {
						Log.v(TAG, "Is School Zone Active: " + "Yes");
						return true;
					}
				}
			}
		}
		return false;

	}

}
