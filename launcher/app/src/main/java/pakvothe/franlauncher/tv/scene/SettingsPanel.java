package pakvothe.franlauncher.tv.scene;

import android.content.Context;

import pakvothe.franlauncher.tv.R;
import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.data.Config;

/** Ajustes: filas de 68 px (11 filas de 92 no entran en el panel); OK rota el valor. */
public final class SettingsPanel extends RowsPanel {
    public static final int THEME = 0, SECONDS = 1, ANIM = 2, AMBIENT = 3, HOUR = 4, LANGUAGE = 5, OLED = 6,
        NOW_PLAYING = 7, TV = 8, HOME = 9, LICENSES = 10;
    /** Los idiomas se muestran en su propio nombre, en cualquier idioma. */
    private static final String[] LANG_NAMES = {null, "Español", "English", "Português"};

    private final Context c;
    private boolean isHome;

    public SettingsPanel(Context c, Animator a, String version) {
        super(c, a, c.getString(R.string.settings), 68);
        this.c = c;
        setFooter(c.getString(R.string.credit) + "\n" + c.getString(R.string.footer, version));
        int[] titles = {R.string.set_theme, R.string.set_seconds, R.string.set_anim, R.string.set_ambient,
            R.string.set_hour, R.string.set_language, R.string.set_oled, R.string.set_now_playing, R.string.set_tv, R.string.set_home,
            R.string.set_licenses};
        for (int t : titles) rows.add(new Row(c.getString(t)));
    }

    public void setHomeStatus(boolean home) {
        isHome = home;
    }

    public void bind(Config cfg) {
        rows.get(THEME).value = s(new int[] {R.string.theme_auto, R.string.theme_dark, R.string.theme_light}[cfg.theme]);
        rows.get(SECONDS).value = s(new int[] {R.string.seconds_number, R.string.seconds_line, R.string.off}[cfg.seconds]);
        rows.get(ANIM).value = yes(cfg.animatedBackground);
        rows.get(AMBIENT).value = c.getString(R.string.ambient_after, cfg.ambientSeconds());
        rows.get(HOUR).value = s(new int[] {R.string.hour_system, R.string.hour_24, R.string.hour_12}[cfg.hourFormat]);
        rows.get(LANGUAGE).value = cfg.language == Config.LANG_SYSTEM ? s(R.string.lang_system) : LANG_NAMES[cfg.language];
        rows.get(OLED).value = yes(cfg.oledProtection);
        rows.get(NOW_PLAYING).value = yes(cfg.showNowPlaying);
        rows.get(TV).value = s(R.string.open);
        rows.get(HOME).value = yes(isHome);
        rows.get(LICENSES).value = "";
        rebuild();
    }

    private String yes(boolean b) {
        return s(b ? R.string.yes : R.string.no);
    }

    private String s(int id) {
        return c.getString(id);
    }
}
