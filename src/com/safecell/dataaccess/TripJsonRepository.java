package com.safecell.dataaccess;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;

public class TripJsonRepository extends DBAdapter {

	public TripJsonRepository(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public static String CREATE_TABLE_QUERY = "CREATE TABLE TripJson (id INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL,JsonData TEXT  NOT NULL,createat DATETIME  NOT NULL)";

	public void saveJSON(JSONObject tripJsonObject) {

		String insertQuery = "INSERT INTO TripJson (" + "JsonData, createat"
				+ ") VALUES " + "(?, ?)";
		long dateTimeStampMills = System.currentTimeMillis();
		Object[] args = { dateTimeStampMills, tripJsonObject.toString() };
		this.Query(insertQuery, args);

	}

	public int getNumberOfTripJsons() {
		String query = "select count(*) from TripJson";
		String[] args = {};
		Cursor cursor = this.selectQuery(query, args);
		this.selectQuery(query, args).close();
		int numberofJsons = 0;
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			numberofJsons = cursor.getInt(0);
		}
		cursor.close();
		return numberofJsons;
	}

	public ArrayList<JSONObject> getAllTripJsons() throws JSONException {
		String query = "select * from TripJson";
		String[] args = {};
		Cursor cursor = this.selectQuery(query, args);
		this.selectQuery(query, args).close();
		ArrayList<JSONObject> allJsons = new ArrayList<JSONObject>();
		if (cursor != null && cursor.getCount() > 0) {

			cursor.moveToFirst();
			for (int i = 0; i < cursor.getCount(); i++) {

				String json = cursor.getString(2);
				allJsons.add(new JSONObject(json));
				cursor.moveToNext();
			}
		}
		cursor.close();

		return allJsons;
	}

	public void deleteJson(int id) {
		this.deleteQuery("Delete from TripJson where id = " + id);
	}

	public HashMap<Integer, JSONObject> getAllTripJsonsHashMap()
			throws JSONException {
		String query = "select * from TripJson";
		String[] args = {};
		Cursor cursor = this.selectQuery(query, args);
		this.selectQuery(query, args).close();
		HashMap<Integer, JSONObject> hash_map = new HashMap<Integer, JSONObject>();
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			String json = cursor.getString(2);

			hash_map.put(cursor.getInt(0), new JSONObject(json));

		}
		cursor.close();

		return hash_map;
	}
	
	public Cursor getCursor() {
		String query = "select * from TripJson";
		String[] args = {};
		Cursor cursor = this.selectQuery(query, args);
		
		return cursor;
	}

	
}
