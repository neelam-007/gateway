package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.Entity;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.security.cert.TrustedCert;

import javax.validation.groups.Default;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
class EntityPropertiesHelper {

    //- PUBLIC

    public Collection<String> getIgnoredProperties( final Class<? extends Entity> entityClass )  {
        Collection<String> properties = IGNORE_PROPERTIES.get( entityClass );
        if ( properties == null ) {
            properties = Collections.emptyList();
        }
        return properties;
    }

    public Collection<String> getWriteOnlyProperties( final Class<? extends Entity> entityClass )  {
        Collection<String> properties = WRITE_ONLY_PROPERTIES.get( entityClass );
        if ( properties == null ) {
            properties = Collections.emptyList();
        }
        return properties;
    }

    public Map<String,String> getPropertiesMap( final Class<? extends Entity> entityClass )  {
        Map<String,String> properties = PROPERTY_MAP.get( entityClass );
        if ( properties == null ) {
            properties = Collections.emptyMap();
        }
        return properties;
    }

    public Map<String,Object> getPropertyDefaultsMap( final Class<? extends Entity> entityClass )  {
        Map<String,Object> properties = DEFAULTS_PROPERTY_MAP.get( entityClass );
        if ( properties == null ) {
            properties = Collections.emptyMap();
        }
        return properties;
    }

    public String getEnumText( final Enum value ) {
        return value.name();
    }

    public <E extends Enum<E>> E getEnumValue( final Class<E> enumType, final String value ) {
        try {
            return Enum.valueOf( enumType, value );
        } catch ( IllegalArgumentException e ) {
            throw new ResourceFactory.ResourceAccessException("Invalid value '"+value+"', expected one of " + EnumSet.allOf( enumType ));
        }
    }

    public Class[] getValidationGroups( final Object bean ) {
        Class[] groups = new Class[]{ Default.class };

        if ( bean instanceof JmsEndpoint ) {
            JmsEndpoint endpoint = (JmsEndpoint) bean;
            if ( !endpoint.isTemplate() ) {
                groups = new Class[]{ Default.class, JmsEndpoint.StandardValidationGroup.class };
            }
        } else if ( bean instanceof JmsConnection ) {
            JmsConnection connection = (JmsConnection) bean;
            if ( !connection.isTemplate() ) {
                groups = new Class[]{ Default.class, JmsConnection.StandardValidationGroup.class };
            }
        } else if ( bean instanceof PublishedService ) {
            PublishedService service = (PublishedService) bean;
            if ( service.isSoap() ) {
                groups = new Class[]{ Default.class, PublishedService.SoapValidationGroup.class };
            }
        } else if ( bean instanceof Policy ) {
            Policy policy = (Policy) bean;
            if ( policy.getType() == PolicyType.GLOBAL_FRAGMENT ) {
                groups = new Class[]{ Default.class, Policy.GlobalPolicyValidationGroup.class };
            }
        }

        return groups;
    }

    //- PRIVATE

    private static final Map<Class<? extends Entity>,Map<String,String>> PROPERTY_MAP = MapBuilder.<Class<? extends Entity>,Map<String,String>>builder()
        .put( JmsConnection.class, MapBuilder.<String,String>builder()
            .put( "initialContextFactoryClassname", "jndi.initialContextFactoryClassname" )
            .put( "jndiUrl", "jndi.providerUrl" )
            .put( "username", null )
            .put( "password", null )
            .put( "queueFactoryUrl", "queue.connectionFactoryName" )
            .put( "destinationFactoryUrl", "connectionFactoryName" )
            .put( "topicFactoryUrl", "topic.connectionFactoryName" )
            .unmodifiableMap() )
        .put( JmsEndpoint.class, MapBuilder.<String,String>builder()
            .put( "username", null )
            .put( "password", null )
            .put( "replyType", null )
            .put( "replyToQueueName", null )
            .put( "useMessageIdForCorrelation", "useRequestCorrelationId" )
            .put( "acknowledgementType", "inbound.acknowledgementType" )
            .put( "failureDestinationName", "inbound.failureQueueName" )
            .put( "outboundMessageType", "outbound.MessageType" )
            .unmodifiableMap() )
        .put( Policy.class, MapBuilder.<String,String>builder()
            .put( "versionOrdinal", "revision" )
            .put( "soap", null )
            .put( "internalTag", "tag" )
            .unmodifiableMap() )
        .put( PublishedService.class, MapBuilder.<String,String>builder()
            .put( "internal", null )
            .put( "soap", null )
            .put( "defaultRoutingUrl", null )
            .put( "wssProcessingEnabled", null )
            .unmodifiableMap() )
        .put( ResourceEntry.class, MapBuilder.<String,String>builder()
            .put( "description", null )
            .unmodifiableMap() )
        .put( TrustedCert.class, MapBuilder.<String,String>builder()
            .put( "trustAnchor", null )
            .put( "trustedAsSamlAttestingEntity", null )
            .put( "trustedAsSamlIssuer", null )
            .put( "trustedForSigningClientCerts", null )
            .put( "trustedForSigningServerCerts", null )
            .put( "trustedForSsl", null )
            .put( "verifyHostname", null )
            .unmodifiableMap() )
        .unmodifiableMap();

