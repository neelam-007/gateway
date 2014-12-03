package com.l7tech.skunkworks;

import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Utility class for generating a self-signed cert, without relying on any other Gateway code
 * except the Bouncy Castle library.  SKUNKWORKS -- NOT USED BY GATEWAY
 */
public class SelfSignedCertGenerator {
    private static SecureRandom secureRandom = new SecureRandom();
    private String sanitizedUsername;
    private String subjectDn;
    private X509Certificate cert;
    private KeyPair keyPair;
    private int rsaSize = 2048;
    private int daysUntilExpiry = 365 * 10;

    /**
     * Create a generator that will generate a new RSA key pair and self-signed cert with the specified common name.
     *
     * @param username name to use as common name of new cert DN.  Will be sanitized first -- anything that isn't alphanumeric,
     *                 underscore, dash, or period will be changed to underscore.
     */
    public SelfSignedCertGenerator( String username ) {
        this.sanitizedUsername = sanitizeCn( username );
        this.subjectDn = "CN=" + sanitizedUsername;
    }

    @SuppressWarnings( "MalformedRegex" )
    static String sanitizeCn( String username ) {
        return username.replaceAll( "(?U)[^\\p{IsAlphabetic}\\p{IsDigit}-\\.]", "_" );
    }

    /**
     * Get the generated certificate.
     * <p/>
     * This will perform key pair and cert generation if it has not yet been performed.
     *
     * @return the certificate.  Never null.
     * @throws GeneralSecurityException if there is an error generating the key pair or cert
     */
    public X509Certificate getCertificate() throws GeneralSecurityException {
        if ( null == cert )
            generate();

        return cert;
    }

    /**
     * Get the generated private key.
     * <p/>
     * This will perform key pair and cert generation if it has not yet been performed.
     *
     * @return the private key.  Never null.
     * @throws GeneralSecurityException if there is an error generating the key pair or cert
     */
    public PrivateKey getPrivateKey() throws GeneralSecurityException {
        if ( null == keyPair )
            generate();

        return keyPair.getPrivate();
    }

    /**
     * Create PKCS#12 keystore file bytes containing a single key entry for the generated private key, with
     * its cert chain set to its generated self-signed cert, encrypted with the specified passphrase.
     * <p/>
     * The returned bytes can be written to a .p12 file and used by any Java code (with keystore type set to
     * "PKCS12") with the specified passphrase to load it.
     * <p/>
     * The alias for the keystore will be the sanitized username, if not specified.
     * <p/>
     * This method fail if the current JDK's default RSA key pair generator implementation is backed by an HSM
     * that is configured to disallow export of generated private key material.
     *
     * @param passphrase the passphrase to use for the PKCS#12 file.
     * @param keyAlias the alias to use for the key entry, or null to default to the sanitized username.
     * @return bytes of a PKCS#12 file containing a key entry, with alias set to the username.
     * @throws GeneralSecurityException if there is a problem generating the file
     */
    public byte[] toKeyStoreFileBytes( char[] passphrase, String keyAlias ) throws GeneralSecurityException {
        if ( null == keyPair || null == cert )
            generate();

        String alias = keyAlias != null ? keyAlias : sanitizedUsername;

        try {
            KeyStore ks = KeyStore.getInstance( "PKCS12" );
            ks.load( null, null );
            ks.setKeyEntry( alias, keyPair.getPrivate(), passphrase, new X509Certificate[] { cert } );

            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ks.store( bo, passphrase );
            return bo.toByteArray();

        } catch ( IOException e ) {
            // Can't happen
            throw new KeyStoreException( e );
        }

    }

    @SuppressWarnings( "deprecation" )
    void generate() throws GeneralSecurityException {
        // Generate key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance( "RSA" );
        kpg.initialize( rsaSize, secureRandom );
        keyPair = kpg.generateKeyPair();

        // Generate self-signed cert
        X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
        gen.setSerialNumber( new BigInteger( 64, secureRandom ).abs() ); // random serial number
        gen.setNotBefore( new Date( new Date().getTime() - ( 10 * 60 * 1000L ) ) ); // become valid as of 10 minutes ago
        gen.setNotAfter( new Date( new Date().getTime() + ( daysUntilExpiry * 24 * 60 * 60 * 1000L ) ) ); // lasts for specified number of days
        gen.setSignatureAlgorithm( "SHA256withRSA" );
        gen.setSubjectDN( new X500Principal( subjectDn ) );
        gen.setIssuerDN( new X500Principal( subjectDn ) );
        gen.setPublicKey( keyPair.getPublic() );
        gen.addExtension( X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure( keyPair.getPublic() ) );
        gen.addExtension( X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure( keyPair.getPublic() ) );
        cert = gen.generate( keyPair.getPrivate(), secureRandom );
    }
}
