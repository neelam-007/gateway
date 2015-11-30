package com.ca.siteminder;

import com.sun.istack.NotNull;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: whodgins
 * Date: 18/12/14
 * Time: 11:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class SiteMinderContextCache {
    private static final Logger logger = Logger.getLogger(SiteMinderContextCache.class.getName());

    /** A map that automatically purges the eldest entries when a size limit is reached. */
    private class CappedLinkedHashMap<K,V> extends LinkedHashMap<K,V> {
        public CappedLinkedHashMap(final int maxEntries) {
            super(maxEntries);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maxEntries;
        }
    }

    private final Lock lock = new ReentrantLock(true);
    private final String name;
    private int maxEntries;
    private long maxAgeMillis;

    /**
     The map that contains the key and the value that make up a LRU SiteMinderContextCache.
     */
    private Map<String, Entry> cache = null;

    /**
     The map that contains the primary and secondary key's SiteMinderContextCache.
     */
    private ConcurrentMap<String, String> cacheKeys;

    public static class Entry {
        private final long timeStamp;
        private String secondaryKey;
        private SiteMinderContext smContext;

        public Entry(SiteMinderContext smContext, String key) {
            this.timeStamp = System.currentTimeMillis();
            this.secondaryKey = key;
            this.smContext = smContext;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public String getSecondaryKey(){
            return secondaryKey;
        }

        public SiteMinderContext getSmContext() {
            return smContext;
        }
    }

    public SiteMinderContextCache(String name, int maxEntries, long maxAgeMillis) {
        this.name = name;
        this.maxEntries = maxEntries;
        this.maxAgeMillis = maxAgeMillis;
        cache = new CappedLinkedHashMap<>(this.maxEntries);
        cacheKeys = new ConcurrentHashMap<>();
    }

    /**
     * Look up an entry in the cache.
     *
     * @param key the key to look for.
     * @return the cached entry associated with the key, or null if not found or expired.
     *
     */
    public Entry lookup( @NotNull String key ) {
        lock.lock();
        try {
            Entry cachedEntry = cache.get( key );

            if (cachedEntry == null) {
                return null;
            }

            if (cachedEntry.getTimeStamp() < System.currentTimeMillis() - maxAgeMillis) {
                //remove expired entries on lookup
                cache.remove(key);
                return null;
            }

            return cachedEntry;
        } finally {
            lock.unlock();
        }
    }

    public Entry getAnyEntry() {
        lock.lock();
        try {
            if (cache.isEmpty()) {
                return null;
            }
            return cache.values().iterator().next();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Look up an entry by either the Cache or CacheKey map and then remove this entry from both Maps.
     *
     * @param key associated with the Cache
     * @param lookupKey associated with the CacheKey, used to lookup the primary the key.
     */
    public void remove ( String key, String lookupKey ){
        lock.lock();
        try {
            Entry cachedEntry;
            if ( StringUtils.isNotEmpty( key ) && StringUtils.isNotBlank( key )){ //Cache key is used to lookup the secondary key
                if ( ( cachedEntry = cache.get( key ) ) != null){ // ensure the lookup key entry is not null, handles NPE
                    cacheKeys.remove( cachedEntry.getSecondaryKey() );
                    cache.remove( key );
                }
            }

            if ( StringUtils.isNotEmpty( lookupKey ) && StringUtils.isNotBlank( lookupKey )) { //CacheKey key is used to lookup the primary key
                key = cacheKeys.get( lookupKey );
                cache.remove( key );
                cacheKeys.remove( lookupKey );
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Look up an entry from the Cache
     *
     * @param key associated with the Cache
     */
    public void remove ( String key ){
        lock.lock();
        try {
            if ( StringUtils.isNotEmpty( key ) && StringUtils.isNotBlank( key )){
                cache.remove( key );
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Store an entry into the cache.
     *
     * @param primaryKey the key under which to store the entry.
     * @param secondaryKey the key needed to remove entries from the cache, in the event an
     * AgentAPI.doManagment() response needs to expiry an entry from the cache
     * or an SsoToken session is invalid
     * @param smContext the SiteMinder Context object to be cached
     *
     */
    public void store( String primaryKey, String secondaryKey, SiteMinderContext smContext ) {
        lock.lock();
        try {
            cache.put(primaryKey, new Entry(smContext, secondaryKey));
            cacheKeys.put(secondaryKey, primaryKey);
            logger.log(Level.FINE, "Cache size: " + cache.size());
            logger.log(Level.FINE, "Cache name: " + name + "; Cache size: " + cache.size());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Store an entry into the cache.
     *
     * @param primaryKey the key under which to store the entry.
     * or an SsoToken session is invalid
     * @param smContext the SiteMinder Context object to be cached
     *
     */
    public void store( String primaryKey, SiteMinderContext smContext ) {
        lock.lock();
        try {
            cache.put(primaryKey, new Entry(smContext, ""));
            logger.log(Level.FINE, "Cache size: " + cache.size());
            logger.log(Level.FINE, "Cache name: " + name + "; Cache size: " + cache.size());
        } finally {
            lock.unlock();
        }
    }

    public String getPrimaryKey (final String secondaryKey) {
        lock.lock();
        try {
            return cacheKeys.get(secondaryKey);
        } finally {
            lock.unlock();
        }
    }

    //Returns the size of the cache
    public int size() {
        return cache.size();
    }

    /**
     * Shut down this cache, freeing any resources being used by it.
     */
    public void close() {
        lock.lock();
        try {
            clear();
        } finally {
            lock.unlock();
        }
    }

    // clears the cache
    public void clear() {
        lock.lock();
        try {
            cache.clear();
            cacheKeys.clear();
        } finally {
            lock.unlock();
        }
    }
}