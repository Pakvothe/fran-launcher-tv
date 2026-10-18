package pakvothe.franlauncher.tv.theme;

/** Todos los colores de la UI. Inmutable: al cambiar tema o fase se crea otro y se re-graban los nodos afectados. */
public final class Theme {
    public static final int MODE_AUTO = 0, MODE_DARK = 1, MODE_LIGHT = 2;

    public final int mode, phase;
    public final boolean light;
    public final int skyTop, skyBottom, orbA, orbB;
    public final int text, textSecondary, seconds, roundBg, focusRing, shadowRest, shadowFocus, panel, divider;

    private Theme(int mode, int phase) {
        this.mode = mode;
        this.phase = phase;
        light = mode == MODE_LIGHT || (mode == MODE_AUTO && (phase == PhaseResolver.DAWN || phase == PhaseResolver.DAY));
        int[] sky = (light ? Palettes.LIGHT : Palettes.DARK)[phase];
        skyTop = sky[0];
        skyBottom = sky[1];
        orbA = sky[2];
        orbB = sky[3];
        int ink = 0x16171D;
        if (light) {
            text = 0xFF000000 | ink;
            textSecondary = alpha(ink, 0.72f);
            seconds = alpha(ink, 0.55f);
            roundBg = alpha(ink, 0.08f);
            focusRing = 0xFF000000 | ink;
            shadowRest = alpha(0x282846, 0.18f);
            shadowFocus = alpha(0x282846, 0.30f);
            panel = alpha(0xF4F5F8, 0.88f);
            divider = alpha(ink, 0.10f);
        } else {
            text = 0xFFFFFFFF;
            textSecondary = alpha(0xFFFFFF, 0.88f);
            seconds = alpha(0xFFFFFF, 0.70f);
            roundBg = alpha(0xFFFFFF, 0.14f);
            focusRing = alpha(0xFFFFFF, 0.95f);
            shadowRest = alpha(0x000000, 0.30f);
            shadowFocus = alpha(0x000000, 0.50f);
            panel = alpha(0x15172A, 0.86f);
            divider = alpha(0xFFFFFF, 0.10f);
        }
    }

    public static Theme of(int mode, int phase) {
        return new Theme(mode, phase);
    }

    public boolean sameAs(int mode, int phase) {
        return this.mode == mode && this.phase == phase;
    }

    static int alpha(int rgb, float a) {
        return (Math.round(a * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
