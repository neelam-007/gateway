package com.l7tech.server.http;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.l7tech.server.ServerConfig;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static com.l7tech.server.ServerConfigParams.PARAM_IO_HTTP_ROUTING_AUTHORIZATION_STATE_POOL_SIZE;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_HTTP_ROUTING_AUTHORIZATION_STATE_POOL_TIMEOUT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Control the state pools for routing http requests.
 */
public class HttpStatePoolManager implements PropertyChangeListener {

    private static final long DEFAULT_HTTP_AUTHORIZATION_POOL_SIZE = 10000L;
    private static final long DEFAULT_HTTP_AUTHORIZATION_POOL_TIMEOUT = MINUTES.toMillis(1L); // 1 minute

    private ServerConfig serverConfig;

    // holds state by authorization hash
    private Cache<String, Object> stateCacheHash;

    public HttpStatePoolManager(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.stateCacheHash = buildCache();
    }

    private Cache<String, Object> buildCache() {
        // self expiring cache for storing state by hashed authorization and destination
        return CacheBuilder.newBuilder()
                .maximumSize(serverConfig.getLongProperty(PARAM_IO_HTTP_ROUTING_AUTHORIZATION_STATE_POOL_SIZE, DEFAULT_HTTP_AUTHORIZATION_POOL_SIZE))
                .expireAfterWrite(serverConfig.getTimeUnitProperty(PARAM_IO_HTTP_ROUTING_AUTHORIZATION_STATE_POOL_TIMEOUT, DEFAULT_HTTP_AUTHORIZATION_POOL_TIMEOUT), MILLISECONDS)
                .build();
    }

    public Object getFromStateCache(String hashKey) {
        return this.stateCacheHash.getIfPresent(hashKey);
    }

    public void putToStateCacheIfAbsent(String hashKey, Object stateObject) {
        if (stateCacheHash.getIfPresent(hashKey) == null) {
            stateCacheHash.put(hashKey, stateObject);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // this is only triggered for the used properties, so which one it's changed, does not matter.
        Cache<String, Object> oldCache = stateCacheHash;
        stateCacheHash = buildCache();
        oldCache.invalidateAll();
    }

    public long getCacheSize() {
        return stateCacheHash.size();
    }

}
