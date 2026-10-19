package pakvothe.franlauncher.tv.app;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import pakvothe.franlauncher.tv.R;
import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.anim.ClockTicker;
import pakvothe.franlauncher.tv.anim.SlowTicker;
import pakvothe.franlauncher.tv.data.AppEntry;
import pakvothe.franlauncher.tv.data.AppRepository;
import pakvothe.franlauncher.tv.data.Atlas;
import pakvothe.franlauncher.tv.data.Cache;
import pakvothe.franlauncher.tv.data.Config;
import pakvothe.franlauncher.tv.data.Letters;
import pakvothe.franlauncher.tv.data.Snapshot;
import pakvothe.franlauncher.tv.scene.AppListPanel;
import pakvothe.franlauncher.tv.scene.ClockLayer;
import pakvothe.franlauncher.tv.scene.PanelLayer;
import pakvothe.franlauncher.tv.scene.Layer;
import pakvothe.franlauncher.tv.scene.MenuLayer;
import pakvothe.franlauncher.tv.scene.ShelfLayer;
import pakvothe.franlauncher.tv.scene.SkyLayer;
import pakvothe.franlauncher.tv.theme.PhaseResolver;
import pakvothe.franlauncher.tv.theme.Theme;
import pakvothe.franlauncher.tv.util.Launch;
import pakvothe.franlauncher.tv.util.Trace;

/** La única View. Dueña del árbol de nodos: onDraw solo hace drawRenderNode de cada capa. */
public final class Stage extends View implements SlowTicker.Listener, ClockTicker.Listener, AppRepository.Listener {
    private final long createdAt;
    private boolean firstFrameLogged;
    final Animator animator = new Animator(this);
    private final SlowTicker slow = new SlowTicker(this);
    private final ClockTicker clockTicker = new ClockTicker(this);
    final SkyLayer sky = new SkyLayer(animator);
    final ClockLayer clock;
    final ShelfLayer shelf;
    final PanelLayer panel;
    final AppListPanel allApps;
    final MenuLayer menu;
    private static final int ACT_MOVE = 0, ACT_REMOVE = 1, ACT_ADD = 2, ACT_INFO = 3, ACT_UNINSTALL = 4;
    private final int[] menuActions = new int[4];
    private AppEntry menuTarget;
    private int movingIndex = -1;
    /** Apps del panel Todas, ordenadas con el Collator del locale. */
    private final List<AppEntry> sorted = new ArrayList<>();
    final FocusModel focus = new FocusModel();
    /** Favoritas resueltas contra el caché, en orden. */
    final List<AppEntry> favorites = new ArrayList<>();
    boolean hasInputs;
    private final Layer[] layers;
    private int lastMinute = -1;
    /** Con los orbes andando, el segundo nuevo viaja en el próximo frame de orbe (≤ 200 ms) en vez de pedir uno propio. */
    private long clockPendingSince = -1;
    private final int screenW, screenH, density;
    private final Snapshot.Geometry geometry;
    final Config config;
    Cache cache;
    Atlas banners, icons;
    final AppRepository repo;
    private boolean synced;
    private final Runnable firstSync = this::syncApps;
    private final Calendar calendar = Calendar.getInstance();
    private Theme theme;
    private int themeMode = Theme.MODE_AUTO;
    private boolean animatedBackground = true;
    private boolean started;

    public Stage(Context context, long createdAt, int w, int h, int density) {
        super(context);
        this.createdAt = createdAt;
        screenW = w;
        screenH = h;
        this.density = density;
        geometry = new Snapshot.Geometry(h);
        Snapshot snap = Snapshot.read(context, density, w, h, localeTag(), geometry);
        config = snap.config;
        cache = snap.cache;
        banners = snap.banners;
        icons = snap.icons;
        repo = new AppRepository(context, this);
        themeMode = config.theme;
        animatedBackground = config.animatedBackground;
        clock = new ClockLayer(context);
        clock.setSecondsMode(config.seconds);
        clock.setHourFormat(config.hourFormat);
        if (cache != null) clock.seedDate(cache.dateDay, cache.dateText);
        shelf = new ShelfLayer(context, animator);
        shelf.setInvalidator(this::invalidate);
        panel = new PanelLayer(animator);
        allApps = new AppListPanel(context, animator);
        menu = new MenuLayer(context, animator);
        layers = new Layer[] {sky, clock, shelf, panel, menu};
        theme = Theme.of(themeMode, currentPhase());
        sky.setTheme(theme, false);
        clock.setTheme(theme);
        shelf.setTheme(theme);
        panel.setTheme(theme);
        allApps.setTheme(theme);
        menu.setTheme(theme);
        resolveFavorites();
        focusLastApp(false);
        clock.tick(System.currentTimeMillis());
    }

