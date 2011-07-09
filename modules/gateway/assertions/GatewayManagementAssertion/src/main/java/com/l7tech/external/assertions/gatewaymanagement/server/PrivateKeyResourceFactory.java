package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.api.CertificateData;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PrivateKeyExportContext;
import com.l7tech.gateway.api.PrivateKeyExportResult;
import com.l7tech.gateway.api.PrivateKeyImportContext;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyHeader;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.KeyExportedEvent;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.CallableRunnable;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource factory for Private Keys.
 *
 * <p>Private keys are unlike other entities since they exist in a key store.
 * You cannot use the typical Create/Put methods to create and update private
 * keys, you have to use the custom create or import methods and once created
 * you can only replace the certificate chain.</p>
 *
 * TODO [steve] implement key generation methods?
 * TODO [steve] refactor common parts of this factory and TrustedCertAdminImpl
 */
@ResourceFactory.ResourceType(type=PrivateKeyMO.class)
public class PrivateKeyResourceFactory extends ResourceFactorySupport<PrivateKeyMO> {

    //- PUBLIC

    /**
     * Provider for parsing PKCS#12 on import.  Values:
     *   "default" to use the system current most-preferred implementation of KeyStore.PKCS12;  "BC" to use
     *   Bouncy Castle's implementation (note that Bouncy Castle need not be registered as a Security provider
     *   for this to work); or else the name of any registered Security provider that offers KeyStore.PKCS12.
     */
    public static final String PROP_PKCS12_PARSING_PROVIDER = "com.l7tech.keyStore.pkcs12.parsing.provider";

    public PrivateKeyResourceFactory( final RbacServices rbacServices,
                                      final SecurityFilter securityFilter,
                                      final PlatformTransactionManager transactionManager,
                                      final SsgKeyStoreManager ssgKeyStoreManager,
                                      final DefaultKey defaultKey,
                                      final ApplicationEventPublisher applicationEventPublisher ) {
        super( rbacServices, securityFilter, transactionManager );
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.defaultKey = defaultKey;
        this.applicationEventPublisher = applicationEventPublisher;
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
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isCreateSupported() {
        return false; // custom methods are used for key creation
    }

    @Override
    public PrivateKeyMO getResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return transactional( new TransactionalCallback<PrivateKeyMO,ResourceNotFoundException>(){
            @Override
            public PrivateKeyMO execute() throws ObjectModelException, ResourceNotFoundException {
                final Pair<Long,String> keyId = getKeyId( selectorMap );
                final SsgKeyEntry ssgKeyEntry = getSsgKeyEntry( keyId );

                checkPermitted( OperationType.READ, null, ssgKeyEntry );

                return buildPrivateKeyResource( ssgKeyEntry );
            }
        }, true, ResourceNotFoundException.class );
    }

    @Override
    public Collection<Map<String, String>> getResources() {
        return transactional( new TransactionalCallback<Collection<Map<String, String>>, ObjectModelException>(){
            @Override
            public Collection<Map<String, String>> execute() throws ObjectModelException {
                return Functions.map( getEntityHeaders(), new Functions.Unary<Map<String,String>,SsgKeyHeader>(){
                    @Override
                    public Map<String, String> call( final SsgKeyHeader header ) {
                        return Collections.singletonMap( IDENTITY_SELECTOR, header.getStrId() );
                    }
                } );
            }
        }, true );
    }

