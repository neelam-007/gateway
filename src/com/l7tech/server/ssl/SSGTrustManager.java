package com.l7tech.server.ssl;

import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.logging.LogManager;

import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSL trust manager for SSG sub-system.
 *
 * This trust manager uses the trust store of the JRE together with the root cert of the ssg.
 * This is useful for SSL connections between SSG servers that are members of the same cluster
 * (and therefore sharing the same root cert). Because these ssg-ssg connections are using private
 * ip addresses instead of the public host name "ssg.acme.com" it makes a special case for those
 * connections to allow for the ssl handshake to bypass the hostname comparaison.
 *
 * Note: this is not yet plugged in
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 9, 2004<br/>
 * $Id$<br/>
 *
 */
public class SSGTrustManager implements X509TrustManager {

    public static SSGTrustManager initializeTrustManager() {
        X509TrustManager delegateTrustManager = null;
        // todo, get existing trust manager used by the system
        return new SSGTrustManager(delegateTrustManager);
    }

    public X509Certificate[] getAcceptedIssuers() {
        X509Certificate ourcert = getRootCACert();
        X509Certificate[] certs = delegate.getAcceptedIssuers();
        if (ourcert == null) return certs;
        X509Certificate[] output = new X509Certificate[certs.length+1];
        output[certs.length] = ourcert;
        for (int i = 0; i < certs.length; i++) {
            output[i] = certs[i];
        }
        return output;

        /*Collection certs = new ArrayList();
        // add the root ca cert
        try {
            byte[] rootCertBytes = KeystoreUtils.getInstance().readRootCert();
            ByteArrayInputStream bais = new ByteArrayInputStream(rootCertBytes);
            X509Certificate rootCert = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(bais);
            certs.add(rootCert);
        } catch (IOException e) {
            logger.log(Level.WARNING, "error retrieving root ca cert", e);
            return new X509Certificate[0];
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "error retrieving root ca cert", e);
            return new X509Certificate[0];
        }
        // add the JRE certs
        KeyStore jreks = null;
        try {
            jreks = getJREKeystore();
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "error retrieving jre truststore", e);
            jreks = null;
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error retrieving jre truststore", e);
            jreks = null;
        }
        if (jreks != null) {
            Enumeration enum = null;
            try {
                enum = jreks.aliases();
            } catch (KeyStoreException e) {
                logger.log(Level.WARNING, "error getting aliases", e);
                enum = null;
            }
            if (enum != null) {
                while (enum.hasMoreElements()) {
                    try {
                        certs.add(jreks.getCertificate(enum.nextElement().toString()));
                    } catch (KeyStoreException e) {
                        logger.log(Level.WARNING, "error adding certificate", e);
                    }
                }
            }
        }
        // transfer into an array
        X509Certificate[] output = new X509Certificate[certs.size()];
        int i = 0;
        for (Iterator it = certs.iterator(); it.hasNext(); i++) {
            output[i] = (X509Certificate)it.next();
        }
        return output;*/
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        delegate.checkClientTrusted(x509Certificates, s);
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        // give priority to our internal trust
        // onpurpose no name check
        X509Certificate ourrootcert = getRootCACert();
        for (int i = 0; i < x509Certificates.length; i++) {
            X509Certificate cert = x509Certificates[i];
            Principal trustedDN = ourrootcert.getSubjectDN();
            if (cert.getIssuerDN().equals(trustedDN)) {
                try {
                    cert.verify(ourrootcert.getPublicKey());
                    return;
                } catch (Exception e) {
                    continue;
                }
            } else if (cert.getSubjectDN().equals(trustedDN)) {
                if (cert.equals(ourrootcert)) {
                    return;
                }
            }
        }
        // if we can't verify, then pass over to delegate
        delegate.checkServerTrusted(x509Certificates, s);
    }

    private X509Certificate getRootCACert() {
        // add the root ca cert
        try {
            byte[] rootCertBytes = KeystoreUtils.getInstance().readRootCert();
            ByteArrayInputStream bais = new ByteArrayInputStream(rootCertBytes);
            X509Certificate rootCert = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(bais);
            return rootCert;
        } catch (IOException e) {
            logger.log(Level.WARNING, "error retrieving root ca cert", e);
            return null;
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "error retrieving root ca cert", e);
            return null;
        }
    }

    /*private KeyStore getJREKeystore() throws FileNotFoundException, KeyStoreException {
        String jrestorepath = System.getProperty("java.home") +
                              JRE_KS_REL_PATH;
        if ((new File(jrestorepath)).exists()) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream fis = null;
            fis = FileUtils.loadFileSafely(jrestorepath);
            try {
                keyStore.load(fis, JRE_KS_PASSWD.toCharArray());
                fis.close();
            } catch (IOException e) {
                String msg = "could not load jre trusted keystore";
                logger.log(Level.WARNING, msg, e);
                throw new KeyStoreException(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                String msg = "could not load jre trusted keystore";
                logger.log(Level.WARNING, msg, e);
                throw new KeyStoreException(e.getMessage());
            } catch (CertificateException e) {
                String msg = "could not load jre trusted keystore";
                logger.log(Level.WARNING, msg, e);
                throw new KeyStoreException(e.getMessage());
            }
            return keyStore;
        } else {
            throw new FileNotFoundException("no trusted store found");
        }
    }*/

    private SSGTrustManager(X509TrustManager delegate) {
        this.delegate = delegate;

    }

    private X509TrustManager delegate = null;
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    //private static final String JRE_KS_PASSWD = "changeit";
    //private static final String JRE_KS_REL_PATH = "/lib/security/cacerts";
}
