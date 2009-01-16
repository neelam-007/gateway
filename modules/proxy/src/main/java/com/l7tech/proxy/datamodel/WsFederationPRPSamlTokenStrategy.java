package com.l7tech.proxy.datamodel;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.wsfederation.FederationPassiveClient;
import com.l7tech.security.wsfederation.InvalidTokenException;
import com.l7tech.security.wsfederation.ResponseStatusException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.EncryptionUtil;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.*;
import com.l7tech.proxy.util.SslUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Strategy for obtaining a SAML token from a third party WS-Federation server using
 * the Passive Request Profile.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsFederationPRPSamlTokenStrategy extends FederatedSamlTokenStrategy implements Cloneable {

    //- PUBLIC

    public WsFederationPRPSamlTokenStrategy() {
        super(SecurityTokenType.SAML_ASSERTION, null);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public String getIpStsUrl() {
        return ipStsUrl;
    }

    public void setIpStsUrl(String ipStsUrl) {
        this.ipStsUrl = ipStsUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getReplyUrl() {
        return replyUrl;
    }

    public void setReplyUrl(String replyUrl) {
        this.replyUrl = replyUrl;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public boolean isTimestamp() {
        return addTimestamp;
    }

    public void setTimestamp(boolean timestamp) {
        this.addTimestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        checkDecrypt();
    }

    /**
     * @deprecated use password/storePassword methods (leave this for xml file config)
     */
    public char[] getPassword() {
        return new char[0];
    }

    /**
     * @deprecated use password/storePassword methods (leave this for xml file config)
     */
    public void setPassword(char[] password) {
        this.password = password;
    }

    public String getEncryptedPassword() {
        String encrypted = "";

        if(password!=null && password.length>0 && username!=null && username.length()>0) {
            encrypted = EncryptionUtil.obfuscate(new String(password), username);
        }

        return encrypted;
    }

    public void setEncryptedPassword(String password) {
        if(password!=null && password.length()>0) {
            encryptedPassword = password;
            checkDecrypt();
        }
    }

    /**
     * Get the password in clear text.
     *
     * <p>Note: this method is not a bean style accessor to avoid a clear text
     * password in the config file.</p>
     *
     * @return the password
     */
    public char[] password() {
        return password;
    }

    /**
     * Set the password in clear text.
     *
     * @see #password()
     */
    public void storePassword(char[] password) {
        this.password = password;
    }

    public String getTokenServerCertB64() {
        return tokenServerCertB64;
    }

    public void setTokenServerCertB64(String tokenServerCertB64) {
        this.tokenServerCertB64 = tokenServerCertB64;
    }

    public void handleSslException(SslPeer sslPeer, Exception e) throws SSLException, OperationCanceledException, CertificateEncodingException {
        if (!(sslPeer instanceof WsFederationSslPeer))
            throw newSSLException("SSL connection failure, but third-party WS-Federation was not the SSL peer: " + e.getMessage(), e);

        String hostname = getIpStsUrl();
        try {
            hostname = new URL(hostname).getHost();
        } catch (MalformedURLException e1) {
            // fallthrough
        }
        if (hostname == null) hostname = "";
        if (!hostname.equals(sslPeer.getHostname()))
            throw newSSLException("SSL connection failure, but third-party WS-Federation was not the SSL peer: " + e.getMessage(), e);

        final String serverName = "the WS-Federation server " + hostname;
        SslUtils.handleServerCertProblem(sslPeer, serverName, e);

        // Import the peer certificate
        final X509Certificate peerCert = sslPeer.getLastSeenPeerCertificate();
        if (peerCert == null)  // can't happen
            throw newSSLException("SSL connection failure, but no peer certificate presented: " + e.getMessage(), e);

        // Check if the user wants to trust this peer certificate
        Managers.getCredentialManager().notifySslCertificateUntrusted(sslPeer, serverName, peerCert);

        // They do; import it
        storeTokenServerCert(peerCert);
    }

    public X509Certificate getTokenServerCert() throws CertificateException {
        if (tokenServerCert == null && tokenServerCertB64 != null && tokenServerCertB64.length() > 0) {
            tokenServerCert = CertUtils.decodeCert(HexUtils.decodeBase64(tokenServerCertB64, true));
        }
        return tokenServerCert;
    }

    public void storeTokenServerCert(X509Certificate tokenServerCert) throws CertificateEncodingException {
        if (tokenServerCert == this.tokenServerCert)
            return;
        this.tokenServerCert = tokenServerCert;
        tokenServerCertB64 = tokenServerCert == null ? null : HexUtils.encodeBase64(tokenServerCert.getEncoded());
    }

    //- PROTECTED

    protected SamlAssertion acquireSamlAssertion(Ssg ssg) throws OperationCanceledException, GeneralSecurityException, KeyStoreCorruptException, BadCredentialsException, IOException {
        final SamlAssertion samlAssertion;

        URL url = new URL(ipStsUrl);
        GenericHttpRequestParams params = new GenericHttpRequestParams(url);
        params.setSslSocketFactory(SSL_CONTEXT.getSocketFactory());
        params.setPasswordAuthentication(new PasswordAuthentication(username,password));

        X509Certificate tokenServerCert = getTokenServerCert();

        GenericHttpClient httpClient = new SslPeerHttpClient(genericHttpClient,
                                                             new WsFederationSslPeer(tokenServerCert, url),
                                                             ClientProxySecureProtocolSocketFactory.getInstance());

        XmlSecurityToken token = null;
        while(token == null) {
            try {
                token = FederationPassiveClient.obtainFederationToken(httpClient, params, realm, replyUrl, context, addTimestamp);
            } catch(ResponseStatusException rse) {
                if(rse.getStatus() == HttpConstants.STATUS_UNAUTHORIZED) {
                    final String host = url.getHost();
                    PasswordAuthentication pw = ssg.getRuntime().getCredentialManager().getAuxiliaryCredentials(ssg, getType(), host, CredentialManager.ReasonHint.TOKEN_SERVICE, false);
                    if(pw != null) {
                        //TODO if implementing (optional) persistent password for federated ssg then save here
                        setUsername(pw.getUserName());
                        storePassword(pw.getPassword());
                        params.setPasswordAuthentication(pw);
                        continue;
                    }
                    throw rse;
                }
                else throw rse;
            }
        }
        if(token instanceof SamlAssertion) {
            samlAssertion = (SamlAssertion) token;
        }
        else {
            throw new InvalidTokenException("Unsupported token type");
        }

        return samlAssertion;
    }

    //- PRIVATE

    /**
     * SSL context to use for HTTP requests.
     */
    private static final SSLContext SSL_CONTEXT;

    /**
     * Static init
     */
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

    private final UrlConnectionHttpClient genericHttpClient = new UrlConnectionHttpClient();

    private String ipStsUrl;
    private String realm;
    private String replyUrl;
    private String context;
    private boolean addTimestamp;
    private String username;
    private char[] password;

    private String tokenServerCertB64;
    private transient X509Certificate tokenServerCert = null;
    private transient String encryptedPassword = null;

    private SSLException newSSLException(String message, Throwable cause) {
        SSLException ssle = new SSLException(message);
        ssle.initCause(cause);
        return ssle;
    }

    /**
     *
     */
    private void checkDecrypt() {
        if(encryptedPassword!=null && encryptedPassword.length()>0
        && username!=null && username.length()>0) {
            try {
                password = EncryptionUtil.deobfuscate(encryptedPassword, username).toCharArray();
                encryptedPassword = null;
            }
            catch(IllegalArgumentException iae) {
            }
        }
    }

    private static class WsFederationSslPeer implements SslPeer {
        private X509Certificate handshakeCert = null;
        private final X509Certificate tokenServerCert;
        private final URL url;

        public WsFederationSslPeer(X509Certificate tokenServerCert, URL url) {
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
