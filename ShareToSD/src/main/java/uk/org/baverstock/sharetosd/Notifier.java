package uk.org.baverstock.sharetosd;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

class Notifier {
    private NotificationManager notificationManager;
    private PersistentNotification peristentNotification;
    private Notification notification;
    private Intent launchIntent;
    private PendingIntent pendingIntent;

    Notifier(NotificationManager notificationManager, PersistentNotification peristentNotification) {
        this.notificationManager = notificationManager;
        this.peristentNotification = peristentNotification;
    }

    public void notifyStart(Context context, String s, CharSequence headline) {
        notification = new Notification(R.drawable.icon, "Downloading...", 10 /* milliseconds */);
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE;
        launchIntent = new Intent(context, ShareToSD.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingIntent = PendingIntent.getActivity(context, 1, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setLatestEventInfo(context, "Downloading", headline, pendingIntent);
        peristentNotification.startForegroundCompat(DownloadService.NOTIFICATION_ID, notification);
    }

    public void notifyUpdate(Context context, long seen, long want, CharSequence headline) {
        long pct = 100 * seen / want;
        notification.tickerText = "Downloaded " + pct + "% (" + seen + "/" + want + ")";
        notification.setLatestEventInfo(context, notification.tickerText, headline, pendingIntent);
        notificationManager.notify(DownloadService.NOTIFICATION_ID, notification);
    }

    public void setDataAndType(File file, String contentType) {
        launchIntent.setDataAndType(Uri.fromFile(file), contentType);
    }

    public void notifyLast(String outcome, DownloadService downloadService, CharSequence headline) {
        pendingIntent = PendingIntent.getActivity(downloadService, 1, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.flags &= ~(Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE);
        notification.tickerText = outcome;
        notification.setLatestEventInfo(downloadService, notification.tickerText, headline, pendingIntent);
        peristentNotification.stopForegroundCompat(DownloadService.NOTIFICATION_ID);
        notificationManager.notify(DownloadService.NOTIFICATION_ID, notification);
    }
}
