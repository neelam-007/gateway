/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import com.l7tech.security.prov.luna.LunaCmu;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.util.logging.Logger;

/**
 * Generate and/or export Luna certificates for a SecureSpan Gateway.  This implementation uses the LunaCmu
 * class to invoke the Luna cmu command line utility (both Windows and Linux).
 */
public class MakeLunaCerts {
    private static final Logger log = Logger.getLogger(MakeLunaCerts.class.getName());

    private static final String USAGE = "Usage: MakeLunaCerts [-f][-e] ssghostname.company.com\n\n  -f    Force overwrite of existing certificate(s)\n  -e   Export existing certs only; do not make new ones";
    private static final String CA_LABEL = "ssgroot";
    private static final String SSL_LABEL = "tomcat";

    public static void main(String[] args) {
        boolean debug = false;
        try {
            if (args.length < 1) throw new IllegalArgumentException(USAGE);

            String hostname = null;
            boolean force = false;
            boolean exportOnly = false;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("-f".equalsIgnoreCase(arg))
                    force = true;
                else if ("-d".equalsIgnoreCase(arg))
                    debug = true;
                else if ("-e".equalsIgnoreCase(arg))
                    exportOnly = true;
                else if (hostname != null)
                    throw new IllegalArgumentException(USAGE);
                else
                    hostname = arg;
            }

            if (hostname == null || hostname.trim().length() < 1) throw new IllegalArgumentException(USAGE);

            makeCerts(hostname, force, exportOnly, new File("./ca.cer"), new File("./ssl.cer"));

            System.exit(0);
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: The following class was not found: " + ExceptionUtils.getMessage(e) + "\n" +
                               "       Please ensure that the current Java environment has been configured\n" +
                               "       to use the Luna key store.");
            if (debug)
                e.printStackTrace(System.err);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERROR: " + ExceptionUtils.getMessage(e));
            if (debug)
                e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /** @deprecated remove this as soon as megry's code no longer uses it */
    public static void realMain(String hostname, boolean force) throws Exception {
        makeCerts(hostname, force, false, new File("./ca.cer"), new File("./ssl.cer"));
    }

    public static class CertsAlreadyExistException extends Exception {
        public CertsAlreadyExistException() {}
        public CertsAlreadyExistException(String message) {super(message);}
        public CertsAlreadyExistException(String message, Throwable cause) {super(message, cause);}
        public CertsAlreadyExistException(Throwable cause) {super(cause);}
    }

    public static class CertsDoNotAlreadyExistException extends Exception {
        public CertsDoNotAlreadyExistException() { }
        public CertsDoNotAlreadyExistException(String message) { super(message); }
        public CertsDoNotAlreadyExistException(String message, Throwable cause) { super(message, cause); }
        public CertsDoNotAlreadyExistException(Throwable cause) { super(cause); }
    }

