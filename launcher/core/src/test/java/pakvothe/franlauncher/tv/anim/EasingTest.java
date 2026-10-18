package pakvothe.franlauncher.tv.anim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EasingTest {
    @Test
    public void extremos() {
        assertEquals(0f, Easing.standard(0f), 0f);
        assertEquals(1f, Easing.standard(1f), 0f);
        assertEquals(0f, Easing.standard(-1f), 0f);
        assertEquals(1f, Easing.standard(2f), 0f);
    }

    @Test
    public void monotonaYRapidaAlPrincipio() {
        float prev = 0f;
        for (int i = 1; i <= 100; i++) {
            float v = Easing.standard(i / 100f);
            assertTrue(v >= prev);
            prev = v;
        }
        // Curva "standard" de Material: a mitad de tiempo ya recorrió bastante más de la mitad.
        assertTrue(Easing.standard(0.5f) > 0.75f);
    }
}
