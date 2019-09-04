package unimelb.bitbox;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import unimelb.bitbox.util.*;

public class InvalidProtocol
{
    private Socket socket;

    public void setSocket(Socket socket)
    {
        this.socket = socket;
    }

    public InvalidProtocol(Socket socket)
    {
        this.setSocket(socket);
    }

    public void InvalidMessage(String message) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

        Document res = new Document();
        res.append("command", "INVALID_PROTOCOL");
        res.append("message", message);
        System.out.println(message);

        bw.write(res.toJson() + "\n");
        bw.flush();
        socket.close();
    }
}
