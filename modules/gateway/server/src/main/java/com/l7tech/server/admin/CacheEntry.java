package com.l7tech.server.admin;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 28, 2008
 * Time: 9:36:05 AM
 * To change this template use File | Settings | File Templates.
 *
 * CacheEntry wraps any object which is destined to be kept in a Cache.
 * It provides the getTimestamp method which a cache implementation uses to determine if this entry
 * has expired or not
 */
public final class CacheEntry <CE extends Object> {

    private CE cachedEntry;
    private final long timestamp = System.currentTimeMillis();
    
    public CacheEntry(CE cachedEntry){
        this.cachedEntry = cachedEntry;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public CE getCachedObject(){
        return this.cachedEntry;
    }
}
