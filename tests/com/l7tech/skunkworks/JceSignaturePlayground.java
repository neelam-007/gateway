/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import com.ibm.xml.dsig.KeyInfo;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.HexUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JceSignaturePlayground {
    public JceSignaturePlayground() {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );
        dbf.setValidating( false );
    }

    public static void main( String[] args ) throws Exception {
        final Provider aprov = JceProvider.getAsymmetricJceProvider();
        final String cleartext = KEYINFO;
        final byte[] clearBytes = cleartext.getBytes("UTF-8");
        final RSAPrivateKey privateKey = getPrivateKey();
        final X509Certificate cert = getCertificate();
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
        runner = new BenchmarkRunner(sign,500);
        runner.setThreadCount(4);
        runner.run();

        log.info( "\nVERIFY" );
        runner = new BenchmarkRunner(verify,500);
        runner.setThreadCount(4);
        runner.run();

        log.info( "\nVERIFY 2" );
        runner = new BenchmarkRunner(verify2,500);
        runner.setThreadCount(4);
        runner.run();
    }

    private static RSAPublicKey clientCertPublicKey = null;
    private static RSAPublicKey getClientCertPublicKey() throws Exception {
        if (clientCertPublicKey != null) return clientCertPublicKey;
        return clientCertPublicKey = (RSAPublicKey)getCertificate().getPublicKey();
    }

    private static BigInteger getPrivateExponent() throws Exception {
        String keyHex = PRIVATE_EXPONENT;
        return new BigInteger(keyHex, 16);
    }

    private static RSAPrivateKey getPrivateKey() throws Exception {
        final RSAPublicKey pubkey = getClientCertPublicKey();
        final BigInteger exp = getPrivateExponent();
        RSAPrivateKey privkey = new RSAPrivateKey() {
            public BigInteger getPrivateExponent() {
                return exp;
            }

            public byte[] getEncoded() {
                throw new UnsupportedOperationException();
            }

            public String getAlgorithm() {
                return "RSA";
            }

            public String getFormat() {
                return "RAW";
            }

            public BigInteger getModulus() {
                return pubkey.getModulus();
            }
        };

        return privkey;
    }



    private static X509Certificate getCertificate() throws Exception {
        // Find KeyInfo bodyElement, and extract certificate from this
        Document keyInfoDoc = parse( KEYINFO );
        Element keyInfoElement = keyInfoDoc.getDocumentElement();

        if (keyInfoElement == null) {
            throw new Exception("KeyInfo bodyElement not found");
        }

        KeyInfo keyInfo = new KeyInfo(keyInfoElement);

        // Assume a single X509 certificate
        KeyInfo.X509Data[] x509DataArray = keyInfo.getX509Data();

        KeyInfo.X509Data x509Data = x509DataArray[0];
        X509Certificate[] certs = x509Data.getCertificates();

        X509Certificate cert = certs[0];
        return cert;
    }

    private static Document parse( String xml ) throws Exception {
        return dbf.newDocumentBuilder().parse( new ByteArrayInputStream( xml.getBytes("UTF-8") ) );
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

    private static final String PRIVATE_EXPONENT = "575971570e11dfbd9f4586763d88b08b79a7bd3d266bff189871fb9216a021080d7140411d87f1db13f99b68b983c5cf8071aebc28fb0553f366a6b387e435b44f4ea87aeef8bb247ce557bd1a7b09d4754c0eab239ad99d51c7df152956e03ab9e2bd61230b70dc8851113978f39c9d99f5e555aed0d3471619d4873a4520b1";

    private static final Logger log = Logger.getLogger(JceSignaturePlayground.class.getName());

    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    static {
        dbf.setNamespaceAware(true);
    }
    public static final String SIG_ALG = "SHA1withRSA";
}
