/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.xml.TokenServiceClient;
import com.l7tech.common.security.xml.TokenServiceRequestType;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.*;
import com.l7tech.proxy.util.SslUtils;
import org.w3c.dom.Element;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.MalformedURLException;
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
public class WsTrustSamlTokenStrategy extends AbstractSamlTokenStrategy implements Cloneable {
    private static final Logger log = Logger.getLogger(WsTrustSamlTokenStrategy.class.getName());
    private static final SSLContext SSL_CONTEXT;

    // TODO parameterize the HTTP client to use
    private final UrlConnectionHttpClient genericHttpClient = new UrlConnectionHttpClient();

    private String wsTrustUrl;
    private String username;
    private char[] password;
    private String appliesTo;
    private String tokenServerCertB64;

    private transient X509Certificate tokenServerCert = null;

    static {
        try {
            SSL_CONTEXT = SSLContext.getInstance("SSL", System.getProperty(SslPeer.PROP_SSL_PROVIDER,
                                                                          SslPeer.DEFAULT_SSL_PROVIDER));
            SSL_CONTEXT.init(new X509KeyManager[] {new ClientProxyKeyManager()},
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
        super(SecurityTokenType.SAML_ASSERTION, null);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    protected SamlAssertion acquireSamlAssertion()
            throws OperationCanceledException, GeneralSecurityException,
            KeyStoreCorruptException, BadCredentialsException, IOException
    {
        log.log(Level.INFO, "Applying for SAML assertion from WS-Trust server " + wsTrustUrl);
        SamlAssertion s;

        final X509Certificate tokenServerCert = getTokenServerCert();
        final URL url = new URL(wsTrustUrl);

        GenericHttpClient httpClient = new SslPeerHttpClient(genericHttpClient,
                                                             new WsTrustSslPeer(tokenServerCert, url),
                                                             ClientProxySecureProtocolSocketFactory.getInstance());

        UsernameToken usernameToken = new UsernameTokenImpl(getUsername(), getPassword());
        Element utElm = usernameToken.asElement();
        SoapUtil.setWsuId(utElm, SoapUtil.WSU_NAMESPACE, "UsernameToken-1");
        s = TokenServiceClient.obtainSamlAssertion(httpClient, null, url,
                                                   tokenServerCert,
                                                   null, // not overriding timestamp created date
                                                   null, // no client cert (not signing message)
                                                   null, // no client private key (not signing message)
                                                   TokenServiceRequestType.VALIDATE,
                                                   null, // no token type (FIM doesn't like it)
                                                   usernameToken,
                                                   getAppliesTo(),
                                                   false);
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

    /**
     * If an SSL exception occurred while connecting to the token server, this will check if the problem
     * is an untrusted server certificate and, if so, ask the user if they wish to trust it.  If they do,
     * it will be saved within this strategy.
     * <p>
     * If this returns, the server cert will have been imported into this strategy, and the caller should
     * retry the original operation.
     *
     * @param sslPeer  the SslPeer that was active when the SSLException was caught.  This must not be null.
     *                 Will be expected to identify this WS-Trust server.
     * @throws SSLException if we couldn't handle the exception
     * @throws OperationCanceledException if the user cancels the certificate dialog, or declines to trust the certificate
     * @throws CertificateEncodingException if there is a problem with the certificate
     * @param e the SSLException we are to attempt to handle
     */
    public void handleSslException(SslPeer sslPeer, Exception e)
            throws SSLException, OperationCanceledException, CertificateEncodingException
    {
        if (!(sslPeer instanceof WsTrustSslPeer))
            throw (SSLException)new SSLException("SSL connection failure, but third-party WS-Trust was not the SSL peer: " + e.getMessage()).initCause(e);

        String wstHostname = getWsTrustUrl();
        try {
            wstHostname = new URL(wstHostname).getHost();
        } catch (MalformedURLException e1) {
            // fallthrough
        }
        if (wstHostname == null) wstHostname = "";
        if (!wstHostname.equals(sslPeer.getHostname()))
            throw (SSLException)new SSLException("SSL connection failure, but third-party WS-Trust was not the SSL peer: " + e.getMessage()).initCause(e);

        final String serverName = "the WS-Trust server " + wstHostname;
        SslUtils.handleServerCertProblem(sslPeer, serverName, e);

        // Import the peer certificate
        final X509Certificate peerCert = sslPeer.getLastSeenPeerCertificate();
        if (peerCert == null)  // can't happen
            throw (SSLException)new SSLException("SSL connection failure, but no peer certificate presented: " + e.getMessage()).initCause(e);

        // Check if the user wants to trust this peer certificate
        if (sslPeer instanceof Ssg)
            ((Ssg)sslPeer).getRuntime().getCredentialManager().notifySslCertificateUntrusted(sslPeer, serverName, peerCert);
        else
            Managers.getCredentialManager().notifySslCertificateUntrusted(sslPeer, serverName, peerCert);

        // They do; import it
        storeTokenServerCert(peerCert);
    }

    private static class WsTrustSslPeer implements SslPeer {
        private X509Certificate handshakeCert = null;
        private final X509Certificate tokenServerCert;
        private final URL url;

        public WsTrustSslPeer(X509Certificate tokenServerCert, URL url) {
            this.tokenServerCert = tokenServerCert;
            this.url = url;
        }

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
    }
}
