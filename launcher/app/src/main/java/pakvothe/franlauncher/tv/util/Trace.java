package pakvothe.franlauncher.tv.util;

/**
 * Marcadores para Perfetto. Los nombres son fijos: bench.sh y las trazas los buscan tal cual.
 * Un marcador sin begin/end pareado en el mismo hilo rompe la traza: siempre en try/finally.
 */
public final class Trace {
    public static final String SNAPSHOT_READ = "snapshot.read";
    public static final String RECORD_SHELF = "record.shelf";
    public static final String RECORD_PANEL = "record.panel";
    public static final String SYNC_APPS = "sync.apps";
    public static final String FIRST_FRAME = "first.frame";

    private Trace() {}

    public static void begin(String section) {
        android.os.Trace.beginSection(section);
    }

    public static void end() {
        android.os.Trace.endSection();
    }
}
