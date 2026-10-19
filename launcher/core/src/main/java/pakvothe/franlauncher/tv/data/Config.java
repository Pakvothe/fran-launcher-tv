package pakvothe.franlauncher.tv.data;

import java.util.ArrayList;
import java.util.List;

/** Ajustes, favoritas y última app. Chico, versionado y nunca se descarta: si cambia el formato, se migra. */
public final class Config {
    public static final int THEME_AUTO = 0, THEME_DARK = 1, THEME_LIGHT = 2;
    public static final int SECONDS_NUMBER = 0, SECONDS_LINE = 1, SECONDS_OFF = 2;
    public static final int HOUR_SYSTEM = 0, HOUR_24 = 1, HOUR_12 = 2;
    public static final int[] AMBIENT_SECONDS = {8, 15, 30, 60};
    public static final int LANG_SYSTEM = 0, LANG_ES = 1, LANG_EN = 2, LANG_PT = 3;
    public static final String[] LANG_TAGS = {"", "es", "en", "pt"};

    public int theme = THEME_AUTO;
    public int seconds = SECONDS_NUMBER;
    public boolean animatedBackground = true;
    /** Índice en AMBIENT_SECONDS. */
    public int ambient = 0;
    public int hourFormat = HOUR_SYSTEM;
    public boolean oledProtection = true;
    public boolean showNowPlaying = false;
    /** Componentes "paquete/clase", en orden. Vacía + favoritesInitialized=false = primer arranque. */
    public final List<String> favorites = new ArrayList<>();
    public boolean favoritesInitialized;
    public String lastApp = "";
    /** LANG_*: Sistema o un idioma fijo para la app. */
    public int language = LANG_SYSTEM;

    /** Copia para persistir desde el hilo de I/O mientras el main sigue usando la suya. */
    public Config copy() {
        Config c = new Config();
        c.theme = theme;
        c.seconds = seconds;
        c.animatedBackground = animatedBackground;
        c.ambient = ambient;
        c.hourFormat = hourFormat;
        c.oledProtection = oledProtection;
        c.showNowPlaying = showNowPlaying;
        c.favorites.addAll(favorites);
        c.favoritesInitialized = favoritesInitialized;
        c.lastApp = lastApp;
        c.language = language;
        return c;
    }

    public int ambientSeconds() {
        return AMBIENT_SECONDS[Math.max(0, Math.min(ambient, AMBIENT_SECONDS.length - 1))];
    }
}
