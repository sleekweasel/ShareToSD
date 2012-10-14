package uk.org.baverstock.sharetosd;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ShareToSD extends Activity
{
	private TextView textView;
	private Button openDirectory;
	private Button openFile;
	private ScrollView textScroll;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		textScroll = (ScrollView) findViewById(R.id.log_scroller);
		textView = (TextView) findViewById(R.id.log);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(DownloadService.NOTIFICATION_ID);

		textView.setText(DownloadService.log.toString());

		Uri data = getIntent().getData();

		if (data == null)
		{
			return;
		}

		textView.append("\ndataString " + data);

		final File file = new File(data.getPath());

		openDirectory = (Button) findViewById(R.id.open_directory);
		openDirectory.setText("Open " + file.getParent());
		openDirectory.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view)
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(file.getParentFile()), "resource/folder");
				startActivity(intent);
			}
		});
		openFile = (Button) findViewById(R.id.open_file);
		openFile.setText("Open " + file.getName());
		openFile.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(file), getIntent().getType());
				startActivity(intent);
			}
		});
	}
}

