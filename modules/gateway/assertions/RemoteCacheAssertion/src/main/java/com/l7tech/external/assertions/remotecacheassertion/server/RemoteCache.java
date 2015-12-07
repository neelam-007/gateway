package com.l7tech.external.assertions.remotecacheassertion.server;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14/11/11
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RemoteCache {
    public CachedMessageData get(String key) throws Exception;

    public void set(String key, CachedMessageData value, int expiry) throws Exception;

    /**
     * Removes the entry associated to the specified key from the remote cache.
     *
     * @param key the cache entry key
     * @throws Exception
     */
    public void remove(String key) throws Exception;

    public void shutdown();
}
