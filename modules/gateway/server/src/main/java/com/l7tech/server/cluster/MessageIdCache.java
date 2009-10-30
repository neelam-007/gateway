package com.l7tech.server.cluster;

import java.util.Set;
import java.util.List;

/**
 *
 */
public interface MessageIdCache {
    Long get(String key);

    Set<String> getAll();

    void put(String id, Long expiry);

    void remove(String id);

    void getMemberAddresses(List<String> addresses);

    boolean isPopulateNeeded();

    boolean isTimeoutError(Throwable t);

    void restart();

    void startBatch();

    void endBatch(boolean batchSuccess);

    void destroy();

    String toDatabaseId(String key);

    String fromDatabaseId(String id);
}
