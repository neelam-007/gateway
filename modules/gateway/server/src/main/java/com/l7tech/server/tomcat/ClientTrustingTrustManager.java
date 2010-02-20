package com.l7tech.server.tomcat;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.server.ServerConfig;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * X509TrustManager implementation for Gateway's HTTPS listen ports.
 * <p/>
 * This trust manager always accepts all client certificates.  They will be checked later on, in policy.
 */
class ClientTrustingTrustManager implements X509TrustManager {
    private static final Logger logger = Logger.getLogger(ClientTrustingTrustManager.class.getName());

    private final X509Certificate[] outputTrustedCAs;

    ClientTrustingTrustManager(ServerConfig serverConfig) {
        outputTrustedCAs = loadConfiguredIssuerCerts(serverConfig);
    }

    static X509Certificate[] loadConfiguredIssuerCerts(ServerConfig configs) {
        // set the trusted client CAs
        X509Certificate[] ret = new X509Certificate[0];

        // try to get setting in cluster properties
        if (configs == null) {
            logger.warning("cannot get server config");
            return ret;
        }

        String sslAcceptedClientCA = configs.getProperty("ioHttpsAcceptedClientCa");
        if (sslAcceptedClientCA == null || sslAcceptedClientCA.length() <= 0) {
            logger.info("io.httpsAcceptedClientCa not set or empty");
            return ret;
        }

        Set<X509Certificate> outputCertsInList = new TreeSet<X509Certificate>(new EncodedCertificateComparator());

        String[] acceptedCAsItems = sslAcceptedClientCA.split(",");
        for (String item: acceptedCAsItems) {
            if (item != null && item.length() > 0) {
                try {
                    X509Certificate acert = CertUtils.decodeFromPEM(item);
                    if (outputCertsInList.contains(acert)) {
                        logger.warning("duplicated cert " + acert.getSubjectDN());
                    } else {
                        outputCertsInList.add(acert);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "could not parse pem cert from io.httpsAcceptedClientCa: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } catch (CertificateException e) {
                    logger.log(Level.WARNING, "could not parse pem cert from io.httpsAcceptedClientCa: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } catch (RuntimeException e) {
                    CertificateException ce = ExceptionUtils.getCauseIfCausedBy(e, CertificateException.class);
                    if (ce != null)
                        logger.log(Level.WARNING, "could not encode pem cert from io.httpsAcceptedClientCa: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    else
                        throw e;
                }
            }
        }
        logger.info("Set listener with " + outputCertsInList.size() + " trusted ca certs");
        ret = outputCertsInList.toArray(new X509Certificate[outputCertsInList.size()]);
        return ret;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        try {
            if (x509Certificates != null && x509Certificates.length > 0)
                KeyUsageChecker.requireActivity(KeyUsageActivity.sslClientRemote, x509Certificates[0]);
        } catch (CertificateException e) {
            logger.log(Level.FINE, "Rejecting client certificate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw e;
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        throw new CertificateException("This TrustManager only supports client checks.");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return outputTrustedCAs;
    }

    /**
     * An X509Certificate comparator that compares by the certificate encoded form.
     */
    static class EncodedCertificateComparator implements Comparator<X509Certificate> {
        @Override
            public int compare(X509Certificate a, X509Certificate b) {
            try {
                return ArrayUtils.compareArrays(a.getEncoded(), b.getEncoded());
            } catch (CertificateEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
