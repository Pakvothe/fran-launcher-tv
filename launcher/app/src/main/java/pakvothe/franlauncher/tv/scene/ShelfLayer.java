package pakvothe.franlauncher.tv.scene;

import android.content.Context;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import pakvothe.franlauncher.tv.R;
import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.app.FocusModel;
import pakvothe.franlauncher.tv.data.AppEntry;
import pakvothe.franlauncher.tv.data.Atlas;
import pakvothe.franlauncher.tv.theme.Theme;
import pakvothe.franlauncher.tv.util.Icons;
import pakvothe.franlauncher.tv.util.Shadow;
import pakvothe.franlauncher.tv.util.Typography;

/**
 * Fila de favoritas + botones redondos. Un nodo por recuadro; el foco es solo scale/translation/alpha
 * de nodos ya grabados. El nombre se muestra debajo del recuadro con foco.
 */
public final class ShelfLayer extends Layer {
    private static final int FOCUS_MS = 140, PRESS_MS = 80, SCROLL_MS = 240;
    private static final float TW = 232, TH = 130, RADIUS = 14, GAP = 28, BOTTOM = 84, BTN = 64;

    private final Context context;
    private final Animator animator;
    private final RenderNode row = new RenderNode("shelf.row");
    private final RenderNode label = new RenderNode("shelf.label");
    /** Flechas de Mover sobre el recuadro con foco. */
    private final RenderNode arrows = new RenderNode("shelf.arrows");
    private boolean moving;
    private final List<Item> items = new ArrayList<>();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), ring = Icons.strokePaint();
    private final Paint iconFill = new Paint(Paint.ANTI_ALIAS_FLAG), iconStroke = Icons.strokePaint();
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint labelPaint;
    private final Rect cell = new Rect();
    private final Matrix matrix = new Matrix();
    private final String[] buttonNames;
    private Shadow shadowRest, shadowFocus;
    private Theme theme;
    private List<AppEntry> apps = new ArrayList<>();
    private Atlas banners, icons;
    private boolean hasInputs;
    private int focus = -1;
    private float rowWidth, rowLeft, top;
    private String labelText = "";
    private float labelX;
    private boolean labelVisible = true;
    private final Recorder labelRecorder = this::recordLabel;
    /** Tope de la franja de la fila: el nodo row cubre solo [band, h], no la pantalla entera. */
    private int band;
    /**
     * En reposo la fila es una capa compuesta: cada frame de los orbes la dibuja como una textura en vez de
     * re-ejecutar ~15 operaciones (medido: CPU en reposo 2,3% → 1,5%). Se apaga mientras anima el foco.
     */
    private Runnable host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable layerOn = () -> {
        row.setUseCompositingLayer(true, null);
        invalidate();
        if (host != null) host.run();
    };


    /** Un recuadro o botón: nodo propio + nodo hijo con la sombra profunda y el anillo (visibles por alfa). */
    private static final class Item {
        final RenderNode node = new RenderNode("shelf.item");
        final RenderNode focus = new RenderNode("shelf.focus");
        AppEntry app;
        int button = -1;
        float x, w, h;

        Item() {
            node.setClipToBounds(false);
            focus.setClipToBounds(false);
            node.setHasOverlappingRendering(false);
            focus.setHasOverlappingRendering(false);
            focus.setAlpha(0f);
        }
    }

    public ShelfLayer(Context context, Animator animator) {
        super("shelf");
        this.context = context;
        this.animator = animator;
        row.setClipToBounds(false);
        label.setClipToBounds(false);
        label.setHasOverlappingRendering(false);
        arrows.setClipToBounds(false);
        arrows.setAlpha(0f);
        labelPaint = Typography.paint(context, 26, 300, false);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        buttonNames = new String[] {context.getString(R.string.all_apps), context.getString(R.string.inputs),
            context.getString(R.string.settings)};
    }

    /** Pide un frame al Stage cuando cambia la capa. */
    public void setInvalidator(Runnable r) {
        host = r;
    }

    private void animating(int ms) {
        handler.removeCallbacks(layerOn);
        row.setUseCompositingLayer(false, null);
        handler.postDelayed(layerOn, ms + 60);
    }

    public void setTheme(Theme t) {
        theme = t;
        labelPaint.setColor(t.textSecondary);
        if (shadowRest != null) {
            shadowRest.setColor(t.shadowRest);
            shadowFocus.setColor(t.shadowFocus);
        }
        rebuild();
    }

    public void setData(List<AppEntry> apps, Atlas banners, Atlas icons, boolean hasInputs) {
        this.apps = apps;
        this.banners = banners;
        this.icons = icons;
        this.hasInputs = hasInputs;
        rebuild();
    }

    @Override
    protected void onSizeChanged() {
        labelPaint.setTextSize(26 * s);
        shadowRest = new Shadow(RADIUS * s, 22 * s);
        shadowFocus = new Shadow(RADIUS * s, 34 * s);
        if (theme != null) {
            shadowRest.setColor(theme.shadowRest);
            shadowFocus.setColor(theme.shadowFocus);
        }
        label.setPosition(0, 0, w, h);
        arrows.setPosition(0, 0, w, h);
        rebuild();
    }

    private void rebuild() {
        if (w == 0 || theme == null) return;
        int n = apps.size() + (hasInputs ? 3 : 2);
        while (items.size() < n) items.add(new Item());
        while (items.size() > n) items.remove(items.size() - 1);
        float tw = TW * s, th = TH * s, gap = GAP * s, btn = BTN * s;
        top = h - BOTTOM * s - th;
        band = Math.round(top - 70 * s);
        row.setPosition(0, band, w, h);
        float x = 0;
        for (int i = 0; i < n; i++) {
            Item it = items.get(i);
            boolean isApp = i < apps.size();
            it.app = isApp ? apps.get(i) : null;
            it.button = isApp ? -1 : buttonFor(i - apps.size());
            it.w = isApp ? tw : btn;
            it.h = isApp ? th : btn;
            it.x = x;
            float y = (isApp ? top : top + (th - btn) / 2) - band;
            it.node.setPosition(Math.round(x), Math.round(y), Math.round(x + it.w), Math.round(y + it.h));
            it.node.setPivotX(it.w / 2);
            it.node.setPivotY(it.h / 2);
            it.focus.setPosition(0, 0, Math.round(it.w), Math.round(it.h));
            recordItem(it);
            x += it.w + gap;
        }
        rowWidth = x - gap;
        rowLeft = (w - rowWidth) / 2;
        for (Item it : items) it.node.offsetLeftAndRight(Math.round(rowLeft));
        record(row, w, h - band, c -> {
            for (Item it : items) c.drawRenderNode(it.node);
        });
        if (focus >= n) focus = n - 1;
        if (focus >= 0) {
            applyFocus(focus, -1, false);
            scrollTo(focus, false);
        }
        animating(0);
        invalidate();
    }

    private int buttonFor(int b) {
        return hasInputs || b == 0 ? b : FocusModel.BUTTON_SETTINGS;
    }

    private void recordItem(Item it) {
        float iw = it.w, ih = it.h;
        record(it.node, Math.round(iw), Math.round(ih), c -> {
            if (it.app != null) {
                shadowRest.draw(c, 0, 0, iw, ih, 8 * s);
                c.drawRenderNode(it.focus);
                drawTile(c, it.app, iw, ih);
            } else {
                c.drawRenderNode(it.focus);
                fill.setColor(theme.roundBg);
                c.drawCircle(iw / 2, ih / 2, iw / 2, fill);
                iconFill.setColor(theme.text);
                iconStroke.setColor(theme.text);
                Icons.draw(c, it.button == FocusModel.BUTTON_ALL ? Icons.GRID
                    : it.button == FocusModel.BUTTON_INPUTS ? Icons.INPUTS : Icons.SETTINGS, iw / 2, ih / 2, 30 * s,
                    iconFill, iconStroke);
            }
        });
        record(it.focus, Math.round(iw), Math.round(ih), c -> {
            ring.setColor(theme.focusRing);
            ring.setStrokeWidth(3 * s);
            float o = 3 * s;
            if (it.app != null) {
                shadowFocus.draw(c, 0, 0, iw, ih, 16 * s);
                c.drawRoundRect(-o, -o, iw + o, ih + o, RADIUS * s + o, RADIUS * s + o, ring);
            } else {
                c.drawCircle(iw / 2, ih / 2, iw / 2 + o, ring);
            }
        });
    }

    private void drawTile(RecordingCanvas c, AppEntry app, float tw, float th) {
        float r = RADIUS * s;
        if (app.banner >= 0 && banners != null) {
            banners.cell(app.banner, cell);
            BitmapShader sh = new BitmapShader(banners.bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            matrix.setRectToRect(new android.graphics.RectF(cell), new android.graphics.RectF(0, 0, tw, th),
                Matrix.ScaleToFit.FILL);
            sh.setLocalMatrix(matrix);
            bitmapPaint.setShader(sh);
            c.drawRoundRect(0, 0, tw, th, r, r, bitmapPaint);
            bitmapPaint.setShader(null);
        } else {
            fill.setColor(app.color);
            c.drawRoundRect(0, 0, tw, th, r, r, fill);
            if (app.icon >= 0 && icons != null) {
                icons.cell(app.icon, cell);
                float is = 64 * s;
                c.drawBitmap(icons.bitmap, cell, new Rect(Math.round((tw - is) / 2), Math.round((th - is) / 2),
                    Math.round((tw + is) / 2), Math.round((th + is) / 2)), bitmapPaint);
            }
        }
    }

    /** Mueve el foco (−1 = sin foco). */
    public void setFocus(int index, boolean animate) {
        int old = focus;
        focus = index;
        if (w == 0 || items.isEmpty()) return;
        animating(animate ? SCROLL_MS : 0);
        applyFocus(index, old, animate);
        scrollTo(index, animate);
        recordArrows();
    }

    private void applyFocus(int index, int old, boolean animate) {
        int d = animate ? FOCUS_MS : 0;
        if (old >= 0 && old < items.size() && old != index) {
            Item o = items.get(old);
            animator.animate(o.node, Animator.SCALE, 1f, d);
            animator.animate(o.node, Animator.TY, 0f, d);
            animator.animate(o.focus, Animator.ALPHA, 0f, d);
        }
        if (index < 0 || index >= items.size()) {
            animator.animate(label, Animator.ALPHA, 0f, d);
            return;
        }
        Item it = items.get(index);
        animator.animate(it.node, Animator.SCALE, 1.08f, d);
        animator.animate(it.node, Animator.TY, -6 * s, d);
        animator.animate(it.focus, Animator.ALPHA, 1f, d);
        labelText = it.app != null ? it.app.label : buttonNames[it.button];
        labelX = rowLeft + it.x + it.w / 2;
        record(label, w, h, labelRecorder);
        label.setAlpha(0f);
        if (labelVisible) animator.animate(label, Animator.ALPHA, 1f, d);
    }

    private void recordLabel(RecordingCanvas c) {
        c.drawText(labelText, labelX, top + TH * s + 46 * s, labelPaint);
    }

    /** Si la fila no entra, se desplaza para que el foco quede dentro de márgenes seguros. */
    private void scrollTo(int index, boolean animate) {
        float margin = 96 * s, tx = 0;
        if (rowWidth > w - 2 * margin && index >= 0) {
            Item it = items.get(index);
            float left = rowLeft + it.x, right = left + it.w;
            float cur = animator.target(row, Animator.TX);
            tx = cur;
            if (left + tx < margin) tx = margin - left;
            if (right + tx > w - margin) tx = w - margin - right;
            float minTx = w - margin - (rowLeft + rowWidth), maxTx = margin - rowLeft;
            tx = Math.max(minTx, Math.min(maxTx, tx));
        }
        animator.animate(row, Animator.TX, tx, animate ? SCROLL_MS : 0);
        animator.animate(label, Animator.TX, tx, animate ? SCROLL_MS : 0);
    }

    /** Recuadro presionado: escala 1.02 y alfa 0.85 por 80 ms; el intent se lanza en paralelo. */
    public void press(int index) {
        if (index < 0 || index >= items.size()) return;
        Item it = items.get(index);
        animating(PRESS_MS + FOCUS_MS);
        animator.animate(it.node, Animator.SCALE, 1.02f, PRESS_MS, () -> {
            animator.animate(it.node, Animator.SCALE, focus == index ? 1.08f : 1f, FOCUS_MS);
            animator.animate(it.node, Animator.ALPHA, 1f, FOCUS_MS);
        });
        animator.animate(it.node, Animator.ALPHA, 0.85f, PRESS_MS);
    }

    public RenderNode rowNode() {
        return row;
    }

    /** Centro horizontal en pantalla del ítem i (incluye el desplazamiento de la fila). */
    public float centerX(int i) {
        if (i < 0 || i >= items.size()) return w / 2f;
        Item it = items.get(i);
        return rowLeft + it.x + it.w / 2 + animator.target(row, Animator.TX);
    }

    public float top() {
        return top;
    }

    public void setMoving(boolean on) {
        moving = on;
        recordArrows();
        animator.animate(arrows, Animator.ALPHA, on ? 1f : 0f, FOCUS_MS);
        labelVisible = !on;
        animator.animate(label, Animator.ALPHA, on ? 0f : 1f, FOCUS_MS);
    }

    private void recordArrows() {
        if (!moving || focus < 0 || focus >= items.size()) return;
        Item it = items.get(focus);
        float cx = centerX(focus), cy = top + TH * s / 2 - 6 * s, dx = it.w / 2 * 1.08f + 26 * s;
        record(arrows, w, h, c -> {
            iconFill.setColor(theme.text);
            iconStroke.setColor(theme.text);
            iconStroke.setStrokeWidth(4 * s);
            float a = 12 * s;
            c.drawLine(cx - dx + a / 2, cy - a, cx - dx - a / 2, cy, iconStroke);
            c.drawLine(cx - dx - a / 2, cy, cx - dx + a / 2, cy + a, iconStroke);
            c.drawLine(cx + dx - a / 2, cy - a, cx + dx + a / 2, cy, iconStroke);
            c.drawLine(cx + dx + a / 2, cy, cx + dx - a / 2, cy + a, iconStroke);
        });
    }

    @Override
    protected void record(RecordingCanvas c) {
        c.drawRenderNode(row);
        c.drawRenderNode(label);
        c.drawRenderNode(arrows);
    }
}
