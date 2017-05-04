package com.embeddedlog.LightUpDroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

import com.embeddedlog.LightUpDroid.preferences.AlarmPreferencesActivity;
import com.embeddedlog.LightUpDroid.service.AlarmServiceBroadcastReciever;

public abstract class BaseActivity extends ActionBarActivity implements android.view.View.OnClickListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if(menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String url = null;
		Intent intent = null;
		switch (item.getItemId()) {
			case R.id.menu_item_new:
				Intent newAlarmIntent = new Intent(this, AlarmPreferencesActivity.class);
				startActivity(newAlarmIntent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	protected void callMathAlarmScheduleService() {
		Intent mathAlarmServiceIntent = new Intent(this, AlarmServiceBroadcastReciever.class);
		sendBroadcast(mathAlarmServiceIntent, null);
	}
}
