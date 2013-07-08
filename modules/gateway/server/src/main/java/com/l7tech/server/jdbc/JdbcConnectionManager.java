package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;

import java.util.List;

/**
 * An interface of managing JDBC Connection Entity
 *
 * @author ghuang
 */
public interface JdbcConnectionManager extends GoidEntityManager<JdbcConnection, EntityHeader> {
    /**
     * Retrieve a JDBC Connection entity from the database by using a connection name.
     *
     * @param connectionName: the name of a JDBC connection
     * @return a JDBC Connection entity with the name, "connectionName".
     * @throws FindException: thrown when errors finding the JDBC Connection entity.
     */
    JdbcConnection getJdbcConnection(String connectionName) throws FindException;

    /**
     * To retrieve a list of driver classes which the JDBC Query Assertion is allowed to use.
     *
     * @return a list of support driver classes.
     */
    List<String> getSupportedDriverClass();

    /**
     * See if the jdbc drive Class is supported, the jdbcConnection.driverClass.whiteList under serverConfig.properties
     * defined all the supported jdbc driver class.
     *
     * @param driverClass The driver class
     * @return True if the jdbc driver class is supported, False if the jdbc driver class is not supported.
     */
    boolean isDriverClassSupported(String driverClass);
}
