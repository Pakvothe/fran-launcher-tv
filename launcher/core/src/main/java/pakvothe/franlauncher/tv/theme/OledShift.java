package pakvothe.franlauncher.tv.theme;

/**
 * Desplazamiento del reloj en modo ambiente: una posición por minuto dentro de x ±12, y ±8 (px a 1080p),
 * en un orden que salta por la grilla y no repite ninguna posición antes de 35 minutos.
 */
public final class OledShift {
    private static final int[] XS = {-12, -8, -4, 0, 4, 8, 12};
    private static final int[] YS = {-8, -4, 0, 4, 8};
    public static final int PERIOD = XS.length * YS.length; // 35
    /** Paso coprimo con 35: recorre las 35 celdas sin repetir y sin pasos chicos entre minutos seguidos. */
    private static final int STRIDE = 13;

    private OledShift() {}

    public static int x(int minute) {
        return XS[cell(minute) % XS.length];
    }

    public static int y(int minute) {
        return YS[cell(minute) / XS.length];
    }

    private static int cell(int minute) {
        return (int) (((long) Math.floorMod(minute, PERIOD) * STRIDE) % PERIOD);
    }
}
