package unimelb.bitbox;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

/**
 * Starts a ServerMain thread to monitor the local event, a ThreadListen to listen incoming connection and a
 * ThreadSyn to create periodic synchronise information. Then do a BFS to try to connect maximum numbers of peers.
 *
 */
public class Peer {
    /**
     *
     * @return Sockets of current connected peers.
     */
    public static ArrayList<Socket> getConnectedPeers() {
        return connectedPeers;
    }

    /**
     * Add the socket of a new connected peer to the list.
     * @param socket Socket of a new connected peer.
     */
    public static void addConnectedPeers(Socket socket) {
        connectedPeers.add(socket);
    }

    /**
     * Delete a connected peer from the list.
     * @param socket Socket of disconnected peer.
     */
    public static void delConnectedPeers(Socket socket) {
        connectedPeers.remove(socket);
    }

    /**
     * Add the host and port of a new peer.
     * @param e Host and port.
     */
    public static void addKnownPeers(HostPort e) {
        knownPeers.add(e);
    }

    /**
     *
     * @return A list of hosts and ports for known peers.
     */
    public static ArrayList<HostPort> getKnownPeers() {
        return knownPeers;
    }

    /**
     * Add the host and listening port of a new connected peer to the list.
     * @param e The host and listening port of the new connected peer.
     */
    public static void addconnectedPeerName(HostPort e) {
        connectedPeerName.add(e);
    }

    /**
     *
     * @return The list of hosts and listening ports of connected peers.
     */
    public static ArrayList<HostPort> getconnectedPeerName() {
        return connectedPeerName;
    }

    /**
     *
     * @return The list of detected local events.
     */
    public static ArrayList<FileSystemEvent> getEvent() {
        return Event;
    }

    /**
     * Add an event to local event list.
     * @param e An local event monitored by FileManager.
     */
    public static void addEvent(FileSystemEvent e) {
        Event.add(e);
    }

    /**
     *
     * @return The list of synchronized events.
     */
    public static ArrayList<FileSystemEvent> getsyn_Event() {
        return syn_Event;
    }

    /**
     * Add a synchronized event to the list of synchronized events.
     * @param e Periodic generated synchronized events.
     */
    public static void addsyn_Event(FileSystemEvent e) {
        syn_Event.add(e);
    }

    /**
     * The list of hosts and listening ports of connected peers.
     */
    private static ArrayList<HostPort> connectedPeerName = new ArrayList();

    /**
     * A list of hosts and ports for known peers.
     */
    private static ArrayList<HostPort> knownPeers = new ArrayList();
    /**
     * Sockets of current connected peers.
     */
    private static ArrayList<Socket> connectedPeers = new ArrayList();
    /**
     * The list of detected local events.
     */
    private static ArrayList<FileSystemEvent> Event = new ArrayList();
    /**
     * The list of synchronized events.
     */
    private static ArrayList<FileSystemEvent> syn_Event = new ArrayList();

    private static Logger log = Logger.getLogger(Peer.class.getName());

    public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();
        ServerMain serverMain = new ServerMain();

        ThreadListen server = new ThreadListen(serverMain.fileSystemManager);
        server.start();
        new ThreadSyn(serverMain.fileSystemManager).start();
        String[] peers = Configuration.getConfigurationValue("peers").split(",");
        try {
            for (String peer : peers) {
                knownPeers.add(new HostPort(peer));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Peer list is empty!");
        }
        System.out.println(knownPeers);
        int maximum_Connection = Integer.parseInt(
                Configuration.getConfigurationValue("maximumIncommingConnections"));
        ArrayList<ThreadClient> client = new ArrayList<>();
        for (int i = 0; connectedPeers.size() < maximum_Connection && i < knownPeers.size(); i++) {
            try {
                Socket socket = new Socket(knownPeers.get(i).host, knownPeers.get(i).port);
                client.add(new ThreadClient(socket, serverMain.fileSystemManager));
                client.get(i).start();

                try {
                    client.get(i).join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (ConnectException e) {
                System.out.println("Peer is offline!");
            }

            System.out.println("Known Peers List:");
            knownPeers.forEach((k) -> {
                System.out.println(k.toString());
            });
            System.out.println("Connected Peers List:");
            connectedPeers.forEach((action) -> {
                System.out.println(action.toString());
            });

        }
    }

}
