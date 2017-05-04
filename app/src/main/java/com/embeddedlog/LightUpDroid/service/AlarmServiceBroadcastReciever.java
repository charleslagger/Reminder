package com.embeddedlog.LightUpDroid.service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmServiceBroadcastReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent(context, com.embeddedlog.LightUpDroid.service.AlarmService.class);
		context.startService(serviceIntent);
	}

}
