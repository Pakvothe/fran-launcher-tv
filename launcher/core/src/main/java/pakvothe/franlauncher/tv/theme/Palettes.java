package pakvothe.franlauncher.tv.theme;

/** Colores del cielo por fase: arriba, abajo, orbe A, orbe B. Índice = fase de PhaseResolver. */
final class Palettes {
    static final int[][] DARK = {
        {0xFF0A0E1C, 0xFF1B2244, 0xFF3A4C9A, 0xFF5E3C80},
        {0xFF211A3B, 0xFF9E5F60, 0xFFE99A7C, 0xFF7A5AA8},
        {0xFF132E47, 0xFF336685, 0xFF7DBAD8, 0xFFD9C48E},
        {0xFF22152D, 0xFF8E4440, 0xFFE58A52, 0xFF6A3878},
    };
    static final int[][] LIGHT = {
        {0xFFE4E7F1, 0xFFC6CCE2, 0xFF94A2DE, 0xFFC2A8DC},
        {0xFFF7EEE8, 0xFFEDC4B2, 0xFFF1A487, 0xFFC6B2E6},
        {0xFFEEF4F8, 0xFFCCE0EC, 0xFF97C8E4, 0xFFEFDCA4},
        {0xFFF7EDE8, 0xFFE7B6A3, 0xFFEFA070, 0xFFC7A1D4},
    };

    private Palettes() {}
}
