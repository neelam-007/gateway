package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.Entity;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.CollectionUtils.MapBuilder;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Option;

import javax.validation.groups.Default;
import javax.xml.bind.annotation.XmlEnumValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    public static String getEnumText( final Enum value ) {
        return getEnumAnnotationValue(value).orSome( value.name() );
    }

    public static Unary<String,Enum> getEnumText() {
        return new Unary<String, Enum>() {
            @Override
            public String call( final Enum value ) {
                return getEnumAnnotationValue(value).orSome( value.name() );
            }
        };
    }

    public static <E extends Enum<E>> E getEnumValue( final Class<E> enumType, final String value ) throws ResourceFactory.InvalidResourceException {
        E enumValue = null;

        final Set<String> xmlValues = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
        for ( final E currentEnum : EnumSet.allOf( enumType ) ) {
            final Option<String> enumText = getEnumAnnotationValue( currentEnum );
            xmlValues.add( enumText.orSome( currentEnum.name() ) );
            if ( enumText.isSome() && enumText.some().equals( value ) ) {
                enumValue = currentEnum;
                break;
            }
        }

        if ( enumValue == null ) {
            try {
                return Enum.valueOf( enumType, value );
            } catch ( IllegalArgumentException e ) {
                throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid value '"+value+"', expected one of " + xmlValues);
            }
        }

        return enumValue;
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

    private static Option<String> getEnumAnnotationValue( final Enum value ) {
        Option<String> textValue;
        try {
            final XmlEnumValue valueAnnotation =
                    value.getDeclaringClass().getField( value.name() ).getAnnotation( XmlEnumValue.class );
            if ( valueAnnotation != null ) {
                textValue = Option.some( valueAnnotation.value() );
            } else {
                textValue = Option.none();
            }
        } catch ( NoSuchFieldException e ) {
            textValue = Option.none();
        }
        return textValue;
    }

    private static final Map<Class<? extends Entity>,Map<String,String>> PROPERTY_MAP = MapBuilder.<Class<? extends Entity>,Map<String,String>>builder()
        .put( FederatedIdentityProviderConfig.class, MapBuilder.<String,String>builder()
            .put( "samlSupported", "enableCredentialType.saml" )
            .put( "x509Supported", "enableCredentialType.x509" )
            .unmodifiableMap() )
        .put( LdapIdentityProviderConfig.class, MapBuilder.<String,String>builder()
            .put( "adminEnabled", null )
            .put( "groupCacheMaxAge", "groupCacheMaximumAge" )
            .put( "groupCacheSize", null )
            .put( "groupMaxNesting", "groupMaximumNesting" )
            .put( "groupMembershipCaseInsensitive", null )
            .put( "userCertificateIndexSearchFilter", null )
            .put( "userCertificateIssuerSerialSearchFilter", null )
            .put( "userCertificateSKISearchFilter", null )
            .put( "userCertificateUseType", "userCertificateUsage" )
            .unmodifiableMap() )
        .put( IdentityProviderConfig.class, MapBuilder.<String,String>builder()
            .put( "adminEnabled", null )
            .unmodifiableMap() )
        .put( JdbcConnection.class, MapBuilder.<String,String>builder()
            .put( "minPoolSize", "minimumPoolSize" )
            .put( "maxPoolSize", "maximumPoolSize" )
            .unmodifiableMap() )
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
        .put( SecurePassword.class, MapBuilder.<String, String>builder()
            .put( "description", null )
            .put( "lastUpdateAsDate", "lastUpdated" )
            .put( "usageFromVariable", null )
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
        .put( FederatedIdentityProviderConfig.class, MapBuilder.<String,Object>builder()
            .put( "samlSupported", false )
            .put( "x509Supported", false )
            .unmodifiableMap() )
        .put( LdapIdentityProviderConfig.class, MapBuilder.<String,Object>builder()
            .put( "adminEnabled", false )
            .put( "groupCacheMaxAge", 60000L )
            .put( "groupCacheSize", 100 )
            .put( "groupMaxNesting", 0 )
            .put( "groupMembershipCaseInsensitive", false )
            .unmodifiableMap() )
        .put( IdentityProviderConfig.class, MapBuilder.<String,Object>builder()
            .put( "adminEnabled", false )
            .unmodifiableMap() )
        .put( JdbcConnection.class, MapBuilder.<String,Object>builder()
            .put( "minPoolSize", 3 )
            .put( "maxPoolSize", 15 )
            .unmodifiableMap()
        )
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
        .put( SecurePassword.class, MapBuilder.<String,Object>builder()
            .put( "usageFromVariable", false )
            .unmodifiableMap()
        )
        .put( TrustedCert.class, MapBuilder.<String,Object>builder()
            .put( "trustAnchor", false )
            .put( "trustedAsSamlAttestingEntity", false )
            .put( "trustedAsSamlIssuer", false )
            .put( "trustedForSigningClientCerts", false )
            .put( "trustedForSigningServerCerts", false )
            .put( "trustedForSsl", false )
            .put( "verifyHostname", true )
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
        .put( FederatedIdentityProviderConfig.class, Collections.unmodifiableCollection( Arrays.asList(
            "adminEnabled",
            "certificateValidationType",
            "description",
            "id",
            "importedGroupMembership",
            "importedGroups",
            "importedUsers",
            "name",
            "oid",
            "serializedProps",
            "trustedCertOids",
            "typeVal",
            "version"
        ) ) )
        .put( LdapIdentityProviderConfig.class, Collections.unmodifiableCollection( Arrays.asList(
            "bindDN",
            "bindPasswd",
            "certificateValidationType",
            "clientAuthEnabled",
            "description",
            "groupCacheMaxAgeUnit",
            "groupMappings",
            "id",
            "keyAlias",
            "keystoreId",
            "ldapUrl",
            "name",
            "oid",
            "returningAttributes",
            "searchBase",
            "serializedProps",
            "templateName",
            "typeVal",
            "userMappings",
            "version"
        ) ) )
        .put( IdentityProviderConfig.class, Collections.unmodifiableCollection( Arrays.asList(
            "certificateValidationType",
            "description",
            "id",
            "name",
            "oid",
            "serializedProps",
            "typeVal",
            "version"
        ) ) )
        .put( JdbcConnection.class, Collections.unmodifiableCollection( Arrays.asList(
            "driverClass",
            "enabled",
            "id",
            "jdbcUrl",
            "name",
            "oid",
            "password",
            "serializedProps",
            "userName",            
            "version"
        ) ) )
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
        .put( SecurePassword.class, Collections.unmodifiableCollection( Arrays.asList(
            "encodedPassword",
            "id",
            "lastUpdate",
            "name",
            "oid",
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
}
