package pakvothe.franlauncher.tv.app;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import java.util.Calendar;

import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.anim.ClockTicker;
import pakvothe.franlauncher.tv.anim.SlowTicker;
import pakvothe.franlauncher.tv.scene.ClockLayer;
import pakvothe.franlauncher.tv.scene.Layer;
import pakvothe.franlauncher.tv.scene.SkyLayer;
import pakvothe.franlauncher.tv.theme.PhaseResolver;
import pakvothe.franlauncher.tv.theme.Theme;
import pakvothe.franlauncher.tv.util.Trace;

/** La única View. Dueña del árbol de nodos: onDraw solo hace drawRenderNode de cada capa. */
public final class Stage extends View implements SlowTicker.Listener, ClockTicker.Listener {
    private final long createdAt;
    private boolean firstFrameLogged;
    final Animator animator = new Animator(this);
    private final SlowTicker slow = new SlowTicker(this);
    private final ClockTicker clockTicker = new ClockTicker(this);
    final SkyLayer sky = new SkyLayer(animator);
    final ClockLayer clock;
    private final Layer[] layers;
    private int lastMinute = -1;
    /** Con los orbes andando, el segundo nuevo viaja en el próximo frame de orbe (≤ 200 ms) en vez de pedir uno propio. */
    private long clockPendingSince = -1;
    private final Calendar calendar = Calendar.getInstance();
    private Theme theme;
    private int themeMode = Theme.MODE_AUTO;
    private boolean animatedBackground = true;
    private boolean started;

    public Stage(Context context, long createdAt) {
        super(context);
        this.createdAt = createdAt;
        clock = new ClockLayer(context);
        layers = new Layer[] {sky, clock};
        theme = Theme.of(themeMode, currentPhase());
        sky.setTheme(theme, false);
        clock.setTheme(theme);
        clock.tick(System.currentTimeMillis());
    }

    void onStart() {
        started = true;
        refreshTheme(false);
        clockTicker.start();
        updateSlowTicker();
    }

    void onStop() {
        started = false;
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
        if (started && animatedBackground) slow.start(); else slow.stop();
    }

    /** Re-graba solo si cambió la fase o el modo. */
    void refreshTheme(boolean animate) {
        int phase = currentPhase();
        if (theme.sameAs(themeMode, phase)) return;
        theme = Theme.of(themeMode, phase);
        sky.setTheme(theme, animate);
        clock.setTheme(theme);
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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Layer l : layers) l.draw(canvas);
        if (!firstFrameLogged) {
            firstFrameLogged = true;
            Trace.begin(Trace.FIRST_FRAME);
            Trace.end();
            // Una sola línea por arranque: bench.sh la usa para onCreate → primer frame.
            Log.i("FranLauncher", "firstframe=" + (SystemClock.uptimeMillis() - createdAt));
        }
    }
}
