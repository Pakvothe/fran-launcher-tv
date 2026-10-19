package pakvothe.franlauncher.tv.scene;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import pakvothe.franlauncher.tv.theme.Theme;
import pakvothe.franlauncher.tv.util.Typography;

/**
 * Hora, segundero y fecha. La capa es el contenedor (escala y posición para panel, ambiente y OLED);
 * la cara es un nodo hijo que se re-graba una vez por segundo. Sin allocations por tick salvo al cambiar el día.
 */
public final class ClockLayer extends Layer {
    public static final int SECONDS_NUMBER = 0, SECONDS_LINE = 1, SECONDS_OFF = 2;
    public static final int HOUR_SYSTEM = 0, HOUR_24 = 1, HOUR_12 = 2;

    private final Context context;
    public final RenderNode face = new RenderNode("clock.face");
    private final Paint time, secs, date, ampm, line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Calendar cal = Calendar.getInstance();
    private TimeZone zone = TimeZone.getDefault();
    private final char[] hhmm = new char[5];
    private final char[] ss = new char[2];
    private String dateText = "", ampmText = "";
    private int dayKey = -1, ampmKey = -1, hhmmLen;
    private int second;
    private Theme theme;
    private int secondsMode = SECONDS_NUMBER, hourFormat = HOUR_SYSTEM;
    private boolean faceDirty = true, system24;
    private final Recorder faceRecorder = this::recordFace;
    /** Centro del bloque (hora + fecha) y su altura, en px reales: el Stage los usa para ubicar lo demás. */
    public float centerX, centerY, dateBottom;

    public ClockLayer(Context context) {
        super("clock");
        this.context = context;
        time = Typography.paint(context, 240, 200, true);
        time.setLetterSpacing(-6f / 240f);
        secs = Typography.paint(context, 76, 300, true);
        date = Typography.paint(context, 32, 300, false);
        ampm = Typography.paint(context, 30, 300, false);
        face.setHasOverlappingRendering(false);
        system24 = DateFormat.is24HourFormat(context);
    }

    public void setTheme(Theme t) {
        theme = t;
        time.setColor(t.text);
        secs.setColor(t.seconds);
        ampm.setColor(t.seconds);
        date.setColor(t.textSecondary);
        faceDirty = true;
    }

    public void setSecondsMode(int m) {
        secondsMode = m;
        faceDirty = true;
    }

    public void setHourFormat(int f) {
        hourFormat = f;
        system24 = DateFormat.is24HourFormat(context);
        dayKey = -1;
        faceDirty = true;
    }

    /** Fecha cacheada en el snapshot: evita inicializar ICU en el primer frame. */
    public void seedDate(int day, String text) {
        if (day >= 0 && !text.isEmpty()) {
            dayKey = day;
            dateText = text;
        }
    }

    public int dateDay() {
        return dayKey;
    }

    public String dateText() {
        return dateText;
    }

    /** El locale o el huso cambiaron: rehacer la fecha. */
    public void resetDate() {
        zone = TimeZone.getDefault();
        system24 = DateFormat.is24HourFormat(context);
        dayKey = -1;
        ampmKey = -1;
        faceDirty = true;
    }

    @Override
    protected void onSizeChanged() {
        time.setTextSize(240 * s);
        secs.setTextSize(76 * s);
        date.setTextSize(32 * s);
        ampm.setTextSize(30 * s);
        line.setStrokeWidth(0);
        centerX = w / 2f;
        centerY = 370 * s;
        node.setPivotX(centerX);
        node.setPivotY(centerY);
        face.setPosition(0, 0, w, h);
        faceDirty = true;
    }

