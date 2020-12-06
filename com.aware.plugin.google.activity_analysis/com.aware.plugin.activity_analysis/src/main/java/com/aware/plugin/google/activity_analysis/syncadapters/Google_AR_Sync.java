package com.aware.plugin.google.activity_analysis.syncadapters;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import com.aware.plugin.google.activity_analysis.Google_AR_Provider;
import com.aware.syncadapters.AwareSyncAdapter;

public class Google_AR_Sync extends Service {
    private AwareSyncAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new AwareSyncAdapter(getApplicationContext(), true, true);
                sSyncAdapter.init(
                        Google_AR_Provider.DATABASE_TABLES, Google_AR_Provider.TABLES_FIELDS,
                        new Uri[]{
                                Google_AR_Provider.ActivityAnalysisData.CONTENT_URI
                        }
                );
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
