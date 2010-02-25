package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.JMSConnection;
import com.l7tech.gateway.api.JMSDestinationDetails;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * 
 */
@ResourceFactory.ResourceType(type=JMSDestinationMO.class)
public class JMSDestinationResourceFactory extends EntityManagerResourceFactory<JMSDestinationMO, JmsEndpoint, JmsEndpointHeader> {

    //- PUBLIC

    public JMSDestinationResourceFactory( final RbacServices services,
                                          final SecurityFilter securityFilter,
                                          final PlatformTransactionManager transactionManager,
                                          final JmsEndpointManager jmsEndpointManager,
                                          final JmsConnectionManager jmsConnectionManager ) {
        super( false, false, services, securityFilter, transactionManager, jmsEndpointManager );
        this.jmsConnectionManager = jmsConnectionManager;
    }

    //- PROTECTED


    @Override
    protected EntityBag<JmsEndpoint> loadEntityBag( final JmsEndpoint jmsEndpoint ) throws ObjectModelException {
        final JmsConnection jmsConnection = jmsConnectionManager.findByPrimaryKey( jmsEndpoint.getConnectionOid() );
        if ( jmsConnection == null ) {
            throw new ResourceAccessException("JmsConnection not found " + jmsEndpoint.getConnectionOid());
        }

        checkPermitted( OperationType.READ, null, jmsConnection );

        return new JmsEntityBag( jmsEndpoint, jmsConnection );
    }

    @Override
    protected JMSDestinationMO asResource( final EntityBag<JmsEndpoint> entityBag ) {
        final JmsEntityBag jmsEntityBag = cast( entityBag, JmsEntityBag.class );
        final JmsEndpoint jmsEndpoint = jmsEntityBag.getJmsEndpoint();
        final JmsConnection jmsConnectionEntity = jmsEntityBag.getJmsConnection();

        final JMSDestinationDetails jmsDetails = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetails.setId( jmsEndpoint.getId() );
        jmsDetails.setVersion( jmsEndpoint.getVersion() );
        jmsDetails.setDestinationName( jmsEndpoint.getDestinationName() );
        jmsDetails.setEnabled( !jmsEndpoint.isDisabled() );
        jmsDetails.setInbound( jmsEndpoint.isMessageSource() );
        jmsDetails.setProperties( getProperties( jmsEndpoint, JmsEndpoint.class ) );

        final JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setId( jmsConnectionEntity.getId() );
        jmsConnection.setVersion( jmsConnectionEntity.getVersion() );
        jmsConnection.setProperties( getProperties( jmsConnectionEntity, JmsConnection.class ) );
        jmsConnection.setContextPropertiesTemplate( asProperties(jmsConnectionEntity.properties()) );

        final JMSDestinationMO jmsDestination = ManagedObjectFactory.createJMSDestination();
        jmsDestination.setId( jmsEndpoint.getId() );
        jmsDestination.setJmsDestinationDetails( jmsDetails );
        jmsDestination.setJmsConnection( jmsConnection );

        return jmsDestination;
    }

    @Override
    protected EntityBag<JmsEndpoint> fromResourceAsBag( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof JMSDestinationMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected jms destination");

        final JMSDestinationMO jmsDestination = (JMSDestinationMO) resource;
        final JMSDestinationDetails jmsDestinationDetails = jmsDestination.getJmsDestinationDetails();
        final JMSConnection jmsConnectionMO = jmsDestination.getJmsConnection();
        if ( jmsDestinationDetails == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing details");
        }
        if ( jmsConnectionMO == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing connection");            
        }

        final JmsEndpoint jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setDestinationName( jmsDestinationDetails.getDestinationName() );
        jmsEndpoint.setDisabled( !jmsDestinationDetails.isEnabled() );
        jmsEndpoint.setMessageSource( jmsDestinationDetails.isInbound() );
        setProperties( jmsEndpoint, jmsDestinationDetails.getProperties(), JmsEndpoint.class );

        final JmsConnection jmsConnection = new JmsConnection();
        setIdentifier( jmsConnection, jmsConnectionMO.getId(), false );
        setVersion( jmsConnection, jmsConnectionMO.getVersion(), false );
        jmsConnection.properties( asProperties( jmsConnectionMO.getContextPropertiesTemplate() ) );
        setProperties( jmsConnection, jmsConnectionMO.getProperties(), JmsConnection.class );

        return new JmsEntityBag( jmsEndpoint, jmsConnection );
    }

    @Override
    protected void updateEntityBag( final EntityBag<JmsEndpoint> oldEntityBag, final EntityBag<JmsEndpoint> newEntityBag ) throws InvalidResourceException {
        final JmsEntityBag oldJmsEntityBag = cast( oldEntityBag, JmsEntityBag.class );
        final JmsEndpoint oldJmsEndpoint = oldJmsEntityBag.getJmsEndpoint();
        final JmsConnection oldJmsConnection = oldJmsEntityBag.getJmsConnection();

        final JmsEntityBag newJmsEntityBag = cast( newEntityBag, JmsEntityBag.class );
        final JmsEndpoint newJmsEndpoint = newJmsEntityBag.getJmsEndpoint();
        final JmsConnection newJmsConnection = newJmsEntityBag.getJmsConnection();

        // Validate identity and version
        if ( oldJmsConnection.getOid() != PersistentEntity.DEFAULT_OID &&
             oldJmsConnection.getOid() != newJmsConnection.getOid() ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "identifier mismatch");
        }

