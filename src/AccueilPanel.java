import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Écran Welcome : Find + liste dynamique (Nom : IP) avec bouton chat sur la même ligne.
 */
public class AccueilPanel extends JPanel {

    private final JPanel listePairs = new JPanel();
    private final JLabel statut = new JLabel("Appuie sur Find pour voir qui est en ligne sur le réseau");
    private final JLabel moiLabel = new JLabel(" ");
    private final Consumer<PairReseau> onChat;
    private final Runnable onFind;
    private List<PairReseau> cache = List.of();

    public AccueilPanel(String monNom, String monIp, Consumer<PairReseau> onChat,
                        Runnable onFind, Runnable onQuitter) {
        this.onChat = onChat;
        this.onFind = onFind;

        setLayout(new BorderLayout(0, 12));
        setBackground(ThemeChat.FOND);
        setBorder(new EmptyBorder(16, 20, 12, 20));
        setPreferredSize(new Dimension(480, 580));

        moiLabel.setText("Moi : " + monNom + " : " + monIp);
        moiLabel.setForeground(ThemeChat.TEXTE_MUTED);
        moiLabel.setFont(ThemeChat.police(Font.PLAIN, 12));

        add(creerEnTete(), BorderLayout.NORTH);
        add(creerCentre(), BorderLayout.CENTER);
        add(creerBas(onQuitter), BorderLayout.SOUTH);
    }

    public void afficherPairs(List<PairReseau> pairs) {
        this.cache = List.copyOf(pairs);
        reconstruireListe();
        statut.setForeground(ThemeChat.ACCENT_FONCE);
        statut.setText(pairs.isEmpty()
                ? "Personne d'autre en ligne pour le moment (lance une 2ᵉ instance ou un autre PC)"
                : pairs.size() + " appareil(s) en ligne");
    }

    public void afficherErreur(String message) {
        statut.setForeground(ThemeChat.DANGER);
        statut.setText(message);
    }

    private void reconstruireListe() {
        listePairs.removeAll();
        if (cache.isEmpty()) {
            JLabel vide = new JLabel("Aucun pair détecté : les noms : IP apparaîtront ici");
            vide.setForeground(ThemeChat.TEXTE_MUTED);
            vide.setFont(ThemeChat.police(Font.ITALIC, 12));
            listePairs.add(vide);
        } else {
            for (PairReseau p : cache) {
                listePairs.add(creerLigne(p));
                listePairs.add(Box.createVerticalStrut(8));
            }
        }
        listePairs.revalidate();
        listePairs.repaint();
    }

    /** Une ligne : [NomAppareil : IP] ........ [chat] */
    private JPanel creerLigne(PairReseau pair) {
        JPanel ligne = new JPanel(new BorderLayout(10, 0));
        ligne.setBackground(ThemeChat.CARTE);
        ligne.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeChat.BORDURE),
                new EmptyBorder(10, 12, 10, 12)));
        ligne.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel info = new JLabel(pair.libelle()); // Nom : IP
        info.setFont(ThemeChat.police(Font.PLAIN, 14));
        info.setForeground(ThemeChat.TEXTE);
        ligne.add(info, BorderLayout.CENTER);

        BoutonChat btn = new BoutonChat("chat", ThemeChat.ACCENT_FONCE, Color.WHITE);
        btn.setPreferredSize(new Dimension(90, 34));
        btn.addActionListener(e -> onChat.accept(pair));
        ligne.add(btn, BorderLayout.EAST);

        return ligne;
    }

    private JPanel creerEnTete() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel welcome = new JLabel("Welcome");
        welcome.setFont(ThemeChat.police(Font.BOLD, 28));
        welcome.setForeground(ThemeChat.TEXTE);
        p.add(welcome, BorderLayout.WEST);

        JPanel icones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        icones.setOpaque(false);
        icones.add(icone("icon-horloge.png", "Horloge"));
        icones.add(icone("icon-calco.png", "Calcu"));
        JLabel msg = new JLabel("💬");
        msg.setToolTipText("Messagerie");
        msg.setFont(ThemeChat.police(Font.PLAIN, 22));
        icones.add(msg);
        p.add(icones, BorderLayout.EAST);
        return p;
    }

    private static JLabel icone(String fichier, String tip) {
        JLabel l = new JLabel();
        var icon = IconesChat.charger(fichier, 28);
        if (icon != null) {
            l.setIcon(icon);
        }
        l.setToolTipText(tip);
        return l;
    }

    private JPanel creerCentre() {
        JPanel centre = new JPanel(new BorderLayout(0, 10));
        centre.setOpaque(false);

        BoutonChat find = new BoutonChat("Find", ThemeChat.ACCENT, Color.WHITE);
        find.addActionListener(e -> {
            statut.setForeground(ThemeChat.TEXTE_MUTED);
            statut.setText("Recherche des appareils sur le réseau…");
            onFind.run();
            reconstruireListe();
        });
        JPanel barre = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        barre.setOpaque(false);
        barre.add(find);
        centre.add(barre, BorderLayout.NORTH);

        listePairs.setLayout(new BoxLayout(listePairs, BoxLayout.Y_AXIS));
        listePairs.setOpaque(true);
        listePairs.setBackground(ThemeChat.CARTE);
        listePairs.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(listePairs);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeChat.BORDURE));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel bloc = new JPanel(new BorderLayout(0, 6));
        bloc.setOpaque(false);
        JLabel titre = new JLabel("Appareils en ligne (nom : adresse IP)");
        titre.setForeground(ThemeChat.TEXTE_MUTED);
        titre.setFont(ThemeChat.police(Font.PLAIN, 12));
        bloc.add(titre, BorderLayout.NORTH);
        bloc.add(scroll, BorderLayout.CENTER);

        statut.setFont(ThemeChat.police(Font.ITALIC, 12));
        statut.setForeground(ThemeChat.TEXTE_MUTED);
        JPanel basBloc = new JPanel(new BorderLayout());
        basBloc.setOpaque(false);
        basBloc.add(statut, BorderLayout.NORTH);
        basBloc.add(moiLabel, BorderLayout.SOUTH);
        bloc.add(basBloc, BorderLayout.SOUTH);

        centre.add(bloc, BorderLayout.CENTER);
        return centre;
    }

    private JPanel creerBas(Runnable onQuitter) {
        JPanel bas = new JPanel(new BorderLayout());
        bas.setOpaque(false);

        BoutonChat quitter = new BoutonChat("Quitter", ThemeChat.DANGER, Color.WHITE);
        quitter.addActionListener(e -> onQuitter.run());
        JPanel g = new JPanel(new FlowLayout(FlowLayout.LEFT));
        g.setOpaque(false);
        g.add(quitter);
        bas.add(g, BorderLayout.WEST);

        JLabel signature = new JLabel("designed by Gabrielle Sara  ♥", SwingConstants.RIGHT);
        signature.setFont(new Font(Font.SERIF, Font.ITALIC, 12));
        signature.setForeground(ThemeChat.TEXTE_MUTED);
        bas.add(signature, BorderLayout.EAST);
        return bas;
    }
}