    /** Llamado cada segundo. Devuelve true si la cara cambió. */
    public boolean tick(long wallMs) {
        // Aritmética sobre el offset del huso: Calendar.get() asigna memoria en cada llamada.
        long local = wallMs + zone.getOffset(wallMs);
        int secOfDay = (int) Math.floorMod(local / 1000, 86_400L);
        int hour = secOfDay / 3600, min = secOfDay / 60 % 60, sec = secOfDay % 60;
        boolean h24 = hourFormat == HOUR_24 || (hourFormat == HOUR_SYSTEM && system24);
        int shown = h24 ? hour : (hour % 12 == 0 ? 12 : hour % 12);
        int len = 0;
        if (h24 || shown >= 10) hhmm[len++] = (char) ('0' + shown / 10);
        hhmm[len++] = (char) ('0' + shown % 10);
        hhmm[len++] = ':';
        hhmm[len++] = (char) ('0' + min / 10);
        hhmm[len++] = (char) ('0' + min % 10);
        hhmmLen = len;
        ss[0] = (char) ('0' + sec / 10);
        ss[1] = (char) ('0' + sec % 10);
        second = sec;
        int day = (int) Math.floorDiv(local, 86_400_000L);
        if (day != dayKey) {
            dayKey = day;
            cal.setTimeZone(zone);
            cal.setTimeInMillis(wallMs);
            Locale l = Locale.getDefault();
            String d = new SimpleDateFormat(DateFormat.getBestDateTimePattern(l, "EEEEdMMMM"), l).format(cal.getTime());
            dateText = d.isEmpty() ? d : d.substring(0, 1).toUpperCase(l) + d.substring(1);
        }
        int pm = h24 ? -1 : (hour < 12 ? Calendar.AM : Calendar.PM);
        if (pm != ampmKey) {
            ampmKey = pm;
            ampmText = pm < 0 ? "" : (pm == Calendar.AM ? amPm(true) : amPm(false));
        }
        if (secondsMode == SECONDS_OFF && sec != 0 && !faceDirty) return false;
        faceDirty = true;
        return true;
    }

    private static String amPm(boolean am) {
        String[] s = new java.text.DateFormatSymbols(Locale.getDefault()).getAmPmStrings();
        return am ? s[0] : s[1];
    }

    @Override
    protected void prepare() {
        if (!faceDirty || theme == null || w == 0) return;
        faceDirty = false;
        record(face, w, h, faceRecorder);
    }

    /**
     * Igual que el mockup: una fila [HH:mm | columna de 96 px con a. m./p. m. y segundos] alineada por la línea
     * base y centrada como bloque; debajo, la línea del segundero (si va) y la fecha. El bloque entero se centra
     * en centerY (la franja de 740 px sobre la fila; en ambiente y con panel, el Stage lo baja al centro real).
     */
    private void recordFace(RecordingCanvas c) {
        float tw = time.measureText(hhmm, 0, hhmmLen);
        float colW = secondsMode == SECONDS_NUMBER || !ampmText.isEmpty() ? 96 * s : 0;
        float gap = colW > 0 ? 18 * s : 0;
        Paint.FontMetrics tm = time.getFontMetrics(), dm = date.getFontMetrics();
        float timeBox = 240 * s;                       // line-height: 1
        float lineBox = secondsMode == SECONDS_LINE ? (18 + 2 + 10) * s : 0;
        float dateBox = dm.descent - dm.ascent;
        float blockH = timeBox + 4 * s + lineBox + dateBox;
        float top = centerY - blockH / 2;
        // Línea base de la hora dentro de su caja de 240 px.
        float baseline = top + timeBox / 2 - (tm.ascent + tm.descent) / 2;
        float x = centerX - (tw + gap + colW) / 2;
        c.drawText(hhmm, 0, hhmmLen, x, baseline, time);
        float colX = x + tw + gap;
        if (secondsMode == SECONDS_NUMBER) {
            c.drawText(ss, 0, 2, colX, baseline, secs);
            if (!ampmText.isEmpty()) c.drawText(ampmText, colX, baseline - 76 * s * 0.78f - 12 * s, ampm);
        } else if (!ampmText.isEmpty()) {
            c.drawText(ampmText, colX, baseline, ampm);
        }
        float y = top + timeBox + 4 * s;
        if (secondsMode == SECONDS_LINE) {
            float lw = 560 * s, lx = centerX - lw / 2, ly = y + 18 * s, lh = 2 * s;
            line.setColor(theme.roundBg);
            c.drawRect(lx, ly, lx + lw, ly + lh, line);
            line.setColor(theme.seconds);
            c.drawRect(lx, ly, lx + lw * (second / 60f), ly + lh, line);
            y += lineBox;
        }
        float dw = date.measureText(dateText);
        c.drawText(dateText, centerX - dw / 2, y - dm.ascent, date);
        dateBottom = y + dateBox;
    }

    /** Cuánto baja el reloj para quedar en el centro de la pantalla (ambiente y panel abierto). */
    public float fullScreenShiftY() {
        return h / 2f - centerY;
    }

    @Override
    protected void record(RecordingCanvas c) {
        c.drawRenderNode(face);
    }
}
