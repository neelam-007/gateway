package com.l7tech.gateway.common.security.signer;

import com.l7tech.common.io.*;
import com.l7tech.security.cert.BouncyCastleCertUtils;
import com.l7tech.security.cert.ParamsKeyGenerator;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Common utility methods for signing files as well as verifying files signature.
 */
public class SignerUtils {
    /**
     * The ZIP entry name (lower case) containing the signed data bytes.
     */
    static final String SIGNED_DATA_ZIP_ENTRY = "signed.dat";

    /**
     * The ZIP entry name (lower case) containing the signature information:
     * <table class="properties-table" cellpadding="0" cellspacing="0">
     * <tr><th>Element</th><th>Description</th></tr>
     * <tr>
     * <td>{@link #SIGNING_CERT_PROPS cert}</td>
     * <td>ASN.1 encoded X.509 certificate as Base64 string.</td>
     * </tr>
     * <tr>
     * <td>{@link #SIGNATURE_PROP signature}</td>
     * <td>ASN.1 encoded signature value as Base64 string.</td>
     * </tr>
     * </table>
     */
    static final String SIGNATURE_PROPS_ZIP_ENTRY = "signature.properties";

    /**
     * ASN.1 encoded X.509 certificate as Base64 string.
     */
    private static final String SIGNING_CERT_PROPS = "cert";

    /**
     * ASN.1 encoded signature value as Base64 string.
     */
    private static final String SIGNATURE_PROP = "signature";

    /**
     * All known signature property keys.<br/>
     * Currently the following property keys are used:
     * <ul>
     *     <li>{@link #SIGNING_CERT_PROPS}</li>
     *     <li>{@link #SIGNATURE_PROP}</li>
     * </ul>
     *
     * Note: When adding new property keys, update this array!
     */
    public static final String[] ALL_SIGNING_PROPERTIES = {
            SIGNING_CERT_PROPS,
            SIGNATURE_PROP
    };

    /**
     * Trusted Certificate alias prefix in the trusted store, e.g.:
     * trusted1, trusted2, trusted3 etc.
     */
    private static final String TRUSTED_CERTS_ALIAS_PREFIX = "trusted";


    /**
     * Configure for EC P-256 curve.
     */
    private static final String GEN_PRIVATE_KEY_ALG = "P-256";

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // SIGNING
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Sign an arbitrary binary input stream using the specified signing key and cert.
     *
     * @param signingCert    cert to sign with.  Subject DN is used as key name, so should be unique per trusted cert.
     *                       Required and cannot be {@code null}.
     * @param signingKey     key to sign with.  Required and cannot be {@code null}.
     * @param fileToSign     the {@code InputStream} of bytes to sign.  Required and cannot be {@code null}.
     * @param outputZip      the {@code OutputStream} to which .ZIP will be written.  Required and cannot be {@code null}.
     * @throws Exception if a problem occurs.
     */
    public static void signZip(
            @NotNull final Certificate signingCert,
            @NotNull final PrivateKey signingKey,
            @NotNull final InputStream fileToSign,
            @NotNull final OutputStream outputZip
    ) throws Exception {
        try (final ZipOutputStream zos = new ZipOutputStream(outputZip)) {
            // first zip entry should be the signed data bytes
            zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
            // write the fileToSign into the first zip entry
            final TeeInputStream tis = new TeeInputStream(fileToSign, zos);
            // generate module digest
            final DigestInputStream dis = new DigestInputStream(tis, MessageDigest.getInstance("SHA-256"));
            IOUtils.copyStream(dis, new NullOutputStream());
            final byte[] digestBytes = dis.getMessageDigest().digest();

            // next zip entry is the signature information
            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
            // generate the signature
            final String keyAlg = signingKey.getAlgorithm().toUpperCase();
            if ("DSA".equals(keyAlg)) {
                throw new KeyStoreException("DSA key algorithm is not supported; recommend using RSA or EC instead.");
            }
            final String sigAlg = "SHA512with" + ("EC".equals(keyAlg) ? "ECDSA" : keyAlg);
            final Signature signature = Signature.getInstance(sigAlg);
            signature.initSign(signingKey);
            signature.update(digestBytes);
            final byte[] signatureBytes = signature.sign();

            // store the signature (base64 encoded) and signing cert (also base64 encoded)
            final Properties props = new Properties();
            props.setProperty(SIGNATURE_PROP, HexUtils.encodeBase64(signatureBytes, true));
            props.setProperty(SIGNING_CERT_PROPS, HexUtils.encodeBase64(signingCert.getEncoded(), true));
            // save the properties into the zip stream i.e. SIGNATURE_PROPS_ZIP_ENTRY
            props.store(zos, "Signature");
        }
    }

