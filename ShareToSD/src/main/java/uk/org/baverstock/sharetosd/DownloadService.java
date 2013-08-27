package uk.org.baverstock.sharetosd;

import android.app.*;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

/**
 *  Downloads stuff in the background.
 */

public class DownloadService extends IntentService
{
	public static final int NOTIFICATION_ID = 1;
	public static StringBuilder log = new StringBuilder();
    private NotificationManager notificationManager;

    private Notifier notifier;
    private PersistentNotification peristentNotification;
    private String headline;

    public DownloadService()
	{
		super("DownloadService");
	}

    @Override
    public void onDestroy() {
        // Make sure our notification is gone.
        peristentNotification.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        peristentNotification = new PersistentNotification(notificationManager, this);
        notifier = new Notifier(notificationManager, peristentNotification);
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

		Intent passedIntent = intent.getParcelableExtra("intent");
        if (passedIntent == null)
        {
            tell("passedIntent was null(?!)");
            return;
        }

		String intentUrl = getUrlFromIntent(passedIntent);
		if (intentUrl == null)
		{
			tell("IntentUrl was null(?!)");
			return;
		}

		Uri uri = Uri.parse(intentUrl);

		notifier.notifyStart(this, "Downloading...", null);

		String contentType = null;
		InputStream inputStream = null;
		String path = null;
		long size = -1;
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
        ContentValues contentValues = new ContentValues();
        if (path != null && inputStream != null && contentType != null)
		{
            tell("Content-type: " + contentType);
            path = Environment.getExternalStorageDirectory() + "/ShareToSD" + path;

            File file = copyFromStream(inputStream, path, size);

            if (file != null)
			{
                notifier.setDataAndType(file, contentType);
                outcome = "Download succeeded";
            }
            contentValues.put(DownloadEventsContract.EVENT_KEY_FILE, file.toString());
        }
        contentValues.put(DownloadEventsContract.EVENT_KEY_DATE, new Date().toString());
        contentValues.put(DownloadEventsContract.EVENT_KEY_URL, uri.toString());
        contentValues.put(DownloadEventsContract.EVENT_KEY_TYPE, contentType);

        getContentResolver().insert(DownloadEventsContract.EVENT_CONTENT_URL, contentValues);
		notifier.notifyLast(outcome, this, headline);
	}

	private File copyFromStream(InputStream inputStream, String path, long size)
	{
		try
		{
            path = path.replace(":", "%3c");
            path = path.replace(" ", "+");
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
			long seen = 0;
			long lastDisplayed = System.currentTimeMillis();
			while ((len = inputStream.read(bytes)) > 0)
			{
				outputStream.write(bytes, 0, len);
				seen += len;
                long now = System.currentTimeMillis();
                if (size > -1 && lastDisplayed < now - 5000)
				{
					lastDisplayed = now;
					notifier.notifyUpdate(this, seen, size, headline);
				}
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
		log.append("* " + s + "\n");
	}
}
