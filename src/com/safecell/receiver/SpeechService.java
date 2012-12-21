package com.safecell.receiver;

import org.ispeech.SpeechSynthesis;
import org.ispeech.SpeechSynthesisEvent;
import org.ispeech.error.BusyException;
import org.ispeech.error.InvalidApiKeyException;
import org.ispeech.error.NoNetworkException;

import com.safecell.AddTripActivity;
import com.safecell.TrackingService;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 
 *
 */
public class SpeechService {

	private static final String TAG = "SpeechService";
	public static SpeechSynthesis synthesis = null;
	Context _context;

	public SpeechService(Context context) {
		this._context = context;
		// _context = this.getApplicationContext();
		try {
		prepareTTSEngine();
		}
		catch(Exception e)
		{
			Log.e(TAG, "Exception while preparing iSpeech engine.");
			e.printStackTrace();
		}
	}

	private void prepareTTSEngine() {
		Log.v(TAG, "Initializing speech engine");
		try {
			if (TrackingService.trackingScreenActivity != null) {

				synthesis = SpeechSynthesis
						.getInstance(TrackingService.trackingScreenActivity);

			} else if (TrackingService.homeScreenActivity != null) {
				synthesis = SpeechSynthesis
						.getInstance(TrackingService.homeScreenActivity);

			}

			synthesis.setSpeechSynthesisEvent(new SpeechSynthesisEvent() {

				public void onPlaySuccessful() {
					Log.v(TAG, "onPlaySuccessful");
					TrackingService.iSpeechStarted = false;
				}

				public void onPlayStopped() {
					Log.v(TAG, "onPlayStopped");
					TrackingService.iSpeechStarted = false;
				}

				public void onPlayFailed(Exception e) {
					Log.e(TAG, "onPlayFailed");
					TrackingService.iSpeechStarted = false;
					e.printStackTrace();
				}

				public void onPlayStart() {
					Log.v(TAG, "onPlayStart");
					TrackingService.iSpeechStarted = true;
					AudioManager aManager = (AudioManager) TrackingService.context
							.getSystemService(Context.AUDIO_SERVICE);
					aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				}

				@Override
				public void onPlayCanceled() {
					Log.v(TAG, "onPlayCanceled");
				}

			});

			// synthesis.setVoiceType("usenglishfemale1"); // All the values
			// available to you can be found in the developer portal under your
			// account

		} catch (InvalidApiKeyException e) {
			Log.e(TAG, "Invalid API key\n" + e.getStackTrace());
			Toast.makeText(_context, "ERROR: Invalid API key",
					Toast.LENGTH_LONG).show();
		}

	}

	public void makeAudible(String message) {

		try {
			String ttsText = "THis is Demo";
			synthesis.speak(ttsText);

		} catch (BusyException e) {
			Log.e(TAG, "SDK is busy");
			e.printStackTrace();
			Toast.makeText(_context, "ERROR: SDK is busy", Toast.LENGTH_LONG)
					.show();
		} catch (NoNetworkException e) {
			Log.e(TAG, "Network is not available\n" + e.getStackTrace());
			Toast.makeText(_context, "ERROR: Network is not available",
					Toast.LENGTH_LONG).show();
		}

	}

	public class OnStopListener implements OnClickListener {

		public void onClick(View v) {
			if (synthesis != null) {
				synthesis.stop();
			}
		}
	}

}