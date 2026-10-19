package pakvothe.franlauncher.tv.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Íconos dibujados con primitivas (sin vector drawables). Centro (cx, cy), lado nominal size. Solo al grabar. */
public final class Icons {
    public static final int GRID = 0, INPUTS = 1, SETTINGS = 2, BACK = 3, STAR = 4, PLAY = 5, PAUSE = 6;

    private Icons() {}

    public static void draw(Canvas c, int icon, float cx, float cy, float size, Paint fill, Paint stroke) {
        float u = size / 24f;
        switch (icon) {
            case GRID:
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++) c.drawCircle(cx + i * 7 * u, cy + j * 7 * u, 2.2f * u, fill);
                break;
            case INPUTS:
                stroke.setStrokeWidth(2 * u);
                c.drawRoundRect(cx - 10 * u, cy - 7 * u, cx + 10 * u, cy + 6 * u, 2 * u, 2 * u, stroke);
                c.drawLine(cx - 5 * u, cy + 10 * u, cx + 5 * u, cy + 10 * u, stroke);
                break;
            case SETTINGS:
                stroke.setStrokeWidth(2 * u);
                for (int i = -1; i <= 1; i++) {
                    float y = cy + i * 6 * u, k = cx + (i == 0 ? 4 : -4) * u;
                    c.drawLine(cx - 10 * u, y, cx + 10 * u, y, stroke);
                    c.drawCircle(k, y, 2.6f * u, fill);
                }
                break;
            case BACK: {
                stroke.setStrokeWidth(2.2f * u);
                Path p = new Path();
                p.moveTo(cx + 3 * u, cy - 7 * u);
                p.lineTo(cx - 4 * u, cy);
                p.lineTo(cx + 3 * u, cy + 7 * u);
                c.drawPath(p, stroke);
                break;
            }
            case STAR: {
                Path p = new Path();
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    float r = (i % 2 == 0 ? 10 : 4.2f) * u;
                    float x = cx + (float) Math.cos(a) * r, y = cy + (float) Math.sin(a) * r;
                    if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
                }
                p.close();
                c.drawPath(p, fill);
                break;
            }
            case PLAY: {
                Path p = new Path();
                p.moveTo(cx - 5 * u, cy - 8 * u);
                p.lineTo(cx + 8 * u, cy);
                p.lineTo(cx - 5 * u, cy + 8 * u);
                p.close();
                c.drawPath(p, fill);
                break;
            }
            default:
                c.drawRect(cx - 6 * u, cy - 8 * u, cx - 2 * u, cy + 8 * u, fill);
                c.drawRect(cx + 2 * u, cy - 8 * u, cx + 6 * u, cy + 8 * u, fill);
        }
    }

    public static Paint strokePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        return p;
    }
}
