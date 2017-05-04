package com.embeddedlog.LightUpDroid.alarms;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import com.embeddedlog.LightUpDroid.Log;
import com.embeddedlog.LightUpDroid.R;
import com.embeddedlog.LightUpDroid.provider.AlarmInstance;

import java.io.IOException;


public class AlarmKlaxon {
    private static final long[] sVibratePattern = new long[] { 500, 500 };

    private static final float IN_CALL_VOLUME = 0.125f;

    private static boolean sStarted = false;
    private static MediaPlayer sMediaPlayer = null;

    public static void stop(Context context) {

        if (sStarted) {
            sStarted = false;
            if (sMediaPlayer != null) {
                sMediaPlayer.stop();
                AudioManager audioManager = (AudioManager)
                        context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(null);
                sMediaPlayer.release();
                sMediaPlayer = null;
            }

            ((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
        }
    }

    public static void start(final Context context, AlarmInstance instance,
            boolean inTelephoneCall) {

        stop(context);

        if (!AlarmInstance.NO_RINGTONE_URI.equals(instance.mRingtone)) {
            Uri alarmNoise = instance.mRingtone;
            if (alarmNoise == null) {
                alarmNoise = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (Log.LOGV) {
                    Log.v("Using default alarm: " + alarmNoise.toString());
                }
            }

            sMediaPlayer = new MediaPlayer();
            sMediaPlayer.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    AlarmKlaxon.stop(context);
                    return true;
                }
            });

            try {

                if (inTelephoneCall) {
                    sMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                    setDataSourceFromResource(context, sMediaPlayer, R.raw.in_call_alarm);
                } else {
                    sMediaPlayer.setDataSource(context, alarmNoise);
                }
                startAlarm(context, sMediaPlayer);
            } catch (Exception ex) {
                try {

                    sMediaPlayer.reset();
                    setDataSourceFromResource(context, sMediaPlayer, R.raw.fallbackring);
                    startAlarm(context, sMediaPlayer);
                } catch (Exception ex2) {
                    Log.e("không tải đc nhạc chuông", ex2);
                }
            }
        }

        if (instance.mVibrate && !inTelephoneCall) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(sVibratePattern, 0);
        }

        sStarted = true;
    }

    private static void startAlarm(Context context, MediaPlayer player) throws IOException {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            audioManager.requestAudioFocus(null,
                    AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            player.start();
        }
    }

    private static void setDataSourceFromResource(Context context, MediaPlayer player, int res)
            throws IOException {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }
}
