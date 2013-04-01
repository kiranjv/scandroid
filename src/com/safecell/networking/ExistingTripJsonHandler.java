package com.safecell.networking;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.safecell.TrackingService;
import com.safecell.dataaccess.AccountRepository;
import com.safecell.dataaccess.ProfilesRepository;
import com.safecell.dataaccess.TripJsonRepository;
import com.safecell.utilities.ConfigurePreferences;

public class ExistingTripJsonHandler {

	private static final String TAG = "ExistingTripJsonHandler";
	ArrayList<JSONObject> allTripJsons;
	private Context context;
	private String apiKey;
	private Integer accountID;
	private int profileID;
	private SubmitNewTripJourney submitJourneyHandle;
	public static boolean isInProgress = false;

	public ExistingTripJsonHandler(Context context) {
		this.context = context;
		isInProgress = true;

		submitJourneyHandle = createSubmitTripObject();

	}

	private SubmitNewTripJourney createSubmitTripObject() {
		HashMap<Object, Object> ApikeyAndAccountId = new HashMap<Object, Object>();

		AccountRepository accountRepository = new AccountRepository(context);
		ApikeyAndAccountId = accountRepository.selectApiKeyAndAccountID();
		apiKey = (String) ApikeyAndAccountId.get("ApiKey");
		accountID = Integer.valueOf(ApikeyAndAccountId.get("AccountId")
				.toString());

		ProfilesRepository profilesRepository = new ProfilesRepository(context);
		profileID = profilesRepository.getId();

		return new SubmitNewTripJourney(context, accountID, profileID, apiKey);

	}

	public void postAllTripJsons() {
		new AsyncJsonPostBackground().execute();
	}

	private class AsyncJsonPostBackground extends AsyncTask<Void, Void, Void> {

		private boolean resultFlag;

		@Override
		protected Void doInBackground(Void... params) {
			TripJsonRepository tripJsonRepository = new TripJsonRepository(
					context);

			Cursor cursor = tripJsonRepository.getCursor();

			try {
				cursor.moveToFirst();
				if (cursor != null && cursor.getCount() > 0) {

					for (int i = 0; i < cursor.getCount(); i++) {
						int jsonId = cursor.getInt(0);
						JSONObject json = new JSONObject(cursor.getString(2));
						boolean isPosted = postJson(jsonId, json);
						if (isPosted) {
							tripJsonRepository.deleteJson(jsonId);
						}
						cursor.moveToNext();
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "Exception raised");
				e.printStackTrace();
			} finally {
				cursor.close();
			}

			isInProgress = false;
			return null;
		}

		private boolean postJson(int jsonIndex, JSONObject jsonObject) {

			Log.v(TAG,
					"JSON Index: " + jsonIndex + " Data: "
							+ jsonObject.toString());
			HttpResponse httpResponse = submitJourneyHandle
					.postRequest(jsonObject);

			if (httpResponse == null) {

				Log.e(TAG,
						"Unexepted error occure while saving the trip. Response:"
								+ httpResponse);
				resultFlag = false;

			} else {
				Log.v(TAG, " Response code:"
						+ httpResponse.getStatusLine().toString());

				SubmitNewTripJourneyResponceHandler submitNewTripJourneyResponceHandler = new SubmitNewTripJourneyResponceHandler(
						context);
				try {
					if (new ConfigurePreferences(context).isTripAbandon()) {
						Log.v(TAG, "Ignoring abandon trip save response");
					} else {
						Log.d(TAG, "Parsing trip save response");

						submitNewTripJourneyResponceHandler
								.readResponce(httpResponse);
						new TripJsonRepository(context).deleteJson(jsonIndex);
					}

					resultFlag = true;
					Log.d(TAG, "Trip Saved ");
				} catch (Exception e) {
					resultFlag = false;
					e.printStackTrace();
				}

			}
			return resultFlag;

		}
	}
}
