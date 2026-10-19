package pakvothe.franlauncher.tv.data;

import java.text.Normalizer;

/** Índice de letras del panel Todas: '#' (dígitos y otros) + A–Z. Los acentos van a su letra (Á → A, Ñ → N). */
public final class Letters {
    public static final int COUNT = 27;
    public static final String LABELS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Letters() {}

    public static int bucket(String label) {
        if (label == null || label.isEmpty()) return 0;
        String first = Normalizer.normalize(label.substring(0, Character.charCount(label.codePointAt(0))),
            Normalizer.Form.NFD);
        char c = Character.toUpperCase(first.charAt(0));
        return c >= 'A' && c <= 'Z' ? 1 + (c - 'A') : 0;
    }
}
