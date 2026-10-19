package pakvothe.franlauncher.tv.app;

import static org.junit.Assert.assertEquals;
import static pakvothe.franlauncher.tv.app.FocusModel.*;

import org.junit.Test;

import pakvothe.franlauncher.tv.data.Letters;

public class PanelListTest {
    // "2048", "Amazon", "Ápex", "Netflix", "Ñandú", "YouTube" → # A A N N Y
    private static FocusModel open() {
        FocusModel m = new FocusModel();
        m.setShelf(3, false);
        String[] labels = {"2048", "Amazon", "Ápex", "Netflix", "Ñandú", "YouTube"};
        int[] letters = new int[labels.length];
        for (int i = 0; i < labels.length; i++) letters[i] = Letters.bucket(labels[i]);
        m.setList(letters);
        m.openList();
        return m;
    }

    @Test
    public void letrasConAcentosVanASuLetra() {
        assertEquals(0, Letters.bucket("2048"));
        assertEquals(1, Letters.bucket("Ápex"));
        assertEquals(14, Letters.bucket("Ñandú"));
        assertEquals(0, Letters.bucket(""));
        assertEquals('N', Letters.LABELS.charAt(14));
    }

    @Test
    public void recorreLaListaSinDarLaVuelta() {
        FocusModel m = open();
        assertEquals(PANEL_LIST, m.state());
        assertEquals(NONE, m.onKey(UP));
        for (int i = 0; i < 5; i++) assertEquals(LIST_FOCUS, m.onKey(DOWN));
        assertEquals(NONE, m.onKey(DOWN));
        assertEquals(5, m.listIndex());
    }

    @Test
    public void indiceSaltaLetrasVaciasYVuelveALaPrimeraDeLaLetra() {
        FocusModel m = open();
        m.onKey(DOWN); // Amazon (A)
        assertEquals(INDEX_ENTER, m.onKey(RIGHT));
        assertEquals(1, m.letterIndex());
        assertEquals(INDEX_FOCUS, m.onKey(DOWN)); // salta B..M
        assertEquals(14, m.letterIndex());
        assertEquals(INDEX_FOCUS, m.onKey(DOWN));
        assertEquals(25, m.letterIndex());
        assertEquals(NONE, m.onKey(DOWN));
        assertEquals(INDEX_FOCUS, m.onKey(UP));
        assertEquals(INDEX_EXIT, m.onKey(OK));
        assertEquals(PANEL_LIST, m.state());
        assertEquals(3, m.listIndex()); // Netflix, primera con N
    }

    @Test
    public void izquierdaDesdeElIndiceVuelveSinCerrar() {
        FocusModel m = open();
        m.onKey(RIGHT);
        assertEquals(INDEX_EXIT, m.onKey(LEFT));
        assertEquals(PANEL_LIST, m.state());
    }

    @Test
    public void atrasIzquierdaYHomeCierranElPanel() {
        for (int key : new int[] {BACK, LEFT, HOME}) {
            FocusModel m = open();
            assertEquals(CLOSE_PANEL, m.onKey(key));
            assertEquals(SHELF, m.state());
        }
        FocusModel m = open();
        m.onKey(RIGHT);
        assertEquals(CLOSE_PANEL, m.onKey(BACK));
    }

    @Test
    public void okLanzaYPulsacionLargaAbreElMenu() {
        FocusModel m = open();
        assertEquals(LIST_LAUNCH, m.onKey(OK));
        assertEquals(OPEN_MENU, m.onKey(LONG_OK));
        assertEquals(OPEN_MENU, m.onKey(MENU_KEY));
    }

    @Test
    public void listaVaciaNoHaceNada() {
        FocusModel m = new FocusModel();
        m.setList(new int[0]);
        m.openList();
        assertEquals(NONE, m.onKey(OK));
        assertEquals(NONE, m.onKey(RIGHT));
        assertEquals(CLOSE_PANEL, m.onKey(BACK));
    }
}
