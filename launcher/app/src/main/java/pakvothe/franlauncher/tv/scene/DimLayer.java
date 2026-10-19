package pakvothe.franlauncher.tv.scene;

import android.graphics.RecordingCanvas;

/** Rectángulo negro con alfa sobre todo: ambiente (OLED) y noche. Un nodo, costo despreciable. */
public final class DimLayer extends Layer {
    public DimLayer() {
        super("dim");
        node.setAlpha(0f);
    }

    @Override
    protected void record(RecordingCanvas c) {
        c.drawColor(0xFF000000);
    }
}
