package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.TimeUnit;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public interface CassandraConnectionManager {
    String HOST_DISTANCE = "hostDistance";
    String MAX_SIMUL_REQ_PER_HOST = "maxSimultaneousRequestsPerHostThreshold";

    int MAX_SIMUL_REQ_PER_HOST_LOCAL_DEF = 8192;
    int MAX_SIMUL_REQ_PER_HOST_REMOTE_DEF = 256;

    String CONNECTION_TIMEOUT_MILLIS = "connectTimeoutMillis";
    String READ_TIMEOUT_MILLIS = "readTimeoutMillis";
    String KEEP_ALIVE = "keepAlive";
    String RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    String REUSE_ADDRESS = "reuseAddress";
    String SEND_BUFFER_SIZE = "sendBufferSize";
    String SO_LINGER = "soLinger";
    String TCP_NO_DELAY = "tcpNoDelay";
    String QUERY_FETCH_SIZE = "fetchSize";

    long DEFAULT_CONNECTION_MAX_AGE = 0L;
    long DEFAULT_CONNECTION_MAX_IDLE = TimeUnit.MINUTES.toMillis(30);
    int DEFAULT_CONNECTION_CACHE_SIZE = 20;
    int DEFAULT_FETCH_SIZE = 5000;
    int DEFAULT_CONNECTION_TIMEOUT_MS = 5000;
    int DEFAULT_READ_TIMEOUT_MS = 12000;
    boolean DEFAULT_KEEP_ALIVE = true;
    boolean DEFAULT_TCP_NO_DELAY = false;


    CassandraConnectionHolder getConnection(String name);

    void testConnection(CassandraConnection cassandraConnectionEntity) throws Exception;

    void closeAllConnections();

    void addConnection(CassandraConnection cassandraConnectionEntity);

    void removeConnection(CassandraConnection cassandraConnectionEntity);

    void removeConnection(Goid goid);

    void updateConnection(CassandraConnection cassandraConnectionEntity) throws UpdateException;
}
