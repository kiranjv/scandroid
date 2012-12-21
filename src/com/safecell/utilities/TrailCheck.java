package com.safecell.utilities;

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.R.string;
import android.content.Context;
import android.util.Log;

public class TrailCheck {

	public static String messsge;
	public static String title = "Trail Mode";
	private static long remain_days = 0;
	public static String expire_date;
	private final static String TAG = TrailCheck.class.getSimpleName();

	/**
	 * Check the trail version, trail version expires then return true value
	 * which means quit application. Otherwise return false.
	 * 
	 * @param context
	 * @param Current_profile
	 * @return
	 */
	public static boolean validateExpireOn(Context context, String start_date,
			String subcription) {

		if (start_date == null || start_date.equals("null")) {
			UIUtils.OkDialog(context, "start_date is null");
			Log.d(TAG, "Start date is - " + start_date);
			return false;

		}

		else {

			expire_date = calcExpireDate(start_date, subcription);

			if (expire_date == null) {
				Log.e(TAG, "Expire date is nulll");
			}
			long remaining_days = calcRemainingDays(expire_date);
			Log.d(TAG, "Remaining days = " + remaining_days);
			if (remaining_days < 0) {
				messsge = "Your trial period has expired. Please ask the account owner to purchase to activate subscription. The application will now terminate. Please restart the application after activation.";

				Log.d(TAG, "Your trail expired....");
				// quitDialog(context, title, messsge);
				return true;
			} else {
				messsge = remaining_days == 0 ? "Last day of your trial period."
						: (remaining_days + 1)
								+ " more days remaining to expire.";
				Log.d(TAG, (remaining_days + 1)
						+ " more days remaining to expire.");

				return false;
			}
		}
	}

	/**
	 * Calculate remaining days to expire the application.
	 * 
	 * @param expire_date
	 * @return number of days license expire.
	 */
	private static long calcRemainingDays(String expire_date) {

		// remove extra chars
		expire_date = expire_date.replace('T', ' ');
		expire_date = expire_date.replace('Z', ' ');

		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date date;
		try {
			date = (Date) formatter.parse(expire_date);
			long exp_milli = date.getTime();
			long currnt_milli = System.currentTimeMillis();
			remain_days = (exp_milli - currnt_milli) / (24 * 60 * 60 * 1000);
			return remain_days;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return remain_days;
	}

	public static long getRemain_days() {
		return remain_days;
	}

	public static String getMesssge() {
		return messsge;
	}

	/**
	 * Check the start date is after the current date.
	 * 
	 * @param context
	 * @param start_date
	 * @return
	 */
	public static boolean validateStartDate(Context context, String start_date) {

		Date startdate = parseDate(start_date);
		Date today = new Date(System.currentTimeMillis());
		Log.d(TAG, "License start date: " + startdate.toGMTString());
		Log.d(TAG, "Today date: " + today.toGMTString());
		if (today.before(startdate))
			return true;
		else
			return false;
	}

	/**
	 * Calculate the expire date based on the start date and subscription
	 * 
	 * @param start_date
	 *            - date of the license start
	 * @param subcription
	 *            - years license ends
	 * @return
	 */
	private static String calcExpireDate(String start_date, String subcription) {

		Date startdate = parseDate(start_date);
		Calendar calender = Calendar.getInstance();
		calender.setTime(startdate);
		// Format f = new SimpleDateFormat("dd-MMMM-yyyy");
		Format f = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Log.d(TAG, f.format(calender.getTime()));
		calender.add(Calendar.YEAR, Integer.parseInt(subcription));

		String expire_date = f.format(calender.getTime());

		Log.i(TAG, "Caleculated Expirity date = " + expire_date);
		return expire_date;
	}

	public static Date parseDate(String date) {
		// remove extra chars
		date = date.replace('T', ' ');
		date = date.replace('Z', ' ');

		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date startdate;
		try {
			startdate = (Date) formatter.parse(date);
			return startdate;
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}

	}
}