        if ( oldJmsConnection.getVersion() != newJmsConnection.getVersion() ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid version");
        }

        // Copy endpoint properties that allow update
        oldJmsEndpoint.setName( newJmsEndpoint.getDestinationName() ); // Currently name is a copy of the destination name
        oldJmsEndpoint.setDestinationName( newJmsEndpoint.getDestinationName() );
        oldJmsEndpoint.setDisabled( newJmsEndpoint.isDisabled() );
        oldJmsEndpoint.setMessageSource( newJmsEndpoint.isMessageSource() );
        oldJmsEndpoint.setAcknowledgementType( newJmsEndpoint.getAcknowledgementType() );
        oldJmsEndpoint.setFailureDestinationName( newJmsEndpoint.getFailureDestinationName() );
        oldJmsEndpoint.setOutboundMessageType( newJmsEndpoint.getOutboundMessageType() );
        oldJmsEndpoint.setReplyToQueueName( newJmsEndpoint.getReplyToQueueName() );
        oldJmsEndpoint.setReplyType( newJmsEndpoint.getReplyType() );
        oldJmsEndpoint.setUsername( newJmsEndpoint.getUsername() );
        oldJmsEndpoint.setPassword( newJmsEndpoint.getPassword() );
        oldJmsEndpoint.setUseMessageIdForCorrelation( newJmsEndpoint.isUseMessageIdForCorrelation() );

        // Copy connection properties that allow update
        oldJmsConnection.setDestinationFactoryUrl( newJmsConnection.getDestinationFactoryUrl() );
        oldJmsConnection.setInitialContextFactoryClassname( newJmsConnection.getInitialContextFactoryClassname() );
        oldJmsConnection.setJndiUrl( newJmsConnection.getJndiUrl() );
        oldJmsConnection.setQueueFactoryUrl( newJmsConnection.getQueueFactoryUrl() );
        oldJmsConnection.setTopicFactoryUrl( newJmsConnection.getTopicFactoryUrl() );
        oldJmsConnection.setUsername( newJmsConnection.getUsername() );
        oldJmsConnection.setPassword( newJmsConnection.getPassword() );
        oldJmsConnection.properties( newJmsConnection.properties() );
    }

    @Override
    protected void afterDeleteEntity( final EntityBag<JmsEndpoint> entityBag ) throws ObjectModelException {
        final JmsEntityBag jmsEntityBag = cast( entityBag, JmsEntityBag.class );
        final JmsConnection jmsConnection = jmsEntityBag.getJmsConnection();

        checkPermitted( OperationType.DELETE, null, jmsConnection );        

        jmsConnectionManager.delete( jmsConnection );
    }

    @Override
    protected void afterUpdateEntity( final EntityBag<JmsEndpoint> entityBag ) throws ObjectModelException {
        final JmsEntityBag jmsEntityBag = cast( entityBag, JmsEntityBag.class );
        final JmsConnection jmsConnection = jmsEntityBag.getJmsConnection();

        checkPermitted( OperationType.UPDATE, null, jmsConnection );

        jmsConnectionManager.update( jmsConnection );
    }

    @Override
    protected void beforeCreateEntity( final EntityBag<JmsEndpoint> entityBag ) throws ObjectModelException {
        final JmsEntityBag jmsEntityBag = cast( entityBag, JmsEntityBag.class );
        final JmsConnection jmsConnection = jmsEntityBag.getJmsConnection();

        checkPermitted( OperationType.CREATE, null, jmsConnection );

        final long connectionId = jmsConnectionManager.save( jmsConnection );
        jmsEntityBag.getJmsEndpoint().setConnectionOid( connectionId );
    }

    protected static class JmsEntityBag extends EntityBag<JmsEndpoint> {
        private final JmsConnection jmsConnection;

        protected JmsEntityBag( final JmsEndpoint entity, final JmsConnection jmsConnection ) {
            super( entity );
            this.jmsConnection = jmsConnection;
        }

        protected JmsEndpoint getJmsEndpoint() {
            return getEntity();
        }

        protected JmsConnection getJmsConnection() {
            return jmsConnection;
        }

        @Override
        public Iterator<PersistentEntity> iterator() {
            return Arrays.<PersistentEntity>asList( getJmsEndpoint(), getJmsConnection() ).iterator();
        }
    }

    //- PRIVATE

    private final JmsConnectionManager jmsConnectionManager;

    private Map<String, Object> asProperties( final Properties properties ) {
        final Map<String,Object> propMap = new HashMap<String,Object>();

        for ( final String property : properties.stringPropertyNames() ) {
            propMap.put( property, properties.getProperty( property ));   
        }

        return propMap;
    }

    private Properties asProperties( final Map<String, Object> propertiesMap ) throws InvalidResourceException {
        final Properties properties = new Properties();

        if ( propertiesMap != null ) {
            for ( Map.Entry<String,Object> entry : propertiesMap.entrySet() ) {
                if ( !(entry.getValue() instanceof String) ) {
                    if ( entry.getValue() == null ) continue; // skip nulls
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "string property expected");
                }

                properties.setProperty( entry.getKey(), (String) entry.getValue() );
            }
        }

        return properties;
    }

}
