package com.safecell.networking;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPOutputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import com.safecell.TrackingScreenActivity;
import com.safecell.TrackingService;
import com.safecell.dataaccess.AccountRepository;
import com.safecell.dataaccess.InteruptionRepository;
import com.safecell.dataaccess.ProfilesRepository;
import com.safecell.dataaccess.TempTripJourneyWayPointsRepository;
import com.safecell.utilities.DateUtils;
import com.safecell.utilities.URLs;
import com.safecell.utilities.Util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class TripSyncHandler extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "TripSyncHandler";
	private Context context;
	private JSONObject Created_Json;
	private JSONObject savingJsonObject;
	private HttpResponse response;
	private String apiKey;
	private Integer accountID;
	private int profileID;
	private String Failure_MSG = "";
	public boolean isPreviousSyncFail;
	private long SYNC_WAIT_TIME_MINITS = 1; //mintis

	public TripSyncHandler(Context context) {
		this.context = context;

		// retriving apikey & accountid & profileid
		AccountRepository accountRepository = new AccountRepository(context);
		HashMap<Object, Object> ApikeyAndAccountId = accountRepository
				.selectApiKeyAndAccountID();
		apiKey = (String) ApikeyAndAccountId.get("ApiKey");
		accountID = Integer.valueOf(ApikeyAndAccountId.get("AccountId")
				.toString());

		ProfilesRepository profilesRepository = new ProfilesRepository(context);
		profileID = profilesRepository.getId();
	}

	@Override
	protected void onPreExecute() {

		// is_partial_trip flag on SubmitNewJourney
		SubmitNewTripJourney.is_partial_trip = true;
		// Pause gps location read
		TrackingScreenActivity.isTripSavingInProgress = true;
		TrackingService.ignoreLocationUpdates = true;
		Toast.makeText(context, "Trip data sync starting.", Toast.LENGTH_SHORT).show();
		super.onPreExecute();
	}

	@Override
	protected Boolean doInBackground(Void... params) {

		try {
			// prepare json for local data
			Created_Json = new SubmitNewTripJourney(context).createJson();

			if (Created_Json == null) {
				Failure_MSG = "Failled to create JSON";
				return false;
			}
			Log.v(TAG, "Sync Json: "+Created_Json.toString());
			String url = createAPiPath(accountID, profileID);

			HttpResponse postRequest = postRequest(Created_Json, url);

			if (postRequest == null) {
				Failure_MSG = "Server response null";
				return false;
			}
		} catch (Exception e) {
			Failure_MSG = " Syncing Error";
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		// is_partial_trip flag on SubmitNewJourney
		SubmitNewTripJourney.is_partial_trip = false;
		// start gps location read
		TrackingScreenActivity.isTripSavingInProgress = false;
		TrackingService.ignoreLocationUpdates = false;
		
		if(result) {
			
			Toast.makeText(context, "Trip data sync sucess.", Toast.LENGTH_SHORT).show();
			// clear appoints and interruptions
			new TempTripJourneyWayPointsRepository(context).deleteTripWaypoints();
			new InteruptionRepository(context).deleteInteruptions();
			
			isPreviousSyncFail = false;
		}
		else {
			
			Toast.makeText(context, Failure_MSG, Toast.LENGTH_SHORT).show();
			
			// update flag and start timer
			isPreviousSyncFail = true;
			
			
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					isPreviousSyncFail = false;
					
				}
			}, Util.minitToMilliSeconds(SYNC_WAIT_TIME_MINITS));
			
		}
		super.onPostExecute(result);
	}

	public HttpResponse postRequest(JSONObject jsonObject, String url) {
		this.savingJsonObject = jsonObject;
		return postRequest(url);
	}

	public HttpResponse postRequest(String url) {

		Log.e(TAG, "*************Submiting trip json to server****************");

		DefaultHttpClient client = new DefaultHttpClient();

		client.addRequestInterceptor(new HttpRequestInterceptor() {

			public void process(final HttpRequest request,
					final HttpContext context) throws HttpException,
					IOException {
				if (!request.containsHeader("Accept-Encoding")) {
					// Log.v("Header", "Accept-Encoding");
					request.addHeader("Accept-Encoding", "gzip");
				}
			}

		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {

			public void process(final HttpResponse response,
					final HttpContext context) throws HttpException,
					IOException {
				HttpEntity entity = response.getEntity();
				Header ceheader = entity.getContentEncoding();
				if (ceheader != null) {
					HeaderElement[] codecs = ceheader.getElements();
					for (int i = 0; i < codecs.length; i++) {
						if (codecs[i].getName().equalsIgnoreCase("gzip")) {
							// Log.v("SafeCell: GZipInStream",
							// "GzipDecompressingEntity");
							response.setEntity(new GzipDecompressingEntity(
									response.getEntity()));
							return;
						}
					}
				}
			}

		});

		HttpConnectionParams.setConnectionTimeout(client.getParams(), 60000); // Timeout

		Log.v(TAG, "Posting URL: " + url);
		HttpPost post = new HttpPost(url);
		// System.out.println(url);

		try {

			ByteArrayOutputStream arr = new ByteArrayOutputStream();

			OutputStream zipper = new GZIPOutputStream(arr);
			// Messaged on the file
			// Log.d(TAG, "Trip Name = "+tripName);
			// generateMessageOnSD(tripName,outerJsonObject.toString());

			zipper.write(savingJsonObject.toString().getBytes());
			zipper.close();

			ByteArrayEntity entity = new ByteArrayEntity(arr.toByteArray());
			entity.setContentEncoding("gzip");

			// stringEntity = new StringEntity(outerJsonObject.toString());
			post.setHeader("Content-Type", "application/json");
			post.setHeader("x-api-key", apiKey);
			// System.out.println(apiKey);

			post.setEntity(entity);
			response = client.execute(post);
			// Log.v("Safecell :"+"Trip Response",
			// response.getStatusLine().toString());
			// System.out.println("----------------------------------------");
			// System.out.println(response.getStatusLine());
			// System.out.println(response.getLastHeader("Content-Encoding"));
			// System.out.println(response.getLastHeader("Content-Length"));
			// System.out.println("----------------------------------------");

			if (response.getStatusLine().toString()
					.equalsIgnoreCase("HTTP/1.1 200 OK")) {
				return response;
			} else {
				// Log.v("safecell", response.getStatusLine().toString());

				response = null;
			}

			return response;
		} catch (SocketTimeoutException e1) {
			// Log.v("SaveTrip", "SocketTimeoutException");
			response = null;
			post.abort();

		} catch (Exception e) {
			e.printStackTrace();
			response = null;
			post.abort();
		}
		return response;
	}

	private String createAPiPath(int accountID, int profileID) {
		return URLs.REMOTE_URL + "api/1/triplogs?account_id=" + accountID
				+ "&profile_id=" + profileID;
	}

}
