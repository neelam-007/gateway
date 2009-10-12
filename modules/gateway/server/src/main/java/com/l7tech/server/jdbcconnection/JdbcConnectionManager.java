package com.l7tech.server.jdbcconnection;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;

/**
 * @author ghuang
 */
public interface JdbcConnectionManager extends EntityManager<JdbcConnection, EntityHeader> {
    JdbcConnection getJdbcConnection(String connectionName) throws FindException;
}
