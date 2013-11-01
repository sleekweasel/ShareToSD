package uk.org.baverstock.sharetosd;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static uk.org.baverstock.sharetosd.DownloadEventsContract.*;

public class DownloadEventsOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NAME = "DownloadEvents";
    private static final String EVENT_TABLE_CREATE =
            "CREATE TABLE " + EVENT_TABLE + " (" +
                    EVENT_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EVENT_KEY_URL + " TEXT, " +
                    EVENT_KEY_TYPE + " TEXT, " +
                    EVENT_KEY_TIMESTAMP_QUA_TIMESTAMP + " TIMESTAMP NOT NULL DEFAULT current_timestamp, " +
                    EVENT_KEY_FILE + " TEXT);";

    DownloadEventsOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(EVENT_TABLE_CREATE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(this.getClass().getSimpleName(), "Dropping table due to " + oldVersion + "<" + newVersion);
        db.execSQL("drop table if exists " + EVENT_TABLE);
        onCreate(db);
    }
}
