package com.l7tech.server.cassandra;

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
public interface CassandraConnectionHolder {

    public CassandraConnection getCassandraConnectionEntity();

    public Cluster getCluster();

    public Session getSession();
}
