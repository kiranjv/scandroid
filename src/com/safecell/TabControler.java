package com.safecell;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebView.FindListener;
import android.widget.Button;


import com.safecell.utilities.URLs;

public class TabControler {
	OnClickListener onClickListener;
	Context context;
	private AlertDialog alertDialogForTermsConditions;
	private WebView wv;

	public TabControler(Context context) {
		this.context = context;

	}

	public OnClickListener getHomeTabOnClickListner() {
		onClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (TrackingService.homeScreenActivity != null) {
					TrackingService.homeScreenActivity.finish();
				}
				Intent mIntent = new Intent(context, HomeScreenActivity.class);
				context.startActivity(mIntent);
			}
		};
		return onClickListener;
	}

	public OnClickListener getMyTripsOnClickListner() {
		onClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent mIntent = new Intent(context, MyTripsActivity.class);
				context.startActivity(mIntent);

			}
		};
		return onClickListener;
	}

	public OnClickListener getRulesOnClickListner() {
		onClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent mIntent = new Intent(context,
						ViewRulesListActivity.class);
				context.startActivity(mIntent);

			}
		};
		return onClickListener;
	}

	public OnClickListener getSettingOnClickListener() {
		// disabling setting
		SharedPreferences preferences = context.getSharedPreferences(
				"SETTINGS", Context.MODE_WORLD_READABLE);
		boolean isDisabled = preferences.getBoolean("isDisabled", false);
		if (isDisabled) {
			onClickListener = null;
		} else {
			onClickListener = new OnClickListener() {

				@Override
				public void onClick(View v) {

					Intent mIntent = new Intent(context,
							SettingScreenActivity.class);
					context.startActivity(mIntent);

				}
			};
		}
		return null;

	}

	public OnClickListener getFaxOnClickListener() {
		// Load fax view
		onClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Show fax data in web view
				Intent mIntent = new Intent(context,
						SettingScreenActivity.class);
				context.startActivity(mIntent);

			}
		};

		return onClickListener;
	}
	
	public void dialogforWebviewFax(String url, final Activity activity, Context mContext) {
		final ProgressDialog progressDialog = new ProgressDialog(activity);
		AlertDialog.Builder builder;

		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.private_policy_layout,
				(ViewGroup) activity.findViewById(R.id.layout_root));

		
		wv = (WebView) layout.findViewById(R.id.webview);

		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

		wv.setWebViewClient(new HelloWebViewClient());
		wv.setWebChromeClient(new WebChromeClient() {

			public void onProgressChanged(WebView view, int newProgress) {
				activity.setProgress(newProgress * 100);
				progressDialog.setProgress(newProgress);
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
		wv.loadUrl(url); // URLs.REMOTE_URL +
							// "api/1/site_setting/terms_of_service.html");

		builder = new AlertDialog.Builder(mContext);
		builder.setView(layout);
		builder.setTitle("FAQ");

		builder.setPositiveButton("Close",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Button faxButton = (Button) activity.findViewById(R.id.tabBarFaxButton);
						faxButton.setBackgroundResource(R.drawable.fax_unclick);
						dialog.cancel();
					}
				});
		alertDialogForTermsConditions = builder.create();

		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		alertDialogForTermsConditions.show();
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
	
}
