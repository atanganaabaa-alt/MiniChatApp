import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/** Bouton arrondi simple pour le mini-chat. */
public class BoutonChat extends JButton {

    private final Color fond;
    private final Color fondSurvol;
    private boolean survol;

    public BoutonChat(String texte, Color fond, Color texteCouleur) {
        super(texte);
        this.fond = fond;
        this.fondSurvol = fond.brighter();
        setForeground(texteCouleur);
        setFont(ThemeChat.police(Font.BOLD, 13));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 16, 8, 16));
        setPreferredSize(new Dimension(110, 38));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                survol = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                survol = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(survol ? fondSurvol : fond);
        g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2f, getHeight() - 2f, ThemeChat.RAYON, ThemeChat.RAYON));
        g2.dispose();
        super.paintComponent(g);
    }
}