    /**
     * Like {@link #signZip(java.security.cert.Certificate, java.security.PrivateKey, java.io.InputStream, java.io.OutputStream)}
     * but reads the private key and signer cert from a Java KeyStore.
     *
     * @param keyStoreFile    key store file.  Required and cannot be {@code null}.
     * @param storeType       store type, eg. "PKCS12".  Required and cannot be {@code null}.
     * @param storePass       password to decrypt or unlock the key store.
     *                        Generally required except when using certain hardware-based key stores in certain configurations.
     *                        (Required for "PKCS12")
     * @param alias           alias of the key store entry to use for signing.
     *                        Optional and can be {@code null} only if the key store contains a single key entry.
     * @param entryPass       password for loading key entry, if required.
     *                        Optional and can be {@code null} if key store type is one (such as "PKCS12") that does
     *                        not have passwords for individual key entries.
     * @param fileToSign      {@code InputStream} of bytes to sign.  Required and cannot be {@code null}.
     * @param outputZip       {@code OutputStream} to which .ZIP will be written.  Required and cannot be {@code null}.
     * @throws Exception if there is an error
     */
    public static void signWithKeyStore(
            @NotNull final File keyStoreFile,
            @NotNull final String storeType,
            @Nullable final char[] storePass,
            @Nullable String alias,
            @Nullable final char[] entryPass,
            @NotNull final InputStream fileToSign,
            @NotNull final OutputStream outputZip
    ) throws Exception {
        final KeyStore ks = KeyStore.getInstance(storeType);
        loadKeyStoreFromFile(keyStoreFile, ks, storePass);

        // if alias is not specified or is blank make sure the keystore have only one key entry
        if (StringUtils.isBlank(alias)) {
            final List<String> aliases = Collections.list(ks.aliases());
            if (aliases.size() != 1) {
                throw new KeyStoreException("Alias must be specified if the key store contains multiple key entry");
            }
            alias = aliases.get(0);
        }

        // make sure alias contains a private key
        final Key key = ks.getKey(alias, entryPass);
        if (!(key instanceof PrivateKey)) {
            throw new KeyStoreException("Key store entry '" + alias + "' does not contain a private key");
        }

        // get public keys i.e. cert chain
        final PrivateKey privateKey = (PrivateKey) key;
        final Certificate[] chain = ks.getCertificateChain(alias);
        // finally sign using the signing cert (first in the cert chain) and private key
        signZip(chain[0], privateKey, fileToSign, outputZip);
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ON-BOARDING NEW SK DEVELOPER
    // Generate Signing Cert
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Generate a new 2048-bit RSA public and private key pair.
     *
     * @return a new {@code KeyPair} containing a EC P-256 curve {@code PublicKey} and corresponding {@code PrivateKey}.
     * @throws InvalidAlgorithmParameterException if the current environment does not permit creating EC P-256 curve key pairs
     * @throws NoSuchAlgorithmException if no EC P-256 curve key pair generator is available in the current environment
     */
    public static KeyPair generateNewKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        final KeyGenParams params = new KeyGenParams(GEN_PRIVATE_KEY_ALG);
        final ParamsKeyGenerator keyGenerator = new ParamsKeyGenerator(params);
        return keyGenerator.generateKeyPair();
    }

