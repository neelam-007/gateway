package com.l7tech.skunkworks;

import com.l7tech.common.io.*;
import com.l7tech.security.cert.BouncyCastleCertUtils;
import com.l7tech.security.cert.ParamsKeyGenerator;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.util.FileUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Experimenting with signing .zip files (such as .skar files) in a way that signs the entire raw bytes of the
 * target file, rather than just certain components, and which does not require a detached signature file.
 */
public class SkarSigner {

    // KeyStore type for file used as a trust store.  Since certs are public and are not cryptographic secrets, it is
    // fine (preferable, in fact, for performance) to just use JKS format instead of something like PKCS#12.
    private static final String TRUST_STORE_TYPE = "jks";

    /**
     * A password is required in order to load or save a KeyStore, even in JKS format.
     * But, for a trust store (containing only
     * trusted certs) this is superfluous for security since
     * the certificates it contains are almost always public information, or at least not
     * cryptographic secrets.
     * Since we don't care about secrecy of certificates we will just use the string "changeit".
     * This also happens to be the passphrase used for the "cacerts" trust store file that comes with the JVM.
     */
    private static final char[] TRUST_STORE_PASS = "changeit".toCharArray();

    /**
     * Sign an arbitrary binary input stream using the specified signing key and cert.
     *
     * @param signingCert cert to sign with.  Subject DN is used as key name, so should be unique per trusted cert.  Required.
     * @param signingKey  key to sign with. Required.
     * @param fileToSign inputstream of bytes to sign.  Required.
     * @param outputZip  outputstream to which .ZIP will be written.  Required.
     * @throws Exception if a problem occurs
     */
    public static void signZip( Certificate signingCert, PrivateKey signingKey, InputStream fileToSign, OutputStream outputZip ) throws Exception {
        try ( ZipOutputStream zos = new ZipOutputStream( outputZip ) ) {
            zos.putNextEntry( new ZipEntry( "signed.dat" ) );
            TeeInputStream tis = new TeeInputStream( fileToSign, zos );
            DigestInputStream dis = new DigestInputStream( tis, MessageDigest.getInstance( "SHA-256" ) );
            IOUtils.copyStream( dis, new NullOutputStream() );
            byte[] digestBytes = dis.getMessageDigest().digest();
            String digest = HexUtils.hexDump( digestBytes );

            zos.putNextEntry( new ZipEntry( "signature.properties" ) );
            Properties props = new Properties();

            String keyAlg = signingKey.getAlgorithm().toUpperCase();
            String sigAlg = "SHA512with"  + ( "EC".equals( keyAlg )  ? "ECDSA" : keyAlg );
            Signature signature = Signature.getInstance( sigAlg );
            signature.initSign( signingKey );
            signature.update( digestBytes );
            byte[] signatureBytes = signature.sign();

            props.setProperty( "digest", digest );
            props.setProperty( "signature", HexUtils.encodeBase64( signatureBytes ) );
            props.setProperty( "cert", HexUtils.encodeBase64( signingCert.getEncoded() ) );

            props.store( zos, "Signature" );
        }
    }

    /**
     * Verify a signature created by signZip.
     * <p/>
     * This checks signature and extracts signer cert.
     * <p/>
     * Caller MUST verify that signer is trusted!
     *
     * @param zipToVerify input stream containing .ZIP file as produced by signZip.
     * @return the signer certificate, which has NOT YET BEEN VERIFIED AS TRUSTED.
     * @throws Exception if a problem occurs or if signature cannot be verified
     */
    public static X509Certificate verifyZip( InputStream zipToVerify ) throws Exception {
        ZipInputStream zis = new ZipInputStream( zipToVerify );

        ZipEntry signedEntry = zis.getNextEntry();
        if ( signedEntry.isDirectory() || !"signed.dat".equals( signedEntry.getName() ) )
            throw new IOException( "First zip entry is not a plain file named signed.dat" );
        DigestInputStream dis = new DigestInputStream( zis, MessageDigest.getInstance( "SHA-256" ) );
        IOUtils.copyStream( dis, new NullOutputStream() );
        byte[] computedDigest = dis.getMessageDigest().digest();

        ZipEntry sigEntry = zis.getNextEntry();
        if ( sigEntry.isDirectory() || !"signature.properties".equals( sigEntry.getName() ) )
            throw new IOException( "Second zip entry is not a plain file named signature.properties" );

        Properties props = new Properties();
        props.load( zis );

        byte[] signedDigest = HexUtils.unHexDump( (String) props.get( "digest") );
        if ( !Arrays.equals( computedDigest, signedDigest ) )
            throw new IOException( "Digest of signed.dat does not match value from signature.xml" );

        String signatureValueB64 = (String) props.get( "signature" );
        String signingCertB64 = (String) props.get( "cert" );
        return verifySignatureWithDigestAlreadyChecked( computedDigest, signatureValueB64, signingCertB64 );
    }

