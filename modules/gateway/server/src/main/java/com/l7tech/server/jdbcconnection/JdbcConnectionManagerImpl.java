package com.l7tech.server.jdbcconnection;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;

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

    @Override
    public JdbcConnection getJdbcConnection(String connectionName) throws FindException {
        return findByUniqueName(connectionName);
    }
}
