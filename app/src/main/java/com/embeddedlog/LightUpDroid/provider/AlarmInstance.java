package com.embeddedlog.LightUpDroid.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.embeddedlog.LightUpDroid.Log;
import com.embeddedlog.LightUpDroid.R;
import com.embeddedlog.LightUpDroid.SettingsActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public final class AlarmInstance implements ClockContract.InstancesColumns {

    public static final int LOW_NOTIFICATION_HOUR_OFFSET = -2;


    public static final int HIGH_NOTIFICATION_MINUTE_OFFSET = -30;


    private static final int MISSED_TIME_TO_LIVE_HOUR_OFFSET = 12;


    private static final String DEFAULT_ALARM_TIMEOUT_SETTING = "10";


    public static final long INVALID_ID = -1;
    public static final long INVALID_TIMESTAMP = -1;

    private static final String[] QUERY_COLUMNS = {
            _ID,
            YEAR,
            MONTH,
            DAY,
            HOUR,
            MINUTES,
            LABEL,
            VIBRATE,
            RINGTONE,
            ALARM_ID,
            ALARM_STATE,
            LIGHTUPPI_ID,
            TIMESTAMP
    };

    private static final int ID_INDEX = 0;
    private static final int YEAR_INDEX = 1;
    private static final int MONTH_INDEX = 2;
    private static final int DAY_INDEX = 3;
    private static final int HOUR_INDEX = 4;
    private static final int MINUTES_INDEX = 5;
    private static final int LABEL_INDEX = 6;
    private static final int VIBRATE_INDEX = 7;
    private static final int RINGTONE_INDEX = 8;
    private static final int ALARM_ID_INDEX = 9;
    private static final int ALARM_STATE_INDEX = 10;
    private static final int LIGHTUPPI_ID_INDEX = 11;
    private static final int TIMESTAMP_INDEX = 12;

    private static final int COLUMN_COUNT = TIMESTAMP_INDEX + 1;
    private Calendar mTimeout;

    public static ContentValues createContentValues(AlarmInstance instance) {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        if (instance.mId != INVALID_ID) {
            values.put(_ID, instance.mId);
        }

        values.put(YEAR, instance.mYear);
        values.put(MONTH, instance.mMonth);
        values.put(DAY, instance.mDay);
        values.put(HOUR, instance.mHour);
        values.put(MINUTES, instance.mMinute);
        values.put(LABEL, instance.mLabel);
        values.put(VIBRATE, instance.mVibrate ? 1 : 0);
        if (instance.mRingtone == null) {
            values.putNull(RINGTONE);
        } else {
            values.put(RINGTONE, instance.mRingtone.toString());
        }
        values.put(ALARM_ID, instance.mAlarmId);
        values.put(ALARM_STATE, instance.mAlarmState);
        values.put(LIGHTUPPI_ID, instance.mLightuppiId);
        values.put(TIMESTAMP, instance.mTimestamp);
        return values;
    }

    public static Intent createIntent(String action, long instanceId) {
        return new Intent(action).setData(getUri(instanceId));
    }

    public static Intent createIntent(Context context, Class<?> cls, long instanceId) {
        return new Intent(context, cls).setData(getUri(instanceId));
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    public static Uri getUri(long instanceId) {
        return ContentUris.withAppendedId(CONTENT_URI, instanceId);
    }

    public static AlarmInstance getInstance(ContentResolver contentResolver, long instanceId) {
        Cursor cursor = contentResolver.query(getUri(instanceId), QUERY_COLUMNS, null, null, null);
        AlarmInstance result = null;
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                result = new AlarmInstance(cursor);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static List<AlarmInstance> getInstancesByAlarmId(ContentResolver contentResolver,
            long alarmId) {
        return getInstances(contentResolver, ALARM_ID + "=" + alarmId);
    }

    public static List<AlarmInstance> getInstancesByLightuppiId(ContentResolver contentResolver,
                                                            long lightuppiId) {
        return getInstances(contentResolver, LIGHTUPPI_ID + "=" + lightuppiId);
    }


    public static List<AlarmInstance> getInstances(ContentResolver contentResolver,
            String selection, String ... selectionArgs) {
        Cursor cursor  = contentResolver.query(CONTENT_URI, QUERY_COLUMNS,
                selection, selectionArgs, null);
        List<AlarmInstance> result = new LinkedList<AlarmInstance>();
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    result.add(new AlarmInstance(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static AlarmInstance addInstance(ContentResolver contentResolver,
            AlarmInstance instance) {
        String dupSelector = AlarmInstance.ALARM_ID + " = " + instance.mAlarmId;
        for (AlarmInstance otherInstances : getInstances(contentResolver, dupSelector)) {
            if (otherInstances.getAlarmTime().equals(instance.getAlarmTime())) {

                instance.mId = otherInstances.mId;
                updateInstance(contentResolver, instance);
                return instance;
            }
        }

        ContentValues values = createContentValues(instance);
        Uri uri = contentResolver.insert(CONTENT_URI, values);
        instance.mId = getId(uri);
        return instance;
    }

    public static boolean updateInstance(ContentResolver contentResolver, AlarmInstance instance) {
        if (instance.mId == INVALID_ID) return false;
        ContentValues values = createContentValues(instance);
        long rowsUpdated = contentResolver.update(getUri(instance.mId), values, null, null);
        return rowsUpdated == 1;
    }

    public static boolean deleteInstance(ContentResolver contentResolver, long instanceId) {
        if (instanceId == INVALID_ID) return false;
        int deletedRows = contentResolver.delete(getUri(instanceId), "", null);
        return deletedRows == 1;
    }

    // Public fields
    public long mId;
    public int mYear;
    public int mMonth;
    public int mDay;
    public int mHour;
    public int mMinute;
    public String mLabel;
    public boolean mVibrate;
    public Uri mRingtone;
    public Long mAlarmId;
    public int mAlarmState;
    public Long mLightuppiId;
    public Long mTimestamp;

    public AlarmInstance(Calendar calendar, Long alarmId) {
        this(calendar);
        mAlarmId = alarmId;
    }

    public AlarmInstance(Calendar calendar) {
        mId = INVALID_ID;
        setAlarmTime(calendar);
        mLabel = "";
        mVibrate = false;
        mRingtone = null;
        mAlarmState = SILENT_STATE;
        mLightuppiId = INVALID_ID;
        mTimestamp = INVALID_TIMESTAMP;
    }

    public AlarmInstance(Cursor c) {
        mId = c.getLong(ID_INDEX);
        mYear = c.getInt(YEAR_INDEX);
        mMonth = c.getInt(MONTH_INDEX);
        mDay = c.getInt(DAY_INDEX);
        mHour = c.getInt(HOUR_INDEX);
        mMinute = c.getInt(MINUTES_INDEX);
        mLabel = c.getString(LABEL_INDEX);
        mVibrate = c.getInt(VIBRATE_INDEX) == 1;
        if (c.isNull(RINGTONE_INDEX)) {
            mRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            mRingtone = Uri.parse(c.getString(RINGTONE_INDEX));
        }

        if (!c.isNull(ALARM_ID_INDEX)) {
            mAlarmId = c.getLong(ALARM_ID_INDEX);
        }
        mAlarmState = c.getInt(ALARM_STATE_INDEX);

        if (!c.isNull(LIGHTUPPI_ID_INDEX)) {
            mLightuppiId = c.getLong(LIGHTUPPI_ID_INDEX);
        }
        if (!c.isNull(TIMESTAMP_INDEX)) {
            mTimestamp = c.getLong(TIMESTAMP_INDEX);
        }
    }

    public String getLabelOrDefault(Context context) {
        return mLabel.isEmpty() ? context.getString(R.string.default_label) : mLabel;
    }

    public void setAlarmTime(Calendar calendar) {
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
    }
    public Calendar getAlarmTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, mYear);
        calendar.set(Calendar.MONTH, mMonth);
        calendar.set(Calendar.DAY_OF_MONTH, mDay);
        calendar.set(Calendar.HOUR_OF_DAY, mHour);
        calendar.set(Calendar.MINUTE, mMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public Calendar getLowNotificationTime() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.HOUR_OF_DAY, LOW_NOTIFICATION_HOUR_OFFSET);
        return calendar;
    }

    public Calendar getHighNotificationTime() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.MINUTE, HIGH_NOTIFICATION_MINUTE_OFFSET);
        return calendar;
    }

    public Calendar getMissedTimeToLive() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.HOUR, MISSED_TIME_TO_LIVE_HOUR_OFFSET);
        return calendar;
    }


    public Calendar getTimeout(Context context) {
        String timeoutSetting = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.KEY_AUTO_SILENCE, DEFAULT_ALARM_TIMEOUT_SETTING);
        int timeoutMinutes = Integer.parseInt(timeoutSetting);


        if (timeoutMinutes < 0) {
            return null;
        }

        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.MINUTE, timeoutMinutes);
        return calendar;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AlarmInstance)) return false;
        final AlarmInstance other = (AlarmInstance) o;
        return mId == other.mId;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(mId).hashCode();
    }

    @Override
    public String toString() {
        Date timestampDate = new Date(mTimestamp * 1000);
        return "AlarmInstance{" +
                "mId=" + mId +
                ", mYear=" + mYear +
                ", mMonth=" + mMonth +
                ", mDay=" + mDay +
                ", mHour=" + mHour +
                ", mMinute=" + mMinute +
                ", mLabel=" + mLabel +
                ", mVibrate=" + mVibrate +
                ", mRingtone=" + mRingtone +
                ", mAlarmId=" + mAlarmId +
                ", mLightuppiId=" + mLightuppiId +
                ", mAlarmState=" + mAlarmState +
                ", mTimestamp=" + timestampDate.toString() +
                '}';
    }
}