    @Override
    public PrivateKeyMO putResource( final Map<String, String> selectorMap,
                                     final Object resource ) throws ResourceNotFoundException, InvalidResourceException {
        if ( !(resource instanceof PrivateKeyMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected private key");

        final PrivateKeyMO privateKeyResource = (PrivateKeyMO) resource;

        return transactional( new TransactionalCallback<PrivateKeyMO, ResourceFactoryException>(){
            @Override
            public PrivateKeyMO execute() throws ObjectModelException, ResourceFactoryException {
                final Pair<Long,String> keyId = getKeyId( selectorMap );
                final SsgKeyEntry ssgKeyEntry = getSsgKeyEntry( keyId );

                checkPermitted( OperationType.UPDATE, null, ssgKeyEntry );

                final List<CertificateData> certificateChain = privateKeyResource.getCertificateChain();
                if ( certificateChain == null || certificateChain.isEmpty() )
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "certificate chain");

                final X509Certificate[] certificates = toCertificateArray( certificateChain );

                final SsgKeyStore keystore = getSsgKeyStore( ssgKeyEntry.getKeystoreId() );
                try {
                    final Future<Boolean> future =
                            keystore.replaceCertificateChain(auditAfterUpdate(keyId, "certificateChain", "replaced"), keyId.right, certificates);
                    future.get();
                } catch ( final ExecutionException e ) {
                    final Throwable t = e.getCause()!=null ? e.getCause() : e;
                    throw new UpdateException("Error setting new cert: " + ExceptionUtils.getMessage(t), t);
                } catch ( final Exception e) {
                    throw new UpdateException("Error setting new cert: " + ExceptionUtils.getMessage(e), e);
                }

                return buildPrivateKeyResource( getSsgKeyEntry( keyId ) );
            }
        }, false, ResourceNotFoundException.class, InvalidResourceException.class );
    }

    @Override
    public String deleteResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return transactional( new TransactionalCallback<String,ResourceNotFoundException>(){
            @SuppressWarnings({ "unchecked" })
            @Override
            public String execute() throws ObjectModelException, ResourceNotFoundException {
                final Pair<Long,String> keyId = getKeyId( selectorMap );
                final SsgKeyEntry ssgKeyEntry = getSsgKeyEntry( keyId );

                checkPermitted( OperationType.DELETE, null, ssgKeyEntry );

                final SsgKeyStore keystore = getSsgKeyStore( ssgKeyEntry.getKeystoreId() );
                try {
                    if ( isKeyActive(keystore, ssgKeyEntry.getAlias() ) )
                        throw new ResourceFactory.ResourceAccessException("Policy is invalid.");

                    Future<Boolean> result = keystore.deletePrivateKeyEntry(auditAfterDelete(keystore, ssgKeyEntry.getAlias()), ssgKeyEntry.getAlias());
                    result.get();
                } catch (Exception e) {
                    throw new DeleteException("Error deleting key: " + ExceptionUtils.getMessage(e), e);
                }

                return toExternalId( keyId.left, keyId.right );
            }
        }, false, ResourceNotFoundException.class);
    }

