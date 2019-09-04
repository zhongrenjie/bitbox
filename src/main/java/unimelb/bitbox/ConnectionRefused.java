package unimelb.bitbox;

import org.json.simple.JSONObject;
import unimelb.bitbox.util.Document;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import unimelb.bitbox.util.HostPort;

/**
 * The class with a connection_refused method which will be called when receive the connection request but the
 * connection limit has reached.
 */

public class ConnectionRefused
{

    /**
     * The socket passed by ThreadServer.
     */
    private Socket socket;

    /**
     * Constructor for ConnectionRefused.
     * @param socket The socket passed by ThreadServer.
     */
    public ConnectionRefused(Socket socket)
    {
        this.socket = socket;
    }

    /**
     *  Send a CONNECTION_REFUSED message to the peer trying to connect with us.
     * @param peers The host and listened ports of connected peers.
     * @throws IOException This is a mandatory exception when using I/O exception.
     */
    public void connection_refused(ArrayList<HostPort> peers) throws IOException
    {
        Document res = new Document();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

        res.append("command", "CONNECTION_REFUSED");
        res.append("message", "connection limit reached");
        ArrayList<JSONObject> hostport = new ArrayList<>();

        for (HostPort peer : peers)
        {
            JSONObject hp = new JSONObject();
            hp.put("host", peer.host);
            hp.put("port", peer.port);
            hostport.add(hp);
        }
        res.append("peers", hostport);

        bw.write(res.toJson() + "\n");
        bw.flush();
    }
}
