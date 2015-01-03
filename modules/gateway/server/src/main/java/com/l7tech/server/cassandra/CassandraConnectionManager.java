package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.Goid;
import org.springframework.context.ApplicationListener;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public interface CassandraConnectionManager extends ApplicationListener {
    CassandraConnectionHolder getConnection(String name);

    void testConnection(CassandraConnection cassandraConnectionEntity) throws Exception;

    void closeAllConnections();

    void addConnection(CassandraConnection cassandraConnectionEntity);

    void removeConnection(CassandraConnection cassandraConnectionEntity);

    void removeConnection(Goid goid);

    void updateConnection(CassandraConnection cassandraConnectionEntity);
}
