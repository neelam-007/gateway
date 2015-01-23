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
    public static final String HOST_DISTANCE = "hostDistance";
    public static final String CORE_CONNECTION_PER_HOST = "coreConnectionsPerHost";
    public static final String MAX_CONNECTION_PER_HOST = "maxConnectionPerHost";
    public static final String MAX_SIMUL_REQ_PER_HOST = "maxSimultaneousRequestsPerHostThreshold";
    public static final String MAX_CONNECTION_CACHE_AGE = "cassandra.maxConnectionCacheAge";
    public static final String MAX_CONNECTION_CACHE_IDLE_TIME = "cassandra.maxConnectionCacheIdleTime";
    public static final String MAX_CONNECTION_CACHE_SIZE = "cassandra.maxConnectionCacheSize";

    public static final int CORE_CONNECTION_PER_HOST_DEF = 1;
    public static final int CORE_CONNECTION_PER_HOST_LOCAL_DEF = 2;
    public static final int MAX_CONNECTION_PER_HOST_DEF = 2;
    public static final int MAX_CONNECTION_PER_HOST_LOCAL_DEF = 8;
    public static final int MAX_SIMUL_REQ_PER_HOST_LOCAL_DEF = 8192;
    public static final int MAX_SIMUL_REQ_PER_HOST_REMOTE_DEF = 256;

    public static final String CONNECTION_TIMEOUT_MILLIS = "connectTimeoutMillis";
    public static final String READ_TIMEOUT_MILLIS = "readTimeoutMillis";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    public static final String REUSE_ADDRESS = "reuseAddress";
    public static final String SEND_BUFFER_SIZE = "sendBufferSize";
    public static final String SO_LINGER = "soLinger";
    public static final String TCP_NO_DELAY = "tcpNoDelay";

    public static final long DEFAULT_CONNECTION_MAX_AGE = 0L;
    public static final long DEFAULT_CONNECTION_MAX_IDLE = TimeUnit.MINUTES.toMillis(30);
    public  static final int DEFAULT_CONNECTION_CACHE_SIZE = 20;
    public static final int DEFAULT_FETCH_SIZE = 5000;
    public static final String QUERY_FETCH_SIZE = "fetchSize";

    CassandraConnectionHolder getConnection(String name);

    void testConnection(CassandraConnection cassandraConnectionEntity) throws Exception;

    void closeAllConnections();

    void addConnection(CassandraConnection cassandraConnectionEntity);

    void removeConnection(CassandraConnection cassandraConnectionEntity);

    void removeConnection(Goid goid);

    void updateConnection(CassandraConnection cassandraConnectionEntity) throws UpdateException;
}
