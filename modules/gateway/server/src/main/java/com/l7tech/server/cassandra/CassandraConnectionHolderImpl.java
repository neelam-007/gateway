package com.l7tech.server.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.l7tech.gateway.common.cassandra.CassandraConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 12/1/14
 */
public class CassandraConnectionHolderImpl implements CassandraConnectionHolder {
    private final CassandraConnection connectionConfig;
    private final Cluster cluster;
    private final Session session;
    private final Map<String, PreparedStatement> statementMap = new ConcurrentHashMap<>();


    public CassandraConnectionHolderImpl(CassandraConnection connectionConfig, Cluster cluster, Session session){
        this.connectionConfig= connectionConfig;
        this.cluster = cluster;
        this.session = session;
    }

    public CassandraConnection getCassandraConnectionEntity() {
        return connectionConfig;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public Map<String, PreparedStatement> getPreparedStatementMap() {
        return statementMap;
    }
}
