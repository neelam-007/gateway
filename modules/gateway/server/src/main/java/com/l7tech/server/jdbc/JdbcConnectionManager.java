package com.l7tech.server.jdbc;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.jdbc.JdbcConnection;

/**
 * An interface of managing JDBC Connection Entity
 *
 * @author ghuang
 */
public interface JdbcConnectionManager extends EntityManager<JdbcConnection, EntityHeader> {
    JdbcConnection getJdbcConnection(String connectionName) throws FindException;
}
