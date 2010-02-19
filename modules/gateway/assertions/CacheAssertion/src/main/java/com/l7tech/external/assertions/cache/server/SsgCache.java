package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.StashManager;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an instance of a cache.
 * TODO multithreaded cache retrieval
 */
public class SsgCache {
    protected static final Logger logger = Logger.getLogger(SsgCache.class.getName());

    private final StashManagerFactory stashManagerFactory;
    private volatile Config config;
    private final Map<String, Entry> cache = Collections.synchronizedMap(new LinkedHashMap<String, Entry>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            return size() > config.maxEntries || eldest.getValue().timeStamp < System.currentTimeMillis() - config.maxAgeMillis;
        }
    });
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SsgCache(StashManagerFactory stashManagerFactory, Config config) {
        this.stashManagerFactory = stashManagerFactory;
        this.config = config;
    }

    public void updateConfig(Config newConfig) {
        this.config = newConfig;
        logger.log(Level.FINE, "Cache config update for " + toString());
    }

    static SsgCache getNullCache() {
        SsgCache c = new SsgCache(null, null);
        c.closed.set(true);
        return c;
    }

    /**
     * Look up an entry in the cache.
     * 
     * @param key the key to look for.
     * @return the cached entry associated with the key, or null if not found.
     *         If an InputStream is returned, caller is responsible for closing it when they are finished with it.
     */
    public Entry lookup(String key) {
        if (closed.get()) return null;
        Entry cachedEntry = cache.get(key);
        if (cachedEntry == null || cachedEntry.timeStamp < System.currentTimeMillis() - config.maxAgeMillis)
            return null;
        return cachedEntry;
    }

    /**
     * Store an entry into the cache.
     *
     * @param key the key under which to store the entry.
     * @param body the InputStream to store.  Will be drained, read all the way to EOF, but will not be closed.
     * @throws java.io.IOException if there is a problem reading the body or writing the cache information
     */
    public void store(String key, InputStream body) throws IOException {
        store(key, body, null);
    }

    /**
     * Store an entry into the cache.
     *
     * @param key the key under which to store the entry.
     * @param body the InputStream to store.  Will be drained, read all the way to EOF, but will not be closed.
     * @param contetType the content-type associated with the privided input stream
     * @throws java.io.IOException if there is a problem reading the body or writing the cache information
     */
    public void store(String key, InputStream body, String contetType) throws IOException {
        if (closed.get()) return;
        StashManager sm = stashManagerFactory.createStashManager();
        boolean needsClose = true;
        try {
            sm.stash(0, body);
            if (sm.getSize(0) > config.maxSizeBytes) {
                return;
            }
            Entry prev = cache.put(key, new Entry(sm, contetType));
            logger.log(Level.FINE, "Cache size: " + cache.size());
            needsClose = false;
            if (prev != null) synchronized (prev) { prev.stashManager.close(); }
        } finally {
            if (needsClose) sm.close();
        }
    }

    /**
     * Shut down this cache, freeing any resources being used by it.
     */
    public void close() {
        closed.set(true);
        while (!cache.isEmpty()) {
            Iterator<Map.Entry<String,Entry>> it = cache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Entry> entry = it.next();
                it.remove();
                Entry sm = entry.getValue();
                if (sm != null) try {
                    synchronized (sm) { sm.stashManager.close(); }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception while closing StashManager: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return config.toString() + " : " + cache.size() + " entries.";
    }

    public static class Entry {
        private final long timeStamp;
        private final String contentType;
        private final StashManager stashManager;

        public Entry(StashManager stashManager, String contentType) {
            this.timeStamp = System.currentTimeMillis();
            this.stashManager = stashManager;
            this.contentType = contentType;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public StashManager getStashManager() {
            return stashManager;
        }

        public String getContentType() {
            return contentType;
        }
    }

    public static class Config {

        public static final int DEFAULT_MAX_ENTRIES = 1000;
        public static final long DEFAULT_MAX_SIZE_BYTES = 1000000; // one meg
        public static final long DEFAULT_MAX_AGE_MILLIS = 86400000; // one day

        private final String name;
        private int maxEntries = DEFAULT_MAX_ENTRIES;
        private long maxAgeMillis = DEFAULT_MAX_AGE_MILLIS;
        private long maxSizeBytes = DEFAULT_MAX_SIZE_BYTES;

        public Config(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Config maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        public Config maxAgeMillis(long maxAgeMillis) {
            this.maxAgeMillis = maxAgeMillis;
            return this;
        }

        public Config maxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        public SsgCache build(StashManagerFactory stashManagerFactory) {
            return new SsgCache(stashManagerFactory, this);
        }

        @Override
        public String toString() {
            return name + "[" + maxEntries + ", " + maxAgeMillis + "ms, " + maxSizeBytes + "bytes]";
        }
    }
}
