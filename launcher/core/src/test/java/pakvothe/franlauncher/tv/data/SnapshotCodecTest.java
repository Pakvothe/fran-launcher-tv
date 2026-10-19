package pakvothe.franlauncher.tv.data;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Arrays;

import org.junit.Test;

public class SnapshotCodecTest {
    private static byte[] config(Config c) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnapshotCodec.writeConfig(new DataOutputStream(b), c);
        return b.toByteArray();
    }

    private static Cache cache() {
        Cache c = new Cache();
        c.density = 320; c.width = 1920; c.height = 1080;
        c.bannerCols = 4; c.bannerCount = 2; c.iconCols = 16; c.iconCount = 3;
        c.locale = "es-AR"; c.dateDay = 811_291; c.dateText = "Domingo, 18 de octubre";
        c.apps.add(new AppEntry("com.netflix/.Main", "Netflix", 0xFFE50914, 0, 0));
        c.apps.add(new AppEntry("com.yt/.Tv", "YouTube", 0xFFFF0000, 1, 1));
        c.apps.add(new AppEntry("com.x/.A", "Ñandú ü", 0xFF123456, -1, 2));
        return c;
    }

    private static byte[] bytes(Cache c) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnapshotCodec.writeCache(new DataOutputStream(b), c);
        return b.toByteArray();
    }

    private static DataInputStream in(byte[] b) {
        return new DataInputStream(new ByteArrayInputStream(b));
    }

    @Test
    public void configIdaYVuelta() throws IOException {
        Config c = new Config();
        c.theme = Config.THEME_LIGHT; c.seconds = Config.SECONDS_LINE; c.animatedBackground = false;
        c.ambient = 3; c.hourFormat = Config.HOUR_12; c.oledProtection = false; c.showNowPlaying = true;
        c.favoritesInitialized = true; c.favorites.addAll(Arrays.asList("a/.B", "c/.D")); c.lastApp = "c/.D";
        Config r = SnapshotCodec.readConfig(in(config(c)));
        assertEquals(Config.THEME_LIGHT, r.theme);
        assertEquals(Config.SECONDS_LINE, r.seconds);
        assertFalse(r.animatedBackground);
        assertEquals(60, r.ambientSeconds());
        assertEquals(Config.HOUR_12, r.hourFormat);
        assertFalse(r.oledProtection);
        assertTrue(r.showNowPlaying);
        assertTrue(r.favoritesInitialized);
        assertEquals(Arrays.asList("a/.B", "c/.D"), r.favorites);
        assertEquals("c/.D", r.lastApp);
    }

    @Test
    public void configCorruptoDaDefaults() {
        Config r = SnapshotCodec.readConfig(in(new byte[] {1, 2, 3}));
        assertEquals(Config.THEME_AUTO, r.theme);
        assertTrue(r.favorites.isEmpty());
        assertFalse(r.favoritesInitialized);
    }

    @Test
    public void configTruncadoDaDefaults() throws IOException {
        Config c = new Config();
        c.favorites.add("a/.B");
        byte[] b = config(c);
        Config r = SnapshotCodec.readConfig(in(Arrays.copyOf(b, b.length - 3)));
        assertTrue(r.favorites.isEmpty());
    }

    @Test
    public void cacheIdaYVuelta() throws IOException {
        Cache r = SnapshotCodec.readCache(in(bytes(cache())), 320, 1920, 1080, "es-AR");
        assertNotNull(r);
        assertEquals(cache().apps, r.apps);
        assertEquals("Domingo, 18 de octubre", r.dateText);
        assertEquals(811_291, r.dateDay);
        assertEquals(4, r.bannerCols);
    }

    @Test
    public void cacheSeDescartaSiCambiaPantallaOLocale() throws IOException {
        byte[] b = bytes(cache());
        assertNull(SnapshotCodec.readCache(in(b), 240, 1920, 1080, "es-AR"));
        assertNull(SnapshotCodec.readCache(in(b), 320, 3840, 2160, "es-AR"));
        assertNull(SnapshotCodec.readCache(in(b), 320, 1920, 1080, "en-US"));
    }

    @Test
    public void cacheCorruptoEsNull() throws IOException {
        byte[] b = bytes(cache());
        assertNull(SnapshotCodec.readCache(in(Arrays.copyOf(b, b.length / 2)), 320, 1920, 1080, "es-AR"));
        b[5] = 99; // versión
        assertNull(SnapshotCodec.readCache(in(b), 320, 1920, 1080, "es-AR"));
    }

    @Test
    public void cacheConCeldaFueraDelAtlasEsNull() throws IOException {
        Cache c = cache();
        c.apps.add(new AppEntry("z/.Z", "Z", 0, 7, -1));
        assertNull(SnapshotCodec.readCache(in(bytes(c)), 320, 1920, 1080, "es-AR"));
    }
}
