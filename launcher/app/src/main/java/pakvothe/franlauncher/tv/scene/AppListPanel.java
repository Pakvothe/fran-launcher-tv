package pakvothe.franlauncher.tv.scene;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.text.TextPaint;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pakvothe.franlauncher.tv.R;
import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.data.AppEntry;
import pakvothe.franlauncher.tv.data.Atlas;
import pakvothe.franlauncher.tv.data.Letters;
import pakvothe.franlauncher.tv.util.Icons;
import pakvothe.franlauncher.tv.util.Typography;

/**
 * Todas las apps, estilo rueda selectora: el ítem en foco queda siempre en el centro vertical; se mueve la lista.
 * Solo se mantienen grabados los ítems cercanos al foco, reciclando nodos de un pool fijo.
 */
public final class AppListPanel extends Panel {
    private static final float ITEM = 76, ICON = 44, TEXT = 30, HEADER = 128, INDEX_W = 56, PAD = 32;
    private static final int POOL = 25, RANGE = 12, ITEM_MS = 200, SCROLL_MS = 240;

    private final Animator animator;
    private final RenderNode header = new RenderNode("all.header");
    private final RenderNode list = new RenderNode("all.list");
    private final RenderNode index = new RenderNode("all.index");
    private final Slot[] slots = new Slot[POOL];
    private final TextPaint text, textBig;
    private final Paint title, count, letter, icon = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = Icons.strokePaint();
    private final Rect src = new Rect(), dst = new Rect();
    private final String titleText;
    private List<AppEntry> apps = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>();
    private Atlas icons;
    private int focus, letterFocus = -1;
    private boolean indexMode;
    private final boolean[] hasLetter = new boolean[Letters.COUNT];
    private float listTop, listH, itemH;

    private static final class Slot {
        final RenderNode node = new RenderNode("all.item");
        final RenderNode icon = new RenderNode("all.icon");
        final RenderNode text = new RenderNode("all.text");
        /** El nombre en foco (×1.4) se graba aparte y se cruza por alfa: escalar texto re-rasteriza glifos en cada frame (+4 MB nativos, medido). */
        final RenderNode big = new RenderNode("all.big");
        int bound = -1;

        Slot() {
            big.setAlpha(0f);
            for (RenderNode n : new RenderNode[] {node, icon, text, big}) {
                n.setClipToBounds(false);
                n.setHasOverlappingRendering(false);
            }
        }
    }

    public AppListPanel(Context c, Animator animator) {
        super("all");
        this.animator = animator;
        for (int i = 0; i < POOL; i++) slots[i] = new Slot();
        text = new TextPaint(Typography.paint(c, TEXT, 300, false));
        textBig = new TextPaint(Typography.paint(c, TEXT * 1.4f, 300, false));
        title = Typography.paint(c, 34, 300, false);
        count = Typography.paint(c, 26, 300, true);
        letter = Typography.paint(c, 20, 400, false);
        letter.setTextAlign(Paint.Align.CENTER);
        titleText = c.getString(R.string.all_apps);
        list.setClipToBounds(true);
        header.setClipToBounds(false);
        index.setClipToBounds(false);
    }

    /** Lista ya ordenada (Collator) y el conjunto de favoritas para la estrella. */
    public void setData(List<AppEntry> sorted, Set<String> favs, Atlas icons) {
        apps = sorted;
        favorites.clear();
        favorites.addAll(favs);
        this.icons = icons;
        java.util.Arrays.fill(hasLetter, false);
        for (AppEntry e : apps) hasLetter[Letters.bucket(e.label)] = true;
        for (Slot sl : slots) sl.bound = -1;
        focus = Math.max(0, Math.min(focus, apps.size() - 1));
        rebuild();
    }

