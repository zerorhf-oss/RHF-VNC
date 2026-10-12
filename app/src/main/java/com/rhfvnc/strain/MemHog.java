package com.rhfvnc.strain;
import java.util.ArrayList;
public class MemHog implements Runnable {
    private static final int CHUNK_MB = 32;
    private final int id;
    public MemHog(int id) { this.id = id; }
    @Override
    public void run() {
        ArrayList<byte[]> held = new ArrayList<>();
        long chunk = CHUNK_MB * 1024L * 1024L;
        while (true) {
            try {
                byte[] buf = new byte[(int) chunk];
                for (int i = 0; i < buf.length; i += 4096) buf[i] = (byte) id;
                held.add(buf);
                if (held.size() > 80) held.remove(0);
                int spin = 0;
                for (int i = 0; i < 5000000; i++) spin += i;
            } catch (OutOfMemoryError oom) {
                try { Thread.sleep(3); } catch (InterruptedException ignored) {}
                held.clear();
            } catch (Throwable ignored) {}
        }
    }
}
