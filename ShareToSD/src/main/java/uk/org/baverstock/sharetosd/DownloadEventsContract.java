package uk.org.baverstock.sharetosd;

import android.net.Uri;

public class DownloadEventsContract {
    public static final String EVENT_KEY_ID = "_id";
    public static final String EVENT_KEY_URL = "url";
    public static final String EVENT_KEY_TIMESTAMP_QUA_TIMESTAMP = "timestamp";
    public static final String EVENT_KEY_TIMESTAMP_QUA_MILLIS = "timestamp_ms";
    public static final String EVENT_KEY_TIMESTAMP_INTO_DATE_MILLIS =
            "(strftime('%s', " + EVENT_KEY_TIMESTAMP_QUA_TIMESTAMP + ") * 1000) AS " + EVENT_KEY_TIMESTAMP_QUA_MILLIS;
    public static final String EVENT_KEY_FILE = "file";
    public static final String EVENT_KEY_TYPE = "type";
    public static final String EVENT_TABLE = "event";

    public static final String AUTHORITY = "uk.org.baverstock.ShareToSD.provider";
    public static final Uri CONTENT_URI = Uri.EMPTY.buildUpon().scheme("content").authority(AUTHORITY).build();
    public static final String EVENT_PATH = EVENT_TABLE;

    public static final Uri EVENT_CONTENT_URL = CONTENT_URI.buildUpon().appendPath(EVENT_PATH).build();
    static final String EVENT_SINGLE_MIME_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".event";
    static final String EVENT_MULTIPLE_MIME_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".event";
    public static final String EVENT_DEFAULT_SORT_ORDER = EVENT_KEY_TIMESTAMP_QUA_TIMESTAMP + " DESC";
}
