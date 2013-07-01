package com.safecell.utilities;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.util.TimeUtils;

public class DateUtils {

	public DateUtils() {
		// TODO Auto-generated constructor stub
	}

	public static long dateInMillSecond(String dateString) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss'Z'");
		// simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		java.util.Date date = null;
		try {

			date = simpleDateFormat.parse(dateString);

			// System.out.println("" + date.getTime());

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return date.getTime();

	}

	public static String dateInString(long timeInMillSecond) {
		Date date = new Date(timeInMillSecond);
		SimpleDateFormat simpleDate = new SimpleDateFormat(
				"MMM dd',' yyyy hh:mm a");

		// simpleDate.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateString = simpleDate.format(date);
		// Log.v("Safecell :"+"Date", dateString);
		return dateString;
	}

	public static String getTimeStamp(long timeInMillSecond) {
		// Log.v("Safecell :"+"timeInMillSecond",""+timeInMillSecond);
		Date date = new Date(timeInMillSecond);
		SimpleDateFormat simpleDate = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss'Z'");

		// simpleDate.setTimeZone(TimeZone.getTimeZone("UTC"));

		String dateString = simpleDate.format(date);

		return dateString;
	}

	public static String getTimeStampUTC(long timeInMillSecond) {
		Date date = new Date(timeInMillSecond);
		SimpleDateFormat simpleDate = new SimpleDateFormat(
				"MMM dd',' yyyy hh:mm a");

		simpleDate.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateString = simpleDate.format(date);
		// Log.v("Safecell :"+"Date", dateString);
		return dateString;
	}

	public static String getDate(long timeInMillSecond) {
		// Log.v("Safecell :"+"timeInMillSecond",""+timeInMillSecond);
		Date date = new Date(timeInMillSecond);
		SimpleDateFormat simpleDate = new SimpleDateFormat("MMM dd',' yyyy");
		// simpleDate.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateString = simpleDate.format(date);
		// Log.v("Safecell :"+"Date", dateString);
		return dateString;
	}

	public static String getTodayDayOfWeek() {
		Calendar now = Calendar.getInstance();
		// create an array of days
		String[] strDays = new String[] { "Sunday", "Monday", "Tuesday",
				"Wednesday", "Thursday", "Friday", "Saturday" };

		// Day_OF_WEEK starts from 1 while array index starts from 0
		String day = strDays[now.get(Calendar.DAY_OF_WEEK) - 1];
		System.out.println("Current day is : " + day);
		return day;
	}

	public static boolean isTimeElapsed(String startTime) {

		Date c_date = new Date(System.currentTimeMillis());
		System.out.println("Current date: " + c_date.toLocaleString());

		SimpleDateFormat simpleDate = new SimpleDateFormat("HH:MM");
		// simpleDate.setTimeZone(TimeZone.getTimeZone("UTC"));
		String currentTime = simpleDate.format(c_date);
		// Log.v("Safecell :"+"Date", dateString);
		System.out.println(currentTime);

		// compare both strings
		String[] split_starttime = startTime.split(":");
		String[] split_current = currentTime.split(":");

		int start_hours = Integer.parseInt(split_starttime[0]);
		int start_mins = Integer.parseInt(split_starttime[1]);

		int curr_hours = Integer.parseInt(split_current[0]);
		int curr_mins = Integer.parseInt(split_current[1]);

		if (start_hours < curr_hours) {
			return true;

		} else if (start_hours > curr_hours) {
			return false;
		} else {
			// hours same check minits
			if (start_mins <= curr_mins)
				return true;
			else
				return false;
		}

	}

}
