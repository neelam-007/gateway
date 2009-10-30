package com.l7tech.server.cluster;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 */
class SingleNodeMessageIdCache implements MessageIdCache {
    private static final Logger logger = Logger.getLogger(SingleNodeMessageIdCache.class.getName());

    private final Map<String, Long> map = new ConcurrentHashMap<String, Long>();

    private final String nodeid;

    SingleNodeMessageIdCache(String nodeid) {
        logger.info("Using single-node replay protection");
        this.nodeid = nodeid;
    }

    @Override
    public Long get(String id) {
        return map.get(id);
    }

    @Override
    public Set<String> getAll() {
        return new LinkedHashSet<String>(map.keySet());
    }

    @Override
    public void put(String id, Long expiry) {
        map.put(id, expiry);
    }

    @Override
    public void remove(String id) {
        map.remove(id);
    }

    @Override
    public void getMemberAddresses(List<String> addresses) {
        // No-op
    }

    @Override
    public boolean isPopulateNeeded() {
        return true;
    }

    @Override
    public boolean isTimeoutError(Throwable t) {
        return false;
    }

    @Override
    public void restart() {
        map.clear();
    }

    @Override
    public void startBatch() {
    }

    @Override
    public void endBatch(boolean batchSuccess) {
    }

    @Override
    public void destroy() {
        map.clear();
    }

    @Override
    public String toDatabaseId(String key) {
        return nodeid + key;
    }

    @Override
    public String fromDatabaseId(String id) {
        if (id != null && id.startsWith(nodeid) && id.length() > nodeid.length())
            return id.substring(nodeid.length());
        return null;
    }
}
