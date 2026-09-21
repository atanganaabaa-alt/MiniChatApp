import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Découverte UDP multicast + serveur TCP pour le chat réel.
 * Datagramme : MINICHAT|id|nomAppareil|portTcp
 */
public final class ServiceReseau implements AutoCloseable {

    public static final String GROUPE_MULTICAST = "230.0.0.1";
    public static final int PORT_DECOUVERTE = 4446;
    public static final long TIMEOUT_PAIR_MS = 7000;

    private final String monId = UUID.randomUUID().toString();
    private final String monNom;
    private final String monIp;
    private final int monPortTcp;
    private final EcouteurReseau ecouteur;

    private final Map<String, PairReseau> pairs = new ConcurrentHashMap<>();
    private final ServerSocket serveurTcp;
    private volatile boolean actif = true;

    private MulticastSocket socketUdp;

    public ServiceReseau(EcouteurReseau ecouteur) throws IOException {
        this.ecouteur = ecouteur;
        this.monNom = InetAddress.getLocalHost().getHostName();
        this.monIp = detecterIpLocale();
        this.serveurTcp = new ServerSocket(0);
        this.monPortTcp = serveurTcp.getLocalPort();
    }

    public String getMonNom() {
        return monNom;
    }

    public String getMonIp() {
        return monIp;
    }

    public int getMonPortTcp() {
        return monPortTcp;
    }

    public void demarrer() throws IOException {
        socketUdp = new MulticastSocket(PORT_DECOUVERTE);
        socketUdp.setReuseAddress(true);
        InetAddress groupe = InetAddress.getByName(GROUPE_MULTICAST);
        try {
            NetworkInterface nif = NetworkInterface.getByInetAddress(InetAddress.getByName(monIp));
            if (nif != null) {
                socketUdp.joinGroup(new InetSocketAddress(groupe, PORT_DECOUVERTE), nif);
            } else {
                @SuppressWarnings("deprecation")
                boolean joined = joinLegacy(groupe);
                if (!joined) {
                    throw new IOException("joinGroup impossible");
                }
            }
        } catch (Exception ex) {
            joinLegacy(groupe);
        }
        socketUdp.setSoTimeout(1000);

        demarrerDaemon(this::boucleAnnonce, "udp-annonce");
        demarrerDaemon(this::boucleEcouteUdp, "udp-ecoute");
        demarrerDaemon(this::boucleAcceptTcp, "tcp-accept");
        demarrerDaemon(this::boucleNettoyage, "pairs-cleanup");
    }

    @SuppressWarnings("deprecation")
    private boolean joinLegacy(InetAddress groupe) throws IOException {
        socketUdp.joinGroup(groupe);
        return true;
    }

    private void demarrerDaemon(Runnable r, String nom) {
        Thread t = new Thread(r, nom);
        t.setDaemon(true);
        t.start();
    }

    public void ouvrirChatVers(PairReseau pair) {
        demarrerDaemon(() -> {
            try {
                SessionChat session = SessionChat.connecter(pair, monId, monNom, monPortTcp);
                session.demarrerLecture();
                ecouteur.sessionChatOuverte(session);
            } catch (IOException ex) {
                ecouteur.erreurReseau("Impossible de joindre " + pair.libelle() + " : " + ex.getMessage());
            }
        }, "tcp-connect");
    }

    private void boucleAnnonce() {
        String texte = "MINICHAT|" + monId + "|" + monNom + "|" + monPortTcp;
        byte[] payload = texte.getBytes(StandardCharsets.UTF_8);
        try {
            InetAddress groupe = InetAddress.getByName(GROUPE_MULTICAST);
            while (actif) {
                DatagramPacket packet = new DatagramPacket(payload, payload.length, groupe, PORT_DECOUVERTE);
                try (DatagramSocket ds = new DatagramSocket()) {
                    ds.setReuseAddress(true);
                    ds.send(packet);
                }
                Thread.sleep(2000);
            }
        } catch (Exception ex) {
            if (actif) {
                ecouteur.erreurReseau("Annonce UDP : " + ex.getMessage());
            }
        }
    }

    private void boucleEcouteUdp() {
        byte[] buf = new byte[1024];
        while (actif) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socketUdp.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                traiterAnnonce(msg, packet.getAddress().getHostAddress());
            } catch (SocketTimeoutException ignored) {
                // ok
            } catch (IOException ex) {
                if (actif) {
                    ecouteur.erreurReseau("Écoute UDP : " + ex.getMessage());
                }
            }
        }
    }

    private void traiterAnnonce(String msg, String ipSource) {
        if (!msg.startsWith("MINICHAT|")) {
            return;
        }
        String[] p = msg.split("\\|", 4);
        if (p.length < 4) {
            return;
        }
        String id = p[1];
        if (monId.equals(id)) {
            return;
        }
        int port;
        try {
            port = Integer.parseInt(p[3]);
        } catch (NumberFormatException ex) {
            return;
        }
        pairs.put(id, new PairReseau(id, p[2], ipSource, port));
        notifierPairs();
    }

    private void boucleAcceptTcp() {
        while (actif) {
            try {
                Socket socket = serveurTcp.accept();
                demarrerDaemon(() -> {
                    try {
                        SessionChat session = SessionChat.accepter(socket);
                        pairs.put(session.getDistant().getId(), session.getDistant());
                        notifierPairs();
                        session.demarrerLecture();
                        ecouteur.sessionChatOuverte(session);
                    } catch (IOException ex) {
                        ecouteur.erreurReseau("Connexion entrante : " + ex.getMessage());
                    }
                }, "tcp-hello");
            } catch (IOException ex) {
                if (actif) {
                    ecouteur.erreurReseau("Serveur TCP : " + ex.getMessage());
                }
            }
        }
    }

    private void boucleNettoyage() {
        while (actif) {
            try {
                Thread.sleep(2000);
                boolean change = pairs.entrySet().removeIf(e -> !e.getValue().estActif(TIMEOUT_PAIR_MS));
                if (change) {
                    notifierPairs();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void notifierPairs() {
        List<PairReseau> copie = new ArrayList<>(pairs.values());
        copie.sort((a, b) -> a.getNomAppareil().compareToIgnoreCase(b.getNomAppareil()));
        ecouteur.pairsMisAJour(copie);
    }

    private static String detecterIpLocale() throws IOException {
        Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
        while (nics.hasMoreElements()) {
            NetworkInterface nif = nics.nextElement();
            if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) {
                continue;
            }
            Enumeration<InetAddress> addrs = nif.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress a = addrs.nextElement();
                if (a.isSiteLocalAddress() && !a.getHostAddress().contains(":")) {
                    return a.getHostAddress();
                }
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }

    @Override
    public void close() {
        actif = false;
        try {
            if (socketUdp != null) {
                socketUdp.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            serveurTcp.close();
        } catch (IOException ignored) {
            // ignore
        }
    }
}
