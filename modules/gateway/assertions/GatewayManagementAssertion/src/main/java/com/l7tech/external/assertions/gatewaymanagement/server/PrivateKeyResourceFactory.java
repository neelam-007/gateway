package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.CertificateData;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PrivateKeyCreationContext;
import com.l7tech.gateway.api.impl.PrivateKeyExportContext;
import com.l7tech.gateway.api.impl.PrivateKeyExportResult;
import com.l7tech.gateway.api.impl.PrivateKeyGenerateCsrContext;
import com.l7tech.gateway.api.impl.PrivateKeyGenerateCsrResult;
import com.l7tech.gateway.api.impl.PrivateKeyImportContext;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.api.impl.PrivateKeySpecialPurposeContext;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.MultipleAliasesException;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyHeader;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.admin.PrivateKeyAdminHelper;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import static com.l7tech.util.CollectionUtils.foreach;
import com.l7tech.util.Config;
import com.l7tech.util.Either;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.lefts;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Either.rights;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.Functions.UnaryVoidThrows;
import static com.l7tech.util.Functions.map;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import static com.l7tech.util.TextUtils.join;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource factory for Private Keys.
 *
 * <p>Private keys are unlike other entities since they exist in a key store.
 * You cannot use the typical Create/Put methods to create and update private
 * keys, you have to use the custom create or import methods and once created
 * you can only replace the certificate chain.</p>
 */
@ResourceFactory.ResourceType(type=PrivateKeyMO.class)
public class PrivateKeyResourceFactory extends ResourceFactorySupport<PrivateKeyMO> {

    //- PUBLIC

