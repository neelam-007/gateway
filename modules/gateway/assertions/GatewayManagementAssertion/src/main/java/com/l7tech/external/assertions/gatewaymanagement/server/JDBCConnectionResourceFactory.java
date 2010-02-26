package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 
 */
@ResourceFactory.ResourceType(type=JDBCConnectionMO.class)
public class JDBCConnectionResourceFactory extends EntityManagerResourceFactory<JDBCConnectionMO, JdbcConnection, EntityHeader> {

    //- PUBLIC

    public JDBCConnectionResourceFactory( final RbacServices services,
                                          final SecurityFilter securityFilter,
                                          final PlatformTransactionManager transactionManager,
                                          final JdbcConnectionManager jdbcConnectionManager ) {
        super( true, true, services, securityFilter, transactionManager, jdbcConnectionManager );
    }

    //- PROTECTED

    @Override
    protected JDBCConnectionMO asResource( final JdbcConnection jdbcConnection ) {
        JDBCConnectionMO jdbcConnectionMO = ManagedObjectFactory.createJDBCConnection();

        jdbcConnectionMO.setId( jdbcConnection.getId() );
        jdbcConnectionMO.setVersion( jdbcConnection.getVersion() );
        jdbcConnectionMO.setName( jdbcConnection.getName() );
        jdbcConnectionMO.setEnabled( jdbcConnection.isEnabled() );

        return jdbcConnectionMO;
    }
}
