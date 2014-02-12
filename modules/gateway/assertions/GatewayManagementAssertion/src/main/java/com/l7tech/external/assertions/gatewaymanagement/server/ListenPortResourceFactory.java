package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.Functions.UnaryVoidThrows;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.*;

import static com.l7tech.gateway.api.ListenPortMO.TlsSettings;
import static com.l7tech.gateway.api.ListenPortMO.TlsSettings.ClientAuthentication;
import static com.l7tech.util.CollectionUtils.foreach;
import static com.l7tech.util.CollectionUtils.set;
import static com.l7tech.util.Functions.grepFirst;
import static com.l7tech.util.Option.*;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.join;

/**
 *
 */
@ResourceFactory.ResourceType(type=ListenPortMO.class)
public class ListenPortResourceFactory extends SecurityZoneableEntityManagerResourceFactory<ListenPortMO, SsgConnector, EntityHeader> {

    //- PUBLIC

    public ListenPortResourceFactory( final RbacServices services,
                                      final SecurityFilter securityFilter,
                                      final PlatformTransactionManager transactionManager,
                                      final SsgConnectorManager ssgConnectorManager,
                                      final SecurityZoneManager securityZoneManager ) {
        super( false, true, services, securityFilter, transactionManager, ssgConnectorManager, securityZoneManager );
        this.ssgConnectorManager = ssgConnectorManager;
    }

    //- PROTECTED

    @Override
    public ListenPortMO asResource( final SsgConnector entity ) {
        final ListenPortMO listenPort = ManagedObjectFactory.createListenPort();

        listenPort.setName( entity.getName() );
        listenPort.setEnabled( entity.isEnabled() );
        listenPort.setProtocol( entity.getScheme() );
        listenPort.setInterface( entity.getProperty( SsgConnector.PROP_BIND_ADDRESS ) );
        listenPort.setPort( entity.getPort() );
        listenPort.setEnabledFeatures( buildEnabledFeatures( optional( entity.getEndpoints() )) );
        listenPort.setTargetServiceId( entity.getProperty( SsgConnector.PROP_HARDWIRED_SERVICE_ID ) );
        listenPort.setTlsSettings( buildTlsSettings(entity) );
        listenPort.setProperties( buildProperties(entity) );

        // handle SecurityZone
        doSecurityZoneAsResource( listenPort, entity );

        return listenPort;
    }

    @Override
    protected SsgConnector fromResource( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof ListenPortMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected listen port");

        final ListenPortMO listenPort = (ListenPortMO) resource;

        final SsgConnector connector = new SsgConnector();
        connector.setName( asName( listenPort.getName() ) );
        connector.setEnabled( listenPort.isEnabled() );
        connector.setScheme( listenPort.getProtocol() );
        connector.setPort( listenPort.getPort() );
        connector.setEndpoints( buildEndpointsValue(listenPort.getEnabledFeatures()) );

        // properties first so these are overwritten by any explicitly set property
        final Map<String,Object> properties = listenPort.getProperties();
        if ( properties != null ) {
            for ( Map.Entry<String,Object> entry : properties.entrySet() ) {
                if ( !(entry.getValue() instanceof String) ) continue;
                validateProperty( entry.getKey(), (String) entry.getValue() );
                connector.putProperty( entry.getKey(), (String) entry.getValue() );
            }
        }

        putProperty( connector, SsgConnector.PROP_BIND_ADDRESS, optional( listenPort.getInterface() ) );
        Goid serviceId = GoidUpgradeMapper.mapId(EntityType.SERVICE, listenPort.getTargetServiceId());
        putProperty( connector, SsgConnector.PROP_HARDWIRED_SERVICE_ID, optional(serviceId==null?null:serviceId.toString() ));
        setTlsProperties( listenPort, connector );

        // handle SecurityZone
        doSecurityZoneFromResource( listenPort, connector );

        return connector;
    }

