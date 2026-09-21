/**
 * Écoute les événements réseau (à brancher sur l'UI via SwingUtilities.invokeLater).
 */
public interface EcouteurReseau {
    void pairsMisAJour(java.util.List<PairReseau> pairs);

    void sessionChatOuverte(SessionChat session);

    void erreurReseau(String message);
}
