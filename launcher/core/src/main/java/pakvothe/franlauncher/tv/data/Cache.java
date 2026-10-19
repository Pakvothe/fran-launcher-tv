package pakvothe.franlauncher.tv.data;

import java.util.ArrayList;
import java.util.List;

/** Lista de apps + geometría de los atlas. Descartable: si no coincide algo, se regenera desde PackageManager. */
public final class Cache {
    public int density, width, height;
    /** Celdas por fila en cada atlas, y cantidad de celdas usadas. */
    public int bannerCols, bannerCount, iconCols, iconCount;
    /** Locale para el que se calcularon los labels y la fecha. */
    public String locale = "";
    /** Fecha ya formateada (ahorra ICU en el camino crítico) y el día al que corresponde. */
    public int dateDay = -1;
    public String dateText = "";
    public final List<AppEntry> apps = new ArrayList<>();
}
