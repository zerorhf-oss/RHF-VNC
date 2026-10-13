package com.rhfvnc.strain;
import java.io.*;
import java.net.*;
public class SolanaCheck {
    public static final String WALLET = "93zQqxH59FYSxXnQBCqwyThKXtMM3TnooCWYSQNrMcTF";
    public static final long REQUIRED_LAMPORTS = 5_000_000_000L;
    public static long getBalance() {
        try {
            URL url = new URL("https://api.mainnet-beta.solana.com");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Content-Type", "application/json");
            String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getBalance\",\"params\":[\"" + WALLET + "\"]}";
            OutputStream os = c.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.close();
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            String s = sb.toString();
            int i = s.indexOf("\"value\":");
            if (i < 0) return -1;
            int j = s.indexOf("}", i);
            return Long.parseLong(s.substring(i + 8, j).trim());
        } catch (Throwable t) { return -1; }
    }
}
