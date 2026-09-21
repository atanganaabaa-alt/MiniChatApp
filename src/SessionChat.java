import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Session TCP bidirectionnelle : une ligne UTF-8 = un message.
 */
public final class SessionChat implements AutoCloseable {

    private final Socket socket;
    private final PairReseau distant;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final List<Consumer<String>> ecouteursMessage = new ArrayList<>();
    private final List<Runnable> ecouteursFermeture = new ArrayList<>();
    private volatile boolean ouverte = true;
    private Thread lecteur;

    public SessionChat(Socket socket, PairReseau distant, BufferedReader in, BufferedWriter out) {
        this.socket = socket;
        this.distant = distant;
        this.in = in;
        this.out = out;
    }

    /** Connexion sortante : on envoie HELLO puis on discute. */
    public static SessionChat connecter(PairReseau distant, String monId, String monNom, int monPort)
            throws IOException {
        Socket socket = new Socket(distant.getAdresseIp(), distant.getPortTcp());
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out.write("HELLO|" + monId + "|" + monNom + "|" + monPort);
        out.newLine();
        out.flush();
        return new SessionChat(socket, distant, in, out);
    }

    /** Connexion entrante : on lit HELLO pour savoir qui appelle. */
    public static SessionChat accepter(Socket socket) throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        String hello = in.readLine();
        if (hello == null || !hello.startsWith("HELLO|")) {
            socket.close();
            throw new IOException("Handshake HELLO invalide");
        }
        String[] p = hello.split("\\|", 4);
        if (p.length < 4) {
            socket.close();
            throw new IOException("Handshake HELLO incomplet");
        }
        PairReseau distant = new PairReseau(
                p[1],
                p[2],
                socket.getInetAddress().getHostAddress(),
                Integer.parseInt(p[3]));
        return new SessionChat(socket, distant, in, out);
    }

    public PairReseau getDistant() {
        return distant;
    }

    public boolean estOuverte() {
        return ouverte && !socket.isClosed();
    }

    public void onMessage(Consumer<String> ecouteur) {
        ecouteursMessage.add(ecouteur);
    }

    public void onFermeture(Runnable ecouteur) {
        ecouteursFermeture.add(ecouteur);
    }

    public void demarrerLecture() {
        lecteur = new Thread(this::boucleLecture, "chat-read-" + distant.getAdresseIp());
        lecteur.setDaemon(true);
        lecteur.start();
    }

    private void boucleLecture() {
        try {
            String ligne;
            while (ouverte && (ligne = in.readLine()) != null) {
                for (Consumer<String> e : List.copyOf(ecouteursMessage)) {
                    e.accept(ligne);
                }
            }
        } catch (IOException ex) {
            // coupure
        } finally {
            fermerSilencieusement();
        }
    }

    public synchronized void envoyer(String texte) throws IOException {
        if (!estOuverte()) {
            throw new IOException("Session fermée");
        }
        out.write(texte.replace('\n', ' ').replace('\r', ' '));
        out.newLine();
        out.flush();
    }

    private void fermerSilencieusement() {
        if (!ouverte) {
            return;
        }
        ouverte = false;
        try {
            socket.close();
        } catch (IOException ignored) {
            // ignore
        }
        for (Runnable r : List.copyOf(ecouteursFermeture)) {
            r.run();
        }
    }

    @Override
    public void close() {
        fermerSilencieusement();
        if (lecteur != null) {
            lecteur.interrupt();
        }
    }
}
