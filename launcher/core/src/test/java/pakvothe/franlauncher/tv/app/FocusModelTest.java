package pakvothe.franlauncher.tv.app;

import static org.junit.Assert.assertEquals;
import static pakvothe.franlauncher.tv.app.FocusModel.*;

import org.junit.Test;

public class FocusModelTest {
    private static FocusModel shelf(int favs, boolean inputs) {
        FocusModel m = new FocusModel();
        m.setShelf(favs, inputs);
        return m;
    }

    @Test
    public void recorreLaFilaSinDarLaVuelta() {
        FocusModel m = shelf(3, true);
        assertEquals(NONE, m.onKey(LEFT));
        for (int i = 1; i < 6; i++) assertEquals(FOCUS, m.onKey(RIGHT));
        assertEquals(5, m.index());
        assertEquals(NONE, m.onKey(RIGHT));
    }

    @Test
    public void okLanzaFavoritaOAbreElBoton() {
        FocusModel m = shelf(2, true);
        assertEquals(LAUNCH, m.onKey(OK));
        m.focusShelf(2);
        assertEquals(OPEN_ALL, m.onKey(OK));
        m.focusShelf(3);
        assertEquals(OPEN_INPUTS, m.onKey(OK));
        m.focusShelf(4);
        assertEquals(OPEN_SETTINGS, m.onKey(OK));
    }

    @Test
    public void sinEntradasElSegundoBotonEsAjustes() {
        FocusModel m = shelf(1, false);
        assertEquals(3, m.shelfSize());
        m.focusShelf(2);
        assertEquals(BUTTON_SETTINGS, m.focusedButton());
        assertEquals(OPEN_SETTINGS, m.onKey(OK));
    }

    @Test
    public void filaVaciaSoloTieneBotones() {
        FocusModel m = shelf(0, false);
        assertEquals(-1, m.focusedFavorite());
        assertEquals(OPEN_ALL, m.onKey(OK));
    }

    @Test
    public void homeLlevaALaPrimeraFavorita() {
        FocusModel m = shelf(4, false);
        m.focusShelf(3);
        assertEquals(FOCUS, m.onKey(HOME));
        assertEquals(0, m.index());
        assertEquals(NONE, m.onKey(HOME));
    }

    @Test
    public void achicarLaFilaMantieneElFocoEnRango() {
        FocusModel m = shelf(5, true);
        m.focusShelf(7);
        m.setShelf(1, false);
        assertEquals(2, m.index());
    }
}
