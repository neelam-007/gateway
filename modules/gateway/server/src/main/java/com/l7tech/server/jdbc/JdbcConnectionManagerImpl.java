package com.l7tech.server.jdbc;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.gateway.common.jdbc.JdbcConnection;

/**
 * The implementation of managing JDBC Connection Entity
 *
 * @author ghuang
 */
@Transactional(propagation= Propagation.REQUIRED)
public class JdbcConnectionManagerImpl
    extends HibernateEntityManager<JdbcConnection, EntityHeader>
    implements JdbcConnectionManager {

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return JdbcConnection.class;
    }

    @Override
    public String getTableName() {
        return "jdbc_connection";
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return JdbcConnection.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    /**
     * Retrieve a JDBC Connection entity from the database by using a connection name.
     *
     * @param connectionName: the name of a JDBC connection
     * @return a JDBC Connection entity with the name, "connectionName".
     * @throws FindException: thrown when errors finding the JDBC Connection entity.
     */
    @Override
    public JdbcConnection getJdbcConnection(String connectionName) throws FindException {
        return findByUniqueName(connectionName);
    }
}
