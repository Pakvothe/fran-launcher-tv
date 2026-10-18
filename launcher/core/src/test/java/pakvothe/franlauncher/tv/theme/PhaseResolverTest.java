package pakvothe.franlauncher.tv.theme;

import static org.junit.Assert.assertEquals;
import static pakvothe.franlauncher.tv.theme.PhaseResolver.*;

import org.junit.Test;

public class PhaseResolverTest {
    @Test
    public void bordesDeCadaFase() {
        int[] esperado = {NIGHT, NIGHT, NIGHT, NIGHT, NIGHT, DAWN, DAWN, DAWN, DAY, DAY, DAY, DAY,
            DAY, DAY, DAY, DAY, DAY, DUSK, DUSK, DUSK, DUSK, NIGHT, NIGHT, NIGHT};
        for (int h = 0; h < 24; h++) assertEquals("hora " + h, esperado[h], phaseAt(h));
    }

    @Test(expected = IllegalArgumentException.class)
    public void horaInvalida() {
        phaseAt(24);
    }

    @Test
    public void temaAutoEsClaroDeDiaYOscuroDeNoche() {
        assertEquals(true, Theme.of(Theme.MODE_AUTO, DAWN).light);
        assertEquals(true, Theme.of(Theme.MODE_AUTO, DAY).light);
        assertEquals(false, Theme.of(Theme.MODE_AUTO, DUSK).light);
        assertEquals(false, Theme.of(Theme.MODE_AUTO, NIGHT).light);
        assertEquals(true, Theme.of(Theme.MODE_LIGHT, NIGHT).light);
        assertEquals(false, Theme.of(Theme.MODE_DARK, DAY).light);
    }

    @Test
    public void paletaOscuraDeLaNoche() {
        Theme t = Theme.of(Theme.MODE_DARK, NIGHT);
        assertEquals(0xFF0A0E1C, t.skyTop);
        assertEquals(0xFF1B2244, t.skyBottom);
    }
}
