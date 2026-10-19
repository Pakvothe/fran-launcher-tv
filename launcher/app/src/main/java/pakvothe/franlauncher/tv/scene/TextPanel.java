package pakvothe.franlauncher.tv.scene;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RenderNode;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.util.Icons;
import pakvothe.franlauncher.tv.util.Typography;

/** Texto largo con scroll por pasos (▲▼): Licencias y "Configurar como inicio". Atrás vuelve a Ajustes. */
public final class TextPanel extends Panel {
    private static final float HEADER = 128, PAD = 40;
    private final Animator animator;
    private final RenderNode header = new RenderNode("text.header");
    private final RenderNode clip = new RenderNode("text.clip");
    private final RenderNode body = new RenderNode("text.body");
    private final Paint title, fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = Icons.strokePaint();
    private final TextPaint text;
    private String titleText = "", bodyText = "";
    private StaticLayout layout;
    private float step;

    public TextPanel(Context c, Animator animator) {
        super("text");
        this.animator = animator;
        title = Typography.paint(c, 34, 300, false);
        text = new TextPaint(Typography.paint(c, 24, 300, false));
        clip.setClipToBounds(true);
    }

    public void setText(String t, String b) {
        titleText = t;
        bodyText = b;
        layout = null;
        rebuild();
    }

    /** Cantidad de pasos de scroll (para el FocusModel). */
    public int steps() {
        if (layout == null) return 1;
        float visible = ph - HEADER * s - PAD * s;
        return Math.max(1, (int) Math.ceil(Math.max(0, layout.getHeight() - visible) / step) + 1);
    }

    public void scrollTo(int i) {
        animator.animate(body, Animator.TY, -i * step, 200);
    }

    @Override
    protected void onLayout() {
        title.setTextSize(34 * s);
        text.setTextSize(24 * s);
        layout = null;
        rebuild();
    }

    @Override
    protected void onTheme() {
        title.setColor(theme.text);
        text.setColor(theme.textSecondary);
        layout = null;
        rebuild();
    }

    private void rebuild() {
        if (theme == null || pw == 0) return;
        float top = HEADER * s, x0 = PAD * s * 0.8f, tw = pw - 2 * PAD * s;
        if (layout == null) layout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length(), text, Math.round(tw))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0, 1.15f).build();
        step = 6 * text.getFontSpacing();
        header.setPosition(0, 0, pw, Math.round(top));
        Layer.record(header, pw, Math.round(top), c -> {
            float cy = top / 2 + 6 * s;
            fill.setColor(theme.roundBg);
            c.drawCircle(x0 + 26 * s, cy, 26 * s, fill);
            fill.setColor(theme.text);
            stroke.setColor(theme.text);
            Icons.draw(c, Icons.BACK, x0 + 26 * s, cy, 26 * s, fill, stroke);
            c.drawText(titleText, x0 + 72 * s, cy + 12 * s, title);
        });
        clip.setPosition(Math.round(PAD * s), Math.round(top), Math.round(PAD * s + tw), ph - Math.round(PAD * s / 2));
        body.setPosition(0, 0, Math.round(tw), layout.getHeight());
        body.setTranslationY(0);
        Layer.record(body, Math.round(tw), layout.getHeight(), layout::draw);
        Layer.record(clip, Math.round(tw), Math.round(ph - top), c -> c.drawRenderNode(body));
        Layer.record(content, pw, ph, c -> {
            c.drawRenderNode(header);
            c.drawRenderNode(clip);
        });
    }
}
