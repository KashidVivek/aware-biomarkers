package com.aware.plugin.tone_analyser;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Keyboard_Provider;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Http;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Plugin extends Aware_Plugin {
    private int minutes = 0;
    private String tone = null;
    public static final String ACTION_AWARE_PLUGIN_TONE_ANALYSER = "ACTION_AWARE_PLUGIN_TONE_ANALYSER";
    public static final String TONE ="tone";

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }
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
                ContentValues context_data = new ContentValues();
                context_data.put(Provider.ToneAnalyser_Data.TIMESTAMP, System.currentTimeMillis());
                context_data.put(Provider.ToneAnalyser_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                context_data.put(Provider.ToneAnalyser_Data.TONE, getTone());


                if (DEBUG) Log.d(TAG, context_data.toString());

                //insert data to table
                getContentResolver().insert(Provider.ToneAnalyser_Data.CONTENT_URI, context_data);

                JSONObject row = new JSONObject();

                try {
                    row.put(Provider.ToneAnalyser_Data.TIMESTAMP, System.currentTimeMillis());
                    row.put(Provider.ToneAnalyser_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    row.put(Provider.ToneAnalyser_Data.TONE, getTone());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new MyTask().execute(row);

                Intent sharedContext = new Intent(ACTION_AWARE_PLUGIN_TONE_ANALYSER);
                sharedContext.putExtra(TONE, tone);
                sendBroadcast(sharedContext);
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

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TONE_ANALYSER, true);

            /**
             * Example of how to enable accelerometer sensing and how to access the data in real-time for your app.
             * In this particular case, we are sending a broadcast that the ContextCard listens to and updates the UI in real-time.
             */
            Aware.startAWARE(this);
            Applications.isAccessibilityServiceActive(getApplicationContext());
            String selection = Keyboard_Provider.Keyboard_Data._ID + " IN (SELECT "
                    + Keyboard_Provider.Keyboard_Data._ID +"- 1"+ " FROM "
                    + "keyboard" + " WHERE "
                    + Keyboard_Provider.Keyboard_Data.BEFORE_TEXT + "=?)";
            String[] selectionArgs = new String[]{""};
            BroadcastReceiver tickReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                        minutes ++;
                        if (minutes == 4){
                            Cursor keyboardCursor = getContentResolver().query(Keyboard_Provider.Keyboard_Data.CONTENT_URI,null, selection,selectionArgs,null);
                            List<String> text = new ArrayList<String>();

                            if (keyboardCursor!=null && keyboardCursor.getCount()>0){
                                keyboardCursor.moveToFirst();
                                do{
                                    text.add(keyboardCursor.getString(keyboardCursor.getColumnIndexOrThrow(Keyboard_Provider.Keyboard_Data.CURRENT_TEXT)));
                                }while(keyboardCursor.moveToNext());
                                String textToAnalyse = text.toString().replace("[","").replace("]","");
                                RetrieveToneTask emotion = new RetrieveToneTask(getApplicationContext(),textToAnalyse);
                                try {
                                    setTone(emotion.execute().get());
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            CONTEXT_PRODUCER.onContext();
                            context.getContentResolver().delete(Provider.ToneAnalyser_Data.CONTENT_URI,null,null);
                            minutes = 0;
                        }
                    }
                }
            };
            registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

            //Enable our plugin's sync-adapter to upload the data to the server if part of a study
            if (Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE).length() >= 0 && !Aware.isSyncEnabled(this, Provider.getAuthority(this)) && Aware.isStudy(this) && getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone)) {
                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), Provider.getAuthority(this), 1);
                ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), true);
                ContentResolver.addPeriodicSync(
                        Aware.getAWAREAccount(this),
                        Provider.getAuthority(this),
                        Bundle.EMPTY,
                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
                );
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

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TONE_ANALYSER, false);
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
            http.dataPOST(setting + "/tone_analysis/insert", postData, false);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }
}