    /**
     * Verifies signature, checking that signature verifies with the public key from the specified certificate,
     * but does NOT check that the signing certificate is trusted.
     * <p/>
     * This can be used when the signature information is stored separately, not inside an outer wrapper zip
     * in a signature.properties file.
     *
     * @param digestHex  SHA-256 digest value as hex string, eg. "64a1b96e336958b9a4b01c76ad313c600eea651737b33d61c75d75f3c8e9d053"
     * @param signatureB64 ASN.1 encoded signature value as Base-64 string
     * @param signingCertB64 ASN.1 encoded X.509 certificate as Base-64 string
     * @param signedInfo input stream that will produce the bytes of the object that was signed and that is being verified
     * @return the signing cert, when verification succeeded.  Never null.  Caller must still check that the cert is trusted
     *         and otherwise acceptable (expiry date, basic constraints, etc).
     * @throws Exception if the digest value does not match, or
     *                   if the signature does not verify, or
     *                   if there is an I/O or input format error
     */
    public static X509Certificate verifySignature(
            String digestHex, String signatureB64, String signingCertB64,
            InputStream signedInfo ) throws Exception
    {
        DigestInputStream dis = new DigestInputStream( signedInfo, MessageDigest.getInstance( "SHA-256" ) );
        IOUtils.copyStream( dis, new NullOutputStream() );
        byte[] computedDigest = dis.getMessageDigest().digest();
        if ( !Arrays.equals( computedDigest, HexUtils.unHexDump( digestHex ) ) )
            throw new IOException( "Digest of signed.dat does not match value from signature.xml" );

        return verifySignatureWithDigestAlreadyChecked( computedDigest, signatureB64, signingCertB64 );
    }

    /**
     * Utility method for verifying a signature after the digest has already been computed and compared to the
     * claimed digest value.
     *
     * @param alreadyCheckedDigest the SHA-256 digest of the raw input material.  Required. Note: this MUST NOT just
     *                             be the value claimed by the sender -- it must be a freshly computed value
     *                             from hashing the information covered by the signature.
     * @param signatureB64 ASN.1 encoded signature value as Base-64 string
     * @param signingCertB64 ASN.1 encoded X.509 certificate as Base-64 string
     * @return the signing cert, when verification succeeded.  Never null.  Caller must still check that the cert is trusted
     *         and otherwise acceptable (expiry date, basic constraints, etc).
     * @throws CertificateException if the signing certificate cannot be decoded
     * @throws NoSuchAlgorithmException if the needed signature algorithm is not available from the environment
     * @throws InvalidKeyException if the public key from the cert is not valid
     * @throws SignatureException if signature verification fails
     */
    private static X509Certificate verifySignatureWithDigestAlreadyChecked(
            byte[] alreadyCheckedDigest, String signatureB64, String signingCertB64 )
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
    {
        byte[] signatureValue = HexUtils.decodeBase64( signatureB64 );
        X509Certificate signingCert = CertUtils.decodeCert( HexUtils.decodeBase64( signingCertB64 ) );

        PublicKey verifyKey = signingCert.getPublicKey();
        String keyAlg = verifyKey.getAlgorithm().toUpperCase();
        String sigAlg = "SHA512with"  + ( "EC".equals( keyAlg )  ? "ECDSA" : keyAlg );
        Signature signature = Signature.getInstance( sigAlg );
        signature.initVerify( signingCert );
        signature.update( alreadyCheckedDigest );
        if ( !signature.verify( signatureValue ) )
            throw new SignatureException( "Signature not verified" );

        return signingCert;
    }

