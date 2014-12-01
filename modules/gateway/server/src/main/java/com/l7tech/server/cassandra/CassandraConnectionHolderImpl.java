package com.l7tech.server.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.l7tech.gateway.common.cassandra.CassandraConnection;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 12/1/14
 */
public class CassandraConnectionHolderImpl implements CassandraConnectionHolder {
    final private CassandraConnection connectionConfig;
    final private Cluster cluster;
    final private Session session;


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
}
