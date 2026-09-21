import java.awt.Component;
import javax.swing.JOptionPane;

/** Confirmation avant de quitter. */
public final class DialogueQuitter {

    private DialogueQuitter() {
    }

    public static boolean confirmer(Component parent) {
        String[] options = {"oui", "non"};
        int choix = JOptionPane.showOptionDialog(
                parent,
                "Voulez-vous vraiment quitter ?",
                "Quitter",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
        return choix == 0;
    }
}
