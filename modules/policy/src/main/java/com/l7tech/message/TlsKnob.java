package com.l7tech.message;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Information about a request that arrived over SSL/TLS.
 */
public interface TlsKnob extends TcpKnob {

    /**
     * Get the client certificate that was presented along with this request, if any.
     *
     * @return the X509Certificate chain that was presented in the SSL handshake,
     *         or null if it was wasn't SSL or there was no cert.
     * @throws java.io.IOException if the request contained a certificate type other than X.509
     */
    @Nullable
    X509Certificate[] getClientCertificate() throws IOException;

    /**
     * Check if this request arrived over a secure connection.
     *
     * @return true iff. this request arrived over SSL
     */
    boolean isSecure();
}
