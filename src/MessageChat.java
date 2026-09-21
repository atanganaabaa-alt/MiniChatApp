/** Un message dans le fil de discussion. */
public class MessageChat {
    private final String auteur;
    private final String texte;
    private final boolean deMoi;

    public MessageChat(String auteur, String texte, boolean deMoi) {
        this.auteur = auteur;
        this.texte = texte;
        this.deMoi = deMoi;
    }

    public String getAuteur() {
        return auteur;
    }

    public String getTexte() {
        return texte;
    }

    public boolean isDeMoi() {
        return deMoi;
    }
}
