package com.aware.plugin.ambient_noise;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Http;
import com.aware.utils.Scheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Hashtable;

public class Plugin extends Aware_Plugin {

    public static final String SCHEDULER_PLUGIN_AMBIENT_NOISE = "SCHEDULER_PLUGIN_AMBIENT_NOISE";

    @Override
    public void onCreate() {
        super.onCreate();

        AUTHORITY = Provider.getAuthority(this);
        TAG = "AWARE::Ambient Noise";
        REQUIRED_PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            if (Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE, 1);
            }
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_SAMPLE_SIZE).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_SAMPLE_SIZE, 30);
            }
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_SILENCE_THRESHOLD).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_SILENCE_THRESHOLD, 50);
            }
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_NO_RAW).isEmpty()) {
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_AMBIENT_NOISE_NO_RAW, true); //disables raw audio recording by default
            }

            if (intent != null && intent.getAction() != null) {
                Log.d("AWARE::Noise Analysis", "Started Noise analysis...");

                NoiseStats stats = new NoiseStats(getContentResolver());
                stats.generateStats();

                JSONObject row = new JSONObject();
                try {
                    row.put(Provider.AmbientNoise_Data.TIMESTAMP, System.currentTimeMillis());
                    row.put(Provider.AmbientNoise_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    row.put(Provider.AmbientNoise_Data.LOWNOISETIME, stats.getlowNoiseTime());
                    row.put(Provider.AmbientNoise_Data.NORMALNOISETIME, stats.getnormalNoiseTime());
                    row.put(Provider.AmbientNoise_Data.HIGHNOISETIME, stats.gethighNoiseTime());
                    row.put(Provider.AmbientNoise_Data.ISHARMFULINDAG, stats.isharmfulInDay() ? 1 : 0);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                JSONArray rows = new JSONArray();
                rows.put(row);
                new MyTask().execute(row);

                Log.d("AWARE::Noise Analysis", "Done Noise analysis...");
            } else {

                try {
                    Scheduler.Schedule contacts_sync = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_AMBIENT_NOISE);
                    if (contacts_sync == null || contacts_sync.getInterval() != 1) {
                        contacts_sync = new Scheduler.Schedule(SCHEDULER_PLUGIN_AMBIENT_NOISE);
                        contacts_sync.setInterval(10);
//                        contacts_sync.setInterval(1);
                        contacts_sync.setActionType(Scheduler.ACTION_TYPE_SERVICE);
                        contacts_sync.setActionClass(getPackageName() + "/" + Plugin.class.getName());
                        Scheduler.saveSchedule(this, contacts_sync);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Provider.getAuthority(this),
                Bundle.EMPTY
        );

        Scheduler.removeSchedule(this, SCHEDULER_PLUGIN_AMBIENT_NOISE);
        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIENT_NOISE, false);
    }

    private static AWARESensorObserver awareSensor;

    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }

    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }

    public interface AWARESensorObserver {
        void onRecording(ContentValues data);
    }

    private class MyTask extends AsyncTask<JSONObject, Void, Void> {
        @Override
        protected Void doInBackground(JSONObject... rows) {

            if (rows == null) {
                return null;
            }

            String deviceID = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);

            Http http = new Http();
            Hashtable<String, String> postData = new Hashtable<>();
            postData.put(Aware_Preferences.DEVICE_ID, deviceID);

            postData.put("data", Arrays.toString(rows));
            String setting = Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER);
            http.dataPOST(setting + "/plugin_ambient_noise_analysis/insert", postData, false);
            return null;
        }
    }
}
