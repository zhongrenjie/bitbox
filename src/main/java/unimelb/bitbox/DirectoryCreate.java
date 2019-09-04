package unimelb.bitbox;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class DirectoryCreate {

    private Socket socket;

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public DirectoryCreate(Socket socket) {
        this.setSocket(socket);
    }

    public Void DirectoryCreate_request(FileSystemEvent event) throws IOException {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
            Document req = new Document();
            Document res = new Document();
            req.append("command", "DIRECTORY_CREATE_REQUEST");
            req.append("pathName", event.pathName);

            bw.write(req.toJson() + "\n");
            bw.flush();
//
//            String message = br.readLine();
//            res = Document.parse(message);
//            return res;
        } catch (NullPointerException e) {
            new InvalidProtocol(socket).InvalidMessage("the command is invalid");
        }catch (SocketException e) {
            socket.close();
        }
        return null;
    }

    public void DirectoryCreate_response(Document req, FileSystemManager fm) throws IOException {
        Document res = new Document();
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        String pathName = (String) req.get("pathName");
        res.append("command", "DIRECTORY_CREATE_RESPONSE");
        res.append("pathName", pathName);
        String message = process(pathName, fm);
        res.append("message", message);
        if (message.equals("directory created")) {
            res.append("status", true);
        } else {
            res.append("status", false);
        }

        output.write(res.toJson() + "\n");
        output.flush();
    }

    private String process(String pathName, FileSystemManager fm) {
        if (fm.isSafePathName(pathName)) {
            if (!fm.dirNameExists(pathName)) {
                if (fm.makeDirectory(pathName)) {
                    return "directory created";
                } else {
                    return "there was a problem creating the directory";
                }
            } else {
                return "pathname already exists";
            }
        } else {
            return "unsafe pathname given";
        }
    }
}
