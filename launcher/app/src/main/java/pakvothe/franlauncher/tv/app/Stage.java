package pakvothe.franlauncher.tv.app;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import java.util.Calendar;

import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.anim.SlowTicker;
import pakvothe.franlauncher.tv.scene.Layer;
import pakvothe.franlauncher.tv.scene.SkyLayer;
import pakvothe.franlauncher.tv.theme.PhaseResolver;
import pakvothe.franlauncher.tv.theme.Theme;
import pakvothe.franlauncher.tv.util.Trace;

/** La única View. Dueña del árbol de nodos: onDraw solo hace drawRenderNode de cada capa. */
public final class Stage extends View implements SlowTicker.Listener {
    private final long createdAt;
    private boolean firstFrameLogged;
    final Animator animator = new Animator(this);
    private final SlowTicker slow = new SlowTicker(this);
    final SkyLayer sky = new SkyLayer(animator);
    private final Layer[] layers = {sky};
    private final Calendar calendar = Calendar.getInstance();
    private Theme theme;
    private int themeMode = Theme.MODE_AUTO;
    private boolean animatedBackground = true;
    private boolean started;

    public Stage(Context context, long createdAt) {
        super(context);
        this.createdAt = createdAt;
        theme = Theme.of(themeMode, currentPhase());
        sky.setTheme(theme, false);
    }

    void onStart() {
        started = true;
        refreshTheme(false);
        updateSlowTicker();
    }

    void onStop() {
        started = false;
        slow.stop();
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
        invalidate();
    }

    private int currentPhase() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        return PhaseResolver.phaseAt(calendar.get(Calendar.HOUR_OF_DAY));
    }

    @Override
    public void onSlowTick(long uptimeMs) {
        if (sky.onSlowTick(uptimeMs)) invalidate();
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
