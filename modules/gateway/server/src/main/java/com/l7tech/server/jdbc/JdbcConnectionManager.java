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
    /**
     * Retrieve a JDBC Connection entity from the database by using a connection name.
     *
     * @param connectionName: the name of a JDBC connection
     * @return a JDBC Connection entity with the name, "connectionName".
     * @throws FindException: thrown when errors finding the JDBC Connection entity.
     */
    JdbcConnection getJdbcConnection(String connectionName) throws FindException;
}
