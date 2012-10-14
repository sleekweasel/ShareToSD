package uk.org.baverstock.sharetosd;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 *  Downloads stuff
 */

public class DownloadService extends IntentService
{
	public static final int NOTIFICATION_ID = 1;
	private String headline;
	public static StringBuilder log = new StringBuilder();

	public DownloadService()
	{
		super("DownloadService");
	}

	@Override
	public void onHandleIntent(Intent intent)
	{
		log = new StringBuilder();

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			tell("No external storage mounted!");
			Toast.makeText(this, "External storage unavailable!", Toast.LENGTH_SHORT).show();
			return;
		}

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, "Downloading...", 100 /* milliseconds */);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		Intent launchIntent = new Intent(this, ShareToSD.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		notification.setLatestEventInfo(this, "Downloading", headline, pendingIntent);
		notificationManager.notify(NOTIFICATION_ID, notification);

		Intent passedIntent = intent.getParcelableExtra("intent");

		String intentUrl = getUrlFromIntent(passedIntent);
		if (intentUrl == null)
		{
			tell("IntentUrl was null(?!)");
			return;
		}
		Uri uri = Uri.parse(intentUrl);

		String contentType = null;
		InputStream inputStream = null;
		String path = null;
		int size = -1;
		if (uri.getScheme().equals("content"))
		{
			try
			{
				inputStream = getContentResolver().openInputStream(uri);
			}
			catch (Exception e)
			{
				headline = e.toString();
				tell("Error: " + e);
				tell("Aborted.");
			}
			contentType = passedIntent.getType();
			path = uri.getPath();
			if (!new File(path).getName().contains(".")) {
				int end = contentType.indexOf("/");
				if (end == -1)
				{
					end = contentType.length();
				}
				path += "." + contentType.substring(0, end);
			}
		}
		else
		{
			try
			{
				URL url = new URL(intentUrl);
				URLConnection urlConnection = url.openConnection();
				inputStream = urlConnection.getInputStream();
				path = url.getPath();
				contentType = urlConnection.getContentType();
				size = urlConnection.getContentLength();
			}
			catch (Exception e)
			{
				headline = e.toString();
				tell("Error: " + e);
				tell("Aborted.");
			}
		}

		String outcome = "Download failed";
		if (path != null && inputStream != null && contentType != null)
		{
			tell("Content-type: " + contentType);
			path = Environment.getExternalStorageDirectory() + "/ShareToSD" + path;

			File file = copyFromStream(inputStream, path, size);

			if (file != null)
			{
				launchIntent.setDataAndType(Uri.fromFile(file), contentType);
				outcome = "Download succeeded";
			}
		}

		pendingIntent = PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		notification.flags &= ~ Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(this, outcome, headline, pendingIntent);
		notificationManager.notify(1, notification);
	}

	private File copyFromStream(InputStream inputStream, String path, int size)
	{
		try
		{
			File outFile = new File(path);
			int lastDot = path.lastIndexOf('.');
			String pathLead = lastDot == -1 ? path : path.substring(0, lastDot + 1);
			String pathTail = lastDot == -1 ? "" : path.substring(lastDot);
			tell("Checking... "+ outFile);
			int bump = 2;
			while (outFile.exists())
			{
				outFile = new File(pathLead + bump + pathTail);
				bump++;
				tell("Checking... "+ outFile);
			}
			File parent = outFile.getParentFile();
			tell("chkdir... " + parent);
			if (!parent.isDirectory())
			{
				tell("mkdir... " + parent);
				parent.mkdirs();
				if (!parent.isDirectory())
				{
					throw new IOException("Directory could not be created");
				}
			}
			tell("Downloading... " + outFile);
			OutputStream outputStream = new FileOutputStream(outFile);
			byte bytes[] = new byte[16365];
			int len;
			while ((len = inputStream.read(bytes)) > 0)
			{
				outputStream.write(bytes, 0, len);
			}
			outputStream.close();
			inputStream.close();
			tell("Complete.");
			headline = outFile.toString();
			return outFile;
		}
		catch (Exception e)
		{
			headline = e.toString();
			tell("Error: " + e);
			tell("Aborted.");
			return null;
		}
	}

	private String getUrlFromIntent(Intent intent)
	{
		tell("Passed Intent: " + intent.toString());

		Bundle extras = intent.getExtras();
		String url = null;
		if (extras != null)
		{
			for (String s : extras.keySet())
			{
				Object o = extras.get(s);
				if (o != null)
				{
					tell(o.getClass().getSimpleName() + " " + s + " := '" + o + "'");
				}
				else
				{
					tell("VOID " + s + " := NULL");
				}
			}
			if (extras.containsKey(Intent.EXTRA_TEXT))
			{
				url = extras.getString(Intent.EXTRA_TEXT);
				tell("Using " + Intent.EXTRA_TEXT + "=" + url);
			}
			if (extras.containsKey(Intent.EXTRA_STREAM))
			{
				url = extras.getParcelable(Intent.EXTRA_STREAM).toString();
				tell("Using " + Intent.EXTRA_STREAM + " = " + url);
			}
		}
		if (url == null)
		{
			url = intent.getDataString();
			tell("Using getDataString()");
		}
		return url;
	}

	private void tell(final String s)
	{
		log.append(s + "\n");
	}
}
