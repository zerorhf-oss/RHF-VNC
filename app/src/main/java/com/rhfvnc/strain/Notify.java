package com.rhfvnc.strain;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
public class Notify {
    public static Notification build(Context c, String channel, String title, String text) {
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                NotificationChannel ch = new NotificationChannel(channel, "System", NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                NotificationManager nm = c.getSystemService(NotificationManager.class);
                if (nm != null) nm.createNotificationChannel(ch);
            } catch (Throwable ignored) {}
            n = new Notification.Builder(c, channel)
                    .setContentTitle(title).setContentText(text)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setOngoing(true).build();
        } else {
            n = new Notification.Builder(c)
                    .setContentTitle(title).setContentText(text)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setOngoing(true).build();
        }
        return n;
    }
}
