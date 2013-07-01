/**
 * 
 */
package com.safecell.utilities;

import org.json.JSONObject;

/**
 * @author treewalker
 * 
 */
public class TAGS {

	public static final String TAG = "SafeCell";

	public static final String TAG_INACTIVE = "Your account is inactive. Please call customer service at 1-800-XXX-XXXX to active you account or go to www.safecellapp.mobi with controller account and edit your profile.";
	
	public static final String AUTO_REPLY = "The person you are trying to reach is intransit and will contact you upon reaching their destination.";

	public static String license_expire_date;

	public static final String TAG_EXPIRE = "You SafeCell license expired on"
			+ license_expire_date
			+ ". Please log on the www.safecellapp.mobi with your userid and password and renew the license";

	public static final String TAG_MANAGER = "Cannot login with Manager Account. Please provide a registered device user.";

	

	public static String CONTORL_NUMBER = "+918971855771";

	public static boolean SHOW_SPLASH = true;
	
	
	public static int tripStartSpeed = 5;

	public static int tripStopTime = 2;

	public static boolean disableEmail = true;

	public static boolean disableCall = true;

	public static boolean disableTexting = true;

	public static boolean disableWeb = true;

	public static boolean logWayPoints = true;

	public static boolean keypadLock = true;

	public static double TRIP_SYNC_DISTANCE = 2; // Miles

	public static String CURRENT_TRIPNAME = "";

	public static double PREV_SYNC_MILES = 0;
	
	public static final double LOCATION_DISTANCE_THRESHOLD = 0.08; //miles

	public static final double FALSE_TRIP_TIME_THRESHOLD = 15;    // minits

	public static final long EMERGENCY_TRIP_HALT_TIME = 30;    // minits
	
	public static boolean IS_EMERGENCY_HALT_ACTIVATED = false; 
	

	public static String ABANDON_REASON = "";
	
	
	/* Represent re-check time interval for trip start time config. */
	public static int recheckInterval = 2;

	public static String dayOfWeek = "Sunday";

	public static boolean isActive = true;

	public static String startTime = "16:40";

	public static String endTime = "17:00";
	

}
