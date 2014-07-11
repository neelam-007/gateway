package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 11/24/11
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class SingleCertX509KeyManager extends X509ExtendedKeyManager {
    private final X509Certificate[] certChain;
    private final PrivateKey privateKey;
    private final String alias;

    public SingleCertX509KeyManager(X509Certificate[] certChain, PrivateKey privateKey) {
        this.certChain = certChain;
        this.privateKey = privateKey;
        this.alias = toString();
    }

    public SingleCertX509KeyManager(X509Certificate[] certChain, PrivateKey privateKey, String alias) {
        this.certChain = certChain;
        this.privateKey = privateKey;
        this.alias = alias;
        if(alias == null || alias.trim().length() < 1) throw new IllegalArgumentException("alias must be non-empty");
    }

    public String[] getClientAliases(String s, Principal[] principals) {
        return new String[] {alias};
    }

    public String chooseClientAlias(String[] strings, Principal[] pricipals, Socket socket) {
        return alias;
    }

    public String[] getServerAliases(String s, Principal[] principals) {
        return new String[] {alias};
    }

    public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
        return alias;
    }

    public X509Certificate[] getCertificateChain(String s) {
        return alias.equals(s) ? certChain : null;
    }

    public PrivateKey getPrivateKey(String s) {
        return alias.equals(s) ? privateKey : null;
    }

    public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
        return alias;
    }

    public String chooseEngineServerAlias(String string, Principal[] principals, SSLEngine sslEngine) {
        return alias;
    }
}
