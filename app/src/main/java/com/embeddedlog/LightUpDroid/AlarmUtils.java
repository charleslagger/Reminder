package com.embeddedlog.LightUpDroid;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Build;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.android.datetimepicker.time.TimePickerDialog;
import com.embeddedlog.LightUpDroid.provider.Alarm;
import com.embeddedlog.LightUpDroid.provider.AlarmInstance;

import java.util.Calendar;

public class AlarmUtils {
    public static final String FRAG_TAG_TIME_PICKER = "time_dialog";

    public static String getFormattedTime(Context context, Calendar time) {
        String skeleton = DateFormat.is24HourFormat(context) ? (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) ? "E H:mm" : "E k:mm" : "E h:mm a";
        return (String) DateFormat.format(skeleton, time);
    }

    public static String getAlarmText(Context context, AlarmInstance instance) {
        String alarmTimeStr = getFormattedTime(context, instance.getAlarmTime());
        return !instance.mLabel.isEmpty() ? alarmTimeStr + " - " + instance.mLabel
                : alarmTimeStr;
    }

    public static void showTimeEditDialog(FragmentManager manager, final Alarm alarm,
            TimePickerDialog.OnTimeSetListener listener, boolean is24HourMode) {

        int hour, minutes;
        if (alarm == null) {
            hour = 0; minutes = 0;
        } else {
            hour = alarm.hour;
            minutes = alarm.minutes;
        }
        TimePickerDialog dialog = TimePickerDialog.newInstance(listener,
                hour, minutes, is24HourMode);
        dialog.setThemeDark(true);

        manager.executePendingTransactions();
        final FragmentTransaction ft = manager.beginTransaction();
        final Fragment prev = manager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();

        if (dialog != null && !dialog.isAdded()) {
            dialog.show(manager, FRAG_TAG_TIME_PICKER);
        }
    }

    private static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                        context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                        context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                        context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                (dispHour ? 2 : 0) |
                (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    public static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }
}