    @Override
    protected void onLayout() {
        text.setTextSize(TEXT * s);
        textBig.setTextSize(TEXT * 1.4f * s);
        title.setTextSize(34 * s);
        count.setTextSize(26 * s);
        letter.setTextSize(20 * s);
        itemH = ITEM * s;
        listTop = HEADER * s;
        listH = ph - listTop - 24 * s;
        header.setPosition(0, 0, pw, Math.round(listTop));
        list.setPosition(0, Math.round(listTop), Math.round(pw - INDEX_W * s), Math.round(listTop + listH));
        index.setPosition(Math.round(pw - INDEX_W * s), Math.round(listTop), pw, Math.round(listTop + listH));
        for (Slot sl : slots) sl.bound = -1;
        rebuild();
    }

    @Override
    protected void onTheme() {
        text.setColor(theme.text);
        textBig.setColor(theme.text);
        title.setColor(theme.text);
        count.setColor(theme.textSecondary);
        for (Slot sl : slots) sl.bound = -1;
        rebuild();
    }

    private void rebuild() {
        if (theme == null || pw == 0) return;
        Layer.record(header, pw, Math.round(listTop), c -> {
            float cy = listTop / 2 + 6 * s;
            fill.setColor(theme.roundBg);
            c.drawCircle(PAD * s + 26 * s, cy, 26 * s, fill);
            fill.setColor(theme.text);
            stroke.setColor(theme.text);
            Icons.draw(c, Icons.BACK, PAD * s + 26 * s, cy, 26 * s, fill, stroke);
            c.drawText(titleText, PAD * s + 72 * s, cy + 12 * s, title);
            String n = String.valueOf(apps.size());
            c.drawText(n, pw - PAD * s - count.measureText(n), cy + 10 * s, count);
            fill.setColor(theme.divider);
            c.drawRect(PAD * s, listTop - 2 * s, pw - PAD * s, listTop - s, fill);
        });
        recordIndex();
        bindAround(focus);
        applyFocus(false);
        Layer.record(content, pw, ph, c -> {
            c.drawRenderNode(header);
            c.drawRenderNode(list);
            c.drawRenderNode(index);
        });
    }

    private void recordList() {
        Layer.record(list, Math.round(pw - INDEX_W * s), Math.round(listH), c -> {
            for (Slot sl : slots) if (sl.bound >= 0) c.drawRenderNode(sl.node);
        });
    }

    /** Asegura que los ítems a ±RANGE del foco estén grabados en algún nodo del pool. */
    private void bindAround(int f) {
        boolean changed = false;
        for (int i = Math.max(0, f - RANGE); i <= Math.min(apps.size() - 1, f + RANGE); i++) {
            Slot sl = slots[i % POOL];
            if (sl.bound == i) continue;
            bind(sl, i);
            changed = true;
        }
        if (changed) recordList();
    }

    private void bind(Slot sl, int i) {
        sl.bound = i;
        AppEntry e = apps.get(i);
        float lw = pw - INDEX_W * s, y = i * itemH;
        sl.node.setPosition(0, Math.round(y), Math.round(lw), Math.round(y + itemH));
        float ix = PAD * s, is = ICON * s, cy = itemH / 2;
        sl.icon.setPosition(Math.round(ix), Math.round(cy - is / 2), Math.round(ix + is), Math.round(cy + is / 2));
        sl.icon.setPivotX(is / 2);
        sl.icon.setPivotY(is / 2);
        Layer.record(sl.icon, Math.round(is), Math.round(is), c -> {
            if (icons != null && e.icon >= 0) {
                icons.cell(e.icon, src);
                dst.set(0, 0, Math.round(is), Math.round(is));
                c.drawBitmap(icons.bitmap, src, dst, icon);
            } else {
                fill.setColor(e.color);
                c.drawRoundRect(0, 0, is, is, 10 * s, 10 * s, fill);
            }
        });
        float tx = ix + is + 22 * s, tw = lw - tx - 16 * s;
        boolean fav = favorites.contains(e.component);
        recordLabel(sl.text, e.label, text, 1f, tx, tw, fav);
        recordLabel(sl.big, e.label, textBig, 1.4f, tx, tw, fav);
        Layer.record(sl.node, Math.round(lw), Math.round(itemH), c -> {
            c.drawRenderNode(sl.icon);
            c.drawRenderNode(sl.text);
            c.drawRenderNode(sl.big);
        });
    }

