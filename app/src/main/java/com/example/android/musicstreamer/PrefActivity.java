package com.example.android.musicstreamer;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by kenm on 7/19/2015.
 */
public class PrefActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener{

    private static final String LOG_TAG = "PrefActivity";

    public interface OnPrefChangedListener {
        public void onPrefNotificationChanged(boolean notification);
        public void onPrefCountryCodeChanged(String countryCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        bindPreferenceSummaryToValue(findPreference(getString(R.string.notification_preference)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.countrycode_preference)));

    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof CheckBoxPreference) {
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
        }
        else {
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();
        Log.v(LOG_TAG, "onPreferenceChange key = " + preference.getKey());
        Log.v(LOG_TAG, "onPreferenceChange stringValue = " + stringValue);

        if (preference.getKey() == getString(R.string.notification_preference)) {
            Log.v(LOG_TAG, "onPreferenceChange notification_preference");
        }
        else if (preference.getKey() == getString(R.string.countrycode_preference)) {
            Log.v(LOG_TAG, "onPreferenceChange countrycode_preference");
        }

        preference.setSummary(stringValue);

        return true;
    }

}