    /**
     * Generate a new CSR (Certificate Signing Request) requesting certification of the public key
     * from the specified key pair as the true public key belonging to the specified subject DN,
     * signing the CSR using the corresponding private key.
     *
     * @param keyPair       a key pair.  Required and cannot be {@code null}.
     * @param subjectDn     requested subject DN to include in CSR, e.g. "cn=foo.example.com".  Must be a valid X.500 name.
     *                      Required and cannot be {@code null}.
     *                      Note that the certificate authority is not required to issue a cert to the exact requested DN.
     * @param expiryDays    days from now until the cert shall expire, or 0 to use default.
     * @param sigAlg        signature algorithm to use.  Optional and can be {@code null} to use default.
     * @return the encoded bytes of the generated CSR.  Never {@code null}.
     * @throws InvalidKeyException       if there is a problem with the provided key pair
     * @throws SignatureException        if there is a problem signing the cert
     * @throws NoSuchProviderException   if "the current asymmetric JCE provider is incorrect" (likely can't happen)
     * @throws NoSuchAlgorithmException  if a required algorithm is not available in the current asymmetric JCE provider
     */
    @NotNull
    public static byte[] generatePkcs10CertificateSigningRequest(
            @NotNull final KeyPair keyPair,
            @NotNull final String subjectDn,
            int expiryDays,
            @Nullable final String sigAlg
    ) throws NoSuchProviderException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        final CertGenParams params = new CertGenParams(new X500Principal(subjectDn), expiryDays, false, sigAlg);
        return BouncyCastleCertUtils.makeCertificateRequest(params, keyPair).getEncoded();
    }

    /**
     * Use the specified certificate authority certificate and private key to process a submitted certificate
     * signing request and produce an official subject certificate, signed by the CA cert.
     *
     * @param csrBytes     bytes of an encoded PKCS#10 certificate signing request.  Required and cannot be {@code null}.
     * @param caCert       the certificate authority certificate.  Required and cannot be {@code null}.
     * @param caPrivateKey the corresponding certificate authority private key.  Required and cannot be {@code null}.
     * @param expiryDays   days from now until the cert shall expire, or 0 to use default.
     * @return a new certificate signed by the CA key, certifying the public key and subject DN from the CSR.
     *         the expiry date is currently always set by this method to five years from today.
     * @throws Exception if something goes wrong
     */
    public static X509Certificate createCertificateFromCsr(
            @NotNull final byte[] csrBytes,
            @NotNull final X509Certificate caCert,
            @NotNull final PrivateKey caPrivateKey,
            int expiryDays
    ) throws Exception {
        final CertGenParams params = new CertGenParams();
        params.setDaysUntilExpiry(expiryDays);
        final RsaSignerEngine signer = JceProvider.getInstance().createRsaSignerEngine(caPrivateKey, new X509Certificate[]{caCert});
        return (X509Certificate) signer.createCertificate(csrBytes, params);
    }

    /**
     * Save a private key and its certificate chain into a key store file, creating it if necessary.
     *
     * @param keyStoreFile    file path to read.  Required and cannot be {@code null}.
     * @param storeType       the key store type, eg "PKCS12".  Required and cannot be {@code null}.
     * @param storePass       store pass to use for decrypting or unlocking the key store.  Generally required by software (file-based) key stores.
     * @param alias           key alias to save under.  Required and cannot be {@code null}.
     * @param entryPass       password for loading key entry, if required.  May be {@code null} if key store type is one
     *                        (such as "PKCS12") that does not have passwords for individual key entries.
     * @param privateKey      private key to save into key entry.  Required and cannot be {@code null}.
     * @param certChain       cert chain to save as this key entry's certificate chain.  Required and cannot be {@code null}.
     * @throws KeyStoreException if the key store file format is invalid
     * @throws CertificateException if any of the certificates in the trust store could not be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an error reading the file (including file not found, permission denied, etc)
     */
    public static void saveKeyAndCertChainToKeyStoreFile(
            @NotNull final File keyStoreFile,
            @NotNull final String storeType,
            @Nullable final char[] storePass,
            @NotNull final String alias,
            @Nullable final char[] entryPass,
            @NotNull final PrivateKey privateKey,
            @NotNull final X509Certificate[] certChain
    ) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore ks = KeyStore.getInstance(storeType);
        try {
            loadKeyStoreFromFile(keyStoreFile, ks, storePass);
        } catch (final FileNotFoundException e) {
            ks.load(null, null);
        }

        ks.setKeyEntry(alias, privateKey, entryPass, certChain);
        storeKeyStoreToFile(keyStoreFile, ks, storePass);
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // VERIFYING
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Verify a signature created by {@link #signZip}.
     * <p/>
     * This checks signature and extracts signer cert.
     * <p/>
     * Caller MUST verify that signer is trusted!
     *
     * @param zipToVerify    input stream containing .ZIP file as produced by signZip.  Required and cannot be {@code null}.
     * @return the signer certificate, which has NOT YET BEEN VERIFIED AS TRUSTED.
     * @throws Exception if a problem occurs or if signature cannot be verified
     */
    public static X509Certificate verifyZip(@NotNull final InputStream zipToVerify) throws Exception {
        // get SHA-256 the message digest
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        // process zipToVerify
        final Pair<byte[], Properties> digestAndSignature = walkSignedZip(
                zipToVerify,
                new SignedZipVisitor<byte[], Properties>() {
                    @Override
                    public byte[] visitData(@NotNull final InputStream inputStream) throws IOException {
                        // calc SHA-256 of SIGNED_DATA_ZIP_ENTRY
                        final DigestInputStream dis = new DigestInputStream(inputStream, messageDigest);
                        IOUtils.copyStream(dis, new com.l7tech.common.io.NullOutputStream());
                        return dis.getMessageDigest().digest();
                    }

                    @Override
                    public Properties visitSignature(@NotNull final InputStream inputStream) throws IOException {
                        final Properties signatureProperties = new Properties();
                        signatureProperties.load(inputStream);
                        return signatureProperties;
                    }
                },
                true
        );

        // get digest and signature props
        final byte[] computedDigest = digestAndSignature.left;
        final Properties signatureProperties = digestAndSignature.right;
        // check if both are read correctly
        if (computedDigest != null && signatureProperties != null) {
            return verifySignatureWithDigest(computedDigest, signatureProperties);
        }

        // shouldn't happen though, just in case throw
        final String detailedMessage;
        if (computedDigest == null && signatureProperties == null) {
            detailedMessage = "Both '" + SIGNED_DATA_ZIP_ENTRY + "' and '" + SIGNATURE_PROPS_ZIP_ENTRY + "' entries are missing";
        } else if (computedDigest == null) {
            detailedMessage = "'" + SIGNED_DATA_ZIP_ENTRY + "' entry is missing";
        } else {
            detailedMessage = "'" + SIGNATURE_PROPS_ZIP_ENTRY + "' entry is missing";
        }
        // missing required zip entries; throw IOException
        throw new IOException("Invalid signed Zip: " + detailedMessage);
    }

    /**
     * Convenient method with zip file.
     *
     * @see #walkSignedZip(java.io.InputStream, SignedZipVisitor, boolean)
     */
    @NotNull
    public static <Data, SigProps> Pair<Data, SigProps> walkSignedZip(
            @NotNull final File zipFile,
            @NotNull final SignedZipVisitor<Data, SigProps> visitor,
            final boolean strict
    ) throws IOException {
        try (final InputStream bis = new BufferedInputStream(new FileInputStream(zipFile))) {
            return walkSignedZip(bis, visitor, strict);
        }
    }

    /**
     * Walks a signed zip file tree.
     *
     * <p>This method walks through each zip-entry in the specified signed zip {@code InputStream} (created using {@link #signZip} method).<br/>
     * If the entry is the signed file contents then {@link SignedZipVisitor#visitData(java.io.InputStream)}
     * is invoked with the entry's {@code ZipInputStream}, so that content can be read. <br/>
     * If the entry is the signature properties then {@link SignedZipVisitor#visitSignature(java.io.InputStream)}
     * is invoked with the entry's {@code ZipInputStream}, so that content can be read. <br/>
     * When {@code strict} is specified the method will stop and throw {@code IOException} on any unexpected zip-entry in the signed zip.
     * </p>
     *
     * @param zipFileStream    signed zip {@code InputStream}.  Required and cannot be {@code null}.
     * @param visitor          the signed zip visitor to invoke for each relevant zip-entry found.  Required and cannot be {@code null}.
     * @param strict           a flag indicating whether this method will fail (with {@code IOException}) on any unexpected zip-entry,
     *                         found in the signed zip, before visiting signed data and signature properties.
     * @throws IOException if either signed data or signature properties entries have not been found in the signed zip
     * or an IO error happens while walking through the signed zip.
     */
    @NotNull
    public static <Data, SigProps> Pair<Data, SigProps> walkSignedZip(
            @NotNull final InputStream zipFileStream,
            @NotNull final SignedZipVisitor<Data, SigProps> visitor,
            final boolean strict
    ) throws IOException {
        // create new zip input stream from the zipFileStream InputStream.
        try (final ZipInputStream zis = (zipFileStream instanceof ZipInputStream ? (ZipInputStream)zipFileStream : new ZipInputStream(zipFileStream))) {
            // flags indicating whether signed.dat and signature.properties entries have been found
            boolean foundSignedDat = false, foundSigProps = false;
            Data data = null;
            SigProps sigProps = null;
            // get the first zip entry
            ZipEntry entry = zis.getNextEntry();
            // loop while there are more entries
            while (entry != null) {
                // check if entry is directory
                try {
                    if (!entry.isDirectory()) {
                        // check if entity name is blank
                        final String entryName = entry.getName();
                        if (SIGNED_DATA_ZIP_ENTRY.equals(entryName)) {
                            if (!foundSignedDat) {
                                foundSignedDat = true;
                                data = visitor.visitData(new NonCloseableInputStream(zis));
                            }
                        } else if (SIGNATURE_PROPS_ZIP_ENTRY.equals(entryName)) {
                            if (!foundSigProps) {
                                foundSigProps = true;
                                sigProps = visitor.visitSignature(new NonCloseableInputStream(zis));
                            }
                        } else if (strict) {
                            // throw exception if this entry is a directory
                            throw new IOException("Invalid signed Zip: Unexpected entry: " + entryName);
                        }
                    } else if (strict) {
                        throw new IOException("Invalid signed Zip: Unexpected directory entry: " + entry.getName());
                    }
                } finally {
                    // close the entry
                    zis.closeEntry();
                }

                // check if all entries have been read already
                if (foundSignedDat && foundSigProps) {
                    // nothing more to read
                    break;
                }

                // finally move to the next entry
                entry = zis.getNextEntry();
            }

            // check zip validity
            if (!foundSignedDat || !foundSigProps) {
                final String detailedMessage;
                if (!foundSignedDat && !foundSigProps) {
                    detailedMessage = "Both '" + SIGNED_DATA_ZIP_ENTRY + "' and '" + SIGNATURE_PROPS_ZIP_ENTRY + "' entries are missing";
                } else if (!foundSignedDat) {
                    detailedMessage = "'" + SIGNED_DATA_ZIP_ENTRY + "' entry is missing";
                } else {
                    detailedMessage = "'" + SIGNATURE_PROPS_ZIP_ENTRY + "' entry is missing";
                }
                // missing required zip entries; throw IOException
                throw new IOException("Invalid signed Zip: " + detailedMessage);
            }

            // finally return requested data and signature properties
            return Pair.pair(data, sigProps);
        }
    }

    /**
     * Verify a signature after the digest has already been computed and compared to the claimed digest value.
     *
     * @param digest    the SHA-256 digest of the raw input material.  Required and cannot be {@code null}.
     *                  Note: this MUST NOT just be the value claimed by the sender -- it must be a freshly computed
     *                  value from hashing the information covered by the signature.
     * @param props     Signature properties, holding ASN.1 encoded X.509 certificate as Base64 string and ASN.1 encoded
     *                  signature value as Base64 string.  Required and cannot be {@code null}.
     * @return the signing cert, when verification succeeded.  Never {@code null}.
     * Caller must still check that the cert is trusted and otherwise acceptable (expiry date, basic constraints, etc).
     * @throws java.security.cert.CertificateException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.InvalidKeyException
     * @throws java.security.SignatureException
     */
    public static X509Certificate verifySignatureWithDigest(
            @NotNull final byte[] digest,
            @NotNull final Properties props
    ) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // get signature and signing cert from properties
        final String signatureB64 = (String) props.get(SIGNATURE_PROP);
        if (StringUtils.isBlank(signatureB64)) {
            throw new SignatureException("Signature property is missing.");
        }
        final String signingCertB64 = (String) props.get(SIGNING_CERT_PROPS);
        if (StringUtils.isBlank(signingCertB64)) {
            throw new SignatureException("Signing Certificate property is missing.");
        }

        // decode signature
        final byte[] signatureValue = HexUtils.decodeBase64(signatureB64);
        // decode signing cert
        final X509Certificate signingCert = CertUtils.decodeCert(HexUtils.decodeBase64(signingCertB64));

        final PublicKey verifyKey = signingCert.getPublicKey();
        final String keyAlg = verifyKey.getAlgorithm().toUpperCase();
        if ("DSA".equals(keyAlg)) {
            throw new SignatureException("DSA key algorithm is not supported; recommend using RSA or EC instead.");
        }
        final String sigAlg = "SHA512with" + ("EC".equals(keyAlg) ? "ECDSA" : keyAlg);
        final Signature signature = Signature.getInstance(sigAlg);
        signature.initVerify(signingCert);
        signature.update(digest);
        if (!signature.verify(signatureValue)) {
            throw new SignatureException("Signature not verified");
        }

        return signingCert;
    }

    /**
     * Verify that the specified signer certificate was duly issued by (or is identical to) one of the trust anchor
     * certificates in the specified trust store file.
     * <p/>
     * If this method returns without throwing an exception, the signer cert is trusted.
     *
     * @param trustStoreFile       trust store containing trusted issuer certs.  Required and cannot be {@code null}.
     * @param trustStoreType       trust store type.  Required and cannot be {@code null}.
     * @param trustStorePassword   trust store password to use for decrypting or unlocking the key store.  Generally required by software (file-based) key stores.
     * @param signerCert     a signer cert that may or may not be trusted.  Required and cannot be {@code null}.
     * @throws Exception if the signer cert should not be trusted or if an error occurs
     */
    public static void verifySignerCertIsTrusted(
            @NotNull final File trustStoreFile,
            @NotNull final String trustStoreType,
            @Nullable final char[] trustStorePassword,
            @NotNull final X509Certificate signerCert
    ) throws Exception {
        // TODO this is very very sketchy and may not be complete or secure, even though it appears to work in my test driver
        // Needs testing to ensure can't be fooled by cert not issued by trust anchor,
        // spoofed intermediate certs (e.g. use your real code signing cert to sign some other code signing cert)

        // Load trusted certs
        final Collection<X509Certificate> trustedCerts = loadTrustedCertsFromTrustStore(trustStoreFile, trustStoreType, trustStorePassword);
        final Set<TrustAnchor> trustAnchors = new HashSet<>();
        for (final X509Certificate trustedCert : trustedCerts) {
            trustAnchors.add(new TrustAnchor(trustedCert, null));
        }

        // Build certificate path, since signerCert is just the subject cert and not the full path
        final X509CertSelector sel = new X509CertSelector();
        sel.setCertificate(signerCert);
        final Set<TrustAnchor> tempAnchors = new HashSet<>();
        tempAnchors.addAll(trustAnchors);
        final PKIXBuilderParameters pbp = new PKIXBuilderParameters(tempAnchors, sel);
        pbp.setRevocationEnabled(false);
        pbp.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(trustedCerts)));
        final CertPathBuilder pathBuilder = CertPathBuilder.getInstance("PKIX");
        final CertPathBuilderResult builderResult = pathBuilder.build(pbp);
        final CertPath certPath = builderResult.getCertPath();

        // Perform PKIX validation fo cert chain using trusted certs
        final PKIXParameters pkixParams = new PKIXParameters(trustAnchors);
        pkixParams.setRevocationEnabled(false);
        final CertPathValidator pathValidator = CertPathValidator.getInstance("PKIX");
        //noinspection UnusedDeclaration
        final PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) pathValidator.validate(certPath, pkixParams);

        // TODO:
        // pathValidator.validate(...) throws CertPathValidatorException and InvalidAlgorithmParameterException when validating signature
        // CertPathValidatorException and InvalidAlgorithmParameterException can be used to detect failure to verify signer cert.

        // Can inspect things like result.getTrustAnchor() here, if we care which trusted cert validated

        // Ok
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MISCELLANEOUS
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Load a list of root trusted certificates from a trust store file in the specified format (eg, JKS or PKCS12).
     *
     * @param trustStoreFile trust store file to load from disk.  Required and cannot be {@code null}.
     * @param type           trust store type.  Required and cannot be {@code null}.
     * @param password       trust store password to use for decrypting or unlocking the key store.  Generally required by software (file-based) key stores.
     * @return a collection containing every X.509 certificate from a trusted certificate entry within the trust store.
     * @throws KeyStoreException if the key store file format is invalid
     * @throws CertificateException if any of the certificates in the trust store could not be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an error reading the file (including file not found, permission denied, etc)
     */
    public static Collection<X509Certificate> loadTrustedCertsFromTrustStore(
            @NotNull final File trustStoreFile,
            @NotNull final String type,
            @Nullable final char[] password
    ) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final Collection<X509Certificate> ret = new ArrayList<>();

        final KeyStore ks = KeyStore.getInstance(type);
        loadKeyStoreFromFile(trustStoreFile, ks, password);

        final Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            if (ks.isCertificateEntry(alias)) {
                final java.security.cert.Certificate cert = ks.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    final X509Certificate x509 = (X509Certificate) cert;
                    ret.add(x509);
                }
            }
        }

        return ret;
    }

    /**
     * Add a new trusted cert entry to a trust store file.
     *
     * @param trustStoreFile trust store file.  Required and cannot be {@code null}.
     *                       Must be in a directory writable by the OS-level user under which this JVM process is running.
     * @param type           trust store type.  Required and cannot be {@code null}.
     * @param password       trust store password to use for decrypting or unlocking the key store.  Generally required by software (file-based) key stores.
     * @param newTrustedCert certificate to add to the trust store file.  Required and cannot be {@code null}.
     * @return {@code true} if the certificate was successfully added to the trust store file
     * (in which case the new file has already been flushed safely to disk).
     * {@code false} if this exact certificate was found to already be present in the specified trust store file.
     * @throws KeyStoreException if the key store file format is invalid
     * @throws CertificateException if any of the certificates in the trust store could not be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an error reading the file (including file not found, permission denied, etc)
     */
    public static boolean addTrustedCertificateToTrustStore(@NotNull final File trustStoreFile,
                                                            @NotNull final String type,
                                                            @Nullable final char[] password,
                                                            @NotNull final X509Certificate newTrustedCert
    ) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        // get the keyStore instance of the specified type
        final KeyStore ks = KeyStore.getInstance(type);
        // load the specified trustStoreFile into our keyStore instance
        // fails with FileNotFoundException (i.e. IOException) if file is not found in the disk
        loadKeyStoreFromFile(trustStoreFile, ks, password);

        // See what aliases are already in use, and if the cert we are adding is already present
        final Set<String> usedAliases = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            usedAliases.add(alias);
            if (ks.isCertificateEntry(alias)) {
                final Certificate cert = ks.getCertificate(alias);
                if (cert instanceof X509Certificate && CertUtils.certsAreEqual((X509Certificate) cert, newTrustedCert)) {
                    // This exact cert is already present in this trust store.
                    return false;
                }
            }
        }

        // Generate an alias that isn't already used in the file
        int num = 1;
        String newAlias = TRUSTED_CERTS_ALIAS_PREFIX + num;
        while (usedAliases.contains(newAlias)) {
            num++;
            newAlias = TRUSTED_CERTS_ALIAS_PREFIX + num;
        }

        // Add the new cert to the trust store and safely overwrite the existing file in-place
        ks.setCertificateEntry(newAlias, newTrustedCert);

        // finally save the trust store file
        storeKeyStoreToFile(trustStoreFile, ks, password);

        return true;
    }

    /**
     * Create an empty key store with the specified file path.
     *
     * @param keyStoreFile    file path to write to.  Required and cannot be {@code null}.
     * @param type            key store type.  Required and cannot be {@code null}.
     * @param password        key store password to use for decrypting or unlocking the key store.  Generally required by software (file-based) key stores.
     * @throws java.io.IOException if the specified file path already exists or if there is an unexpected error creating the file (including invalid path, permission denied, etc).
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws java.security.KeyStoreException if the key store file format is invalid
     * @throws java.security.cert.CertificateException shouldn't really happen as we are loading empty key store
     */
    public static void createEmptyKeyStoreFile(
            @NotNull final File keyStoreFile,
            @NotNull final String type,
            @Nullable final char[] password
    ) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        // try to load the file first
        try (final InputStream ignored = FileUtils.loadFileSafely(keyStoreFile.getPath())) {
            // file exists so throw
            throw new FileAlreadyExistsException(keyStoreFile.getPath());
        } catch (final FileNotFoundException e) {
            final KeyStore ks = KeyStore.getInstance(type);
            // Create new empty trust store
            ks.load(null, null);
            storeKeyStoreToFile(keyStoreFile, ks, password);
        } catch (final IOException e) {
            // file exists but failed to be opened/loaded
            throw new FileAlreadyExistsException(keyStoreFile.getPath());
        }
    }

    /**
     * Safely write a key store file to disk, in a way that can recover if the power goes out while an
     * existing file is being rewritten.
     *
     * @param keyStoreFile    file path to write to.  Required and cannot be {@code null}.
     *                        File may or may not already exist. Its directory must be writable by the current process.
     * @param keyStore        key store to save.  Required and cannot be {@code null}.
     * @param storePass       store pass to use for saving.  Generally required by software (file-based) key stores.
     * @throws IOException if there is an error saving the key store.
     */
    public static void storeKeyStoreToFile(
            @NotNull final File keyStoreFile,
            @NotNull final KeyStore keyStore,
            @Nullable final char[] storePass
    ) throws IOException {
        FileUtils.saveFileSafely(keyStoreFile.getPath(), true, new FileUtils.Saver() {
            @Override
            public void doSave(final FileOutputStream fos) throws IOException {
                try {
                    keyStore.store(fos, storePass);
                } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
                    throw new IOException(e);
                }
            }
        });
    }

    /**
     * Loads a {@code KeyStore} file from disk.
     *
     * @param keyStoreFile    file path to read.  Required and cannot be {@code null}. File must already exist in order for this method to succeed.
     * @param keyStore        key store object to load into.  Required and cannot be {@code null}.
     * @param storePass       store pass to use for decrypting or unlocking the key store.  Generally required by software (file-based) key stores.
     * @throws CertificateException if any of the certificates in the trust store could not be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an error reading the file (including file not found, permission denied, etc)
     */
    public static void loadKeyStoreFromFile(
            @NotNull final File keyStoreFile,
            @NotNull final KeyStore keyStore,
            @Nullable final char[] storePass
    ) throws CertificateException, NoSuchAlgorithmException, IOException {
        try (final InputStream fis = new BufferedInputStream(new FileInputStream(keyStoreFile))) {
            keyStore.load(fis, storePass);
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
