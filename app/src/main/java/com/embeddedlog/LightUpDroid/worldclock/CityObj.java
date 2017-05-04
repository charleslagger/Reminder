package com.embeddedlog.LightUpDroid.worldclock;

import android.content.SharedPreferences;

public class CityObj {

    private static final String CITY_NAME = "city_name_";
    private static final String CITY_TIME_ZONE = "city_tz_";
    private static final String CITY_ID = "city_id_";

    public String mCityName;
    public String mTimeZone;
    public String mCityId;
    public boolean isHeader;

    public CityObj(String name, String timezone, String id) {
        mCityName = name;
        mTimeZone = timezone;
        mCityId = id;
    }

    @Override
    public String toString() {
        return "CityObj{" +
                "name=" + mCityName +
                ", timezone=" + mTimeZone +
                ", id=" + mCityId +
                '}';
    }

    public CityObj(SharedPreferences prefs, int index) {
        mCityName = prefs.getString(CITY_NAME + index, null);
        mTimeZone = prefs.getString(CITY_TIME_ZONE + index, null);
        mCityId = prefs.getString(CITY_ID + index, null);
    }

    public void saveCityToSharedPrefs(SharedPreferences.Editor editor, int index) {
        editor.putString (CITY_NAME + index, mCityName);
        editor.putString (CITY_TIME_ZONE + index, mTimeZone);
        editor.putString (CITY_ID + index, mCityId);
    }
}
