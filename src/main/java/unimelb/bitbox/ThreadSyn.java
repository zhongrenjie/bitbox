package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/**
 * A thread generate synchronization events according to the interval in the configuration file.
 * And add them to synchronization event list.
 */
public class ThreadSyn extends Thread {

    private FileSystemManager fileSystemManager;

    /**
     * Constructor for ThreadSyn.
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public ThreadSyn(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public void run() {
        while (true) {
            try {
                Thread.currentThread().sleep(1000*Long.parseLong(Configuration.getConfigurationValue("syncInterval")));
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            for (FileSystemEvent eve : fileSystemManager.generateSyncEvents()) {
                Peer.addsyn_Event(eve);
            }

        }
    }
}
