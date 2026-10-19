package pakvothe.franlauncher.tv.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Formato binario de config.bin y cache.bin. Sin reflexión ni serialización de Java. */
public final class SnapshotCodec {
    static final int CONFIG_MAGIC = 0x464C4346; // FLCF
    static final int CACHE_MAGIC = 0x464C4341;  // FLCA
    public static final int CONFIG_VERSION = 1;
    public static final int CACHE_VERSION = 2;

    private SnapshotCodec() {}

    public static void writeConfig(DataOutputStream out, Config c) throws IOException {
        out.writeInt(CONFIG_MAGIC);
        out.writeInt(CONFIG_VERSION);
        out.writeByte(c.theme);
        out.writeByte(c.seconds);
        out.writeBoolean(c.animatedBackground);
        out.writeByte(c.ambient);
        out.writeByte(c.hourFormat);
        out.writeBoolean(c.oledProtection);
        out.writeBoolean(c.showNowPlaying);
        out.writeBoolean(c.favoritesInitialized);
        out.writeShort(c.favorites.size());
        for (String f : c.favorites) out.writeUTF(f);
        out.writeUTF(c.lastApp);
    }

    /** Nunca devuelve null: si no se puede leer, devuelve los valores por defecto. Versiones viejas se migran acá. */
    public static Config readConfig(DataInputStream in) {
        Config c = new Config();
        try {
            if (in.readInt() != CONFIG_MAGIC) return new Config();
            int version = in.readInt();
            if (version < 1 || version > CONFIG_VERSION) return new Config();
            c.theme = clamp(in.readByte(), 0, 2);
            c.seconds = clamp(in.readByte(), 0, 2);
            c.animatedBackground = in.readBoolean();
            c.ambient = clamp(in.readByte(), 0, Config.AMBIENT_SECONDS.length - 1);
            c.hourFormat = clamp(in.readByte(), 0, 2);
            c.oledProtection = in.readBoolean();
            c.showNowPlaying = in.readBoolean();
            c.favoritesInitialized = in.readBoolean();
            int n = in.readUnsignedShort();
            for (int i = 0; i < n; i++) c.favorites.add(in.readUTF());
            c.lastApp = in.readUTF();
            return c;
        } catch (IOException | RuntimeException e) {
            return new Config();
        }
    }

    public static void writeCache(DataOutputStream out, Cache c) throws IOException {
        out.writeInt(CACHE_MAGIC);
        out.writeInt(CACHE_VERSION);
        out.writeInt(c.density);
        out.writeInt(c.width);
        out.writeInt(c.height);
        out.writeShort(c.bannerCols);
        out.writeShort(c.bannerCount);
        out.writeShort(c.iconCols);
        out.writeShort(c.iconCount);
        out.writeUTF(c.locale);
        out.writeInt(c.dateDay);
        out.writeUTF(c.dateText);
        out.writeShort(c.apps.size());
        for (AppEntry e : c.apps) {
            out.writeUTF(e.component);
            out.writeUTF(e.label);
            out.writeInt(e.color);
            out.writeShort(e.banner);
            out.writeShort(e.icon);
            out.writeLong(e.stamp);
            out.writeBoolean(e.system);
        }
    }

    /** null si la versión, la pantalla o el locale no coinciden, o si está corrupto. */
    public static Cache readCache(DataInputStream in, int density, int width, int height, String locale) {
        try {
            if (in.readInt() != CACHE_MAGIC || in.readInt() != CACHE_VERSION) return null;
            Cache c = new Cache();
            c.density = in.readInt();
            c.width = in.readInt();
            c.height = in.readInt();
            if (c.density != density || c.width != width || c.height != height) return null;
            c.bannerCols = in.readShort();
            c.bannerCount = in.readShort();
            c.iconCols = in.readShort();
            c.iconCount = in.readShort();
            c.locale = in.readUTF();
            if (!c.locale.equals(locale)) return null;
            c.dateDay = in.readInt();
            c.dateText = in.readUTF();
            int n = in.readUnsignedShort();
            for (int i = 0; i < n; i++) {
                String comp = in.readUTF();
                String label = in.readUTF();
                int color = in.readInt();
                int banner = in.readShort();
                int icon = in.readShort();
                long stamp = in.readLong();
                boolean system = in.readBoolean();
                if ((banner >= c.bannerCount) || (icon >= c.iconCount)) return null;
                c.apps.add(new AppEntry(comp, label, color, banner, icon, stamp, system));
            }
            return c;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
