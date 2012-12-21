package com.safecell.networking;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONObject;

import com.safecell.utilities.URLs;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class SigninHanlder extends AbstractProxy {

	private static final String TAG = SigninHanlder.class.getSimpleName();

	/** HttpClient response as string */
	String response_body = "";

	/** User login details json object. */
	JSONObject jsonObject = new JSONObject();

	public SigninHanlder(Context context, JSONObject json) {
		super(context);
		this.jsonObject = json;
	}

	/**
	 * User login details json object is forwarded to the server database using
	 * {@link HttpClient}, retrieves the response body and return the response
	 * body as string.
	 * 
	 * @return - response body as string
	 */
	public String accountLogin() {

		HttpClient client = new DefaultHttpClient();
		HttpConnectionParams.setConnectionTimeout(client.getParams(), 60000); // Timeout
		String url = Uri.decode(URLs.REMOTE_URL + "api/1/user_session");

		Log.v(TAG, "Login URL = " + url);
		try {

			HttpPost post = new HttpPost(url);
			StringEntity stringEntity = new StringEntity(jsonObject.toString());
			post.setHeader("Content-Type", "application/json");
			post.setEntity(stringEntity);
			Log.d(TAG, "Setting entity = " + jsonObject.toString());
			Log.d(TAG, "HttpPost as string = " + post.toString());
			response = client.execute(post);
			response_body = getResponseBody();
			statusCode = response.getStatusLine().getStatusCode();
			Log.d(TAG, "HttpPost response status code = " + statusCode);
			if (statusCode != 200) {
				response = null;
				response_body = null;
				failureMessage = "The Login fail. Server Response code incorrect. "
						+ statusCode;
			}
			if (statusCode == 422) {
				response = null;
				response_body = null;
				failureMessage = "The username or password is incorrect.";
			}

		} catch (Exception e) {
			e.printStackTrace();
			response = null;
			response_body = null;
			failureMessage = "The Login fail, exception raise while sending request. Cause: "
					+ e.getMessage();
		}
		return response_body;

	}

	/**
	 * Getter method of response body.
	 * 
	 * @return
	 */
	public String getResponse_body() {
		return response_body;
	}

	/**
	 * Setter method of response body.
	 * 
	 * @param response_body
	 */
	public void setResponse_body(String response_body) {
		this.response_body = response_body;
	}
}
