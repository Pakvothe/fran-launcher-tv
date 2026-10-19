package pakvothe.franlauncher.tv.scene;

import android.graphics.RenderNode;

import pakvothe.franlauncher.tv.theme.Theme;

/** Contenido de un panel lateral. Cada uno es dueño de su nodo y se re-graba solo cuando cambia. */
public abstract class Panel {
    public final RenderNode content;
    protected int pw, ph;
    protected float s = 1f;
    protected Theme theme;

    protected Panel(String name) {
        content = new RenderNode(name);
        content.setClipToBounds(false);
        content.setHasOverlappingRendering(false);
    }

    public void layout(int pw, int ph, float s) {
        this.pw = pw;
        this.ph = ph;
        this.s = s;
        content.setPosition(0, 0, pw, ph);
        onLayout();
    }

    public void setTheme(Theme t) {
        theme = t;
        onTheme();
    }

    protected abstract void onLayout();

    protected abstract void onTheme();

    /** Se llama al abrir el panel. */
    public void onShow() {}

    /** Se llama cuando el panel terminó de salir: soltar lo que se pueda regrabar barato. */
    public void onHidden() {}
}
