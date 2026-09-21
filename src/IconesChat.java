import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** Charge les icônes du projet (horloge, calcu, …). */
public final class IconesChat {

    private IconesChat() {
    }

    public static ImageIcon charger(String nomFichier, int taille) {
        BufferedImage img = lire(nomFichier);
        if (img == null) {
            return null;
        }
        Image scaled = img.getScaledInstance(taille, taille, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static List<Image> iconesFenetre() {
        List<Image> list = new ArrayList<>();
        BufferedImage img = lire("icon-chat.png");
        if (img == null) {
            img = lire("icon-horloge.png");
        }
        if (img == null) {
            return list;
        }
        for (int t : new int[] {16, 32, 64}) {
            list.add(img.getScaledInstance(t, t, Image.SCALE_SMOOTH));
        }
        return list;
    }

    private static BufferedImage lire(String nom) {
        try (InputStream in = IconesChat.class.getResourceAsStream("/" + nom)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
            // fallback
        }
        try {
            File f = new File("resources", nom);
            if (!f.isFile()) {
                f = new File("out", nom);
            }
            if (f.isFile()) {
                return ImageIO.read(f);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }
}
