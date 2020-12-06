package com.aware.plugin.activity_analysis;

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

import static com.aware.plugin.activity_analysis.Settings.FREQUENCY_PLUGIN_ACTIVITY_ANALYSIS;

public class Plugin extends Aware_Plugin {

    public static String ACTION_AWARE_ACTIVITY_ANALYSIS = "ACTION_AWARE_ACTIVITY_ANALYSIS";
    public static final String SCHEDULER_PLUGIN_ACTIVITY_ANALYSIS = "SCHEDULER_PLUGIN_ACTIVITY_ANALYSIS";

    @Override
    public void onCreate() {
        super.onCreate();

        //This allows plugin data to be synced on demand from broadcast Aware#ACTION_AWARE_SYNC_DATA
        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE::" + getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /**
     * Allow callback to other applications when data is stored in provider
     */
    private static AWARESensorObserver awareSensor;

    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }

    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }

    public interface AWARESensorObserver {
        void onDataChanged(ContentValues data);
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (Aware.getSetting(this, FREQUENCY_PLUGIN_ACTIVITY_ANALYSIS).length() == 0) {
            Aware.setSetting(this, FREQUENCY_PLUGIN_ACTIVITY_ANALYSIS, 60);
        }

        if (PERMISSIONS_OK) {

            if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ACTION_AWARE_ACTIVITY_ANALYSIS)) {
                Log.d("AWARE::Activity Analysis", "Started activity analysis...");

                ActivityStats stats = new ActivityStats(getContentResolver());
                stats.generateStats();

                JSONObject row = new JSONObject();
                try {
                    row.put(Provider.ActivityAnalysisData.TIMESTAMP, System.currentTimeMillis());
                    row.put(Provider.ActivityAnalysisData.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    row.put(Provider.ActivityAnalysisData.MODERATE_ACTIVITY_TIME, stats.getModerateActivityTime());
                    row.put(Provider.ActivityAnalysisData.HIGH_ACTIVITY_TIME, stats.getHighActivityTime());
                    row.put(Provider.ActivityAnalysisData.IS_USER_ACTIVE, stats.isUserActiveInDay() ? 1 : 0);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                JSONArray rows = new JSONArray();
                rows.put(row);
                new MyTask().execute(row);

                Log.d("AWARE::Activity Analysis", "Done activity analysis...");
            } else {

                try {
                    Scheduler.Schedule contacts_sync = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_ACTIVITY_ANALYSIS);
                    if (contacts_sync == null || contacts_sync.getInterval() != 60) {
                        contacts_sync = new Scheduler.Schedule(SCHEDULER_PLUGIN_ACTIVITY_ANALYSIS);
                        contacts_sync.setInterval(60);
//                        contacts_sync.setInterval(1);
                        contacts_sync.setActionType(Scheduler.ACTION_TYPE_SERVICE);
                        contacts_sync.setActionIntentAction(ACTION_AWARE_ACTIVITY_ANALYSIS);
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

        Aware.setSetting(this, Settings.STATUS_PLUGIN_ACTIVITY_ANALYSIS, false);
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
            http.dataPOST(setting + "/plugin_activity_analysis/insert", postData, false);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }
}
