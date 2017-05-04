package com.embeddedlog.LightUpDroid.widget;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.*;

import static android.view.ViewDebug.ExportedProperty;
import static android.widget.RemoteViews.*;


@RemoteView
public class TextClock extends TextView {

    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";

    public static final CharSequence DEFAULT_FORMAT_24_HOUR =  Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 ? "H:mm" : "k:mm";

    private CharSequence mFormat12;
    private CharSequence mFormat24;

    @ExportedProperty
    private CharSequence mFormat;
    @ExportedProperty
    private boolean mHasSeconds;

    private boolean mAttached;

    private Calendar mTime;
    private String mTimeZone;

    private final ContentObserver mFormatChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            chooseFormat();
            onTimeChanged();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            chooseFormat();
            onTimeChanged();
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String timeZone = intent.getStringExtra("time-zone");
                createTime(timeZone);
            }
            onTimeChanged();
        }
    };

    private final Runnable mTicker = new Runnable() {
        public void run() {
            onTimeChanged();

            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);

            getHandler().postAtTime(mTicker, next);
        }
    };

    public TextClock(Context context) {
        super(context);
        init();
    }

    public TextClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public TextClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mFormat12 = "h:mm a";

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mFormat24 = "H:mm";
        } else {
            mFormat24 = "k:mm";
        }


        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"),
                Locale.getDefault());
        Date currentLocalTime = calendar.getTime();
        SimpleDateFormat date = new SimpleDateFormat("Z");
        String localTime = date.format(currentLocalTime);
        try {
            localTime = "GMT" + localTime.substring(0, 3) + ":" + localTime.substring(3);
            android.util.Log.v("time_zone", localTime);
            mTimeZone = localTime;
        } catch (Exception e) {
            mTimeZone = localTime;
        }

        init();
    }

    private void init() {
        if (mFormat12 == null || mFormat24 == null) {
            LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);
            if (mFormat12 == null) {
                mFormat12 = ld.timeFormat12;
            }
            if (mFormat24 == null) {
                mFormat24 = ld.timeFormat24;
            }
        }

        createTime(mTimeZone);
        // Wait until onAttachedToWindow() to handle the ticker
        chooseFormat(false);
    }

    private void createTime(String timeZone) {
        if (timeZone != null) {
            mTime = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        } else {
            mTime = Calendar.getInstance();
        }
    }


    @ExportedProperty
    public CharSequence getFormat12Hour() {
        return mFormat12;
    }


    public void setFormat12Hour(CharSequence format) {
        mFormat12 = format;

        chooseFormat();
        onTimeChanged();
    }


    @ExportedProperty
    public CharSequence getFormat24Hour() {
        return mFormat24;
    }


    public void setFormat24Hour(CharSequence format) {
        mFormat24 = format;

        chooseFormat();
        onTimeChanged();
    }


    public boolean is24HourModeEnabled() {
        return DateFormat.is24HourFormat(getContext());
    }


    public String getTimeZone() {
        return mTimeZone;
    }

    public void setTimeZone(String timeZone) {
        mTimeZone = timeZone;

        createTime(timeZone);
        onTimeChanged();
    }


    private void chooseFormat() {
        chooseFormat(true);
    }


    public CharSequence getFormat() {
        return mFormat;
    }


    private void chooseFormat(boolean handleTicker) {
        final boolean format24Requested = is24HourModeEnabled();

        if (format24Requested) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mFormat = abc(mFormat24, mFormat12, "H:mm");
            } else {
                mFormat = abc(mFormat24, mFormat12, "k:mm");
            }
        } else {
            mFormat = abc(mFormat12, mFormat24, "h:mm a");
        }

        boolean hadSeconds = mHasSeconds;
        mHasSeconds = true;

        if (handleTicker && mAttached && hadSeconds != mHasSeconds) {
            if (hadSeconds) getHandler().removeCallbacks(mTicker);
            else mTicker.run();
        }
    }


    private static CharSequence abc(CharSequence a, CharSequence b, CharSequence c) {
        return a == null ? (b == null ? c : b) : a;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;

            registerReceiver();
            registerObserver();

            createTime(mTimeZone);

            if (mHasSeconds) {
                mTicker.run();
            } else {
                onTimeChanged();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached) {
            unregisterReceiver();
            unregisterObserver();

            getHandler().removeCallbacks(mTicker);

            mAttached = false;
        }
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    private void registerObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }

    private void unregisterObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.unregisterContentObserver(mFormatChangeObserver);
    }

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());
        setText(DateFormat.format(mFormat, mTime));
    }
}