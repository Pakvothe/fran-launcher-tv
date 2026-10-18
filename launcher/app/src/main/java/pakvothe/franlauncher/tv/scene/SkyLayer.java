package pakvothe.franlauncher.tv.scene;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.graphics.Shader;

import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.theme.Theme;

/** Cielo de la hora: degradado de dos colores + 2 orbes difusos que se mueven solo por translation. */
public final class SkyLayer extends Layer {
    private static final int ORB_TEX = 128;
    private static final float ORB_SIZE = 1200f;
    private static final int CROSSFADE = 2000;

    private final Animator animator;
    private final RenderNode base = new RenderNode("sky.base");
    private final RenderNode fade = new RenderNode("sky.fade");
    private final RenderNode orbA = new RenderNode("sky.orbA");
    private final RenderNode orbB = new RenderNode("sky.orbB");
    private final Paint gradient = new Paint();
    private final Paint orbPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect orbDst = new Rect();
    private Bitmap texA, texB;
    private Theme theme;
    private float orbPx, axA, ayA, axB, ayB, bxA, byA, bxB, byB;
    /** Último offset dibujado y salto mínimo para pedir otro frame. */
    private float drawnAx, drawnAy, drawnBx, drawnBy, minStep;

    private final Runnable fadeDone = () -> {
        recordGradient(base, theme);
        fade.setAlpha(0f);
    };

    public SkyLayer(Animator animator) {
        super("sky");
        this.animator = animator;
        for (RenderNode n : new RenderNode[] {base, fade, orbA, orbB}) n.setHasOverlappingRendering(false);
        fade.setAlpha(0f);
    }

    public void setTheme(Theme t, boolean animate) {
        Theme old = theme;
        theme = t;
        if (w == 0) return;
        texA = orbTexture(texA, t.orbA, t.light);
        texB = orbTexture(texB, t.orbB, t.light);
        recordOrbs();
        if (old == null || !animate) {
            recordGradient(base, t);
            fade.setAlpha(0f);
        } else {
            recordGradient(fade, t);
            fade.setAlpha(0f);
            animator.animate(fade, Animator.ALPHA, 1f, CROSSFADE, fadeDone);
        }
    }

    @Override
    protected void onSizeChanged() {
        base.setPosition(0, 0, w, h);
        fade.setPosition(0, 0, w, h);
        orbPx = ORB_SIZE * s;
        int o = Math.round(orbPx);
        orbA.setPosition(0, 0, o, o);
        orbB.setPosition(0, 0, o, o);
        orbDst.set(0, 0, o, o);
        bxA = w * 0.22f - orbPx / 2; byA = h * 0.28f - orbPx / 2;
        bxB = w * 0.80f - orbPx / 2; byB = h * 0.74f - orbPx / 2;
        axA = 170 * s; ayA = 100 * s;
        axB = 150 * s; ayB = 120 * s;
        minStep = 3 * s;
        if (theme != null) setTheme(theme, false);
        onSlowTick(0);
    }

    /**
     * Ciclos de ida y vuelta de 48 s y 63 s: períodos distintos para que no se vea un patrón.
     * Devuelve true solo si algún orbe se movió al menos minStep: en esta GPU cada frame cuesta lo mismo
     * por chico que sea el cambio, y un salto de 3 px en un orbe difuso de 1200 px no se ve.
     */
    public boolean onSlowTick(long now) {
        double a = (now % 48_000L) / 48_000.0 * 2 * Math.PI;
        double b = (now % 63_000L) / 63_000.0 * 2 * Math.PI;
        float ax = axA * (float) Math.sin(a), ay = ayA * (float) Math.cos(a);
        float bx = axB * (float) Math.cos(b), by = ayB * (float) Math.sin(b);
        if (now != 0 && Math.abs(ax - drawnAx) < minStep && Math.abs(ay - drawnAy) < minStep
            && Math.abs(bx - drawnBx) < minStep && Math.abs(by - drawnBy) < minStep) return false;
        drawnAx = ax; drawnAy = ay; drawnBx = bx; drawnBy = by;
        orbA.setTranslationX(bxA + ax);
        orbA.setTranslationY(byA + ay);
        orbB.setTranslationX(bxB + bx);
        orbB.setTranslationY(byB + by);
        return true;
    }

    public void setOrbsVisible(boolean visible) {
        orbA.setAlpha(visible ? 1f : 0f);
        orbB.setAlpha(visible ? 1f : 0f);
    }

    @Override
    protected void record(RecordingCanvas c) {
        c.drawRenderNode(base);
        c.drawRenderNode(fade);
        c.drawRenderNode(orbA);
        c.drawRenderNode(orbB);
    }

    private void recordGradient(RenderNode n, Theme t) {
        gradient.setShader(new LinearGradient(0, 0, 0, h, t.skyTop, t.skyBottom, Shader.TileMode.CLAMP));
        record(n, w, h, c -> c.drawPaint(gradient));
    }

    private void recordOrbs() {
        record(orbA, orbDst.right, orbDst.bottom, c -> c.drawBitmap(texA, null, orbDst, orbPaint));
        record(orbB, orbDst.right, orbDst.bottom, c -> c.drawBitmap(texB, null, orbDst, orbPaint));
    }

    /** Gradiente radial de 128×128 que se dibuja escalado: al ser difuso no se nota la baja resolución. */
    private static Bitmap orbTexture(Bitmap reuse, int color, boolean light) {
        Bitmap b = reuse != null ? reuse : Bitmap.createBitmap(ORB_TEX, ORB_TEX, Bitmap.Config.ARGB_8888);
        b.eraseColor(0);
        int a = light ? 0x99 : 0x80;
        int c0 = (a << 24) | (color & 0xFFFFFF);
        int c1 = (a / 3 << 24) | (color & 0xFFFFFF);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = ORB_TEX / 2f;
        p.setShader(new RadialGradient(r, r, r, new int[] {c0, c1, color & 0xFFFFFF}, new float[] {0f, 0.45f, 1f},
            Shader.TileMode.CLAMP));
        new Canvas(b).drawCircle(r, r, r, p);
        return b;
    }
}
