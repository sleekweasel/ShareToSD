package uk.org.baverstock.sharetosd;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
	private Notifier notifier = new Notifier();

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

		notifier.notifyStart(this, "Downloading...");

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
		}

		notifier.notifyLast(outcome, this);
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
					notifier.notifyUpdate(this, seen, size);
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

	private class Notifier
	{

		private NotificationManager notificationManager;
		private Notification notification;
		private Intent launchIntent;
		private PendingIntent pendingIntent;

		public void notifyStart(Context context, String s)
		{
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notification = new Notification(R.drawable.icon, "Downloading...", 10 /* milliseconds */);
			notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE;
			launchIntent = new Intent(context, ShareToSD.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			pendingIntent = PendingIntent.getActivity(context, 1, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			notification.setLatestEventInfo(context, "Downloading", headline, pendingIntent);
			startForegroundCompat(NOTIFICATION_ID, notification);
		}

		public void notifyUpdate(Context context, long seen, long want)
		{
			long pct = 100 * seen / want;
            notification.tickerText = "Downloaded " + pct + "% (" + seen + "/" + want + ")";
            notification.setLatestEventInfo(context, notification.tickerText, headline, pendingIntent);
			mNM.notify(NOTIFICATION_ID, notification);
		}

		public void setDataAndType(File file, String contentType)
		{
			launchIntent.setDataAndType(Uri.fromFile(file), contentType);
		}

		private void notifyLast(String outcome, DownloadService downloadService)
		{
			pendingIntent = PendingIntent.getActivity(downloadService, 1, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			notification.flags &= ~ (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE);
            notification.tickerText = outcome;
			notification.setLatestEventInfo(downloadService, notification.tickerText, headline, pendingIntent);
			stopForegroundCompat(NOTIFICATION_ID);
			mNM.notify(NOTIFICATION_ID, notification);
		}
	}


	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Class<?>[] mSetForegroundSignature = new Class[] {
			boolean.class};
	private static final Class<?>[] mStartForegroundSignature = new Class[] {
			int.class, Notification.class};
	private static final Class<?>[] mStopForegroundSignature = new Class[] {
			boolean.class};

	private NotificationManager mNM;
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	void invokeMethod(Method method, Object[] args) {
		try {
			method.invoke(this, args);
		} catch (InvocationTargetException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		} catch (IllegalAccessException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		}
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			invokeMethod(mStartForeground, mStartForegroundArgs);
			return;
		}

		// Fall back on the old API.
		mSetForegroundArgs[0] = Boolean.TRUE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
		mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			invokeMethod(mStopForeground, mStopForegroundArgs);
			return;
		}

		// Fall back on the old API.  Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		mNM.cancel(id);
		mSetForegroundArgs[0] = Boolean.FALSE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
			return;
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
		try {
			mSetForeground = getClass().getMethod("setForeground",
					mSetForegroundSignature);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(
					"OS doesn't have Service.startForeground OR Service.setForeground!");
		}
	}

	@Override
	public void onDestroy() {
		// Make sure our notification is gone.
		stopForegroundCompat(R.string.foreground_service_started);
		super.onDestroy();
	}
}
