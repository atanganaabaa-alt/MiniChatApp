import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Mini-chat P2P local (découverte UDP + messages TCP).
 *
 * Test sur 1 PC : lance DEUX fois MiniChatApp (deux fenêtres).
 * Test sur 2 PC : même Wi‑Fi/LAN, lance une instance sur chaque machine.
 *
 * javac -d out src/*.java && cp resources/*.png out/
 * java -cp out MiniChatApp
 */
public class MiniChatApp extends JFrame implements EcouteurReseau {

    private static final String VUE_ACCUEIL = "accueil";
    private static final String VUE_CHAT = "chat";

    private final CardLayout cartes = new CardLayout();
    private final JPanel zone = new JPanel(cartes);
    private final AccueilPanel accueil;
    private final ChatPanel chatPanel;
    private final ServiceReseau reseau;

    public MiniChatApp() throws IOException {
        super("Mini Chat : Gabrielle Sara");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(ThemeChat.FOND);
        getContentPane().setLayout(new BorderLayout());

        List<Image> icones = IconesChat.iconesFenetre();
        if (!icones.isEmpty()) {
            setIconImages(icones);
        }

        reseau = new ServiceReseau(this);
        chatPanel = new ChatPanel(() -> afficher(VUE_ACCUEIL));
        accueil = new AccueilPanel(
                reseau.getMonNom(),
                reseau.getMonIp(),
                pair -> reseau.ouvrirChatVers(pair),
                () -> {
                    /* Find = rafraîchir l'affichage ; la découverte tourne déjà en fond */
                },
                this::demanderQuitter);

        zone.setOpaque(false);
        zone.add(accueil, VUE_ACCUEIL);
        zone.add(chatPanel, VUE_CHAT);
        getContentPane().add(zone, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                demanderQuitter();
            }
        });

        reseau.demarrer();
        setTitle("Mini Chat : " + reseau.getMonNom() + " : " + reseau.getMonIp());

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void afficher(String vue) {
        cartes.show(zone, vue);
    }

    @Override
    public void pairsMisAJour(List<PairReseau> pairs) {
        SwingUtilities.invokeLater(() -> accueil.afficherPairs(pairs));
    }

    @Override
    public void sessionChatOuverte(SessionChat session) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.lierSession(session);
            afficher(VUE_CHAT);
        });
    }

    @Override
    public void erreurReseau(String message) {
        SwingUtilities.invokeLater(() -> accueil.afficherErreur(message));
    }

    private void demanderQuitter() {
        if (DialogueQuitter.confirmer(this)) {
            reseau.close();
            dispose();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                MiniChatApp app = new MiniChatApp();
                app.setVisible(true);
                app.toFront();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Impossible de démarrer le réseau :\n" + ex.getMessage()
                                + "\n\nVérifie le Wi‑Fi / autorise Java dans le pare-feu.",
                        "Erreur réseau",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
