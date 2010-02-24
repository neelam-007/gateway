package com.l7tech.server;

import com.l7tech.common.mime.HybridStashManager;
import com.l7tech.common.mime.StashManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * @author jbufu
 */
public class ResponseCacheStashManagerFactory implements StashManagerFactory, PropertyChangeListener {

    // - PUBLIC

    public static StashManagerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public StashManager createStashManager() {
        return new HybridStashManager(diskThreshold, ConfigHolder.RESPONSE_CACHE_DIR, CACHED_ENTRY_PREFIX + getStashFileUnique());
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if ( event.getPropertyName().equals(ServerConfig.PARAM_RESPONSECACHE_DISK_THRESHOLD) && event.getNewValue() != null ) {
            reloadConfig();
        }
    }

    // - PRIVATE

    private static class ConfigHolder {
        private static final File RESPONSE_CACHE_DIR = ServerConfig.getInstance().getLocalDirectoryProperty(ServerConfig.PARAM_RESPONSECACHE_DIRECTORY, true);
    }

    private static final ResponseCacheStashManagerFactory INSTANCE = new ResponseCacheStashManagerFactory();
    private static final Logger logger = Logger.getLogger(ResponseCacheStashManagerFactory.class.getName());
    private static final String CACHED_ENTRY_PREFIX = "respCache";
    private static AtomicLong stashFileUnique = new AtomicLong(0);
    private static volatile int diskThreshold;
    static {
        reloadConfig();
    }

    private static void reloadConfig() {
        diskThreshold = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_RESPONSECACHE_DISK_THRESHOLD, 8096);
        logger.config("Response cache disk threshold set to " + diskThreshold + " bytes.");
    }

    private static long getStashFileUnique() {
        return stashFileUnique.getAndIncrement();
    }

    private ResponseCacheStashManagerFactory() {}
}
