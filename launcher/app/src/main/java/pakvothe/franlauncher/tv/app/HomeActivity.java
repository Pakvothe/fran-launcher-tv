package pakvothe.franlauncher.tv.app;

import android.app.Activity;
import android.content.Intent;
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
        // El Stage es opaco: el fondo de la ventana sería una capa de overdraw de más.
        getWindow().setBackgroundDrawable(null);
        applyDebugExtras(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        applyDebugExtras(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        stage.onStart();
    }

    @Override
    protected void onStop() {
        stage.onStop();
        super.onStop();
    }

    /** Para medir con bench.sh: am start ... --ez fl.anim false · --ei fl.secs 0|1|2 · --ei fl.hour 0|1|2. */
    private void applyDebugExtras(Intent i) {
        if (i == null) return;
        if (i.hasExtra("fl.anim")) stage.setAnimatedBackground(i.getBooleanExtra("fl.anim", true));
        if (i.hasExtra("fl.secs")) stage.setSecondsMode(i.getIntExtra("fl.secs", 0));
        if (i.hasExtra("fl.hour")) stage.setHourFormat(i.getIntExtra("fl.hour", 0));
    }
}
