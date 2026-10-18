package pakvothe.franlauncher.tv.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import pakvothe.franlauncher.tv.util.Trace;

/** La única View. Dueña del árbol de nodos. */
public final class Stage extends View {
    private final long createdAt;
    private boolean firstFrameLogged;
    private final Paint sky = new Paint();

    public Stage(Context context, long createdAt) {
        super(context);
        this.createdAt = createdAt;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        sky.setShader(new LinearGradient(0, 0, 0, h, 0xFF0A0E1C, 0xFF1B2244, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPaint(sky);
        if (!firstFrameLogged) {
            firstFrameLogged = true;
            Trace.begin(Trace.FIRST_FRAME);
            Trace.end();
            // Una sola línea por arranque: bench.sh la usa para onCreate → primer frame.
            Log.i("FranLauncher", "firstframe=" + (SystemClock.uptimeMillis() - createdAt));
        }
    }
}
