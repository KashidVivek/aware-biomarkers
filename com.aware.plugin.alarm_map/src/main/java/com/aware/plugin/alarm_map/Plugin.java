package com.aware.plugin.alarm_map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Http;
import com.aware.utils.Scheduler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.PriorityQueue;

import static com.aware.plugin.alarm_map.Settings.FREQUENCY_PLUGIN_ALARM_MAP;

public class Plugin extends Aware_Plugin {

    public static String ACTION_AWARE_ALARM_MAP = "ACTION_AWARE_ALARM_MAP";
    public static String ACTION_AWARE_ALARM_MAP_NOTE_LOCATION = "ACTION_AWARE_ALARM_MAP_NOTE_LOCATION";
    public static final String SCHEDULER_PLUGIN_ACTIVITY_ANALYSIS = "SCHEDULER_PLUGIN_ALARM_MAP";
    public static final String SCHEDULER_PLUGIN_ALARM_MAP_NOTE_LOCATION = "SCHEDULER_PLUGIN_ALARM_MAP_NOTE_LOCATION";

    @Override
    public void onCreate() {
        super.onCreate();
        Places.initialize(getApplicationContext(), "AIzaSyCMCcZoyzMIENIzzP2WAB1Fhi6CbbFkIk8");

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

        if (Aware.getSetting(this, FREQUENCY_PLUGIN_ALARM_MAP).length() == 0) {
            Aware.setSetting(this, FREQUENCY_PLUGIN_ALARM_MAP, 60);
        }

        if (PERMISSIONS_OK) {

            if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ACTION_AWARE_ALARM_MAP)) {
                Log.d("AWARE::Alarm Map", "Started Alarm Map...");

                Cursor context_data = getContentResolver().query(Provider.ActivityAnalysisData.CONTENT_URI, null, null, null, null);

                JSONArray rows = new JSONArray();
                try {
                    if (context_data != null && context_data.moveToFirst()) {
                        do {
                            JSONObject row = new JSONObject();
                            String[] columns = context_data.getColumnNames();
                            for (String c_name : columns) {
                                if (c_name.equals("_id")) continue;
                                if (c_name.equals("timestamp")) {
                                    row.put(c_name, context_data.getDouble(context_data.getColumnIndex(c_name)));
                                } else if (c_name.contains("integer")) {
                                    row.put(c_name, context_data.getInt(context_data.getColumnIndex(c_name)));
                                } else {
                                    String str = "";
                                    if (!context_data.isNull(context_data.getColumnIndex(c_name))) {
                                        str = context_data.getString(context_data.getColumnIndex(c_name));
                                    }
                                    row.put(c_name, str);
                                }
                            }
                            rows.put(row);
                        } while (context_data.moveToNext());
                        new MyTask().execute(rows);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (context_data != null) {
                    context_data.close();
                }

                getContentResolver().delete(Provider.ActivityAnalysisData.CONTENT_URI, null, null);

                Log.d("AWARE::Alarm Map", "Done Alarm Map...");
            } else if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ACTION_AWARE_ALARM_MAP_NOTE_LOCATION)) {
                Log.d("AWARE::Alarm Map", "Started Alarm Map note location...");

                //google maps code
                PlacesClient client = Places.createClient(this);

                List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
                        Place.Field.LAT_LNG, Place.Field.TYPES);

                // Use the builder to create a FindCurrentPlaceRequest.
                FindCurrentPlaceRequest request =
                        FindCurrentPlaceRequest.newInstance(placeFields);

                @SuppressWarnings("MissingPermission") final Task<FindCurrentPlaceResponse> placeResult =
                        client.findCurrentPlace(request);

                placeResult.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            FindCurrentPlaceResponse likelyPlaces = task.getResult();
                            PriorityQueue<PlaceLikelihood> queue = new PriorityQueue<>(new Comparator<PlaceLikelihood>() {
                                @Override
                                public int compare(PlaceLikelihood o1, PlaceLikelihood o2) {
                                    return (int) (o1.getLikelihood() - o2.getLikelihood());
                                }
                            });
                            queue.addAll(likelyPlaces.getPlaceLikelihoods());
                            PlaceLikelihood peek = queue.peek();
                            List<Place.Type> types = peek.getPlace().getTypes();
                            if (types != null) {
                                boolean containsBarType = types.contains(Place.Type.BAR);
                                boolean containsLiquorType = types.contains(Place.Type.LIQUOR_STORE);

                                ContentValues values = new ContentValues();

                                values.put(Provider.ActivityAnalysisData.TIMESTAMP, System.currentTimeMillis());
                                values.put(Provider.ActivityAnalysisData.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                if (containsBarType) {
                                    values.put(Provider.ActivityAnalysisData.TYPE_OF_PLACE, Place.Type.BAR.name());
                                } else {
                                    values.put(Provider.ActivityAnalysisData.TYPE_OF_PLACE, Place.Type.LIQUOR_STORE.name());
                                }
                                values.put(Provider.ActivityAnalysisData.NO_OF_VISITS, 1);
                                if (containsBarType || containsLiquorType) {
                                    getContentResolver().update(Provider.ActivityAnalysisData.CONTENT_URI, values, null, null);
                                }
                            }
                        } else {
                            Log.e(TAG, "Exception: %s", task.getException());
                        }
                    }
                });

                Log.d("AWARE::Alarm Map", "Done Alarm Map note location...");
            } else {

                try {
                    Scheduler.Schedule contacts_sync = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_ACTIVITY_ANALYSIS);
                    if (contacts_sync == null || contacts_sync.getInterval() != 60) {
                        contacts_sync = new Scheduler.Schedule(SCHEDULER_PLUGIN_ACTIVITY_ANALYSIS);
                        contacts_sync.setInterval(60);
//                        contacts_sync.setInterval(1);
                        contacts_sync.setActionType(Scheduler.ACTION_TYPE_SERVICE);
                        contacts_sync.setActionIntentAction(ACTION_AWARE_ALARM_MAP);
                        contacts_sync.setActionClass(getPackageName() + "/" + Plugin.class.getName());
                        Scheduler.saveSchedule(this, contacts_sync);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    Scheduler.Schedule contacts_sync = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_ALARM_MAP_NOTE_LOCATION);
                    if (contacts_sync == null || contacts_sync.getInterval() != 5) {
                        contacts_sync = new Scheduler.Schedule(SCHEDULER_PLUGIN_ALARM_MAP_NOTE_LOCATION);
                        contacts_sync.setInterval(5);
//                        contacts_sync.setInterval(1);
                        contacts_sync.setActionType(Scheduler.ACTION_TYPE_SERVICE);
                        contacts_sync.setActionIntentAction(ACTION_AWARE_ALARM_MAP_NOTE_LOCATION);
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

        Aware.setSetting(this, Settings.STATUS_PLUGIN_ALARM_MAP, false);
    }

    private class MyTask extends AsyncTask<JSONArray, Void, Void> {
        @Override
        protected Void doInBackground(JSONArray... rows) {

            if (rows == null) {
                return null;
            }

            String deviceID = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);

            Http http = new Http();
            Hashtable<String, String> postData = new Hashtable<>();
            postData.put(Aware_Preferences.DEVICE_ID, deviceID);

            postData.put("data", rows[0].toString());
            String setting = Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER);
            http.dataPOST(setting + "/plugin_alarm_map/insert", postData, false);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }
}
