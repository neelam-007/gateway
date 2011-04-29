package com.l7tech.skunkworks.crypto;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.ParamsCertificateGenerator;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.cert.X509Certificate;
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
        final X509Certificate certificate = CertUtils.decodeFromPEM( base64, false );
        System.out.println( "Issuer DN:\t" + certificate.getIssuerDN() );
        System.out.println( "Subject DN :\t" + certificate.getSubjectDN() );
        System.out.println( "Serial Number:\t" + certificate.getSerialNumber().toString( 16 ) );
        System.out.println( "Not Before:\t" + ISO8601Date.format( certificate.getNotBefore() ) );
        System.out.println( "Not After:\t" + ISO8601Date.format( certificate.getNotAfter() ) );

        final CertGenParams cgp = new CertGenParams( certificate.getSubjectX500Principal(), 1000000, false, certificate.getSigAlgName() );
        cgp.setNotBefore( certificate.getNotBefore() );
        cgp.setSerialNumber( certificate.getSerialNumber() );
        final ParamsCertificateGenerator pcg = new ParamsCertificateGenerator( cgp );
        final byte[] dummyPrivateKey = new byte[64];
        new Random().nextBytes( dummyPrivateKey );
        final RSAPrivateKeySpec rpks = new RSAPrivateKeySpec( new BigInteger( dummyPrivateKey ), BigInteger.ONE );
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final X509Certificate reanimatedCertificate = pcg.generateCertificate( certificate.getPublicKey(), kf.generatePrivate( rpks ), null );

        System.out.println(  );
        System.out.println( "Issuer DN:\t" + reanimatedCertificate.getIssuerDN() );
        System.out.println( "Subject DN :\t" + reanimatedCertificate.getSubjectDN() );
        System.out.println( "Serial Number:\t" + reanimatedCertificate.getSerialNumber().toString( 16 ) );
        System.out.println( "Not Before:\t" + ISO8601Date.format( reanimatedCertificate.getNotBefore() ) );
        System.out.println( "Not After:\t" + ISO8601Date.format( reanimatedCertificate.getNotAfter() ) );
        System.out.println( HexUtils.encodeBase64( reanimatedCertificate.getEncoded(), true ) );
    }
}
