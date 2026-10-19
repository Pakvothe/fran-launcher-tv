package pakvothe.franlauncher.tv.app;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Como todo se dibuja en una sola View, TalkBack ve nodos virtuales: lo que está en pantalla en el estado
 * actual (recuadros y botones, ítems, filas, opciones). Si no hay servicio de accesibilidad activo, no hace nada.
 */
final class A11y extends AccessibilityNodeProvider {
    interface Source {
        /** Llena etiquetas y rects (en coordenadas de la View) y devuelve el índice con foco, o -1. */
        int collect(List<String> labels, List<Rect> rects);
    }

    private final View host;
    private final Source source;
    private final AccessibilityManager am;
    private final List<String> labels = new ArrayList<>();
    private final List<Rect> rects = new ArrayList<>();
    private final int[] loc = new int[2];
    private int focused = -1;

    A11y(View host, Source source) {
        this.host = host;
        this.source = source;
        am = host.getContext().getSystemService(AccessibilityManager.class);
    }

    boolean active() {
        return am != null && am.isEnabled();
    }

    private void refresh() {
        labels.clear();
        rects.clear();
        focused = source.collect(labels, rects);
    }

    /** Llamar cuando cambia el foco o el estado: anuncia el ítem con foco. */
    void onFocusChanged() {
        if (!active()) return;
        refresh();
        host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        if (focused >= 0) send(focused, AccessibilityEvent.TYPE_VIEW_FOCUSED);
    }

    private void send(int id, int type) {
        AccessibilityEvent e = AccessibilityEvent.obtain(type);
        e.setPackageName(host.getContext().getPackageName());
        e.setClassName("android.widget.Button");
        e.setSource(host, id);
        e.getText().add(labels.get(id));
        host.getParent().requestSendAccessibilityEvent(host, e);
    }

    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo(int id) {
        if (id == View.NO_ID) {
            refresh();
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(host);
            host.onInitializeAccessibilityNodeInfo(info);
            for (int i = 0; i < labels.size(); i++) info.addChild(host, i);
            return info;
        }
        if (id < 0 || id >= labels.size()) return null;
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(host, id);
        info.setPackageName(host.getContext().getPackageName());
        info.setClassName("android.widget.Button");
        info.setParent(host);
        info.setText(labels.get(id));
        Rect r = rects.get(id);
        info.setBoundsInParent(r);
        host.getLocationOnScreen(loc);
        info.setBoundsInScreen(new Rect(r.left + loc[0], r.top + loc[1], r.right + loc[0], r.bottom + loc[1]));
        info.setVisibleToUser(true);
        info.setEnabled(true);
        info.setFocusable(true);
        info.setClickable(true);
        info.setFocused(id == focused);
        info.setAccessibilityFocused(id == focused);
        return info;
    }

    @Override
    public boolean performAction(int id, int action, Bundle args) {
        return false;
    }
}