    @Override
    protected void updateEntity( final SsgConnector oldEntity,
                                 final SsgConnector newEntity ) throws InvalidResourceException {
        oldEntity.setName( newEntity.getName() );
        oldEntity.setEnabled( newEntity.isEnabled() );
        oldEntity.setScheme( newEntity.getScheme() );
        oldEntity.setSecure( newEntity.isSecure() );
        oldEntity.setPort( newEntity.getPort() );
        oldEntity.setEndpoints( newEntity.getEndpoints() );
        oldEntity.setClientAuth( newEntity.getClientAuth() );
        oldEntity.setKeystoreGoid(newEntity.getKeystoreGoid());
        oldEntity.setKeyAlias( newEntity.getKeyAlias() );
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );
        for ( final String name : oldEntity.getPropertyNames() ) {
            oldEntity.removeProperty( name );
        }
        for ( final String name : newEntity.getPropertyNames() ) {
            oldEntity.putProperty( name, newEntity.getProperty( name ) );
        }
    }

    //- PRIVATE

    private static final Set<String> IGNORE_PROPERTIES = set(
            SsgConnector.PROP_BIND_ADDRESS,
            SsgConnector.PROP_HARDWIRED_SERVICE_ID,
            SsgConnector.PROP_TLS_PROTOCOLS,
            SsgConnector.PROP_TLS_CIPHERLIST
    );
    private static final List<String> DEFAULT_TLS_VERSIONS = Collections.unmodifiableList( split( ConfigFactory.getProperty( "com.l7tech.external.assertions.gatewaymanagement.listenPortDefaultTlsVersions", "TLSv1" ) ) );

    private static final Unary<Option<String>,String> BOOLEAN_VALIDATOR =  new Unary<Option<String>,String>(){
        @Override
        public Option<String> call( final String text ) {
            return "true".equals( text ) || "false".equals( text ) ? Option.<String>none() : some( "true/false expected" );
        }
    };
    private static final Unary<Option<String>,String> CONTENT_TYPE_VALIDATOR = new Unary<Option<String>,String>(){
        @Override
        public Option<String> call( final String text ) {
            try {
                ContentTypeHeader.parseValue( text );
                return none();
            } catch ( final IOException e ) {
                return some( "Invalid content type, " + ExceptionUtils.getMessage( e ) );
            }
        }
    };
    private static final Unary<Option<String>,String> POSITIVE_INTEGER_VALIDATOR = new Unary<Option<String>,String>(){
        @Override
        public Option<String> call( final String text ) {
            return ValidationUtils.isValidInteger( text, false, 0, Integer.MAX_VALUE ) ? Option.<String>none() : some( "positive integer expected" );
        }
    };
    private static final Unary<Option<String>,String> POSITIVE_LONG_VALIDATOR = new Unary<Option<String>,String>(){
        @Override
        public Option<String> call( final String text ) {
            return ValidationUtils.isValidLong( text, false, 0L, Long.MAX_VALUE ) ? Option.<String>none() : some( "positive long integer expected" );
        }
    };

    private static final Map<String,Unary<Option<String>,String>> PROPERTY_VALIDATORS = CollectionUtils.<String,Unary<Option<String>,String>>mapBuilder()
            .put( "l7.raw.backlog", POSITIVE_INTEGER_VALIDATOR )
            .put( "l7.raw.readTimeout", POSITIVE_INTEGER_VALIDATOR )
            .put( "l7.raw.requestSizeLimit", POSITIVE_LONG_VALIDATOR )
            .put( "l7.raw.writeTimeout", POSITIVE_INTEGER_VALIDATOR )
            .put( "noSSLv2Hello", BOOLEAN_VALIDATOR )
            .put( "overrideContentType", CONTENT_TYPE_VALIDATOR )
            .put( "portRangeCount", POSITIVE_INTEGER_VALIDATOR )
            .put( "portRangeStart", POSITIVE_INTEGER_VALIDATOR )
            .put( "requestSizeLimit", POSITIVE_LONG_VALIDATOR )
            .put( "sessionCacheSize", POSITIVE_INTEGER_VALIDATOR )
            .put( "sessionCacheTimeout", POSITIVE_INTEGER_VALIDATOR )
            .put( "threadPoolSize", POSITIVE_INTEGER_VALIDATOR ).map();

    private final SsgConnectorManager ssgConnectorManager;

    private List<String> buildEnabledFeatures( final Option<String> endpoints ) {
        final List<String> features = new ArrayList<String>();

        foreach( SsgConnector.Endpoint.parseCommaList( endpoints.orSome( "" ) ), false, new UnaryVoid<Endpoint>() {
            @Override
            public void call( final SsgConnector.Endpoint endpoint ) {
                features.add( EntityPropertiesHelper.getEnumText( endpoint ) );
            }
        } );

        return features;
    }

    private String buildEndpointsValue( final List<String> enabledFeatures ) throws InvalidResourceException {
        final Set<SsgConnector.Endpoint> endpoints = new LinkedHashSet<SsgConnector.Endpoint>();

        foreach( enabledFeatures, false, new UnaryVoidThrows<String, InvalidResourceException>() {
            @Override
            public void call( final String enabledFeature ) throws InvalidResourceException {
                endpoints.add( EntityPropertiesHelper.getEnumValue( SsgConnector.Endpoint.class, enabledFeature ) );
            }
        } );

        return endpoints.isEmpty() ? null : SsgConnector.Endpoint.asCommaList( endpoints );
    }

    private TlsSettings buildTlsSettings( final SsgConnector entity ) {
        TlsSettings tlsSettings = null;

        if ( isSecureProtocol( entity.getScheme() ) ) {
            tlsSettings = ManagedObjectFactory.createTlsSettings();
            if ( entity.getKeyAlias() != null ) {
                tlsSettings.setPrivateKeyId( PrivateKeyResourceFactory.toExternalId( entity.getKeystoreGoid(), entity.getKeyAlias() ) );
            }
            switch ( entity.getClientAuth() ) {
                case SsgConnector.CLIENT_AUTH_ALWAYS:
                    tlsSettings.setClientAuthentication( ClientAuthentication.REQUIRED );
                    break;
                case SsgConnector.CLIENT_AUTH_NEVER:
                    tlsSettings.setClientAuthentication( ClientAuthentication.NONE );
                    break;
                case SsgConnector.CLIENT_AUTH_OPTIONAL:
                    tlsSettings.setClientAuthentication( ClientAuthentication.OPTIONAL );
                    break;
            }
            tlsSettings.setEnabledVersions( coalesce( split( entity.getProperty( SsgConnector.PROP_TLS_PROTOCOLS ) ), DEFAULT_TLS_VERSIONS ) );
            tlsSettings.setEnabledCipherSuites( split(entity.getProperty( SsgConnector.PROP_TLS_CIPHERLIST )) );
        }

        return tlsSettings;
    }

    private void setTlsProperties( final ListenPortMO listenPort,
                                   final SsgConnector connector ) throws InvalidResourceException {
        if ( isSecureProtocol( listenPort.getProtocol() ) ) {
            connector.setSecure( true );
            final Option<TlsSettings> tlsSettings = optional( listenPort.getTlsSettings() );
            if ( tlsSettings.isSome() ) {
                final Option<String> externalKeyIdentifier = optional( tlsSettings.some().getPrivateKeyId() );
                if ( externalKeyIdentifier.isSome() ) {
                    final Pair<Goid,String> keyIdentifier = PrivateKeyResourceFactory.toInternalId( externalKeyIdentifier.some(), PrivateKeyResourceFactory.INVALIDRESOURCE_THROWER );
                    connector.setKeystoreGoid(keyIdentifier.left);
                    connector.setKeyAlias( keyIdentifier.right );
                }
                putProperty( connector, SsgConnector.PROP_TLS_CIPHERLIST, ifNotEmpty( join( ",", tlsSettings.some().getEnabledCipherSuites() ).toString() ) );
                putProperty( connector, SsgConnector.PROP_TLS_PROTOCOLS, ifNotEmpty( join( ",", tlsSettings.some().getEnabledVersions() ).toString() ) );
                switch ( tlsSettings.some().getClientAuthentication() ) {
                    case NONE:
                        connector.setClientAuth( SsgConnector.CLIENT_AUTH_NEVER );
                        break;
                    case OPTIONAL:
                        connector.setClientAuth( SsgConnector.CLIENT_AUTH_OPTIONAL );
                        break;
                    case REQUIRED:
                        connector.setClientAuth( SsgConnector.CLIENT_AUTH_ALWAYS );
                        break;
                }
            } else {
                connector.setClientAuth( SsgConnector.CLIENT_AUTH_OPTIONAL );
            }
        }
    }

    private Map<String, Object> buildProperties( final SsgConnector entity ) {
        final Map<String,Object> properties = new HashMap<String,Object>();

        for ( final String name : entity.getPropertyNames() ) {
            if ( IGNORE_PROPERTIES.contains( name ) ) continue;
            properties.put( name, entity.getProperty( name ) );
        }

        return properties.isEmpty() ? null : properties;
    }

    private void putProperty( final SsgConnector connector,
                              final String property,
                              final Option<String> value ) {
        if ( value.isSome() ) {
            connector.putProperty( property, value.some() );
        }
    }

    private Option<String> ifNotEmpty( final String value ) {
        return optional( value ).filter( isNotEmpty() );
    }

    private boolean isSecureProtocol( final String protocol ) {
        boolean secure = false;

        if ( protocol != null ) {
            final TransportDescriptor transportDescriptor =
                    grepFirst(
                            Arrays.asList( ssgConnectorManager.getTransportProtocols() ),
                            new Unary<Boolean, TransportDescriptor>() {
                                @Override
                                public Boolean call( final TransportDescriptor transportDescriptor ) {
                                    return protocol.equals( transportDescriptor.getScheme() );
                                }
                            } );

            secure = transportDescriptor != null && transportDescriptor.isUsesTls();
        }

        return secure;
    }

    private void validateProperty( final String name,
                                   final String value ) throws InvalidResourceException {
        final Unary<Option<String>,String> validator = PROPERTY_VALIDATORS.get( name );

        if ( validator != null ) {
            Option<String> message = validator.call( value );
            if ( message.isSome() ) {
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid value for property '"+name+"': " + message );
            }
        }
    }

    private static List<String> split( final String commaSeparatedList ) {
        return commaSeparatedList == null ?
                null :
                Arrays.asList( commaSeparatedList.split( "\\s*,\\s*" ) );
    }

    //TODO [jdk7] @SafeVarargs
    private static <T extends Collection> T coalesce( final T... values ) {
        T result = null;

        for ( T value : values ) {
            if ( value != null && !value.isEmpty() ) {
                result = value;
                break;
            }
        }

        return result;
    }
}
