package pakvothe.franlauncher.tv.scene;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RenderNode;

import java.util.ArrayList;
import java.util.List;

import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.util.Icons;
import pakvothe.franlauncher.tv.util.Typography;

/** Panel de filas (Entradas, Ajustes): encabezado + filas con título, valor a la derecha y foco deslizante. */
public class RowsPanel extends Panel {
    private static final float HEADER = 128, PAD = 32;
    private static final int MS = 160;

    public static final class Row {
        public String title, value = "";
        /** Color del punto de estado (0 = sin punto). */
        public int dot;
        public boolean dim;

        public Row(String title) {
            this.title = title;
        }
    }

    private final Animator animator;
    private final RenderNode header = new RenderNode("rows.header");
    private final RenderNode body = new RenderNode("rows.body");
    private final RenderNode highlight = new RenderNode("rows.focus");
    private final Paint title, rowTitle, rowValue, footer, fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = Icons.strokePaint();
    private final String titleText;
    private final float rowH;
    protected final List<Row> rows = new ArrayList<>();
    private String footerText = "";
    private int focus;

    public RowsPanel(Context c, Animator animator, String title, float rowH) {
        super("rows");
        this.animator = animator;
        this.titleText = title;
        this.rowH = rowH;
        this.title = Typography.paint(c, 34, 300, false);
        rowTitle = Typography.paint(c, 30, 300, false);
        rowValue = Typography.paint(c, 26, 300, false);
        rowValue.setTextAlign(Paint.Align.RIGHT);
        footer = Typography.paint(c, 22, 300, false);
        footer.setTextAlign(Paint.Align.CENTER);
        highlight.setHasOverlappingRendering(false);
    }

    public void setFooter(String f) {
        footerText = f;
    }

    @Override
    protected void onLayout() {
        title.setTextSize(34 * s);
        rowTitle.setTextSize(30 * s);
        rowValue.setTextSize(26 * s);
        footer.setTextSize(22 * s);
        rebuild();
    }

    @Override
    protected void onTheme() {
        title.setColor(theme.text);
        rowTitle.setColor(theme.text);
        rowValue.setColor(theme.textSecondary);
        footer.setColor(theme.seconds);
        rebuild();
    }

    /** Las subclases cambian rows y llaman a esto. */
    protected void rebuild() {
        if (theme == null || pw == 0) return;
        float top = HEADER * s, rh = rowH * s, x0 = PAD * s, x1 = pw - PAD * s;
        Layer.record(header, pw, Math.round(top), c -> {
            float cy = top / 2 + 6 * s;
            fill.setColor(theme.roundBg);
            c.drawCircle(x0 + 26 * s, cy, 26 * s, fill);
            fill.setColor(theme.text);
            stroke.setColor(theme.text);
            Icons.draw(c, Icons.BACK, x0 + 26 * s, cy, 26 * s, fill, stroke);
            c.drawText(titleText, x0 + 72 * s, cy + 12 * s, title);
            fill.setColor(theme.divider);
            c.drawRect(x0, top - 2 * s, x1, top - s, fill);
        });
        header.setPosition(0, 0, pw, Math.round(top));
        highlight.setPosition(Math.round(x0 - 12 * s), Math.round(top + 12 * s), Math.round(x1 + 12 * s),
            Math.round(top + 12 * s + rh));
        float hw = x1 - x0 + 24 * s;
        Layer.record(highlight, Math.round(hw), Math.round(rh), c -> {
            fill.setColor(theme.roundBg);
            c.drawRoundRect(0, 0, hw, rh, 16 * s, 16 * s, fill);
        });
        body.setPosition(0, 0, pw, ph);
        Layer.record(body, pw, ph, c -> {
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                float y = top + 12 * s + i * rh, base = y + rh / 2 + 10 * s;
                int a = r.dim ? 153 : 255;
                rowTitle.setAlpha(a);
                float tx = x0 + 8 * s;
                if (r.dot != 0) {
                    fill.setColor(r.dot);
                    fill.setAlpha(a);
                    c.drawCircle(tx + 7 * s, y + rh / 2, 7 * s, fill);
                    tx += 30 * s;
                }
                c.drawText(r.title, tx, base, rowTitle);
                rowValue.setAlpha(r.dim ? 153 : 255);
                c.drawText(r.value, x1 - 8 * s, base - 2 * s, rowValue);
            }
            rowTitle.setAlpha(255);
            rowValue.setAlpha(255);
            if (!footerText.isEmpty()) c.drawText(footerText, pw / 2f, ph - 18 * s, footer);
        });
        Layer.record(content, pw, ph, c -> {
            c.drawRenderNode(header);
            c.drawRenderNode(highlight);
            c.drawRenderNode(body);
        });
        setFocus(focus, false);
    }

    public void setFocus(int i, boolean animate) {
        focus = i;
        animator.animate(highlight, Animator.TY, i * rowH * s, animate ? MS : 0);
        highlight.setAlpha(rows.isEmpty() ? 0f : 1f);
    }

    public String label(int i) {
        Row r = rows.get(i);
        return r.value.isEmpty() ? r.title : r.title + ", " + r.value;
    }

    public void bounds(int i, android.graphics.Rect out) {
        float y = HEADER * s + 12 * s + i * rowH * s;
        out.set(0, Math.round(y), pw, Math.round(y + rowH * s));
    }

    public int size() {
        return rows.size();
    }
}
