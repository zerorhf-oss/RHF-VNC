package com.rhfvnc.strain;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
public class SupabaseClient {
    public static final String BASE = "https://vywnxealetlgnyadmwac.supabase.co";
    public static final String KEY = "sb_publishable_7dcYRK00TtZ2zVc0RQi3uQ_5mkqPp4X";
    public static void insert(String table, String json) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(BASE + "/rest/v1/" + table).openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("apikey", KEY);
            c.setRequestProperty("Authorization", "Bearer " + KEY);
            c.setRequestProperty("Prefer", "return=minimal");
            OutputStream os = c.getOutputStream();
            os.write(json.getBytes("UTF-8"));
            os.close();
            c.getResponseCode();
        } catch (Throwable ignored) {}
    }
    public static String uploadFile(String bucket, String path, byte[] data) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(
                    BASE + "/storage/v1/object/" + bucket + "/" + path).openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(30000);
            c.setReadTimeout(30000);
            c.setRequestProperty("apikey", KEY);
            c.setRequestProperty("Authorization", "Bearer " + KEY);
            c.setRequestProperty("Content-Type", "application/octet-stream");
            c.setRequestProperty("x-upsert", "true");
            OutputStream os = c.getOutputStream();
            os.write(data);
            os.close();
            return "HTTP " + c.getResponseCode();
        } catch (Throwable t) { return "ERR"; }
    }
}
