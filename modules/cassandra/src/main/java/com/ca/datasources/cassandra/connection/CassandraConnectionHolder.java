package com.ca.datasources.cassandra.connection;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.l7tech.gateway.common.cassandra.CassandraConnection;


/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class CassandraConnectionHolder {
    final private CassandraConnection connectionConfig;
    final private Cluster cluster;
    final private Session session;


    public CassandraConnectionHolder(CassandraConnection connectionConfig, Cluster cluster, Session session){
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
