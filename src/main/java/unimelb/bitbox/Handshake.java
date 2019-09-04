package unimelb.bitbox;

import java.io.*;
import java.net.Socket;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.*;

public class Handshake {

    private Socket socket;

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Handshake(Socket socket) {
        this.setSocket(socket);
    }

    public Document Handshake_request() throws ParseException, IOException {
        Document req = new Document();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));

        String local_host = Configuration.getConfigurationValue("advertisedName");
        int local_port = Integer.parseInt(Configuration.getConfigurationValue("port"));
        req.append("command", "HANDSHAKE_REQUEST");

        Document local_hostport = new Document();
        local_hostport.append("host", local_host);
        local_hostport.append("port", local_port);
        req.append("hostPort", local_hostport);

        bw.write(req.toJson() + "\n");
        bw.flush();

        Document res = new Document();
        String message = "";
        message = br.readLine();
        res = Document.parse(message);
        return res;
        
    }

    public void Handshake_response() throws ParseException, IOException {
        Document res = new Document();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

        String local_host = Configuration.getConfigurationValue("advertisedName");
        int local_port = Integer.parseInt(Configuration.getConfigurationValue("port"));

        res.append("command", "HANDSHAKE_RESPONSE");
        Document local_hostport = new Document();
        local_hostport.append("host", local_host);
        local_hostport.append("port", local_port);
        res.append("hostPort", local_hostport);

        bw.write(res.toJson() + "\n");
        bw.flush();
    }
}
