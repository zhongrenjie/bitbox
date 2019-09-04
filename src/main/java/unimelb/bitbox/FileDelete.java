package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

/**
 * The class with request and response methods for file deletion.
 */
public class FileDelete {

    /**
     * The socket passed by ThreadPeer.
     */
    private Socket socket;

    /**
     * Constructor for FileDelete.
     * @param socket the socket passed by ThreadPeer.
     */
    public FileDelete(Socket socket) {
        this.socket = socket;
    }

    /**
     * The method to process the detected local FileDelete events and send the file delete request to the
     * connected peers.
     * @param event The detected FileDelete events passed by ThreadPeer.
     * @throws IOException This is a mandatory exception when using I/O Stream.
     */
    public void FileDelete_request(FileSystemManager.FileSystemEvent event) throws IOException {
        try {
            // This is the file delete request message
            Document req = new Document();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
            req.append("command", "FILE_DELETE_REQUEST");

            // This is the descriptor object in the request message
            Document descriptor = new Document();
            descriptor.append("md5", event.fileDescriptor.md5);
            descriptor.append("lastModified", event.fileDescriptor.lastModified);
            descriptor.append("fileSize", event.fileDescriptor.fileSize);

            req.append("fileDescriptor", descriptor);
            req.append("pathName", event.pathName);

            bw.write(req.toJson() + "\n");
            bw.flush();
//
//            Document res = new Document();
//            String message = br.readLine();
//            res = Document.parse(message);
//            return res;
        } catch (NullPointerException e) {
            new InvalidProtocol(socket).InvalidMessage("the command is invalid");
        }catch (SocketException e) {
            socket.close();
        }
    }

    /**
     * The method to receive the FILE_DELETE_REQUEST message and process it with FileDelete_process method and
     * send a response message back to the peer who sent the request.
     * @param req The received FILE_DELETE document pass by ThreadPeer.
     * @param manager The fileSystemManager which is going to pass into the FileDelete_process method.
     * @throws IOException This is a mandatory exception when using I/O Stream.
     * @throws NoSuchAlgorithmException This is an exception which may occur when using the FileDelete_process method.
     */
    public void FileDelete_response(Document req, FileSystemManager manager) throws IOException, NoSuchAlgorithmException {
        
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        Document res = new Document();
        Document descriptor = (Document) req.get("fileDescriptor");
        String pathname = (String) req.get("pathName");

        res.append("command", "FILE_DELETE_RESPONSE");
        res.append("fileDescriptor", descriptor);
        res.append("pathName", pathname);

        String message = FileDelete_process(req, manager);
        if (message.equals("file deleted")) {
            res.append("status", true);
        } else {
            res.append("status", false);
        }

        bw.write(res.toJson() + "\n");
        bw.flush();
    }

    /**
     * The internal method of this class which use the APIs in the FileSystemManager to process the file delete request 
     * and identify the message of this deletion.
     * @param req The received file delete request document passed by FileDelete_response method.
     * @param manager The fileSystemManager used to conduct the file deletion.
     * @return The message of this delete operation.
     * @exception NoSuchAlgorithmException This is a mandatory exception when using the createFileLoader API.
     * @exception IOException This is a mandatory exception when using the createFileLoader API.
     */
    private String FileDelete_process(Document req, FileSystemManager manager) throws NoSuchAlgorithmException, IOException {
        String message;

        // get the path from the req document
        String pathname = (String) req.get("pathName");
        Document fileDescriptor = new Document();
        fileDescriptor = (Document) req.get("fileDescriptor");
        String md5 = (String) fileDescriptor.get("md5");
        long lastModified = (long) fileDescriptor.get("lastModified");
        long length = (long) fileDescriptor.get("fileSize");

        // check if the given path is safe
        if (manager.isSafePathName(pathname)) {
            // check if the given path existed or not
            // check if the file existed
            if (manager.fileNameExists(pathname, md5)) {
                boolean temp = manager.createFileLoader(pathname, md5, length, lastModified);
                // System.out.println(temp);
                // check if the delete operation success
                if (manager.deleteFile(pathname, lastModified, md5)) {
                    message = "file deleted";
                } else {
                    message = "there was a problem deleting the file";
                }
            } else {
                message = "file does not exist";
            }

        } else {
            message = "unsafe pathname given";
        }
        manager.cancelFileLoader(pathname);
        return message;
    }
}
