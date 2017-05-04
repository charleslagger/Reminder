package com.embeddedlog.LightUpDroid;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

import com.embeddedlog.LightUpDroid.alarms.AlarmStateManager;
import com.embeddedlog.LightUpDroid.provider.AlarmInstance;
import com.embeddedlog.LightUpDroid.timer.TimerObj;

public class AlarmInitReceiver extends BroadcastReceiver {

    private static final String PREF_VOLUME_DEF_DONE = "vol_def_done";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        Log.v("AlarmInitReceiver " + action);

        final PendingResult result = goAsync();
        final WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();

        AlarmStateManager.updateGloablIntentId(context);
        AsyncHandler.post(new Runnable() {
            @Override public void run() {

                if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {

                    SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(context);
                    TimerObj.resetTimersInSharedPrefs(prefs);
                    Utils.clearSwSharedPref(prefs);

                    if (!prefs.getBoolean(PREF_VOLUME_DEF_DONE, false)) {

                        switchVolumeButtonDefault(prefs);
                    }
                }

                AlarmStateManager.fixAlarmInstances(context);

                result.finish();
                wl.release();
            }
        });
    }

    private void switchVolumeButtonDefault(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
            SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);

        editor.putBoolean(PREF_VOLUME_DEF_DONE, true);
        editor.apply();
    }
}
