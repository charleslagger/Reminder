package com.embeddedlog.LightUpDroid;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;

import com.embeddedlog.LightUpDroid.provider.Alarm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class AlarmTimelineView extends View {

    private static final String TAG = "AlarmTimelineView";

    private static final String FORMAT_12_HOUR = "E h mm a";
    private static final String FORMAT_24_HOUR = "E H mm";

    private static final int DAYS_IN_WEEK = 7;

    private int mAlarmTimelineColor;
    private int mAlarmTimelineLength;
    private int mAlarmTimelineMarginTop;
    private int mAlarmTimelineMarginBottom;
    private int mAlarmNodeRadius;
    private int mAlarmNodeInnerRadius;
    private int mAlarmNodeInnerRadiusColor;
    private int mAlarmTextPadding;
    private int mAlarmTextSize;
    private int mAlarmMinDistance;

    private Paint mPaint;
    private ContentResolver mResolver;
    private SimpleDateFormat mDateFormat;
    private TreeMap<Date, AlarmTimeNode> mAlarmTimes = new TreeMap<Date, AlarmTimeNode>();
    private Calendar mCalendar;
    private AlarmObserver mAlarmObserver = new AlarmObserver(getHandler());
    private GetAlarmsTask mAlarmsTask = new GetAlarmsTask();
    private String mNoAlarmsScheduled;
    private boolean mIsAnimatingOut;

    private class AlarmObserver extends ContentObserver {

        public AlarmObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean changed) {
            if (mAlarmsTask != null) {
                mAlarmsTask.cancel(true);
            }
            mAlarmsTask = new GetAlarmsTask();
            mAlarmsTask.execute();
        }

        @Override
        public void onChange(boolean changed, Uri uri) {
            onChange(changed);
        }
    }

    private class AlarmTimeNode {
        public Date date;
        public boolean isRepeating;

        public AlarmTimeNode(Date date, boolean isRepeating) {
            this.date = date;
            this.isRepeating = isRepeating;
        }
    }

    private class GetAlarmsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected synchronized Void doInBackground(Void... params) {
            List<Alarm> enabledAlarmList = Alarm.getAlarms(mResolver, Alarm.ENABLED + "=1");
            final Date currentTime = mCalendar.getTime();
            mAlarmTimes.clear();
            for (Alarm alarm : enabledAlarmList) {
                int hour = alarm.hour;
                int minutes = alarm.minutes;
                HashSet<Integer> repeatingDays = alarm.daysOfWeek.getSetDays();

                if (repeatingDays.isEmpty()) {
                    mCalendar.add(Calendar.DATE, getDaysFromNow(hour, minutes));
                    mCalendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
                    mCalendar.set(Calendar.MINUTE, alarm.minutes);
                    Date date = mCalendar.getTime();

                    if (!mAlarmTimes.containsKey(date)) {

                        mAlarmTimes.put(date, new AlarmTimeNode(date, false));
                    }
                    mCalendar.setTime(currentTime);
                    continue;
                }


                for (int day : alarm.daysOfWeek.getSetDays()) {
                    mCalendar.add(Calendar.DATE, getDaysFromNow(day, hour, minutes));
                    mCalendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
                    mCalendar.set(Calendar.MINUTE, alarm.minutes);
                    Date date = mCalendar.getTime();

                    if (!mAlarmTimes.containsKey(date)) {

                        mAlarmTimes.put(date, new AlarmTimeNode(mCalendar.getTime(), true));
                    } else {

                        mAlarmTimes.get(date).isRepeating = true;
                    }
                    mCalendar.setTime(currentTime);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            requestLayout();
            AlarmTimelineView.this.invalidate();
        }

        private int getDaysFromNow(int hour, int minutes) {
            final int currentHour = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hour > currentHour ||
                    (hour == currentHour && minutes >= mCalendar.get(Calendar.MINUTE)) ) {
                return 0;
            }
            return 1;
        }

        private int getDaysFromNow(int day, int hour, int minute) {
            final int currentDay = mCalendar.get(Calendar.DAY_OF_WEEK);
            if (day != currentDay) {
                if (day < currentDay) {
                    day += DAYS_IN_WEEK;
                }
                return day - currentDay;
            }

            final int currentHour = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hour != currentHour) {
                return (hour < currentHour) ? DAYS_IN_WEEK : 0;
            }

            final int currentMinute = mCalendar.get(Calendar.MINUTE);
            return (minute < currentMinute) ? DAYS_IN_WEEK : 0;
        }
    }

    public AlarmTimelineView(Context context) {
        super(context);
        init(context);
    }

    public AlarmTimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mResolver = context.getContentResolver();

        final Resources res = context.getResources();

        mAlarmTimelineColor = res.getColor(R.color.alarm_timeline_color);
        mAlarmTimelineLength = res.getDimensionPixelOffset(R.dimen.alarm_timeline_length);
        mAlarmTimelineMarginTop = res.getDimensionPixelOffset(R.dimen.alarm_timeline_margin_top);
        mAlarmTimelineMarginBottom = res.getDimensionPixelOffset(R.dimen.footer_button_size) +
                2 * res.getDimensionPixelOffset(R.dimen.footer_button_layout_margin);
        mAlarmNodeRadius = res.getDimensionPixelOffset(R.dimen.alarm_timeline_radius);
        mAlarmNodeInnerRadius = res.getDimensionPixelOffset(R.dimen.alarm_timeline_inner_radius);
        mAlarmNodeInnerRadiusColor = res.getColor(R.color.primary);
        mAlarmTextSize = res.getDimensionPixelOffset(R.dimen.alarm_text_font_size);
        mAlarmTextPadding = res.getDimensionPixelOffset(R.dimen.alarm_text_padding);
        mAlarmMinDistance = res.getDimensionPixelOffset(R.dimen.alarm_min_distance) +
                2 * mAlarmNodeRadius;
        mNoAlarmsScheduled = context.getString(R.string.no_upcoming_alarms);

        mPaint = new Paint();
        mPaint.setTextSize(mAlarmTextSize);
        mPaint.setStrokeWidth(res.getDimensionPixelOffset(R.dimen.alarm_timeline_width));
        mPaint.setAntiAlias(true);

        mCalendar = Calendar.getInstance();
        final Locale locale = Locale.getDefault();
        String formatString = DateFormat.is24HourFormat(context) ? FORMAT_24_HOUR : FORMAT_12_HOUR;
        mDateFormat = new SimpleDateFormat(formatString, locale);

        mAlarmsTask.execute();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mResolver.registerContentObserver(Alarm.CONTENT_URI, true, mAlarmObserver);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mResolver.unregisterContentObserver(mAlarmObserver);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int timelineHeight = !mAlarmTimes.isEmpty() ?  mAlarmTimelineLength : 0;
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                timelineHeight + mAlarmTimelineMarginTop + mAlarmTimelineMarginBottom);
    }

    @Override
    public synchronized void onDraw(Canvas canvas) {

        if (mIsAnimatingOut) {
            return;
        }

        super.onDraw(canvas);

        final int x = getWidth() / 2;
        int y = mAlarmTimelineMarginTop;

        mPaint.setColor(mAlarmTimelineColor);
        if (mAlarmTimes == null || mAlarmTimes.isEmpty()) {
            mPaint.setTextAlign(Align.CENTER);
            canvas.drawText(mNoAlarmsScheduled, x, y, mPaint);
            return;
        }

        canvas.drawLine(x, y, x, y + mAlarmTimelineLength, mPaint);

        final int xLeft = x - mAlarmNodeRadius - mAlarmTextPadding;
        final int xRight = x + mAlarmNodeRadius + mAlarmTextPadding;

        Iterator<AlarmTimeNode> iter = mAlarmTimes.values().iterator();
        Date firstDate = null;
        int prevY = 0;
        int i=0;
        final int maxY = mAlarmTimelineLength + mAlarmTimelineMarginTop;
        while (iter.hasNext()) {
            AlarmTimeNode node = iter.next();
            Date date = node.date;

            if (firstDate == null) {
                y = mAlarmTimelineMarginTop;
                firstDate = date;
            } else {
                y = Math.max(convertToDistance(date, firstDate), prevY + mAlarmMinDistance);
            }

            if (y > maxY) {
                y = maxY;
            }

            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, mAlarmNodeRadius, mPaint);

            if (!node.isRepeating) {
                mPaint.setColor(mAlarmNodeInnerRadiusColor);
                canvas.drawCircle(x, y, mAlarmNodeInnerRadius, mPaint);
            }
            prevY = y;

            final String timeString = mDateFormat.format(date).toUpperCase();
            mPaint.setColor(mAlarmTimelineColor);
            if (i % 2 == 0) {
                mPaint.setTextAlign(Align.RIGHT);
                canvas.drawText(timeString, xLeft, y + mAlarmTextSize / 3, mPaint);
            } else {
                mPaint.setTextAlign(Align.LEFT);
                canvas.drawText(timeString, xRight, y + mAlarmTextSize / 3, mPaint);
            }
            i++;
        }
    }

    public void setIsAnimatingOut(boolean animatingOut) {
        mIsAnimatingOut = animatingOut;
    }


    private int convertToDistance(final Date date, final Date firstDate) {
        if (date == null || firstDate == null) {
            return 0;
        }
        return (int) ((date.getTime() - firstDate.getTime())
                * mAlarmTimelineLength / DateUtils.WEEK_IN_MILLIS + mAlarmTimelineMarginTop);
    }
}
