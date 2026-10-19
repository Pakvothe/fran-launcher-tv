package pakvothe.franlauncher.tv.scene;

import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;

import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.theme.Theme;

/**
 * Panel flotante a la derecha: 700 px, a 48 px de los bordes, radio 28, sin blur.
 * Entra y sale con translationX + alfa (300 ms). El contenido es el nodo del Panel activo.
 */
public final class PanelLayer extends Layer {
    public static final float WIDTH = 700, MARGIN = 48, RADIUS = 28;
    private static final int MS = 300;

    private final Animator animator;
    private final RenderNode box = new RenderNode("panel.box");
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Panel current;
    private Theme theme;
    private boolean open;
    private final Runnable hidden = () -> {
        if (!open) {
            if (current != null) current.onHidden();
            current = null;
            invalidate();
        }
    };

    public PanelLayer(Animator animator) {
        super("panel");
        this.animator = animator;
        box.setClipToBounds(false);
        box.setHasOverlappingRendering(false);
        box.setAlpha(0f);
    }

    public int panelWidth() {
        return Math.round(WIDTH * s);
    }

    public int panelHeight() {
        return Math.round(h - 2 * MARGIN * s);
    }

    /** Borde izquierdo del panel en px de pantalla: el reloj se centra en el espacio que queda a la izquierda. */
    public float left() {
        return w - (MARGIN + WIDTH) * s;
    }

    public boolean isOpen() {
        return open;
    }

    public Panel current() {
        return current;
    }

    public void setTheme(Theme t) {
        theme = t;
        bg.setColor(t.panel);
        invalidate();
    }

    @Override
    protected void onSizeChanged() {
        int x = Math.round(left()), y = Math.round(MARGIN * s);
        box.setPosition(x, y, x + panelWidth(), y + panelHeight());
    }

    public void show(Panel p) {
        open = true;
        current = p;
        p.onShow();
        invalidate();
        if (box.getAlpha() == 0f) box.setTranslationX(60 * s);
        animator.animate(box, Animator.TX, 0f, MS);
        animator.animate(box, Animator.ALPHA, 1f, MS);
    }

    public void hide() {
        open = false;
        animator.animate(box, Animator.TX, 60 * s, MS);
        animator.animate(box, Animator.ALPHA, 0f, MS, hidden);
    }

    @Override
    protected void record(RecordingCanvas c) {
        if (current == null) return;
        float r = RADIUS * s;
        RecordingCanvas b = box.beginRecording(panelWidth(), panelHeight());
        try {
            b.drawRoundRect(0, 0, panelWidth(), panelHeight(), r, r, bg);
            b.drawRenderNode(current.content);
        } finally {
            box.endRecording();
        }
        c.drawRenderNode(box);
    }
}
