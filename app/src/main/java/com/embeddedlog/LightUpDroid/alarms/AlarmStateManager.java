package com.embeddedlog.LightUpDroid.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.embeddedlog.LightUpDroid.AlarmAlertWakeLock;
import com.embeddedlog.LightUpDroid.AlarmClockFragment;
import com.embeddedlog.LightUpDroid.AlarmUtils;
import com.embeddedlog.LightUpDroid.AsyncHandler;
import com.embeddedlog.LightUpDroid.DeskClock;
import com.embeddedlog.LightUpDroid.Log;
import com.embeddedlog.LightUpDroid.R;
import com.embeddedlog.LightUpDroid.SettingsActivity;
import com.embeddedlog.LightUpDroid.Utils;
import com.embeddedlog.LightUpDroid.provider.Alarm;
import com.embeddedlog.LightUpDroid.provider.AlarmInstance;

import java.util.Calendar;
import java.util.List;


public final class AlarmStateManager extends BroadcastReceiver {

    private static final String DEFAULT_SNOOZE_MINUTES = "10";

    public static final String CHANGE_STATE_ACTION = "change_state";

    public static final String SHOW_AND_DISMISS_ALARM_ACTION = "show_and_dismiss_alarm";

    private static final String INDICATOR_ACTION = "indicator";

    public static final String ALARM_STATE_EXTRA = "intent.extra.alarm.state";

    private static final String ALARM_GLOBAL_ID_EXTRA = "intent.extra.alarm.global.id";

    public static final String ALARM_MANAGER_TAG = "ALARM_MANAGER";

    public static final int ALARM_FIRE_BUFFER = 15;

