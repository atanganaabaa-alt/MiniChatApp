import java.util.Objects;

/**
 * Pair découvert sur le réseau local (nom d'appareil + IP + port TCP chat).
 */
public final class PairReseau {

    private final String id;
    private final String nomAppareil;
    private final String adresseIp;
    private final int portTcp;
    private volatile long derniereAnnonceMs;

    public PairReseau(String id, String nomAppareil, String adresseIp, int portTcp) {
        this.id = id;
        this.nomAppareil = nomAppareil;
        this.adresseIp = adresseIp;
        this.portTcp = portTcp;
        this.derniereAnnonceMs = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getNomAppareil() {
        return nomAppareil;
    }

    public String getAdresseIp() {
        return adresseIp;
    }

    public int getPortTcp() {
        return portTcp;
    }

    public void toucher() {
        derniereAnnonceMs = System.currentTimeMillis();
    }

    public boolean estActif(long delaiMs) {
        return System.currentTimeMillis() - derniereAnnonceMs <= delaiMs;
    }

    /** Affichage : NomAppareil : 192.168.x.x */
    public String libelle() {
        return nomAppareil + " : " + adresseIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PairReseau p)) {
            return false;
        }
        return Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
