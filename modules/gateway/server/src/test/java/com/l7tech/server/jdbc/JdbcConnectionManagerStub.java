package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;

/**
 *
 */
public class JdbcConnectionManagerStub extends EntityManagerStub<JdbcConnection, EntityHeader> implements JdbcConnectionManager {

    public JdbcConnectionManagerStub( final JdbcConnection... entitiesIn ) {
        super( entitiesIn );
    }

    @Override
    public JdbcConnection getJdbcConnection( final String connectionName ) throws FindException {
        throw new FindException("Not implemented");
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return JdbcConnection.class;
    }
}
