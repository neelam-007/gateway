/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import com.l7tech.common.security.prov.luna.LunaCmu;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class MakeLunaCerts {
    private static final Logger log = Logger.getLogger(MakeLunaCerts.class.getName());

    private static final String USAGE = "Usage: MakeLunaCerts [-f] ssghostname.company.com\n\n  -f    Force overwrite of existing certificate(s)";

    public static void main(String[] args) {
        try {
            realMain(args);
            System.exit(0);
        } catch (Exception e) {
            System.err.println("ERROR: " + ExceptionUtils.getMessage(e));
            System.exit(1);
        }
    }

    public static void realMain(String[] args) throws Exception {
        if (args.length < 1) throw new IllegalArgumentException(USAGE);

        String hostname = null;
        boolean force = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-f".equalsIgnoreCase(arg))
                force = true;
            else if (hostname != null)
                throw new IllegalArgumentException(USAGE);
            else
                hostname = arg;
        }

        if (hostname == null || hostname.trim().length() < 1) throw new IllegalArgumentException(USAGE);

        makeCerts(force, hostname);

    }

    /**
     *
     * @param force  set this to true if existing certifictes should be overwritten.  Otherwise no action will be
     *               taken if existing certificates are detected.
     * @param hostname  the hostname to use in the CN of the newly generated certs.  Must not be null or empty.
     *                  The SSL cert will use the DN "CN=hostname".  The CA cert will use the DN "CN=root.hostname".
     * @throws LunaCmu.LunaCmuException if the Luna cmu utility was not found (check "lunaCmuPath" system property)
     * @throws LunaCmu.LunaCmuException if the Luna token manager is not currently logged into a partition
     * @throws ClassNotFoundException if the Luna classes are not in the current classpath
     * @throws ClassNotFoundException if the Luna class version is not compatible with this code
     * @throws KeyStoreException if there is a problem creating a Luna keystore or locating or storing a key with it
     * @throws IOException if there is a problem writing the exported certificates to disk
     * @throws NoSuchAlgorithmException if the certs could not be loaded or exported due to a signature algorithm being missing.
     *                                  Normally, this should not be possible.
     * @throws CertificateException if the new certificates cannot be DER encoded for export.
     *                              Normally, this should not be possible.
     */
    public static void makeCerts(boolean force, String hostname) throws LunaCmu.LunaCmuException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, ClassNotFoundException {
        log.info("Checking for Luna Certificate Management Utility (cmu) command... ");
        LunaCmu cmu = new LunaCmu();
        log.info("Connecting to Luna KeyStore... ");
        KeyStore ks = KeyStore.getInstance("Luna");
        ks.load(null, null);

        if (keyExists(ks, "tomcat") || keyExists(ks, "ssgroot")) {
            if (!force)
                throw new RuntimeException("SSG Certificates already present on this KeyStore.\n       Use -f switch to force them to be overwritten.");
            log.info("Deleting existing CA and SSL certificates with labels 'tomcat' or 'ssgroot'...");
            ks.deleteEntry("tomcat");
            ks.deleteEntry("ssgroot");
            LunaCmu.CmuObject[] objs = cmu.list();
            for (int i = 0; i < objs.length; i++) {
                LunaCmu.CmuObject obj = objs[i];
                if (obj.getLabel() != null && (obj.getLabel().startsWith("tomcat") || obj.getLabel().startsWith("ssgroot"))) {
                    log.info("  deleting object " + obj);
                    cmu.delete(obj);
                }
            }
        }

        // Generate CA certificate
        final LunaCmu.CmuObject caCertObj;
        final X509Certificate rootCertKs;
        {
            final String cn = "root." + hostname;
            log.info("Generating new CA certificate: cn=" + cn);

            LunaCmu.CmuObject caKeyObj = cmu.generateRsaKeyPair("ssgroot");
            caCertObj = cmu.generateCaCert(caKeyObj, null, cn);
            X509Certificate rootCert = cmu.exportCertificate(caCertObj);

            rootCertKs = (X509Certificate)ks.getCertificate("ssgroot");
            if (!CertUtils.certsAreEqual(rootCert, rootCertKs))
                throw new IllegalStateException("Exported CA cert from CMU differs from CA cert retrieved through Luna KeyStore");

            if (!keyExists(ks, "ssgroot"))
                throw new IllegalStateException("Unable to find newly created CA key and cert through Luna KeyStore");

            log.info("Generated and saved a CA certificate under alias \"ssgroot\": " + rootCert.getSubjectDN().toString());

            new FileOutputStream("ca.cer").write(rootCert.getEncoded());
            log.info("CA cert exported to ca.cer in current directory");
        }

        // Generate SSL certificate signed by the CA certificate
        {
            final String cn = hostname;
            log.info("Generating new SSL certificate: cn=" + cn);

            LunaCmu.CmuObject sslKeyObj = cmu.generateRsaKeyPair("tomcat");
            byte[] csr = cmu.requestCertificate(sslKeyObj, cn);
            X509Certificate sslCert = cmu.certify(csr, caCertObj, 365 * 5, 1002, "tomcat--cert0");

            X509Certificate sslCertKs = (X509Certificate)ks.getCertificate("tomcat");
            if (!CertUtils.certsAreEqual(sslCert, sslCertKs))
                throw new IllegalStateException("Exported SSL cert from CMU differs from SSL cert retrieved through Luna KeyStore");

            if (!keyExists(ks, "tomcat"))
                throw new IllegalStateException("Unable to find newly recreated SSL key and cert through Luna KeyStore");

            log.info("Generated and saved an SSL certificate under alias \"tomcat\": " + sslCert.getSubjectDN().toString());

            new FileOutputStream("ssl.cer").write(sslCert.getEncoded());
            log.info("SSL cert exported to ssl.cer in current directory");
        }

        log.info("Success.");
    }

    private static boolean keyExists(KeyStore ks, final String alias) throws NoSuchAlgorithmException, KeyStoreException {
        try {
            return ks.getKey(alias, null) != null && ks.getCertificate(alias) instanceof X509Certificate;
        } catch (UnrecoverableKeyException kse) {
            return false;
        }
    }
}
