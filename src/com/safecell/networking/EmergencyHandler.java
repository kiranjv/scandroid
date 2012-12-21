package com.safecell.networking;

import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.safecell.model.Emergency;
import com.safecell.model.Emergency.Emergencies;
import com.safecell.model.EmergencyProvider;
import com.safecell.utilities.StreamToStringHelper;
import com.safecell.utilities.TAGS;
import com.safecell.utilities.URLs;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class EmergencyHandler extends AsyncTask<Void, Void, String> {

	private static final String TAG = EmergencyHandler.class.getSimpleName();
	private int profileid;
	private Context context;

	public EmergencyHandler(Context context, int profileid) {
		this.profileid = profileid;
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		// delete emergency database
		context.getContentResolver()
				.delete(Emergencies.CONTENT_URI, null, null);
		super.onPreExecute();
	}

	@Override
	protected String doInBackground(Void... params) {

		Log.d(TAG, "Sending request for emergency configuration");
		String response = postRequest(profileid);
		return response;
	}

	@Override
	protected void onPostExecute(String response) {
		if (response != null && !response.isEmpty()) {
			Log.v(TAG, "Emergency JSON Object: " + response);
			Log.d(TAG, "Parsing emergency request response");

			JSONObject jsonObject;
			try {

				// Delete emergencys
				context.getContentResolver().delete(Emergencies.CONTENT_URI,
						null, null);

				// Add emergency numbers to db
				jsonObject = new JSONObject(response);
				JSONArray emergencyconfigObj = jsonObject
						.getJSONArray("emergencynumbers");
				int totalnumbers = emergencyconfigObj.length();
				Log.d(TAG, "Number of emergency:" + emergencyconfigObj.length());
				ArrayList<String> emergencynames = new ArrayList<String>();
				ArrayList<String> emergencynumbers = new ArrayList<String>();
				for (int i = 0; i < totalnumbers; i++) {
					String name = emergencyconfigObj.getJSONObject(i)
							.getString("name");
					String number = emergencyconfigObj.getJSONObject(i)
							.getString("value");
					boolean inbound = emergencyconfigObj.getJSONObject(i)
							.getBoolean("inbound");
					if (!name.isEmpty() && !number.isEmpty()) {
						emergencynames.add(name);
						emergencynumbers.add(number);
						// configuring emergency numbers
						setEmergencyNumbers(name, number, inbound);
					}
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			Log.e(TAG, "Response is null");
		}
		super.onPostExecute(response);
	}

	private void setEmergencyNumbers(String name, String number, boolean inbound) {

		Log.d(TAG, "Loading Emergency Name:" + name + " , Emergency Number: "
				+ number + " Inbound: " + inbound);
		ContentValues cvalue1 = new ContentValues();
		cvalue1.put("name", name);
		cvalue1.put("number", number);
		context.getContentResolver().insert(Emergencies.CONTENT_URI, cvalue1);

		// Add inbound details into Inbound_Details HashTable
		Emergencies.Inbound_Details.put(number, inbound);

	}

	private String postRequest(int id) {

		String url = Uri.decode(URLs.EMERGENCY_URL + id);
		String result = null;
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		Log.v(TAG, "Emergency Configuration URL = " + url);
		HttpResponse response;

		try {
			response = httpclient.execute(httpget);
			Log.d(TAG, "Status:[" + response.getStatusLine().toString() + "]");

			HttpEntity entity = response.getEntity();

			if (response.getStatusLine().toString()
					.equalsIgnoreCase("HTTP/1.1 200 OK")) {
				if (entity != null) {

					InputStream instream = entity.getContent();
					result = StreamToStringHelper
							.convertStreamToString(instream);
					instream.close();
					return result;
				}
			} else {

				result = null;
			}

		} catch (Exception e) {
			Log.d(TAG, "Exception");
			result = null;
			e.printStackTrace();

		}
		return result;

	}

}
