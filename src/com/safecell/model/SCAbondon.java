package com.safecell.model;

public class SCAbondon {

	private String phonenumber;

	private String abodon_pin;

	private String username;

	private String profile_id;

	private String controller_number;

	private String request_time;

	private String response_time;

	private String manager_id;

	
	
	public String getPhonenumber() {
		return phonenumber;
	}

	public void setPhonenumber(String phonenumber) {
		this.phonenumber = phonenumber;
	}

	public String getAbodon_pin() {
		return abodon_pin;
	}

	public void setAbodon_pin(String abodon_pin) {
		this.abodon_pin = abodon_pin;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getProfile_id() {
		return profile_id;
	}

	public void setProfile_id(String profile_id) {
		this.profile_id = profile_id;
	}

	public String getController_number() {
		return controller_number;
	}

	public void setController_number(String controller_number) {
		this.controller_number = controller_number;
	}

	public String getRequest_time() {
		return request_time;
	}

	public void setRequest_time(String request_time) {
		this.request_time = request_time;
	}

	public String getResponse_time() {
		return response_time;
	}

	public void setResponse_time(String response_time) {
		this.response_time = response_time;
	}

	public void setManager_id(String manager_id) {
		this.manager_id = manager_id;
		
	}

	public String getManager_id()
	{
		return this.manager_id;
	}

}