    @ResourceMethod(name="ImportKey", resource=true, selectors=true)
    public PrivateKeyMO importPrivateKey( final Map<String,String> selectorMap,
                                          final PrivateKeyImportContext resource ) throws InvalidResourceException {
        return transactional( new TransactionalCallback<PrivateKeyMO,ResourceFactoryException>(){
            @Override
            public PrivateKeyMO execute() throws ObjectModelException, ResourceFactoryException {
                final Pair<Long,String> keyId = getKeyId( selectorMap );
                checkPermitted( OperationType.CREATE, null, null );

                final String alias = keyId.right;
                final byte[] pkcs12bytes = resource.getPkcs12Data();
                final char[] pkcs12pass = resource.getPassword().toCharArray();
                String pkcs12alias = resource.getAlias();

                try {
                    KeyStore inks = createKeyStoreForParsingPkcs12();
                    inks.load(new ByteArrayInputStream(pkcs12bytes), pkcs12pass);

                    if (pkcs12alias == null) {
                        List<String> aliases = new ArrayList<String>(Collections.list(inks.aliases()));
                        if (aliases.isEmpty())
                            throw new AliasNotFoundException("PKCS#12 file contains no private key entries");
                        if (aliases.size() > 1) {
                            // Retain private keys and filter out those certificates.
                            for ( Iterator<String> itr = aliases.iterator(); itr.hasNext();) {
                                if (! inks.isKeyEntry(itr.next())) {
                                    itr.remove();
                                }
                            }
                            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Multiple aliases - " + aliases );
                        }
                        pkcs12alias = aliases.iterator().next();
                    }

                    Certificate[] chain = inks.getCertificateChain(pkcs12alias);
                    Key key = inks.getKey(pkcs12alias, pkcs12pass);
                    if (chain == null || key == null)
                        throw new AliasNotFoundException("alias not found in PKCS#12 file: " + pkcs12alias);

                    X509Certificate[] x509chain = CertUtils.asX509CertificateArray(chain);
                    if (!(key instanceof PrivateKey))
                        throw new KeyStoreException("Key entry is not a PrivateKey: " + key.getClass());

                    SsgKeyStore keystore = getSsgKeyStore(keyId.left);
                    SsgKeyEntry entry = new SsgKeyEntry(keystore.getOid(), alias, x509chain, (PrivateKey)key);
                    Future<Boolean> future = keystore.storePrivateKeyEntry(auditAfterCreate(keystore, alias, "imported"), entry, false);
                    if (!future.get())
                        throw new KeyStoreException("Import operation returned false"); // can't happen

                    return buildPrivateKeyResource( keystore.getCertificateChain(alias) );
                } catch ( AliasNotFoundException e ) {
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Aliases not found : " + pkcs12alias );
                } catch ( IOException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( CertificateException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( NoSuchAlgorithmException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( UnrecoverableKeyException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( ExecutionException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( InterruptedException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( NoSuchProviderException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( KeyStoreException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                }
            }
        }, false, InvalidResourceException.class );
    }

    @ResourceMethod(name="ExportKey", resource=true, selectors=true)
    public PrivateKeyExportResult exportPrivateKey( final Map<String,String> selectorMap,
                                                    final PrivateKeyExportContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return transactional( new TransactionalCallback<PrivateKeyExportResult,ResourceFactoryException>(){
            @Override
            public PrivateKeyExportResult execute() throws ObjectModelException, ResourceFactoryException {
                final SsgKeyEntry entry = getSsgKeyEntry( getKeyId( selectorMap ) );
                checkPermitted( OperationType.DELETE, null, entry ); //TODO [steve] permission for export?

                if ( !entry.isPrivateKeyAvailable() )
                    throw new ResourceNotFoundException("Private Key cannot be exported.");

                final byte[] data;
                final PoolByteArrayOutputStream outputStream = new PoolByteArrayOutputStream();
                try {
                    final PrivateKey privateKey = entry.getPrivateKey();
                    final Certificate[] certChain = entry.getCertificateChain();

                    final char[] exportPassword = resource.getPassword().toCharArray();
                    final String exportAlias = resource.getAlias()==null ? entry.getAlias() : resource.getAlias();

                    final KeyStore keystore = KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
                    keystore.load(null, exportPassword);
                    keystore.setKeyEntry(exportAlias, privateKey, exportPassword, certChain);
                    keystore.store(outputStream, exportPassword);
                    data = outputStream.toByteArray();
                    applicationEventPublisher.publishEvent(new KeyExportedEvent(this, entry.getKeystoreId(), entry.getAlias(), getSubjectDN(entry)));
                } catch ( IOException e) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( NoSuchAlgorithmException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( CertificateException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( UnrecoverableKeyException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( KeyStoreException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } finally {
                    outputStream.close();
                }

                final PrivateKeyExportResult result = ManagedObjectFactory.createPrivateKeyExportResult();
                result.setPkcs12Data( data );
                return result;
            }
        }, false, InvalidResourceException.class, ResourceNotFoundException.class );
    }

    //- PACKAGE

    /**
     * Convert the given key identifier to the internal Pair<Long,String> format.
     *
     * @param identifier The identifier to process.
     * @param validationThrower Function that throws an exception when called.
     * @return The identifier pair
     * @throws E If the given identifier is not valid
     */
    static <E extends Exception> Pair<Long,String>  toInternalId( final String identifier,
                                                                  final Functions.NullaryVoidThrows<E> validationThrower ) throws E {
        if ( identifier == null ) validationThrower.call();

        final String[] keystoreAndAlias = identifier.split( ":" );
        if ( keystoreAndAlias.length!=2 ) {
            validationThrower.call();
        }

        long keyStoreId;
        try {
            keyStoreId = Long.parseLong(keystoreAndAlias[0]);
        } catch ( NumberFormatException nfe ) {
            validationThrower.call();
            keyStoreId = -1L;
        }
        final String alias = keystoreAndAlias[1];

        return new Pair<Long,String>( keyStoreId, alias );
    }

    static String toExternalId( final long keyStoreId, final String alias ) {
        return keyStoreId + ":" + alias;
    }

    static final Functions.NullaryVoidThrows<InvalidResourceSelectors> SELECTOR_THROWER = new Functions.NullaryVoidThrows<InvalidResourceSelectors>(){
        @Override
        public void call() throws InvalidResourceSelectors {
            throw new InvalidResourceSelectors();
        }
    };

    static final Functions.NullaryVoidThrows<InvalidResourceException> INVALIDRESOURCE_THROWER = new Functions.NullaryVoidThrows<InvalidResourceException>(){
        @Override
        public void call() throws InvalidResourceException {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid private key identifier");
        }
    };

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PrivateKeyResourceFactory.class.getName() );

    private static final String IDENTITY_SELECTOR = "id";

    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final DefaultKey defaultKey;
    private final ApplicationEventPublisher applicationEventPublisher;

    private Pair<Long,String> getKeyId( final Map<String,String> selectorMap ) throws InvalidResourceSelectors {
        return toInternalId( selectorMap.get( IDENTITY_SELECTOR ), SELECTOR_THROWER );
    }

    private Collection<SsgKeyHeader> getEntityHeaders() throws FindException {
        final Collection<SsgKeyHeader> headers = new ArrayList<SsgKeyHeader>();

        try {
            for ( final SsgKeyFinder ssgKeyFinder : ssgKeyStoreManager.findAll() ) {
                for ( final String alias : ssgKeyFinder.getAliases() ) {
                    headers.add( new SsgKeyHeader( toExternalId(ssgKeyFinder.getOid(), alias), ssgKeyFinder.getOid(), alias, alias ) );
                }
            }
        } catch ( KeyStoreException e ) {
            throw new ResourceAccessException(e);
        } catch ( FindException e ) {
            throw new ResourceAccessException(e);
        }

        return accessFilter(headers, EntityType.SSG_KEY_ENTRY, OperationType.READ, null);
    }

    private PrivateKeyMO buildPrivateKeyResource( final SsgKeyEntry ssgKeyEntry ) {
        final PrivateKeyMO privateKey = ManagedObjectFactory.createPrivateKey();

        privateKey.setId( toExternalId( ssgKeyEntry.getKeystoreId(), ssgKeyEntry.getAlias() ) );
        privateKey.setKeystoreId( Long.toString( ssgKeyEntry.getKeystoreId() ) );
        privateKey.setAlias( ssgKeyEntry.getAlias() );
        privateKey.setCertificateChain( buildCertificateChain( ssgKeyEntry ) );
        privateKey.setProperties( buildProperties( ssgKeyEntry ) );

        return privateKey;
    }

    private SsgKeyStore getSsgKeyStore( final long keystoreId ) throws ObjectModelException {
        final SsgKeyFinder ssgKeyFinder;
        try {
            ssgKeyFinder = ssgKeyStoreManager.findByPrimaryKey( keystoreId );
        } catch ( KeyStoreException e ) {
            logger.log( Level.WARNING, "Error accessing keystore '" + ExceptionUtils.getMessage(e) + "'", ExceptionUtils.getDebugException(e) );
            throw new ResourceAccessException( "Key access error: " + ExceptionUtils.getMessage(e) );
        }
        final SsgKeyStore keystore = ssgKeyFinder.getKeyStore();
        if (keystore == null)
            throw new UpdateException("error: keystore ID " + keystoreId + " is read-only");
        return keystore;
    }

    private SsgKeyEntry getSsgKeyEntry( final Pair<Long,String> keyId ) throws FindException, ResourceNotFoundException {
        final long keyStoreId = keyId.left;
        final String alias = keyId.right;

        SsgKeyEntry ssgKeyEntry = null;
        try {
            if ( keyStoreId == -1L ) {
                ssgKeyEntry = ssgKeyStoreManager.lookupKeyByKeyAlias( alias, -1L );
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
        } else {
            EntityContext.setEntityInfo( getType(), toExternalId(keyStoreId, alias) );
        }

        return ssgKeyEntry;
    }

    private X509Certificate[] toCertificateArray( final List<CertificateData> certificateChain ) throws InvalidResourceException {
        List<X509Certificate> certificates = new ArrayList<X509Certificate>();

        for ( final CertificateData certificateData : certificateChain ) {
            certificates.add( getCertificate( certificateData ) );
        }

        return certificates.toArray( new X509Certificate[ certificates.size() ] );
    }

    private X509Certificate getCertificate( final CertificateData certificateData ) throws InvalidResourceException {
        if ( certificateData == null || certificateData.getEncoded().length == 0 ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "encoded certificate data");
        }

        final X509Certificate x509Certificate;
        try {
            final Certificate certificate = CertUtils.getFactory().generateCertificate(
                    new ByteArrayInputStream( certificateData.getEncoded() ) );

            if ( !(certificate instanceof X509Certificate) )
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "unexpected encoded certificate type");

            x509Certificate = (X509Certificate) certificate;
        } catch ( CertificateException e ) {
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "encoded certificate error: " + ExceptionUtils.getMessage(e));
        }

        return x509Certificate;
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

    private boolean isKeyActive(SsgKeyFinder store, String keyAlias) throws KeyStoreException {
        HttpServletRequest req = RemoteUtils.getHttpServletRequest();
        if (null == req)
            return false;
        SsgConnector connector = HttpTransportModule.getConnector(req);
        if (null == connector)
            return false;
        if (connector.getKeyAlias() != null && !keyAlias.equalsIgnoreCase(connector.getKeyAlias()))
            return false;
        Long portStoreOid = connector.getKeystoreOid();
        if (portStoreOid != null && portStoreOid != store.getOid())
            return false;

        final SsgKeyEntry entry;
        try {
            entry = store.getCertificateChain(keyAlias);
        } catch (ObjectNotFoundException e) {
            return false;
        }

        try {
            SsgKeyEntry portEntry = findKeyEntry(connector.getKeyAlias(), portStoreOid != null ? portStoreOid : -1L );
            return CertUtils.certsAreEqual(portEntry.getCertificate(), entry.getCertificate());
        } catch (FindException e) {
            return false;
        }
    }

    private SsgKeyEntry findKeyEntry(String keyAlias, long preferredKeystoreOid) throws FindException, KeyStoreException {
        try {
            return keyAlias == null ? defaultKey.getSslInfo() : ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, preferredKeystoreOid);
        } catch ( IOException e) {
            // No default SSL key
            return null;
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    private String getSubjectDN(SsgKeyEntry entry) {
        X509Certificate cert = entry.getCertificate();
        if (cert == null) return null;
        return cert.getSubjectDN().getName();
    }

    private KeyStore createKeyStoreForParsingPkcs12() throws KeyStoreException, NoSuchProviderException {
        String p = SyspropUtil.getString(PROP_PKCS12_PARSING_PROVIDER, "BC");
        if (null == p || p.length() < 1 || p.equalsIgnoreCase("default"))
            return KeyStore.getInstance("PKCS12");
        if ("BC".equalsIgnoreCase(p))
            return KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
        return KeyStore.getInstance("PKCS12", p);
    }

    private Runnable auditAfterCreate(SsgKeyStore keystore, String alias, String note) {
        return publisher(new Created<SsgKeyEntry>(SsgKeyEntry.createDummyEntityForAuditing(keystore.getOid(), alias), note));
    }

    private Runnable auditAfterUpdate( final Pair<Long,String> keyId,
                                       final String property,
                                       final String note ) {
        EntityChangeSet changeset = new EntityChangeSet(new String[] {property}, new Object[] {new Object()}, new Object[] {new Object()});
        return publisher(new Updated<SsgKeyEntry>(SsgKeyEntry.createDummyEntityForAuditing(keyId.left, keyId.right), changeset, note));
    }

    private Runnable auditAfterDelete(SsgKeyStore keystore, String alias) {
        return publisher(new Deleted<SsgKeyEntry>(SsgKeyEntry.createDummyEntityForAuditing(keystore.getOid(), alias)));
    }

    private Runnable publisher(final ApplicationEvent event) {
        return new CallableRunnable<Object>( AdminInfo.find(true).wrapCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                if ( applicationEventPublisher != null ) applicationEventPublisher.publishEvent(event);
                return null;
            }
        }));
    }


}
