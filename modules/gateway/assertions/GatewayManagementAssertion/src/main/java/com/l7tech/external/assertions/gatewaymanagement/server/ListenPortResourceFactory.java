package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ListenPortMO;
import static com.l7tech.gateway.api.ListenPortMO.*;
import static com.l7tech.gateway.api.ListenPortMO.TlsSettings.*;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.transport.SsgConnectorManager;
import static com.l7tech.util.CollectionUtils.foreach;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.Functions.UnaryVoidThrows;
import static com.l7tech.util.Functions.grepFirst;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.join;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@ResourceFactory.ResourceType(type=ListenPortMO.class)
public class ListenPortResourceFactory extends EntityManagerResourceFactory<ListenPortMO, SsgConnector, EntityHeader> {

    //- PUBLIC

    public ListenPortResourceFactory( final RbacServices services,
                                      final SecurityFilter securityFilter,
                                      final PlatformTransactionManager transactionManager,
                                      final SsgConnectorManager ssgConnectorManager ) {
        super( false, true, services, securityFilter, transactionManager, ssgConnectorManager );
        this.ssgConnectorManager = ssgConnectorManager;
    }

    //- PROTECTED

    @Override
    protected ListenPortMO asResource( final SsgConnector entity ) {
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
        connector.setScheme( listenPort.getProtocol() ); //TODO [steve] validation for scheme values?
        connector.setPort( listenPort.getPort() );
        connector.setEndpoints( buildEndpointsValue(listenPort.getEnabledFeatures()) );

        // properties first so these are overwritten by any explicitly set property
        final Map<String,Object> properties = listenPort.getProperties();
        if ( properties != null ) {
            for ( Map.Entry<String,Object> entry : properties.entrySet() ) {
                if ( !(entry.getValue() instanceof String) ) continue;
                connector.putProperty( entry.getKey(), (String) entry.getValue() );
            }
        }

        putProperty( connector, SsgConnector.PROP_BIND_ADDRESS, optional( listenPort.getInterface() ) );
        putProperty( connector, SsgConnector.PROP_HARDWIRED_SERVICE_ID, optional( listenPort.getTargetServiceId() ) );
        setTlsProperties( listenPort, connector );

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
        oldEntity.setKeystoreOid( newEntity.getKeystoreOid() );
        oldEntity.setKeyAlias( newEntity.getKeyAlias() );
        for ( final String name : oldEntity.getPropertyNames() ) {
            oldEntity.removeProperty( name );
        }
        for ( final String name : newEntity.getPropertyNames() ) {
            oldEntity.putProperty( name, newEntity.getProperty( name ) );
        }
    }

    //- PRIVATE

    private static final Set<String> IGNORE_PROPERTIES = Collections.unmodifiableSet( new HashSet<String>( Arrays.asList(
        SsgConnector.PROP_BIND_ADDRESS,
        SsgConnector.PROP_HARDWIRED_SERVICE_ID,
        SsgConnector.PROP_TLS_PROTOCOLS,
        SsgConnector.PROP_TLS_CIPHERLIST
    ) ) );
    private static final List<String> DEFAULT_TLS_VERSIONS = Collections.unmodifiableList( split( SyspropUtil.getString( "com.l7tech.external.assertions.gatewaymanagement.listenPortDefaultTlsVersions", "TLSv1" ) ) );

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
                tlsSettings.setPrivateKeyId( PrivateKeyResourceFactory.toExternalId( entity.getKeystoreOid(), entity.getKeyAlias() ) );
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
                    final Pair<Long,String> keyIdentifier = PrivateKeyResourceFactory.toInternalId( externalKeyIdentifier.some(), PrivateKeyResourceFactory.INVALIDRESOURCE_THROWER );
                    connector.setKeystoreOid( keyIdentifier.left );
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
            // TODO [steve] type conversion for non String properties
// Might want to map these names / types
//allowUnsafeLegacyRenegotiation
//noSSLv2Hello
//overrideContentType text/xml; charset=utf-8
//overrideProtocols
//keepAliveTimeout 120000
//l7.raw.backlog
//l7.raw.readTimeout 2000
//l7.raw.requestSizeLimit
//l7.raw.writeTimeout
//portRangeCount 10
//portRangeStart 2223
//protocol
//protocolProvider
//requestSizeLimit 1000000
//sessionCacheSize
//sessionCacheTimeout
//threadPoolSize 10
        }

        return properties;
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
