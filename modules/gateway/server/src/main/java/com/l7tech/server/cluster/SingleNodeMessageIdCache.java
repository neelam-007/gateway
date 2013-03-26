package com.l7tech.server.cluster;

import java.util.logging.Logger;

/**
 *
 */
class SingleNodeMessageIdCache extends JgroupsMessageIdCache {
    private static final Logger logger = Logger.getLogger(SingleNodeMessageIdCache.class.getName());

    private final String nodeid;

    SingleNodeMessageIdCache(String nodeid) {
        super();
        logger.info("Using single-node replay protection");
        this.nodeid = nodeid;
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
