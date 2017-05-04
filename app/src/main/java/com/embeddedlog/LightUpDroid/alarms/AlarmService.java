package com.embeddedlog.LightUpDroid.alarms;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.embeddedlog.LightUpDroid.AlarmAlertWakeLock;
import com.embeddedlog.LightUpDroid.Log;
import com.embeddedlog.LightUpDroid.provider.AlarmInstance;


public class AlarmService extends Service {

    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";

    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

    public static final String START_ALARM_ACTION = "START_ALARM";

    public static final String STOP_ALARM_ACTION = "STOP_ALARM";

    public static void startAlarm(Context context, AlarmInstance instance) {
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(START_ALARM_ACTION);


        AlarmAlertWakeLock.acquireCpuWakeLock(context);
        context.startService(intent);
    }

    public static void stopAlarm(Context context, AlarmInstance instance) {
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(STOP_ALARM_ACTION);

        context.startService(intent);
    }

    private TelephonyManager mTelephonyManager;
    private int mInitialCallState;
    private AlarmInstance mCurrentAlarm = null;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {

            if (state != TelephonyManager.CALL_STATE_IDLE && state != mInitialCallState) {
                sendBroadcast(AlarmStateManager.createStateChangeIntent(AlarmService.this,
                        "AlarmService", mCurrentAlarm, AlarmInstance.MISSED_STATE));
            }
        }
    };

    private void startAlarm(AlarmInstance instance) {
        Log.v("AlarmService.start : " + instance.mId);
        if (mCurrentAlarm != null) {
            AlarmStateManager.setMissedState(this, mCurrentAlarm);
            stopCurrentAlarm();
        }

        AlarmAlertWakeLock.acquireCpuWakeLock(this);
        mCurrentAlarm = instance;
        AlarmNotifications.showAlarmNotification(this, mCurrentAlarm);
        mInitialCallState = mTelephonyManager.getCallState();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        boolean inCall = mInitialCallState != TelephonyManager.CALL_STATE_IDLE;
        AlarmKlaxon.start(this, mCurrentAlarm, inCall);
        sendBroadcast(new Intent(ALARM_ALERT_ACTION));
    }

    private void stopCurrentAlarm() {
        if (mCurrentAlarm == null) {
            Log.v(" không dừng lại đc");
            return;
        }

        Log.v("AlarmService.stop : " + mCurrentAlarm.mId);
        AlarmKlaxon.stop(this);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        sendBroadcast(new Intent(ALARM_DONE_ACTION));
        mCurrentAlarm = null;
        AlarmAlertWakeLock.releaseCpuLock();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        long instanceId = AlarmInstance.getId(intent.getData());
        if (START_ALARM_ACTION.equals(intent.getAction())) {
            ContentResolver cr = this.getContentResolver();
            AlarmInstance instance = AlarmInstance.getInstance(cr, instanceId);
            if (instance == null) {
                if (mCurrentAlarm != null) {

                    AlarmAlertWakeLock.releaseCpuLock();
                }
                return Service.START_NOT_STICKY;
            } else if (mCurrentAlarm != null && mCurrentAlarm.mId == instanceId) {
                return Service.START_NOT_STICKY;
            }
            startAlarm(instance);
        } else if(STOP_ALARM_ACTION.equals(intent.getAction())) {
            if (mCurrentAlarm != null && mCurrentAlarm.mId != instanceId) {

                return Service.START_NOT_STICKY;
            }
            stopSelf();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        stopCurrentAlarm();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
