package pakvothe.franlauncher.tv.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;

/** Ciclo de vida y despacho de teclas. Sin lógica de UI. */
public final class HomeActivity extends Activity {
    private Stage stage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long t0 = SystemClock.uptimeMillis();
        super.onCreate(null);
        stage = new Stage(this, t0);
        setContentView(stage);
    }
}
