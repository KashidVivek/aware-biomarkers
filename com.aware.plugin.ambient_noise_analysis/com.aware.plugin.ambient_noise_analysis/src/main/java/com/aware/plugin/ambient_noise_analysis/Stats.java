package com.aware.plugin.ambient_noise;

import android.content.ContentResolver;
import android.database.Cursor;
public class Stats {
    public static long getTimeLow(ContentResolver resolver, long timestamp_start, long timestamp_end) {
        long total_time_still = 0;

        String selection = Provider.AmbientNoise_Data.TIMESTAMP + " between " + timestamp_start + " AND " + timestamp_end;
        Cursor activity_raw = resolver.query(Provider.AmbientNoise_Data.CONTENT_URI, null, selection, null, Provider.AmbientNoise_Data.TIMESTAMP + " ASC");
        if( activity_raw != null && activity_raw.moveToFirst() ) {
            int last_activity = activity_raw.getInt(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.ANALYSIS_RESULT));
            long last_activity_timestamp = activity_raw.getLong(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.TIMESTAMP));

            while(activity_raw.moveToNext()) {
                int activity = activity_raw.getInt(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.ANALYSIS_RESULT));
                long activity_timestamp = activity_raw.getLong(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.TIMESTAMP));

                if( activity ==Integer.parseInt(Provider.AmbientNoise_Data.ANALYSIS_RESULT) && last_activity ==Integer.parseInt(Provider.AmbientNoise_Data.ANALYSIS_RESULT) ) { //continuing still
                    total_time_still += activity_timestamp-last_activity_timestamp;
                }

                last_activity = activity;
                last_activity_timestamp = activity_timestamp;
            }
        }
        if( activity_raw != null && ! activity_raw.isClosed() ) activity_raw.close();
        return total_time_still;
    }

    public static long getTimeNormal(ContentResolver resolver, long timestamp_start, long timestamp_end) {
        long total_time_still = 0;

        String selection = Provider.AmbientNoise_Data.TIMESTAMP + " between " + timestamp_start + " AND " + timestamp_end;
        Cursor activity_raw = resolver.query(Provider.AmbientNoise_Data.CONTENT_URI, null, selection, null, Provider.AmbientNoise_Data.TIMESTAMP + " ASC");
        if( activity_raw != null && activity_raw.moveToFirst() ) {
            int last_activity = activity_raw.getInt(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.ANALYSIS_RESULT));
            long last_activity_timestamp = activity_raw.getLong(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.TIMESTAMP));

            while(activity_raw.moveToNext()) {
                int activity = activity_raw.getInt(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.ANALYSIS_RESULT));
                long activity_timestamp = activity_raw.getLong(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.TIMESTAMP));

                if( activity ==Integer.parseInt(Provider.AmbientNoise_Data.ANALYSIS_RESULT) && last_activity ==Integer.parseInt(Provider.AmbientNoise_Data.ANALYSIS_RESULT) ) { //continuing still
                    total_time_still += activity_timestamp-last_activity_timestamp;
                }

                last_activity = activity;
                last_activity_timestamp = activity_timestamp;
            }
        }
        if( activity_raw != null && ! activity_raw.isClosed() ) activity_raw.close();
        return total_time_still;
    }

    public static long getTimeHigh(ContentResolver resolver, long timestamp_start, long timestamp_end) {
        long total_time_still = 0;

        String selection = Provider.AmbientNoise_Data.TIMESTAMP + " between " + timestamp_start + " AND " + timestamp_end;
        Cursor activity_raw = resolver.query(Provider.AmbientNoise_Data.CONTENT_URI, null, selection, null, Provider.AmbientNoise_Data.TIMESTAMP + " ASC");
        if( activity_raw != null && activity_raw.moveToFirst() ) {
            int last_activity = activity_raw.getInt(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.ANALYSIS_RESULT));
            long last_activity_timestamp = activity_raw.getLong(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.TIMESTAMP));

            while(activity_raw.moveToNext()) {
                int activity = activity_raw.getInt(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.ANALYSIS_RESULT));
                long activity_timestamp = activity_raw.getLong(activity_raw.getColumnIndex(Provider.AmbientNoise_Data.TIMESTAMP));

                if( activity ==Integer.parseInt(Provider.AmbientNoise_Data.ANALYSIS_RESULT) && last_activity ==Integer.parseInt(Provider.AmbientNoise_Data.ANALYSIS_RESULT) ) { //continuing still
                    total_time_still += activity_timestamp-last_activity_timestamp;
                }

                last_activity = activity;
                last_activity_timestamp = activity_timestamp;
            }
        }
        if( activity_raw != null && ! activity_raw.isClosed() ) activity_raw.close();
        return total_time_still;
    }
}