    public static int getGlobalIntentId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(ALARM_GLOBAL_ID_EXTRA, -1);
    }

    public static void updateGloablIntentId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int globalId = prefs.getInt(ALARM_GLOBAL_ID_EXTRA, -1) + 1;
        prefs.edit().putInt(ALARM_GLOBAL_ID_EXTRA, globalId).commit();
    }

    public static void updateNextAlarm(Context context) {
        AlarmInstance nextAlarm = null;
        ContentResolver cr = context.getContentResolver();
        String activeAlarmQuery = AlarmInstance.ALARM_STATE + "<" + AlarmInstance.FIRED_STATE;
        for (AlarmInstance instance : AlarmInstance.getInstances(cr, activeAlarmQuery)) {
            if (nextAlarm == null || instance.getAlarmTime().before(nextAlarm.getAlarmTime())) {
                nextAlarm = instance;
            }
        }
        AlarmNotifications.broadcastNextAlarm(context, nextAlarm);
    }

    private static void updateParentAlarm(Context context, AlarmInstance instance) {
        ContentResolver cr = context.getContentResolver();
        Alarm alarm = Alarm.getAlarm(cr, instance.mAlarmId);
        if (alarm == null) {
            return;
        }

        if (!alarm.daysOfWeek.isRepeating()) {
            if (alarm.deleteAfterUse) {
                Alarm.deleteAlarm(cr, alarm.id);
            } else {
                alarm.enabled = false;
                Alarm.updateAlarm(cr, alarm);
            }
        } else {
            Calendar currentTime = Calendar.getInstance();
            Calendar alarmTime = instance.getAlarmTime();
            if (currentTime.after(alarmTime)) {
                alarmTime = currentTime;
            }
            AlarmInstance nextRepeatedInstance = alarm.createInstanceAfter(alarmTime);
            AlarmInstance.addInstance(cr, nextRepeatedInstance);
            registerInstance(context, nextRepeatedInstance, true);
        }
    }

    public static Intent createStateChangeIntent(Context context, String tag,
            AlarmInstance instance, Integer state) {
        Intent intent = AlarmInstance.createIntent(context, AlarmStateManager.class, instance.mId);
        intent.setAction(CHANGE_STATE_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.addCategory(tag);
        intent.putExtra(ALARM_GLOBAL_ID_EXTRA, getGlobalIntentId(context));
        if (state != null) {
            intent.putExtra(ALARM_STATE_EXTRA, state.intValue());
        }
        return intent;
    }

    private static void scheduleInstanceStateChange(Context context, Calendar time,
            AlarmInstance instance, int newState) {
        long timeInMillis = time.getTimeInMillis();
        Intent stateChangeIntent = createStateChangeIntent(context, ALARM_MANAGER_TAG, instance,
                newState);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(),
                stateChangeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Utils.isKitKatOrLater()) {
            am.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }

    private static void cancelScheduledInstance(Context context, AlarmInstance instance) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(),
                createStateChangeIntent(context, ALARM_MANAGER_TAG, instance, null),
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }

    public static void setSilentState(Context context, AlarmInstance instance) {

        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.SILENT_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);

        AlarmNotifications.clearNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getLowNotificationTime(),
                instance, AlarmInstance.LOW_NOTIFICATION_STATE);
    }

    public static void setLowNotificationState(Context context, AlarmInstance instance) {

        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.LOW_NOTIFICATION_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);

        AlarmNotifications.showLowPriorityNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getHighNotificationTime(),
                instance, AlarmInstance.HIGH_NOTIFICATION_STATE);
    }

    public static void setHideNotificationState(Context context, AlarmInstance instance) {

        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.HIDE_NOTIFICATION_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);

        AlarmNotifications.clearNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getHighNotificationTime(),
                instance, AlarmInstance.HIGH_NOTIFICATION_STATE);
    }

    public static void setHighNotificationState(Context context, AlarmInstance instance) {

        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.HIGH_NOTIFICATION_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);

        AlarmNotifications.showHighPriorityNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getAlarmTime(),
                instance, AlarmInstance.FIRED_STATE);
    }

    public static void setFiredState(Context context, AlarmInstance instance) {

        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.FIRED_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);


        AlarmService.startAlarm(context, instance);

        Calendar timeout = instance.getTimeout(context);
        if (timeout != null) {
            scheduleInstanceStateChange(context, timeout, instance, AlarmInstance.MISSED_STATE);
        }

        updateNextAlarm(context);
    }

    public static void setSnoozeState(Context context, AlarmInstance instance) {
        AlarmService.stopAlarm(context, instance);

        String snoozeMinutesStr = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.KEY_ALARM_SNOOZE, DEFAULT_SNOOZE_MINUTES);
        int snoozeMinutes = Integer.parseInt(snoozeMinutesStr);
        Calendar newAlarmTime = Calendar.getInstance();
        newAlarmTime.add(Calendar.MINUTE, snoozeMinutes);

        instance.setAlarmTime(newAlarmTime);
        instance.mAlarmState = AlarmInstance.SNOOZE_STATE;
        AlarmInstance.updateInstance(context.getContentResolver(), instance);

        AlarmNotifications.showSnoozeNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getAlarmTime(),
                instance, AlarmInstance.FIRED_STATE);

        String displayTime = context.getString(R.string.alarm_alert_snooze_set, snoozeMinutes);
        Toast.makeText(context, displayTime, Toast.LENGTH_LONG).show();
        updateNextAlarm(context);

    }

    public static void setMissedState(Context context, AlarmInstance instance) {

        AlarmService.stopAlarm(context, instance);


        if (instance.mAlarmId != null) {
            updateParentAlarm(context, instance);
        }


        ContentResolver contentResolver = context.getContentResolver();
        instance.mAlarmState = AlarmInstance.MISSED_STATE;
        AlarmInstance.updateInstance(contentResolver, instance);


        AlarmNotifications.showMissedNotification(context, instance);
        scheduleInstanceStateChange(context, instance.getMissedTimeToLive(),
                instance, AlarmInstance.DISMISSED_STATE);


        updateNextAlarm(context);

    }

    public static void setDismissState(Context context, AlarmInstance instance) {

        unregisterInstance(context, instance);

        if (instance.mAlarmId != null) {
            updateParentAlarm(context, instance);
        }

        AlarmInstance.deleteInstance(context.getContentResolver(), instance.mId);

        updateNextAlarm(context);
    }
    public static void unregisterInstance(Context context, AlarmInstance instance) {

        AlarmService.stopAlarm(context, instance);
        AlarmNotifications.clearNotification(context, instance);
        cancelScheduledInstance(context, instance);
    }

    public static void registerInstance(Context context, AlarmInstance instance,
            boolean updateNextAlarm) {
        Calendar currentTime = Calendar.getInstance();
        Calendar alarmTime = instance.getAlarmTime();
        Calendar timeoutTime = instance.getTimeout(context);
        Calendar lowNotificationTime = instance.getLowNotificationTime();
        Calendar highNotificationTime = instance.getHighNotificationTime();
        Calendar missedTTL = instance.getMissedTimeToLive();

        if (instance.mAlarmState == AlarmInstance.DISMISSED_STATE) {

            setDismissState(context, instance);
            return;
        } else if (instance.mAlarmState == AlarmInstance.FIRED_STATE) {
            boolean hasTimeout = timeoutTime != null && currentTime.after(timeoutTime);
            if (!hasTimeout) {
                setFiredState(context, instance);
                return;
            }
        } else if (instance.mAlarmState == AlarmInstance.MISSED_STATE) {
            if (currentTime.before(alarmTime)) {
                if (instance.mAlarmId == null) {

                    setDismissState(context, instance);
                    return;
                }

                ContentResolver cr = context.getContentResolver();
                Alarm alarm = Alarm.getAlarm(cr, instance.mAlarmId);
                alarm.enabled = true;
                Alarm.updateAlarm(cr, alarm);
            }
        }

        if (currentTime.after(missedTTL)) {
            setDismissState(context, instance);
        } else if (currentTime.after(alarmTime)) {
            Calendar alarmBuffer = Calendar.getInstance();
            alarmBuffer.setTime(alarmTime.getTime());
            alarmBuffer.add(Calendar.SECOND, ALARM_FIRE_BUFFER);
            if (currentTime.before(alarmBuffer)) {
                setFiredState(context, instance);
            } else {
                setMissedState(context, instance);
            }
        } else if (instance.mAlarmState == AlarmInstance.SNOOZE_STATE) {
            AlarmNotifications.showSnoozeNotification(context, instance);
            scheduleInstanceStateChange(context, instance.getAlarmTime(),
                    instance, AlarmInstance.FIRED_STATE);
        } else if (currentTime.after(highNotificationTime)) {
            setHighNotificationState(context, instance);
        } else if (currentTime.after(lowNotificationTime)) {
            if (instance.mAlarmState == AlarmInstance.HIDE_NOTIFICATION_STATE) {
                setHideNotificationState(context, instance);
            } else {
                setLowNotificationState(context, instance);
            }
        } else {
          setSilentState(context, instance);
        }
        if (updateNextAlarm) {
            updateNextAlarm(context);
        }
    }

    public static void deleteAllInstances(Context context, long alarmId) {
        ContentResolver cr = context.getContentResolver();
        List<AlarmInstance> instances = AlarmInstance.getInstancesByAlarmId(cr, alarmId);
        for (AlarmInstance instance : instances) {
            unregisterInstance(context, instance);
            AlarmInstance.deleteInstance(context.getContentResolver(), instance.mId);
        }
        updateNextAlarm(context);
    }

    public static void fixAlarmInstances(Context context) {
        // Register all instances after major time changes or when phone restarts
        // TODO: Refactor this code to not use the overloaded registerInstance method.
        ContentResolver contentResolver = context.getContentResolver();
        for (AlarmInstance instance : AlarmInstance.getInstances(contentResolver, null)) {
           AlarmStateManager.registerInstance(context, instance, false);
        }
        AlarmStateManager.updateNextAlarm(context);
    }

    public void setAlarmState(Context context, AlarmInstance instance, int state) {
        switch(state) {
            case AlarmInstance.SILENT_STATE:
                setSilentState(context, instance);
                break;
            case AlarmInstance.LOW_NOTIFICATION_STATE:
                setLowNotificationState(context, instance);
                break;
            case AlarmInstance.HIDE_NOTIFICATION_STATE:
                setHideNotificationState(context, instance);
                break;
            case AlarmInstance.HIGH_NOTIFICATION_STATE:
                setHighNotificationState(context, instance);
                break;
            case AlarmInstance.FIRED_STATE:
                setFiredState(context, instance);
                break;
            case AlarmInstance.SNOOZE_STATE:
                setSnoozeState(context, instance);
                break;
            case AlarmInstance.MISSED_STATE:
                setMissedState(context, instance);
                break;
            case AlarmInstance.DISMISSED_STATE:
                setDismissState(context, instance);
                break;
            default:
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (INDICATOR_ACTION.equals(intent.getAction())) {
            return;
        }

        final PendingResult result = goAsync();
        final PowerManager.WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();
        AsyncHandler.post(new Runnable() {
            @Override
            public void run() {
                handleIntent(context, intent);
                result.finish();
                wl.release();
            }
        });
    }

    private void handleIntent(Context context, Intent intent) {
        final String action = intent.getAction();
        if (CHANGE_STATE_ACTION.equals(action)) {
            Uri uri = intent.getData();
            AlarmInstance instance = AlarmInstance.getInstance(context.getContentResolver(),
                    AlarmInstance.getId(uri));
            if (instance == null) {

                return;
            }

            int globalId = getGlobalIntentId(context);
            int intentId = intent.getIntExtra(ALARM_GLOBAL_ID_EXTRA, -1);
            int alarmState = intent.getIntExtra(ALARM_STATE_EXTRA, -1);
            if (intentId != globalId) {
                Log.i(" Intent. IntentId: " + intentId + " GlobalId: " + globalId +
                      " AlarmState: " + alarmState);
                return;
            }

            if (alarmState >= 0) {
                setAlarmState(context, instance, alarmState);
            } else {
                registerInstance(context, instance, true);
            }
        } else if (SHOW_AND_DISMISS_ALARM_ACTION.equals(action)) {
            Uri uri = intent.getData();
            AlarmInstance instance = AlarmInstance.getInstance(context.getContentResolver(),
                    AlarmInstance.getId(uri));

            long alarmId = instance.mAlarmId == null ? Alarm.INVALID_ID : instance.mAlarmId;
            Intent viewAlarmIntent = Alarm.createIntent(context, DeskClock.class, alarmId);
            viewAlarmIntent.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
            viewAlarmIntent.putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, alarmId);
            viewAlarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(viewAlarmIntent);
            setDismissState(context, instance);
        }
    }


    public static Intent createIndicatorIntent(Context context) {
        return new Intent(context, AlarmStateManager.class).setAction(INDICATOR_ACTION);
    }
}
