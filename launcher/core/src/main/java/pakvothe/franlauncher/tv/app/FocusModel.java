package pakvothe.franlauncher.tv.app;

/**
 * Máquina de estados de foco. Java plano, sin APIs de Android: se testea en la JVM.
 * Recibe teclas lógicas y devuelve un efecto que el Stage traduce a animaciones o acciones.
 */
public final class FocusModel {
    // Estados
    public static final int SHELF = 0, MOVING = 1, MENU = 2, PANEL_LIST = 3, PANEL_INDEX = 4, PANEL_ROWS = 5, AMBIENT = 6,
        NOW_PLAYING = 7;
    // Teclas lógicas
    public static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3, OK = 4, LONG_OK = 5, BACK = 6, MENU_KEY = 7, HOME = 8;
    /** Solo para bench.sh: abre Todas desde cualquier estado del inicio. */
    public static final int HOME_OPEN_ALL = 9;
    // Efectos
    public static final int NONE = 0, FOCUS = 1, LAUNCH = 2, OPEN_ALL = 3, OPEN_INPUTS = 4, OPEN_SETTINGS = 5,
        CLOSE_PANEL = 6, LIST_FOCUS = 7, LIST_LAUNCH = 8, INDEX_ENTER = 9, INDEX_FOCUS = 10, INDEX_EXIT = 11,
        OPEN_MENU = 12, MENU_FOCUS = 13, MENU_PICK = 14, CLOSE_MENU = 15, MOVE_STEP = 16, MOVE_DROP = 17,
        ROW_FOCUS = 18, ROW_PICK = 19, WAKE = 20, WAKE_HOME = 21,
        NP_FOCUS = 22, NP_TOGGLE = 23, NP_LEAVE = 24;
    // Botones redondos al final de la fila
    public static final int BUTTON_ALL = 0, BUTTON_INPUTS = 1, BUTTON_SETTINGS = 2;

    private int state = SHELF;
    private int index;
    // Panel Todas: posición en la lista, letra de cada ítem y letra elegida en el índice.
    private int listIndex, letterIndex;
    private int[] letterOf = new int[0];
    // Menú de pulsación larga: opción con foco, cantidad y desde qué estado se abrió.
    private int menuIndex, menuSize, menuFrom;
    // Paneles de filas (Entradas, Ajustes).
    private int rowIndex, rowCount;
    /** Hay píldora de Reproduciendo: ▲ desde la fila la enfoca. */
    private boolean nowPlaying;
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

    public int rowIndex() {
        return rowIndex;
    }

    /** Abre un panel de filas (Entradas o Ajustes) con el foco en la primera. */
    public void openRows(int count) {
        state = PANEL_ROWS;
        rowCount = count;
        rowIndex = 0;
    }

    /** Si desaparece con el foco encima, el foco vuelve a la fila. Devuelve true si hubo que moverlo. */
    public boolean setNowPlaying(boolean visible) {
        nowPlaying = visible;
        if (!visible && state == NOW_PLAYING) {
            state = SHELF;
            return true;
        }
        return false;
    }

    public void focusRow(int i) {
        rowIndex = Math.max(0, Math.min(i, rowCount - 1));
    }

    public void setRowCount(int count) {
        rowCount = count;
        rowIndex = Math.max(0, Math.min(rowIndex, count - 1));
    }

    public void closePanel() {
        state = SHELF;
    }

    public int menuIndex() {
        return menuIndex;
    }

    /** SHELF o PANEL_LIST: de dónde se abrió el menú (define las opciones y adónde vuelve). */
    public int menuFrom() {
        return menuFrom;
    }

    /** El Stage decide las opciones; el modelo solo recorre. */
    public void openMenu(int size) {
        menuFrom = state;
        menuSize = size;
        menuIndex = 0;
        state = MENU;
    }

    public void closeMenu() {
        if (state == MENU) state = menuFrom;
    }

    /** Solo desde el inicio limpio: no con panel, menú ni Mover. */
    public boolean enterAmbient() {
        if (state != SHELF) return false;
        state = AMBIENT;
        return true;
    }

    public void startMoving() {
        state = MOVING;
    }

    public int onKey(int key) {
        switch (state) {
            case AMBIENT:
                // La primera tecla solo despierta. Home además lleva el foco a la primera favorita.
                state = SHELF;
                if (key == HOME) {
                    index = 0;
                    return WAKE_HOME;
                }
                return WAKE;
            case MENU:
                return menu(key);
            case NOW_PLAYING:
                return nowPlaying(key);
            case MOVING:
                return moving(key);
            case SHELF:
                return shelf(key);
            case PANEL_LIST:
                return list(key);
            case PANEL_INDEX:
                return index(key);
            case PANEL_ROWS:
                return rows(key);
            default:
                return NONE;
        }
    }

    private int nowPlaying(int key) {
        switch (key) {
            case OK:
                return NP_TOGGLE;
            case DOWN:
            case BACK:
                state = SHELF;
                return NP_LEAVE;
            case HOME:
                state = SHELF;
                index = 0;
                return NP_LEAVE;
            default:
                return NONE;
        }
    }

    private int rows(int key) {
        switch (key) {
            case UP:
                if (rowIndex == 0) return NONE;
                rowIndex--;
                return ROW_FOCUS;
            case DOWN:
                if (rowIndex >= rowCount - 1) return NONE;
                rowIndex++;
                return ROW_FOCUS;
            case OK:
                return rowCount == 0 ? NONE : ROW_PICK;
            case LEFT:
            case BACK:
            case HOME:
                state = SHELF;
                return CLOSE_PANEL;
            default:
                return NONE;
        }
    }

    private int menu(int key) {
        switch (key) {
            case UP:
                if (menuIndex == 0) return NONE;
                menuIndex--;
                return MENU_FOCUS;
            case DOWN:
                if (menuIndex >= menuSize - 1) return NONE;
                menuIndex++;
                return MENU_FOCUS;
            case OK:
                state = menuFrom;
                return MENU_PICK;
            case BACK:
            case MENU_KEY:
                state = menuFrom;
                return CLOSE_MENU;
            case HOME:
                state = SHELF;
                return CLOSE_PANEL;
            default:
                return NONE;
        }
    }

    /** Mover: ◀ ▶ corren la favorita; OK, Atrás o Home la sueltan donde está. */
    private int moving(int key) {
        switch (key) {
            case LEFT:
                if (index == 0) return NONE;
                index--;
                return MOVE_STEP;
            case RIGHT:
                if (index >= favorites - 1) return NONE;
                index++;
                return MOVE_STEP;
            case OK:
            case BACK:
            case HOME:
                state = SHELF;
                return MOVE_DROP;
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
            case LONG_OK:
            case MENU_KEY:
                return focusedFavorite() >= 0 ? OPEN_MENU : NONE;
            case UP:
                if (!nowPlaying) return NONE;
                state = NOW_PLAYING;
                return NP_FOCUS;
            default:
                return NONE;
        }
    }
}
