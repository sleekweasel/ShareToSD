package uk.org.baverstock.sharetosd;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import static uk.org.baverstock.sharetosd.DownloadEventsContentProvider.Match.EVENT_DIR;
import static uk.org.baverstock.sharetosd.DownloadEventsContentProvider.Match.EVENT_ITEM;
import static uk.org.baverstock.sharetosd.DownloadEventsContentProvider.Match.NO_MATCH;
import static uk.org.baverstock.sharetosd.DownloadEventsContract.*;

public class DownloadEventsContentProvider extends ContentProvider {

    enum Match {
        NO_MATCH(null),
        EVENT_DIR(DownloadEventsContract.EVENT_MULTIPLE_MIME_TYPE),
        EVENT_ITEM(DownloadEventsContract.EVENT_SINGLE_MIME_TYPE);

        private String mimeType;

        Match(String mimeType) {
            this.mimeType = mimeType;
        }

        String getMimeType() {
            return mimeType;
        }
    }

    static final UriMatcher matcher = new UriMatcher(NO_MATCH.ordinal());

    static {
        matcher.addURI(AUTHORITY, EVENT_PATH + "/#", EVENT_ITEM.ordinal());
        matcher.addURI(AUTHORITY, EVENT_PATH, EVENT_DIR.ordinal());
    }

    private DownloadEventsOpenHelper dbHelper;

    private SQLiteDatabase getReadableDatabase() { return dbHelper.getReadableDatabase(); }

    private SQLiteDatabase getWriteableDatabase() { return dbHelper.getWritableDatabase(); }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        dbHelper = new DownloadEventsOpenHelper(context);
        return (dbHelper == null) ? false : true;
    }

    @Override
    public String getType(Uri uri) {
        Match match = Match.values()[matcher.match(uri)];
        return match.getMimeType();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder query = new SQLiteQueryBuilder();
        switch (Match.values()[matcher.match(uri)]) {
            case EVENT_ITEM:
                query.appendWhere(EVENT_KEY_ID + "=" + uri.getLastPathSegment());
            case EVENT_DIR:
                query.setTables(EVENT_TABLE);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        String orderBy = TextUtils.isEmpty(sortOrder)
                ? EVENT_DEFAULT_SORT_ORDER
                : sortOrder;
        String groupBy = null;
        String having = null;

        Cursor c = query.query(getReadableDatabase(), projection, selection, selectionArgs, groupBy, having, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        switch (Match.values()[matcher.match(uri)]) {
            case EVENT_DIR:
                long rowId = getReadableDatabase().insert(EVENT_TABLE, null, contentValues);
                if (rowId > 0) {
                    Uri newRow = ContentUris.withAppendedId(EVENT_CONTENT_URL, rowId);
                    getContext().getContentResolver().notifyChange(newRow, null);
                    return newRow;
                }
                return null;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int count;
        switch (Match.values()[matcher.match(uri)]) {
            case EVENT_DIR:
                count = getWriteableDatabase().delete(EVENT_TABLE, where, whereArgs);
                break;
            case EVENT_ITEM:
                String whereClause = EVENT_KEY_ID + "=" + uri.getLastPathSegment()
                        + (TextUtils.isEmpty(where) ? "" : " AND (" + where + ')');
                count = getWriteableDatabase().delete(EVENT_TABLE, whereClause, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String where, String[] whereArgs) {
        int count;
        switch (Match.values()[matcher.match(uri)]) {
            case EVENT_DIR:
                count = getWriteableDatabase().update(EVENT_TABLE, contentValues, where, whereArgs);
                break;
            case EVENT_ITEM:
                String whereClause = EVENT_KEY_ID + "=" + uri.getLastPathSegment()
                        + (TextUtils.isEmpty(where) ? "" : " AND (" + where + ')');
                count = getWriteableDatabase().update(EVENT_TABLE, contentValues, whereClause, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
}
