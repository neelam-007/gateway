package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.JMSConnection;
import com.l7tech.gateway.api.JMSDestinationDetail;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsEndpointMessagePropertyRule;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.Option;
import static com.l7tech.util.TextUtils.trim;

import com.l7tech.util.ValidationUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

/**
 * 
 */
@ResourceFactory.ResourceType(type=JMSDestinationMO.class)
public class JMSDestinationResourceFactory extends SecurityZoneableEntityManagerResourceFactory<JMSDestinationMO, JmsEndpoint, JmsEndpointHeader> {

    //- PUBLIC

    public JMSDestinationResourceFactory( final RbacServices services,
                                          final SecurityFilter securityFilter,
                                          final PlatformTransactionManager transactionManager,
                                          final JmsEndpointManager jmsEndpointManager,
                                          final JmsConnectionManager jmsConnectionManager,
                                          final SecurityZoneManager securityZoneManager) {
        super( false, false, services, securityFilter, transactionManager, jmsEndpointManager, securityZoneManager );
        this.jmsConnectionManager = jmsConnectionManager;
    }

    //- PROTECTED


    @Override
    protected EntityBag<JmsEndpoint> loadEntityBag( final JmsEndpoint jmsEndpoint ) throws ObjectModelException {
        final JmsConnection jmsConnection = jmsConnectionManager.findByPrimaryKey( jmsEndpoint.getConnectionGoid() );
        if ( jmsConnection == null ) {
            throw new ResourceAccessException("JmsConnection not found " + jmsEndpoint.getConnectionGoid());
        }

        checkPermitted( OperationType.READ, null, jmsConnection );

        return new JmsEntityBag( jmsEndpoint, jmsConnection );
    }

    @Override
    protected JMSDestinationMO asResource( final EntityBag<JmsEndpoint> entityBag ) {
        final JmsEntityBag jmsEntityBag = cast( entityBag, JmsEntityBag.class );
        final JmsEndpoint jmsEndpoint = jmsEntityBag.getJmsEndpoint();
        final JmsConnection jmsConnectionEntity = jmsEntityBag.getJmsConnection();

        final JMSDestinationDetail jmsDetails = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetails.setId( jmsEndpoint.getId() );
        jmsDetails.setVersion( jmsEndpoint.getVersion() );
        jmsDetails.setName( jmsEndpoint.getName() );
        jmsDetails.setDestinationName( jmsEndpoint.getDestinationName() );
        jmsDetails.setEnabled( !jmsEndpoint.isDisabled() );
        jmsDetails.setTemplate( jmsEndpoint.isTemplate() );
        jmsDetails.setInbound( jmsEndpoint.isMessageSource() );
        jmsDetails.setPassthroughMessageRules(jmsEndpoint.isPassthroughMessageRules());

        final List<JMSDestinationDetail.JmsEndpointMessagePropertyRuleSupport> ruleList = createJmsEndpointMessagePropertyRuleList(jmsEndpoint);
        jmsDetails.setJmsEndpointMessagePropertyRules(ruleList);

        final Map<String,Object> properties = getProperties( jmsEndpoint, JmsEndpoint.class );
        properties.put( "type", jmsEndpoint.isQueue() ? JMSDestinationDetail.TYPE_QUEUE : JMSDestinationDetail.TYPE_TOPIC );
        jmsDetails.setProperties( properties );



        final JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setId( jmsConnectionEntity.getId() );
        jmsConnection.setVersion( jmsConnectionEntity.getVersion() );
        jmsConnection.setProviderType( providerType( jmsConnectionEntity.getProviderType() ) );
        jmsConnection.setTemplate( jmsConnectionEntity.isTemplate() );
        jmsConnection.setProperties( getProperties( jmsConnectionEntity, JmsConnection.class ) );
        jmsConnection.setContextPropertiesTemplate( asProperties(jmsConnectionEntity.properties()) );

        final JMSDestinationMO jmsDestination = ManagedObjectFactory.createJMSDestination();
        jmsDestination.setId( jmsEndpoint.getId() );
        jmsDestination.setJmsDestinationDetail( jmsDetails );
        jmsDestination.setJmsConnection( jmsConnection );

        // handleSecurityZone - use zone from JMS endpoint
        doSecurityZoneAsResource( jmsDestination, jmsEndpoint );

        return jmsDestination;
    }

