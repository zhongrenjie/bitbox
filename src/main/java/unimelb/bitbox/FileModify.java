package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class FileModify {

    private Socket socket;

    public FileModify(Socket socket) {
        this.setSocket(socket);
    }
    
    /*
     * FileModify_request: almost the same as file create, only change the command from "FILE_CREATE_REQUEST" to "FILE_MODIFY_REQUEST".
     */
    public void FileModify_request(FileSystemEvent event) throws IOException, NoSuchAlgorithmException
    {
        String pathName = event.pathName;
        Document fileDescriptor = event.fileDescriptor.toDoc();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

        Document req = new Document();
        req.append("command", "FILE_MODIFY_REQUEST");
        req.append("fileDescriptor", fileDescriptor);
        req.append("pathName", pathName);

        bw.write(req.toJson() + "\n");
        bw.flush();
    }
    
    /*
     * FileModify_response: almost the same as FileCreate_response, only change the command information.
     */
    public boolean FileModify_response(Document req, FileSystemManager fm) throws IOException, NoSuchAlgorithmException,NullPointerException
    {
            Document fileDescriptor = (Document) req.get("fileDescriptor");
            Document res = new Document();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

            String pathName = (String) req.get("pathName");
            res.append("command", "FILE_MODIFY_RESPONSE");
            res.append("fileDescriptor", fileDescriptor);
            res.append("pathName", pathName);
            String message = process(pathName, fileDescriptor, fm);
            res.append("message", message);

            if (message.equals("file loader ready")) {
                res.append("status", true);
                bw.write(res.toJson() + "\n");
                //System.out.println(res.toJson());
                bw.flush();
                FileBytes fb = new FileBytes(this.socket);
                fb.FileBytes_request(fm, req);
                return true;
            }
            else {
            	res.append("status", false);
                bw.write(res.toJson() + "\n");
                bw.flush();
                return false;
            }
    }

    /*
     * process: this function takes the information of the existing file, and check if the modify file loader succeed.
     * parameter: String pathName, Document fileDescriptor, FileSystemManager fm.
     * return: the message whether the modify file loader succeed.
     */
    private String process(String pathName, Document fileDescriptor, FileSystemManager fm) throws NoSuchAlgorithmException, IOException {
        String md5 = (String) fileDescriptor.get("md5");
        long lastModified = (long) fileDescriptor.get("lastModified");

        String message = "";
        if (fm.isSafePathName(pathName)) {
            if (fm.fileNameExists(pathName)) {
                if (!fm.checkShortcut(pathName)) {
                    if (fm.modifyFileLoader(pathName, md5, lastModified)) {
                        message += "file loader ready";
                    } else {
                        //System.out.println("there is something wrong with file loader");
                    	message += "there is something wrong with file loader";
                    }
                } else {
                    message += "there is a shortcut";
                }
            } else {
                message += "pathname already exists";
            }
        } else {
            message += "unsafe pathname given";
        }
        return message;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

}
