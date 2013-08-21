package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.CertificateData;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PrivateKeyCreationContext;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.api.impl.*;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.MultipleAliasesException;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.KeyUsageUtils;
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
import com.l7tech.util.*;
import com.l7tech.util.Eithers.*;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.Functions.UnaryVoidThrows;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.gatewaymanagement.server.EntityPropertiesHelper.getEnumText;
import static com.l7tech.util.CollectionUtils.foreach;
import static com.l7tech.util.CollectionUtils.iterable;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.*;
import static com.l7tech.util.Functions.exists;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.join;
import static java.util.EnumSet.allOf;

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
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(IDENTITY_SELECTOR, NAME_SELECTOR)));
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
        return getResourceInternal( selectorMap, Option.<Collection<SpecialKeyType>>none() );
    }

    @Override
    public Collection<Map<String, String>> getResources() {
        return transactional( new TransactionalCallback<Collection<Map<String, String>>>(){
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

        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException,InvalidResourceException,PrivateKeyMO>>() {
            @Override
            public E2<ResourceNotFoundException,InvalidResourceException,PrivateKeyMO> execute() throws ObjectModelException {
                try {
                    final Pair<Goid, String> keyId = getKeyId( selectorMap );
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

                    return right2( buildPrivateKeyResource( getSsgKeyEntry( keyId ), Option.<Collection<SpecialKeyType>>none() ) );
                } catch ( ResourceNotFoundException e ) {
                    return left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return left2_2( e );
                }
            }
        }, false ) );
    }

    @Override
    public String deleteResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException, String>>() {
            @Override
            public Either<ResourceNotFoundException, String> execute() throws ObjectModelException {
                try {
                    final Pair<Goid, String> keyId = getKeyId( selectorMap );
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

                    return right( toExternalId( keyId.left, keyId.right ) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, false ) );
    }

    @ResourceMethod(name="SetSpecialPurposes", resource=true, selectors=true)
    public PrivateKeyMO setSpecialPurposes( final Map<String,String> selectorMap,
                                            final PrivateKeySpecialPurposeContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, PrivateKeyMO>>() {
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, PrivateKeyMO> execute() throws ObjectModelException {
                try {
                    final SsgKeyEntry entry = getSsgKeyEntry( getKeyId( selectorMap ) );
                    checkPermittedForAnyEntity( OperationType.DELETE, EntityType.SSG_KEY_ENTRY ); // constent with TrustedCertAdmin.setDefaultKey

                    final List<String> specialPurposes = resource.getSpecialPurposes();
                    final List<Either<String, SpecialKeyType>> processedPurposes = map( specialPurposes, new Unary<Either<String, SpecialKeyType>, String>() {
                        @Override
                        public Either<String, SpecialKeyType> call( final String specialPurpose ) {
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
                    final Collection<SpecialKeyType> assignedTypes = rights( processedPurposes );
                    foreach( assignedTypes, false, new UnaryVoidThrows<SpecialKeyType, InvalidResourceException>() {
                        @Override
                        public void call( final SpecialKeyType specialKeyType ) throws InvalidResourceException {
                            if ( specialKeyType == SpecialKeyType.SSL && !KeyUsageUtils.isCertSslCapable( entry.getCertificate() ) ) {
                                throw new InvalidResourceException( ExceptionType.INVALID_VALUES, "Invalid special purpose, Key not SSL capable: " + getEnumText( specialKeyType ) );
                            }
                        }
                    } );
                    final Collection<SpecialKeyType> currentTypes = new ArrayList<SpecialKeyType>();
                    final String externalId = toClusterPropertyValue( entry.getKeystoreId(), entry.getAlias() );
                    final Unary<Boolean,ClusterProperty> forThisKey = clusterPropertyValueMatchPredicate( externalId );
                    final Collection<ClusterProperty> forUpdate = new ArrayList<ClusterProperty>();
                    foreach( speciaKeysAndProperties(forUpdateLookup()), false, new UnaryVoidThrows<Triple<SpecialKeyType,String,Either<FindException,Option<ClusterProperty>>>, ObjectModelException>() {
                        @Override
                        public void call( final Triple<SpecialKeyType,String,Either<FindException,Option<ClusterProperty>>> keyAndProperty ) throws ObjectModelException {
                            final SpecialKeyType specialKeyType = keyAndProperty.left;
                            final Option<ClusterProperty> propertyOption = extract( keyAndProperty.right );
                            if ( propertyOption.exists( forThisKey ) ) {
                                currentTypes.add( specialKeyType );
                            } else if ( assignedTypes.contains( specialKeyType ) ) {
                                forUpdate.add( propertyOption.orSome( new Nullary<ClusterProperty>() {
                                    @Override
                                    public ClusterProperty call() {
                                        return new ClusterProperty( keyAndProperty.middle, null );
                                    }
                                } ) );
                            }
                        }
                    } );
                    @SuppressWarnings({ "unchecked" }) //TODO [jdk7] @SafeVarargs (remove unchecked)
                    final Iterable<SpecialKeyType> combinedSpecialKeyTypes = iterable( assignedTypes, currentTypes );
                    final boolean hasRestrictedKeyType = exists( combinedSpecialKeyTypes, new Unary<Boolean, SpecialKeyType>() {
                        @Override
                        public Boolean call( final SpecialKeyType specialKeyType ) {
                            return specialKeyType.isRestrictedAccess();
                        }
                    } );
                    if ( hasRestrictedKeyType && (assignedTypes.size()+currentTypes.size()) > 1 ) {
                        throw new InvalidResourceException(
                                ExceptionType.INVALID_VALUES,
                                "Invalid special purpose(s), incompatible assignments: " + map( combinedSpecialKeyTypes, getEnumText() ) );
                    }
                    foreach( forUpdate, false, new UnaryVoidThrows<ClusterProperty, ObjectModelException>() {
                        @Override
                        public void call( final ClusterProperty property ) throws ObjectModelException {
                            saveOrUpdateClusterProperty( property, externalId );
                        }
                    } );
                    return right2( getResourceInternal( selectorMap, some(assignedTypes) ) );
                } catch ( ResourceNotFoundException e ) {
                    return left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return left2_2( e );
                }
            }
        }, false ) );
    }

    @ResourceMethod(name="GenerateCSR", resource=true, selectors=true)
    public PrivateKeyGenerateCsrResult generateCSR( final Map<String,String> selectorMap,
                                                    final PrivateKeyGenerateCsrContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, PrivateKeyGenerateCsrResult>>() {
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, PrivateKeyGenerateCsrResult> execute() throws ObjectModelException {
                try {
                    final SsgKeyEntry entry = getSsgKeyEntry( getKeyId( selectorMap ) );
                    checkPermittedForAnyEntity( OperationType.UPDATE, EntityType.SSG_KEY_ENTRY );  // Consistent with TrustedCertAdmin.generateCSR

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
                    return right2( result );
                } catch ( ResourceNotFoundException e ) {
                    return left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return left2_2( e );
                }
            }
        }, true ) );
    }

    @ResourceMethod(name="CreateKey", resource=true, selectors=true)
    public PrivateKeyMO createPrivateKey( final Map<String,String> selectorMap,
                                          final PrivateKeyCreationContext resource ) throws InvalidResourceException, InvalidResourceSelectors {
        final Pair<Goid,String> keyId = getKeyId( selectorMap );
        return extract( transactional( new TransactionalCallback<Either<InvalidResourceException,PrivateKeyMO>>(){
            @Override
            public Either<InvalidResourceException,PrivateKeyMO> execute() throws ObjectModelException {
                try {
                    checkPermittedForSomeEntity( OperationType.CREATE, EntityType.SSG_KEY_ENTRY );

                    final Goid keystoreId = keyId.left;
                    final String alias = keyId.right;

                    final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                    final Map<String,Object> properties = resource.getProperties();
                    final String dn = resource.getDn();
                    final Option<String> curveName = getProperty( properties, PROP_ELLIPTIC_CURVE_NAME, Option.<String>none(), String.class );
                    final int keybits = getProperty( properties, PROP_RSA_KEY_SIZE, some( DEFAULT_RSA_KEY_SIZE ), Integer.class).some();
                    final int expiryDays = getProperty( properties, PROP_DAYS_UNTIL_EXPIRY, some( DEFAULT_CERTIFICATE_EXPIRY_DAYS ), Integer.class ).some();
                    final boolean makeCaCert = getProperty( properties, PROP_CA_CAPABLE, some( false ), Boolean.class).some();
                    final Option<String> signatureHashAlgorithm = getProperty( properties, PROP_SIGNATURE_HASH, Option.<String>none(), String.class );

                    try {
                        final KeyGenParams keyGenParams = curveName.isSome() ?
                                new KeyGenParams(curveName.some()) :
                                new KeyGenParams(keybits);

                        helper.doGenerateKeyPair(
                                keystoreId,
                                alias,
                                null, // TODO metadata for security zone
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

                    return right( buildPrivateKeyResource( getSsgKeyEntry( keyId ), Option.<Collection<SpecialKeyType>>none() ) );
                } catch ( ResourceNotFoundException e ) {
                    throw new ResourceAccessException( e ); // error since we just created the resource
                } catch ( InvalidResourceException e ) {
                    return left( e );
                }
            }
        }, false ) );
    }

    @ResourceMethod(name="ImportKey", resource=true, selectors=true)
    public PrivateKeyMO importPrivateKey( final Map<String,String> selectorMap,
                                          final PrivateKeyImportContext resource ) throws InvalidResourceException, InvalidResourceSelectors {
        final Pair<Goid,String> keyId = getKeyId( selectorMap );
        return extract( transactional( new TransactionalCallback<Either<InvalidResourceException, PrivateKeyMO>>() {
            @Override
            public Either<InvalidResourceException, PrivateKeyMO> execute() throws ObjectModelException {
                checkPermittedForSomeEntity( OperationType.CREATE, EntityType.SSG_KEY_ENTRY );

                final Goid keystoreId = keyId.left;
                final String alias = keyId.right;
                final byte[] pkcs12bytes = resource.getPkcs12Data();
                final char[] pkcs12pass = resource.getPassword().toCharArray();
                String pkcs12alias = resource.getAlias();

                try {
                    final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                    return right( buildPrivateKeyResource(
                            helper.doImportKeyFromKeyStoreFile(keystoreId, alias, null, pkcs12bytes, "PKCS12", pkcs12pass, pkcs12pass, pkcs12alias),  // TODO metadata for security zone
                            Option.<Collection<SpecialKeyType>>none() ) );
                } catch ( AliasNotFoundException e ) {
                    return left( new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Aliases not found : " + pkcs12alias ) );
                } catch ( MultipleAliasesException e ) {
                    return left( new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Alias must be specified : " + Arrays.asList( e.getAliases() ) ) );
                } catch ( IOException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( CertificateException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( NoSuchAlgorithmException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( UnrecoverableKeyException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( ExecutionException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( InterruptedException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( NoSuchProviderException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                } catch ( KeyStoreException e ) {
                    throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                }
            }
        }, false ) );
    }

    @ResourceMethod(name="ExportKey", resource=true, selectors=true)
    public PrivateKeyExportResult exportPrivateKey( final Map<String,String> selectorMap,
                                                    final PrivateKeyExportContext resource ) throws ResourceNotFoundException {
        final Pair<Goid,String> keyId = getKeyId( selectorMap );
        return extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException, PrivateKeyExportResult>>() {
            @Override
            public Either<ResourceNotFoundException, PrivateKeyExportResult> execute() throws ObjectModelException {
                try {
                    final SsgKeyEntry entry = getSsgKeyEntry( keyId );
                    checkPermittedForAnyEntity( OperationType.DELETE, EntityType.SSG_KEY_ENTRY ); // constent with TrustedCertAdmin.exportKey

                    final char[] exportPassword = resource.getPassword().toCharArray();
                    final String exportAlias = resource.getAlias() == null ? entry.getAlias() : resource.getAlias();

                    final byte[] data;
                    try {
                        final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                        data = helper.doExportKeyAsPkcs12(
                                entry.getKeystoreId(),
                                entry.getAlias(),
                                exportAlias,
                                exportPassword );
                    } catch ( UnrecoverableKeyException e ) {
                        throw new ResourceNotFoundException( "Private Key cannot be exported." );
                    } catch ( KeyStoreException e ) {
                        throw new ResourceAccessException( ExceptionUtils.getMessage( e ), e );
                    }

                    final PrivateKeyExportResult result = new PrivateKeyExportResult();
                    result.setPkcs12Data( data );
                    return right( result );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, false ) );
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
    static <E extends Exception> Pair<Goid,String>  toInternalId( final String identifier ,
                                                                  final Functions.NullaryVoidThrows<E> validationThrower ) throws E {
        return toInternalId(Collections.<String,String>emptyMap().put(IDENTITY_SELECTOR,identifier), validationThrower);
    }

    static <E extends Exception> Pair<Goid,String>  toInternalId( final Map<String,String> selectorMap ,
                                                                  final Functions.NullaryVoidThrows<E> validationThrower ) throws E {
        String identifier = selectorMap.get(NAME_SELECTOR);
        Goid keyStoreId;
        final String alias;
        if ( identifier == null ){
            identifier = selectorMap.get(IDENTITY_SELECTOR);
            if ( identifier == null ) validationThrower.call();
            final String[] keystoreAndAlias = identifier.split( ":" );
            if ( keystoreAndAlias.length!=2 ) {
                validationThrower.call();
            }
            try {
                keyStoreId = GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keystoreAndAlias[0]);
            } catch ( NumberFormatException nfe ) {
                validationThrower.call();
                keyStoreId = PersistentEntity.DEFAULT_GOID;
            }
            alias = keystoreAndAlias[1];
        }else{
            keyStoreId = PersistentEntity.DEFAULT_GOID;
            alias = identifier;
        }

        return new Pair<Goid,String>( keyStoreId, alias );
    }

    static String toExternalId( final Goid keyStoreId, final String alias ) {
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
    private static final String NAME_SELECTOR = "name";
    private static final int DEFAULT_CERTIFICATE_EXPIRY_DAYS = ConfigFactory.getIntProperty( "com.l7tech.external.assertions.gatewaymanagement.defaultCertificateExpiryDays", 365 * 5 );
    private static final int DEFAULT_CSR_EXPIRY_DAYS = ConfigFactory.getIntProperty( "com.l7tech.external.assertions.gatewaymanagement.defaultCsrExpiryDays", 365 * 2 );
    private static final int DEFAULT_RSA_KEY_SIZE = ConfigFactory.getIntProperty( "com.l7tech.external.assertions.gatewaymanagement.defaultRsaKeySize", 2048 );

    private static final String PROP_CA_CAPABLE = "caCapable";
    private static final String PROP_DAYS_UNTIL_EXPIRY = "daysUntilExpiry";
    private static final String PROP_ELLIPTIC_CURVE_NAME = "ecName";
    private static final String PROP_RSA_KEY_SIZE = "rsaKeySize";
    private static final String PROP_SIGNATURE_HASH = "signatureHashAlgorithm";

    private final Config config;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final ClusterPropertyCache clusterPropertyCache;
    private final ClusterPropertyManager clusterPropertyManager;
    private final DefaultKey defaultKey;
    private final ApplicationEventPublisher applicationEventPublisher;

    private PrivateKeyAdminHelper getPrivateKeyAdminHelper() {
        return new PrivateKeyAdminHelper( defaultKey, ssgKeyStoreManager, applicationEventPublisher );
    }

    private Pair<Goid,String> getKeyId( final Map<String,String> selectorMap ) throws InvalidResourceSelectors {
        return toInternalId( selectorMap, SELECTOR_THROWER );
    }

    private PrivateKeyMO getResourceInternal( final Map<String, String> selectorMap,
                                              final Option<Collection<SpecialKeyType>> keyTypes ) throws ResourceNotFoundException {
        return extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException,PrivateKeyMO>>(){
            @Override
            public Either<ResourceNotFoundException,PrivateKeyMO> execute() throws ObjectModelException {
                try {
                    final Pair<Goid,String> keyId = getKeyId( selectorMap );
                    final SsgKeyEntry ssgKeyEntry = getSsgKeyEntry( keyId );

                    checkPermitted( OperationType.READ, null, ssgKeyEntry );

                    return right( buildPrivateKeyResource( ssgKeyEntry, keyTypes ) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, true ) );
    }

    private Collection<SsgKeyHeader> getEntityHeaders() throws FindException {
        final Collection<SsgKeyHeader> headers = new ArrayList<SsgKeyHeader>();

        try {
            for ( final SsgKeyFinder ssgKeyFinder : ssgKeyStoreManager.findAll() ) {
                for ( final String alias : ssgKeyFinder.getAliases() ) {
                    headers.add( new SsgKeyHeader( toExternalId(ssgKeyFinder.getGoid(), alias), ssgKeyFinder.getGoid(), alias, alias ) );
                }
            }
        } catch ( KeyStoreException e ) {
            throw new ResourceAccessException(e);
        } catch ( FindException e ) {
            throw new ResourceAccessException(e);
        }

        return accessFilter(headers, EntityType.SSG_KEY_ENTRY, OperationType.READ, null);
    }

    private PrivateKeyMO buildPrivateKeyResource( final SsgKeyEntry ssgKeyEntry,
                                                  final Option<Collection<SpecialKeyType>> keyTypes ) {
        final PrivateKeyMO privateKey = ManagedObjectFactory.createPrivateKey();

        privateKey.setId( toExternalId( ssgKeyEntry.getKeystoreId(), ssgKeyEntry.getAlias() ) );
        privateKey.setKeystoreId( Goid.toString( ssgKeyEntry.getKeystoreId() ) );
        privateKey.setAlias( ssgKeyEntry.getAlias() );
        privateKey.setCertificateChain( buildCertificateChain( ssgKeyEntry ) );
        privateKey.setProperties( buildProperties( ssgKeyEntry, keyTypes ) );

        return privateKey;
    }

    private SsgKeyStore getSsgKeyStore( final Goid keystoreId ) throws ObjectModelException {
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

    private SsgKeyEntry getSsgKeyEntry( final Pair<Goid,String> keyId ) throws FindException, ResourceNotFoundException {
        final Goid keyStoreId = keyId.left;
        final String alias = keyId.right;

        SsgKeyEntry ssgKeyEntry = null;
        try {
            if ( Goid.isDefault(keyStoreId) ) {
                ssgKeyEntry = ssgKeyStoreManager.lookupKeyByKeyAlias( alias, PersistentEntity.DEFAULT_GOID );
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

    private String toClusterPropertyValue( final Goid keystoreId,
                                           final String alias ) {
        // currently this is the same as the external identifier format
        return toExternalId( keystoreId, alias );
    }

    private Unary<Boolean,ClusterProperty> clusterPropertyValueMatchPredicate( final String value ) {
        return new Unary<Boolean,ClusterProperty>(){
            @Override
            public Boolean call( final ClusterProperty clusterProperty ) {
                return value.equalsIgnoreCase( clusterProperty.getValue() );
            }
        };
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

        if ( PersistentEntity.DEFAULT_GOID.equals(property.getGoid()) ) {
            checkPermitted( OperationType.CREATE, null, property );
            clusterPropertyManager.save(property);
        } else {
            checkPermitted( OperationType.UPDATE, null, property );
            clusterPropertyManager.update(property);
        }
    }

    private Map<String, Object> buildProperties( final SsgKeyEntry ssgKeyEntry,
                                                 final Option<Collection<SpecialKeyType>> keyTypes ) {
        final Map<String,Object> properties = new HashMap<String,Object>();
        optional( ssgKeyEntry.getPublic() ).foreach( new UnaryVoid<PublicKey>() {
            @Override
            public void call( final PublicKey publicKey ) {
                properties.put( "keyAlgorithm", publicKey.getAlgorithm() );
            }
        } );

        final Collection<SpecialKeyType> types = new LinkedHashSet<SpecialKeyType>();
        final String externalId = toClusterPropertyValue( ssgKeyEntry.getKeystoreId(), ssgKeyEntry.getAlias() );
        final Unary<Boolean,ClusterProperty> forThisKey = clusterPropertyValueMatchPredicate( externalId );
        for ( final Triple<SpecialKeyType,String,Option<ClusterProperty>> keyAndProperty : speciaKeysAndProperties(cacheLookup()) ) {
            if ( keyAndProperty.right.exists( forThisKey ) ) {
                types.add( keyAndProperty.left );
            }
        }

        if ( keyTypes.isSome() ) {
            types.addAll( keyTypes.some() );
        }

        if ( !types.isEmpty() ) {
            properties.put( "specialPurposes", join( ",", Functions.<SpecialKeyType, String>map( types, EntityPropertiesHelper.getEnumText() ) ).toString() );
        }

        return properties;
    }

    private Unary<Either<FindException,Option<ClusterProperty>>,String> forUpdateLookup() {
        return new Unary<Either<FindException,Option<ClusterProperty>>,String>(){
            @Override
            public Either<FindException,Option<ClusterProperty>> call( final String propertyName ) {
                try {
                    return right( optional( clusterPropertyManager.findByUniqueName( propertyName ) ) );
                } catch ( FindException e ) {
                    return left( e );
                }
            }
        };
    }

    private Unary<Option<ClusterProperty>,String> cacheLookup() {
        return new Unary<Option<ClusterProperty>,String>(){
            @Override
            public Option<ClusterProperty> call( final String propertyName ) {
                return optional( clusterPropertyCache.getCachedEntityByName( propertyName ) );
            }
        };
    }

    private <T> Collection<Triple<SpecialKeyType,String,T>> speciaKeysAndProperties( final Unary<T,String> lookup ) {
        return map( allOf( SpecialKeyType.class ), new Unary<Triple<SpecialKeyType,String,T>,SpecialKeyType>(){
            @Override
            public Triple<SpecialKeyType, String,T> call( final SpecialKeyType specialKeyType ) {
                final String propertyName = PrivateKeyAdminHelper.getClusterPropertyForSpecialKeyType( specialKeyType );
                return new Triple<SpecialKeyType, String, T>(
                        specialKeyType,
                        propertyName,
                        lookup.call( propertyName )
                );
            }
        } );
    }

}
