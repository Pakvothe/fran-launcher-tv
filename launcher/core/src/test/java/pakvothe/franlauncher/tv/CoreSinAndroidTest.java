package pakvothe.franlauncher.tv;

import static org.junit.Assert.fail;

import org.junit.Test;

/** Garantiza que core siga siendo Java plano: si alguien suma una dependencia de Android, falla. */
public class CoreSinAndroidTest {
    @Test
    public void noHayClasesDeAndroidEnElClasspath() {
        try {
            Class.forName("android.os.Trace");
            fail("core no puede depender de Android");
        } catch (ClassNotFoundException esperado) {
            // ok
        }
    }
}
