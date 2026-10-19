package pakvothe.franlauncher.tv.app;

/**
 * Máquina de estados de foco. Java plano, sin APIs de Android: se testea en la JVM.
 * Recibe teclas lógicas y devuelve un efecto que el Stage traduce a animaciones o acciones.
 */
public final class FocusModel {
    // Estados
    public static final int SHELF = 0, MOVING = 1, MENU = 2, PANEL_LIST = 3, PANEL_INDEX = 4, PANEL_ROWS = 5, AMBIENT = 6;
    // Teclas lógicas
    public static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3, OK = 4, LONG_OK = 5, BACK = 6, MENU_KEY = 7, HOME = 8;
    // Efectos
    public static final int NONE = 0, FOCUS = 1, LAUNCH = 2, OPEN_ALL = 3, OPEN_INPUTS = 4, OPEN_SETTINGS = 5;
    // Botones redondos al final de la fila
    public static final int BUTTON_ALL = 0, BUTTON_INPUTS = 1, BUTTON_SETTINGS = 2;

    private int state = SHELF;
    private int index;
    private int favorites;
    private boolean hasInputs;

    public int state() {
        return state;
    }

    public int index() {
        return index;
    }

    /** Cantidad de favoritas y si hay botón de Entradas. Mantiene el foco dentro del rango. */
    public void setShelf(int favorites, boolean hasInputs) {
        this.favorites = favorites;
        this.hasInputs = hasInputs;
        index = Math.max(0, Math.min(index, shelfSize() - 1));
    }

    public int shelfSize() {
        return favorites + buttonCount();
    }

    public int buttonCount() {
        return hasInputs ? 3 : 2;
    }

    /** Índice de favorita con foco, o -1 si el foco está en un botón. */
    public int focusedFavorite() {
        return index < favorites ? index : -1;
    }

    /** BUTTON_* con foco, o -1 si el foco está en una favorita. */
    public int focusedButton() {
        if (index < favorites) return -1;
        int b = index - favorites;
        return hasInputs || b == 0 ? b : BUTTON_SETTINGS;
    }

    public void focusShelf(int i) {
        index = Math.max(0, Math.min(i, shelfSize() - 1));
    }

    public int onKey(int key) {
        switch (state) {
            case SHELF:
                return shelf(key);
            default:
                return NONE;
        }
    }

    private int shelf(int key) {
        switch (key) {
            case LEFT:
                if (index == 0) return NONE;
                index--;
                return FOCUS;
            case RIGHT:
                if (index >= shelfSize() - 1) return NONE;
                index++;
                return FOCUS;
            case OK:
                if (focusedFavorite() >= 0) return LAUNCH;
                switch (focusedButton()) {
                    case BUTTON_ALL: return OPEN_ALL;
                    case BUTTON_INPUTS: return OPEN_INPUTS;
                    default: return OPEN_SETTINGS;
                }
            case HOME:
                if (index == 0) return NONE;
                index = 0;
                return FOCUS;
            default:
                return NONE;
        }
    }
}
