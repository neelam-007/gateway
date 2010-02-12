package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.CertificateData;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.transaction.PlatformTransactionManager;

import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
@ResourceFactory.ResourceType(type=PrivateKeyMO.class)
public class PrivateKeyResourceFactory extends ResourceFactorySupport<PrivateKeyMO> {

    //- PUBLIC

    public PrivateKeyResourceFactory( final RbacServices rbacServices,
                                      final SecurityFilter securityFilter,
                                      final PlatformTransactionManager transactionManager,
                                      final SsgKeyStoreManager ssgKeyStoreManager ) {
        super( rbacServices, securityFilter, transactionManager );
        this.ssgKeyStoreManager = ssgKeyStoreManager;    
    }

    @Override
    public EntityType getType() {
        return EntityType.SSG_KEY_ENTRY;
    }

    @Override
    public Set<String> getSelectors() {
        return Collections.singleton( IDENTITY_SELECTOR );
    }

    @Override
    public PrivateKeyMO getResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return transactional( new TransactionalCallback<PrivateKeyMO,ResourceNotFoundException>(){
            @Override
            public PrivateKeyMO execute() throws ObjectModelException, ResourceNotFoundException {
                final String id = selectorMap.get( IDENTITY_SELECTOR );
                if ( id == null ) {
                    throw new InvalidResourceSelectors();
                }

                final String[] keystoreAndAlias = id.split( ":" );
                if ( keystoreAndAlias.length!=2 ) {
                    throw new InvalidResourceSelectors();
                }

                final long keyStoreId = toInternalId( keystoreAndAlias[0] );
                final String alias = keystoreAndAlias[1];

                return buildPrivateKeyResource( keyStoreId, alias );
            }
        }, true, ResourceNotFoundException.class );
    }

    @Override
    public Collection<Map<String, String>> getResources() {
        return transactional( new TransactionalCallback<Collection<Map<String, String>>, ObjectModelException>(){
            @Override
            public Collection<Map<String, String>> execute() throws ObjectModelException {
                return Functions.map( getExternalIdentifiers(), new Functions.Unary<Map<String,String>,String>(){
                    @Override
                    public Map<String, String> call( final String id ) {
                        return Collections.singletonMap( IDENTITY_SELECTOR, id );
                    }
                } );
            }
        }, true );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PrivateKeyResourceFactory.class.getName() );

    private static final String IDENTITY_SELECTOR = "id";

    private final SsgKeyStoreManager ssgKeyStoreManager;

    /**
     * Convert the given identifier to the internal <code>long</code> format.
     *
     * @param identifier The identifier to process.
     * @return The identifier as a long
     * @throws InvalidResourceSelectors If the given identifier is not valid
     */
    private long toInternalId( final String identifier ) throws InvalidResourceSelectors {
        try {
            return Long.parseLong(identifier);
        } catch ( NumberFormatException nfe ) {
            throw new InvalidResourceSelectors();
        }
    }

    private String toExternalId( final long keyStoreId, final String alias ) {
        return keyStoreId + ":" + alias;
    }

    private Collection<String> getExternalIdentifiers() {
        final List<String> externalIds = new ArrayList<String>();

        try {
            for ( final SsgKeyFinder ssgKeyFinder : ssgKeyStoreManager.findAll() ) {
                for ( final String alias : ssgKeyFinder.getAliases() ) {
                    externalIds.add( toExternalId( ssgKeyFinder.getOid(), alias ) );
                }
            }
        } catch ( KeyStoreException e ) {
            throw new ResourceAccessException(e);
        } catch ( FindException e ) {
            throw new ResourceAccessException(e);
        }

        return externalIds;
    }

    private PrivateKeyMO buildPrivateKeyResource( final long keyStoreId, final String alias ) throws ObjectModelException, ResourceNotFoundException {
        final PrivateKeyMO privateKey = ManagedObjectFactory.createPrivateKey();

        SsgKeyEntry ssgKeyEntry = null;
        try {
            if ( keyStoreId == -1 ) {
                ssgKeyEntry = ssgKeyStoreManager.lookupKeyByKeyAlias( alias, -1 );
            } else {
                final SsgKeyFinder ssgKeyFinder = ssgKeyStoreManager.findByPrimaryKey( keyStoreId );
                ssgKeyEntry = ssgKeyFinder.getCertificateChain( alias );
            }
        } catch ( KeyStoreException e ) {
            throw new ResourceAccessException(e);
        } catch ( ObjectNotFoundException e ) {
            // handled below
        } 

        if ( ssgKeyEntry == null ) {
            throw new ResourceNotFoundException("Resource not found " + toExternalId(keyStoreId, alias));
        }

        checkPermitted( OperationType.READ, null, ssgKeyEntry );

        privateKey.setId( toExternalId(keyStoreId, alias) );
        privateKey.setKeystoreId( Long.toString( keyStoreId ) );
        privateKey.setAlias( alias );
        privateKey.setCertificateChain( buildCertificateChain( ssgKeyEntry ) );
        privateKey.setProperties( buildProperties( ssgKeyEntry ) );

        return privateKey;
    }

    private List<CertificateData> buildCertificateChain( final SsgKeyEntry ssgKeyEntry ) {
        List<CertificateData> data = new ArrayList<CertificateData>();

        for ( X509Certificate certificate : ssgKeyEntry.getCertificateChain() ) {
            try {
                data.add( ManagedObjectFactory.createCertificateData( certificate ) );
            } catch ( ManagedObjectFactory.FactoryException e ) {
                throw new ResourceAccessException(e);
            }
        }

        return data;
    }

    private Map<String, Object> buildProperties( final SsgKeyEntry ssgKeyEntry ) {
        Map<String,Object> properties = new HashMap<String,Object>();
        try {
            properties.put( "keyAlgorithm", ssgKeyEntry.getPrivateKey().getAlgorithm() );
        } catch ( UnrecoverableKeyException e ) {
            logger.log( Level.WARNING, "Error accessing private key '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );           
        }
        return properties;
    }
}