    /**
     * Ensure the specified trust store exists as a valid jks file with the expected password.
     *
     * @param trustStoreFile trust store file to verify or create on disk.  Required
     * @return false if a valid trust store file already exists at this location.
     *         true if a file did not exist at this location and was successfully created.
     * @throws KeyStoreException if the trust store file already exists but the file format is invalid
     * @throws CertificateException if the trust store file already exists but contains at least one certificate that can't be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an unexpected error reading or creating the file (including invalid path, permission denied, etc)
     */
    public static boolean ensureTrustStoreFileExists( @NotNull File trustStoreFile ) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        final KeyStore ks = KeyStore.getInstance( TRUST_STORE_TYPE );
        try {
            loadKeyStoreFromFile( trustStoreFile, ks, TRUST_STORE_PASS );
            return false;
        } catch ( FileNotFoundException fe ) {
            // Create new empty trust store
            ks.load( null, null );
            storeKeyStoreToFile( trustStoreFile, ks, TRUST_STORE_PASS );
            return true;
        }
    }

    /**
     * Load a list of root trusted certificates from a trust store file in the specified format (eg, JKS or PKCS12).
     *
     * @param trustStoreFile trust store file to load from disk.  Required
     * @return a collection containing every X.509 certificate from a trusted certificate entry within the trust store.
     * @throws KeyStoreException if the key store file format is invalid
     * @throws CertificateException if any of the certificates in the trust store could not be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an error reading the file (including file not found, permission denied, etc)
     */
    public static Collection<X509Certificate> loadTrustedCertsFromTrustStore( @NotNull File trustStoreFile )
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException
    {
        Collection<X509Certificate> ret = new ArrayList<>();

        KeyStore ks = KeyStore.getInstance( TRUST_STORE_TYPE );
        loadKeyStoreFromFile( trustStoreFile, ks, TRUST_STORE_PASS );

        Enumeration<String> aliases = ks.aliases();
        while ( aliases.hasMoreElements() ) {
            String alias = aliases.nextElement();
            if ( ks.isCertificateEntry( alias ) ) {
                java.security.cert.Certificate cert = ks.getCertificate( alias );
                if ( cert instanceof X509Certificate ) {
                    X509Certificate x509 = (X509Certificate) cert;
                    ret.add( x509 );
                }
            }
        }

        return ret;
    }

    /**
     * Add a new trusted cert entry to a trust store file.
     *
     * @param trustStoreFile trust store file.  Required.
     *                       Must be in a directory writable by the OS-level user under which
     *                       this JVM process is running.
     * @param newTrustedCert certificate to add to the trust store file.  Required.
     * @return true if the certificate was successfully added to the trust store file
     *              (in which case the new file has already been flushed safely to disk).
     *         false if this exact certificate was found to already be present in the specified trust store file.
     * @throws KeyStoreException if the key store file format is invalid
     * @throws CertificateException if any of the certificates in the trust store could not be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an error reading the file (including file not found, permission denied, etc)
     */
    public static boolean addCertificateToTrustStore( @NotNull File trustStoreFile,
                                                      @NotNull X509Certificate newTrustedCert )
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException
    {
        final KeyStore ks = KeyStore.getInstance( TRUST_STORE_TYPE );
        loadKeyStoreFromFile( trustStoreFile, ks, TRUST_STORE_PASS );

        // See what aliases are already in use, and if the cert we are adding is already present
        Set<String> usedAliases = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
        Enumeration<String> aliases = ks.aliases();
        while ( aliases.hasMoreElements() ) {
            String alias = aliases.nextElement();
            usedAliases.add( alias );
            if ( ks.isCertificateEntry( alias ) ) {
                Certificate cert = ks.getCertificate( alias );
                if ( cert instanceof X509Certificate && CertUtils.certsAreEqual( (X509Certificate) cert, newTrustedCert ) ) {
                    // This exact cert is already present in this trust store.
                    return false;
                }
            }
        }

        // Generate an alias that isn't already used in the file
        int num = 1;
        String newAlias = "trusted" + num;
        while ( usedAliases.contains( newAlias ) ) {
            num++;
            newAlias = "trusted" + num;
        }

        // Add the new cert to the trust store and safely overwrite the existing file in-place
        ks.setCertificateEntry( newAlias, newTrustedCert );

        FileUtils.saveFileSafely( trustStoreFile.getPath(), true, new FileUtils.Saver() {
            @Override
            public void doSave( FileOutputStream fos ) throws IOException {
                try {
                    ks.store( fos, TRUST_STORE_PASS );
                } catch ( KeyStoreException | NoSuchAlgorithmException | CertificateException e ) {
                    throw new IOException( e );
                }
            }
        } );

        return true;
    }

    /**
     * Like {@link #signZip(java.security.cert.Certificate, java.security.PrivateKey, java.io.InputStream, java.io.OutputStream)}
     * but reads the private key and signer cert from a Java KeyStore.
     *
     * @param keyStoreFile key store file.  Required.
     * @param storeType store type, eg. "PKCS12".  Required.
     * @param storePass password to decrypt or unlock the key store.
     *                  Generally required except when using certain hardware-based key stores
     *                  in certain configurations.  (Required for "PKCS12")
     * @param alias alias of the key store entry to use for signing.  May be null only if the key store only contains
     *              a single key entry.
     * @param entryPass password for loading key entry, if required.  May be null if key store type is one
     *                  (such as "PKCS12") that does not have passwords for individual key entries.
     * @param fileToSign inputstream of bytes to sign.  Required.
     * @param outputZip  outputstream to which .ZIP will be written.  Required.
     * @throws Exception if there is an error
     */
    public static void signWithKeyStore( @NotNull File keyStoreFile, @NotNull String storeType, @Nullable char[] storePass,
                                         @Nullable String alias, @Nullable char[] entryPass,
                                         InputStream fileToSign, OutputStream outputZip )
            throws Exception
    {
        KeyStore ks = KeyStore.getInstance( storeType );
        loadKeyStoreFromFile( keyStoreFile, ks, storePass );

        Key key = ks.getKey( alias, entryPass );
        if ( !(key instanceof PrivateKey ) ) {
            throw new KeyStoreException( "Key store entry does not contain a private key" );
        }

        PrivateKey privateKey = (PrivateKey) key;
        Certificate[] chain = ks.getCertificateChain( alias );
        signZip( chain[0], privateKey, fileToSign, outputZip );
    }

    /**
     * Safely write a key store file to disk, in a way that can recover if the power goes out while an
     * existing file is being rewritten.
     *
     * @param keyStoreFile file path to write to.  Required.  File may or may not already exist.
     *                     Its directory must be writable by the current process.
     * @param keyStore key store to save.  Required.
     * @param storePass store pass to use for saving.  Generally required by software (file-based) key stores.
     * @throws IOException if there is an error saving the key store.
     */
    static void storeKeyStoreToFile( @NotNull File keyStoreFile, @NotNull final KeyStore keyStore, @Nullable final char[] storePass )
            throws IOException
    {
        FileUtils.saveFileSafely( keyStoreFile.getPath(), true, new FileUtils.Saver() {
            @Override
            public void doSave( FileOutputStream fos ) throws IOException {
                try {
                    keyStore.store( fos, storePass );
                } catch ( KeyStoreException | NoSuchAlgorithmException | CertificateException e ) {
                    throw new IOException( e );
                }
            }
        } );
    }

    /**
     * Does the inverse of {@link #storeKeyStoreToFile(java.io.File, java.security.KeyStore, char[])} in that it loads
     * a KeyStore file from disk in a way that safely recovers if a previous storeKeyStoreToFile() was interrupted
     * (by power loss or the process being killed or a JVM crash etc).
     *
     * @param keyStoreFile file path to read.  Required.  File must already exist in order for this method to succeed.
     * @param keyStore key store object to load into.  Required.
     * @param storePass store pass to use for decrypting or unlocking the key store.  Generally required by software (file-based) key stores.
     * @throws CertificateException if any of the certificates in the trust store could not be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an error reading the file (including file not found, permission denied, etc)
     */
    static void loadKeyStoreFromFile( @NotNull File keyStoreFile, @NotNull final KeyStore keyStore, @Nullable final char[] storePass )
            throws CertificateException, NoSuchAlgorithmException, IOException
    {
        try ( InputStream fis = FileUtils.loadFileSafely( keyStoreFile.getPath() ) ) {
            keyStore.load( fis, storePass );
        }
    }

    /**
     * Generate a new 2048-bit RSA public and private key pair.
     *
     * @return a new KeyPair containing a 2048-bit RSA PublicKey and corresponding PrivateKey.
     * @throws InvalidAlgorithmParameterException if the current environment does not permit creating 2048 bit RSA key pairs
     * @throws NoSuchAlgorithmException if no RSA key pair generator is available in the current environment
     */
    public static KeyPair generateNewKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        // Configure for 2048 bit RSA key.  Could also pass in something like "secp256k1" to use EC P-256 curve
        KeyGenParams params = new KeyGenParams( 2048 );
        ParamsKeyGenerator keyGenerator = new ParamsKeyGenerator( params );
        return keyGenerator.generateKeyPair();
    }

    /**
     * Generate a new self-signed cert using the specified key pair.
     *
     * @param keyPair a key pair.  Required.
     * @param subjectDn subject DN to use for cert, e.g. "cn=foo.example.com".  Required.  Must be a valid X.500 name.
     * @param expiryDays how many days from now before the cert should expire, e.g. 5 * 365
     * @param caCert true if the new self-signed certificate will be used as a CA cert, to sign other certificates
     *               false if it is just a regular old cert
     * @return a self-signed cert certifying the public key from the key pair.
     * @throws CertificateGeneratorException if there is an error generating the cert.
     */
    public static X509Certificate generateSelfSignedCert( KeyPair keyPair, String subjectDn, int expiryDays, boolean caCert )
            throws CertificateGeneratorException
    {
        CertGenParams params = new CertGenParams( new X500Principal( subjectDn ), expiryDays, caCert, null );
        ParamsCertificateGenerator certGenerator = new ParamsCertificateGenerator( params );
        return certGenerator.generateCertificate( keyPair.getPublic(), keyPair.getPrivate(), null );
    }

    /**
     * Generate a new CSR (certificate signing request) requesting certification of the public key
     * from the specified key pair as the true public key belonging to the specified subject DN,
     * signing the CSR using the corresponding private key.
     *
     * @param keyPair a key pair.  Required.
     * @param subjectDn requested subject DN to include in CSR, e.g. "cn=foo.example.com".  Must be a valid X.500 name.  Required.
     *                  Note that the certificate authority is not required to issue a cert to the exact requested DN.
     * @return the encoded bytes of the generated CSR.  Never null.
     * @throws InvalidKeyException       if there is a problem with the provided key pair
     * @throws SignatureException        if there is a problem signing the cert
     * @throws NoSuchProviderException   if "the current asymmetric JCE provider is incorrect" (likely can't happen)
     * @throws NoSuchAlgorithmException  if a required algorithm is not available in the current asymmetric JCE provider
     */
    @NotNull
    public static byte[] generatePkcs10CertificateSigningRequest( KeyPair keyPair, String subjectDn )
            throws NoSuchProviderException, SignatureException, NoSuchAlgorithmException, InvalidKeyException
    {
        CertGenParams params = new CertGenParams( new X500Principal( subjectDn ), 30 * 365, false, null );
        return BouncyCastleCertUtils.makeCertificateRequest( params, keyPair ).getEncoded();
    }

    /**
     * Use the specified certificate authority certificate and private key to process a submitted certificate
     * signing request and produce an official subject certificate, signed by the CA cert.
     *
     * @param csrBytes bytes of an encoded PKCS#10 certificate signing request.  Required.
     * @param caCert the certificate authority certificate.  Required.
     * @param caPrivateKey the corresponding certificate authority private key.  Required.
     * @return a new certificate signed by the CA key, certifying the public key and subject DN from the CSR.
     *         the expiry date is currently always set by this method to five years from today.
     * @throws Exception if something goes wrong
     */
    public static X509Certificate createCertificateFromCsr( byte[] csrBytes, X509Certificate caCert, PrivateKey caPrivateKey ) throws Exception {
        CertGenParams params = new CertGenParams();
        params.setDaysUntilExpiry( 5 * 365 );
        RsaSignerEngine signer = JceProvider.getInstance().createRsaSignerEngine( caPrivateKey, new X509Certificate[] { caCert } );
        return (X509Certificate) signer.createCertificate( csrBytes, params );
    }

    /**
     * Save a private key and its certificate chain into a key store file, creating it if necessary.
     *
     * @param keyStoreFile file path to read.  Required.  File must already exist in order for this method to succeed.
     * @param storeType the key store type, eg "PKCS12".  Required.
     * @param storePass store pass to use for decrypting or unlocking the key store.  Generally required by software (file-based) key stores.
     * @param alias key alias to save under.  Required.
     * @param entryPass password for loading key entry, if required.  May be null if key store type is one
     *                  (such as "PKCS12") that does not have passwords for individual key entries.
     * @param privateKey private key to save into key entry.  Required.
     * @param certChain cert chain to save as this key entry's certificate chain.  Required.
     * @throws KeyStoreException if the key store file format is invalid
     * @throws CertificateException if any of the certificates in the trust store could not be loaded
     * @throws java.security.NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     * @throws IOException if there is an error reading the file (including file not found, permission denied, etc)
     */
    public static void saveKeyAndCertChainToKeyStoreFile( File keyStoreFile, String storeType, final char[] storePass, String alias, char[] entryPass,
                                     PrivateKey privateKey, X509Certificate[] certChain )
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException
    {
        final KeyStore ks = KeyStore.getInstance( storeType );
        try {
            loadKeyStoreFromFile( keyStoreFile, ks, storePass );
        } catch ( FileNotFoundException e ) {
            ks.load( null, null );
        }

        ks.setKeyEntry( alias, privateKey, entryPass, certChain );
        storeKeyStoreToFile( keyStoreFile, ks, storePass );
    }

    /**
     * Verify that the specified signer certificate was duly issued by (or is identical to) one of the trust anchor
     * certificates in the specified trust store file.
     * <p/>
     * If this method returns without throwing an exception, the signer cert is trusted.
     *
     * @param trustStoreFile trust store containing trusted issuer certs.  Required.
     * @param signerCert a signer cert that may or may not be trusted.  Required.
     * @throws Exception if the signer cert should not be trusted or if an error occurs
     */
    public static void verifySignerCertIsTrusted( File trustStoreFile, X509Certificate signerCert ) throws Exception {
        // TODO this is very very sketchy and may not be complete or secure, even though it appears to work in my test driver
        // Needs testing to ensure can't be fooled by cert not issued by trust anchor,
        // spoofed intermediate certs (e.g. use your real code signing cert to sign some other code signing cert)

        // Load trusted certs
        Collection<X509Certificate> trustedCerts = loadTrustedCertsFromTrustStore( trustStoreFile );
        Set<TrustAnchor> trustAnchors = new HashSet<>();
        for ( X509Certificate trustedCert : trustedCerts ) {
            trustAnchors.add( new TrustAnchor( trustedCert, null ) );
        }

        // Build certificate path, since signerCert is just the subject cert and not the full path
        X509CertSelector sel = new X509CertSelector();
        sel.setCertificate( signerCert );
        Set<TrustAnchor> tempAnchors = new HashSet<>();
        tempAnchors.addAll( trustAnchors );
        PKIXBuilderParameters pbp = new PKIXBuilderParameters(tempAnchors, sel);
        pbp.setRevocationEnabled(false);
        pbp.addCertStore( CertStore.getInstance( "Collection", new CollectionCertStoreParameters( trustedCerts ) ) );
        CertPathBuilder pathBuilder = CertPathBuilder.getInstance( "PKIX" );
        CertPathBuilderResult builderResult = pathBuilder.build( pbp );
        CertPath certPath = builderResult.getCertPath();

        // Perform PKIX validation fo cert chain using trusted certs
        PKIXParameters pkixParams = new PKIXParameters( trustAnchors );
        pkixParams.setRevocationEnabled( false );
        CertPathValidator pathValidator = CertPathValidator.getInstance( "PKIX" );
        PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) pathValidator.validate( certPath, pkixParams );

        // Can inspect things like result.getTrustAnchor() here, if we care which trusted cert validated

        // Ok
    }


    /*
     * Test the utility methods in this class by simulating a work flow from prospective solution kit developer ->
     * appication for a code signing cert -> approval and issuance of code signing cert -> SK dev use of code signing cert to sign code ->
     * gateway loading the signed code and checking the signature, verifying that signing cert is duly issued and trusted
     */
    public static void main( String[] args ) throws Exception {
        TestCertificateGenerator gen = new TestCertificateGenerator();
        Pair<X509Certificate, PrivateKey> ca = gen.basicConstraintsCa( 1 ).subject( "cn=root.apim.ca.com" ).keySize( 1024 ).generateWithKey();

        // prospective SK dev generates key pair, placeholder self-signed cert, and csr
        KeyPair signerKeyPair = generateNewKeyPair();
        X509Certificate signerPlaceholderCert = generateSelfSignedCert( signerKeyPair, "cn=placeholder", 100 * 365, false );
        // SK dev stores signer key pair and placeholder cert in key store
        File signerKeyStore = new File( "SkarSigner_keyStore.p12" );
        final char[] signerStorePass = "7layer".toCharArray();
        saveKeyAndCertChainToKeyStoreFile( signerKeyStore, "PKCS12", signerStorePass, "signer", null,
                signerKeyPair.getPrivate(), new X509Certificate[] { signerPlaceholderCert } );
        // SK dev generates CSR, and sends CSR to
        // API Gateway Solution Kit Code Signing Program Administrator  (Tien?)
        byte[] signerCsr = generatePkcs10CertificateSigningRequest( signerKeyPair, "cn=signer.mag.apim.ca.com" );

        // SK dev sends email to Tien applying for a code signing cert, attaches CSR file (just as binary file is fine, can zip it if you want)

        // If application approved, Tien uses code signing CA private key to sign new code signing cert
        X509Certificate signerCert = createCertificateFromCsr( signerCsr, ca.left, ca.right );
        X509Certificate[] signerCertChain = new X509Certificate[] { signerCert, ca.left };

        // Tien emails new signer cert chain back to SK dev (maybe in PEM format, with multiple --BEGIN CERT-- blocks)

        // SK dev replaces placeholder cert in his key store with new real signerCertChain
        saveKeyAndCertChainToKeyStoreFile( signerKeyStore, "PKCS12", signerStorePass, "signer", null,
                signerKeyPair.getPrivate(), signerCertChain );


        byte[] fileToSign = "blah blah blah blah".getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // SK dev can now sign his custom assertion jar file:
        signWithKeyStore( signerKeyStore, "PKCS12", signerStorePass, "signer", null,
                new ByteArrayInputStream( fileToSign ), baos );

        new FileOutputStream( "SkarSigner_test.zip" ).write( baos.toByteArray() );


        // Gateway now checks signature using trust store that contains CA cert that issued code signing cert
        File storeFile = new File( "SkarSigner_trustStore.jks" );
        storeFile.delete();
        ensureTrustStoreFileExists( storeFile );
        addCertificateToTrustStore( storeFile, ca.left );

        X509Certificate sawSigner = verifyZip( new ByteArrayInputStream( baos.toByteArray() ) );
        verifySignerCertIsTrusted( storeFile, sawSigner );

        System.out.println( "Signature verified successfully" );
    }

}
