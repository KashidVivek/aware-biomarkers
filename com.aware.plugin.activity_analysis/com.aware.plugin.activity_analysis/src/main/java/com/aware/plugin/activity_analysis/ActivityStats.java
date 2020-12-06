package com.aware.plugin.activity_analysis;

import android.content.ContentResolver;

import com.aware.plugin.google.activity_recognition.Stats;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ActivityStats {

    private final ContentResolver resolver;
    private long moderateActivityTime;
    private long highActivityTime;
    private boolean userActiveInDay;

    public ActivityStats(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public void generateStats() {
        long previousDay = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000).getTime();
        long currentTime = new Date(System.currentTimeMillis()).getTime();
        moderateActivityTime = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeWalking(resolver, previousDay, currentTime));
        highActivityTime = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeBiking(resolver, previousDay, currentTime))
                + TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeRunning(resolver, previousDay, currentTime));
        userActiveInDay = moderateActivityTime > 23 || highActivityTime > 12;
    }

    public long getModerateActivityTime() {
        return moderateActivityTime;
    }

    public long getHighActivityTime() {
        return highActivityTime;
    }

    public boolean isUserActiveInDay() {
        return userActiveInDay;
    }

//    @Override
//    protected void onHandleIntent(Intent intent) {
//        Log.d("AWARE::Activity Analysis", "Started activity analysis...");
//        generateStats();
//
//        ContentValues context_data = new ContentValues();
//        context_data.put(Provider.ActivityAnalysisData.TIMESTAMP, System.currentTimeMillis());
//        context_data.put(Provider.ActivityAnalysisData.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
//        context_data.put(Provider.ActivityAnalysisData.MODERATE_ACTIVITY_TIME, moderateActivityTime);
//        context_data.put(Provider.ActivityAnalysisData.HIGH_ACTIVITY_TIME, highActivityTime);
//        context_data.put(Provider.ActivityAnalysisData.IS_USER_ACTIVE, userActiveInDay);
//
//        getContentResolver().insert(Provider.ActivityAnalysisData.CONTENT_URI, context_data);
//
//        if (Plugin.getSensorObserver() != null)
//            Plugin.getSensorObserver().onRecording(context_data);
//
//        Intent context = new Intent(Plugin.ACTION_AWARE_ACTIVITY_ANALYSIS);
//        context.putExtra(Provider.ActivityAnalysisData.MODERATE_ACTIVITY_TIME, moderateActivityTime);
//        context.putExtra(Provider.ActivityAnalysisData.HIGH_ACTIVITY_TIME, highActivityTime);
//        context.putExtra(Provider.ActivityAnalysisData.IS_USER_ACTIVE, userActiveInDay);
//        sendBroadcast(context);
//
//        Log.d("AWARE::Activity Analysis", "Finished activity analysis...");
//    }
}
