package com.l7tech.server.transport.http;

import com.l7tech.common.io.CertUtils;
import com.l7tech.server.DefaultKey;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verifies that the peer certificate is identical to SSG SSL certificate.
 * This is for use in internode communication (i.e., direct node-to-node
 * communication without going through cluster load balancer). The purpose is
 * to make sure SSG is not tricked into making an HTTP request to a rogue server.
 *
 * @since SecureSpan 4.3
 * @author rmak
 */
public class InternodeSslClientHostnameVerifier implements HostnameVerifier {

    private static final Logger _logger = Logger.getLogger(InternodeSslClientHostnameVerifier.class.getName());

    private final DefaultKey defaultKey;

    public InternodeSslClientHostnameVerifier(final DefaultKey defaultKey) {
        this.defaultKey = defaultKey;
    }

    /**
     * Verifies that the peer certificate is identical to SSG SSL certificate.
     */
    public boolean verify(String hostname, SSLSession sslSession) {
        boolean verified = false;

        if (sslSession != null) {
            try {
                final Certificate[] certChain = sslSession.getPeerCertificates();
                if (certChain.length > 0 && certChain[0] instanceof X509Certificate) {
                    X509Certificate certificate = (X509Certificate) certChain[0];
                    verified = CertUtils.certsAreEqual(certificate, defaultKey.getSslInfo().getCertificate());
                }
            } catch (Exception e) {
                _logger.log(Level.WARNING, "Could not verify certificate.", e);
            }
        }

        return verified;
    }
}