    void onStart() {
        started = true;
        // Al volver de otra app (o de desinstalar): resincronizar. En el arranque lo hace el primer frame.
        if (firstFrameLogged) syncApps();
        refreshTheme(false);
        clockTicker.start();
        updateSlowTicker();
    }

    void onDestroy() {
        removeCallbacks(firstSync);
        repo.quit();
    }

    private static String localeTag() {
        return Locale.getDefault().toLanguageTag();
    }

    /** Fuera del camino crítico: se llama después del primer frame y al volver de otra app. */
    void syncApps() {
        repo.sync(cache, banners, icons, geometry, density, screenW, screenH, localeTag(), clock.dateDay(), clock.dateText());
    }

    @Override
    public void onApps(Cache c, Atlas b, Atlas i) {
        cache = c;
        banners = b;
        icons = i;
        String focused = focusedComponent();
        resolveFavorites();
        int idx = indexOfFavorite(focused);
        focus.focusShelf(idx >= 0 ? idx : focus.index());
        shelf.setFocus(focus.index(), false);
        invalidate();
    }

    /** Mapea config.favorites contra el caché. Primer arranque: las primeras 6 apps con banner. */
    private void resolveFavorites() {
        favorites.clear();
        if (cache != null) {
            if (!config.favoritesInitialized) {
                for (AppEntry e : cache.apps) {
                    if (e.banner >= 0 && config.favorites.size() < 6) config.favorites.add(e.component);
                }
                config.favoritesInitialized = true;
                repo.saveConfig(config.copy());
            }
            Map<String, AppEntry> byComponent = new HashMap<>();
            for (AppEntry e : cache.apps) byComponent.put(e.component, e);
            for (String f : config.favorites) {
                AppEntry e = byComponent.get(f);
                if (e != null) favorites.add(e);
            }
        }
        focus.setShelf(favorites.size(), hasInputs);
        shelf.setData(favorites, banners, icons, hasInputs);
        if (panel.current() == allApps) refreshAllApps();
    }

    private int indexOfFavorite(String component) {
        if (component == null) return -1;
        for (int i = 0; i < favorites.size(); i++) if (favorites.get(i).component.equals(component)) return i;
        return -1;
    }

    private String focusedComponent() {
        int f = focus.focusedFavorite();
        return f >= 0 && f < favorites.size() ? favorites.get(f).component : null;
    }

    /** Al volver al inicio, el foco queda sobre la última app lanzada desde el launcher. */
    void focusLastApp(boolean animate) {
        int idx = indexOfFavorite(config.lastApp);
        focus.focusShelf(Math.max(idx, 0));
        shelf.setFocus(focus.index(), animate);
    }

    private void refreshAllApps() {
        sorted.clear();
        if (cache != null) sorted.addAll(cache.apps);
        Collator col = Collator.getInstance();
        col.setStrength(Collator.PRIMARY);
        sorted.sort((a, b) -> col.compare(a.label, b.label));
        int[] letters = new int[sorted.size()];
        for (int i = 0; i < letters.length; i++) letters[i] = Letters.bucket(sorted.get(i).label);
        focus.setList(letters);
        allApps.setData(sorted, new HashSet<>(config.favorites), icons);
    }

    private void openPanel() {
        panel.show(allApps);
        float left = panel.left();
        animator.animate(clock.node, Animator.SCALE, 0.78f, 400);
        animator.animate(clock.node, Animator.TX, left / 2 - getWidth() / 2f, 400);
        animator.animate(shelf.node, Animator.ALPHA, 0f, 300);
        updateSlowTicker();
    }

