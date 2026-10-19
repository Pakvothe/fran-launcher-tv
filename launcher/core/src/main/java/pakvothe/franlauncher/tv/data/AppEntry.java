package pakvothe.franlauncher.tv.data;

/** Una app lanzable, tal como vive en el caché. */
public final class AppEntry {
    /** "paquete/clase". */
    public final String component;
    public final String label;
    /** Color de fondo para el recuadro si no tiene banner (promedio del ícono). */
    public final int color;
    /** Celda en el atlas de banners, o -1. */
    public final int banner;
    /** Celda en el atlas de íconos, o -1. */
    public final int icon;

    public AppEntry(String component, String label, int color, int banner, int icon) {
        this.component = component;
        this.label = label;
        this.color = color;
        this.banner = banner;
        this.icon = icon;
    }

    public String packageName() {
        int i = component.indexOf('/');
        return i < 0 ? component : component.substring(0, i);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppEntry)) return false;
        AppEntry e = (AppEntry) o;
        return component.equals(e.component) && label.equals(e.label) && color == e.color
            && banner == e.banner && icon == e.icon;
    }

    @Override
    public int hashCode() {
        return component.hashCode();
    }
}
