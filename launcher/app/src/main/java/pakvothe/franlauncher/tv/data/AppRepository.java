package pakvothe.franlauncher.tv.data;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pakvothe.franlauncher.tv.util.Trace;

/**
 * Consulta de apps a PackageManager en el único hilo de I/O de la app. Calcula el diff contra el caché anterior,
 * regenera solo los banners e íconos que cambiaron, avisa al main y persiste.
 */
public final class AppRepository {
    public interface Listener {
        /** En el main. Los atlas son nuevos: los anteriores se pueden soltar. */
        void onApps(Cache cache, Atlas banners, Atlas icons);
    }

    private static final int BANNER_COLS = 4, ICON_COLS = 16;

    private final Context context;
    private final Listener listener;
    private final HandlerThread thread = new HandlerThread("io", Process.THREAD_PRIORITY_BACKGROUND);
    private final Handler io;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Config pendingConfig;
    private final Runnable saveConfig = this::flushConfig;

    private void flushConfig() {
        Config c;
        synchronized (this) {
            c = pendingConfig;
            pendingConfig = null;
        }
        if (c != null) Snapshot.writeConfig(context, c);
    }

    public AppRepository(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        thread.start();
        io = new Handler(thread.getLooper());
    }

    public void quit() {
        io.post(saveConfig);
        thread.quitSafely();
    }

    /** Debounce de 500 ms después del último cambio. Recibe una copia: el main sigue usando la suya. */
    public void saveConfig(Config copy) {
        synchronized (this) {
            pendingConfig = copy;
        }
        io.removeCallbacks(saveConfig);
        io.postDelayed(saveConfig, 500);
    }

    public void post(Runnable r) {
        io.post(r);
    }

    public void sync(Cache prev, Atlas prevBanners, Atlas prevIcons, Snapshot.Geometry g, int density, int width,
                     int height, String locale, int dateDay, String dateText) {
        io.post(() -> {
            Trace.begin(Trace.SYNC_APPS);
            long t0 = android.os.SystemClock.uptimeMillis();
            try {
                Result r = build(prev, prevBanners, prevIcons, g);
                r.cache.density = density;
                r.cache.width = width;
                r.cache.height = height;
                r.cache.locale = locale;
                r.cache.dateDay = dateDay;
                r.cache.dateText = dateText;
                if (prev == null || r.changed || !prev.locale.equals(locale) || prev.dateDay != dateDay) {
                    if (r.changed || prev == null) main.post(() -> listener.onApps(r.cache, r.banners, r.icons));
                    Snapshot.writeCache(context, r.cache, r.banners, r.icons);
                }
                // Una línea por sincronización: bench.sh y el diagnóstico la usan.
                android.util.Log.i("FranLauncher", "sync=" + (android.os.SystemClock.uptimeMillis() - t0)
                    + " apps=" + r.cache.apps.size() + " banners=" + r.cache.bannerCount + " changed=" + r.changed);
            } finally {
                Trace.end();
            }
        });
    }

    private static final class Result {
        Cache cache;
        Atlas banners, icons;
        boolean changed;
    }

    private Result build(Cache prev, Atlas prevBanners, Atlas prevIcons, Snapshot.Geometry g) {
        PackageManager pm = context.getPackageManager();
        String self = context.getPackageName();
        Map<String, ResolveInfo> found = new LinkedHashMap<>();
        // Leanback primero; LAUNCHER como respaldo para apps que no declaran la categoría de TV.
        for (String cat : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String pkg = ri.activityInfo.packageName;
                if (!pkg.equals(self) && !found.containsKey(pkg)) found.put(pkg, ri);
            }
        }
        Map<String, AppEntry> old = new HashMap<>();
        if (prev != null) for (AppEntry e : prev.apps) old.put(e.component, e);

        List<ResolveInfo> list = new ArrayList<>(found.values());
        List<Drawable> bannerDrawables = new ArrayList<>(list.size());
        int bannerCount = 0;
        long[] stamps = new long[list.size()];
        AppEntry[] reuse = new AppEntry[list.size()];
        for (int i = 0; i < list.size(); i++) {
            ActivityInfo ai = list.get(i).activityInfo;
            stamps[i] = stamp(pm, ai.packageName);
            AppEntry o = old.get(ai.packageName + "/" + ai.name);
            boolean cellsOk = o != null && prevBanners != null && prevIcons != null;
            if (cellsOk && o.stamp == stamps[i]) {
                reuse[i] = o;
                bannerDrawables.add(null);
                if (o.banner >= 0) bannerCount++;
            } else {
                Drawable b = ai.loadBanner(pm);
                bannerDrawables.add(b);
                if (b != null) bannerCount++;
            }
        }

