/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * 
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.wstrust.TokenServiceClient;
import com.l7tech.common.security.wstrust.WsTrustConfig;
import com.l7tech.common.security.wstrust.WsTrustConfigFactory;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.EncryptionUtil;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.common.xml.WsTrustRequestType;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.ssl.*;
import com.l7tech.proxy.util.SslUtils;
import com.l7tech.proxy.gui.dialogs.LogonDialog;
import com.l7tech.proxy.gui.Gui;

import org.w3c.dom.Element;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.PasswordAuthentication;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;

/**
 * This is the strategy for obtaining a SAML token from a third-party WS-Trust server.
 */
public class WsTrustSamlTokenStrategy extends FederatedSamlTokenStrategy implements Cloneable {
    private static final Logger log = Logger.getLogger(WsTrustSamlTokenStrategy.class.getName());
    private static final SSLContext SSL_CONTEXT;

    /**
     *
     */
    private static final String LOGON_DIALOG_TITLE = "Log On to Trust server";
    private static final String LOGON_LABEL_TEXT = "for the Trust server:";

    // TODO parameterize the HTTP client to use
    private final UrlConnectionHttpClient genericHttpClient = new UrlConnectionHttpClient();

    private String wsTrustUrl;
    private String username;
    private char[] password;
    private String appliesTo;
    private String wstIssuer;
    private String tokenServerCertB64;
    private String requestType = WsTrustRequestType.ISSUE.getUri();

    private transient X509Certificate tokenServerCert = null;
    private transient String encryptedPassword = null;

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

    protected SamlAssertion acquireSamlAssertion(Ssg ssg)
            throws OperationCanceledException, GeneralSecurityException,
            KeyStoreCorruptException, BadCredentialsException, IOException
    {
        log.log(Level.INFO, "Applying for SAML assertion from WS-Trust server " + wsTrustUrl);
        SamlAssertion s = null;

        final X509Certificate tokenServerCert = getTokenServerCert();
        final URL url = new URL(wsTrustUrl);

        WsTrustSslPeer sslPeer = new WsTrustSslPeer(tokenServerCert, url);
        GenericHttpClient httpClient = new SslPeerHttpClient(genericHttpClient,
                                                             sslPeer,
                                                             ClientProxySecureProtocolSocketFactory.getInstance());

        UsernameToken usernameToken = new UsernameTokenImpl(getUsername(), password());
        Element utElm = usernameToken.asElement();
        SoapUtil.setWsuId(utElm, SoapUtil.WSU_NAMESPACE, "UsernameToken-1");
        try {
            while(s==null) {
                try {
                    WsTrustConfig wstConfig = WsTrustConfigFactory.getDefaultWsTrustConfig();
                    TokenServiceClient tokenServiceClient = new TokenServiceClient(wstConfig, httpClient);

                    s = tokenServiceClient.obtainSamlAssertion(null, url,
                                                               tokenServerCert,
                                                               null, // not overriding timestamp created date
                                                               null, // no client cert (not signing message)
                                                               null, // no client private key (not signing message)
                                                               WsTrustRequestType.fromString(getRequestType()),
                                                               null, // no token type (FIM doesn't like it)
                                                               usernameToken,
                                                               getAppliesTo(),
                                                               getWstIssuer(),
                                                               false);
                }
                catch(CausedIOException cioe) {
                    Throwable cause = cioe.getCause();
                    if(cause instanceof InvalidDocumentFormatException) {
                        if(cause.getMessage() != null && cause.getMessage().startsWith("Unexpected SOAP fault from token service: p383:FailedAuthentication:")) {
                            if(ssg.getRuntime().promptForUsernameAndPassword()) {
                                final String host = url.getHost();
                                final Collection cc = new ArrayList(1);
                                invokeOnSwingThread(new Runnable(){public void run(){
                                    cc.add(LogonDialog.logon(Gui.getInstance().getFrame(),LOGON_DIALOG_TITLE,LOGON_LABEL_TEXT,host,getUsername(),false,false,""));
                                }});
                                if(!cc.isEmpty()) {
                                    PasswordAuthentication pa = (PasswordAuthentication) cc.iterator().next();
                                    if(pa!=null) { //TODO if implementing (optional) persistent password for wstrust federation then save here
                                        setUsername(pa.getUserName());
                                        storePassword(pa.getPassword());
                                        usernameToken = new UsernameTokenImpl(getUsername(), password());
                                        continue;
                                    }
                                    else if (ssg.getRuntime().incrementNumTimesLogonDialogCanceled() >= SsgRuntime.MAX_LOGON_CANCEL) {
                                        ssg.getRuntime().promptForUsernameAndPassword(false);
                                    }
                                    
                                }
                            }
                        }
                    }
                    throw cioe;
                }
            }
        } catch (TokenServiceClient.UnrecognizedServerCertException e) {
            CurrentSslPeer.set(sslPeer);
            throw new ServerCertificateUntrustedException(e);
        }
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
            encrypted = EncryptionUtil.encrypt(new String(password), username);
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

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public String getWstIssuer() {
        return wstIssuer;
    }

    public void setWstIssuer(String wstIssuer) {
        this.wstIssuer = wstIssuer;
    }

    public String getTokenServerCertB64() {
        return tokenServerCertB64;
    }

    public void setTokenServerCertB64(String tokenServerCertB64) {
        this.tokenServerCertB64 = tokenServerCertB64;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
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
     * @param e the SSLException we are to attempt to handle
     * @throws SSLException if we couldn't handle the exception
     * @throws OperationCanceledException if the user cancels the certificate dialog, or declines to trust the certificate
     * @throws CertificateEncodingException if there is a problem with the certificate
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
        Managers.getCredentialManager().notifySslCertificateUntrusted(sslPeer, serverName, peerCert);

        // They do; import it
        storeTokenServerCert(peerCert);
    }

    /**
     * Invoke the specified runnable and wait for it to finish.  If this is the event dispatch thread,
     * just runs it; otherwise uses SwingUtilities.invokeAndWait()
     * @param runnable  code that displays a modal dialog
     */
    private void invokeOnSwingThread(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                log.log(Level.WARNING, "Thread interrupted; reasserting interrupt and continuing");
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                log.log(Level.WARNING, "Dialog code threw an exception; continuing", e);
            }
        }
    }

    /**
     *
     */
    private void checkDecrypt() {
        if(encryptedPassword!=null && encryptedPassword.length()>0
        && username!=null && username.length()>0) {
            try {
                password = EncryptionUtil.decrypt(encryptedPassword, username).toCharArray();
                encryptedPassword = null;
            }
            catch(IllegalArgumentException iae) {
            }
        }
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
