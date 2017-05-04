package com.embeddedlog.LightUpDroid;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class ScreensaverSettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    static final String KEY_CLOCK_STYLE =
            "screensaver_clock_style";
    static final String KEY_NIGHT_MODE =
            "screensaver_night_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dream_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (KEY_CLOCK_STYLE.equals(pref.getKey())) {
            final ListPreference listPref = (ListPreference) pref;
            final int idx = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[idx]);
        } else if (KEY_NIGHT_MODE.equals(pref.getKey())) {
            boolean state = ((CheckBoxPreference) pref).isChecked();
        }
        return true;
    }

    private void refresh() {
        ListPreference listPref = (ListPreference) findPreference(KEY_CLOCK_STYLE);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);

        Preference pref = findPreference(KEY_NIGHT_MODE);
        boolean state = ((CheckBoxPreference) pref).isChecked();
        pref.setOnPreferenceChangeListener(this);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }
}
