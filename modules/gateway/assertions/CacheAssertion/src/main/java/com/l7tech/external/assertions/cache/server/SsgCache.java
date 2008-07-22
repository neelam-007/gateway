package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an instance of a cache.
 * TODO cache replacement
 * TODO multithreaded cache retrieval
 */
public class SsgCache {
    protected static final Logger logger = Logger.getLogger(SsgCache.class.getName());

    private final StashManagerFactory stashManagerFactory;
    private final ConcurrentHashMap<String, StashManager> cache = new ConcurrentHashMap<String, StashManager>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SsgCache(StashManagerFactory stashManagerFactory) {
        this.stashManagerFactory = stashManagerFactory;
    }

    static SsgCache getNullCache() {
        SsgCache c = new SsgCache(null);
        c.closed.set(true);
        return c;
    }

    /**
     * Look up an entry in the cache.
     * 
     * @param key the key to look for.
     * @return a ready-to-read InputStream for this key, or null if not found.
     *         If an InputStream is returned, caller is responsible for closing it when they are finished with it.
     */
    public InputStream lookup(String key) {
        if (closed.get()) return null;
        StashManager got = cache.get(key);
        if (got == null) return null;
        try {
            synchronized (got) {
                return got.recall(0);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception while retrieving cached information: " + ExceptionUtils.getMessage(e), e);
            return null;
        } catch (NoSuchPartException e) {
            logger.log(Level.WARNING, "Exception while retrieving cached information: " + ExceptionUtils.getMessage(e), e); // can't happen
            return null;
        }
    }

    /**
     * Store an entry into the cache.
     *
     * @param key the key under which to store the entry.
     * @param body the InputStream to store.  Will be drained, read all the way to EOF, but will not be closed.
     * @throws java.io.IOException if there is a problem reading the body or writing the cache information
     */
    public void store(String key, InputStream body) throws IOException {
        if (closed.get()) return;
        StashManager sm = stashManagerFactory.createStashManager();
        boolean needsClose = true;
        try {
            sm.stash(0, body);
            StashManager prev = cache.put(key, sm);
            needsClose = false;
            if (prev != null) synchronized (prev) { prev.close(); }
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
            Iterator<Map.Entry<String,StashManager>> it = cache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, StashManager> entry = it.next();
                it.remove();
                StashManager sm = entry.getValue();
                if (sm != null) try {
                    synchronized (sm) { sm.close(); }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception while closing StashManager: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
    }
}
