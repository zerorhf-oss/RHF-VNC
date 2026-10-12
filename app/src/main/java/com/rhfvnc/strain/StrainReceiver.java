package com.rhfvnc.strain;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

public class StrainReceiver extends BroadcastReceiver {

    public static final String ACTION_RELAUNCH = "com.rhfvnc.strain.RELAUNCH";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String a = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || "android.intent.action.QUICKBOOT_POWERON".equals(a)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(a)
                || ACTION_RELAUNCH.equals(a)) {

            StrainService.start(ctx);
            scheduleRelaunch(ctx, 50);

            Intent i = new Intent(ctx, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            try { ctx.startActivity(i); } catch (Throwable ignored) {}
        }
    }

    public static void scheduleRelaunch(Context ctx, long ms) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent i = new Intent(ctx, StrainReceiver.class);
        i.setAction(ACTION_RELAUNCH);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1337, i, flags);
        long when = SystemClock.elapsedRealtime() + ms;

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
            } else {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
            }
        } catch (Throwable t) {
            try { am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi); } catch (Throwable ignored) {}
        }
    }
}
