package pakvothe.franlauncher.tv.data;

import java.util.ArrayList;
import java.util.List;

/** Ajustes, favoritas y última app. Chico, versionado y nunca se descarta: si cambia el formato, se migra. */
public final class Config {
    public static final int THEME_AUTO = 0, THEME_DARK = 1, THEME_LIGHT = 2;
    public static final int SECONDS_NUMBER = 0, SECONDS_LINE = 1, SECONDS_OFF = 2;
    public static final int HOUR_SYSTEM = 0, HOUR_24 = 1, HOUR_12 = 2;
    public static final int[] AMBIENT_SECONDS = {8, 15, 30, 60};

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

    public int ambientSeconds() {
        return AMBIENT_SECONDS[Math.max(0, Math.min(ambient, AMBIENT_SECONDS.length - 1))];
    }
}