    void closePanel(boolean animate) {
        if (!panel.isOpen()) return;
        focus.closePanel();
        panel.hide();
        int d = animate ? 400 : 0;
        animator.animate(clock.node, Animator.SCALE, 1f, d);
        animator.animate(clock.node, Animator.TX, 0f, d);
        animator.animate(shelf.node, Animator.ALPHA, 1f, animate ? 300 : 0);
        if (!animate) animator.cancelAll();
        updateSlowTicker();
    }

    private void openMenu() {
        boolean fromShelf = focus.state() == FocusModel.SHELF;
        menuTarget = fromShelf ? favorites.get(focus.focusedFavorite()) : allApps.focused();
        if (menuTarget == null) return;
        boolean isFav = config.favorites.contains(menuTarget.component);
        int n = 0;
        if (fromShelf) {
            menuActions[n++] = ACT_MOVE;
            menuActions[n++] = ACT_REMOVE;
        } else {
            menuActions[n++] = isFav ? ACT_REMOVE : ACT_ADD;
        }
        menuActions[n++] = ACT_INFO;
        if (!menuTarget.system) menuActions[n++] = ACT_UNINSTALL;
        String[] labels = new String[n];
        int[] ids = {R.string.move, R.string.remove_home, R.string.add_home, R.string.app_info, R.string.uninstall};
        for (int i = 0; i < n; i++) labels[i] = getContext().getString(ids[menuActions[i]]);
        focus.openMenu(n);
        float s = getHeight() / 1080f;
        if (fromShelf) menu.show(menuTarget.label, labels, shelf.centerX(focus.index()), shelf.top() - 36 * s);
        else menu.show(menuTarget.label, labels, panel.left() - 240 * s, getHeight() / 2f + 160 * s);
        menu.setFocus(0);
    }

    private void runMenuAction(int action) {
        AppEntry e = menuTarget;
        if (e == null) return;
        switch (action) {
            case ACT_MOVE:
                movingIndex = focus.index();
                focus.startMoving();
                shelf.setMoving(true);
                break;
            case ACT_REMOVE:
                config.favorites.remove(e.component);
                resolveFavorites();
                shelf.setFocus(focus.index(), false);
                repo.saveConfig(config.copy());
                break;
            case ACT_ADD:
                if (!config.favorites.contains(e.component)) config.favorites.add(e.component);
                resolveFavorites();
                repo.saveConfig(config.copy());
                break;
            case ACT_INFO:
                Launch.appInfo(getContext(), e.packageName());
                break;
            default:
                Launch.uninstall(getContext(), e.packageName());
        }
    }

    private void launch(AppEntry e) {
        if (Launch.app(getContext(), e.component)) {
            config.lastApp = e.component;
            repo.saveConfig(config.copy());
        }
    }

    /** Tecla lógica del InputRouter. */
    void onKey(int key) {
        switch (focus.onKey(key)) {
            case FocusModel.OPEN_ALL:
                refreshAllApps();
                focus.openList();
                allApps.setIndex(-1, false);
                allApps.setFocus(0, false);
                openPanel();
                break;
            case FocusModel.LIST_FOCUS:
                allApps.setFocus(focus.listIndex(), true);
                break;
            case FocusModel.INDEX_ENTER:
            case FocusModel.INDEX_FOCUS:
                allApps.setIndex(focus.letterIndex(), true);
                break;
            case FocusModel.INDEX_EXIT:
                allApps.setIndex(focus.letterIndex(), false);
                allApps.setFocus(focus.listIndex(), true);
                break;
            case FocusModel.LIST_LAUNCH: {
                AppEntry e = allApps.focused();
                if (e != null) launch(e);
                break;
            }
            case FocusModel.OPEN_MENU:
                openMenu();
                break;
            case FocusModel.MENU_FOCUS:
                menu.setFocus(focus.menuIndex());
                break;
            case FocusModel.MENU_PICK:
                menu.hide(true);
                runMenuAction(menuActions[focus.menuIndex()]);
                break;
            case FocusModel.CLOSE_MENU:
                menu.hide(true);
                break;
            case FocusModel.MOVE_STEP: {
                // Se mueve sobre la lista resuelta: config.favorites puede tener apps que hoy no están instaladas.
                int to = focus.index();
                List<String> order = new ArrayList<>();
                for (AppEntry f : favorites) order.add(f.component);
                order.add(to, order.remove(movingIndex));
                for (String f : config.favorites) if (!order.contains(f)) order.add(f);
                config.favorites.clear();
                config.favorites.addAll(order);
                movingIndex = to;
                resolveFavorites();
                shelf.setFocus(to, true);
                break;
            }
            case FocusModel.MOVE_DROP:
                movingIndex = -1;
                shelf.setMoving(false);
                repo.saveConfig(config.copy());
                break;
            case FocusModel.CLOSE_PANEL:
                if (menu.isOpen()) menu.hide(true);
                closePanel(true);
                if (key == FocusModel.HOME) {
                    focus.focusShelf(0);
                    shelf.setFocus(0, true);
                }
                break;
            case FocusModel.FOCUS:
                shelf.setFocus(focus.index(), true);
                break;
            case FocusModel.LAUNCH: {
                AppEntry e = favorites.get(focus.focusedFavorite());
                shelf.press(focus.index());
                launch(e);
                break;
            }
            default:
                break;
        }
    }

