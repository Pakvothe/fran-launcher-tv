package pakvothe.franlauncher.tv.anim;

import android.os.Handler;
import android.os.Looper;

/** 1 Hz alineado al cambio de segundo del reloj de pared. */
public final class ClockTicker implements Runnable {
    public interface Listener {
        void onSecond(long wallMs);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private boolean running;

    public ClockTicker(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) return;
        running = true;
        run();
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(this);
    }

    /** Para cambios de hora o huso: re-alinea sin esperar al próximo tick. */
    public void now() {
        if (!running) return;
        handler.removeCallbacks(this);
        run();
    }

    @Override
    public void run() {
        if (!running) return;
        long now = System.currentTimeMillis();
        listener.onSecond(now);
        // +2 ms para caer siempre del lado nuevo del segundo.
        handler.postDelayed(this, 1000 - (now % 1000) + 2);
    }
}
