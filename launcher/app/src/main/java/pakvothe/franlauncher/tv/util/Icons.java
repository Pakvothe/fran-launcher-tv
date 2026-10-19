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
            // GRID, INPUTS y SETTINGS: los trazos del mockup (viewBox 24), mapeados a (cx, cy, size).
            case GRID:
                stroke.setStrokeWidth(1.8f * u);
                for (int k = 0; k < 3; k++) {
                    float y = cy + (6 + 6 * k - 12) * u;
                    c.drawLine(cx - 4 * u, y, cx + 8 * u, y, stroke);
                    // r 0.9 relleno + trazo de 1.8 heredado en el SVG = radio visible 1.8.
                    c.drawCircle(cx - 8 * u, y, 1.8f * u, fill);
                }
                break;
            case INPUTS: {
                stroke.setStrokeWidth(1.6f * u);
                Path p = new Path();
                p.moveTo(cx - 9 * u, cy - 4 * u);
                p.lineTo(cx + 9 * u, cy - 4 * u);
                p.lineTo(cx + 9 * u, cy + 1 * u);
                p.lineTo(cx + 6 * u, cy + 4 * u);
                p.lineTo(cx - 6 * u, cy + 4 * u);
                p.lineTo(cx - 9 * u, cy + 1 * u);
                p.close();
                c.drawPath(p, stroke);
                c.drawLine(cx - 5 * u, cy - 1 * u, cx + 5 * u, cy - 1 * u, stroke);
                break;
            }
            case SETTINGS: {
                // Engranaje: 8 dientes (radio 10 en la punta, 7,2 en la base) y agujero central de 3.
                stroke.setStrokeWidth(1.6f * u);
                Path p = new Path();
                int teeth = 8;
                for (int k = 0; k < teeth * 4; k++) {
                    double a = (k / (teeth * 4.0)) * 2 * Math.PI - Math.PI / 2 - Math.PI / (teeth * 4);
                    float r = (k % 4 == 1 || k % 4 == 2 ? 10f : 7.2f) * u;
                    float x = cx + (float) Math.cos(a) * r, y = cy + (float) Math.sin(a) * r;
                    if (k == 0) p.moveTo(x, y); else p.lineTo(x, y);
                }
                p.close();
                c.drawPath(p, stroke);
                c.drawCircle(cx, cy, 3 * u, stroke);
                break;
            }
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
