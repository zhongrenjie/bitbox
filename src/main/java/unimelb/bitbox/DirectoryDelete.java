package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

/**
 * A class with request and response methods for directory delete operation.
 */

public class DirectoryDelete {

    /**
     * The socket passed from ThreadPeer.
     */
    private Socket socket;

    /**
     *  Constructor for DirectoryDelete.
     * @param socket the socket passed from ThreadPeer.
     */
    public DirectoryDelete(Socket socket) {
        this.socket = socket;
    }

    /**
     * The method to process the detected local DirectoryDelete events and send the directory delete request to the
     * connected peers.
     * @param event the DirectoryDelete events passed by ThreadPeer.
     * @throws IOException This is a mandatory exception when using I/O Stream.
     */
    public void DirectoryDelete_request(FileSystemEvent event) throws IOException {
        try {
            Document req = new Document();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));

            req.append("command", "DIRECTORY_DELETE_REQUEST");
            req.append("pathName", event.pathName);
            bw.write(req.toJson() + "\n");
            bw.flush();
            /*

            Document res = new Document();
            String message = br.readLine();
            res = Document.parse(message);
            return res;

            */
        } catch (NullPointerException e) {
            new InvalidProtocol(socket).InvalidMessage("the command is invalid");
        } catch (SocketException e) {
            socket.close();
        }
    }

    /**
     * The method to receive the DIRECTORY_DELETE_REQUEST message and process it with DirectoryDelete_process method and
     * send a response message back to the peer who sent the request.
     * @param req The received DIRECTORY_DELETE document pass by ThreadPeer.
     * @param manager The fileSystemManager which is going to pass into the DirectoryDelete_process method.
     * @throws IOException This is a mandatory exception when using I/O Stream.
     */
    public void DirectoryDelete_response(Document req, FileSystemManager manager) throws IOException {

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        Document res = new Document();
        String dir = (String) req.get("pathName");

        res.append("command", "DIRECTORY_DELETE_RESPONSE");
        res.append("pathName", dir);
        String message = DirectoryDelete_process(dir, manager);
        res.append("message", message);
        if (message.equals("directory deleted")) {
            res.append("status", true);
        } else {
            res.append("status", false);
        }

        bw.write(res.toJson() + "\n");
        bw.flush();
    }

    /**
     * The internal method of this class which use the APIs in the FileSystemManager to process the directory delete
     * request and identify the message of this deletion.
     * @param pathName The path name of the dir that is requested to be deleted.
     * @param manager The fileSystemManager used to conduct the directory deletion.
     * @return The message of this delete operation.
     */
    private String DirectoryDelete_process(String pathName, FileSystemManager manager) {
        String message;
        if (manager.isSafePathName(pathName)) {
            if (manager.dirNameExists(pathName)) {

                if (manager.deleteDirectory(pathName)) {
                    message = "directory deleted";
                } else {
                    message = "there was a problem deleting the directory";
                }
            } else {
                message = "pathname dose not exist";
            }
        } else {
            message = "unsafe pathname given";
        }

        return message;
    }
}
