/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.ClientProxyTrustManager;
import com.l7tech.proxy.ssl.SslPeer;
import com.l7tech.proxy.util.TokenServiceClient;
import org.w3c.dom.Element;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the strategy for obtaining a SAML token from a third-party WS-Trust server.
 */
public class WsTrustSamlTokenStrategy extends AbstractSamlTokenStrategy {
    private static final Logger log = Logger.getLogger(WsTrustSamlTokenStrategy.class.getName());

    private String wsTrustUrl;
    private String username;
    private char[] password;
    private String appliesTo;
    private String tokenServerCertB64;

    private transient X509Certificate tokenServerCert = null;
    private static final SSLContext SSL_CONTEXT;

    static {
        try {
            SSL_CONTEXT = SSLContext.getInstance("SSL", System.getProperty(SslPeer.PROP_SSL_PROVIDER,
                                                                          SslPeer.DEFAULT_SSL_PROVIDER));
            SSL_CONTEXT.init(null,
                            new X509TrustManager[] {new ClientProxyTrustManager()},
                            null);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e); // shouldn't happen
        } catch (NoSuchProviderException e) {
            throw new Error(e); // shouldn't happen
        } catch (KeyManagementException e) {
            throw new Error(e); // shouldn't happen
        }
    }

    public WsTrustSamlTokenStrategy() {
        super(SecurityTokenType.SAML_AUTHENTICATION, null);
    }

    protected SamlAssertion acquireSamlAssertion()
            throws OperationCanceledException, GeneralSecurityException,
            KeyStoreCorruptException, BadCredentialsException, IOException
    {
        log.log(Level.INFO, "Applying for SAML assertion from WS-Trust server " + wsTrustUrl);
        SamlAssertion s;

        final X509Certificate tokenServerCert = getTokenServerCert();
        final URL url = new URL(wsTrustUrl);
        SslPeer sslPeer = new SslPeer() {
            private X509Certificate handshakeCert = null;

            public X509Certificate getServerCertificate() {
                return tokenServerCert;
            }

            public X509Certificate getClientCertificate() {
                return null;
            }

            public PrivateKey getClientCertificatePrivateKey() {
                return null;
            }

            public String getHostname() {
                return url.getHost();
            }

            public void storeLastSeenPeerCertificate(X509Certificate actualPeerCert) {
                handshakeCert = actualPeerCert;
            }

            public X509Certificate getLastSeenPeerCertificate() {
                return handshakeCert;
            }

            public SSLContext getSslContext() {
                return SSL_CONTEXT;
            }
        };

        UsernameToken usernameToken = new UsernameTokenImpl(getUsername(), getPassword());
        Element utElm = usernameToken.asElement();
        SoapUtil.setWsuId(utElm, SoapUtil.WSU_NAMESPACE, "UsernameToken-1");
        s = TokenServiceClient.obtainSamlAssertion(url,
                                                   sslPeer,
                                                   null,
                                                   null,
                                                   null,
                                                   TokenServiceClient.RequestType.VALIDATE,
                                                   usernameToken,
                                                   getAppliesTo());
        log.log(Level.INFO, "Obtained SAML assertion from WS-Trust server " + wsTrustUrl);
        return s;
    }

    public X509Certificate getTokenServerCert() throws CertificateException {
        if (tokenServerCert == null && tokenServerCertB64 != null && tokenServerCertB64.length() > 0) {
            try {
                tokenServerCert = CertUtils.decodeCert(HexUtils.decodeBase64(tokenServerCertB64, true));
            } catch (IOException e) {
                throw (CertificateException)new CertificateException(
                        "Unable to decode stores Base64 token server certificate: " + e.getMessage()).initCause(e);
            }
        }
        return tokenServerCert;
    }

    public void storeTokenServerCert(X509Certificate tokenServerCert) throws CertificateEncodingException {
        if (tokenServerCert == this.tokenServerCert)
            return;
        this.tokenServerCert = tokenServerCert;
        tokenServerCertB64 = tokenServerCert == null ? null : HexUtils.encodeBase64(tokenServerCert.getEncoded());
    }

    public String getWsTrustUrl() {
        return wsTrustUrl;
    }

    public void setWsTrustUrl(String wsTrustUrl) {
        this.wsTrustUrl = wsTrustUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public String getTokenServerCertB64() {
        return tokenServerCertB64;
    }

    public void setTokenServerCertB64(String tokenServerCertB64) {
        this.tokenServerCertB64 = tokenServerCertB64;
    }
}
