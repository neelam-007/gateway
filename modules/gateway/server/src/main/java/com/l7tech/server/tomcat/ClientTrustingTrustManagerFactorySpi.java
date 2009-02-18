package com.l7tech.server.tomcat;

import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Factory for our client trusting TrustManager
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ClientTrustingTrustManagerFactorySpi extends TrustManagerFactorySpi {

    //- PUBLIC

    public ClientTrustingTrustManagerFactorySpi() {
    }

    //- PROTECTED

    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[]{new ClientTrustingTrustManager()};
    }

    protected void engineInit(KeyStore keyStore) throws KeyStoreException {
    }

    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ClientTrustingTrustManagerFactorySpi.class.getName());

    /**
     *
     */
    private static class ClientTrustingTrustManager implements X509TrustManager {
        private ClientTrustingTrustManager() {
        }

        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            try {
                if (x509Certificates != null && x509Certificates.length > 0)
                    KeyUsageChecker.requireActivity(KeyUsageActivity.sslClientRemote, x509Certificates[0]);
            } catch (CertificateException e) {
                logger.log(Level.FINE, "Rejecting client certificate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                throw e;
            }
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            throw new CertificateException("This TrustManager only supports client checks.");
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public boolean equals(Object obj) {
            boolean equal = false;

            if (obj == this) {
                equal = true;
            }
            else if (obj instanceof ClientTrustingTrustManager) {
                equal = true;
            }

            return equal;
        }

        public int hashCode() {
            return 17 * ClientTrustingTrustManager.class.hashCode();
        }
    }
}