    private void recordLabel(RenderNode n, String full, TextPaint p, float k, float tx, float tw, boolean fav) {
        String label = TextUtils.ellipsize(full, p, tw - 40 * s * k, TextUtils.TruncateAt.END).toString();
        n.setPosition(Math.round(tx), 0, Math.round(tx + tw), Math.round(itemH));
        Layer.record(n, Math.round(tw), Math.round(itemH), c -> {
            c.drawText(label, 0, itemH / 2 + TEXT * k * s * 0.35f, p);
            if (fav) {
                fill.setColor(theme.textSecondary);
                Icons.draw(c, Icons.STAR, p.measureText(label) + 18 * s * k, itemH / 2, 16 * s * k, fill, stroke);
            }
        });
    }

    private void recordIndex() {
        float lh = listH / Letters.COUNT;
        Layer.record(index, Math.round(INDEX_W * s), Math.round(listH), c -> {
            for (int l = 0; l < Letters.COUNT; l++) {
                float cy = l * lh + lh / 2;
                boolean cur = l == letterFocus;
                if (cur && indexMode) {
                    fill.setColor(theme.focusRing);
                    c.drawCircle(INDEX_W * s / 2, cy, lh / 2, fill);
                }
                letter.setColor(cur && indexMode ? (theme.light ? 0xFFFFFFFF : 0xFF16171D)
                    : cur ? theme.text : hasLetter[l] ? (theme.text & 0x00FFFFFF) | 0x80000000 : (theme.text & 0x00FFFFFF) | 0x1F000000);
                c.drawText(Letters.LABELS, l, l + 1, INDEX_W * s / 2, cy + 7 * s, letter);
            }
        });
    }

    public void setFocus(int i, boolean animate) {
        focus = i;
        bindAround(i);
        applyFocus(animate);
    }

    /** Modo índice: la letra elegida se resalta; null = vuelve a la lista. */
    public void setIndex(int letterIdx, boolean active) {
        indexMode = active;
        letterFocus = letterIdx;
        recordIndex();
    }

    private void applyFocus(boolean animate) {
        if (apps.isEmpty()) return;
        int d = animate ? ITEM_MS : 0;
        for (Slot sl : slots) {
            if (sl.bound < 0) continue;
            int dist = Math.abs(sl.bound - focus);
            boolean f = dist == 0;
            animator.animate(sl.node, Animator.ALPHA, f ? 1f : Math.max(0.16f, 0.78f - 0.13f * dist), d);
            animator.animate(sl.icon, Animator.SCALE, f ? 1.15f : 1f, d);
            animator.animate(sl.text, Animator.ALPHA, f ? 0f : 1f, d);
            animator.animate(sl.big, Animator.ALPHA, f ? 1f : 0f, d);
        }
        float target = listH / 2 - (focus * itemH + itemH / 2);
        for (Slot sl : slots) animator.animate(sl.node, Animator.TY, target, animate ? SCROLL_MS : 0);
        if (!indexMode && letterFocus != Letters.bucket(apps.get(focus).label)) {
            letterFocus = Letters.bucket(apps.get(focus).label);
            recordIndex();
        }
    }

    @Override
    public void onHidden() {
        for (Slot sl : slots) {
            sl.bound = -1;
            sl.node.discardDisplayList();
            sl.icon.discardDisplayList();
            sl.text.discardDisplayList();
            sl.big.discardDisplayList();
        }
        list.discardDisplayList();
    }

    public int size() {
        return apps.size();
    }

    public AppEntry get(int i) {
        return apps.get(i);
    }

    /** Rect (relativo al panel) del ítem i, con el foco centrado. */
    public void bounds(int i, android.graphics.Rect out) {
        float y = listTop + listH / 2 + (i - focus) * itemH - itemH / 2;
        out.set(0, Math.round(y), Math.round(pw - INDEX_W * s), Math.round(y + itemH));
    }

    public AppEntry focused() {
        return focus >= 0 && focus < apps.size() ? apps.get(focus) : null;
    }
}
