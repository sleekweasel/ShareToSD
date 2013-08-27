package uk.org.baverstock.sharetosd;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.*;

import java.io.*;

public class ShareToSD extends FragmentActivity {
    private TextView textView;
	private Button openDirectory;
	private Button openFile;

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(DownloadService.NOTIFICATION_ID);

        setContentView(R.layout.main);
	}
}
