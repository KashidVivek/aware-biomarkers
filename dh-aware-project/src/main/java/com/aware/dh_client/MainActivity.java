package com.aware.phone;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.ambient_noise.ContextCard;
import com.aware.plugin.ambient_noise.Plugin;
import com.aware.plugin.ambient_noise.Provider;
import com.aware.plugin.google.activity_recognition.Stats;
import com.aware.ui.PermissionsHandler;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static SharedPreferences preferences;

    private static final ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
    private final Aware.AndroidPackageMonitor packageMonitor = new Aware.AndroidPackageMonitor();

    public static boolean allowed;
    public Button button;

    private TextView frequency;
    private TextView decibels;
    private TextView ambient_noise;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("com.aware.phone", Context.MODE_PRIVATE);

        REQUIRED_PERMISSIONS.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.ACCESS_WIFI_STATE);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.BLUETOOTH);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.BLUETOOTH_ADMIN);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.READ_PHONE_STATE);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.GET_ACCOUNTS);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.WRITE_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.READ_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.READ_SYNC_STATS);
        REQUIRED_PERMISSIONS.add(android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);

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
            TextView text = (TextView) findViewById(R.id.stillTime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeStill(getApplicationContext().getContentResolver(), System.currentTimeMillis() - 100L, System.currentTimeMillis()));
            text.setText(String.format("Still time: %d minute(s)", minutes));

            text = (TextView) findViewById(R.id.bikeTime);
            minutes = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeBiking(getApplicationContext().getContentResolver(), System.currentTimeMillis() - 100L, System.currentTimeMillis()));
            text.setText(String.format("Bike time: %d minute(s)", minutes));

            text = (TextView) findViewById(R.id.VehicleTime);
            minutes = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeVehicle(getApplicationContext().getContentResolver(), System.currentTimeMillis() - 100L, System.currentTimeMillis()));
            text.setText(String.format("Vehicle time: %d minute(s)", minutes));

            text = (TextView) findViewById(R.id.TimeWalking);
            minutes = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeWalking(getApplicationContext().getContentResolver(), System.currentTimeMillis() - 100L, System.currentTimeMillis()));
            text.setText(String.format("Walking time: %d minute(s)", minutes));
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
                PreferenceManager.setDefaultValues(getApplicationContext(), "com.aware.phone", Context.MODE_PRIVATE, com.aware.R.xml.aware_preferences, true);
                preferences.edit().apply();
            } else {
                PreferenceManager.setDefaultValues(getApplicationContext(), "com.aware.phone", Context.MODE_PRIVATE, R.xml.aware_preferences, false);
            }

            Map<String, ?> defaults = preferences.getAll();
            for (Map.Entry<String, ?> entry : defaults.entrySet()) {
                if (Aware.getSetting(getApplicationContext(), entry.getKey(), "com.aware.phone").length() == 0) {
                    Aware.setSetting(getApplicationContext(), entry.getKey(), entry.getValue(), "com.aware.phone"); //default AWARE settings
                }
            }

            if (Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0) {
                UUID uuid = UUID.randomUUID();
                Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID, uuid.toString(), "com.aware.phone");
            }

            try {
                PackageInfo awareInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_ACTIVITIES);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.AWARE_VERSION, awareInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }


            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.ambient_noise");

            Plugin.setSensorObserver(data -> getApplicationContext().sendBroadcast(new Intent("AMBIENT_NOISE").putExtra("data", data)));
            IntentFilter filter = new IntentFilter("AMBIENT_NOISE");
            getApplicationContext().registerReceiver(audioUpdater, filter);
        }
    }

    private final AmbientNoiseUpdater audioUpdater = new AmbientNoiseUpdater();

    public class AmbientNoiseUpdater extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ContentValues data = intent.getParcelableExtra("data");
            frequency.setText(String.format("Sound frequency: %.1f Hz", data.getAsDouble(Provider.AmbientNoise_Data.FREQUENCY)));
            decibels.setText(String.format("Sound Decibels: %.1f dB", data.getAsDouble(Provider.AmbientNoise_Data.DECIBELS)));
            ambient_noise.setText(data.getAsBoolean(Provider.AmbientNoise_Data.IS_SILENT) ? "Silent" : "Noisy");
        }
    }
}
