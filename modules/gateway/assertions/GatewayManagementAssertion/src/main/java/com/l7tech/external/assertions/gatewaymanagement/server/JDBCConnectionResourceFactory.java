package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

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
        super( false, true, services, securityFilter, transactionManager, jdbcConnectionManager );
    }

    //- PROTECTED

    @Override
    protected JDBCConnectionMO asResource( final JdbcConnection jdbcConnection ) {
        JDBCConnectionMO jdbcConnectionMO = ManagedObjectFactory.createJDBCConnection();

        jdbcConnectionMO.setId( jdbcConnection.getId() );
        jdbcConnectionMO.setVersion( jdbcConnection.getVersion() );
        jdbcConnectionMO.setName( jdbcConnection.getName() );
        jdbcConnectionMO.setEnabled( jdbcConnection.isEnabled() );
        jdbcConnectionMO.setDriverClass( jdbcConnection.getDriverClass() );
        jdbcConnectionMO.setJdbcUrl( jdbcConnection.getJdbcUrl() );
        jdbcConnectionMO.setConnectionProperties( getConnectionProperties( jdbcConnection ) );
        jdbcConnectionMO.setProperties( getProperties( jdbcConnection, JdbcConnection.class ) );

        return jdbcConnectionMO;
    }

    @Override
    protected JdbcConnection fromResource( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof JDBCConnectionMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected jdbc connection");

        final JDBCConnectionMO connectionResource = (JDBCConnectionMO) resource;

        final JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName( connectionResource.getName() );
        jdbcConnection.setEnabled( connectionResource.isEnabled() );
        jdbcConnection.setDriverClass( connectionResource.getDriverClass() );
        jdbcConnection.setJdbcUrl( connectionResource.getJdbcUrl() );
        jdbcConnection.setUserName( getProperty( connectionResource.getConnectionProperties(), CONN_PROP_USER ) );
        jdbcConnection.setPassword( getProperty( connectionResource.getConnectionProperties(), CONN_PROP_PASSWORD ) );
        jdbcConnection.getAdditionalProperties().putAll( filterProperties(connectionResource.getConnectionProperties()) );

        setProperties( jdbcConnection, connectionResource.getProperties(), JdbcConnection.class );

        return jdbcConnection;
    }

    @Override
    protected void updateEntity( final JdbcConnection oldEntity, final JdbcConnection newEntity ) throws InvalidResourceException {
        oldEntity.setName( newEntity.getName() );
        oldEntity.setEnabled( newEntity.isEnabled() );
        oldEntity.setDriverClass( newEntity.getDriverClass() );
        oldEntity.setJdbcUrl( newEntity.getJdbcUrl() );
        oldEntity.setUserName( newEntity.getUserName() );
        if ( newEntity.getPassword() != null ) {
            oldEntity.setPassword( newEntity.getPassword() );
        }
        oldEntity.setMinPoolSize( newEntity.getMinPoolSize() );
        oldEntity.setMaxPoolSize( newEntity.getMaxPoolSize() );
        oldEntity.getAdditionalProperties().clear();
        oldEntity.getAdditionalProperties().putAll( newEntity.getAdditionalProperties() );
    }

    //- PRIVATE

    private static final String CONN_PROP_USER = "user";
    private static final String CONN_PROP_PASSWORD = "password";

    private String getProperty( final Map<String, Object> connectionProperties,
                                final String name ) throws InvalidResourceException {
        String propValue;

        final Object value = connectionProperties==null ? null : connectionProperties.get( name );
        if ( value != null && !(value instanceof String) ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, name + " property should be a string");
        }

        propValue = (String) value;

        return propValue;
    }

    private Map<String, Object> getConnectionProperties( final JdbcConnection jdbcConnection ) {
        final Map<String,Object> connectionProperties = new HashMap<String,Object>();

        connectionProperties.putAll( jdbcConnection.getAdditionalProperties() );

        // configured username/password override any in the properties
        // we don't include the password since it is "write only"
        connectionProperties.put( CONN_PROP_USER, jdbcConnection.getUserName() );
        connectionProperties.remove( CONN_PROP_PASSWORD );

        return connectionProperties;
    }

    private Map<String,Object> filterProperties( final Map<String, Object> connectionProperties ) {
        final Map<String,Object> additionalProperties = new HashMap<String,Object>();

        if ( connectionProperties != null ) {
            additionalProperties.putAll( connectionProperties );
            additionalProperties.remove( CONN_PROP_USER );
            additionalProperties.remove( CONN_PROP_PASSWORD );
        }

        return additionalProperties;
    }
}
