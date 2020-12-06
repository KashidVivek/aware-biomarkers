package com.aware.plugin.activity_analysis;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_ACTIVITY_ANALYSIS = "status_plugin_activity_analysis";
    public static final String FREQUENCY_PLUGIN_ACTIVITY_ANALYSIS = "frequency_plugin_activity_analysis";

    //Plugin settings UI elements
    private static CheckBoxPreference status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_ACTIVITY_ANALYSIS);
        if( Aware.getSetting(this, STATUS_PLUGIN_ACTIVITY_ANALYSIS).length() == 0 ) {
            Aware.setSetting( this, STATUS_PLUGIN_ACTIVITY_ANALYSIS, true ); //by default, the setting is true on install
        }
        if (Aware.getSetting(this, FREQUENCY_PLUGIN_ACTIVITY_ANALYSIS).length() == 0) {
            Aware.setSetting(this, FREQUENCY_PLUGIN_ACTIVITY_ANALYSIS, 16);
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_ACTIVITY_ANALYSIS).equals("true"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);
        if( setting.getKey().equals(STATUS_PLUGIN_ACTIVITY_ANALYSIS) ) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }
        if (Aware.getSetting(this, STATUS_PLUGIN_ACTIVITY_ANALYSIS).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.activity_analysis");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.activity_analysis");
        }
    }
}
