package com.embeddedlog.LightUpDroid.alarms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.embeddedlog.LightUpDroid.Log;
import com.embeddedlog.LightUpDroid.R;
import com.embeddedlog.LightUpDroid.SettingsActivity;
import com.embeddedlog.LightUpDroid.Utils;
import com.embeddedlog.LightUpDroid.provider.AlarmInstance;
import com.embeddedlog.LightUpDroid.widget.TextClock;
import com.embeddedlog.LightUpDroid.widget.multiwaveview.GlowPadView;


public class AlarmActivity extends Activity {


    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
    private static final String LOGTAG = AlarmActivity.class.getSimpleName();

    private class GlowPadController extends Handler implements GlowPadView.OnTriggerListener {
        private static final int PING_MESSAGE_WHAT = 101;
        private static final long PING_AUTO_REPEAT_DELAY_MSEC = 1200;

        public void startPinger() {
            sendEmptyMessage(PING_MESSAGE_WHAT);
        }

        public void stopPinger() {
            removeMessages(PING_MESSAGE_WHAT);
        }

        @Override
        public void handleMessage(Message msg) {
            ping();
            sendEmptyMessageDelayed(PING_MESSAGE_WHAT, PING_AUTO_REPEAT_DELAY_MSEC);
        }

        @Override
        public void onGrabbed(View v, int handle) {
            stopPinger();
        }

        @Override
        public void onReleased(View v, int handle) {
            startPinger();

        }

        @Override
        public void onTrigger(View v, int target) {
            switch (mGlowPadView.getResourceIdForTarget(target)) {
                case R.drawable.ic_alarm_alert_snooze:
                    snooze();
                    break;

                case R.drawable.ic_alarm_alert_dismiss:
                    dismiss();
                    break;
                default:
                    Log.e("Tạm dừng");
            }
        }

        @Override
        public void onGrabbedStateChange(View v, int handle) {
        }

        @Override
        public void onFinishFinalAnimation() {
        }
    }

    private AlarmInstance mAlarmInstance;
    private int mVolumeBehavior;
    private GlowPadView mGlowPadView;
    private GlowPadController glowPadController = new GlowPadController();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(ALARM_SNOOZE_ACTION)) {
                snooze();
            } else if (action.equals(ALARM_DISMISS_ACTION)) {
                dismiss();
            } else if (action.equals(AlarmService.ALARM_DONE_ACTION)) {
                finish();
            } else {
                Log.i(LOGTAG + "Lỗi trong AlarmActivity: " + action);
            }
        }
    };

    private void snooze() {
        AlarmStateManager.setSnoozeState(this, mAlarmInstance);
    }

    private void dismiss() {
        AlarmStateManager.setDismissState(this, mAlarmInstance);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final long instanceId = AlarmInstance.getId(getIntent().getData());
        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);
        if (mAlarmInstance != null) {
            Log.v(LOGTAG  + mAlarmInstance);
        } else {

            Log.v(LOGTAG + getIntent());
            finish();
            return;
        }

        final String vol =
                PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        mVolumeBehavior = Integer.parseInt(vol);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
        updateLayout();

        final IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);
    }


    private void updateTitle() {
        final String titleText = mAlarmInstance.getLabelOrDefault(this);
        TextView tv = (TextView)findViewById(R.id.alertTitle);
        tv.setText(titleText);
        super.setTitle(titleText);
    }

    private void updateLayout() {
        final LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.alarm_alert, null);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(view);
        updateTitle();
        Utils.setTimeFormat((TextClock)(view.findViewById(R.id.digitalClock)),
                (int)getResources().getDimension(R.dimen.bottom_text_size));

        mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
        mGlowPadView.setOnTriggerListener(glowPadController);
        glowPadController.startPinger();
    }

    private void ping() {
        mGlowPadView.ping();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glowPadController.startPinger();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glowPadController.stopPinger();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mAlarmInstance != null) {
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {

        Log.v(LOGTAG +  keyEvent);

        switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    switch (mVolumeBehavior) {
                        case SettingsActivity.VOLUME_BEHAVIOR_SNOOZE_INT:
                            snooze();
                            break;
                        case SettingsActivity.VOLUME_BEHAVIOR_DISMISS_INT:
                            dismiss();
                            break;
                        default:
                            break;
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(keyEvent);
        }
    }
}
