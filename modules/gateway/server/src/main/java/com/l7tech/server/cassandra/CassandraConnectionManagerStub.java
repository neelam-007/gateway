package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.Goid;
import org.springframework.context.ApplicationEvent;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 11/27/14
 */
public class CassandraConnectionManagerStub implements CassandraConnectionManager{

    private CassandraConnectionHolder connectionHolder = null;

    @Override
    public CassandraConnectionHolder getConnection(String name) {
        return connectionHolder;
    }

    @Override
    public void testConnection(CassandraConnection cassandraConnectionEntity) throws Exception {

    }

    @Override
    public void closeAllConnections() {

    }

    @Override
    public void addConnection(CassandraConnection cassandraConnectionEntity) {

    }

    @Override
    public void removeConnection(CassandraConnection cassandraConnectionEntity) {

    }

    @Override
    public void removeConnection(Goid goid) {

    }

    @Override
    public void updateConnection(CassandraConnection cassandraConnectionEntity) {

    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {

    }

    public void setMockConnection(CassandraConnectionHolder mockConnectionHolder) {
        this.connectionHolder = mockConnectionHolder;
    }
}
