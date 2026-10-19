package pakvothe.franlauncher.tv.app;

import static org.junit.Assert.assertEquals;
import static pakvothe.franlauncher.tv.app.FocusModel.*;

import org.junit.Test;

public class MenuMoveTest {
    @Test
    public void pulsacionLargaSoloEnFavoritas() {
        FocusModel m = new FocusModel();
        m.setShelf(2, false);
        assertEquals(OPEN_MENU, m.onKey(LONG_OK));
        assertEquals(OPEN_MENU, m.onKey(MENU_KEY));
        m.focusShelf(2); // botón Todas
        assertEquals(NONE, m.onKey(LONG_OK));
    }

    @Test
    public void menuRecorreEligeYVuelveAlEstadoDeOrigen() {
        FocusModel m = new FocusModel();
        m.setShelf(2, false);
        m.openMenu(4);
        assertEquals(MENU, m.state());
        assertEquals(NONE, m.onKey(UP));
        assertEquals(MENU_FOCUS, m.onKey(DOWN));
        assertEquals(MENU_FOCUS, m.onKey(DOWN));
        assertEquals(MENU_FOCUS, m.onKey(DOWN));
        assertEquals(NONE, m.onKey(DOWN));
        assertEquals(3, m.menuIndex());
        assertEquals(MENU_PICK, m.onKey(OK));
        assertEquals(SHELF, m.state());
    }

    @Test
    public void atrasCierraElMenuSinHacerNada() {
        FocusModel m = new FocusModel();
        m.setList(new int[] {1, 2});
        m.openList();
        m.openMenu(3);
        assertEquals(PANEL_LIST, m.menuFrom());
        assertEquals(CLOSE_MENU, m.onKey(BACK));
        assertEquals(PANEL_LIST, m.state());
    }

    @Test
    public void homeDesdeElMenuCierraTodo() {
        FocusModel m = new FocusModel();
        m.setList(new int[] {1});
        m.openList();
        m.openMenu(3);
        assertEquals(CLOSE_PANEL, m.onKey(HOME));
        assertEquals(SHELF, m.state());
    }

    @Test
    public void moverCorreDentroDeLasFavoritasYSuelta() {
        FocusModel m = new FocusModel();
        m.setShelf(3, true);
        m.focusShelf(1);
        m.startMoving();
        assertEquals(MOVE_STEP, m.onKey(RIGHT));
        assertEquals(NONE, m.onKey(RIGHT)); // no pasa a los botones
        assertEquals(2, m.index());
        assertEquals(MOVE_STEP, m.onKey(LEFT));
        assertEquals(MOVE_STEP, m.onKey(LEFT));
        assertEquals(NONE, m.onKey(LEFT));
        assertEquals(MOVE_DROP, m.onKey(OK));
        assertEquals(SHELF, m.state());
        m.startMoving();
        assertEquals(MOVE_DROP, m.onKey(BACK));
    }

    @Test
    public void ambienteSoloDesdeElInicioYLaPrimeraTeclaSoloDespierta() {
        FocusModel m = new FocusModel();
        m.setShelf(3, false);
        m.focusShelf(2);
        assertEquals(true, m.enterAmbient());
        assertEquals(AMBIENT, m.state());
        assertEquals(WAKE, m.onKey(OK));
        assertEquals(SHELF, m.state());
        assertEquals(2, m.index());
        m.enterAmbient();
        assertEquals(WAKE_HOME, m.onKey(HOME));
        assertEquals(0, m.index());
        m.openMenu(2);
        assertEquals(false, m.enterAmbient());
    }

    @Test
    public void reproduciendoSeEnfocaConArribaYSeSueltaConAbajo() {
        FocusModel m = new FocusModel();
        m.setShelf(2, false);
        assertEquals(NONE, m.onKey(UP));
        m.setNowPlaying(true);
        assertEquals(NP_FOCUS, m.onKey(UP));
        assertEquals(NOW_PLAYING, m.state());
        assertEquals(NP_TOGGLE, m.onKey(OK));
        assertEquals(NONE, m.onKey(LEFT));
        assertEquals(NP_LEAVE, m.onKey(DOWN));
        assertEquals(SHELF, m.state());
        m.onKey(UP);
        assertEquals(true, m.setNowPlaying(false));
        assertEquals(SHELF, m.state());
    }
}
