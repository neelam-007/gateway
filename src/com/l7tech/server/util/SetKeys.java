/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.util;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.prov.bc.BouncyCastleRsaSignerEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class SetKeys {
    public static final String PREFIX = "com.l7tech.server.util.SetKeys.";

    public static final String CA_KS = System.getProperty(PREFIX + "CA_KS", "ca.ks");
    public static final String CA_CERT = System.getProperty(PREFIX + "CA_CERT", "ca.cer");
    public static final String CA_ALIAS = System.getProperty(PREFIX + "CA_ALIAS", "ssgroot");
    public static final String CA_DN_PREFIX = System.getProperty(PREFIX + "CA_DN_PREFIX", "cn=root.");
    public static final int CA_VALIDITY_DAYS = Integer.getInteger(PREFIX + "CA_VALIDITY_DAYS", 5 * 365 ).intValue();

    public static final String SSL_KS = System.getProperty(PREFIX + "SSL_KS", "ssl.ks");
    public static final String SSL_CERT = System.getProperty(PREFIX + "SSL_CERT", "ssl.cer");
    public static final String SSL_ALIAS = System.getProperty(PREFIX + "SSL_ALIAS", "tomcat");
    public static final String SSL_DN_PREFIX = System.getProperty(PREFIX + "SSL_DN_PREFIX", "cn=");
    public static final int SSL_VALIDITY_DAYS = Integer.getInteger(PREFIX + "SSL_VALIDITY_DAYS", 2 * 365 ).intValue();

    private static final Logger log = Logger.getLogger(SetKeys.class.getName());

    SetKeys( String[] args, boolean newca ) {
        if (args.length < 4)
            throw new IllegalArgumentException( "Usage: java ( " +
                                                SetKeys.class.getName() + ".NewCa | " +
                                                SetKeys.class.getName() + ".ExistingCa" +
                                                " ) hostname ksdir capass sslpass kstype" );
        this.newca = newca;
        hostname = args[0];
        ksdir = args[1];
        capass = args[2];
        sslpass = args[3];
        kstype = args[4];

        JceProvider.init();
    }

    private void doIt() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, SignatureException, InvalidKeyException, UnrecoverableKeyException {
        File dir = new File(ksdir);
        if ( !dir.exists() ) throw new IOException( "Keystore directory '" + ksdir + "' does not exist" );
        if ( !dir.isDirectory() ) throw new IOException( "Keystore directory '" + ksdir + "' is not a directory" );

        X509Certificate caCert;
        PrivateKey caPrivateKey;
        {
            File caksfile = new File(dir,CA_KS);
            KeyStore caks = KeyStore.getInstance(kstype);

            if ( newca ) {
                log.info("Creating new CA keystore");
                caks.load(null,null);

                log.info("Generating RSA keypair for CA cert");
                KeyPair cakp = JceProvider.generateRsaKeyPair();
                caPrivateKey = cakp.getPrivate();

                log.info("Generating self-signed CA cert");
                caCert = BouncyCastleRsaSignerEngine.makeSelfSignedRootCertificate(CA_DN_PREFIX + hostname, CA_VALIDITY_DAYS, cakp );

                caks.setKeyEntry(CA_ALIAS, caPrivateKey, capass.toCharArray(), new X509Certificate[] { caCert } );
                FileOutputStream fos = null;

                try {
                    fos = new FileOutputStream(caksfile);
                    caks.store(fos, capass.toCharArray());
                    log.info("Saved " + kstype + " keystore to <" + caksfile.getAbsolutePath() + ">");
                } finally {
                    if ( fos != null ) fos.close();
                }
            } else {
                log.info("Using existing CA keystore");

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(caksfile);
                    caks.load(fis, capass.toCharArray());
                    log.info("Loaded " + kstype + " keystore from <" + caksfile.getAbsolutePath() + ">");
                } finally {
                    if ( fis != null ) fis.close();
                }

                caCert = (X509Certificate)caks.getCertificate(CA_ALIAS);
                caPrivateKey = (PrivateKey)caks.getKey(CA_ALIAS, capass.toCharArray());

                log.info("Loaded " + kstype + " from <" + caksfile.getAbsolutePath() + ">");
            }
        }

        {
            log.info("Exporting DER-encoded CA certificate");
            byte[] caCertBytes = caCert.getEncoded();
            FileOutputStream fos = null;
            try {
                File caCertFile = new File(dir,CA_CERT);
                fos = new FileOutputStream(caCertFile);
                fos.write(caCertBytes);
                log.info("Saved DER-encoded X.509 certificate to <" + caCertFile.getAbsolutePath() + ">" );
            } finally {
                if ( fos != null ) fos.close();
            }
        }

        X509Certificate sslCert = null;
        {
            File sslKsFile = new File(dir, SSL_KS);
            KeyStore sslks = KeyStore.getInstance(kstype);
            log.info( "Creating new SSL keystore" );
            sslks.load(null,null);
            log.info( "Generating RSA keypair for SSL cert" );
            KeyPair sslkp = JceProvider.generateRsaKeyPair();

            sslCert =
                BouncyCastleRsaSignerEngine.makeSignedCertificate( SSL_DN_PREFIX + hostname,
                                                                   SSL_VALIDITY_DAYS,
                                                                   sslkp.getPublic(), caCert, caPrivateKey );
            sslks.setKeyEntry( SSL_ALIAS, sslkp.getPrivate(), sslpass.toCharArray(),
                               new X509Certificate[] { sslCert, caCert } );
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(sslKsFile);
                sslks.store(fos, sslpass.toCharArray());
                log.info("Saved " + kstype + " keystore to <" + sslKsFile.getAbsolutePath() + ">");
            } finally {
                if ( fos != null ) fos.close();
            }
        }

        {
            log.info("Exporting DER-encoded SSL certificate");
            byte[] sslCertBytes = sslCert.getEncoded();
            FileOutputStream fos = null;
            try {
                File sslCertFile = new File(dir,SSL_CERT);
                fos = new FileOutputStream(sslCertFile);
                fos.write(sslCertBytes);
                log.info("Saved DER-encoded X.509 certificate to <" + sslCertFile.getAbsolutePath() +">");
            } finally {
                if ( fos != null ) fos.close();
            }
        }
    }

    public static class ExistingCa {
        public static void main( String[] args ) throws Exception {
            SetKeys me = new SetKeys(args, false);
            me.doIt();
        }
    }

    public static class NewCa {
        public static void main( String[] args ) throws Exception {
            SetKeys me = new SetKeys(args, true);
            me.doIt();
        }
    }

    private final String hostname;
    private final String ksdir;
    private final String capass;
    private final String sslpass;
    private final boolean newca;
    private final String kstype;
}
