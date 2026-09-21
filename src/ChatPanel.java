import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Discussion réelle (TCP) : messages des deux côtés, pas de faux "Reçu…".
 */
public class ChatPanel extends JPanel {

    private final JLabel titre = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel fil = new JPanel();
    private final JScrollPane scroll;
    private final JTextField champ = new JTextField();
    private final JLabel statut = new JLabel(" ");
    private final List<MessageChat> historique = new ArrayList<>();
    private final Runnable onRetour;

    private SessionChat session;

    public ChatPanel(Runnable onRetour) {
        this.onRetour = onRetour;
        setLayout(new BorderLayout(0, 8));
        setBackground(ThemeChat.FOND);
        setBorder(new EmptyBorder(12, 16, 12, 16));
        setPreferredSize(new Dimension(480, 580));

        add(creerHaut(), BorderLayout.NORTH);

        fil.setLayout(new BoxLayout(fil, BoxLayout.Y_AXIS));
        fil.setBackground(ThemeChat.CARTE);
        fil.setBorder(new EmptyBorder(10, 10, 10, 10));
        scroll = new JScrollPane(fil);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeChat.BORDURE));
        add(scroll, BorderLayout.CENTER);

        JPanel bas = new JPanel(new BorderLayout(0, 4));
        bas.setOpaque(false);
        bas.add(creerSaisie(), BorderLayout.CENTER);
        statut.setFont(ThemeChat.police(Font.ITALIC, 11));
        statut.setForeground(ThemeChat.TEXTE_MUTED);
        bas.add(statut, BorderLayout.SOUTH);
        add(bas, BorderLayout.SOUTH);
    }

    public void lierSession(SessionChat session) {
        fermerSessionCourante();
        this.session = session;
        historique.clear();
        titre.setText(session.getDistant().libelle());
        statut.setText("Connecté : écris un message");
        statut.setForeground(ThemeChat.ACCENT_FONCE);
        rafraichirFil();

        session.onMessage(texte -> SwingUtilities.invokeLater(() -> {
            historique.add(new MessageChat(session.getDistant().getNomAppareil(), texte, false));
            rafraichirFil();
        }));
        session.onFermeture(() -> SwingUtilities.invokeLater(() -> {
            statut.setForeground(ThemeChat.DANGER);
            statut.setText("Connexion fermée");
        }));
        champ.requestFocusInWindow();
    }

    public void quitterVersAccueil() {
        fermerSessionCourante();
        onRetour.run();
    }

    private void fermerSessionCourante() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    private JPanel creerHaut() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        BoutonChat retour = new BoutonChat("← Retour", ThemeChat.BULLE_AUTRE, ThemeChat.TEXTE);
        retour.setPreferredSize(new Dimension(110, 34));
        retour.addActionListener(e -> quitterVersAccueil());
        p.add(retour, BorderLayout.WEST);
        titre.setFont(ThemeChat.police(Font.BOLD, 14));
        titre.setForeground(ThemeChat.TEXTE);
        p.add(titre, BorderLayout.CENTER);
        return p;
    }

    private JPanel creerSaisie() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        champ.setFont(ThemeChat.police(Font.PLAIN, 14));
        champ.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeChat.BORDURE),
                new EmptyBorder(8, 10, 8, 10)));
        champ.addActionListener(e -> envoyer());
        p.add(champ, BorderLayout.CENTER);
        BoutonChat envoyer = new BoutonChat("Envoyer", ThemeChat.ACCENT, Color.WHITE);
        envoyer.addActionListener(e -> envoyer());
        p.add(envoyer, BorderLayout.EAST);
        return p;
    }

    private void envoyer() {
        String texte = champ.getText().trim();
        if (texte.isEmpty() || session == null || !session.estOuverte()) {
            return;
        }
        try {
            session.envoyer(texte);
            historique.add(new MessageChat("Moi", texte, true));
            champ.setText("");
            rafraichirFil();
        } catch (IOException ex) {
            statut.setForeground(ThemeChat.DANGER);
            statut.setText("Envoi impossible : " + ex.getMessage());
        }
    }

    private void rafraichirFil() {
        fil.removeAll();
        for (MessageChat m : historique) {
            fil.add(creerBulle(m));
            fil.add(Box.createVerticalStrut(8));
        }
        fil.add(Box.createVerticalGlue());
        fil.revalidate();
        fil.repaint();
        SwingUtilities.invokeLater(() ->
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
    }

    private Component creerBulle(MessageChat m) {
        JPanel ligne = new JPanel(new FlowLayout(m.isDeMoi() ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        ligne.setOpaque(false);
        JLabel bulle = new JLabel("<html><body style='width:220px'>" + echapper(m.getTexte()) + "</body></html>");
        bulle.setOpaque(true);
        bulle.setBackground(m.isDeMoi() ? ThemeChat.BULLE_MOI : ThemeChat.BULLE_AUTRE);
        bulle.setForeground(m.isDeMoi() ? Color.WHITE : ThemeChat.TEXTE);
        bulle.setFont(ThemeChat.police(Font.PLAIN, 13));
        bulle.setBorder(new EmptyBorder(10, 12, 10, 12));
        ligne.add(bulle);
        return ligne;
    }

    private static String echapper(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