        Result r = new Result();
        r.cache = new Cache();
        r.cache.bannerCols = BANNER_COLS;
        r.cache.iconCols = ICON_COLS;
        r.banners = Atlas.create(bannerCount, BANNER_COLS, g.bannerW, g.bannerH, Bitmap.Config.RGB_565);
        r.icons = Atlas.create(list.size(), ICON_COLS, g.iconW, g.iconH, Bitmap.Config.ARGB_8888);
        Canvas bc = new Canvas(r.banners.bitmap), ic = new Canvas(r.icons.bitmap);
        Rect src = new Rect(), dst = new Rect();
        Bitmap probe = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        int nextBanner = 0;
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo ri = list.get(i);
            ActivityInfo ai = ri.activityInfo;
            String comp = ai.packageName + "/" + ai.name;
            String label = String.valueOf(ri.loadLabel(pm));
            AppEntry o = reuse[i];
            int banner = -1, color;
            r.icons.cell(i, dst);
            if (o != null) {
                prevIcons.cell(o.icon, src);
                ic.drawBitmap(prevIcons.bitmap, src, dst, null);
                color = o.color;
                if (o.banner >= 0) {
                    banner = nextBanner++;
                    prevBanners.cell(o.banner, src);
                    r.banners.cell(banner, dst);
                    bc.drawBitmap(prevBanners.bitmap, src, dst, null);
                }
                if (!label.equals(o.label)) r.changed = true;
            } else {
                r.changed = true;
                Drawable icon = ri.loadIcon(pm);
                drawInto(ic, icon, dst);
                color = averageColor(icon, probe);
                Drawable b = bannerDrawables.get(i);
                if (b != null) {
                    banner = nextBanner++;
                    r.banners.cell(banner, dst);
                    bc.save();
                    bc.clipRect(dst);
                    bc.drawColor(color);
                    bc.restore();
                    drawInto(bc, b, dst);
                }
            }
            r.cache.apps.add(new AppEntry(comp, label, color, banner, i, stamps[i]));
        }
        r.cache.bannerCount = bannerCount;
        r.cache.iconCount = list.size();
        if (prev == null || prev.apps.size() != r.cache.apps.size()) r.changed = true;
        else for (int i = 0; i < prev.apps.size() && !r.changed; i++)
            if (!prev.apps.get(i).component.equals(r.cache.apps.get(i).component)) r.changed = true;
        probe.recycle();
        return r;
    }

    private static long stamp(PackageManager pm, String pkg) {
        try {
            return pm.getPackageInfo(pkg, 0).lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private static void drawInto(Canvas c, Drawable d, Rect dst) {
        if (d == null) return;
        Rect keep = d.copyBounds();
        d.setBounds(dst);
        d.draw(c);
        d.setBounds(keep);
    }

    /** Promedio de los píxeles opacos del ícono, oscurecido si es muy claro para que el texto blanco se lea. */
    static int averageColor(Drawable icon, Bitmap probe) {
        if (icon == null) return 0xFF2A2E3A;
        probe.eraseColor(0);
        Canvas c = new Canvas(probe);
        Rect keep = icon.copyBounds();
        icon.setBounds(0, 0, probe.getWidth(), probe.getHeight());
        icon.draw(c);
        icon.setBounds(keep);
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < probe.getHeight(); y++)
            for (int x = 0; x < probe.getWidth(); x++) {
                int p = probe.getPixel(x, y);
                if (Color.alpha(p) < 128) continue;
                r += Color.red(p); g += Color.green(p); b += Color.blue(p); n++;
            }
        if (n == 0) return 0xFF2A2E3A;
        float[] hsv = new float[3];
        Color.RGBToHSV((int) (r / n), (int) (g / n), (int) (b / n), hsv);
        hsv[2] = Math.min(hsv[2], 0.62f);
        return Color.HSVToColor(hsv);
    }
}
