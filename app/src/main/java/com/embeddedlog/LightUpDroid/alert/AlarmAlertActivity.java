package com.embeddedlog.LightUpDroid.alert;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.embeddedlog.LightUpDroid.Alarm;
import com.embeddedlog.LightUpDroid.R;


public class AlarmAlertActivity extends Activity {

	private Alarm alarm;
	private MediaPlayer mediaPlayer;
	private TextView mTextView, textViewLoc, textViewTime;

	private Vibrator vibrator;
	private Button button_cancel2;
	public boolean alarmActive;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.alert_activity);



		button_cancel2 =(Button) findViewById(R.id.button_cancel2);
		mTextView = (TextView)findViewById(R.id.alarm_title_text);
		textViewLoc = (TextView)findViewById(R.id.alarm_loc_text);
		textViewTime = (TextView)findViewById(R.id.alarm_time_text);


		Bundle bundle = this.getIntent().getExtras();
		alarm = (Alarm) bundle.getSerializable("alarm");
		TelephonyManager telephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		PhoneStateListener phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch (state) {
					case TelephonyManager.CALL_STATE_RINGING:
						try {
							mediaPlayer.pause();
						} catch (IllegalStateException e) {

						}
						break;
					case TelephonyManager.CALL_STATE_IDLE:
						try {
							mediaPlayer.start();
						} catch (IllegalStateException e) {

						}
						break;
				}
				super.onCallStateChanged(state, incomingNumber);
			}
		};
		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);

		startAlarm();
		button_cancel2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopAlarm();

			}
		});

	}

	private void startAlarm() {
		mediaPlayer = new MediaPlayer();

		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		long[] pattern = { 1000, 200, 200, 200 };
		vibrator.vibrate(pattern, 0);
		try {
			mediaPlayer.setVolume(1.0f, 1.0f);
			mTextView.setText(alarm.getAlarmName());
			textViewLoc.setText(alarm.getAlarmLoc());
			textViewTime.setText(alarm.getAlarmTimeString());

			mediaPlayer.setDataSource(this,
					Uri.parse(alarm.getAlarmTonePath()));
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			mediaPlayer.setLooping(true);
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (Exception e) {
			mediaPlayer.release();
			alarmActive = false;
		}


	}
	public void stopAlarm(){
		alarmActive = false;
//				vibrator.cancel();
//				mediaPlayer.stop();
		Toast.makeText(AlarmAlertActivity.this, "Tắt thông báo", Toast.LENGTH_SHORT).show();
		finish();
		System.exit(0);
	}


	@Override
	public void onBackPressed() {
		if (!alarmActive)
			super.onBackPressed();
	}

	@Override
	protected void onPause() {
		super.onPause();
		StaticWakeLock.lockOff(this);
	}

}
