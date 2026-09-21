import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/** Palette douce pour le mini-chat (Swing). */
public final class ThemeChat {

    private ThemeChat() {
    }

    public static final Color FOND = new Color(0xE8EEF4);
    public static final Color CARTE = new Color(0xF7FAFC);
    public static final Color ACCENT = new Color(0x5B8DEF);
    public static final Color ACCENT_FONCE = new Color(0x3B6FD9);
    public static final Color TEXTE = new Color(0x2D3748);
    public static final Color TEXTE_MUTED = new Color(0x718096);
    public static final Color BULLE_MOI = new Color(0x5B8DEF);
    public static final Color BULLE_AUTRE = new Color(0xE2E8F0);
    public static final Color DANGER = new Color(0xE53E3E);
    public static final Color BORDURE = new Color(0xCBD5E0);

    public static final int RAYON = 14;

    private static String famille;

    public static Font police(int style, int taille) {
        if (famille == null) {
            List<String> dispo = Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String p : List.of("Segoe UI", "Ubuntu", "Cantarell", "SansSerif")) {
                if (dispo.contains(p)) {
                    famille = p;
                    break;
                }
            }
            if (famille == null) {
                famille = Font.SANS_SERIF;
            }
        }
        return new Font(famille, style, taille);
    }
}