    private static final Map<Class<? extends Entity>,Map<String,Object>> DEFAULTS_PROPERTY_MAP = MapBuilder.<Class<? extends Entity>,Map<String,Object>>builder()
        .put( JmsEndpoint.class, MapBuilder.<String,Object>builder()
            .put( "replyType", "AUTOMATIC" )
            .put( "useMessageIdForCorrelation", false )
            .put( "acknowledgementType", "AUTOMATIC" )
            .put( "outboundMessageType", "AUTOMATIC" )
            .unmodifiableMap()
        )
        .put( Policy.class, MapBuilder.<String,Object>builder()
            .put( "versionOrdinal", 0 )
            .put( "soap", false )
            .unmodifiableMap()
        )
        .put( PublishedService.class, MapBuilder.<String,Object>builder()
            .put( "internal", false )
            .put( "soap", false )
            .put( "wssProcessingEnabled", true )
            .unmodifiableMap()
        )
        .unmodifiableMap();

    private static final Map<Class<? extends Entity>,Collection<String>> WRITE_ONLY_PROPERTIES = MapBuilder.<Class<? extends Entity>,Collection<String>>builder()
        .put( JmsConnection.class, Collections.unmodifiableCollection( Arrays.asList(
            "password"
        ) ) )
        .put( JmsEndpoint.class, Collections.unmodifiableCollection( Arrays.asList(
            "password"
        ) ) )
        .unmodifiableMap();

    private static final Map<Class<? extends Entity>,Collection<String>> IGNORE_PROPERTIES = MapBuilder.<Class<? extends Entity>,Collection<String>>builder()
        .put( JmsConnection.class, Collections.unmodifiableCollection( Arrays.asList(
            "version",
            "id",
            "name",
            "oid",
            "properties",
            "providerType",
            "template"
        ) ) )
        .put( JmsEndpoint.class, Collections.unmodifiableCollection( Arrays.asList(
            "oid",
            "id",
            "name",
            "destinationName",
            "version",
            "disabled",
            "connectionOid",
            "maxConcurrentRequests",
            "messageSource",
            "template",
            "queue"
        ) ) )
        .put( Policy.class, Collections.unmodifiableCollection( Arrays.asList(
            "xml",
            "oid",
            "guid",
            "versionActive",
            "folder",
            "id",
            "type",
            "version",
            "name"
        ) ) )
        .put( PublishedService.class, Collections.unmodifiableCollection( Arrays.asList(
            "wsdlUrl",
            "id",
            "policy",
            "folder",
            "httpMethods",
            "wsdlXml",
            "routingUri",
            "version",
            "name",
            "laxResolution",
            "tracingEnabled",
            "oid",
            "disabled",
            "soapVersion"
        ) ) )
        .put( ResourceEntry.class, Collections.unmodifiableCollection( Arrays.asList(
            "content",
            "contentType",
            "id",
            "oid",
            "resourceKey1",
            "resourceKey2",
            "resourceKey3",
            "sourceUrl",
            "type",
            "uri",
            "uriHash",
            "version"
        )))
        .put( TrustedCert.class, Collections.unmodifiableCollection( Arrays.asList(
            "ski",
            "certBase64",
            "version",
            "thumbprintSha1",
            "issuerDn",
            "id",
            "oid",
            "name",
            "serial",
            "subjectDn",
            "revocationCheckPolicyOid",
            "revocationCheckPolicyType",
            "certificate"
        ) ) )
        .unmodifiableMap();

    private static final class MapBuilder<K,V> {
        private final Map<K,V> map = new HashMap<K,V>();

        public static <K,V> MapBuilder<K,V> builder() {
            return new MapBuilder<K,V>();
        }

        public MapBuilder<K,V> put( K key, V value ) {
            map.put( key, value );
            return this;
        }

        public Map<K,V> map() {
            return map;
        }

        public Map<K,V> unmodifiableMap() {
            return Collections.unmodifiableMap( map );
        }
    }
}
