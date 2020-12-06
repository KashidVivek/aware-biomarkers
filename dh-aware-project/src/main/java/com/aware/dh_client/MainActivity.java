package com.aware.dh_client;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.google.activity_recognition.Stats;
import com.aware.providers.Aware_Provider;
import com.aware.providers.Keyboard_Provider;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Http;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import static com.aware.plugin.noise_analysis.Provider.DB_TBL_NOISE_ANALYSIS_FIELDS;

public class MainActivity extends AppCompatActivity {

    private static SharedPreferences preferences;

    private static final ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();

    public static boolean allowed;
    public Button button;

    private TextView frequency;
    private TextView decibels;
    private TextView ambient_noise;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("com.aware.dh_client", Context.MODE_PRIVATE);

//        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_WEBSERVICE, false);

        REQUIRED_PERMISSIONS.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.WRITE_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.READ_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.READ_SYNC_STATS);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);

        Applications.isAccessibilityServiceActive(getApplicationContext());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            REQUIRED_PERMISSIONS.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        boolean PERMISSIONS_OK = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                PERMISSIONS_OK = false;
                break;
            }
        }
        if (PERMISSIONS_OK) {
            Intent aware = new Intent(this, Aware.class);
            startService(aware);
        }

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener((View.OnClickListener) v -> {
            long previousDay = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000).getTime();
            long currentTime = new Date(System.currentTimeMillis()).getTime();
            TextView text = (TextView) findViewById(R.id.stillTime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeStill(getApplicationContext().getContentResolver(), previousDay, currentTime));
            text.setText(String.format("Still time: %d minute(s)", minutes));

            text = (TextView) findViewById(R.id.bikeTime);
            minutes = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeBiking(getApplicationContext().getContentResolver(), previousDay, currentTime));
            text.setText(String.format("Bike time: %d minute(s)", minutes));

            text = (TextView) findViewById(R.id.VehicleTime);
            minutes = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeVehicle(getApplicationContext().getContentResolver(), previousDay, currentTime));
            text.setText(String.format("Vehicle time: %d minute(s)", minutes));

            text = (TextView) findViewById(R.id.TimeWalking);
            minutes = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeWalking(getApplicationContext().getContentResolver(), previousDay, currentTime));
            text.setText(String.format("Walking time: %d minute(s)", minutes));

            Toast.makeText(getApplicationContext(), "Syncing data...", Toast.LENGTH_SHORT).show();
            Intent sync = new Intent(Aware.ACTION_AWARE_SYNC_DATA);
            sendBroadcast(sync);
        });

        frequency = (TextView) findViewById(R.id.AudioFreq);
        decibels = (TextView) findViewById(R.id.AudioDecibel);
        ambient_noise = (TextView) findViewById(R.id.AudioNoise);

    }


    @Override
    protected void onResume() {
        super.onResume();

        allowed = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                allowed = false;
                break;
            }
        }

        if (!allowed) {
            Log.d(Aware.TAG, "Requesting permissions...");
            Intent permissionsHandler = new Intent(this, PermissionsHandler.class);
            permissionsHandler.putStringArrayListExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissionsHandler.putExtra(PermissionsHandler.EXTRA_REDIRECT_ACTIVITY, getPackageName() + "/" + getClass().getName());
            permissionsHandler.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissionsHandler);

        } else {
            if (preferences.getAll().isEmpty() && Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0) {
                PreferenceManager.setDefaultValues(getApplicationContext(), "com.aware.dh_client", Context.MODE_PRIVATE, com.aware.R.xml.aware_preferences, true);
                preferences.edit().apply();
            } else {
                PreferenceManager.setDefaultValues(getApplicationContext(), "com.aware.dh_client", Context.MODE_PRIVATE, R.xml.aware_preferences, false);
            }

            Map<String, ?> defaults = preferences.getAll();
            for (Map.Entry<String, ?> entry : defaults.entrySet()) {
                if (Aware.getSetting(getApplicationContext(), entry.getKey(), "com.aware.dh_client").length() == 0) {
                    Aware.setSetting(getApplicationContext(), entry.getKey(), entry.getValue(), "com.aware.dh_client"); //default AWARE settings
                }
            }

            if (Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0) {
                UUID uuid = UUID.randomUUID();
                Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID, uuid.toString(), "com.aware.dh_client");
            }

            try {
                PackageInfo awareInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_ACTIVITIES);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.AWARE_VERSION, awareInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE, 10);
            Aware.startScreen(getApplicationContext());
            Aware.startCommunication(getApplicationContext());
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");
            Aware.startPlugin(getApplicationContext(),"com.aware.plugin.device_usage");
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.activity_analysis");
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.tone_analyser");
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.alarm_map");
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.ambient_noise");
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.noise_analysis");

            Cursor cursorJoined = getApplicationContext().getContentResolver().query(Aware_Provider.Aware_Studies.CONTENT_URI,
                    new String[]{Aware_Provider.Aware_Studies.STUDY_JOINED}, null, null, null);

            if (cursorJoined != null && cursorJoined.getCount() == 0) {
                Aware.joinStudy(getApplicationContext(), "http://167.71.59.111:8080/index.php/1/4lph4num3ric");
                //new CreateTableTask().execute();
            }

            Aware.startAWARE(getApplicationContext());
            Aware.setSetting(this, Aware_Preferences.STATUS_KEYBOARD,true);
            Aware.setSetting(getApplicationContext(),Aware_Preferences.STATUS_COMMUNICATION_EVENTS,true);
            Aware.setSetting(getApplicationContext(),Aware_Preferences.STATUS_MESSAGES,true);
            Aware.setSetting(getApplicationContext(),Aware_Preferences.STATUS_CALLS,true);
            Aware.setSetting(getApplicationContext(),Aware_Preferences.STATUS_SCREEN,true);
            Cursor keyboardCursor = getContentResolver().query(Keyboard_Provider.Keyboard_Data.CONTENT_URI,null, null, null,null);

        }
    }

    private final AmbientNoiseUpdater audioUpdater = new AmbientNoiseUpdater();

    public class AmbientNoiseUpdater extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ContentValues data = intent.getParcelableExtra("data");
            frequency.setText(String.format("Sound frequency: %.1f Hz", data.getAsDouble(AmbientNoise_Data.FREQUENCY)));
            decibels.setText(String.format("Sound Decibels: %.1f dB", data.getAsDouble(AmbientNoise_Data.DECIBELS)));
            ambient_noise.setText(data.getAsBoolean(AmbientNoise_Data.IS_SILENT) ? "Silent" : "Noisy");
        }
    }

    private class CreateTableTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... rows) {

            if (rows == null) {
                return null;
            }

            String deviceID = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);

            Hashtable<String, String> postData = new Hashtable<>();
            postData.put(Aware_Preferences.DEVICE_ID, deviceID);

            postData.put("fields", com.aware.plugin.activity_analysis.Provider.DB_TBL_ACTIVITY_ANALYSIS_FIELDS);

            Http http = new Http();
            String setting = Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER);

            http.dataPOST(setting + "/plugin_activity_analysis/create", postData, false);

            postData = new Hashtable<>();
            postData.put(Aware_Preferences.DEVICE_ID, deviceID);

            postData.put("fields", com.aware.plugin.tone_analyser.Provider.DB_TBL_TEMPLATE);

            http = new Http();

            http.dataPOST(setting + "/tone_analysis/create", postData, false);

            postData = new Hashtable<>();
            postData.put(Aware_Preferences.DEVICE_ID, deviceID);

            postData.put("fields", com.aware.plugin.tone_analyser.Provider.DB_TBL_TEMPLATE);

            http = new Http();

            http.dataPOST(setting + "/plugin_alarm_map/create", postData, false);

            postData = new Hashtable<>();
            postData.put(Aware_Preferences.DEVICE_ID, deviceID);

            postData.put("fields", DB_TBL_NOISE_ANALYSIS_FIELDS);

            http = new Http();

            http.dataPOST(setting + "/plugin_noise_analysis/create", postData, false);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }
}
