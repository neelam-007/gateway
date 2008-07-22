package com.l7tech.server;

import java.util.concurrent.atomic.AtomicLong;
import java.io.File;
import java.io.IOException;

import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.HybridStashManager;

/**
 * Makes stash managers for server code that needs one.
 */
public final class TestStashManagerFactory implements StashManagerFactory {
    private TestStashManagerFactory(){};
    private static AtomicLong stashFileUnique = new AtomicLong(0);

    private static long getStashFileUnique() {
        return stashFileUnique.getAndIncrement();
    }

    private static class ConfigHolder {
        private static final int DISK_THRESHOLD = ServerConfig.getInstance().getAttachmentDiskThreshold();
        private static final File ATTACHMENT_DIR;
        private static final String PREFIX;

        static {
            try {
                File temp = File.createTempFile("temp", ".txt");
                temp.delete();
                ATTACHMENT_DIR = temp.getParentFile();
                System.out.println("Using temp attachment directory '"+ATTACHMENT_DIR.getAbsolutePath()+"'.");
                PREFIX = "att" + temp.getName().replace(".txt", "T");
            }
            catch(IOException ioe) {
                throw new RuntimeException("Could not get temporary directory!", ioe);    
            }
        }

        private static final StashManagerFactory INSTANCE = new TestStashManagerFactory();
    }

    /**
     * Get the TestStashManagerFactory.
     *
     * @return The test StashManagerFactory.
     */
    public static StashManagerFactory getInstance() {
        return ConfigHolder.INSTANCE;
    }

    /**
     * Create a new StashManager to use for some request.  A HybridStashManager will be created
     *
     * @return a new StashManager instance.  Never null.
     */
    public StashManager createStashManager() {
        StashManager stashManager = new HybridStashManager(ConfigHolder.DISK_THRESHOLD,
                                                           ConfigHolder.ATTACHMENT_DIR,
                                                           ConfigHolder.PREFIX + getStashFileUnique());
        return stashManager;
    }

}
