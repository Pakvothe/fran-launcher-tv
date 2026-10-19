package pakvothe.franlauncher.tv.app;

import android.view.KeyEvent;

/** Keycodes del control → teclas lógicas del FocusModel. Los controles varían mucho: se aceptan varios por acción. */
final class InputRouter {
    private InputRouter() {}

    /** -1 si la tecla no es nuestra (las de media siguen yendo a la app que suena). */
    static int map(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return FocusModel.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return FocusModel.RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP: return FocusModel.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return FocusModel.DOWN;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A: return FocusModel.OK;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_BUTTON_B: return FocusModel.BACK;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BUTTON_Y: return FocusModel.MENU_KEY;
            default: return -1;
        }
    }
}
