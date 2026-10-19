package pakvothe.franlauncher.tv.theme;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class OledShiftTest {
    @Test
    public void noRepiteAntesDe35MinutosYQuedaEnRango() {
        Set<String> seen = new HashSet<>();
        for (int m = 0; m < OledShift.PERIOD; m++) {
            int x = OledShift.x(m), y = OledShift.y(m);
            assertTrue(Math.abs(x) <= 12 && Math.abs(y) <= 8);
            assertTrue("repetida en el minuto " + m, seen.add(x + "," + y));
        }
        assertEquals(35, seen.size());
        assertEquals(OledShift.x(0), OledShift.x(35));
        assertEquals(OledShift.y(7), OledShift.y(42));
    }

    @Test
    public void minutosConsecutivosNoQuedanEnElMismoLugar() {
        for (int m = 0; m < 100; m++)
            assertFalse(OledShift.x(m) == OledShift.x(m + 1) && OledShift.y(m) == OledShift.y(m + 1));
    }

    @Test
    public void minutosNegativosNoRompen() {
        assertEquals(OledShift.x(34), OledShift.x(-1));
    }
}
