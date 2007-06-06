package com.l7tech.common.security;

import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.SSLEngine;
import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.Principal;
import java.net.Socket;

/**
 * An SSL X509KeyManager that holds a single preconfigured client cert and private key, and always uses
 * it to respond to challenges.
 */
public class SingleCertX509KeyManager extends X509ExtendedKeyManager {
    private final X509Certificate[] clientCertChain;
    private final PrivateKey clientKey;
    private final String alias;

    /**
     * Create a a KeyManager that only knows about a single X.509 certificate.
     *
     * @param clientCertChain the client certificate chain to present when challenged.  Required.
     * @param clientKey  the private key for this client certificate.  Required.
     */
    public SingleCertX509KeyManager(X509Certificate[] clientCertChain, PrivateKey clientKey) {
        this.clientCertChain = clientCertChain;
        this.clientKey = clientKey;
        this.alias = toString();
    }

    /**
     * Create a KeyManager that only knows about a single X.509 certificate, and refers to it
     * as the specified alias.
     *
     * @param clientCertChain the client certificate chain to present when challenged.  Required.
     * @param clientKey  the private key for this client certificate.  Required.
     * @param alias      the alias to return from chooseClientAlias() and chooseServerAlias().  Must be non-empty.
     */
    public SingleCertX509KeyManager(X509Certificate[] clientCertChain, PrivateKey clientKey, String alias) {
        this.clientCertChain = clientCertChain;
        this.clientKey = clientKey;
        this.alias = alias;
        if (alias == null || alias.trim().length() < 1) throw new IllegalArgumentException("alias must be non-empty");
    }

    public String[] getClientAliases(String s, Principal[] principals) {
        return new String[] { alias };
    }

    public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
        return alias;
    }

    public String[] getServerAliases(String s, Principal[] principals) {
        return new String[] { alias };
    }

    public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
        return alias;
    }

    public X509Certificate[] getCertificateChain(String s) {
        return alias.equals(s) ? clientCertChain : null;
    }

    public PrivateKey getPrivateKey(String s) {
        return alias.equals(s) ? clientKey : null;
    }

    public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
        return alias;
    }

    public String chooseEngineServerAlias(String string, Principal[] principals, SSLEngine sslEngine) {
        return alias;
    }
}
