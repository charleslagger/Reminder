package com.embeddedlog.LightUpDroid.preferences;

import android.content.Context;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.embeddedlog.LightUpDroid.Alarm;
import com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AlarmPreferenceListAdapter extends BaseAdapter implements Serializable {

	private Context context;
	private Alarm alarm;
	private List<com.embeddedlog.LightUpDroid.preferences.AlarmPreference> preferences = new ArrayList<com.embeddedlog.LightUpDroid.preferences.AlarmPreference>();
	private final String[] repeatDays = {"Chủ nhật","Thứ hai","Thứ ba","Thứ tư","Thứ năm","Thứ sáu","Thứ bảy"};


	private String[] alarmTones;
	private String[] alarmTonePaths;

	public AlarmPreferenceListAdapter(Context context, Alarm alarm) {
		setContext(context);
		RingtoneManager ringtoneMgr = new RingtoneManager(getContext());
		ringtoneMgr.setType(RingtoneManager.TYPE_ALARM);
		Cursor alarmsCursor = ringtoneMgr.getCursor();

		alarmTones = new String[alarmsCursor.getCount()+1];
		alarmTones[0] = "Silent";
		alarmTonePaths = new String[alarmsCursor.getCount()+1];
		alarmTonePaths[0] = "";

		if (alarmsCursor.moveToFirst()) {
			do {
				alarmTones[alarmsCursor.getPosition()+1] = ringtoneMgr.getRingtone(alarmsCursor.getPosition()).getTitle(getContext());
				alarmTonePaths[alarmsCursor.getPosition()+1] = ringtoneMgr.getRingtoneUri(alarmsCursor.getPosition()).toString();
			}while(alarmsCursor.moveToNext());
		}
		alarmsCursor.close();
		setMathAlarm(alarm);
	}

	@Override
	public int getCount() {
		return preferences.size();
	}

	@Override
	public Object getItem(int position) {
		return preferences.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		com.embeddedlog.LightUpDroid.preferences.AlarmPreference alarmPreference = (com.embeddedlog.LightUpDroid.preferences.AlarmPreference) getItem(position);
		LayoutInflater layoutInflater = LayoutInflater.from(getContext());
		switch (alarmPreference.getType()) {
			case BOOLEAN:
				if(null == convertView || convertView.getId() != android.R.layout.simple_list_item_checked)
					convertView = layoutInflater.inflate(android.R.layout.simple_list_item_checked, null);

				CheckedTextView checkedTextView = (CheckedTextView) convertView.findViewById(android.R.id.text1);
				checkedTextView.setText(alarmPreference.getTitle());
				checkedTextView.setChecked((Boolean) alarmPreference.getValue());
				break;
			case INTEGER:
			case STRING:
			case LIST:
			case MULTIPLE_LIST:
			case TIME:
			default:
				if(null == convertView || convertView.getId() != android.R.layout.simple_list_item_2)
					convertView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null);

				TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
				text1.setTextSize(18);
				text1.setText(alarmPreference.getTitle());

				TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
				text2.setText(alarmPreference.getSummary());
				break;
		}

		return convertView;
	}
	public Alarm getMathAlarm() {
		for(com.embeddedlog.LightUpDroid.preferences.AlarmPreference preference : preferences){
			switch(preference.getKey()){
				case ALARM_ACTIVE:
					alarm.setAlarmActive((Boolean) preference.getValue());
					break;
				case ALARM_NAME:
					alarm.setAlarmName((String) preference.getValue());
					break;
				case ALARM_LOC:
					alarm.setAlarmLoc((String) preference.getValue());
					break;
				case ALARM_TIME:
					alarm.setAlarmTime((String) preference.getValue());
					break;
				case ALARM_TONE:
					alarm.setAlarmTonePath((String) preference.getValue());
					break;
				case ALARM_VIBRATE:
					alarm.setVibrate((Boolean) preference.getValue());
					break;
				case ALARM_REPEAT:
					alarm.setDays((Alarm.Day[]) preference.getValue());
					break;
			}
		}
		return alarm;
	}
	public void setMathAlarm(Alarm alarm) {
		this.alarm = alarm;
		preferences.clear();
		preferences.add(new com.embeddedlog.LightUpDroid.preferences.AlarmPreference(com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key.ALARM_ACTIVE,"Bật/Tắt thông báo", null, null, alarm.getAlarmActive(),Type.BOOLEAN));
		preferences.add(new com.embeddedlog.LightUpDroid.preferences.AlarmPreference(com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key.ALARM_NAME, "Tên môn học",alarm.getAlarmName(), null, alarm.getAlarmName(), Type.STRING));
		preferences.add(new com.embeddedlog.LightUpDroid.preferences.AlarmPreference(com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key.ALARM_LOC, "Phòng học",alarm.getAlarmLoc(), null, alarm.getAlarmLoc(), Type.STRING));
		preferences.add(new com.embeddedlog.LightUpDroid.preferences.AlarmPreference(com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key.ALARM_TIME, "Giờ vào lớp",alarm.getAlarmTimeString(), null, alarm.getAlarmTime(), Type.TIME));
		preferences.add(new com.embeddedlog.LightUpDroid.preferences.AlarmPreference(com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key.ALARM_REPEAT, "Lặp lại",alarm.getRepeatDaysString(), repeatDays, alarm.getDays(),Type.MULTIPLE_LIST));

		Uri alarmToneUri = Uri.parse(alarm.getAlarmTonePath());
		Ringtone alarmTone = RingtoneManager.getRingtone(getContext(), alarmToneUri);

		if(alarmTone instanceof Ringtone && !alarm.getAlarmTonePath().equalsIgnoreCase("")){
			preferences.add(new com.embeddedlog.LightUpDroid.preferences.AlarmPreference(com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key.ALARM_TONE, "Nhạc chuông", alarmTone.getTitle(getContext()),alarmTones, alarm.getAlarmTonePath(), Type.LIST));
		}else{
			preferences.add(new com.embeddedlog.LightUpDroid.preferences.AlarmPreference(com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key.ALARM_TONE, "Nhạc chuông", getAlarmTones()[0],alarmTones, null, Type.LIST));
		}

		preferences.add(new com.embeddedlog.LightUpDroid.preferences.AlarmPreference(com.embeddedlog.LightUpDroid.preferences.AlarmPreference.Key.ALARM_VIBRATE, "Rung khi thông báo",null, null, alarm.getVibrate(), Type.BOOLEAN));
	}


	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public String[] getRepeatDays() {
		return repeatDays;
	}


	public String[] getAlarmTones() {
		return alarmTones;
	}

	public String[] getAlarmTonePaths() {
		return alarmTonePaths;
	}

}
