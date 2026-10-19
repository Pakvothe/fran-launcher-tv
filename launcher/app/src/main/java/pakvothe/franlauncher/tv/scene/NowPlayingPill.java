package pakvothe.franlauncher.tv.scene;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.text.TextPaint;
import android.text.TextUtils;

import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.theme.Theme;
import pakvothe.franlauncher.tv.util.Icons;
import pakvothe.franlauncher.tv.util.Typography;

/**
 * Píldora de Reproduciendo: carátula circular de 56 px, título, artista · app y play/pausa de 52 px.
 * La dibuja el reloj (va debajo de la fecha y se mueve con él). Se re-graba solo cuando cambia la pista.
 */
public final class NowPlayingPill {
    public final RenderNode node = new RenderNode("np");
    private final RenderNode ring = new RenderNode("np.ring");
    private final Animator animator;
    private final TextPaint title, sub;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), art = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint stroke = Icons.strokePaint();
    private final Matrix m = new Matrix();
    private Theme theme;
    private float s = 1;
    private String t = "", st = "";
    private Bitmap cover;
    private boolean playing, visible;
    private float pw, ph;

    public NowPlayingPill(Context c, Animator animator) {
        this.animator = animator;
        title = new TextPaint(Typography.paint(c, 26, 400, false));
        sub = new TextPaint(Typography.paint(c, 22, 300, false));
        node.setAlpha(0f);
        node.setClipToBounds(false);
        node.setHasOverlappingRendering(true);
        ring.setAlpha(0f);
        ring.setClipToBounds(false);
    }

    public void setScale(float s) {
        this.s = s;
        title.setTextSize(26 * s);
        sub.setTextSize(22 * s);
        record();
    }

    public void setTheme(Theme th) {
        theme = th;
        record();
    }

    public boolean visible() {
        return visible;
    }

    /** Alto que ocupa en el bloque del reloj (margen de 36 px incluido) si está visible. */
    public float blockHeight() {
        return visible ? (36 + 72) * s : 0;
    }

    public float width() {
        return pw;
    }

    public void set(String title, String artist, String app, Bitmap art, boolean playing) {
        boolean show = !title.isEmpty();
        t = title;
        st = artist.isEmpty() ? app : app.isEmpty() ? artist : artist + " · " + app;
        cover = art;
        this.playing = playing;
        record();
        if (show != visible) {
            visible = show;
            animator.animate(node, Animator.ALPHA, show ? 1f : 0f, 200);
        }
    }

    public void setFocused(boolean f) {
        animator.animate(ring, Animator.ALPHA, f ? 1f : 0f, 140);
    }

    private void record() {
        if (theme == null) return;
        float h = 72 * s, a = 56 * s, btn = 52 * s, pad = 10 * s, gap = 18 * s;
        String tt = TextUtils.ellipsize(t, title, 480 * s, TextUtils.TruncateAt.END).toString();
        String ss = TextUtils.ellipsize(st, sub, 480 * s, TextUtils.TruncateAt.END).toString();
        float tw = Math.max(title.measureText(tt), sub.measureText(ss));
        pw = pad + a + gap + tw + gap + btn + 12 * s;
        ph = h;
        node.setPosition(0, 0, Math.round(pw), Math.round(ph));
        float bx = pw - 12 * s - btn / 2, by = h / 2;
        ring.setPosition(0, 0, Math.round(pw), Math.round(ph));
        Layer.record(ring, Math.round(pw), Math.round(ph), c -> {
            stroke.setColor(theme.focusRing);
            stroke.setStrokeWidth(3 * s);
            c.drawCircle(bx, by, btn / 2 + 4 * s, stroke);
        });
        Layer.record(node, Math.round(pw), Math.round(ph), c -> {
            fill.setColor(theme.roundBg);
            c.drawRoundRect(0, 0, pw, ph, h / 2, h / 2, fill);
            float cx = pad + a / 2, cy = h / 2;
            if (cover != null) {
                BitmapShader sh = new BitmapShader(cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                float k = a / Math.min(cover.getWidth(), cover.getHeight());
                m.setScale(k, k);
                m.postTranslate(cx - cover.getWidth() * k / 2, cy - cover.getHeight() * k / 2);
                sh.setLocalMatrix(m);
                art.setShader(sh);
                c.drawCircle(cx, cy, a / 2, art);
                art.setShader(null);
            } else {
                fill.setColor(0xFF6E4A86);
                c.drawCircle(cx, cy, a / 2, fill);
            }
            float x = pad + a + gap;
            title.setColor(theme.text);
            sub.setColor(theme.textSecondary);
            c.drawText(tt, x, cy - 4 * s, title);
            c.drawText(ss, x, cy + 24 * s, sub);
            fill.setColor(theme.roundBg);
            c.drawCircle(bx, by, btn / 2, fill);
            fill.setColor(theme.text);
            stroke.setColor(theme.text);
            Icons.draw(c, playing ? Icons.PAUSE : Icons.PLAY, bx, by, 22 * s, fill, stroke);
            c.drawRenderNode(ring);
        });
    }
}
