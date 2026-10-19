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
    /** Solo para bench.sh: abre Todas desde cualquier estado del inicio. */
    public static final int HOME_OPEN_ALL = 9;
    // Efectos
    public static final int NONE = 0, FOCUS = 1, LAUNCH = 2, OPEN_ALL = 3, OPEN_INPUTS = 4, OPEN_SETTINGS = 5,
        CLOSE_PANEL = 6, LIST_FOCUS = 7, LIST_LAUNCH = 8, INDEX_ENTER = 9, INDEX_FOCUS = 10, INDEX_EXIT = 11,
        OPEN_MENU = 12;
    // Botones redondos al final de la fila
    public static final int BUTTON_ALL = 0, BUTTON_INPUTS = 1, BUTTON_SETTINGS = 2;

    private int state = SHELF;
    private int index;
    // Panel Todas: posición en la lista, letra de cada ítem y letra elegida en el índice.
    private int listIndex, letterIndex;
    private int[] letterOf = new int[0];
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

    public int listIndex() {
        return listIndex;
    }

    public int letterIndex() {
        return letterIndex;
    }

    /** Lista ordenada del panel Todas: letterOf[i] = letra (0..26) del ítem i. Mantiene el foco en rango. */
    public void setList(int[] letterOf) {
        this.letterOf = letterOf;
        listIndex = Math.max(0, Math.min(listIndex, letterOf.length - 1));
    }

    /** Abre el panel Todas con el foco en el primer ítem. */
    public void openList() {
        state = PANEL_LIST;
        listIndex = 0;
    }

    public void closePanel() {
        state = SHELF;
    }

    public int onKey(int key) {
        switch (state) {
            case SHELF:
                return shelf(key);
            case PANEL_LIST:
                return list(key);
            case PANEL_INDEX:
                return index(key);
            default:
                return NONE;
        }
    }

    private int list(int key) {
        switch (key) {
            case UP:
                if (listIndex == 0) return NONE;
                listIndex--;
                return LIST_FOCUS;
            case DOWN:
                if (listIndex >= letterOf.length - 1) return NONE;
                listIndex++;
                return LIST_FOCUS;
            case RIGHT:
                if (letterOf.length == 0) return NONE;
                state = PANEL_INDEX;
                letterIndex = letterOf[listIndex];
                return INDEX_ENTER;
            case LEFT:
            case BACK:
            case HOME:
                state = SHELF;
                return CLOSE_PANEL;
            case OK:
                return letterOf.length == 0 ? NONE : LIST_LAUNCH;
            case LONG_OK:
            case MENU_KEY:
                return letterOf.length == 0 ? NONE : OPEN_MENU;
            default:
                return NONE;
        }
    }

    /** En el índice ▲▼ saltan entre letras con apps; OK o ◀ vuelven a la lista en esa letra. */
    private int index(int key) {
        switch (key) {
            case UP:
            case DOWN: {
                int step = key == UP ? -1 : 1;
                for (int l = letterIndex + step; l >= 0 && l < 27; l += step) {
                    if (firstOfLetter(l) >= 0) {
                        letterIndex = l;
                        return INDEX_FOCUS;
                    }
                }
                return NONE;
            }
            case OK:
            case LEFT: {
                state = PANEL_LIST;
                int first = firstOfLetter(letterIndex);
                if (first >= 0) listIndex = first;
                return INDEX_EXIT;
            }
            case BACK:
            case HOME:
                state = SHELF;
                return CLOSE_PANEL;
            default:
                return NONE;
        }
    }

    public int firstOfLetter(int letter) {
        for (int i = 0; i < letterOf.length; i++) if (letterOf[i] == letter) return i;
        return -1;
    }

    private int shelf(int key) {
        switch (key) {
            case HOME_OPEN_ALL:
                return OPEN_ALL;
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
