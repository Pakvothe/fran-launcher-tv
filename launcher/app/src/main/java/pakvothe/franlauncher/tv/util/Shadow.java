package pakvothe.franlauncher.tv.util;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Sombra difusa pre-horneada en un bitmap chico y dibujada en 9 partes (como un nine-patch).
 * Nada de setElevation ni sombras dinámicas. Solo se usa al grabar nodos, nunca por frame.
 */
public final class Shadow {
    private final Bitmap bitmap;
    /** Lado de cada esquina en el bitmap (= blur + radio). */
    private final int corner;
    private final int blur;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect src = new Rect(), dst = new Rect();

    public Shadow(float radius, float blur) {
        this.blur = Math.round(blur);
        corner = Math.round(blur + radius);
        int size = corner * 2 + 2;
        bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF000000);
        p.setMaskFilter(new BlurMaskFilter(Math.max(1f, blur / 2f), BlurMaskFilter.Blur.NORMAL));
        new Canvas(bitmap).drawRoundRect(new RectF(this.blur, this.blur, size - this.blur, size - this.blur), radius,
            radius, p);
    }

    public void setColor(int argb) {
        paint.setColorFilter(new PorterDuffColorFilter(argb | 0xFF000000, PorterDuff.Mode.SRC_IN));
        paint.setAlpha(argb >>> 24);
    }

    /** Dibuja la sombra de un rectángulo (left, top, right, bottom) desplazada dy hacia abajo. */
    public void draw(Canvas c, float l, float t, float r, float b, float dy) {
        draw(c, l, t, r, b, dy, paint);
    }

    private void draw(Canvas c, float l, float t, float r, float b, float dy, Paint paint) {
        int L = Math.round(l) - blur, T = Math.round(t + dy) - blur, R = Math.round(r) + blur, B = Math.round(b + dy) + blur;
        int n = bitmap.getWidth(), k = corner;
        int[] xs = {0, k, n - k, n}, ys = {0, k, n - k, n};
        int[] dx = {L, L + k, R - k, R}, dyv = {T, T + k, B - k, B};
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                src.set(xs[j], ys[i], xs[j + 1], ys[i + 1]);
                dst.set(dx[j], dyv[i], dx[j + 1], dyv[i + 1]);
                if (dst.width() > 0 && dst.height() > 0) c.drawBitmap(bitmap, src, dst, paint);
            }
    }
}
