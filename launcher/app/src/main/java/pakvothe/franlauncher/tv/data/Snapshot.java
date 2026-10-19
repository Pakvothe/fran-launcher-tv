package pakvothe.franlauncher.tv.data;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import pakvothe.franlauncher.tv.util.Trace;

/**
 * config.bin, cache.bin y los dos atlas en crudo (banners RGB_565, íconos ARGB_8888).
 * La lectura va en el main (es el camino crítico, ≤ 10 ms); la escritura, en el hilo de I/O y atómica.
 */
public final class Snapshot {
    public static final String CONFIG = "config.bin", CACHE = "cache.bin", BANNERS = "banners.raw", ICONS = "icons.raw";

    public Config config;
    /** null si no hay caché válido: el primer frame muestra cielo y reloj, la fila llega con la primera sincronización. */
    public Cache cache;
    public Atlas banners, icons;

    private Snapshot() {}

    public static Snapshot read(Context c, int density, int width, int height, String locale, Geometry g) {
        Trace.begin(Trace.SNAPSHOT_READ);
        try {
            Snapshot s = new Snapshot();
            File dir = c.getFilesDir();
            s.config = readConfig(new File(dir, CONFIG));
            Cache cache = readCache(new File(dir, CACHE), density, width, height, locale);
            if (cache != null) {
                Atlas b = readAtlas(new File(dir, BANNERS), cache.bannerCount, cache.bannerCols, g.bannerW, g.bannerH,
                    Bitmap.Config.RGB_565);
                Atlas i = readAtlas(new File(dir, ICONS), cache.iconCount, cache.iconCols, g.iconW, g.iconH,
                    Bitmap.Config.ARGB_8888);
                if (b != null && i != null) {
                    s.cache = cache;
                    s.banners = b;
                    s.icons = i;
                }
            }
            return s;
        } finally {
            Trace.end();
        }
    }

    private static Config readConfig(File f) {
        if (!f.exists()) return new Config();
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 4096))) {
            return SnapshotCodec.readConfig(in);
        } catch (IOException e) {
            return new Config();
        }
    }

    private static Cache readCache(File f, int density, int width, int height, String locale) {
        if (!f.exists()) return null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 8192))) {
            return SnapshotCodec.readCache(in, density, width, height, locale);
        } catch (IOException e) {
            return null;
        }
    }

    private static Atlas readAtlas(File f, int count, int cols, int cellW, int cellH, Bitmap.Config cfg) {
        if (count == 0) return Atlas.create(0, Math.max(cols, 1), cellW, cellH, cfg);
        Atlas a = Atlas.create(count, cols, cellW, cellH, cfg);
        long bytes = (long) a.bitmap.getRowBytes() * a.bitmap.getHeight();
        try (RandomAccessFile raf = new RandomAccessFile(f, "r"); FileChannel ch = raf.getChannel()) {
            if (ch.size() != bytes) return null;
            MappedByteBuffer buf = ch.map(FileChannel.MapMode.READ_ONLY, 0, bytes);
            a.bitmap.copyPixelsFromBuffer(buf);
            return a;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    // --- Escritura (hilo de I/O) ---

    public static void writeConfig(Context c, Config config) {
        File dir = c.getFilesDir();
        atomic(dir, CONFIG, out -> SnapshotCodec.writeConfig(out, config));
    }

    public static void writeCache(Context c, Cache cache, Atlas banners, Atlas icons) {
        File dir = c.getFilesDir();
        // Primero los atlas y al final el índice: si se corta a mitad, el índice viejo no coincide con el tamaño y se descarta.
        if (!atomicPixels(dir, BANNERS, banners.bitmap) || !atomicPixels(dir, ICONS, icons.bitmap)) return;
        atomic(dir, CACHE, out -> SnapshotCodec.writeCache(out, cache));
    }

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static boolean atomic(File dir, String name, Writer w) {
        File tmp = new File(dir, name + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos, 8192))) {
            w.write(out);
            out.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            tmp.delete();
            return false;
        }
        return tmp.renameTo(new File(dir, name));
    }

    private static boolean atomicPixels(File dir, String name, Bitmap b) {
        File tmp = new File(dir, name + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp); FileChannel ch = fos.getChannel()) {
            ByteBuffer buf = ByteBuffer.allocate(b.getRowBytes() * b.getHeight());
            b.copyPixelsToBuffer(buf);
            buf.flip();
            while (buf.hasRemaining()) ch.write(buf);
            fos.getFD().sync();
        } catch (IOException | RuntimeException e) {
            tmp.delete();
            return false;
        }
        return tmp.renameTo(new File(dir, name));
    }

    /** Tamaños de celda en px reales, derivados de la altura de la pantalla. */
    public static final class Geometry {
        public final int bannerW, bannerH, iconW, iconH;

        public Geometry(int screenHeight) {
            float s = screenHeight / 1080f;
            bannerW = Math.round(232 * s);
            bannerH = Math.round(130 * s);
            iconW = iconH = Math.round(56 * s);
        }
    }
}
