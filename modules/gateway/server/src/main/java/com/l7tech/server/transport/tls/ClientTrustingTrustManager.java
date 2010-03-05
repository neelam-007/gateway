package com.l7tech.server.transport.tls;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.server.ServerConfig;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * X509TrustManager implementation for Gateway's HTTPS listen ports.
 * <p/>
 * This trust manager always accepts all client certificates.  They will be checked later on, in policy.
 */
public class ClientTrustingTrustManager implements X509TrustManager {
    private static final Logger logger = Logger.getLogger(ClientTrustingTrustManager.class.getName());

    private final Callable<X509Certificate[]> issuersFinder;

    public ClientTrustingTrustManager(final X509Certificate[] outputTrustedCAs) {
        logger.info("Set listener with " + outputTrustedCAs.length + " trusted ca certs");
        this.issuersFinder = new Callable<X509Certificate[]>() {
            @Override
            public X509Certificate[] call() throws Exception {
                return outputTrustedCAs;
            }
        };
    }

    public ClientTrustingTrustManager(Callable<X509Certificate[]> issuersFinder) {
        this.issuersFinder = issuersFinder;
    }

    /**
     * Look up overridden issuer certs from a ServerConfig instance.
     *
     * @param serverConfig severConfig instance to query.  If null, this method logs a warning and immediately returns null.
     * @return the configured issuer certs PEM, or null if not configured.
     */
    public static String getConfiguredIssuerCerts(ServerConfig serverConfig) {
        // try to get setting in cluster properties
        if (serverConfig == null) {
            logger.warning("cannot get server config");
            return null;
        }

        String sslAcceptedClientCA = serverConfig.getProperty("ioHttpsAcceptedClientCa");
        if (sslAcceptedClientCA == null || sslAcceptedClientCA.length() <= 0) {
            logger.fine("io.httpsAcceptedClientCa not set or empty");
            return null;
        }

        return sslAcceptedClientCA;
    }

    /**
     * Parse a list of overridden issuer cert pems out of a comma-delimited string.
     * <p/>
     * This method will catch and log certificate parsing errors, rather than propagating them.
     * If some certificates in the string cannot be parsed, it will attempt to omit them but still
     * try to parse the other certificates.
     *
     * @param sslAcceptedClientCA issuer certs PEMs to parse, comma-delimited.  Must not be null, but may be empty.
     * @return an array of zero or more X509Certificate entries.
     */
    public static X509Certificate[] parseIssuerCerts(String sslAcceptedClientCA) {
        Set<X509Certificate> outputCertsInList = new TreeSet<X509Certificate>(new CertUtils.EncodedCertificateComparator());

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
        return outputCertsInList.toArray(new X509Certificate[outputCertsInList.size()]);
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
        try {
            return issuersFinder.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
