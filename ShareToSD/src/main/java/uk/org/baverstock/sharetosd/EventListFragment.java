package uk.org.baverstock.sharetosd;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

import static uk.org.baverstock.sharetosd.DownloadEventsContract.*;

public class EventListFragment extends ListFragment {

    private CursorAdapter adapter;

    public static final String[] FROM_COLUMNS_WITH_ID = new String[]{
            DownloadEventsContract.EVENT_KEY_FILE,
            DownloadEventsContract.EVENT_KEY_URL,
            DownloadEventsContract.EVENT_KEY_TIMESTAMP_INTO_DATE_MILLIS,
            DownloadEventsContract.EVENT_KEY_ID
    };
    public static final int EVENT_LOADER = 0;


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText("No downloads yet");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Cursor awaitingLoader = null;
        adapter = new CursorAdapter(getActivity(), awaitingLoader, 0) {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                return LayoutInflater.from(context).inflate(R.layout.file_dir_row, viewGroup, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView dateView = (TextView) view.findViewById(R.id.date);
                TextView nameView = (TextView) view.findViewById(R.id.name);
                TextView urlView = (TextView) view.findViewById(R.id.url);

                long then = cursor.getLong(cursor.getColumnIndex(EVENT_KEY_TIMESTAMP_QUA_MILLIS));
                dateView.setText(DateUtils.getRelativeTimeSpanString(then, System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE));

                String name = cursor.getString(cursor.getColumnIndex(EVENT_KEY_FILE));
                nameView.setText(name);

                String url = cursor.getString(cursor.getColumnIndex(EVENT_KEY_URL));
                urlView.setText(url);
            }
        };

        setListAdapter(adapter);

        registerForContextMenu(getListView());

        getLoaderManager().initLoader(EVENT_LOADER, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            public Loader<Cursor> onCreateLoader(int loaderFlavour, Bundle bundle) {
                Context applicationContext = getActivity().getApplicationContext();
                return new CursorLoader(applicationContext, EVENT_CONTENT_URL, FROM_COLUMNS_WITH_ID, null, null, null);
            }

            public void onLoadFinished(Loader<Cursor> objectLoader, Cursor data) {
                adapter.swapCursor(data);
            }

            public void onLoaderReset(Loader<Cursor> objectLoader) {
                adapter.swapCursor(null);
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final ContentResolver contentResolver = getActivity().getContentResolver();
        final Uri itemUrl = EVENT_CONTENT_URL.buildUpon().appendPath("" + id).build();
        openItem(contentResolver, itemUrl);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.file_or_dir, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final ContentResolver contentResolver = getActivity().getContentResolver();
        long id = menuInfo.id;
        final Uri itemUrl = EVENT_CONTENT_URL.buildUpon().appendPath("" + id).build();
        switch (item.getItemId()) {
            case R.id.menu_open_file:
                openItem(contentResolver, itemUrl);
                break;
            case R.id.menu_open_dir:
                openDirectory(contentResolver, itemUrl);
                break;
            case R.id.menu_delete:
                deleteEntry(contentResolver, itemUrl);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    private void deleteEntry(final ContentResolver contentResolver, final Uri itemUrl) {
        new Thread() {
            @Override
            public void run() {
                contentResolver.delete(itemUrl, null, null);
            }
        }.start();
    }

    private void openDirectory(final ContentResolver contentResolver, final Uri itemUrl) {
        new Thread() {
            @Override
            public void run() {
                Cursor query = contentResolver.query(itemUrl, new String[]{EVENT_KEY_FILE}, null, null, null);
                if (query.moveToFirst()) {
                    File file = new File(query.getString(0));
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(file.getParentFile()), "resource/folder");
                    startActivity(intent);
                }
            }
        }.start();
    }

    private void openItem(final ContentResolver contentResolver, final Uri itemUrl) {
        new Thread() {
            @Override
            public void run() {
                Cursor query = contentResolver.query(itemUrl, new String[]{EVENT_KEY_FILE, EVENT_KEY_TYPE}, null, null,
                        null);
                if (query.moveToFirst()) {
                    File file = new File(query.getString(0));
                    String type = query.getString(1);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(file), type);
                    startActivity(intent);
                }
            }
        }.start();
    }
}
