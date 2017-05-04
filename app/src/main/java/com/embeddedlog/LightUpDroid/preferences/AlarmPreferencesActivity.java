package com.embeddedlog.LightUpDroid.preferences;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.embeddedlog.LightUpDroid.Alarm;
import com.embeddedlog.LightUpDroid.BaseActivity;
import com.embeddedlog.LightUpDroid.R;
import com.embeddedlog.LightUpDroid.database.Database;
import com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key;

import java.util.Calendar;

public class AlarmPreferencesActivity extends BaseActivity {
	private Alarm alarm;
	private MediaPlayer mediaPlayer;
	private ListAdapter listAdapter;
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.alarm_preferences);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.containsKey("alarm")) {
			setMathAlarm((Alarm) bundle.getSerializable("alarm"));
		} else {
			setMathAlarm(new Alarm());
		}
		if (bundle != null && bundle.containsKey("adapter")) {
			setListAdapter((AlarmPreferenceListAdapter) bundle.getSerializable("adapter"));
		} else {
			setListAdapter(new AlarmPreferenceListAdapter(this, getMathAlarm()));
		}

		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				final AlarmPreferenceListAdapter alarmPreferenceListAdapter = (AlarmPreferenceListAdapter) getListAdapter();
				final AlarmPreference alarmPreference = (AlarmPreference) alarmPreferenceListAdapter.getItem(position);

				AlertDialog.Builder alert;
				v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				switch (alarmPreference.getType()) {
					case BOOLEAN:
						CheckedTextView checkedTextView = (CheckedTextView) v;
						boolean checked = !checkedTextView.isChecked();
						((CheckedTextView) v).setChecked(checked);
						switch (alarmPreference.getKey()) {
							case ALARM_ACTIVE:
								alarm.setAlarmActive(checked);
								break;
							case ALARM_VIBRATE:
								alarm.setVibrate(checked);
								if (checked) {
									Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
									vibrator.vibrate(1000);
								}
								break;
						}
						alarmPreference.setValue(checked);
						break;
					case STRING:

						alert = new AlertDialog.Builder(AlarmPreferencesActivity.this);

						alert.setTitle(alarmPreference.getTitle());
						// alert.setMessage(message);
						final EditText input = new EditText(AlarmPreferencesActivity.this);

						input.setText(alarmPreference.getValue().toString());

						alert.setView(input);
						alert.setPositiveButton("Hoàn tất", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {

								alarmPreference.setValue(input.getText().toString());

								if (alarmPreference.getKey() == Key.ALARM_NAME) {
									alarm.setAlarmName(alarmPreference.getValue().toString());
								}
								alarmPreferenceListAdapter.setMathAlarm(getMathAlarm());
								alarmPreferenceListAdapter.notifyDataSetChanged();


								if (alarmPreference.getKey() == Key.ALARM_LOC) {
									alarm.setAlarmLoc(alarmPreference.getValue().toString());
								}
								alarmPreferenceListAdapter.setMathAlarm(getMathAlarm());
								alarmPreferenceListAdapter.notifyDataSetChanged();
							}
						});
						alert.show();
						break;

					case LIST:
						alert = new AlertDialog.Builder(AlarmPreferencesActivity.this);

						alert.setTitle(alarmPreference.getTitle());
						// alert.setMessage(message);

						CharSequence[] items = new CharSequence[alarmPreference.getOptions().length];
						for (int i = 0; i < items.length; i++)
							items[i] = alarmPreference.getOptions()[i];

						alert.setItems(items, new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (alarmPreference.getKey()) {
									case ALARM_TONE:
										alarm.setAlarmTonePath(alarmPreferenceListAdapter.getAlarmTonePaths()[which]);
										if (alarm.getAlarmTonePath() != null) {
											if (mediaPlayer == null) {
												mediaPlayer = new MediaPlayer();
											} else {
												if (mediaPlayer.isPlaying())
													mediaPlayer.stop();
												mediaPlayer.reset();
											}
											try {
												// mediaPlayer.setVolume(1.0f, 1.0f);
												mediaPlayer.setVolume(0.2f, 0.2f);
												mediaPlayer.setDataSource(AlarmPreferencesActivity.this, Uri.parse(alarm.getAlarmTonePath()));
												mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
												mediaPlayer.setLooping(false);
												mediaPlayer.prepare();
												mediaPlayer.start();

												if (alarmToneTimer != null)
													alarmToneTimer.cancel();
												alarmToneTimer = new CountDownTimer(3000, 3000) {
													@Override
													public void onTick(long millisUntilFinished) {

													}

													@Override
													public void onFinish() {
														try {
															if (mediaPlayer.isPlaying())
																mediaPlayer.stop();
														} catch (Exception e) {

														}
													}
												};
												alarmToneTimer.start();
											} catch (Exception e) {
												try {
													if (mediaPlayer.isPlaying())
														mediaPlayer.stop();
												} catch (Exception e2) {

												}
											}
										}
										break;
									default:
										break;
								}
								alarmPreferenceListAdapter.setMathAlarm(getMathAlarm());
								alarmPreferenceListAdapter.notifyDataSetChanged();
							}

						});

						alert.show();
						break;
					case MULTIPLE_LIST:
						alert = new AlertDialog.Builder(AlarmPreferencesActivity.this);

						alert.setTitle(alarmPreference.getTitle());
						// alert.setMessage(message);

						CharSequence[] multiListItems = new CharSequence[alarmPreference.getOptions().length];
						for (int i = 0; i < multiListItems.length; i++)
							multiListItems[i] = alarmPreference.getOptions()[i];

						boolean[] checkedItems = new boolean[multiListItems.length];
						for (Alarm.Day day : getMathAlarm().getDays()) {
							checkedItems[day.ordinal()] = true;
						}
						alert.setMultiChoiceItems(multiListItems, checkedItems, new OnMultiChoiceClickListener() {

							@Override
							public void onClick(final DialogInterface dialog, int which, boolean isChecked) {

								Alarm.Day thisDay = Alarm.Day.values()[which];

								if (isChecked) {
									alarm.addDay(thisDay);
								} else {
									if (alarm.getDays().length > 1) {
										alarm.removeDay(thisDay);
									} else {
										((AlertDialog) dialog).getListView().setItemChecked(which, true);
									}
								}

							}
						});
						alert.setOnCancelListener(new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								alarmPreferenceListAdapter.setMathAlarm(getMathAlarm());
								alarmPreferenceListAdapter.notifyDataSetChanged();

							}
						});
						alert.show();
						break;
					case TIME:
						TimePickerDialog timePickerDialog = new TimePickerDialog(AlarmPreferencesActivity.this, new OnTimeSetListener() {

							@Override
							public void onTimeSet(TimePicker timePicker, int hours, int minutes) {
								Calendar newAlarmTime = Calendar.getInstance();
								newAlarmTime.set(Calendar.HOUR_OF_DAY, hours);
								newAlarmTime.set(Calendar.MINUTE, minutes);
								newAlarmTime.set(Calendar.SECOND, 0);
								alarm.setAlarmTime(newAlarmTime);
								alarmPreferenceListAdapter.setMathAlarm(getMathAlarm());
								alarmPreferenceListAdapter.notifyDataSetChanged();
							}
						}, alarm.getAlarmTime().get(Calendar.HOUR_OF_DAY), alarm.getAlarmTime().get(Calendar.MINUTE), true);
						timePickerDialog.setTitle(alarmPreference.getTitle());
						timePickerDialog.show();
					default:
						break;
				}
			}
		});
	}

	public void resurfS(){
		Toast.makeText(AlarmPreferencesActivity.this, alarm.getAlarmName(), Toast.LENGTH_SHORT).show();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.findItem(R.id.menu_item_new).setVisible(false);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_save:
				Database.init(getApplicationContext());
				if (getMathAlarm().getId() < 1) {
					Database.create(getMathAlarm());
				} else {
					Database.update(getMathAlarm());
				}
				callMathAlarmScheduleService();
				Toast.makeText(AlarmPreferencesActivity.this, getMathAlarm().getTimeUntilNextAlarmMessage(), Toast.LENGTH_LONG).show();
				finish();
				break;
			case R.id.menu_item_delete:
				AlertDialog.Builder dialog = new AlertDialog.Builder(AlarmPreferencesActivity.this);
				dialog.setTitle("Xóa Lịch");
				dialog.setMessage("Bạn chắc chắn chứ?");
				dialog.setPositiveButton("Có", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						Database.init(getApplicationContext());
						if (getMathAlarm().getId() < 1) {
						} else {
							Database.deleteEntry(alarm);
							callMathAlarmScheduleService();
						}
						finish();
					}
				});
				dialog.setNegativeButton("Thôi", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				dialog.show();

				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private CountDownTimer alarmToneTimer;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("alarm", getMathAlarm());
		outState.putSerializable("adapter", (AlarmPreferenceListAdapter) getListAdapter());
	}

	;

	@Override
	protected void onPause() {
		super.onPause();
		try {
			if (mediaPlayer != null)
				mediaPlayer.release();
		} catch (Exception e) {
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	public Alarm getMathAlarm() {
		return alarm;
	}

	public void setMathAlarm(Alarm alarm) {
		this.alarm = alarm;
	}

	public ListAdapter getListAdapter() {
		return listAdapter;
	}

	public void setListAdapter(ListAdapter listAdapter) {
		this.listAdapter = listAdapter;
		getListView().setAdapter(listAdapter);

	}

	public ListView getListView() {
		if (listView == null)
			listView = (ListView) findViewById(android.R.id.list);
		return listView;
	}

	public void setListView(ListView listView) {
		this.listView = listView;
	}

	@Override
	public void onClick(View v) {
		// super.onClick(v);

	}
}
