package unimelb.bitbox;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/**
 * A thread monitor the local file operations in the shared directory.
 */
public class ServerMain implements FileSystemObserver {
    private static Logger log = Logger.getLogger(ServerMain.class.getName());
    protected FileSystemManager fileSystemManager;

    public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
    }

    /**
     * Add a fileSystemEvent detected into the event list waiting for process.
     * @param fileSystemEvent The path and name of the file and the kind of operation.
     */
    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        Peer.addEvent(fileSystemEvent);

    }
}
