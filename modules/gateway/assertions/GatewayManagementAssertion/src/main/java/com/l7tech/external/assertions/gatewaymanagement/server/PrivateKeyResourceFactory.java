package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.*;
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
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.KeyUsageUtils;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.admin.PrivateKeyAdminHelper;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.*;
import com.l7tech.util.Eithers.*;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.Functions.UnaryVoidThrows;
import org.apache.commons.lang.NotImplementedException;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
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
public class PrivateKeyResourceFactory extends ResourceFactorySupport<PrivateKeyMO, SsgKeyEntry> {

    //- PUBLIC

    public PrivateKeyResourceFactory( final RbacServices rbacServices,
                                      final SecurityFilter securityFilter,
                                      final PlatformTransactionManager transactionManager,
                                      final Config config,
                                      final SsgKeyStoreManager ssgKeyStoreManager,
                                      final ClusterPropertyCache clusterPropertyCache,
                                      final ClusterPropertyManager clusterPropertyManager,
                                      final DefaultKey defaultKey,
                                      final ApplicationEventPublisher applicationEventPublisher,
                                      final SecurityZoneManager securityZoneManager ) {
        super( rbacServices, securityFilter, transactionManager );
        this.config = config;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.clusterPropertyCache = clusterPropertyCache;
        this.clusterPropertyManager = clusterPropertyManager;
        this.defaultKey = defaultKey;
        this.applicationEventPublisher = applicationEventPublisher;
        this.securityZoneManager = securityZoneManager;

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
                return Functions.map( getEntityHeaders(null), new Functions.Unary<Map<String, String>, SsgKeyHeader>() {
                    @Override
                    public Map<String, String> call( final SsgKeyHeader header ) {
                        return Collections.singletonMap( IDENTITY_SELECTOR, header.getStrId() );
                    }
                } );
            }
        }, true );
    }

    @Override
    public List<PrivateKeyMO> getResources(String sort, final Boolean ascending, final Map<String, List<Object>> filters) {
        List<PrivateKeyMO> privateKeys = transactional(new TransactionalCallback<List<PrivateKeyMO>>() {
            @Override
            public List<PrivateKeyMO> execute() throws ObjectModelException {
                return Functions.map(getEntityHeaders(!filters.containsKey("alias") ? null : Functions.map(filters.get("alias"), new Unary<String, Object>() {
                    @Override
                    public String call(Object o) {
                        return o.toString();
                    }
                })), new Functions.UnaryThrows<PrivateKeyMO, SsgKeyHeader, FindException>() {
                    @Override
                    public PrivateKeyMO call(final SsgKeyHeader header) throws FindException {
                        SsgKeyEntry keyEntry;
                        try {
                            keyEntry = getSsgKeyEntry(new Pair<>(header.getKeystoreId(), header.getAlias()));
                        } catch (ResourceNotFoundException e) {
                            throw new FindException(e.getMessage(), e);
                        }
                        return buildPrivateKeyResource(keyEntry, Option.<Collection<SpecialKeyType>>none());
                    }
                });
            }
        }, true);
        //sort the keys if a sort is specified.
        if(sort != null){
            switch (sort) {
                case "id":
                    Collections.sort(privateKeys, new Comparator<PrivateKeyMO>() {
                        @Override
                        public int compare(PrivateKeyMO o1, PrivateKeyMO o2) {
                            return (ascending == null || ascending) ? o1.getId().compareTo(o2.getId()) : o2.getId().compareTo(o1.getId());
                        }
                    });
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sort type: " + sort);
            }
        }
        return privateKeys;
    }

    @Override
    public boolean resourceExists(final Map<String, String> selectorMap) {
        return transactional(new TransactionalCallback<Boolean>() {
            @Override
            public Boolean execute() throws ObjectModelException {
                try {
                    final Pair<Goid, String> keyId = getKeyId(selectorMap);
                    final SsgKeyEntry ssgKeyEntry = getSsgKeyEntry(keyId);
                    return ssgKeyEntry != null;
                } catch (ResourceNotFoundException e) {
                    return false;
                }
            }
        }, true);
    }

    @Override
    public PrivateKeyMO asResource(SsgKeyEntry entity) {
        return buildPrivateKeyResource(entity, Option.<Collection<SpecialKeyType>>none());
    }

    @Override
    public final PrivateKeyMO identify( final PrivateKeyMO resource, final SsgKeyEntry entity ) {
        resource.setId(entity.getId());
        return resource;
    }

    @Override
    public SsgKeyEntry fromResource(Object resource, boolean strict) throws InvalidResourceException {
        throw new NotImplementedException("From resource for a private key is not yet implemented.");
    }

    private PrivateKeyMO buildPrivateKeyResource(SsgKeyHeader header) {
        final PrivateKeyMO privateKeyMO = ManagedObjectFactory.createPrivateKey();
        privateKeyMO.setAlias(header.getAlias());
        privateKeyMO.setKeystoreId(header.getKeystoreId().toString());
        privateKeyMO.setId(header.getStrId());
        return privateKeyMO;
    }

    @Override
    public PrivateKeyMO putResource( final Map<String, String> selectorMap,
                                     final Object resource ) throws ResourceNotFoundException, InvalidResourceException {
        if ( !(resource instanceof PrivateKeyMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected private key");

        final PrivateKeyMO privateKeyResource = (PrivateKeyMO) resource;
        final SecurityZone securityZone = getSecurityZone( privateKeyResource.getSecurityZoneId() );

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

                    // handle SecurityZone
                    try {
                        SsgKeyMetadata keyMetadata = null;
                        if (securityZone != null)
                            keyMetadata = new SsgKeyMetadata(ssgKeyEntry.getKeystoreId(), ssgKeyEntry.getAlias(), securityZone);

                        final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                        helper.doUpdateKeyMetadata(
                                ssgKeyEntry.getKeystoreId(),
                                ssgKeyEntry.getAlias(),
                                keyMetadata
                        );

                    } catch ( final Exception e ) {
                        throw new UpdateException( "Error updating security zone reference: " + ExceptionUtils.getMessage( e ), e );
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
                        //add Subject Alternative names to CSR
                        final List<String> sansList = (List<String>) properties.get(PrivateKeyGenerateCsrContext.PROP_SANS);
                        List<X509GeneralName> csrSAN = CertUtils.extractX509GeneralNamesFromList(sansList);
                        final CertificateRequest res = ssgKeyStore.makeCertificateSigningRequest(
                                entry.getAlias(),
                                new CertGenParams(
                                        principal,
                                        csrSAN,
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
                    } catch ( IllegalArgumentException e) {
                        throw new InvalidResourceException(ExceptionType.INVALID_VALUES, ExceptionUtils.getMessage(e));
                    } catch ( UnsupportedX509GeneralNameException e) {
                        throw new InvalidResourceException(ExceptionType.UNEXPECTED_TYPE, "Unsupported Subject Alternative Name Type");
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

    public PrivateKeySignCsrResult signCert( final Map<String,String> selectorMap, final String subjectDN, final Integer expiryAge, final String signatureHash,
                                                    final byte[] certificate ) throws ResourceNotFoundException, InvalidResourceException {
        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, PrivateKeySignCsrResult>>() {
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, PrivateKeySignCsrResult> execute() throws ObjectModelException {
                try {
                    final SsgKeyEntry entry = getSsgKeyEntry( getKeyId( selectorMap ) );
                    checkPermittedForAnyEntity( OperationType.UPDATE, EntityType.SSG_KEY_ENTRY );  // Consistent with TrustedCertAdmin.generateCSR

                    X509Certificate cert;

                    try {
                        RsaSignerEngine signer = JceProvider.getInstance().createRsaSignerEngine(entry.getPrivateKey(), entry.getCertificateChain());


                        byte[] decodedCsrBytes;
                        decodedCsrBytes = CertUtils.csrPemToBinary(certificate);
                        final X500Principal subject;
                        if(subjectDN == null || subjectDN.isEmpty()) {
                            PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(decodedCsrBytes);
                            CertificationRequestInfo certReqInfo = pkcs10.getCertificationRequestInfo();
                            subject = new X500Principal(certReqInfo.getSubject().toString(true, X509Name.DefaultSymbols));
                        } else {
                            subject = new X500Principal(subjectDN);
                        }

                        final CertGenParams certGenParams = new CertGenParams(subject, expiryAge, false, null);
                        certGenParams.setHashAlgorithm(signatureHash);

                        cert = (X509Certificate) signer.createCertificate(decodedCsrBytes, certGenParams.useUserCertDefaults());
                    } catch (UnrecoverableKeyException e) {
                        return left2_1(new ResourceNotFoundException("Error finding key in keystore", e));
                    } catch (IOException e) {
                        return left2_2(new InvalidResourceException(ExceptionType.INVALID_VALUES, "Error resding csr request: " + ExceptionUtils.getMessage(e)));
                    } catch (Exception e) {
                        return left2_2(new InvalidResourceException(ExceptionType.INVALID_VALUES, "Error signing certificate: " + ExceptionUtils.getMessage(e)));
                    }

                    X509Certificate[] caChain = entry.getCertificateChain();
                    X509Certificate[] retChain = new X509Certificate[caChain.length + 1];
                    System.arraycopy(caChain, 0, retChain, 1, caChain.length);
                    retChain[0] = cert;

                    String pemChain = "";
                    try {
                        for (X509Certificate aRetChain : retChain)
                            pemChain += CertUtils.encodeAsPEM(aRetChain);
                    } catch (CertificateEncodingException | IOException e) {
                        return left2_2(new InvalidResourceException(ExceptionType.INVALID_VALUES, "Error encoding signed certificate: " + ExceptionUtils.getMessage(e)));
                    }
                    final PrivateKeySignCsrResult result = new PrivateKeySignCsrResult();
                    result.setCertData(pemChain);
                    return right2(result);
                } catch ( ResourceNotFoundException e ) {
                    return left2_1(e);
                }
            }
        }, true ) );
    }

    @ResourceMethod(name="CreateKey", resource=true, selectors=true)
    public PrivateKeyMO createPrivateKey( final Map<String,String> selectorMap,
                                          final PrivateKeyCreationContext resource ) throws InvalidResourceException, InvalidResourceSelectors {
        final Pair<Goid,String> keyId = getKeyId( selectorMap );
        final SecurityZone securityZone = getSecurityZone( resource.getSecurityZoneId() );

        // The generation of the key pair (helper.doGenerateKeyPair(...)) is executed in a separate thread.
        // Therefore there is a chance of a racing condition when the process of key pair generation takes longer
        // i.e. if the key size is 4096 to complete, in which case the getter below (i.e. getSsgKeyEntry(...)) will not see
        // the updated keystore (i.e. the newly created key-pair), until this transaction completes and flushes all objects.
        //
        // Hence sometimes (mostly when generating keys with bigger size i.e. key-size of 4096) this operation will fail
        // with 403 (FORBIDDEN), but the key will be successfully created.
        // See DE238694 for more details.
        //
        // Separating the writer and getter into distinct transactions will make sure the getter manager (inside getSsgKeyEntry(...))
        // sees the newly created private key.
        //
        // Ideally the transactional callback would have been TransactionalCallback<Option<InvalidResourceException>>,
        // however transactional(...) method only seem to support Either<...> as a exception holder and handle it
        // correctly (i.e. setup the transaction for rollback).
        // That being said I'll use a dummy boolean and do NOT care about the result.
        extract( transactional( new TransactionalCallback<Either<InvalidResourceException,Boolean>>(){
            @Override
            public Either<InvalidResourceException,Boolean> execute() throws ObjectModelException {
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
                    // handle SecurityZone
                    SsgKeyMetadata keyMetadata = null;
                    if (securityZone != null) {
                        keyMetadata = new SsgKeyMetadata( keystoreId, alias, securityZone );
                    }

                    try {
                        final KeyGenParams keyGenParams = curveName.isSome() ?
                                new KeyGenParams(curveName.some()) :
                                new KeyGenParams(keybits);

                        // the generation of the key pair is executed in a separate thread
                        helper.doGenerateKeyPair(
                                keystoreId,
                                alias,
                                keyMetadata, // set metadata for security zone
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
                    } catch (IllegalArgumentException e) {
                        return left(new InvalidResourceException(ExceptionType.INVALID_VALUES, e.getMessage()));
                    }

                    // all good return success
                    return right( true );
                } catch ( InvalidResourceException e ) {
                    return left( e );
                }
            }
        }, false ) );

        // now that the writer transaction is complete start a new one (read-only) to get the newly created private-key MO.
        return extract( transactional( new TransactionalCallback<Either<InvalidResourceException,PrivateKeyMO>>(){
            @Override
            public Either<InvalidResourceException,PrivateKeyMO> execute() throws ObjectModelException {
                try {
                    return right( buildPrivateKeyResource( getSsgKeyEntry( keyId ), Option.<Collection<SpecialKeyType>>none() ) );
                } catch ( ResourceNotFoundException e ) {
                    throw new ResourceAccessException( e ); // error since we just created the resource
                }
            }
        }, true ) );
    }

    @ResourceMethod(name="ImportKey", resource=true, selectors=true)
    public PrivateKeyMO importPrivateKey( final Map<String,String> selectorMap,
                                          final PrivateKeyImportContext resource ) throws InvalidResourceException, InvalidResourceSelectors {
        final Pair<Goid,String> keyId = getKeyId( selectorMap );
        final SecurityZone securityZone = getSecurityZone( resource.getSecurityZoneId() );


        return extract( transactional( new TransactionalCallback<Either<InvalidResourceException, PrivateKeyMO>>() {
            @Override
            public Either<InvalidResourceException, PrivateKeyMO> execute() throws ObjectModelException {
                checkPermittedForSomeEntity( OperationType.CREATE, EntityType.SSG_KEY_ENTRY );

                final Goid keystoreId = keyId.left;
                final String alias = keyId.right;
                final byte[] pkcs12bytes = resource.getPkcs12Data();
                final char[] pkcs12pass = resource.getPassword().toCharArray();
                String pkcs12alias = resource.getAlias();
                // handle SecurityZone
                SsgKeyMetadata keyMetadata = null;
                if (securityZone != null) {
                    keyMetadata = new SsgKeyMetadata( keystoreId, alias, securityZone );
                }

                try {
                    final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
                    return right( buildPrivateKeyResource(
                            helper.doImportKeyFromKeyStoreFile(keystoreId, alias, keyMetadata, pkcs12bytes, "PKCS12", pkcs12pass, pkcs12pass, pkcs12alias),
                            Option.<Collection<SpecialKeyType>>none() ) );
                } catch ( AliasNotFoundException e ) {
                    return left( new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Aliases not found : " + pkcs12alias ) );
                } catch ( MultipleAliasesException e ) {
                    return left( new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Alias must be specified : " + Arrays.asList( e.getAliases() ) ) );
                } catch ( IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException |
                          ExecutionException | InterruptedException | NoSuchProviderException | KeyStoreException e ) {
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
        return toInternalId(Collections.<String,String>singletonMap(IDENTITY_SELECTOR,identifier), validationThrower);
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
            } catch ( IllegalArgumentException nfe ) {
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
    private final SecurityZoneManager securityZoneManager;

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

    private List<SsgKeyHeader> getEntityHeaders(@Nullable List<String> aliasIncludes) throws FindException {
        final List<SsgKeyHeader> headers = new ArrayList<>();

        try {
            for ( final SsgKeyFinder ssgKeyFinder : ssgKeyStoreManager.findAll() ) {
                for ( final String alias : ssgKeyFinder.getAliases() ) {
                    if(aliasIncludes == null || aliasIncludes.contains(alias)) {
                        headers.add( new SsgKeyHeader( toExternalId(ssgKeyFinder.getGoid(), alias), ssgKeyFinder.getGoid(), alias, alias ) );
                    }
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

        // handle SecurityZone
        doSecurityZoneAsResource( privateKey, ssgKeyEntry );

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

    public X509Certificate[] toCertificateArray( final List<CertificateData> certificateChain ) throws InvalidResourceException {
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

    private void doSecurityZoneAsResource(PrivateKeyMO resource, final SsgKeyEntry zoneable) {
        if (zoneable.getSecurityZone() != null) {
            resource.setSecurityZoneId( zoneable.getSecurityZone().getId() );
            resource.setSecurityZone( zoneable.getSecurityZone().getName() );
        }
    }

    private SecurityZone getSecurityZone(final String zoneId) throws InvalidResourceException {
        if (zoneId != null && !zoneId.isEmpty()) {
            final Goid securityZoneGoid;
            try {
                securityZoneGoid = GoidUpgradeMapper.mapId(EntityType.SECURITY_ZONE, zoneId );
            } catch( IllegalArgumentException nfe ) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
            }
            try {
                SecurityZone zone = securityZoneManager.findByPrimaryKey( securityZoneGoid );
                if ( zone == null )
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
                if ( !zone.permitsEntityType(EntityType.SSG_KEY_METADATA) )
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "entity type not permitted for referenced security zone");

                return zone;

            } catch (FindException e) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
            }
        }
        return null;
    }
}
