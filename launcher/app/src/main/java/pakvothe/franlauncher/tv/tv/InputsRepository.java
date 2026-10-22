package pakvothe.franlauncher.tv.tv;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/** Entradas de hardware (HDMI, AV, componente...). Se consulta al abrir el panel; callback mientras está abierto. */
public final class InputsRepository {
    public interface Listener {
        void onInputsChanged();
    }

    private final Context context;
    private TvInputManager tim;
    private Listener listener;
    private final TvInputManager.TvInputCallback callback = new TvInputManager.TvInputCallback() {
        @Override
        public void onInputStateChanged(String inputId, int state) {
            if (listener != null) listener.onInputsChanged();
        }

        @Override
        public void onInputAdded(String inputId) {
            if (listener != null) listener.onInputsChanged();
        }

        @Override
        public void onInputRemoved(String inputId) {
            if (listener != null) listener.onInputsChanged();
        }
    };

    public InputsRepository(Context c) {
        context = c;
    }

    private TvInputManager manager() {
        if (tim == null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LIVE_TV))
            tim = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        return tim;
    }

    public static boolean isHardware(TvInputInfo i) {
        switch (i.getType()) {
            case TvInputInfo.TYPE_HDMI:
            case TvInputInfo.TYPE_COMPOSITE:
            case TvInputInfo.TYPE_COMPONENT:
            case TvInputInfo.TYPE_SVIDEO:
            case TvInputInfo.TYPE_SCART:
            case TvInputInfo.TYPE_VGA:
            case TvInputInfo.TYPE_DVI:
                return true;
            default:
                return false;
        }
    }

    /** Para el caché (hilo de I/O): ¿hay al menos una entrada de hardware? */
    public static boolean hasHardwareInputs(Context c) {
        if (!c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LIVE_TV)) return false;
        TvInputManager m = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (m == null) return false;
        for (TvInputInfo i : m.getTvInputList()) if (isHardware(i)) return true;
        return false;
    }

    public List<TvInputInfo> list() {
        List<TvInputInfo> out = new ArrayList<>();
        TvInputManager m = manager();
        if (m == null) return out;
        for (TvInputInfo i : m.getTvInputList()) if (isHardware(i) && i.getParentId() == null) out.add(i);
        // HDMI primero, en orden de puerto; después AV y el resto. El sistema las devuelve al revés.
        out.sort((x, y) -> x.getType() != y.getType()
            ? (x.getType() == TvInputInfo.TYPE_HDMI ? -1 : y.getType() == TvInputInfo.TYPE_HDMI ? 1 : x.getType() - y.getType())
            : x.getId().compareTo(y.getId()));
        return out;
    }

    public int state(TvInputInfo i) {
        TvInputManager m = manager();
        return m == null ? TvInputManager.INPUT_STATE_DISCONNECTED : m.getInputState(i.getId());
    }

    public String label(TvInputInfo i) {
        CharSequence custom = i.loadCustomLabel(context);
        return String.valueOf(custom != null && custom.length() > 0 ? custom : i.loadLabel(context));
    }

    private boolean registered;

    public void watch(Listener l) {
        TvInputManager m = manager();
        if (m == null) return;
        listener = l;
        if (l != null && !registered) m.registerCallback(callback, new Handler(Looper.getMainLooper()));
        else if (l == null && registered) m.unregisterCallback(callback);
        registered = l != null;
    }

    public android.content.Intent intentFor(TvInputInfo i) {
        // Tipo explícito: la app Live TV filtra por MIME y no todos los proveedores lo resuelven solos.
        return new android.content.Intent(android.content.Intent.ACTION_VIEW)
            .setDataAndType(TvContract.buildChannelUriForPassthroughInput(i.getId()), TvContract.Channels.CONTENT_ITEM_TYPE)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
