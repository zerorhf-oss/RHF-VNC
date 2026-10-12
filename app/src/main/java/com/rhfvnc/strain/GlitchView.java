package com.rhfvnc.strain;
import android.content.Context;
import android.graphics.*;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.Random;
public class GlitchView extends SurfaceView implements Runnable {
    private Thread worker;
    private volatile boolean running = true;
    private final Random rnd = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float pulse = 0f;
    private int frame = 0;
    public GlitchView(Context c) {
        super(c);
        getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder h) { start(); }
            public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
            public void surfaceDestroyed(SurfaceHolder h) { stop(); }
        });
    }
    public void start() {
        if (worker != null && worker.isAlive()) return;
        running = true;
        worker = new Thread(this);
        worker.setDaemon(true);
        worker.start();
    }
    public void stop() { running = false; }
    @Override
    public void run() {
        while (running) {
            SurfaceHolder h = getHolder();
            Canvas cv = null;
            try { cv = h.lockCanvas(); } catch (Throwable t) { continue; }
            if (cv == null) continue;
            int w = cv.getWidth();
            int hh = cv.getHeight();
            cv.drawColor(Color.BLACK);
            // scanline noise
            paint.setColor(Color.argb(80, 200, 0, 0));
            for (int y = 0; y < hh; y += 6) {
                if (rnd.nextInt(10) < 3) cv.drawRect(0, y, w, y + 2, paint);
            }
            // random red pixels
            paint.setColor(Color.argb(120, 255, 0, 0));
            for (int i = 0; i < 800; i++) {
                cv.drawPoint(rnd.nextInt(w), rnd.nextInt(hh), paint);
            }
            // glitch bars
            paint.setColor(Color.argb(150, 255, 0, 0));
            for (int i = 0; i < 4; i++) {
                int y = rnd.nextInt(hh);
                int bh = 4 + rnd.nextInt(30);
                cv.drawRect(0, y, w, y + bh, paint);
            }
            // RHF/VNC logo — pulse
            pulse = (float) (Math.sin(frame * 0.08) * 0.5 + 0.5);
            int size = (int) (110 + pulse * 60);
            paint.setColor(Color.RED);
            paint.setShadowLayer(40, 0, 0, Color.RED);
            paint.setTextSize(size);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            cv.drawText("RHF/VNC", w / 2f, hh / 2f, paint);
            // subtitle
            paint.setShadowLayer(20, 0, 0, Color.RED);
            paint.setTextSize(48);
            paint.setColor(Color.argb(200, 255, 80, 80));
            cv.drawText("STRAIN ACTIVE", w / 2f, hh / 2f + size, paint);
            paint.setShadowLayer(0, 0, 0, 0);
            try { h.unlockCanvasAndPost(cv); } catch (Throwable ignored) {}
            frame++;
            // ~30fps to save a bit of cpu for the hogs
            try { Thread.sleep(33); } catch (InterruptedException ignored) {}
        }
    }
}