    /**
     * Create new CA and SSL certificates in the current Luna partition
     * and optionally export them to disk in DER format.
     *
     * @param hostname  the hostname to use in the CN of the newly generated certs.  Must not be null or empty.
     *                  The SSL cert will use the DN "CN=hostname".  The CA cert will use the DN "CN=root.hostname".
     * @param forceOverwrite  set this to true if existing certifictes should be overwritten.  Otherwise no action will be
     *                        taken if existing certificates are detected.
     * @param exportOnly   if true, do not actually create new certs -- just export the existing ones.
     *                     This will fail if existing certs do not already exist.
     *                     Requires at least one of exportCaCert and exportSslCert.
     * @param exportCaCert if non-null, the CA cert will exported to this file in DER format.  Any existing file
     *                     will be overwritten.
     * @param exportSslCert if non-null, the SSL cert will be exported to this file in DER format.  Any existing
     *                      file will be overwritten.
     * @throws LunaCmu.LunaCmuException if the Luna cmu utility was not found (check "lunaCmuPath" system property)
     * @throws LunaCmu.LunaTokenNotLoggedOnException if the Luna token manager is not currently logged into a partition
     * @throws ClassNotFoundException if the Luna classes are not in the current classpath
     * @throws ClassNotFoundException if the Luna class version is not compatible with this code
     * @throws CertsAlreadyExistException if existing certificates are detected in the keystore and forceOverwrite is false.
     * @throws CertsDoNotAlreadyExistException if exportOnly is true, but no existing certs were found
     * @throws KeyStoreException if there is a problem creating a Luna keystore or locating or storing a key with it
     * @throws IOException if there is a problem writing the exported certificates to disk
     * @throws NoSuchAlgorithmException if the certs could not be loaded or exported due to a signature algorithm being missing.
     *                                  Normally, this should not be possible.
     * @throws CertificateException if the new certificates cannot be DER encoded for export.
     *                              Normally, this should not be possible.
     * @throws IllegalArgumentException if exportOnly is requested but neither exportCaCert nor exportSslCert are given.
     */
    public static void makeCerts(String hostname, boolean forceOverwrite, boolean exportOnly, File exportCaCert, File exportSslCert) throws LunaCmu.LunaCmuException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, ClassNotFoundException, LunaCmu.LunaTokenNotLoggedOnException, CertsAlreadyExistException, CertsDoNotAlreadyExistException {
        if (exportOnly) forceOverwrite = true;

        log.info("Checking for Luna Certificate Management Utility (cmu) command... ");
        LunaCmu cmu = new LunaCmu();
        log.info("Connecting to Luna KeyStore... ");
        KeyStore ks = KeyStore.getInstance("Luna");
        ks.load(null, null);

        final X509Certificate oldSslCert = keyExists(ks, SSL_LABEL);
        final X509Certificate oldRootCert = keyExists(ks, CA_LABEL);
        if (oldSslCert != null || oldRootCert != null) {
            if (!forceOverwrite)
                throw new CertsAlreadyExistException("SSG Certificates already present on this KeyStore.\n       Use -f switch to forceOverwrite them to be overwritten.");

            if (exportOnly) {
                if (oldSslCert == null)
                    throw new CertsDoNotAlreadyExistException("Unable to export existing certificates: no SSL cert with alias " + SSL_LABEL + " was found in the current partition");

                if (oldRootCert == null)
                    throw new CertsDoNotAlreadyExistException("Unable to export existing certificates: no CA cert with alias " + CA_LABEL + " was found in the current partition");

                boolean exportedSomething = false;
                if (exportCaCert != null) {
                    writeCertToFile(exportSslCert, oldSslCert);
                    log.info("SSL cert exported to " + exportSslCert.toString());
                    exportedSomething = true;
                }

                if (exportCaCert != null) {
                    writeCertToFile(exportCaCert, oldRootCert);
                    log.info("CA cert exported to " + exportCaCert.toString());
                    exportedSomething = true;
                }

                if (exportedSomething)
                    log.info("Luna certificates successfully exported.");
                else
                    throw new IllegalArgumentException("Unable to export Luna certificates - no export paths were provided");
                return;

            }

            log.info("Deleting existing CA and SSL certificates with labels 'tomcat' or 'ssgroot'...");
            ks.deleteEntry(SSL_LABEL);
            ks.deleteEntry(CA_LABEL);
            LunaCmu.CmuObject[] objs = cmu.list();
            for (int i = 0; i < objs.length; i++) {
                LunaCmu.CmuObject obj = objs[i];
                if (obj.getLabel() != null && (obj.getLabel().startsWith(SSL_LABEL) || obj.getLabel().startsWith(CA_LABEL))) {
                    log.info("  deleting object " + obj);
                    cmu.delete(obj);
                }
            }
        } else {
            if (exportOnly)
                throw new CertsDoNotAlreadyExistException("Unable to export existing certificate because there do not appear to be any existing certificates.");
        }

        // Generate CA certificate
        final LunaCmu.CmuObject caCertObj;
        final X509Certificate rootCertKs;
        {
            final String cn = "root." + hostname;
            log.info("Generating new CA certificate: cn=" + cn);

            LunaCmu.CmuObject caKeyObj = cmu.generateRsaKeyPair(CA_LABEL);
            caCertObj = cmu.generateCaCert(caKeyObj, null, cn);
            X509Certificate rootCert = cmu.exportCertificate(caCertObj);

            rootCertKs = (X509Certificate)ks.getCertificate(CA_LABEL);
            if (!CertUtils.certsAreEqual(rootCert, rootCertKs))
                throw new IllegalStateException("Exported CA cert from CMU differs from CA cert retrieved through Luna KeyStore");

            if (keyExists(ks, CA_LABEL) == null)
                throw new IllegalStateException("Unable to find newly created CA key and cert through Luna KeyStore");

            log.info("Generated and saved a CA certificate under alias \"ssgroot\": " + rootCert.getSubjectDN().toString());

            if (exportCaCert != null) {
                writeCertToFile(exportCaCert, rootCert);
                log.info("CA cert exported to " + exportCaCert.toString());
            }
        }

        // Generate SSL certificate signed by the CA certificate
        {
            log.info("Generating new SSL certificate: cn=" + hostname);

            LunaCmu.CmuObject sslKeyObj = cmu.generateRsaKeyPair(SSL_LABEL);
            byte[] csr = cmu.requestCertificate(sslKeyObj, hostname);
            X509Certificate sslCert = cmu.certify(csr, caCertObj, 365 * 5, 1002, "tomcat--cert0");

            X509Certificate sslCertKs = (X509Certificate)ks.getCertificate(SSL_LABEL);
            if (!CertUtils.certsAreEqual(sslCert, sslCertKs))
                throw new IllegalStateException("Exported SSL cert from CMU differs from SSL cert retrieved through Luna KeyStore");

            if (keyExists(ks, SSL_LABEL) == null)
                throw new IllegalStateException("Unable to find newly recreated SSL key and cert through Luna KeyStore");

            log.info("Generated and saved an SSL certificate under alias \"tomcat\": " + sslCert.getSubjectDN().toString());

            if (exportSslCert != null) {
                writeCertToFile(exportSslCert, sslCert);
                log.info("SSL cert exported to " + exportSslCert.toString());
            }
        }

        log.info("Luna certificate successfully created.");
    }

    private static void writeCertToFile(File exportCaCert, X509Certificate rootCert) throws IOException, CertificateEncodingException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(exportCaCert);
            fos.write(rootCert.getEncoded());
        } finally {
            if (fos != null) //noinspection EmptyCatchBlock
                try { fos.close(); } catch (IOException e) {}
        }
    }

    private static X509Certificate keyExists(KeyStore ks, final String alias) throws NoSuchAlgorithmException, KeyStoreException {
        try {
            if (ks.getKey(alias, null) == null)
                return null;

            Certificate cert = ks.getCertificate(alias);
            if (cert instanceof X509Certificate)
                return (X509Certificate)cert;

            return null;
        } catch (UnrecoverableKeyException kse) {
            return null;
        }
    }
}