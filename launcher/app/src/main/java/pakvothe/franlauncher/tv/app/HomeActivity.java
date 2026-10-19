package pakvothe.franlauncher.tv.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.KeyEvent;

/** Ciclo de vida y despacho de teclas. Sin lógica de UI. */
public final class HomeActivity extends Activity {
    private Stage stage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long t0 = SystemClock.uptimeMillis();
        super.onCreate(null);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        stage = new Stage(this, t0, dm.widthPixels, dm.heightPixels, dm.densityDpi);
        setContentView(stage);
        // El Stage es opaco: el fondo de la ventana sería una capa de overdraw de más.
        getWindow().setBackgroundDrawable(null);
        applyDebugExtras(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home estando en el inicio: el sistema nos re-entrega el intent HOME.
        if (intent != null && intent.hasCategory(Intent.CATEGORY_HOME)) stage.onKey(FocusModel.HOME);
        applyDebugExtras(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int k = InputRouter.map(keyCode);
        if (k < 0) return super.onKeyDown(keyCode, event);
        if (k == FocusModel.OK) {
            // OK se resuelve al soltar, para distinguirlo de la pulsación larga.
            if (event.getRepeatCount() == 0) event.startTracking();
            return true;
        }
        stage.onKey(k);
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (InputRouter.map(keyCode) == FocusModel.OK) {
            stage.onKey(FocusModel.LONG_OK);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int k = InputRouter.map(keyCode);
        if (k < 0) return super.onKeyUp(keyCode, event);
        if (k == FocusModel.OK && event.isTracking() && !event.isCanceled()) stage.onKey(FocusModel.OK);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        stage.onStart();
        stage.focusLastApp(false);
    }

    @Override
    protected void onStop() {
        stage.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stage.onDestroy();
        super.onDestroy();
    }

    /** Para medir con bench.sh: --ez fl.anim false · --ei fl.secs 0|1|2 · --ei fl.hour 0|1|2 · --es fl.open all. */
    private void applyDebugExtras(Intent i) {
        if (i == null) return;
        if (i.hasExtra("fl.anim")) stage.setAnimatedBackground(i.getBooleanExtra("fl.anim", true));
        if (i.hasExtra("fl.secs")) stage.setSecondsMode(i.getIntExtra("fl.secs", 0));
        if (i.hasExtra("fl.hour")) stage.setHourFormat(i.getIntExtra("fl.hour", 0));
        if ("all".equals(i.getStringExtra("fl.open"))) stage.onKey(FocusModel.HOME_OPEN_ALL);
    }
}
