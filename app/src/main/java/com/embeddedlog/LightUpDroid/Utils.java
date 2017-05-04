package com.embeddedlog.LightUpDroid;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.embeddedlog.LightUpDroid.stopwatch.Stopwatches;
import com.embeddedlog.LightUpDroid.timer.Timers;
import com.embeddedlog.LightUpDroid.widget.TextClock;
import com.embeddedlog.LightUpDroid.worldclock.CityObj;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class Utils {
    private final static String PARAM_LANGUAGE_CODE = "hl";


    private final static String PARAM_VERSION = "version";

    private static String sCachedVersionCode = null;

    public static final String CLOCK_TYPE_DIGITAL = "digital";
    public static final String CLOCK_TYPE_ANALOG = "analog";

    public static boolean isKitKatOrLater() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2;
    }


    public static void prepareHelpMenuItem(Context context, MenuItem helpMenuItem) {
        String helpUrlString = context.getResources().getString(R.string.desk_clock_help_url);
        if (TextUtils.isEmpty(helpUrlString)) {

            helpMenuItem.setVisible(false);
            return;
        }

        final Uri fullUri = uriWithAddedParameters(context, Uri.parse(helpUrlString));

        Intent intent = new Intent(Intent.ACTION_VIEW, fullUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        helpMenuItem.setIntent(intent);
        helpMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        helpMenuItem.setVisible(true);
    }


    private static Uri uriWithAddedParameters(Context context, Uri baseUri) {
        Uri.Builder builder = baseUri.buildUpon();

        builder.appendQueryParameter(PARAM_LANGUAGE_CODE, Locale.getDefault().toString());


        if (sCachedVersionCode == null) {

            try {

                PackageInfo info = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0);
                sCachedVersionCode = Integer.toString(info.versionCode);

                builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
            } catch (NameNotFoundException e) {

            }
        } else {
            builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
        }


        return builder.build();
    }

    public static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

    public static float calculateRadiusOffset(
            float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

    public static float calculateRadiusOffset(Resources resources) {
        if (resources != null) {
            float strokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
            float dotStrokeSize = resources.getDimension(R.dimen.circletimer_dot_size);
            float markerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
            return calculateRadiusOffset(strokeSize, dotStrokeSize, markerStrokeSize);
        } else {
            return 0f;
        }
    }

    public static int getPressedColorId() {
        return R.color.clock_red;
    }


    public static int getGrayColorId() {
        return R.color.clock_gray;
    }

    public static void clearSwSharedPref(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove (Stopwatches.PREF_START_TIME);
        editor.remove (Stopwatches.PREF_ACCUM_TIME);
        editor.remove (Stopwatches.PREF_STATE);
        int lapNum = prefs.getInt(Stopwatches.PREF_LAP_NUM, Stopwatches.STOPWATCH_RESET);
        for (int i = 0; i < lapNum; i++) {
            String key = Stopwatches.PREF_LAP_TIME + Integer.toString(i);
            editor.remove(key);
        }
        editor.remove(Stopwatches.PREF_LAP_NUM);
        editor.apply();
    }

    public static void showInUseNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_IN_USE_SHOW);
        context.sendBroadcast(timerIntent);
    }

    public static void showTimesUpNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_TIMES_UP_SHOW);
        context.sendBroadcast(timerIntent);
    }

    public static void cancelTimesUpNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_TIMES_UP_CANCEL);
        context.sendBroadcast(timerIntent);
    }

    public static class ScreensaverMoveSaverRunnable implements Runnable {
        static final long MOVE_DELAY = 60000; // DeskClock.SCREEN_SAVER_MOVE_DELAY;
        static final long SLIDE_TIME = 10000;
        static final long FADE_TIME = 3000;

        static final boolean SLIDE = false;

        private View mContentView, mSaverView;
        private final Handler mHandler;

        private static TimeInterpolator mSlowStartWithBrakes;


        public ScreensaverMoveSaverRunnable(Handler handler) {
            mHandler = handler;
            mSlowStartWithBrakes = new TimeInterpolator() {
                @Override
                public float getInterpolation(float x) {
                    return (float)(Math.cos((Math.pow(x,3) + 1) * Math.PI) / 2.0f) + 0.5f;
                }
            };
        }

        public void registerViews(View contentView, View saverView) {
            mContentView = contentView;
            mSaverView = saverView;
        }

        @Override
        public void run() {
            long delay = MOVE_DELAY;
            if (mContentView == null || mSaverView == null) {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, delay);
                return;
            }

            final float xrange = mContentView.getWidth() - mSaverView.getWidth();
            final float yrange = mContentView.getHeight() - mSaverView.getHeight();

            if (xrange == 0 && yrange == 0) {
                delay = 500;
            } else {
                final int nextx = (int) (Math.random() * xrange);
                final int nexty = (int) (Math.random() * yrange);

                if (mSaverView.getAlpha() == 0f) {

                    mSaverView.setX(nextx);
                    mSaverView.setY(nexty);
                    ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f)
                        .setDuration(FADE_TIME)
                        .start();
                } else {
                    AnimatorSet s = new AnimatorSet();
                    Animator xMove   = ObjectAnimator.ofFloat(mSaverView,
                                         "x", mSaverView.getX(), nextx);
                    Animator yMove   = ObjectAnimator.ofFloat(mSaverView,
                                         "y", mSaverView.getY(), nexty);

                    Animator xShrink = ObjectAnimator.ofFloat(mSaverView, "scaleX", 1f, 0.85f);
                    Animator xGrow   = ObjectAnimator.ofFloat(mSaverView, "scaleX", 0.85f, 1f);

                    Animator yShrink = ObjectAnimator.ofFloat(mSaverView, "scaleY", 1f, 0.85f);
                    Animator yGrow   = ObjectAnimator.ofFloat(mSaverView, "scaleY", 0.85f, 1f);
                    AnimatorSet shrink = new AnimatorSet(); shrink.play(xShrink).with(yShrink);
                    AnimatorSet grow = new AnimatorSet(); grow.play(xGrow).with(yGrow);

                    Animator fadeout = ObjectAnimator.ofFloat(mSaverView, "alpha", 1f, 0f);
                    Animator fadein = ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f);


                    if (SLIDE) {
                        s.play(xMove).with(yMove);
                        s.setDuration(SLIDE_TIME);

                        s.play(shrink.setDuration(SLIDE_TIME/2));
                        s.play(grow.setDuration(SLIDE_TIME/2)).after(shrink);
                        s.setInterpolator(mSlowStartWithBrakes);
                    } else {
                        AccelerateInterpolator accel = new AccelerateInterpolator();
                        DecelerateInterpolator decel = new DecelerateInterpolator();

                        shrink.setDuration(FADE_TIME).setInterpolator(accel);
                        fadeout.setDuration(FADE_TIME).setInterpolator(accel);
                        grow.setDuration(FADE_TIME).setInterpolator(decel);
                        fadein.setDuration(FADE_TIME).setInterpolator(decel);
                        s.play(shrink);
                        s.play(fadeout);
                        s.play(xMove.setDuration(0)).after(FADE_TIME);
                        s.play(yMove.setDuration(0)).after(FADE_TIME);
                        s.play(fadein).after(FADE_TIME);
                        s.play(grow).after(FADE_TIME);
                    }
                    s.start();
                }

                long now = System.currentTimeMillis();
                long adjust = (now % 60000);
                delay = delay
                        + (MOVE_DELAY - adjust)
                        - (SLIDE ? 0 : FADE_TIME)
                        ;
            }

            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, delay);
        }
    }

    public static long getAlarmOnQuarterHour() {
        Calendar nextQuarter = Calendar.getInstance();
        //  Set 1 second to ensure quarter-hour threshold passed.
        nextQuarter.set(Calendar.SECOND, 1);
        int minute = nextQuarter.get(Calendar.MINUTE);
        nextQuarter.add(Calendar.MINUTE, 15 - (minute % 15));
        long alarmOnQuarterHour = nextQuarter.getTimeInMillis();
        if (0 >= (alarmOnQuarterHour - System.currentTimeMillis())
                || (alarmOnQuarterHour - System.currentTimeMillis()) > 901000) {
        }
        return alarmOnQuarterHour;
    }

    public static void setMidnightUpdater(Handler handler, Runnable runnable) {
        String timezone = TimeZone.getDefault().getID();
        if (handler == null || runnable == null || timezone == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Time time = new Time(timezone);
        time.set(now);
        long runInMillis = ((24 - time.hour) * 3600 - time.minute * 60 - time.second + 1) * 1000;
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, runInMillis);
    }

    public static void cancelMidnightUpdater(Handler handler, Runnable runnable) {
        if (handler == null || runnable == null) {
            return;
        }
        handler.removeCallbacks(runnable);
    }

    public static void setQuarterHourUpdater(Handler handler, Runnable runnable) {
        String timezone = TimeZone.getDefault().getID();
        if (handler == null || runnable == null || timezone == null) {
            return;
        }
        long runInMillis = getAlarmOnQuarterHour() - System.currentTimeMillis();
        if (runInMillis < 1000) {
            runInMillis = 1000;
        }
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, runInMillis);
    }

    public static void cancelQuarterHourUpdater(Handler handler, Runnable runnable) {
        if (handler == null || runnable == null) {
            return;
        }
        handler.removeCallbacks(runnable);
    }

    public static View setClockStyle(Context context, View digitalClock, View analogClock,
            String clockStyleKey) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultClockStyle = context.getResources().getString(R.string.default_clock_style);
        String style = sharedPref.getString(clockStyleKey, defaultClockStyle);
        View returnView;
        if (style.equals(CLOCK_TYPE_ANALOG)) {
            digitalClock.setVisibility(View.GONE);
            analogClock.setVisibility(View.VISIBLE);
            returnView = analogClock;
        } else {
            digitalClock.setVisibility(View.VISIBLE);
            analogClock.setVisibility(View.GONE);
            returnView = digitalClock;
        }

        return returnView;
    }

    public static void dimClockView(boolean dim, View clockView) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter(
                        (dim ? 0x40FFFFFF : 0xC0FFFFFF),
                PorterDuff.Mode.MULTIPLY));
        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    public static void refreshAlarm(Context context, View clock) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);
        TextView nextAlarmView;
        nextAlarmView = (TextView) clock.findViewById(R.id.nextAlarm);
        if (!TextUtils.isEmpty(nextAlarm) && nextAlarmView != null) {
            nextAlarmView.setText(
                    context.getString(R.string.control_set_alarm_with_existing, nextAlarm));
            nextAlarmView.setContentDescription(context.getResources().getString(
                    R.string.next_alarm_description, nextAlarm));
            nextAlarmView.setVisibility(View.VISIBLE);
        } else  {
            nextAlarmView.setVisibility(View.GONE);
        }
    }

    public static void updateDate(
            String dateFormat, String dateFormatForAccessibility, View clock) {

        Date now = new Date();
        TextView dateDisplay;
        dateDisplay = (TextView) clock.findViewById(R.id.date);
        if (dateDisplay != null) {
            final Locale l = Locale.getDefault();
            String fmt = dateFormat;
            SimpleDateFormat sdf = new SimpleDateFormat(fmt, l);
            dateDisplay.setText(sdf.format(now));
            dateDisplay.setVisibility(View.VISIBLE);
            fmt = dateFormatForAccessibility;
            sdf = new SimpleDateFormat(fmt, l);
            dateDisplay.setContentDescription(sdf.format(now));
        }
    }


    public static void setTimeFormat(TextClock clock, int amPmFontSize) {
        if (clock != null) {
            clock.setFormat12Hour(get12ModeFormat(amPmFontSize));
            clock.setFormat24Hour(get24ModeFormat());
        }
    }

    public static CharSequence get12ModeFormat(int amPmFontSize) {
        String pattern = "h:mm a";
        if (amPmFontSize <= 0) {
            pattern.replaceAll("a", "").trim();
        }
        pattern = pattern.replaceAll(" ", "\u200A");
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }
        Spannable sp = new SpannableString(pattern);
        sp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        sp.setSpan(new AbsoluteSizeSpan(amPmFontSize), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        sp.setSpan(new TypefaceSpan("sans-serif-condensed"), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        return sp;
    }

    public static CharSequence get24ModeFormat() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return "H:mm";
        } else {
            return "k:mm";
        }
    }

    public static CityObj[] loadCitiesFromXml(Context c) {
        Resources r = c.getResources();
        String[] cities = r.getStringArray(R.array.cities_names);
        String[] timezones = r.getStringArray(R.array.cities_tz);
        String[] ids = r.getStringArray(R.array.cities_id);
        int minLength = cities.length;
        if (cities.length != timezones.length || ids.length != cities.length) {
            minLength = Math.min(cities.length, Math.min(timezones.length, ids.length));
        }
        CityObj[] tempList = new CityObj[minLength];
        for (int i = 0; i < cities.length; i++) {
            tempList[i] = new CityObj(cities[i], timezones[i], ids[i]);
        }
        return tempList;
    }

    public static String getGMTHourOffset(TimeZone timezone, boolean showMinutes) {
        StringBuilder sb = new StringBuilder();
        sb.append("GMT");
        int gmtOffset = timezone.getRawOffset();
        if (gmtOffset < 0) {
            sb.append('-');
        } else {
            sb.append('+');
        }
        sb.append(Math.abs(gmtOffset) / DateUtils.HOUR_IN_MILLIS); // Hour

        if (showMinutes) {
            final int min = (Math.abs(gmtOffset) / (int) DateUtils.MINUTE_IN_MILLIS) % 60;
            sb.append(':');
            if (min < 10) {
                sb.append('0');
            }
            sb.append(min);
        }

        return sb.toString();
    }

    public static String getCityName(CityObj city, CityObj dbCity) {
        return (city.mCityId == null || dbCity == null) ? city.mCityName : dbCity.mCityName;
    }
}
