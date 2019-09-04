package unimelb.bitbox;

import unimelb.bitbox.util.*;
import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * A thread keeping monitoring incoming connection request.
 * For each request create a ThreadServer to handle the message read.
 */

public class ThreadListen extends Thread {

    /**
     * Declare server port number.
     */
    private static final int port = Integer.parseInt(Configuration.getConfigurationValue("port"));

    private FileSystemManager fileSystemManager;

    /**
     * Constructor for ThreadListen.
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public ThreadListen(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    @Override
    public void run() {
        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try (ServerSocket serverSocket = factory.createServerSocket(port)) {
            ArrayList<ThreadServer> server = new ArrayList<>();
            int i = 0;
            // Wait for the connection
            while (true) {
                // Receive one connection
                new ThreadServer(serverSocket.accept(), fileSystemManager).start();
                // Start a new thread for the connection
            }
        } catch (IOException ex) {
            ex.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
