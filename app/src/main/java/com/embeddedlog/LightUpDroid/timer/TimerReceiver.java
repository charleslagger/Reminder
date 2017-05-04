package com.embeddedlog.LightUpDroid.timer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.embeddedlog.LightUpDroid.DeskClock;
import com.embeddedlog.LightUpDroid.R;
import com.embeddedlog.LightUpDroid.TimerRingService;
import com.embeddedlog.LightUpDroid.Utils;

import java.util.ArrayList;
import java.util.Iterator;

public class TimerReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerReceiver";

    private static final int IN_USE_NOTIFICATION_ID = Integer.MAX_VALUE - 2;

    ArrayList<TimerObj> mTimers;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Timers.LOGGING) {
            Log.v(TAG, "Received intent " + intent.toString());
        }
        String actionType = intent.getAction();
        if (Timers.NOTIF_IN_USE_CANCEL.equals(actionType)) {
            cancelInUseNotification(context);
            return;
        }

        if (mTimers == null) {
            mTimers = new ArrayList<TimerObj> ();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        TimerObj.getTimersFromSharedPrefs(prefs, mTimers);

        if (Timers.NOTIF_IN_USE_SHOW.equals(actionType)) {
            showInUseNotification(context);
            return;
        } else if (Timers.NOTIF_TIMES_UP_SHOW.equals(actionType)) {
            showTimesUpNotification(context);
            return;
        } else if (Timers.NOTIF_TIMES_UP_CANCEL.equals(actionType)) {
            cancelTimesUpNotification(context);
            return;
        }

        if (!intent.hasExtra(Timers.TIMER_INTENT_EXTRA)) {

            return;
        }

        int timerId = intent.getIntExtra(Timers.TIMER_INTENT_EXTRA, -1);
        if (timerId == -1) {
        }

        TimerObj t = Timers.findTimer(mTimers, timerId);

        if (Timers.TIMES_UP.equals(actionType)) {
            if (t == null) {
                return;
            }

            t.mState = TimerObj.STATE_TIMESUP;
            t.writeToSharedPref(prefs);
            Intent si = new Intent();
            si.setClass(context, TimerRingService.class);
            context.startService(si);

            if (getNextRunningTimer(mTimers, false, Utils.getTimeNow()) == null) {
                cancelInUseNotification(context);
            } else {
                showInUseNotification(context);
            }

            Intent timersAlert = new Intent(context, TimerAlertFullScreen.class);
            timersAlert.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            context.startActivity(timersAlert);
        } else if (Timers.TIMER_RESET.equals(actionType)
                || Timers.DELETE_TIMER.equals(actionType)
                || Timers.TIMER_DONE.equals(actionType)) {
            stopRingtoneIfNoTimesup(context);
        } else if (Timers.NOTIF_TIMES_UP_STOP.equals(actionType)) {
            if (t == null) {
                return;
            } else if (t.mState != TimerObj.STATE_TIMESUP) {
                return;
            }

            t.mState = t.getDeleteAfterUse() ? TimerObj.STATE_DELETED : TimerObj.STATE_DONE;
            t.mTimeLeft = t.mOriginalLength - (Utils.getTimeNow() - t.mStartTime);
            t.writeToSharedPref(prefs);

            prefs.edit().putBoolean(Timers.FROM_NOTIFICATION, true).apply();

            cancelTimesUpNotification(context, t);

            if (t.getDeleteAfterUse()) {
                t.deleteFromSharedPref(prefs);
            }

            stopRingtoneIfNoTimesup(context);
        } else if (Timers.NOTIF_TIMES_UP_PLUS_ONE.equals(actionType)) {
            if (t == null) {
                return;
            } else if (t.mState != TimerObj.STATE_TIMESUP) {
                return;
            }

            t.mState = TimerObj.STATE_RUNNING;
            t.mStartTime = Utils.getTimeNow();
            t.mTimeLeft = t. mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
            t.writeToSharedPref(prefs);

            prefs.edit().putBoolean(Timers.FROM_NOTIFICATION, true).apply();

            cancelTimesUpNotification(context, t);

            if (!prefs.getBoolean(Timers.NOTIF_APP_OPEN, false)) {
                showInUseNotification(context);
            }

            stopRingtoneIfNoTimesup(context);
        } else if (Timers.TIMER_UPDATE.equals(actionType)) {
            if (t.mState == TimerObj.STATE_TIMESUP) {
                // Must cancel the previous notification to get all updates displayed correctly
                cancelTimesUpNotification(context, t);
                showTimesUpNotification(context, t);
            }
        }
        // Update the next "Times up" alarm
        updateNextTimesup(context);
    }

    private void stopRingtoneIfNoTimesup(final Context context) {
        if (Timers.findExpiredTimer(mTimers) == null) {
            // Stop ringtone
            Log.d(TAG, "stopping ringtone");
            Intent si = new Intent();
            si.setClass(context, TimerRingService.class);
            context.stopService(si);
        }
    }

    // Scan all timers and find the one that will expire next.
    // Tell AlarmManager to send a "Time's up" message to this receiver when this timer expires.
    // If no timer exists, clear "time's up" message.
    private void updateNextTimesup(Context context) {
        TimerObj t = getNextRunningTimer(mTimers, false, Utils.getTimeNow());
        long nextTimesup = (t == null) ? -1 : t.getTimesupTime();
        int timerId = (t == null) ? -1 : t.mTimerId;

        Intent intent = new Intent();
        intent.setAction(Timers.TIMES_UP);
        intent.setClass(context, TimerReceiver.class);
        if (!mTimers.isEmpty()) {
            intent.putExtra(Timers.TIMER_INTENT_EXTRA, timerId);
        }
        AlarmManager mngr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent p = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        if (t != null) {
            if (Utils.isKitKatOrLater()) {
                mngr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTimesup, p);
            } else {
                mngr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTimesup, p);
            }
            if (Timers.LOGGING) {
                Log.d(TAG, "Setting times up to " + nextTimesup);
            }
        } else {
            mngr.cancel(p);
            if (Timers.LOGGING) {
                Log.v(TAG, "no next times up");
            }
        }
    }

    private void showInUseNotification(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean appOpen = prefs.getBoolean(Timers.NOTIF_APP_OPEN, false);
        ArrayList<TimerObj> timersInUse = Timers.timersInUse(mTimers);
        int numTimersInUse = timersInUse.size();

        if (appOpen || numTimersInUse == 0) {
            return;
        }

        String title, contentText;
        Long nextBroadcastTime = null;
        long now = Utils.getTimeNow();
        if (timersInUse.size() == 1) {
            TimerObj timer = timersInUse.get(0);
            boolean timerIsTicking = timer.isTicking();
            String label = timer.getLabelOrDefault(context);
            title = timerIsTicking ? label : context.getString(R.string.timer_stopped);
            long timeLeft = timerIsTicking ? timer.getTimesupTime() - now : timer.mTimeLeft;
            contentText = buildTimeRemaining(context, timeLeft);
            if (timerIsTicking && timeLeft > TimerObj.MINUTE_IN_MILLIS) {
                nextBroadcastTime = getBroadcastTime(now, timeLeft);
            }
        } else {
            TimerObj timer = getNextRunningTimer(timersInUse, false, now);
            if (timer == null) {
                // No running timers.
                title = String.format(
                        context.getString(R.string.timers_stopped), numTimersInUse);
                contentText = context.getString(R.string.all_timers_stopped_notif);
            } else {
                // We have at least one timer running and other timers stopped.
                title = String.format(
                        context.getString(R.string.timers_in_use), numTimersInUse);
                long completionTime = timer.getTimesupTime();
                long timeLeft = completionTime - now;
                contentText = String.format(context.getString(R.string.next_timer_notif),
                        buildTimeRemaining(context, timeLeft));
                if (timeLeft <= TimerObj.MINUTE_IN_MILLIS) {
                    TimerObj timerWithUpdate = getNextRunningTimer(timersInUse, true, now);
                    if (timerWithUpdate != null) {
                        completionTime = timerWithUpdate.getTimesupTime();
                        timeLeft = completionTime - now;
                        nextBroadcastTime = getBroadcastTime(now, timeLeft);
                    }
                } else {
                    nextBroadcastTime = getBroadcastTime(now, timeLeft);
                }
            }
        }
        showCollapsedNotificationWithNext(context, title, contentText, nextBroadcastTime);
    }

    private long getBroadcastTime(long now, long timeUntilBroadcast) {
        long seconds = timeUntilBroadcast / 1000;
        seconds = seconds - ( (seconds / 60) * 60 );
        return now + (seconds * 1000);
    }

    private void showCollapsedNotificationWithNext(
            final Context context, String title, String text, Long nextBroadcastTime) {
        Intent activityIntent = new Intent(context, DeskClock.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX);
        PendingIntent pendingActivityIntent = PendingIntent.getActivity(context, 0, activityIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        showCollapsedNotification(context, title, text, Notification.PRIORITY_HIGH,
                pendingActivityIntent, IN_USE_NOTIFICATION_ID, false);

        if (nextBroadcastTime == null) {
            return;
        }
        Intent nextBroadcast = new Intent();
        nextBroadcast.setAction(Timers.NOTIF_IN_USE_SHOW);
        PendingIntent pendingNextBroadcast =
                PendingIntent.getBroadcast(context, 0, nextBroadcast, 0);
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Utils.isKitKatOrLater()) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME, nextBroadcastTime, pendingNextBroadcast);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, nextBroadcastTime, pendingNextBroadcast);
        }
    }

    private static void showCollapsedNotification(final Context context, String title, String text,
            int priority, PendingIntent pendingIntent, int notificationId, boolean showTicker) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
        .setAutoCancel(false)
        .setContentTitle(title)
        .setContentText(text)
        .setDeleteIntent(pendingIntent)
        .setOngoing(true)
        .setPriority(priority)
        .setSmallIcon(R.drawable.stat_notify_timer);
        if (showTicker) {
            builder.setTicker(text);
        }

        Notification notification = builder.build();
        notification.contentIntent = pendingIntent;
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }

    private String buildTimeRemaining(Context context, long timeLeft) {
        if (timeLeft < 0) {
            // We should never be here...
            Log.v(TAG, "Will not show notification for timer already expired.");
            return null;
        }

        long hundreds, seconds, minutes, hours;
        seconds = timeLeft / 1000;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 99) {
            hours = 0;
        }

        String hourSeq = (hours == 0) ? "" :
            ( (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours)) );
        String minSeq = (minutes == 0) ? "" :
            ( (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes)) );

        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;
        int index = (dispHour ? 1 : 0) | (dispMinute ? 2 : 0);
        String[] formats = context.getResources().getStringArray(R.array.timer_notifications);
        return String.format(formats[index], hourSeq, minSeq);
    }

    private TimerObj getNextRunningTimer(
            ArrayList<TimerObj> timers, boolean requireNextUpdate, long now) {
        long nextTimesup = Long.MAX_VALUE;
        boolean nextTimerFound = false;
        Iterator<TimerObj> i = timers.iterator();
        TimerObj t = null;
        while(i.hasNext()) {
            TimerObj tmp = i.next();
            if (tmp.mState == TimerObj.STATE_RUNNING) {
                long timesupTime = tmp.getTimesupTime();
                long timeLeft = timesupTime - now;
                if (timesupTime < nextTimesup && (!requireNextUpdate || timeLeft > 60) ) {
                    nextTimesup = timesupTime;
                    nextTimerFound = true;
                    t = tmp;
                }
            }
        }
        if (nextTimerFound) {
            return t;
        } else {
            return null;
        }
    }

    private void cancelInUseNotification(final Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(IN_USE_NOTIFICATION_ID);
    }

    private void showTimesUpNotification(final Context context) {
        for (TimerObj timerObj : Timers.timersInTimesUp(mTimers) ) {
            showTimesUpNotification(context, timerObj);
        }
    }

    private void showTimesUpNotification(final Context context, TimerObj timerObj) {
        // Content Intent. When clicked will show the timer full screen
        PendingIntent contentIntent = PendingIntent.getActivity(context, timerObj.mTimerId,
                new Intent(context, TimerAlertFullScreen.class).putExtra(
                        Timers.TIMER_INTENT_EXTRA, timerObj.mTimerId),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Add one minute action button
        PendingIntent addOneMinuteAction = PendingIntent.getBroadcast(context, timerObj.mTimerId,
                new Intent(Timers.NOTIF_TIMES_UP_PLUS_ONE)
                        .putExtra(Timers.TIMER_INTENT_EXTRA, timerObj.mTimerId),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Add stop/done action button
        PendingIntent stopAction = PendingIntent.getBroadcast(context, timerObj.mTimerId,
                new Intent(Timers.NOTIF_TIMES_UP_STOP)
                        .putExtra(Timers.TIMER_INTENT_EXTRA, timerObj.mTimerId),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Notification creation
        Notification notification = new Notification.Builder(context)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_menu_add,
                        context.getResources().getString(R.string.timer_plus_1_min),
                        addOneMinuteAction)
                .addAction(
                        timerObj.getDeleteAfterUse()
                                ? android.R.drawable.ic_menu_close_clear_cancel
                                : R.drawable.ic_stop_normal,
                        timerObj.getDeleteAfterUse()
                                ? context.getResources().getString(R.string.timer_done)
                                : context.getResources().getString(R.string.timer_stop),
                        stopAction)
                .setContentTitle(timerObj.getLabelOrDefault(context))
                .setContentText(context.getResources().getString(R.string.timer_times_up))
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setWhen(0)
                .build();

        // Send the notification using the timer's id to identify the
        // correct notification
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(
                timerObj.mTimerId, notification);
        if (Timers.LOGGING) {
            Log.v(TAG, "Setting times-up notification for "
                    + timerObj.getLabelOrDefault(context) + " #" + timerObj.mTimerId);
        }
    }

    private void cancelTimesUpNotification(final Context context) {
        for (TimerObj timerObj : Timers.timersInTimesUp(mTimers) ) {
            cancelTimesUpNotification(context, timerObj);
        }
    }

    private void cancelTimesUpNotification(final Context context, TimerObj timerObj) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(timerObj.mTimerId);
        if (Timers.LOGGING) {
            Log.v(TAG, "Canceling times-up notification for "
                    + timerObj.getLabelOrDefault(context) + " #" + timerObj.mTimerId);
        }
    }
}