    private List<JMSDestinationDetail.JmsEndpointMessagePropertyRuleSupport> createJmsEndpointMessagePropertyRuleList(JmsEndpoint jmsEndpoint) {
        final List<JMSDestinationDetail.JmsEndpointMessagePropertyRuleSupport> ruleList =  new ArrayList<>();
        for(JmsEndpointMessagePropertyRule jmsRule : jmsEndpoint.getJmsEndpointMessagePropertyRules()){
            JMSDestinationDetail.JmsEndpointMessagePropertyRuleSupport jmsRuleSupport = ManagedObjectFactory.createJmsEndpointMessagePropertyRule();
            jmsRuleSupport.setRulename(jmsRule.getRuleName());
            jmsRuleSupport.setPassThru(jmsRule.isPassThru());
            jmsRuleSupport.setCustomPattern(jmsRule.getCustomPattern());
            ruleList.add(jmsRuleSupport);
        }
        return ruleList;
    }

    @Override
    protected EntityBag<JmsEndpoint> fromResourceAsBag( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof JMSDestinationMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected jms destination");

        final JMSDestinationMO jmsDestination = (JMSDestinationMO) resource;
        final JMSDestinationDetail jmsDestinationDetails = jmsDestination.getJmsDestinationDetail();
        final JMSConnection jmsConnectionMO = jmsDestination.getJmsConnection();
        if ( jmsDestinationDetails == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing details");
        }
        if ( jmsConnectionMO == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing connection");            
        }

        final JmsEndpoint jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setName( jmsDestinationDetails.getName() );
        jmsEndpoint.setDestinationName( jmsDestinationDetails.getDestinationName() );
        jmsEndpoint.setQueue( isQueue( jmsDestinationDetails.getProperties() ) );
        jmsEndpoint.setDisabled( !jmsDestinationDetails.isEnabled() );
        if ( jmsDestinationDetails.isTemplate() != null ) {
            jmsEndpoint.setTemplate( jmsDestinationDetails.isTemplate() );   
        }
        jmsEndpoint.setPassthroughMessageRules(jmsDestinationDetails.isPassthroughMessageRules());
        jmsEndpoint.setMessageSource( jmsDestinationDetails.isInbound() );
        setProperties( jmsEndpoint, jmsDestinationDetails.getProperties(), JmsEndpoint.class );
        //add JmsEndpointMessagePropertyRule
        Set<JmsEndpointMessagePropertyRule> jmsEndpointMessagePropertyRuleSet = new HashSet<>();
        List<JMSDestinationDetail.JmsEndpointMessagePropertyRuleSupport> jmsEndpointMessagePropertyRuleList = jmsDestinationDetails.getJmsEndpointMessagePropertyRules();
        if(jmsEndpointMessagePropertyRuleList != null){
            for(JMSDestinationDetail.JmsEndpointMessagePropertyRuleSupport ruleSupport : jmsEndpointMessagePropertyRuleList) {
                JmsEndpointMessagePropertyRule rule = new JmsEndpointMessagePropertyRule();
                rule.setRuleName(ruleSupport.getRulename());
                rule.setPassThru(ruleSupport.getPassThru());
                rule.setCustomPattern(ruleSupport.getCustomPattern());
                jmsEndpointMessagePropertyRuleSet.add(rule);
            }
        }
        jmsEndpoint.setJmsEndpointMessagePropertyRules(jmsEndpointMessagePropertyRuleSet);

        final JmsConnection jmsConnection = new JmsConnection();
        jmsConnection.setName( jmsDestinationDetails.getName() );
        jmsConnection.setProviderType( providerType( jmsConnectionMO.getProviderType() ) );
        if ( jmsConnectionMO.isTemplate() != null ) {
            jmsConnection.setTemplate( jmsConnectionMO.isTemplate() );
        }
        setIdentifier( jmsConnection, jmsConnectionMO.getId(), false );
        setVersion( jmsConnection, jmsConnectionMO.getVersion() );
        jmsConnection.properties( asProperties( jmsConnectionMO.getContextPropertiesTemplate() ) );
        setProperties( jmsConnection, jmsConnectionMO.getProperties(), JmsConnection.class );

        // map the service id value if required
        Properties props = jmsConnection.properties();
        String serviceId = props.getProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID);
        if(serviceId!=null){
            Goid serviceGoid = GoidUpgradeMapper.mapId(EntityType.SERVICE, serviceId);
            props.setProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID, serviceGoid.toString());
            jmsConnection.properties(props);
        }

        // handleSecurityZone
        doSecurityZoneFromResource( jmsDestination, jmsEndpoint );
        jmsConnection.setSecurityZone( jmsEndpoint.getSecurityZone() );

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

        // Validate identity and version (the endpoint is validated as the main entity)
        verifyIdentifier( oldJmsConnection.getGoid(), newJmsConnection.getGoid() );
        verifyVersion( oldJmsConnection.getVersion(), newJmsConnection.getVersion() );

        // Copy endpoint properties that allow update
        oldJmsEndpoint.setName( newJmsEndpoint.getName() );
        oldJmsEndpoint.setDestinationName( newJmsEndpoint.getDestinationName() );
        oldJmsEndpoint.setQueue( newJmsEndpoint.isQueue() );
        oldJmsEndpoint.setDisabled( newJmsEndpoint.isDisabled() );
        oldJmsEndpoint.setTemplate( newJmsEndpoint.isTemplate() );
        oldJmsEndpoint.setPassthroughMessageRules( newJmsEndpoint.isPassthroughMessageRules());
        oldJmsEndpoint.setMessageSource( newJmsEndpoint.isMessageSource() );
        oldJmsEndpoint.setAcknowledgementType( newJmsEndpoint.getAcknowledgementType() );
        oldJmsEndpoint.setFailureDestinationName( newJmsEndpoint.getFailureDestinationName() );
        oldJmsEndpoint.setOutboundMessageType( newJmsEndpoint.getOutboundMessageType() );
        oldJmsEndpoint.setReplyToQueueName( newJmsEndpoint.getReplyToQueueName() );
        oldJmsEndpoint.setReplyType( newJmsEndpoint.getReplyType() );
        oldJmsEndpoint.setUsername( newJmsEndpoint.getUsername() );
        if(newJmsEndpoint.getPassword()!=null){
            oldJmsEndpoint.setPassword( newJmsEndpoint.getPassword() );
        }
        oldJmsEndpoint.setUseMessageIdForCorrelation( newJmsEndpoint.isUseMessageIdForCorrelation() );
        oldJmsEndpoint.setRequestMaxSize(newJmsEndpoint.getRequestMaxSize());
        oldJmsEndpoint.setSecurityZone(newJmsEndpoint.getSecurityZone());
        oldJmsEndpoint.setJmsEndpointMessagePropertyRules(copyJmsEnpointMessagePropertyRules(oldJmsEndpoint, newJmsEndpoint));

        // Copy connection properties that allow update
        oldJmsConnection.setProviderType( newJmsConnection.getProviderType() );
        oldJmsConnection.setTemplate( newJmsConnection.isTemplate() );
        oldJmsConnection.setDestinationFactoryUrl( newJmsConnection.getDestinationFactoryUrl() );
        oldJmsConnection.setInitialContextFactoryClassname( newJmsConnection.getInitialContextFactoryClassname() );
        oldJmsConnection.setJndiUrl( newJmsConnection.getJndiUrl() );
        oldJmsConnection.setQueueFactoryUrl( newJmsConnection.getQueueFactoryUrl() );
        oldJmsConnection.setTopicFactoryUrl( newJmsConnection.getTopicFactoryUrl() );
        oldJmsConnection.setUsername( newJmsConnection.getUsername() );
        if(newJmsConnection.getPassword()!=null){
            oldJmsConnection.setPassword( newJmsConnection.getPassword() );
        }

        // not update the JNDI connection password if not specified
        Properties newProperties = newJmsConnection.properties();
        if(oldJmsConnection.properties().containsKey("java.naming.security.credentials") && !newProperties.containsKey("java.naming.security.credentials"))
            newProperties.setProperty("java.naming.security.credentials",oldJmsConnection.properties().getProperty("java.naming.security.credentials"));
        oldJmsConnection.properties( newProperties );

        oldJmsConnection.setSecurityZone( newJmsConnection.getSecurityZone() );
    }

    private Set<JmsEndpointMessagePropertyRule> copyJmsEnpointMessagePropertyRules(JmsEndpoint oldJmsEndpoint, JmsEndpoint newJmsEndpoint) {
        Set<JmsEndpointMessagePropertyRule> updatedSet = oldJmsEndpoint.getJmsEndpointMessagePropertyRules();
        updatedSet.clear();
        for(JmsEndpointMessagePropertyRule newRule : newJmsEndpoint.getJmsEndpointMessagePropertyRules()) {
            updatedSet.add(newRule.copy(oldJmsEndpoint));
        }
        return updatedSet;
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

        final Goid connectionId = jmsConnectionManager.save( jmsConnection );
        jmsEntityBag.getJmsEndpoint().setConnectionGoid(connectionId);
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

    private boolean isQueue( final Map<String,Object> properties ) throws InvalidResourceException {
        boolean queue = true;

        final Option<String> type = getProperty( properties, "type", Option.<String>none(), String.class).map( trim() );
        if ( type.isSome() ) {
            if ( JMSDestinationDetail.TYPE_QUEUE.equals( type.some() ) ) {
                queue = true;
            } else if ( JMSDestinationDetail.TYPE_TOPIC.equals( type.some() ) ) {
                queue = false;
            } else {
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid type '"+type+"'" );
            }
        }

        return queue;
    }

    private JmsProviderType providerType( final JMSConnection.JMSProviderType providerType ) throws InvalidResourceException {
        JmsProviderType type = null;

        if ( providerType != null ) {
            switch ( providerType ) {
                case TIBCO_EMS:
                    type = JmsProviderType.Tibco;
                    break;
                case WebSphere_MQ:
                    type = JmsProviderType.MQ;
                    break;
                case Weblogic:
                    type = JmsProviderType.Weblogic;
                    break;
                default:
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid provider type '"+providerType+"'" );
            }
        }

        return type;
    }

    private JMSConnection.JMSProviderType providerType( final JmsProviderType providerType ) throws ResourceAccessException {
        JMSConnection.JMSProviderType type = null;

        if ( providerType != null ) {
            switch ( providerType ) {
                case Tibco:
                    type = JMSConnection.JMSProviderType.TIBCO_EMS;
                    break;
                case MQ:
                    type = JMSConnection.JMSProviderType.WebSphere_MQ;
                    break;
                case Weblogic:
                    type = JMSConnection.JMSProviderType.Weblogic;
                    break;
                default:
                    throw new ResourceAccessException( "Invalid provider type '"+providerType+"'" );
            }
        }

        return type;
    }

    private Map<String, Object> asProperties( final Properties properties ) {
        final Map<String,Object> propMap = new HashMap<String,Object>();

        for ( final String property : properties.stringPropertyNames() ) {
            // not include JNDI connection password
            if(!property.equals("java.naming.security.credentials"))
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
