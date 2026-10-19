package pakvothe.franlauncher.tv.scene;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;

import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.theme.Theme;
import pakvothe.franlauncher.tv.util.Shadow;
import pakvothe.franlauncher.tv.util.Typography;

/** Tarjeta de menú de 400 px, radio 22: nombre de la app y opciones de 64 px. El foco es un nodo que se desliza. */
public final class MenuLayer extends Layer {
    private static final float W = 400, R = 22, OPT = 64, PAD = 16, TITLE = 76;
    private static final int MS = 140;

    private final Animator animator;
    private final RenderNode card = new RenderNode("menu.card");
    private final RenderNode highlight = new RenderNode("menu.focus");
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG), hl = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint title, option;
    private Shadow shadow;
    private Theme theme;
    private String name = "";
    private String[] options = new String[0];
    private boolean open;

    public MenuLayer(Context c, Animator animator) {
        super("menu");
        this.animator = animator;
        title = Typography.paint(c, 26, 300, false);
        option = Typography.paint(c, 28, 300, false);
        card.setClipToBounds(false);
        card.setHasOverlappingRendering(true);
        card.setAlpha(0f);
        highlight.setHasOverlappingRendering(false);
    }

    public void setTheme(Theme t) {
        theme = t;
        bg.setColor(t.light ? 0xFFF4F5F8 : 0xFF1E2133);
        hl.setColor(t.roundBg);
        title.setColor(t.textSecondary);
        option.setColor(t.text);
        if (shadow != null) shadow.setColor(t.shadowFocus);
    }

    @Override
    protected void onSizeChanged() {
        title.setTextSize(26 * s);
        option.setTextSize(28 * s);
        shadow = new Shadow(R * s, 40 * s);
        if (theme != null) shadow.setColor(theme.shadowFocus);
    }

    public boolean isOpen() {
        return open;
    }

    /** cx = centro horizontal deseado; bottom = borde inferior de la tarjeta (se ajusta a márgenes seguros). */
    public void show(String appName, String[] opts, float cx, float bottom) {
        name = appName;
        options = opts;
        open = true;
        float cw = W * s, chh = (TITLE + opts.length * OPT + PAD) * s, m = 48 * s;
        float left = Math.max(m, Math.min(w - m - cw, cx - cw / 2));
        float top = Math.max(m, Math.min(h - m - chh, bottom - chh));
        card.setPosition(Math.round(left), Math.round(top), Math.round(left + cw), Math.round(top + chh));
        float ow = cw - 2 * PAD * s;
        highlight.setPosition(Math.round(PAD * s), Math.round(TITLE * s), Math.round(PAD * s + ow),
            Math.round((TITLE + OPT) * s));
        record(highlight, Math.round(ow), Math.round(OPT * s), c -> c.drawRoundRect(0, 0, ow, OPT * s, 14 * s, 14 * s, hl));
        highlight.setTranslationY(0);
        record(card, Math.round(cw), Math.round(chh), c -> {
            shadow.draw(c, 0, 0, cw, chh, 12 * s);
            c.drawRoundRect(0, 0, cw, chh, R * s, R * s, bg);
            c.drawText(name, (PAD + 16) * s, 48 * s, title);
            c.drawRenderNode(highlight);
            for (int i = 0; i < options.length; i++)
                c.drawText(options[i], (PAD + 16) * s, (TITLE + i * OPT + OPT / 2 + 10) * s, option);
        });
        card.setTranslationY(12 * s);
        animator.animate(card, Animator.ALPHA, 1f, MS);
        animator.animate(card, Animator.TY, 0f, MS);
        invalidate();
    }

    public String option(int i) {
        return options[i];
    }

    public int optionCount() {
        return options.length;
    }

    public void bounds(int i, android.graphics.Rect out) {
        float t = card.getTop() + (TITLE + i * OPT) * s;
        out.set(card.getLeft(), Math.round(t), card.getRight(), Math.round(t + OPT * s));
    }

    public void setFocus(int i) {
        animator.animate(highlight, Animator.TY, i * OPT * s, MS);
    }

    public void hide(boolean animate) {
        open = false;
        animator.animate(card, Animator.ALPHA, 0f, animate ? MS : 0);
    }

    @Override
    protected void record(RecordingCanvas c) {
        c.drawRenderNode(card);
    }
}
