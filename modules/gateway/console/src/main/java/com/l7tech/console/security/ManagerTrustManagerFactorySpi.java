package com.l7tech.console.security;

import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Factory for our TrustManager
 *
 * <p>This works just like the standard one, but reloads the trust store on failure.</p>
 *
 * @author Steve Jones, $Author: steve $
 */
public class ManagerTrustManagerFactorySpi extends TrustManagerFactorySpi {

    //- PUBLIC

    public ManagerTrustManagerFactorySpi() {
    }

    //- PROTECTED

    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[]{new ManagerTrustManagerFactorySpi.ClientTrustingTrustManager()};
    }

    protected void engineInit(final KeyStore keyStore) throws KeyStoreException {
    }

    protected void engineInit(final ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ManagerTrustManagerFactorySpi.class.getName());

    /**
     *
     */
    private static class ClientTrustingTrustManager implements X509TrustManager {
        private X509TrustManager trustManager;

        private ClientTrustingTrustManager() {
            trustManager = getX509TrustManager();
        }

        private static X509TrustManager getX509TrustManager() {
            X509TrustManager trustManager = null;

            try {
                if (logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, "Creating TrustManager instance.");

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
                tmf.init((KeyStore)null);

                for (TrustManager tm : tmf.getTrustManagers()) {
                    if (tm instanceof X509TrustManager) {
                        trustManager = (X509TrustManager) tm;
                        break;
                    }
                }
            }
            catch(Exception nsae) {
                throw new RuntimeException("Missing standard PKIX trust manager", nsae);
            }

            if (trustManager == null)
                throw new RuntimeException("Missing standard PKIX trust manager");

            return trustManager;
        }

        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            throw new CertificateException("This TrustManager only supports server checks.");
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "Checking server trusted.");

            X509TrustManager xtm = trustManager;
            if (xtm == null) {
                xtm = getX509TrustManager();
                trustManager = xtm;
            }

            if (x509Certificates != null && x509Certificates.length > 0)
                KeyUsageChecker.requireActivity(KeyUsageActivity.sslServerRemote, x509Certificates[0]);

            try {
                xtm.checkServerTrusted(x509Certificates, s);
            }
            catch(RuntimeException re) {
                if( ExceptionUtils.causedBy(re, InvalidAlgorithmParameterException.class)) {
                    // This occurs when the trustStore is empty
                    if (logger.isLoggable(Level.INFO))
                        logger.log(Level.INFO, "Trust check failed, reloading trust store.");
                    xtm = getX509TrustManager();
                    trustManager = xtm;
                    try {
                        xtm.checkServerTrusted(x509Certificates, s);
                    }
                    catch(RuntimeException re2) {
                        if( ExceptionUtils.causedBy(re2, InvalidAlgorithmParameterException.class)) {
                            throw new CertificateException("Certificate not trusted");
                        }
                    }
                }
            }
            catch(CertificateException ce) {
                if (logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO, "Trust check failed, reloading trust store.");
                xtm = getX509TrustManager();
                trustManager = xtm;
                xtm.checkServerTrusted(x509Certificates, s);
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            X509TrustManager xtm = trustManager;
            if (xtm == null) {
                xtm = getX509TrustManager();
                trustManager = xtm;
            }

            return xtm.getAcceptedIssuers();
        }

        public boolean equals(Object obj) {
            boolean equal = false;

            if (obj == this) {
                equal = true;
            }
            else if (obj instanceof ManagerTrustManagerFactorySpi.ClientTrustingTrustManager) {
                equal = true;
            }

            return equal;
        }

        public int hashCode() {
            return 17 * ManagerTrustManagerFactorySpi.ClientTrustingTrustManager.class.hashCode();
        }
    }
}
