/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.HexUtils;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JceSignaturePlayground {
    public static void main( String[] args ) throws Exception {
        if ( args.length < 3 ) throw new Exception("args: keystore storepass keypass count threads [kstype]");
        String keystore = args[0];
        String storepass = args[1];
        String keypass = args[2];
        String scount = args[3];
        String sthreads = args[4];
        String kstype = args.length >= 6 ? args[5] : "JKS";

        int count = Integer.valueOf(scount).intValue();
        int threads = Integer.valueOf(sthreads).intValue();

        FileInputStream fis = null;
        KeyStore ks;

        try {
            fis = new FileInputStream(keystore);
            ks = KeyStore.getInstance(kstype);
            ks.load(fis,storepass.toCharArray());
        } finally {
            if ( fis != null ) fis.close();
        }

        String alias = (String)ks.aliases().nextElement();

        final X509Certificate cert = (X509Certificate)ks.getCertificate(alias);
        final RSAPrivateKey privateKey = (RSAPrivateKey)ks.getKey(alias,keypass.toCharArray());

        final Provider aprov = JceProvider.getAsymmetricJceProvider();
        log.info( "Using asymmetric crypto provider " + aprov );

        final String cleartext = KEYINFO;
        final byte[] clearBytes = cleartext.getBytes("UTF-8");
        final byte[] signature;

        try {
            Signature signer = Signature.getInstance( SIG_ALG, aprov );
            signer.initSign( privateKey );
            signer.update( clearBytes );
            signature = signer.sign();
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }

        Runnable sign = new Runnable() {
            public void run() {
                Signature signer = null;
                try {
                    signer = Signature.getInstance( SIG_ALG, aprov );
                    signer.initSign( privateKey );
                    signer.update( clearBytes );
                    signer.sign();
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };

        final String base64Signature = HexUtils.encodeBase64( signature, true );

        Runnable verify = new Runnable() {
            public void run() {
                try {
                    Signature verifier = Signature.getInstance( SIG_ALG, aprov );
                    verifier.initVerify( cert );
                    verifier.update( clearBytes );
                    byte[] signature2 = HexUtils.decodeBase64( base64Signature );
                    if ( !verifier.verify(signature2) )
                        throw new AssertionError( "Verification should have succeeded" );
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };

        Runnable verify2 = new Runnable() {
            public void run() {
                try {
                    Signature verifier = Signature.getInstance( SIG_ALG, aprov );
                    verifier.initVerify( cert );
                    verifier.update( clearBytes );
                    if ( !verifier.verify( signature ) )
                        throw new AssertionError( "Verification should have succeeded" );
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };

        BenchmarkRunner runner;

        log.info( "\nSIGN" );
        runner = new BenchmarkRunner(sign,count, "sign("+ threads+")");
        runner.setThreadCount(threads);
        runner.run();

        log.info( "\nVERIFY" );
        runner = new BenchmarkRunner(verify,count, "verify 1("+threads+")");
        runner.setThreadCount(threads);
        runner.run();

        log.info( "\nVERIFY 2" );
        runner = new BenchmarkRunner(verify2,count, "verify 2("+threads+")");
        runner.setThreadCount(threads);
        runner.run();
    }


    private static final String KEYINFO = "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                                         "    <ds:X509Data>\n" +
                                         "        <ds:X509IssuerSerial>\n" +
                                         "            <ds:X509IssuerName>CN=root.locutus.l7tech.com</ds:X509IssuerName>\n" +
                                         "            <ds:X509SerialNumber>4202746819200835680</ds:X509SerialNumber>\n" +
                                         "        </ds:X509IssuerSerial>\n" +
                                         "        <ds:X509SKI>o7bou7bVKgAfnOHAlGFohaqUWIc=</ds:X509SKI>\n" +
                                         "        <ds:X509SubjectName>CN=alex</ds:X509SubjectName>\n" +
                                         "\n" +
                                         "        <ds:X509Certificate>MIICEDCCAXmgAwIBAgIIOlMn9wdeYGAwDQYJKoZIhvcNAQEFBQAwIjEgMB4GA1UEAxMXcm9vdC5s\n" +
                                         "            b2N1dHVzLmw3dGVjaC5jb20wHhcNMDQwNDAxMDEzMTExWhcNMDYwNDAxMDE0MTExWjAPMQ0wCwYD\n" +
                                         "            VQQDEwRhbGV4MIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQCG/pfjoWFxDceZ/lmk6oU0pL1q\n" +
                                         "            RzthFeAxalY+3SYEgM7016pzdQFp2Q1kipMRd4aAr7D0P1VlUzJY0xV07FMA19pc1NLsoiL48H9v\n" +
                                         "            wi02uAks5ydw/lbOWzO2VMUW+W0619tPsqrkibZQapowncWOvvFWwCU/Wh+aEQWCirGMtQIBEaNk\n" +
                                         "            MGIwDwYDVR0TAQH/BAUwAwEBADAPBgNVHQ8BAf8EBQMDB6AAMB0GA1UdDgQWBBSjtui7ttUqAB+c\n" +
                                         "            4cCUYWiFqpRYhzAfBgNVHSMEGDAWgBScfxfUJeD+dDOvgtX95a+OkkV25TANBgkqhkiG9w0BAQUF\n" +
                                         "            AAOBgQByNGVcPLg+H8s+CHUAGEmRhaQlitWBVs5F9effsyqwaz9gMr641OBaccvNtl92+25KmEIu\n" +
                                         "            ul6YvM0LfZG4r/LoUv8Xfgjb4IvHLbIFmekN80Pqr7pxVaiYRqMMwtGlTHpok9dtKLHZm0o/OoKz EishCafGGrzBkx2uSH4xUTeTYg==</ds:X509Certificate>\n" +
                                         "    </ds:X509Data>\n" +
                                         "</ds:KeyInfo>";

    private static final Logger log = Logger.getLogger(JceSignaturePlayground.class.getName());

    public static final String SIG_ALG = "SHA1withRSA";
}
