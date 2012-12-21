package com.safecell;

import com.android.internal.telephony.ITelephony;
import com.safecell.dataaccess.ContactRepository;
import com.safecell.model.SCInterruption;
import com.safecell.model.Emergency.Emergencies;
import com.safecell.networking.ConfigurationHandler;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.Util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;

public class PhoneReceiver extends BroadcastReceiver {

	public static boolean EMERGENCYCALL_ACTIVE = false;

	TelephonyManager telephonyManager;
	private ITelephony telephonyService;
	int events = PhoneStateListener.LISTEN_SIGNAL_STRENGTH
			| PhoneStateListener.LISTEN_DATA_ACTIVITY
			| PhoneStateListener.LISTEN_CELL_LOCATION
			| PhoneStateListener.LISTEN_CALL_STATE
			| PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
			| PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
			| PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
			| PhoneStateListener.LISTEN_SERVICE_STATE;
	private boolean isSendSMS;
	private PhoneStateListener phoneStateListener;

	private Context mContext;

	private String TAG = PhoneReceiver.class.getSimpleName();
	private static boolean ALREADY_CALL = false;

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences preferences = context.getSharedPreferences("TRIP", 1);

		this.mContext = context;
		Log.d(TAG, "Intent Action:" + intent.getAction());
		if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
			// check already in call
			if (ALREADY_CALL) {
				ALREADY_CALL = false;
				Log.v(TAG, "Already call is running. Ending call");
				if (new ConfigurePreferences(context).getEmergencyTRIPSAVE()) {
					Log.v(TAG, "Clearing emergency active settings");
					new ConfigurePreferences(context)
							.setEmergencyTripSave(false);
				}
			} else {
				ALREADY_CALL = true;
				Log.d(TAG, "Making call");
			}
		}

		telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		connectToTelephonyService();

		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"TRIP", 1);
		boolean b = sharedPreferences.getBoolean("isTripStarted", false);

		isSendSMS = context.getSharedPreferences("SMSAutoReplyCheckBox",
				Context.MODE_WORLD_READABLE).getBoolean("isAutoreply", true);

		if (!intent.getAction().equals("android.intent.action.PHONE_STATE"))
			return;
	
		
		String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

		if(b && new ConfigurePreferences(mContext).isTripAbandon()) {
			Log.v(TAG, "Call received when trip abandon");
			return;
		}
		
		
		if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && b) {
			// Allow emergency numbers incoming
			String phonenumber = intent
					.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

			Log.v(TAG, "extra state phone ringing number: " + phonenumber);

			// Log.v("Safecell :"+"Line Number",
			// telephonyManager.getVoiceMailNumber());

			TrackingScreenActivity.INCOMING_CALL_OCCUER = true;
			telephonyManager.listen(phoneStateListener, events);
			// this is not allowed
			// telephonyService.silenceRinger();

			// telephonyService.endCall();
			TrackingScreenActivity.incomingCallCounter++;

		}
	}

	@SuppressWarnings("unchecked")
	private void connectToTelephonyService() {
		try {
			Class c = Class.forName(telephonyManager.getClass().getName());
			Method m = c.getDeclaredMethod("getITelephony");
			m.setAccessible(true);
			// telephonyService = (ITelephony)m.invoke(tm);
			telephonyService = (ITelephony) m.invoke(telephonyManager);

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("call prompt",
					"FATAL ERROR: could not connect to telephony subsystem");
			Log.e("call prompt", "Exception object: " + e);
			// finish();
		}

		phoneStateListener = new PhoneStateListener() {

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				super.onCallStateChanged(state, incomingNumber);
				String callState = TelephonyManager.EXTRA_STATE_RINGING;
				Log.d(TAG, "STATE:" + state);
				
				switch (state) {

				case TelephonyManager.CALL_STATE_RINGING:
					callState = "Ringing (" + incomingNumber + ")";

					boolean is_emergency = isEmergencyNumber(incomingNumber);
					Log.d(TAG, "is emergency number: " + is_emergency);
					if (is_emergency) {
						Log.v(TAG,
								"Emergency number inbound status:"
										+ Emergencies.Inbound_Details.get(
												incomingNumber).booleanValue());
						boolean inbound = Emergencies.Inbound_Details.get(
								incomingNumber).booleanValue();
						if (!inbound) {
							Log.v(TAG,
									"Incoming call is emergency number. But InBound is false");
							try {
								if (ConfigurationHandler.getInstance()
										.getConfiguration().isDisableCall()) {
									telephonyService.endCall();
								}
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return;
						} else {
							// Allow incoming emergency generate interruption
							EMERGENCYCALL_ACTIVE = true;
							//Util.saveInterruption(mContext, SCInterruption.EMEI);
						}
					} else {
						Log.d(TAG, "Ending incoming call");
						try {

							if (ConfigurationHandler.getInstance()
									.getConfiguration().isDisableCall()) {
								telephonyService.endCall();
							}
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// checking incoming number already called
					if (!TrackingService.incomingNumberArrayList
							.contains(incomingNumber) && !is_emergency) {
						TrackingService.incomingNumberArrayList
								.add(incomingNumber);
						Log.v(TAG, "Incoming call occured. auto reply sent:"
								+ isSendSMS);
						if (isSendSMS) {
							if (ConfigurationHandler.getInstance()
									.getConfiguration().isDisableCall()) {
								Log.v(TAG, "Sending auto reply message to "
										+ incomingNumber);
								sendmessage(incomingNumber);
							}
						}
					}

					// Log.v("Safecell :"+"incomingNumber", callState);
					break;
				}

			}
		};

	}

	private void sendmessage(String destinationAddress) {
		String senderMessage = "The person was in driving and will receive your message upon reaching their destination.";
		sendSMS(destinationAddress, senderMessage);
	}

	/**
	 * Send sms
	 * 
	 * @param phoneNumber
	 * @param message
	 */
	public void sendSMS(String phoneNumber, String message) {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(mContext, 0,
				new Intent(SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(mContext, 0,
				new Intent(DELIVERED), 0);

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
		Log.v(TAG, "Message sent sucesfully to " + phoneNumber);
	}

	private boolean isEmergencyNumber(String number) {

		Log.v(TAG, "Checking for Emergency number");
		boolean isEmergencyNumber = false;
		Cursor cursor = mContext.getContentResolver().query(
				Emergencies.CONTENT_URI, new String[] { Emergencies.NUMBER },
				null, null, null);

		if (cursor == null) {
			Log.d(TAG, "Contact provider cursor is null");
			ContactRepository contactrepo = new ContactRepository(mContext);
			cursor = contactrepo.SelectContacts();
		}
		if (number != null && cursor != null) {
			cursor.moveToFirst();
			for (int i = 0; cursor != null && i < cursor.getCount(); i++) {
				String emergency_number = cursor.getString(cursor
						.getColumnIndex(Emergencies.NUMBER));
				Log.d(TAG, "**** Emergency number " + i + " = "
						+ emergency_number + " and incoming number is "
						+ number);
				if (number.equals(emergency_number)) {
					isEmergencyNumber = true;
					break;
				}
				cursor.moveToNext();
			}
			cursor.close();
		}
		return isEmergencyNumber;

	}

}