    void onStop() {
        started = false;
        if (menu.isOpen()) {
            menu.hide(false);
            focus.closeMenu();
        }
        if (focus.state() == FocusModel.MOVING) onKey(FocusModel.OK);
        closePanel(false);
        slow.stop();
        clockPendingSince = -1;
        clockTicker.stop();
    }

    void setAnimatedBackground(boolean on) {
        animatedBackground = on;
        sky.setOrbsVisible(on);
        updateSlowTicker();
    }

    private void updateSlowTicker() {
        // Con un panel abierto, el panel tapa la mayor parte: los orbes se pausan.
        if (started && animatedBackground && !panel.isOpen()) slow.start(); else slow.stop();
    }

    /** Re-graba solo si cambió la fase o el modo. */
    void refreshTheme(boolean animate) {
        int phase = currentPhase();
        if (theme.sameAs(themeMode, phase)) return;
        theme = Theme.of(themeMode, phase);
        sky.setTheme(theme, animate);
        clock.setTheme(theme);
        shelf.setTheme(theme);
        panel.setTheme(theme);
        allApps.setTheme(theme);
        menu.setTheme(theme);
        invalidate();
    }

    private int currentPhase() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        return PhaseResolver.phaseAt(calendar.get(Calendar.HOUR_OF_DAY));
    }

    void setSecondsMode(int m) {
        clock.setSecondsMode(m);
        clock.tick(System.currentTimeMillis());
        invalidate();
    }

    void setHourFormat(int f) {
        clock.setHourFormat(f);
        clock.tick(System.currentTimeMillis());
        invalidate();
    }

    /** Hora, huso o locale cambiaron. */
    void onTimeChanged() {
        clock.resetDate();
        lastMinute = -1;
        clockTicker.now();
    }

    @Override
    public void onSecond(long wallMs) {
        if (clock.tick(wallMs)) {
            if (slow.isRunning()) clockPendingSince = SystemClock.uptimeMillis(); else invalidate();
        }
        // El cambio de fase se chequea una vez por minuto dentro del mismo tick.
        int minute = (int) (wallMs / 60_000L);
        if (minute != lastMinute) {
            lastMinute = minute;
            refreshTheme(true);
        }
    }

    @Override
    public void onSlowTick(long uptimeMs) {
        boolean moved = sky.onSlowTick(uptimeMs);
        boolean clockDue = clockPendingSince >= 0 && uptimeMs - clockPendingSince >= 200;
        if (moved || clockDue) {
            clockPendingSince = -1;
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        for (Layer l : layers) l.setSize(w, h);
        allApps.layout(panel.panelWidth(), panel.panelHeight(), h / 1080f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Layer l : layers) l.draw(canvas);
        if (!firstFrameLogged) {
            firstFrameLogged = true;
            Trace.begin(Trace.FIRST_FRAME);
            Trace.end();
            // Una sola línea por arranque: bench.sh la usa para onCreate → primer frame.
            // proc = desde que el sistema creó el proceso: no depende de cuánto tarde en pausarse la app anterior.
            long now = SystemClock.uptimeMillis();
            Log.i("FranLauncher", "firstframe=" + (now - createdAt) + " proc=" + (now - android.os.Process.getStartUptimeMillis()));
            post(firstSync);
        }
    }
}