    public PrivateKeyResourceFactory( final RbacServices rbacServices,
                                      final SecurityFilter securityFilter,
                                      final PlatformTransactionManager transactionManager,
                                      final Config config,
                                      final SsgKeyStoreManager ssgKeyStoreManager,
                                      final ClusterPropertyCache clusterPropertyCache,
                                      final ClusterPropertyManager clusterPropertyManager,
                                      final DefaultKey defaultKey,
                                      final ApplicationEventPublisher applicationEventPublisher ) {
        super( rbacServices, securityFilter, transactionManager );
        this.config = config;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.clusterPropertyCache = clusterPropertyCache;
        this.clusterPropertyManager = clusterPropertyManager;
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
                return Functions.map( getEntityHeaders(), new Functions.Unary<Map<String, String>, SsgKeyHeader>() {
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

        return transactional( new TransactionalCallback<PrivateKeyMO, ResourceFactoryException>() {
            @Override
            public PrivateKeyMO execute() throws ObjectModelException, ResourceFactoryException {
                final Pair<Long, String> keyId = getKeyId( selectorMap );
                final SsgKeyEntry ssgKeyEntry = getSsgKeyEntry( keyId );

                checkPermitted( OperationType.UPDATE, null, ssgKeyEntry );

                final List<CertificateData> certificateChain = privateKeyResource.getCertificateChain();
                if ( certificateChain == null || certificateChain.isEmpty() )
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.MISSING_VALUES, "certificate chain" );

                final X509Certificate[] certificates = toCertificateArray( certificateChain );
                try {
                    final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                    helper.doUpdateCertificateChain(
                            ssgKeyEntry.getKeystoreId(),
                            ssgKeyEntry.getAlias(),
                            certificates );
                } catch ( final Exception e ) {
                    throw new UpdateException( "Error setting new cert: " + ExceptionUtils.getMessage( e ), e );
                }

                return buildPrivateKeyResource( getSsgKeyEntry( keyId ) );
            }
        }, false, ResourceNotFoundException.class, InvalidResourceException.class );
    }

    @Override
    public String deleteResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return transactional( new TransactionalCallback<String, ResourceNotFoundException>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            public String execute() throws ObjectModelException, ResourceNotFoundException {
                final Pair<Long, String> keyId = getKeyId( selectorMap );
                final SsgKeyEntry ssgKeyEntry = getSsgKeyEntry( keyId );

                checkPermitted( OperationType.DELETE, null, ssgKeyEntry );

                final SsgKeyStore keystore = getSsgKeyStore( ssgKeyEntry.getKeystoreId() );
                try {
                    final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                    if ( helper.isKeyActive( keystore, ssgKeyEntry.getAlias() ) )
                        throw new ResourceFactory.ResourceAccessException( "Policy is invalid." );

                    helper.doDeletePrivateKeyEntry( keystore, ssgKeyEntry.getAlias() );
                } catch ( KeyStoreException e ) {
                    throw new DeleteException( "Error deleting key: " + ExceptionUtils.getMessage( e ), e );
                }

                return toExternalId( keyId.left, keyId.right );
            }
        }, false, ResourceNotFoundException.class );
    }

    @ResourceMethod(name="SetSpecialPurposes", resource=true, selectors=true)
    public PrivateKeyMO setSpecialPurposes( final Map<String,String> selectorMap,
                                            final PrivateKeySpecialPurposeContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        transactional( new TransactionalCallback<Void,ResourceFactoryException>(){
            @Override
            public Void execute() throws ObjectModelException, ResourceFactoryException {
                final SsgKeyEntry entry = getSsgKeyEntry( getKeyId( selectorMap ) );
                checkPermitted( OperationType.READ, null, entry );

                final List<String> specialPurposes = resource.getSpecialPurposes();
                final List<Either<String,SpecialKeyType>> processedPurposes = map( specialPurposes, new Unary<Either<String,SpecialKeyType>,String>(){
                    @Override
                    public Either<String,SpecialKeyType> call( final String specialPurpose ) {
                        try {
                            return right( EntityPropertiesHelper.getEnumValue( SpecialKeyType.class, specialPurpose ) );
                        } catch ( InvalidResourceException e ) {
                            return left( specialPurpose );
                        }
                    }
                } );
                final List<String> invalidSpecialPurposes = lefts( processedPurposes );
                if ( !invalidSpecialPurposes.isEmpty() ) {
                    throw new InvalidResourceException( ExceptionType.INVALID_VALUES, "Invalid special purpose(s): " + invalidSpecialPurposes );
                }
                foreach( rights( processedPurposes ), false, new UnaryVoidThrows<SpecialKeyType,ObjectModelException>(){
                    @Override
                    public void call( final SpecialKeyType specialKeyType ) throws ObjectModelException {
                        final String clusterPropertyName =
                                PrivateKeyAdminHelper.getClusterPropertyForSpecialKeyType( specialKeyType );
                        final Option<ClusterProperty> propertyOption = optional( clusterPropertyManager.findByUniqueName( clusterPropertyName ) );
                        final ClusterProperty property = propertyOption.orSome( new Nullary<ClusterProperty>(){
                            @Override
                            public ClusterProperty call() {
                                return new ClusterProperty( clusterPropertyName, null );
                            }
                        } );
                        saveOrUpdateClusterProperty( property, toClusterPropertyValue( entry.getKeystoreId(), entry.getAlias() ) );
                    }
                } );

                return null;
            }
        }, false, InvalidResourceException.class, ResourceNotFoundException.class );

        return getResource( selectorMap ); // get in new transaction to see updates
    }

    @ResourceMethod(name="GenerateCSR", resource=true, selectors=true)
    public PrivateKeyGenerateCsrResult generateCSR( final Map<String,String> selectorMap,
                                                    final PrivateKeyGenerateCsrContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return transactional( new TransactionalCallback<PrivateKeyGenerateCsrResult, ResourceFactoryException>() {
            @Override
            public PrivateKeyGenerateCsrResult execute() throws ObjectModelException, ResourceFactoryException {
                final SsgKeyEntry entry = getSsgKeyEntry( getKeyId( selectorMap ) );
                checkPermitted( OperationType.READ, null, entry );

                final byte[] csrData;
                final String dn = optional( resource.getDn() ).orSome( entry.getSubjectDN() );
                final X500Principal principal = new X500Principal( dn );
                try {
                    final SsgKeyStore ssgKeyStore = getSsgKeyStore( entry.getKeystoreId() );
                    final Map<String, Object> properties = resource.getProperties();
                    final Option<String> signatureHashAlgorithm = getProperty( properties, PrivateKeyGenerateCsrContext.PROP_SIGNATURE_HASH, Option.<String>none(), String.class );

                    final CertificateRequest res = ssgKeyStore.makeCertificateSigningRequest(
                            entry.getAlias(),
                            new CertGenParams(
                                    principal,
                                    config.getIntProperty( "pkix.csr.defaultExpiryAge", DEFAULT_CSR_EXPIRY_DAYS ),
                                    false,
                                    signatureHashAlgorithm.map( getSignatureAlgorithmMapper( entry.getPrivateKey().getAlgorithm() ) ).toNull() ) );
                    csrData = res.getEncoded();
                } catch ( SignatureException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( KeyStoreException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( InvalidKeyException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( UnrecoverableKeyException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                }

                final PrivateKeyGenerateCsrResult result = new PrivateKeyGenerateCsrResult();
                result.setCsrData( csrData );
                return result;
            }
        }, true, InvalidResourceException.class, ResourceNotFoundException.class );
    }

    @ResourceMethod(name="CreateKey", resource=true, selectors=true)
    public PrivateKeyMO createPrivateKey( final Map<String,String> selectorMap,
                                          final PrivateKeyCreationContext resource ) throws InvalidResourceException {
        return transactional( new TransactionalCallback<PrivateKeyMO,ResourceFactoryException>(){
            @Override
            public PrivateKeyMO execute() throws ObjectModelException, ResourceFactoryException {
                final Pair<Long,String> keyId = getKeyId( selectorMap );
                checkPermitted( OperationType.CREATE, null, null );

                final long keystoreId = keyId.left;
                final String alias = keyId.right;

                final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                final Map<String,Object> properties = resource.getProperties();
                final String dn = resource.getDn();
                final Option<String> curveName = getProperty( properties, PrivateKeyCreationContext.PROP_ELLIPTIC_CURVE_NAME, Option.<String>none(), String.class );
                final int keybits = getProperty( properties, PrivateKeyCreationContext.PROP_RSA_KEY_SIZE, some( DEFAULT_RSA_KEY_SIZE ), Integer.class).some();
                final int expiryDays = getProperty( properties, PrivateKeyCreationContext.PROP_DAYS_UNTIL_EXPIRY, some( DEFAULT_CERTIFICATE_EXPIRY_DAYS ), Integer.class ).some();
                final boolean makeCaCert = getProperty( properties, PrivateKeyCreationContext.PROP_CA_CAPABLE, some( false ), Boolean.class).some();
                final Option<String> signatureHashAlgorithm = getProperty( properties, PrivateKeyCreationContext.PROP_SIGNATURE_HASH, Option.<String>none(), String.class );

                try {
                    final KeyGenParams keyGenParams = curveName.isSome() ?
                            new KeyGenParams(curveName.some()) :
                            new KeyGenParams(keybits);

                    helper.doGenerateKeyPair(
                            keystoreId,
                            alias,
                            new X500Principal(dn, ValidationUtils.getOidKeywordMap()),
                            keyGenParams,
                            expiryDays,
                            makeCaCert,
                            signatureHashAlgorithm.map( getSignatureAlgorithmMapper( keyGenParams.getAlgorithm() ) ).toNull())
                            .get();
                } catch ( GeneralSecurityException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( InterruptedException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                } catch ( ExecutionException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                }

                return buildPrivateKeyResource( getSsgKeyEntry( keyId ) );
            }
        }, false, InvalidResourceException.class );
    }

    @ResourceMethod(name="ImportKey", resource=true, selectors=true)
    public PrivateKeyMO importPrivateKey( final Map<String,String> selectorMap,
                                          final PrivateKeyImportContext resource ) throws InvalidResourceException {
        return transactional( new TransactionalCallback<PrivateKeyMO,ResourceFactoryException>(){
            @Override
            public PrivateKeyMO execute() throws ObjectModelException, ResourceFactoryException {
                final Pair<Long,String> keyId = getKeyId( selectorMap );
                checkPermitted( OperationType.CREATE, null, null );

                final long keystoreId = keyId.left;
                final String alias = keyId.right;
                final byte[] pkcs12bytes = resource.getPkcs12Data();
                final char[] pkcs12pass = resource.getPassword().toCharArray();
                String pkcs12alias = resource.getAlias();

                try {
                    final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                    return buildPrivateKeyResource( helper.doImportKeyFromPkcs12(keystoreId, alias, pkcs12bytes, pkcs12pass, pkcs12alias) );
                } catch ( AliasNotFoundException e ) {
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Aliases not found : " + pkcs12alias );
                } catch ( MultipleAliasesException e ) {
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Alias must be specified : " + Arrays.asList( e.getAliases() ) );
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
                checkPermitted( OperationType.DELETE, null, entry );

                final char[] exportPassword = resource.getPassword().toCharArray();
                final String exportAlias = resource.getAlias()==null ? entry.getAlias() : resource.getAlias();

                final byte[] data;
                try {
                    final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                    data = helper.doExportKeyAsPkcs12(
                            entry.getKeystoreId(),
                            entry.getAlias(),
                            exportAlias,
                            exportPassword );
                } catch ( UnrecoverableKeyException e ) {
                    throw new ResourceNotFoundException("Private Key cannot be exported.");
                } catch ( KeyStoreException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage(e), e );
                }

                final PrivateKeyExportResult result = new PrivateKeyExportResult();
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
    private static final int DEFAULT_CERTIFICATE_EXPIRY_DAYS = SyspropUtil.getInteger( "com.l7tech.external.assertions.gatewaymanagement.defaultCertificateExpiryDays", 365 * 5 );
    private static final int DEFAULT_CSR_EXPIRY_DAYS = SyspropUtil.getInteger( "com.l7tech.external.assertions.gatewaymanagement.defaultCsrExpiryDays", 365 * 2);
    private static final int DEFAULT_RSA_KEY_SIZE = SyspropUtil.getInteger( "com.l7tech.external.assertions.gatewaymanagement.defaultRsaKeySize", 2048 );

    private final Config config;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final ClusterPropertyCache clusterPropertyCache;
    private final ClusterPropertyManager clusterPropertyManager;
    private final DefaultKey defaultKey;
    private final ApplicationEventPublisher applicationEventPublisher;

    private PrivateKeyAdminHelper getPrivateKeyAdminHelper() {
        return new PrivateKeyAdminHelper( defaultKey, ssgKeyStoreManager, applicationEventPublisher );
    }

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

    private Unary<String,String> getSignatureAlgorithmMapper( final String keyAlgorithm ) {
        return new Unary<String, String>(){
            @Override
            public String call( final String hashAlgorithm ) {
                return KeyGenParams.getSignatureAlgorithm( keyAlgorithm, hashAlgorithm );
            }
        };
    }

    private String toClusterPropertyValue( final long keystoreId,
                                           final String alias ) {
        // currently this is the same as the external identifier format
        return toExternalId( keystoreId, alias );
    }

    /**
     * Persist the cluster property with the given value with permission enforcement.
     */
    private void saveOrUpdateClusterProperty( final ClusterProperty property,
                                              final String updatedValue ) throws ObjectModelException {
        property.setValue( updatedValue );

        try {
            validate(property);
        } catch (InvalidResourceException e) {
            throw new ResourceAccessException( "Updated cluster property value is invalid", e);
        }

        if ( property.getOid() == PersistentEntity.DEFAULT_OID ) {
            checkPermitted( OperationType.CREATE, null, property );
            clusterPropertyManager.save(property);
        } else {
            checkPermitted( OperationType.UPDATE, null, property );
            clusterPropertyManager.update(property);
            property.getOid();
        }
    }

    private Map<String, Object> buildProperties( final SsgKeyEntry ssgKeyEntry ) {
        final Map<String,Object> properties = new HashMap<String,Object>();
        try {
            properties.put( "keyAlgorithm", ssgKeyEntry.getPrivateKey().getAlgorithm() );
        } catch ( UnrecoverableKeyException e ) {
            logger.log( Level.WARNING, "Error accessing private key '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );           
        }

        final List<SpecialKeyType> types = new ArrayList<SpecialKeyType>();
        for ( final SpecialKeyType keyType : SpecialKeyType.values() ) {
            final String propertyName = PrivateKeyAdminHelper.getClusterPropertyForSpecialKeyType( keyType );
            final Option<ClusterProperty> clusterProperty = optional( clusterPropertyCache.getCachedEntityByName( propertyName ) );
            final String externalId = toClusterPropertyValue( ssgKeyEntry.getKeystoreId(), ssgKeyEntry.getAlias() );
            clusterProperty.foreach( new UnaryVoid<ClusterProperty>() {
                @Override
                public void call( final ClusterProperty clusterProperty ) {
                    if( externalId.equalsIgnoreCase( clusterProperty.getValue() ) ) {
                        types.add( keyType );
                    }
                }
            } );
        }

        if ( !types.isEmpty() ) {
            properties.put( "specialPurposes", join( ",", Functions.<SpecialKeyType, String>map( types, EntityPropertiesHelper.getEnumText() ) ).toString() );
        }

        return properties;
    }

}
