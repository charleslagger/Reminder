package com.embeddedlog.LightUpDroid;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.embeddedlog.LightUpDroid.alarms.AlarmStateManager;
import com.embeddedlog.LightUpDroid.provider.Alarm;
import com.embeddedlog.LightUpDroid.stopwatch.StopwatchFragment;
import com.embeddedlog.LightUpDroid.stopwatch.StopwatchService;
import com.embeddedlog.LightUpDroid.stopwatch.Stopwatches;
import com.embeddedlog.LightUpDroid.timer.TimerFragment;
import com.embeddedlog.LightUpDroid.timer.TimerObj;
import com.embeddedlog.LightUpDroid.timer.Timers;
import com.embeddedlog.LightUpDroid.worldclock.CitiesActivity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

public class DeskClock extends Activity implements LabelDialogFragment.TimerLabelDialogHandler,
            LabelDialogFragment.AlarmLabelDialogHandler{
    private static final boolean DEBUG = false;

    private static final String LOG_TAG = "DeskClock";

    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final String KEY_CLOCK_STATE = "clock_state";

    public static final String SELECT_TAB_INTENT_EXTRA = "LightUpDroid.select.tab";


    private ActionBar mActionBar;
    private Tab mAlarmTab;
    private Tab mClockTab;
    private Tab mTimerTab;
    private Tab mStopwatchTab;
    private Menu mMenu;

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    public static final int ALARM_TAB_INDEX = 0;
    public static final int CLOCK_TAB_INDEX = 1;
    public static final int TIMER_TAB_INDEX = 2;
    public static final int STOPWATCH_TAB_INDEX = 3;

    public static final int RTL_ALARM_TAB_INDEX = 3;
    public static final int RTL_CLOCK_TAB_INDEX = 2;
    public static final int RTL_TIMER_TAB_INDEX = 1;
    public static final int RTL_STOPWATCH_TAB_INDEX = 0;

    private int mSelectedTab;

    final Handler mHandler = new Handler();

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        if (DEBUG) Log.d(LOG_TAG, "onNewIntent with intent: " + newIntent);

        setIntent(newIntent);

        int tab = newIntent.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
        if (tab != -1) {
            if (mActionBar != null) {
                mActionBar.setSelectedNavigationItem(tab);
            }
        }
    }

    private void initViews() {
        if (mTabsAdapter == null) {
            mViewPager = new ViewPager(this);
            mViewPager.setId(R.id.desk_clock_pager);
            mViewPager.setOffscreenPageLimit(2);
            mTabsAdapter = new TabsAdapter(this, mViewPager);
            createTabs(mSelectedTab);
        }
        setContentView(mViewPager);
        mActionBar.setSelectedNavigationItem(mSelectedTab);
    }

    private void createTabs(int selectedIndex) {
        mActionBar = getActionBar();

        if (mActionBar != null) {
            mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            mAlarmTab = mActionBar.newTab();
            mAlarmTab.setIcon(R.drawable.clock_tab);
            mAlarmTab.setContentDescription(R.string.menu_alarm);
            mTabsAdapter.addTab(mAlarmTab, AlarmClockFragment.class, ALARM_TAB_INDEX);

            mClockTab = mActionBar.newTab();
            mClockTab.setIcon(R.drawable.alarm_tab);
            mClockTab.setContentDescription(R.string.menu_clock);
            mTabsAdapter.addTab(mClockTab, ClockFragment.class, CLOCK_TAB_INDEX);

            mTimerTab = mActionBar.newTab();
            mTimerTab.setIcon(R.drawable.timer_tab);
            mTimerTab.setContentDescription(R.string.menu_timer);
            mTabsAdapter.addTab(mTimerTab, TimerFragment.class, TIMER_TAB_INDEX);

            mStopwatchTab = mActionBar.newTab();
            mStopwatchTab.setIcon(R.drawable.stopwatch_tab);
            mStopwatchTab.setContentDescription(R.string.menu_stopwatch);
            mTabsAdapter.addTab(mStopwatchTab, StopwatchFragment.class,STOPWATCH_TAB_INDEX);

            mActionBar.setSelectedNavigationItem(selectedIndex);
            mTabsAdapter.notifySelectedPage(selectedIndex);

            forceTabsInActionBar(mActionBar);
        }
    }

    public void forceTabsInActionBar(final ActionBar actionBar) {
        try {
            final Method setHasEmbeddedTabsMethod =
                    actionBar.getClass().getDeclaredMethod("setHasEmbeddedTabs", boolean.class);
            setHasEmbeddedTabsMethod.setAccessible(true);
            setHasEmbeddedTabsMethod.invoke(actionBar, true);
        }
        catch(final Exception e) {
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration config) {
        super.onConfigurationChanged(config);
        if (config.orientation==Configuration.ORIENTATION_PORTRAIT) {
            forceTabsInActionBar(getActionBar());
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mSelectedTab = ALARM_TAB_INDEX;
        if (icicle != null) {
            mSelectedTab = icicle.getInt(KEY_SELECTED_TAB, ALARM_TAB_INDEX);
        }
        Intent i = getIntent();
        if (i != null) {
            int tab = i.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
            if (tab != -1) {
                mSelectedTab = tab;
            }
        }
        initViews();
        setHomeTimeZone();

        AlarmStateManager.updateNextAlarm(this);

        String correctString = "android:switcher:" + mViewPager.getId() + ":" + ALARM_TAB_INDEX;
    }

    @Override
    protected void onResume() {
        super.onResume();


        Intent stopwatchIntent = new Intent(getApplicationContext(), StopwatchService.class);
        stopwatchIntent.setAction(Stopwatches.KILL_NOTIF);
        startService(stopwatchIntent);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Timers.NOTIF_APP_OPEN, true);
        editor.apply();
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_IN_USE_CANCEL);
        sendBroadcast(timerIntent);

    }

    @Override
    public void onPause() {
        Intent intent = new Intent(getApplicationContext(), StopwatchService.class);
        intent.setAction(Stopwatches.SHOW_NOTIF);
        startService(intent);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Timers.NOTIF_APP_OPEN, false);
        editor.apply();
        Utils.showInUseNotifications(this);

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, mActionBar.getSelectedNavigationIndex());
    }

    public void clockButtonsOnClick(View v) {
        if (v == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.cities_button:
                startActivity(new Intent(this, CitiesActivity.class));
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        mMenu = menu;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mActionBar.getSelectedNavigationIndex() == ALARM_TAB_INDEX ||
                    mActionBar.getSelectedNavigationIndex() == CLOCK_TAB_INDEX) {

                menu.clear();
                getMenuInflater().inflate(R.menu.desk_clock_menu, menu);
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenu(menu);
        return true;
    }

    private void updateMenu(Menu menu) {

        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }

        MenuItem nightMode = menu.findItem(R.id.menu_item_night_mode);
        if (mActionBar.getSelectedNavigationIndex() == CLOCK_TAB_INDEX) {
            nightMode.setVisible(true);
        } else {
            nightMode.setVisible(false);
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (processMenuClick(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean processMenuClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                startActivity(new Intent(DeskClock.this, SettingsActivity.class));
                return true;
            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i != null) {
                    try {
                        startActivity(i);
                    } catch (ActivityNotFoundException e) {

                    }
                }
                return true;
            case R.id.menu_item_night_mode:
                startActivity(new Intent(DeskClock.this, ScreensaverActivity.class));
//
            default:
                break;
        }
        return true;
    }


    private void setHomeTimeZone() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String homeTimeZone = prefs.getString(SettingsActivity.KEY_HOME_TZ, "");
        if (!homeTimeZone.isEmpty()) {
            return;
        }
        homeTimeZone = TimeZone.getDefault().getID();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SettingsActivity.KEY_HOME_TZ, homeTimeZone);
        editor.apply();

    }

    public void registerPageChangedListener(DeskClockFragment frag) {
        if (mTabsAdapter != null) {
            mTabsAdapter.registerPageChangedListener(frag);
        }
    }

    public void unregisterPageChangedListener(DeskClockFragment frag) {
        if (mTabsAdapter != null) {
            mTabsAdapter.unregisterPageChangedListener(frag);
        }
    }

    private class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

        private static final String KEY_TAB_POSITION = "tab_position";

        final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, int position) {
                clss = _class;
                args = new Bundle();
                args.putInt(KEY_TAB_POSITION, position);
            }

            public int getPosition() {
                return args.getInt(KEY_TAB_POSITION, 0);
            }
        }

        private final ArrayList<TabInfo> mTabs = new ArrayList <TabInfo>();
        ActionBar mMainActionBar;
        Context mContext;
        ViewPager mPager;
        HashSet<String> mFragmentTags = new HashSet<String>();

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            mContext = activity;
            mMainActionBar = activity.getActionBar();
            mPager = pager;
            mPager.setAdapter(this);
            mPager.setOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int position) {
            final String name = makeFragmentName(R.id.desk_clock_pager, position);
            Fragment fragment = getFragmentManager().findFragmentByTag(name);
            if (fragment == null) {
                TabInfo info = mTabs.get(getRtlPosition(position));
                fragment = Fragment.instantiate(mContext, info.clss.getName(), info.args);
            }
            return fragment;
        }

        private String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, int position) {
            TabInfo info = new TabInfo(clss, position);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mMainActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {

            mMainActionBar.setSelectedNavigationItem(getRtlPosition(position));
            notifyPageChanged(position);

            if (mMenu != null) {
                if (position == ALARM_TAB_INDEX || position == CLOCK_TAB_INDEX) {
                    mMenu.setGroupVisible(R.id.menu_items, true);
                    onCreateOptionsMenu(mMenu);
                } else {
                    mMenu.setGroupVisible(R.id.menu_items, false);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

        @Override
        public void onTabReselected(Tab arg0, FragmentTransaction arg1) {

        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            TabInfo info = (TabInfo)tab.getTag();
            int position = info.getPosition();
            mPager.setCurrentItem(getRtlPosition(position));
        }

        @Override
        public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {

        }

        public void notifySelectedPage(int page) {
            notifyPageChanged(page);
        }

        private void notifyPageChanged(int newPage) {
            for (String tag : mFragmentTags) {
                final FragmentManager fm = getFragmentManager();
                DeskClockFragment f = (DeskClockFragment) fm.findFragmentByTag(tag);
                if (f != null) {
                    f.onPageChanged(newPage);
                }
            }
        }

        public void registerPageChangedListener(DeskClockFragment frag) {
            String tag = frag.getTag();
            if (mFragmentTags.contains(tag)) {
                Log.wtf(LOG_TAG, "Lỗi " + tag);
            } else {
                mFragmentTags.add(frag.getTag());
            }
            frag.onPageChanged(mMainActionBar.getSelectedNavigationIndex());
        }

        public void unregisterPageChangedListener(DeskClockFragment frag) {
            mFragmentTags.remove(frag.getTag());
        }

        private boolean isRtl() {
            if (Build.VERSION.SDK_INT >= 17) {
                return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                        View.LAYOUT_DIRECTION_RTL;
            } else {
                return true;
            }
        }

        private int getRtlPosition(int position) {
            if (isRtl()) {
                switch (position) {
                    case TIMER_TAB_INDEX:
                        return RTL_TIMER_TAB_INDEX;
                    case CLOCK_TAB_INDEX:
                        return RTL_CLOCK_TAB_INDEX;
                    case STOPWATCH_TAB_INDEX:
                        return RTL_STOPWATCH_TAB_INDEX;
                    case ALARM_TAB_INDEX:
                        return RTL_ALARM_TAB_INDEX;
                    default:
                        break;
                }
            }
            return position;
        }
    }

    public static abstract class OnTapListener implements OnTouchListener {
        private float mLastTouchX;
        private float mLastTouchY;
        private long mLastTouchTime;
        private final TextView mMakePressedTextView;
        private final int mPressedColor, mGrayColor;
        private final float MAX_MOVEMENT_ALLOWED = 20;
        private final long MAX_TIME_ALLOWED = 500;

        public OnTapListener(Activity activity, TextView makePressedView) {
            mMakePressedTextView = makePressedView;
            mPressedColor = activity.getResources().getColor(Utils.getPressedColorId());
            mGrayColor = activity.getResources().getColor(Utils.getGrayColorId());
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    mLastTouchTime = Utils.getTimeNow();
                    mLastTouchX = e.getX();
                    mLastTouchY = e.getY();
                    if (mMakePressedTextView != null) {
                        mMakePressedTextView.setTextColor(mPressedColor);
                    }
                    break;
                case (MotionEvent.ACTION_UP):
                    float xDiff = Math.abs(e.getX() - mLastTouchX);
                    float yDiff = Math.abs(e.getY() - mLastTouchY);
                    long timeDiff = (Utils.getTimeNow() - mLastTouchTime);
                    if (xDiff < MAX_MOVEMENT_ALLOWED && yDiff < MAX_MOVEMENT_ALLOWED
                            && timeDiff < MAX_TIME_ALLOWED) {
                        if (mMakePressedTextView != null) {
                            v = mMakePressedTextView;
                        }
                        processClick(v);
                        resetValues();
                        return true;
                    }
                    resetValues();
                    break;
                case (MotionEvent.ACTION_MOVE):
                    xDiff = Math.abs(e.getX() - mLastTouchX);
                    yDiff = Math.abs(e.getY() - mLastTouchY);
                    if (xDiff >= MAX_MOVEMENT_ALLOWED || yDiff >= MAX_MOVEMENT_ALLOWED) {
                        resetValues();
                    }
                    break;
                default:
                    resetValues();
            }
            return false;
        }

        private void resetValues() {
            mLastTouchX = -1 * MAX_MOVEMENT_ALLOWED + 1;
            mLastTouchY = -1 * MAX_MOVEMENT_ALLOWED + 1;
            mLastTouchTime = -1 * MAX_TIME_ALLOWED + 1;
            if (mMakePressedTextView != null) {
                mMakePressedTextView.setTextColor(mGrayColor);
            }
        }

        protected abstract void processClick(View v);
    }


    @Override
    public void onDialogLabelSet(TimerObj timer, String label, String tag) {
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof TimerFragment) {
            ((TimerFragment) frag).setLabel(timer, label);
        }
    }

    @Override
    public void onDialogLabelSet(Alarm alarm, String label, String tag) {
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof AlarmClockFragment) {
            ((AlarmClockFragment) frag).setLabel(alarm, label);
        }
    }

    public int getSelectedTab() {
        return mSelectedTab;
    }

}
