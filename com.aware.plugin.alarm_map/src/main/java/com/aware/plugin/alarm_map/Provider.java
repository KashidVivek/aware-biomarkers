package com.aware.plugin.alarm_map;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.provider.plugin_alarm_map"; //change to package.provider.your_plugin_name

    public static final int DATABASE_VERSION = 1; //increase this if you make changes to the database structure, i.e., rename columns, etc.
    public static final String DATABASE_NAME = "plugin_alarm_map.db"; //the database filename, use plugin_xxx for plugins.

    //Add here your database table names, as many as you need
    public static final String DB_TBL_ALARM_MAP = "plugin_alarm_map";

    //For each table, add two indexes: DIR and ITEM. The index needs to always increment. Next one is 3, and so on.
    private static final int ALARM_MAP_DIR = 1;
    private static final int ALARM_MAP_ITEM = 2;

    //Put tables names in this array so AWARE knows what you have on the database
    public static final String[] DATABASE_TABLES = {
            DB_TBL_ALARM_MAP
    };

    //These are columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    /**
     * Create one of these per database table
     * In this example, we are adding example columns
     */
    public static final class ActivityAnalysisData implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_ALARM_MAP);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.plugin.alarm_map.provider.plugin_alarm_map"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.plugin.alarm_map.provider.plugin_alarm_map"; //modify me

        //Note: integers and strings don't need a type prefix_
        public static final String TYPE_OF_PLACE = "type_of_place";
        public static final String NO_OF_VISITS = "no_of_visits";
    }

    //Define each database table fields
    public static final String DB_TBL_ALARM_MAP_FIELDS =
            ActivityAnalysisData._ID + " integer primary key autoincrement," +
                    ActivityAnalysisData.TIMESTAMP + " real default 0," +
                    ActivityAnalysisData.DEVICE_ID + " text default ''," +
                    ActivityAnalysisData.TYPE_OF_PLACE + " text default ''," +
                    ActivityAnalysisData.NO_OF_VISITS + " integer default 0";

    /**
     * Share the fields with AWARE so we can replicate the table schema on the server
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_ALARM_MAP_FIELDS
    };

    //Helper variables for ContentProvider - DO NOT CHANGE
    private UriMatcher sUriMatcher;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;

    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }
    //--

    //For each table, create a hashmap needed for database queries
    private HashMap<String, String> activityAnalysis;

    /**
     * Returns the provider authority that is dynamic
     *
     * @return
     */
    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".provider.plugin_alarm_map";
        return AUTHORITY;
    }

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getAuthority(getContext());

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //For each table, add indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], ALARM_MAP_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", ALARM_MAP_ITEM);

        //Create each table hashmap so Android knows how to insert data to the database. Put ALL table fields.
        activityAnalysis = new HashMap<>();
        activityAnalysis.put(ActivityAnalysisData._ID, ActivityAnalysisData._ID);
        activityAnalysis.put(ActivityAnalysisData.TIMESTAMP, ActivityAnalysisData.TIMESTAMP);
        activityAnalysis.put(ActivityAnalysisData.DEVICE_ID, ActivityAnalysisData.DEVICE_ID);
        activityAnalysis.put(ActivityAnalysisData.TYPE_OF_PLACE, ActivityAnalysisData.TYPE_OF_PLACE);
        activityAnalysis.put(ActivityAnalysisData.NO_OF_VISITS, ActivityAnalysisData.NO_OF_VISITS);

        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case, increasing the index accordingly
            case ALARM_MAP_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        initialiseDatabase();

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case ALARM_MAP_DIR:
                long _id = database.insert(DATABASE_TABLES[0], ActivityAnalysisData.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(ActivityAnalysisData.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            //Add all tables' DIR entries, with the right table index
            case ALARM_MAP_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(activityAnalysis); //the hashmap of the table
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            //Add each table indexes DIR and ITEM
            case ALARM_MAP_DIR:
                return ActivityAnalysisData.CONTENT_TYPE;
            case ALARM_MAP_ITEM:
                return ActivityAnalysisData.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public int incrementCount(Uri uri, ContentValues values, String type) {

        initialiseDatabase();

        database.beginTransaction();

        int count = 0;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case ALARM_MAP_DIR:
                String selection = ActivityAnalysisData.TYPE_OF_PLACE + " = '" + type + "'";
                Cursor activity_raw = query(ActivityAnalysisData.CONTENT_URI, null, selection, null, null);
                if (activity_raw != null && activity_raw.moveToFirst()) {
                    database.execSQL("UPDATE " + DB_TBL_ALARM_MAP +
                            " SET " + ActivityAnalysisData.NO_OF_VISITS + " = " + ActivityAnalysisData.NO_OF_VISITS + " + 1" +
                            " WHERE " + ActivityAnalysisData.TYPE_OF_PLACE + " = '" + type + "'");
                } else {
                    insert(uri, values);
                }
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        initialiseDatabase();

        database.beginTransaction();

        int count = 0;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case ALARM_MAP_DIR:

                String type = values.getAsString(ActivityAnalysisData.TYPE_OF_PLACE);
                String selection1 = ActivityAnalysisData.TYPE_OF_PLACE + " = '" + type + "'";
                Cursor activity_raw = query(ActivityAnalysisData.CONTENT_URI, null, selection1, null, null);
                if (activity_raw != null && activity_raw.moveToFirst()) {
                    database.execSQL("UPDATE " + DB_TBL_ALARM_MAP +
                            " SET " + ActivityAnalysisData.NO_OF_VISITS + " = " + ActivityAnalysisData.NO_OF_VISITS + " + 1" +
                            " WHERE " + ActivityAnalysisData.TYPE_OF_PLACE + " = '" + type + "'");
                } else {
                    insert(uri, values);
                }
                break;
//                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
//                break;
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
}
