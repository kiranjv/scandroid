/*******************************************************************************
 * Configuration.java.java, Created: Apr 19, 2012
 *
 * Part of Muni Project
 *
 * Copyright (c) 2012 : NDS Limited
 *
 * P R O P R I E T A R Y &amp; C O N F I D E N T I A L
 *
 * The copyright of this code and related documentation together with any
 * other associated intellectual property rights are vested in NDS Limited
 * and may not be used except in accordance with the terms of the licence
 * that you have entered into with NDS Limited. Use of this material without
 * an express licence from NDS Limited shall be an infringement of copyright
 * and any other intellectual property rights that may be incorporated with
 * this material.
 *
 * ******************************************************************************
 * ******     Please Check GIT for revision/modification history    *******
 * ******************************************************************************
 */

package com.safecell.model;

import java.util.Calendar;

/**
 * @author uttama
 */
public class Configuration {

	private int tripStartSpeed = 5;

	private int tripStopTime = 2;

	private boolean disableEmail = true;

	private boolean disableCall = true;

	private boolean disableTexting = true;

	private boolean disableWeb = true;

	private boolean logWayPoints = true;

	private String controller_number = "+918971855771";

	private boolean splashShow = true;

	private boolean keypadlock = true;

	/* temporary configuration.. */

	/* Represent re-check time interval for trip start time config. */
	private int recheckInterval = 2;

	private String dayOfWeek = "Sunday";

	private boolean isActive = true;

	private String startTime = "16:40";

	private String endTime = "17:00";

	public int getRecheckInterval() {
		return recheckInterval;
	}

	public void setRecheckInterval(int recheckInterval) {
		this.recheckInterval = recheckInterval;
	}

	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public boolean getKeypadlock() {

		return keypadlock;
	}

	public void setKeypadlock(boolean keypadlock) {
		this.keypadlock = keypadlock;
	}

	public boolean getSplashShow() {
		return splashShow;
	}

	public void setSplashShow(boolean splashShow) {
		this.splashShow = splashShow;
	}

	public String getController_number() {
		return controller_number;
	}

	public void setController_number(String controller_number) {
		this.controller_number = controller_number;
	}

	public int getTripStartSpeed() {
		return tripStartSpeed;
	}

	public void setTripStartSpeed(int tripStartSpeed) {
		this.tripStartSpeed = tripStartSpeed;
	}

	public int getTripStopTime() {
		return tripStopTime;
	}

	public void setTripStopTime(int tripStopTime) {
		this.tripStopTime = tripStopTime;
	}

	public boolean isDisableEmail() {
		return disableEmail;
	}

	public void setDisableEmail(boolean disableEmail) {
		this.disableEmail = disableEmail;
	}

	public boolean isDisableCall() {
		return disableCall;
	}

	public void setDisableCall(boolean disableCall) {
		this.disableCall = disableCall;
	}

	public boolean isDisableTexting() {
		return disableTexting;
	}

	public void setDisableTexting(boolean disableTexting) {
		this.disableTexting = disableTexting;
	}

	public boolean isDisableWeb() {
		return disableWeb;
	}

	public void setDisableWeb(boolean disableWeb) {
		this.disableWeb = disableWeb;
	}

	public boolean isLogWayPoints() {
		return logWayPoints;
	}

	public void setLogWayPoints(boolean logWayPoints) {
		this.logWayPoints = logWayPoints;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TSN = " + tripStartSpeed + " TST = " + tripStopTime
				+ " DEMAIL " + disableEmail + " DCALL = " + disableCall
				+ " DTTEXTING = " + disableTexting + " DWEB = " + disableWeb
				+ " LOGWEBPOINTS = " + logWayPoints;
	}
}
