package com.l7tech.skunkworks.crypto;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.ParamsCertificateGenerator;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Random;

/**
 * Create an updated version of a test certificate (when tests fail due to expiry)
 *
 * Note that this won't work if the certificate is covered by the signature.
 */
public class TestCertReanimator {

    private static String base64 = "MIIB+jCCAWOgAwIBAgIIOZnsTJQgnDEwDQYJKoZIhvcNAQEFBQAwDTELMAkGA1UEAwwCY2EwHhcNMDkwNDI5MTg0NDM1WhcNMTEwNDI5MTg1NDM1WjAQMQ4wDAYDVQQDDAV1c2VyMTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAiSz7d1lQNl5xpkdJ55CL/3IXSS2336n1ZzN3Gy6PaOAzKgWN6nWX+10PKRC3WSNKmzGyEdAMdtn5bVaTdq0BfgTL8Wj0TX8fr+4K7fuNjiPIqoscUflkKyzFpX3fjW23c2GXk6CvMbvQrddPk9RzcHmG/i1i5f8rih6gFNW1fO0CAwEAAaNgMF4wDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBeAwHQYDVR0OBBYEFGcnHikf5Qjf/QCyJPI/cc07jEnZMB8GA1UdIwQYMBaAFFIzqBOiLaRPfnWuFj65gLovitskMA0GCSqGSIb3DQEBBQUAA4GBAEUnXuK1cWHqQ5hVHYKxeIGehs39P3wmO9LDGi0wENy6Q0vo4g8YcQCcYh38eai5Oob00/u4nHUG06JmB6ub01cRF4KdNSM3fn4zc65S6TEdKzpCYjhlgUYkZUqj0r6lPb7TSADwK+/hJmchiCOON1higqNFm7m99MUQKJK7KG2B";

    public static void main( String[] args ) throws Exception {
        JceProvider.init();

        final byte[] certBin = CertUtils.decodeCertBytesFromPEM( base64, false );
        X509Certificate certificate;
        try {
            certificate = CertUtils.decodeCert( certBin );
        } catch ( final CertificateParsingException e ) {
            certificate = (X509Certificate) CertificateFactory.getInstance( "X.509", new BouncyCastleProvider() ).generateCertificate(new ByteArrayInputStream(certBin));
        }

        System.out.println( "Issuer DN:\t" + certificate.getIssuerDN() );
        System.out.println( "Subject DN :\t" + certificate.getSubjectDN() );
        System.out.println( "Serial Number:\t" + certificate.getSerialNumber().toString( 16 ) );
        System.out.println( "Not Before:\t" + ISO8601Date.format( certificate.getNotBefore() ) );
        System.out.println( "Not After:\t" + ISO8601Date.format( certificate.getNotAfter() ) );

        String algorithm = certificate.getSigAlgName();
        PrivateKey key;
        if ( "RSA".equals( certificate.getPublicKey().getAlgorithm() ) ) {
            final byte[] dummyPrivateKey = new byte[64];
            new Random().nextBytes( dummyPrivateKey );
            final RSAPrivateKeySpec rpks = new RSAPrivateKeySpec( new BigInteger( dummyPrivateKey ), BigInteger.ONE );
            final KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePrivate( rpks );
        } else if ( "EC".equals( certificate.getPublicKey().getAlgorithm() ) ) {
            algorithm = "SHA1WITHECDSA";
            // random garbage
            final BigInteger p = new BigInteger( HexUtils.unHexDump("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFF") );
            final BigInteger a = new BigInteger( HexUtils.unHexDump("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFC") );
            final BigInteger b = new BigInteger( HexUtils.unHexDump("00B3312FA7E23EE7E4988E056BE3F82D19181D9C6EFE8141120314088F5013875AC656398D8A2ED19D2A85C8EDD3EC2AEF") );
            final BigInteger n = new BigInteger( HexUtils.unHexDump("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7634D81F4372DDF581A0DB248B0A77AECEC196ACCC52973") );
            final ECPrivateKeySpec rpks = new ECPrivateKeySpec( BigInteger.ONE, new ECParameterSpec( new EllipticCurve( new ECFieldFp( p ), a, b), new ECPoint( BigInteger.ONE, BigInteger.ONE ), n, 1 ) );
            final KeyFactory kf = KeyFactory.getInstance("EC");
            key = kf.generatePrivate( rpks );
        } else {
            throw new IllegalArgumentException( "Unsupported key type : " + certificate.getPublicKey().getAlgorithm() );
        }

        final CertGenParams cgp = new CertGenParams( certificate.getSubjectX500Principal(), 1000000, false, algorithm );
        cgp.setNotBefore( certificate.getNotBefore() );
        cgp.setSerialNumber( certificate.getSerialNumber() );
        final ParamsCertificateGenerator pcg = new ParamsCertificateGenerator( cgp );
        final X509Certificate reanimatedCertificate = pcg.generateCertificate( certificate.getPublicKey(), key, null );

        System.out.println(  );
        System.out.println( "Issuer DN:\t" + reanimatedCertificate.getIssuerDN() );
        System.out.println( "Subject DN :\t" + reanimatedCertificate.getSubjectDN() );
        System.out.println( "Serial Number:\t" + reanimatedCertificate.getSerialNumber().toString( 16 ) );
        System.out.println( "Not Before:\t" + ISO8601Date.format( reanimatedCertificate.getNotBefore() ) );
        System.out.println( "Not After:\t" + ISO8601Date.format( reanimatedCertificate.getNotAfter() ) );
        System.out.println( HexUtils.encodeBase64( reanimatedCertificate.getEncoded(), true ) );
    }
}
