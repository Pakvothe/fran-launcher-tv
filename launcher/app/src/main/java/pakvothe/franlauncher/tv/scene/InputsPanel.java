package pakvothe.franlauncher.tv.scene;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;

import java.util.ArrayList;
import java.util.List;

import pakvothe.franlauncher.tv.R;
import pakvothe.franlauncher.tv.anim.Animator;
import pakvothe.franlauncher.tv.tv.InputsRepository;

/** Entradas: nombre, punto de estado y "Con señal / Sin señal". Sin señal al 60% de alfa. */
public final class InputsPanel extends RowsPanel {
    private final InputsRepository repo;
    private final Context context;
    private final List<TvInputInfo> inputs = new ArrayList<>();

    public InputsPanel(Context c, Animator a, InputsRepository repo) {
        super(c, a, c.getString(R.string.inputs), 92);
        this.context = c;
        this.repo = repo;
    }

    public void refresh() {
        inputs.clear();
        inputs.addAll(repo.list());
        rows.clear();
        for (TvInputInfo i : inputs) {
            Row r = new Row(repo.label(i));
            int st = repo.state(i);
            boolean on = st == TvInputManager.INPUT_STATE_CONNECTED;
            r.value = context.getString(on ? R.string.signal_on
                : st == TvInputManager.INPUT_STATE_CONNECTED_STANDBY ? R.string.signal_standby : R.string.signal_off);
            r.dot = on ? 0xFF4CD08A : st == TvInputManager.INPUT_STATE_CONNECTED_STANDBY ? 0xFFE0B04A : 0xFF8A8F9C;
            r.dim = !on;
            rows.add(r);
        }
        rebuild();
    }

    public TvInputInfo get(int i) {
        return i >= 0 && i < inputs.size() ? inputs.get(i) : null;
    }
}
