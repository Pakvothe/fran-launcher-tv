package pakvothe.franlauncher.tv.anim;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/** Tick a 15 fps con Handler, para movimientos lentos donde 60 fps no se distinguen. */
public final class SlowTicker implements Runnable {
    public interface Listener {
        void onSlowTick(long uptimeMs);
    }

    private static final long PERIOD = 1000 / 15;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private boolean running;

    public SlowTicker(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) return;
        running = true;
        handler.post(this);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(this);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        if (!running) return;
        long now = SystemClock.uptimeMillis();
        listener.onSlowTick(now);
        handler.postAtTime(this, now + PERIOD);
    }
}
