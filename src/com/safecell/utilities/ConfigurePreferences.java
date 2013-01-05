package com.safecell.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ConfigurePreferences {

	private final String TAG = ConfigurePreferences.class.getSimpleName();
	Context context;
	private static SharedPreferences sharedPreferences;

	public ConfigurePreferences(Context context) {
		this.context = context;

	}

	/**
	 * Configures the {@link SharedPreferences} for first time tracking service
	 * is running.
	 */
	public void defaultConfigure() {
		setTripStrated(false);
		setTripPaused(false);
		setisBackgroundTrip(true);
		setisTripSaving(false);
	}
	
	
	

	/**
	 * Clears all the {@link SharedPreferences} settings to default values.
	 * Internally this method calls startUpConfiguration method to set default
	 * preference.
	 */
	public void clearConfiguration() {
		defaultConfigure();
	}

	/**
	 * Set the trip starting status preference value in to the SharedPreferences
	 * 
	 * @param value
	 */
	public void setTripStrated(boolean value) {
		Log.i(TAG, "*************************************");
		Log.i(TAG, "Setting trip status to " + value);
		Log.i(TAG, "*************************************");

		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("isTripStarted", value);
		editor.commit();
	}

	/**
	 * Getter method for trip started flag preference value.
	 * 
	 * @return false by default.
	 */
	public boolean getTripStrated() {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("isTripStarted", false);

	}

	/**
	 * Set the trip pause status preference value in to the SharedPreferences
	 * 
	 * @param value
	 */
	public void setTripPaused(boolean value) {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("isTripPaused", value);
		editor.commit();
	}

	/**
	 * Getter method for trip pause flag preference value.
	 * 
	 * @return false by default.
	 */
	public boolean getTripPaused() {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("isTripPaused", false);
	}
	
	
	/**
	 * Set the trip abandon status preference value in to the SharedPreferences
	 * 
	 * @param value
	 */
	public void isTripAbandon(boolean value) {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("isTripAbandon", value);
		editor.commit();
	}

	/**
	 * Getter method for trip abandon flag preference value.
	 * 
	 * @return false by default.
	 */
	public boolean isTripAbandon() {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("isTripAbandon", false);
	}

	/**
	 * Set the isBackGround status preference value in to the SharedPreferences
	 * 
	 * @param value
	 */
	public void setisBackgroundTrip(boolean value) {
		sharedPreferences = context.getSharedPreferences("TripCheckBox",
				context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("isbackgroundtrip", value);
		editor.commit();
	}

	/**
	 * Getter method for isBackgroundTrip flag preference value.
	 * 
	 * @return true by default.
	 */
	public boolean getisBackgroundTrip() {
		sharedPreferences = context.getSharedPreferences("TripCheckBox",
				context.MODE_WORLD_WRITEABLE);
		return sharedPreferences.getBoolean("isbackgroundtrip", true);

	}

	/**
	 * Getter method for the isTripSaving flag.
	 * 
	 * @return true - If saving in progress otherwise false.
	 */
	public boolean getisTripSaving() {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("isTripSaving", false);
	}

	/**
	 * Setter method for the isTripSaving flag.
	 * 
	 * @param value
	 *            - Represent flag value to set.
	 */
	public void setisTripSaving(boolean value) {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("isTripSaving", value);
		editor.commit();
	}

	/**
	 * Getter method for the SAVETRIP flag.
	 * 
	 * @return true - If saving in progress otherwise false.
	 */
	public boolean getSAVETRIP() {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("SAVETRIP", false);
	}

	/**
	 * Setter method for the SAVETRIP flag.
	 * 
	 * @param value
	 *            - Represent flag value to set.
	 */
	public void setSAVETRIP(boolean value) {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("SAVETRIP", value);
		editor.commit();
	}

	/**
	 * Setter method for the user name flag.
	 * 
	 * @param value
	 *            - Represent user name value to set.
	 */

	public void setUserName(String username) {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("USERNAME", username);
		editor.commit();
	}

	/**
	 * Setter method for the pass word flag.
	 * 
	 * @param value
	 *            - Represent pass word flag value to set.
	 */

	public void setPassWord(String password) {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("PASSWORD", password);
		editor.commit();
	}

	/**
	 * Getter method for the User name flag.
	 * 
	 * @return User name logged in.
	 */
	public String getUserName() {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		return sharedPreferences.getString("USERNAME", null);
	}

	/**
	 * Getter method for the Password flag.
	 * 
	 * @return Password logged in.
	 */
	public String getPassWord() {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		return sharedPreferences.getString("PASSWORD", null);
	}

	/**
	 * Setter method for the selected profile index value flag.
	 * 
	 * @param value
	 *            - Represent pass word flag value to set.
	 */

	public void setProfileIndex(String index) {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("PROFILEINDEX", index);
		editor.commit();
	}

	/**
	 * Getter method for the selected profile index value flag.
	 * 
	 * @return User name logged in.
	 */
	public String getProfileIndex() {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		return sharedPreferences.getString("PROFILEINDEX", null);
	}

	/**
	 * Setter method for the selected profile ID value flag.
	 * 
	 * @param value
	 *            - Represent pass word flag value to set.
	 */

	public void setSelectedProfile(String profile) {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("SELECTED_PROFILE", profile);
		editor.commit();
	}

	/**
	 * Getter method for the selected profile ID value flag.
	 * 
	 * @return User name logged in.
	 */
	public String getSelectedProfile() {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		return sharedPreferences.getString("SELECTED_PROFILE", null);
	}

	/**
	 * Setter method for the is login value flag.
	 * 
	 * @param value
	 *            - Represent pass word flag value to set.
	 */

	public void setIsLogin(boolean value) {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("isLogin", value);
		editor.commit();
	}

	/**
	 * Getter method for the is login value flag.
	 * 
	 * @return User name logged in.
	 */
	public boolean getIsLogin() {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("isLogin", false);
	}

	/**
	 * Setter method for the is trip done value flag.
	 * 
	 * @param value
	 *            - Represent pass word flag value to set.
	 */

	public void set_isTripDone(boolean value) {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("isTripDone", value);
		editor.commit();
	}

	/**
	 * Getter method for the is trip done value flag.
	 * 
	 * @return User name logged in.
	 */
	public boolean get_isTripDone() {
		sharedPreferences = context.getSharedPreferences("Login",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("isTripDone", false);
	}

	public void set_ProfileID(String pid) {
		sharedPreferences = context.getSharedPreferences("Profile",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("profile_id", pid);
		editor.commit();

	}

	public String get_ProfileID() {
		return context.getSharedPreferences("Profile", context.MODE_PRIVATE)
				.getString("profile_id", null);
	}

	public void set_ManagerID(String managerid) {

		sharedPreferences = context.getSharedPreferences("Profile",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("manager_id", managerid);
		editor.commit();

	}

	public String get_ManagerID() {
		return context.getSharedPreferences("Profile", context.MODE_PRIVATE)
				.getString("manager_id", null);

	}

	public void set_LicenseStartDate(String startdate) {

		sharedPreferences = context.getSharedPreferences("Profile",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("start_date", startdate);
		editor.commit();

	}

	public String get_LicenseStartDate() {
		return context.getSharedPreferences("Profile", context.MODE_PRIVATE)
				.getString("start_date", null);

	}

	public void set_LicenseSubscription(String subscription) {

		sharedPreferences = context.getSharedPreferences("Profile",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("subscription", subscription);
		editor.commit();

	}

	public String get_LicenseSubscription() {
		return context.getSharedPreferences("Profile", context.MODE_PRIVATE)
				.getString("subscription", null);
	}

	public void set_AccountID(String account) {
		sharedPreferences = context.getSharedPreferences("Profile",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("accountid", account);
		editor.commit();
	}
	
	public String get_AccountID() {
		return context.getSharedPreferences("Profile", context.MODE_PRIVATE)
				.getString("accountid", null);
	}

	public void setEmergencyTripSave(boolean b) {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("EMERGENCYSAVETRIP", b);
		editor.commit();
	}

	public boolean getEmergencyTRIPSAVE() {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("EMERGENCYSAVETRIP", false);
	}

	public void isShutDown(boolean status) {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("ShutDown", status);
		editor.commit();
		
	}
	
	public boolean isShutDown() {
		sharedPreferences = context.getSharedPreferences("TRIP",
				context.MODE_PRIVATE);
		return sharedPreferences.getBoolean("ShutDown", false);
	}
	
}
