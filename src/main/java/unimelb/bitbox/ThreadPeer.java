/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/**
 * A thread repeats monitoring incoming messages from the peer. Sort the message according to its command and pass it to
 * the specific file operation methods to perform the file operations. And send file operation requests to the peer
 * according to the local event list and synchronized event list.
 */
public class ThreadPeer extends Thread implements Runnable {

    private FileSystemManager fileSystemManager;

    /**
     * The socket passed from ThreadServer or ThreadClient.
     */
    private Socket socket;

    /**
     * To read the input of socket.
     */
    private BufferedReader input;

    /**
     * Constructor for ThreadListen.
     *
     * @param socket            The socket passed from ThreadServer or ThreadClient.
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public ThreadPeer(Socket socket, FileSystemManager fileSystemManager) {
        this.socket = socket;
        this.fileSystemManager = fileSystemManager;
        try {
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
        } catch (UnsupportedEncodingException ex) {

        } catch (IOException ex) {

        }
    }


    @Override

    public synchronized void run() {
        System.out.println("Peer start");

        try {
            // Create ArrayLists for recording the commands sent.
            ArrayList<String> DIRECTORY_CREATE = new ArrayList<>();
            ArrayList<String> DIRECTORY_DELETE = new ArrayList<>();
            ArrayList<String> FILE_DELETE = new ArrayList<>();
            ArrayList<String> FILE_CREATE = new ArrayList<>();
            ArrayList<String> FILE_MODIFY = new ArrayList<>();
            ArrayList<String> FILE_BYTE = new ArrayList<>();
            ArrayList<String> FILE_BYTETO = new ArrayList<>();

            // The JSON Parser
            Document doc = new Document();
            // Input stream
            // Client messages
            String clientMessage;
            int count = 0;
            int syn = 0;
            while (!socket.isClosed()) {
                // Send requests for local events and record them.
                if (Peer.getEvent().size() > count) {
                    FileSystemEvent eve = Peer.getEvent().get(count);
                    switch (eve.event) {
                        case FILE_CREATE:
                            FileCreate file_Cre = new FileCreate(socket);
                            FILE_CREATE.add(eve.fileDescriptor.md5);
                        {
                            try {
                                file_Cre.FileCreate_request(eve);
                                System.out.println("Send FILE_CREATE_REQUEST to : " + socket.getInetAddress().toString()
                                        + ":" + socket.getPort());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                        case FILE_DELETE:
                            // System.out.println("Connection closed: " + socket.isClosed());
                            FileDelete file_Del = new FileDelete(socket);
                            FILE_DELETE.add(eve.fileDescriptor.md5);
                        {
                            try {
                                file_Del.FileDelete_request(eve);
                                System.out.println("Send FILE_DELETE_REQUEST to : " + socket.getInetAddress().toString()
                                        + ":" + socket.getPort());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                        case FILE_MODIFY:
                            // System.out.println("Connection closed: " + socket.isClosed());
                            FileModify file_Mod = new FileModify(socket);
                            FILE_MODIFY.add(eve.fileDescriptor.md5);
                        {
                            try {
                                file_Mod.FileModify_request(eve);
                                System.out.println("Send FILE_MODIFY_REQUEST to : " + socket.getInetAddress().toString()
                                        + ":" + socket.getPort());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                        case DIRECTORY_CREATE:
                            DirectoryCreate dir_Cre = new DirectoryCreate(socket);
                            DIRECTORY_CREATE.add(eve.pathName);
                        {
                            try {
                                dir_Cre.DirectoryCreate_request(eve);
                                System.out.println("Send DIRECTORY_CREATE_REQUEST to : " + socket.getInetAddress().toString()
                                        + ":" + socket.getPort());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                        case DIRECTORY_DELETE:
                            DirectoryDelete dir_Del = new DirectoryDelete(socket);
                            DIRECTORY_DELETE.add(eve.pathName);
                            try {
                                dir_Del.DirectoryDelete_request(eve);
                                System.out.println("Send DIRECTORY_DELETE_REQUEST to : " + socket.getInetAddress().toString()
                                        + ":" + socket.getPort());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;

                        default:
                            break;
                    }
                    count++;
                }

                // Send request for synchronized events and record them.
                if (Peer.getsyn_Event().size() > syn) {
                    System.out.println("Syn detected");
                    System.out.println(syn);
                    // System.out.println(Peer.getsyn_Event());
                    System.out.println();
                    FileSystemEvent eve = Peer.getsyn_Event().get(syn);
                    if (!eve.event.toString().equals("DIRECTORY_CREATE")) {
                        FileCreate file_Cre = new FileCreate(socket);
                        FileModify file_Mod = new FileModify(socket);

                        try {
                            file_Cre.FileCreate_request(eve);
                            System.out.println("Send FILE_CREATE_REQUEST to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            FILE_CREATE.add(eve.fileDescriptor.md5);
                            file_Mod.FileModify_request(eve);
                            System.out.println("Send FILE_MODIFY_REQUEST to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            FILE_MODIFY.add(eve.fileDescriptor.md5);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    } else {
                        DirectoryCreate dir_Dir = new DirectoryCreate(socket);
                        dir_Dir.DirectoryCreate_request(eve);
                        System.out.println("Send DIRECTORY_CREATE_REQUEST to : " + socket.getInetAddress().toString()
                                + ":" + socket.getPort());
                        DIRECTORY_CREATE.add(eve.pathName);
                    }
                    syn++;
                }

                // Sort and process the incoming messages from peers
                if (input.ready()) {
                    clientMessage = input.readLine();
                    // Parse the request
                    Document request = Document.parse(clientMessage);
                    //System.out.println(request.toJson());

                    String command = request.getString("command");
                    System.out.println("Received from " + socket.getInetAddress().toString()
                            + ":" + socket.getPort() + " : " + command);
                    Document filedes;
                    String md5 = "";

                    if (!command.substring(0, 9).equals("DIRECTORY")) {

                        filedes = (Document) request.get("fileDescriptor");
                        md5 = filedes.getString("md5");

                    }
                    System.out.println(command);
                    // Classify the request
                    switch (command) {
                        case "FILE_DELETE_RESPONSE":
                            if (FILE_DELETE.contains(md5)) {
                                FILE_DELETE.remove(md5);
                            } else {
                                new InvalidProtocol(socket).InvalidMessage("Corresponding request is not found!");
                            }
                            break;

                        case "FILE_DELETE_REQUEST":
                            new FileDelete(socket).FileDelete_response(request, fileSystemManager);
                            System.out.println("Send FILE_DELETE_RESPONSE to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            // System.out.println("Connection closed: " + socket.isClosed());
                            break;

                        case "DIRECTORY_CREATE_RESPONSE":
                            String dcre = request.getString("pathName");
                            if (DIRECTORY_CREATE.contains(dcre)) {
                                DIRECTORY_CREATE.remove(dcre);
                            } else {
                                new InvalidProtocol(socket).InvalidMessage("Corresponding request is not found!");
                            }
                            break;

                        case "DIRECTORY_CREATE_REQUEST":
                            new DirectoryCreate(socket).DirectoryCreate_response(request, fileSystemManager);
                            System.out.println("Send DIRECTORY_CREATE_RESPONSE to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            // System.out.println("Connection closed: " + socket.isClosed());
                            break;

                        case "DIRECTORY_DELETE_RESPONSE":
                            String ddel = request.getString("pathName");
                            if (DIRECTORY_DELETE.contains(ddel)) {
                                DIRECTORY_DELETE.remove(ddel);
                            } else {
                                new InvalidProtocol(socket).InvalidMessage("Corresponding request is not found!");
                            }
                            break;

                        case "DIRECTORY_DELETE_REQUEST":
                            new DirectoryDelete(socket).DirectoryDelete_response(request, fileSystemManager);
                            System.out.println("Send DIRECTORY_DELETE_RESPONSE to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            // System.out.println("Connection closed: " + socket.isClosed());
                            break;

                        case "FILE_CREATE_RESPONSE":
//                            for (Document d:FILE_CREATE){
//
//                                System.out.println(d.toJson());
//                            }
                            if (FILE_CREATE.contains(md5)) {
                                FILE_CREATE.remove(md5);
                                FILE_BYTE.add(md5);
                            } else {
                                new InvalidProtocol(socket).InvalidMessage("Corresponding request is not found!");
                            }
                            break;

                        case "FILE_CREATE_REQUEST":
                            try {
                                if (new FileCreate(socket).FileCreate_response(request, fileSystemManager)) {
                                    FILE_BYTETO.add(md5);
                                }
                                System.out.println("Send FILE_CREATE_RESPONSE to : " + socket.getInetAddress().toString()
                                        + ":" + socket.getPort());
                                // System.out.println("Connection closed: " + socket.isClosed());
                            } catch (IOException e) {

                            }
                            break;

                        case "FILE_MODIFY_RESPONSE":
                            if (FILE_MODIFY.contains(md5)) {
                                FILE_MODIFY.remove(md5);
                                FILE_BYTE.add(md5);
                            } else {
                                new InvalidProtocol(socket).InvalidMessage("Corresponding request is not found!");
                            }
                            break;

                        case "FILE_MODIFY_REQUEST":
                            if (new FileModify(socket).FileModify_response(request, fileSystemManager)) {
                                FILE_BYTETO.add(md5);
                            }
                            System.out.println("Send FILE_MODIFY_RESPONSE to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            // System.out.println("Connection closed: " + socket.isClosed());
                            break;

                        case "FILE_BYTES_REQUEST":
                            if (FILE_BYTE.contains(md5)) {

                                if (new FileBytes(socket).FileBytes_response(request, fileSystemManager)) {
                                    FILE_BYTE.remove(md5);
                                }
                            }
                            break;

                        case "FILE_BYTES_RESPONSE":
                            if (FILE_BYTETO.contains(md5)) {
                                String statusto = new FileBytes(socket).FileBytes_request_continous(request, fileSystemManager);
                                if (statusto.equals("complete") || statusto.equals("failure")) {
                                    FILE_BYTETO.remove(md5);
                                }
                            }

                            break;

                        case "HANDSHAKE_REQUEST":
                            new InvalidProtocol(socket).InvalidMessage("Connection already built.");
                            System.out.println("Send INVALID_PROTOCOL to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            // System.out.println("Connection closed: " + socket.isClosed());
                            break;

                        case "HANDSHAKE_RESPONSE":
                            new InvalidProtocol(socket).InvalidMessage("Connection already built.");
                            System.out.println("Send INVALID_PROTOCOL to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            // System.out.println("Connection closed: " + socket.isClosed());
                            break;

                        default:
                            new InvalidProtocol(socket).InvalidMessage("the command is invalid");
                            System.out.println("Send INVALID_PROTOCOL to : " + socket.getInetAddress().toString()
                                    + ":" + socket.getPort());
                            // System.out.println("Connection closed: " + socket.isClosed());
                            break;
                    }
                }
            }
            if (Peer.getConnectedPeers().contains(socket)) {
                Peer.delConnectedPeers(socket);

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Invalid Input/Output, connection stop!");
        } catch (NullPointerException e) {
            try {
                new InvalidProtocol(socket).InvalidMessage("The command is invalid!");
            } catch (IOException e1) {
                System.out.println("Invalid Input/Output, connection stop!");
            }
        } catch (Exception e) {
        }

    }
}
