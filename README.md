# Mini Chat P2P (Swing)

Chat réel entre 2 personnes sur le **même réseau local** (Wi-Fi / Ethernet).

## Lancer

```bash
cd ~/Projects/mini-chat
javac -d out src/*.java
cp resources/*.png out/
java -cp out MiniChatApp
```

**Pour tester à 2 :**
- **1 PC** : ouvre **deux** terminaux et lance `java -cp out MiniChatApp` deux fois  
- **2 PC** : même Wi‑Fi, une instance sur chaque machine  

Puis **Find** → tu vois `NomAppareil : 192.168.x.x` → **chat** sur la même ligne → discutez.

## Fonctionnement technique

| Couche | Techno | Rôle |
|--------|--------|------|
| Découverte | UDP multicast `230.0.0.1:4446` | Qui est en ligne (nom + IP + port) |
| Messages | TCP (port dynamique) | Discussion bidirectionnelle réelle |
| UI | Swing `CardLayout` | Accueil + chat |

## Pare-feu

Si tu ne vois personne : autorise Java (UDP 4446 + TCP) sur le réseau privé.
