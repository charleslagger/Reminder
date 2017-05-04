package com.embeddedlog.LightUpDroid.timer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.embeddedlog.LightUpDroid.R;
import com.embeddedlog.LightUpDroid.Utils;
import com.embeddedlog.LightUpDroid.timer.TimerFragment.OnEmptyListListener;

public class TimerAlertFullScreen extends Activity implements OnEmptyListListener {

    private static final String TAG = "TimerAlertFullScreen";
    private static final String FRAGMENT = "timer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.timer_alert_full_screen);
        final View view = findViewById(R.id.fragment_container);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        if (getFragment() == null) {
            TimerFragment timerFragment = new TimerFragment();

            Bundle args = new Bundle();
            args.putBoolean(Timers.TIMESUP_MODE, true);

            timerFragment.setArguments(args);

            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, timerFragment, FRAGMENT).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.cancelTimesUpNotifications(this);
    }

    @Override
    public void onPause() {
        Utils.showTimesUpNotifications(this);

        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        switch (event.getKeyCode()) {

        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_MUTE:
        case KeyEvent.KEYCODE_CAMERA:
        case KeyEvent.KEYCODE_FOCUS:
            if (up) {
                stopAllTimesUpTimers();
            }
            return true;

        default:
            break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        TimerFragment timerFragment = getFragment();
        if (timerFragment != null) {
            timerFragment.restartAdapter();
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        ViewGroup viewContainer = (ViewGroup)findViewById(R.id.fragment_container);
        viewContainer.requestLayout();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void stopAllTimesUpTimers() {
        TimerFragment timerFragment = getFragment();
        if (timerFragment != null) {
            timerFragment.stopAllTimesUpTimers();
        }
    }

    @Override
    public void onEmptyList() {
        if (Timers.LOGGING) {
            Log.v(TAG, "onEmptyList");
        }
        onListChanged();
        finish();
    }

    @Override
    public void onListChanged() {
        Utils.showInUseNotifications(this);
    }

    private TimerFragment getFragment() {
        return (TimerFragment) getFragmentManager().findFragmentByTag(FRAGMENT);
    }
}
