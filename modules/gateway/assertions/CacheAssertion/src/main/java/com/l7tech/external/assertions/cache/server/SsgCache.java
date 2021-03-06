package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an instance of a cache.
 * TODO multithreaded cache retrieval
 */
public class SsgCache {

    // - PUBLIC

    public SsgCache(StashManagerFactory stashManagerFactory, Config config) {
        this.stashManagerFactory = stashManagerFactory;
        this.config = config;
    }

    public void updateConfig(Config newConfig) {
        if(! this.config.equals(newConfig)) {
            this.config = newConfig;
            logger.log(Level.FINE, "Cache config update for " + toString());
            removeExpired();
        }
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
        Entry cachedEntry;
        synchronized (this) {
            cachedEntry = cache.get(key);
        }
        return (cachedEntry == null || cachedEntry.timeStamp < System.currentTimeMillis() - config.maxAgeMillis) ? null : cachedEntry;
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
            synchronized (this) {
                if (closed.get()) return;
                Entry prev = cache.put(key, new Entry(sm, contetType));
                removeExpired();
                logger.log(Level.FINE, "Cache size: " + cache.size());
                needsClose = false;
                if (prev != null) removed.add(prev);
            }
        } finally {
            if (needsClose) sm.close();
        }
    }

    /**
     * Shut down this cache, freeing any resources being used by it.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Collection<Entry> entries;
            synchronized (this) { // don't start closing if a store may be in progress
                entries = cache.values();
                cache.clear();
                for(Entry entry : entries) {
                    closeEntry(entry);
                }
            }
        }
    }

    @Override
    public synchronized String toString() {
        return config.toString() + " : " + cache.size() + " entries.";
    }

    public static class Entry {
        private final long timeStamp;
        private final String contentType;
        private final StashManager stashManager;
        private final ReadWriteLock stashManagerLock = new ReentrantReadWriteLock(); // ReentrantReadWriteLock supports lock downgrade

        public Entry(StashManager stashManager, String contentType) {
            this.timeStamp = System.currentTimeMillis();
            this.stashManager = stashManager;
            this.contentType = contentType;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public String getContentType() {
            return contentType;
        }

        /**
         * Get the size of the entry.
         *
         * @return The size, suitable for use with putData.
         * @see #putData(byte[])
         */
        public int getDataSize() {
            final long size;

            stashManagerLock.readLock().lock();
            try {
                size = stashManager.getSize( 0 );
            } finally {
                stashManagerLock.readLock().unlock();
            }

            if ( size > Integer.MAX_VALUE || size < 0) {
                return 0;
            }

            return (int) size;
        }

        /**
         * Put the data for this entry into the given byte array.
         *
         * @param buffer The buffer which must be large enough for the data.
         * @throws NoSuchPartException If this entry has no part 0.
         * @throws IOException If an error occurs
         */
        public void putData( final byte[] buffer ) throws NoSuchPartException, IOException {
            InputStream in = null;
            Lock writeLock = stashManagerLock.writeLock();
            writeLock.lock();
            try {
                if ( stashManager.isByteArrayAvailable( 0 ) ) {
                    final byte[] data = stashManager.recallBytes( 0 );
                    stashManagerLock.readLock().lock();
                    try {
                        writeLock.unlock(); // downgrade to read lock for copy
                        writeLock = null;
                        System.arraycopy( data, 0, buffer, 0, data.length );
                    } finally {
                        stashManagerLock.readLock().unlock();
                    }
                } else {
                    in = stashManager.recall(0);
                    stashManagerLock.readLock().lock();
                    try {
                        writeLock.unlock(); // downgrade to read lock for copy
                        writeLock = null;
                        final int read = in.read( buffer );
                        if ( read != buffer.length ) {
                            throw new IOException( "Partial read " + read + "/" + buffer.length );
                        }
                    } finally {
                        stashManagerLock.readLock().unlock();
                    }
                }
            } finally {
                ResourceUtils.closeQuietly( in );
                if ( writeLock != null ) writeLock.unlock();
            }
        }

        private boolean tryClose() {
            boolean closed = false;

            if ( stashManagerLock.writeLock().tryLock() ) {
                try {
                    closed = true;
                    stashManager.close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception while closing StashManager: " + ExceptionUtils.getMessage(e), e);
                } finally {
                    stashManagerLock.writeLock().unlock();
                }
            }

            return closed;
        }
    }

    public static class Config {

        public static final int DEFAULT_MAX_ENTRIES = 10;
        public static final long DEFAULT_MAX_SIZE_BYTES = 10000; // 10k
        public static final long DEFAULT_MAX_AGE_MILLIS = 300000; // 5min

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

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Config config = (Config) o;

            if (maxAgeMillis != config.maxAgeMillis) return false;
            if (maxEntries != config.maxEntries) return false;
            if (maxSizeBytes != config.maxSizeBytes) return false;
            if (name != null ? !name.equals(config.name) : config.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + maxEntries;
            result = 31 * result + (int) (maxAgeMillis ^ (maxAgeMillis >>> 32));
            result = 31 * result + (int) (maxSizeBytes ^ (maxSizeBytes >>> 32));
            return result;
        }
    }

    // - PACKAGE

    static SsgCache getNullCache() {
        SsgCache c = new SsgCache(null, null);
        c.closed.set(true);
        return c;
    }

    // moves expired Entry from the cache to the "removed" queue
    synchronized void removeExpired() {
        Iterator<Map.Entry<String, Entry>> iterator = cache.entrySet().iterator();
        while(iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            if (cache.size() > config.maxEntries || entry.timeStamp < System.currentTimeMillis() - config.maxAgeMillis) {
                iterator.remove();
                removed.add(entry);
            } else {
                return;
            }
        }
    }

    // clears the cache
    synchronized void clear() {
        removed.addAll(cache.values());
        cache.clear();
    }

    void cleanup() {
        Entry entry;
        int count = 0;
        int deferredCount = 0;
        while((entry = removed.poll()) != null) {
            if ( closeEntry(entry) ) {
                count++;
            } else {
                // try again later
                removed.add(entry);
                deferredCount++;
            }
        }
        logger.log(Level.FINE, "Cache " + config.name + ": cleaned up " + count +" entries, deferred " + deferredCount +" entries.");
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(SsgCache.class.getName());
    private final StashManagerFactory stashManagerFactory;
    private volatile Config config;
    private final LinkedHashMap<String, Entry> cache = new LinkedHashMap<String, Entry>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    // entries removed from cache; background thread in SsgCacheManager will close() them
    private final ConcurrentLinkedQueue<Entry> removed = new ConcurrentLinkedQueue<Entry>();

    /**
     * @return true if closed.
     */
    private boolean closeEntry(Entry entry) {
        return entry == null || entry.tryClose();
    }
}
