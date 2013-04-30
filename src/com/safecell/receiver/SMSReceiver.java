package com.safecell.receiver;

import java.sql.Date;

import org.ispeech.SpeechSynthesis;
import org.ispeech.SpeechSynthesisEvent;
import org.ispeech.error.BusyException;
import org.ispeech.error.InvalidApiKeyException;
import org.ispeech.error.NoNetworkException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.safecell.TrackingScreenActivity;
import com.safecell.TrackingService;
import com.safecell.dataaccess.SMSRepository;
import com.safecell.model.SCSms;
import com.safecell.utilities.ConfigurePreferences;
import com.safecell.utilities.DateUtils;
import com.safecell.utilities.TAGS;

public class SMSReceiver extends BroadcastReceiver {

	private String TAG = SMSReceiver.class.getSimpleName();

	public static final Uri uriSms = Uri.parse("content://sms");

	private SMSRepository smsRepository;
	private SCSms scSms;
	private boolean isSendSMS = true;

	public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	Parcel parcel;
	Date date;

	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "Intent Action = " + intent.getAction());
		smsRepository = new SMSRepository(context);
		scSms = new SCSms();
		this.mContext = context;
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"TRIP", 1);
		boolean smsBlock = sharedPreferences.getBoolean("isTripStarted", false);
		isSendSMS = context.getSharedPreferences("SMSAutoReplyCheckBox",
				Context.MODE_WORLD_READABLE).getBoolean("isAutoreply", true);
		Log.d(TAG, "Intent " + intent.getAction());
		//SpeechService speechservice = new SpeechService(mContext);
		
		if(smsBlock && new ConfigurePreferences(mContext).isTripAbandon()) {
			Log.v(TAG, "SMS receive when trip abandon");
			return;
		}
		if ((!(intent.getAction()
				.equals("android.provider.Telephony.SMS_RECEIVED")))
				|| !smsBlock) {

			return;
		}
		if ((intent.getAction()
				.equals("android.provider.Telephony.SMS_RECEIVED")) && smsBlock) {

			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;
			String str = "";
			long timeMillisecond = 0;

			if (bundle != null) {
				// ---retrieve the SMS message received---
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];

				for (int i = 0; i < msgs.length; i++) {
					msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

					str += "SMS from " + msgs[i].getOriginatingAddress();
					str += " :";
					str += msgs[i].getMessageBody().toString();
					str += "\n";
					timeMillisecond = msgs[i].getTimestampMillis();

					// make sms audible
					/*if (SpeechService.synthesis != null) {// if
															// (TrackingScreenActivity.synthesis
															// != null) {
						String headder_message = "You have one incoming message. Message is. ";
						String msge = headder_message
								+ msgs[i].getMessageBody().toString();
						Log.v(TAG, msge);
						try {
							SpeechService.synthesis.speak(msge);
						} catch (BusyException e) {
							Log.e(TAG, "Busy while specking message");
							Toast.makeText(context, "ERROR: SDK is busy",
									Toast.LENGTH_LONG).show();
							e.printStackTrace();
						} catch (NoNetworkException e) {
							Log.e(TAG,
									"NoNetworkException while specking message");
							Toast.makeText(context,
									"ERROR: Network is not available",
									Toast.LENGTH_LONG).show();
							e.printStackTrace();
						} catch (Exception e) {
							Log.e(TAG, "Exception while specking message");
							e.printStackTrace();
						}
						SpeechService.synthesis.stop();
					}*/

					scSms.setAddress(msgs[i].getOriginatingAddress());
					scSms.setBody(msgs[i].getMessageBody());
					scSms.setDate(msgs[i].getTimestampMillis());

					scSms.setProtocol(msgs[i].getProtocolIdentifier());
					scSms.setRead(0);
					scSms.setReply_path_present(0);
					scSms.setService_center(msgs[i].getServiceCenterAddress());
					scSms.setStatus(msgs[i].getStatus());
					scSms.setSubject(msgs[i].getPseudoSubject());
					scSms.setThread_id(0);
					scSms.setType(0);
					scSms.setLocked(0);

					smsRepository.insertQuery(scSms);

				}

				// checking controller incoming message
				String address = scSms.getAddress();
				Log.v(TAG, "Received SMS Source = " + address);
				Toast.makeText(mContext, "SMS received from " + address,
						Toast.LENGTH_LONG).show();
				// ---display the new SMS message---
				date = new Date(timeMillisecond);

				// check incoming msg from controller.
				if (checkControllerMsg()) {

					// Disable SAVE TRIP Flag.
					new ConfigurePreferences(context).setSAVETRIP(false);
					Log.v(TAG, "SAVE TRIP Disabled ");
					Toast.makeText(context, "SAVE TRIP Disabled",
							Toast.LENGTH_LONG).show();
					
					// Set abandon flag in preferences
					new ConfigurePreferences(context).isTripAbandon(true);
					Log.v(TAG, "Trip is Abandoned");
					Toast.makeText(context, "Trip is Abandoned",
							Toast.LENGTH_LONG).show();
					
					// update abondon details like response time
					TrackingScreenActivity.Abondon_Details
							.setResponse_time(DateUtils.getTimeStamp(date
									.getTime()));
					
					TrackingService.ABANDONFLAG = true;
				} else if (isSendSMS) {
					Log.v(TAG, "Incoming number (" + address
							+ ") is not a controller number");
					// checking incoming number already called
					if (!TrackingService.incomingSMSNumberArrayList
							.contains(address)) {
						TrackingService.incomingSMSNumberArrayList.add(address);
						Log.v(TAG, "Incoming sms occured. auto reply sent:"
								+ isSendSMS);
						sendmessage();
					}
				}

				//this.abortBroadcast();

			}
		}
	}

	private void sendmessage() {
		Log.v(TAG, "Sending auto reply message back");
		String destinationAddress = scSms.getAddress();
		//String senderMessage = "The person was in driving and will receive your message upon destination reach.";
		/*
		 * SmsManager smsManager = SmsManager.getDefault(); String
		 * destinationAddress = scSms.getAddress(); String senderMessage=
		 * "The person you are trying to reach is driving and will receive your message upon reaching their destination. Learn more: http://safecellapp.com."
		 * ; smsManOager.sendTextMessage(destinationAddress, null,senderMessage
		 * , null, null);
		 */
		sendSMS(destinationAddress, TAGS.AUTO_REPLY);
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
		Log.v(TAG, "Message sent to " + phoneNumber);
	}

	private boolean checkControllerMsg() {

		String address = scSms.getAddress();
		String sms_body = scSms.getBody();
		// checking any abondon request are made
		if (TrackingScreenActivity.Abondon_Details == null) {
			Log.v(TAG, "No Turn Off Request made yet.");
			return false;
		} else {
			String pin_number = TrackingScreenActivity.Abondon_Details
					.getAbodon_pin();
			String control_number = TrackingScreenActivity.Abondon_Details
					.getController_number();
			Log.d(TAG, "Inbound number: " + address + " , Controller number: "
					+ control_number);
			if (address.equals(control_number)) {
				if (sms_body.contains("Yes") || sms_body.contains("yES")
						|| sms_body.contains("YES")
						|| sms_body.equalsIgnoreCase("yes")
						|| sms_body.contains("YEs")) {
					return true;
				}

			}
		}
		return false;
	}

}
