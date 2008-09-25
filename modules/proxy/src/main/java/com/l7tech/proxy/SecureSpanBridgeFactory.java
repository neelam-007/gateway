/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.proxy.ssl.SslPeer;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Obtain a SecureSpanBridge implementation.
 *
 * @author mike
 * @version 1.0
 */
public class SecureSpanBridgeFactory {
    protected static final Logger logger = Logger.getLogger(SecureSpanBridgeFactory.class.getName());
    public static final String PROPERTY_MESSAGE_INTERCEPTOR = "com.l7tech.proxy.messageInterceptor";

    private static class CausedSendException extends SecureSpanBridge.SendException {
        CausedSendException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    private static class CausedBadCredentialsException extends SecureSpanBridge.BadCredentialsException {
        CausedBadCredentialsException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    private static class CausedCertificateAlreadyIssuedException extends SecureSpanBridge.CertificateAlreadyIssuedException {
        CausedCertificateAlreadyIssuedException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    private static Map<SslPeer, CredentialManager> credentialManagerMap = Collections.synchronizedMap(new HashMap<SslPeer, CredentialManager>());
    static {
        Managers.setCredentialManager(new CredentialManagerImpl() {
            public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException 
            {
                // TODO if this is a third-party token service, can't look up the SSG
                if (sslPeer == null)
                    throw new OperationCanceledException("Unable to determine trustworthiness of non-Gateway peer certificate");
                CredentialManager cm = credentialManagerMap.get(sslPeer);
                if (cm != null)
                    cm.notifySslCertificateUntrusted(sslPeer, "the Gateway " + sslPeer, untrustedCertificate);
                else
                    super.notifySslCertificateUntrusted(sslPeer, "the Gateway " + sslPeer, untrustedCertificate);
            }
        });
    }

    /**
     * Ensure that the default config directory (~/.l7tech by default) exists by creating it if necessary.
     */
    private static void ensureDefaultConfigDirectoryExists() {
        new Ssg().getKeyStoreFile().getParentFile().mkdirs();
    }

    /**
     * Create a new SecureSpanBridge with the specified settings.  The Bridge will be configured to forward messages
     * to the specified Gateway, using the specified credentials.
     *
     * @param options the configuration to use for the new SecureSpanBridge instance
     * @return the newly-created SecureSpanBridge instance
     */
    public static SecureSpanBridge createSecureSpanBridge(SecureSpanBridgeOptions options) {
        final Ssg ssg = new Ssg(options.getId(), options.getGatewayHostname());
        final PasswordAuthentication pw;
        if (options.getUsername() != null) {
            final char[] pass = options.getPassword() == null ? new char[0] : options.getPassword().clone();
            pw = new PasswordAuthentication(options.getUsername(), pass);
            ssg.setUsername(pw.getUserName());
            ssg.getRuntime().setCachedPassword(pw.getPassword());
        } else
            pw = null;
        if (options.getGatewayPort() != 0)
            ssg.setSsgPort(options.getGatewayPort());
        if (options.getGatewaySslPort() != 0)
            ssg.setSslPort(options.getGatewaySslPort());

        // Bug #4165 - if keystore path is defaulting, ensure default config directory gets created
        if (options.getKeyStorePath() == null) {
            ensureDefaultConfigDirectoryExists();
        } else {
            ssg.setKeyStoreFile(new File(options.getKeyStorePath()));
        }

        // Bug #4165 - if cert store path is defaulting, ensure default config directory gets created
        if (options.getCertStorePath() == null) {
            ensureDefaultConfigDirectoryExists();
        } else {
            ssg.setTrustStoreFile(new File(options.getCertStorePath()));
        }

        if (options.getUseSslByDefault() != null) {
            //noinspection UnnecessaryUnboxing
            ssg.setUseSslByDefault(options.getUseSslByDefault().booleanValue());
        }
        if (options.getTrustedGateway() != null) {
            SecureSpanBridgeImpl trustedBridge = (SecureSpanBridgeImpl)options.getTrustedGateway();
            ssg.setTrustedGateway(trustedBridge.getSsg());
        }
        if (options.getGatewayCertificateTrustManager() != null) {
            final SecureSpanBridgeOptions.GatewayCertificateTrustManager tm = options.getGatewayCertificateTrustManager();
            final String msgNoTrust = "Bridge API user's GatewayCertificateTrustManager rejected Gateway certificate";
            credentialManagerMap.put(ssg, new CredentialManagerImpl() {
                public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException {
                    try {
                        if (tm.isGatewayCertificateTrusted(new X509Certificate[] {untrustedCertificate}))
                            return;
                    } catch (CertificateException e) {
                        throw new OperationCanceledException(msgNoTrust, e);
                    }
                    throw new OperationCanceledException(msgNoTrust);
                }
            });
            final CredentialManager oldcm = ssg.getRuntime().getCredentialManager();
            ssg.getRuntime().setCredentialManager(new DelegatingCredentialManager(oldcm) {
                public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException {
                    try {
                        if (tm.isGatewayCertificateTrusted(new X509Certificate[] {untrustedCertificate}))
                            return;
                    } catch (CertificateException e) {
                        throw new OperationCanceledException(msgNoTrust, e);
                    }
                    throw new OperationCanceledException(msgNoTrust);
                }
            });
        }
        final MessageProcessor mp = new MessageProcessor();
        final RequestInterceptor nri = getRequestInterceptor();
        return new SecureSpanBridgeImpl(ssg, nri, mp, pw);
    }

    private static RequestInterceptor getRequestInterceptor() {
        String interceptorClassname = SyspropUtil.getString(PROPERTY_MESSAGE_INTERCEPTOR, NullRequestInterceptor.class.getName());

        try {
            return (RequestInterceptor)Class.forName(interceptorClassname).newInstance();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to instantiate configured message interceptor " + interceptorClassname + ": " + ExceptionUtils.getMessage(e) +
                    "\n Will use NullRequestInterceptor", e);
            return NullRequestInterceptor.INSTANCE;
        }
    }

    static class SecureSpanBridgeImpl implements SecureSpanBridge {
        private final Ssg ssg;
        private final RequestInterceptor nri;
        private final MessageProcessor mp;
        private final PasswordAuthentication pw;
        private final PolicyManager originalPolicyManager;

        private String localUri = "/";

        SecureSpanBridgeImpl(Ssg ssg, RequestInterceptor nri, MessageProcessor mp, PasswordAuthentication pw) {
            this.ssg = ssg;
            this.nri = nri;
            this.mp = mp;
            this.pw = pw;
            originalPolicyManager = ssg.getRuntime().getPolicyManager();
        }

        Ssg getSsg() {
            return ssg;
        }

        RequestInterceptor getRequestInterceptor() {
            return nri;
        }

        MessageProcessor getMessageProcessor() {
            return mp;
        }

        public void setStaticPolicy(String policyXml) throws SAXException {
            if (policyXml == null) {
                ssg.getRuntime().setPolicyManager(originalPolicyManager);
                return;
            }
            ssg.getRuntime().setPolicyManager(getStaticPolicyManager(policyXml));
        }

        private static Map<String, PolicyManager> policyManagerCache = Collections.synchronizedMap(new WeakHashMap<String, PolicyManager>());
        private PolicyManager getStaticPolicyManager(String policyXml) throws SAXException {
            if (policyXml == null) throw new NullPointerException();
            try {
                PolicyManager staticPolicyManager = policyManagerCache.get(policyXml);
                if (staticPolicyManager != null)
                    return staticPolicyManager;
                Assertion rootAssertion = WspReader.getDefault().parsePermissively(policyXml);
                Policy policy = new Policy(rootAssertion, null);
                staticPolicyManager = new StaticPolicyManager(policy);
                policyManagerCache.put(policyXml, staticPolicyManager);
                return staticPolicyManager;
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }

        public SecureSpanBridge.Result send(String soapAction, Document message) throws SendException, IOException, CausedBadCredentialsException, CausedCertificateAlreadyIssuedException {
            final URL origUrl = new URL("http://bridge-api-uri.layer7tech.com" + localUri);
            // TODO if request uses multiple different payload URIs at the same time, we should probably just fail 
            QName[] names = SoapUtil.getPayloadNames(message);
            String nsUri = names == null || names.length < 1 ? null : names[0].getNamespaceURI();
            PolicyAttachmentKey pak = new PolicyAttachmentKey(nsUri, soapAction, origUrl.getFile());
            Message request = new Message();
            request.initialize(message);
            Message response = new Message();
            PolicyApplicationContext context = null;
            try {
                context = new PolicyApplicationContext(ssg, request, response, nri, pak, origUrl);
                mp.processMessage(context);
                // Copy results out before context gets closed
                final HttpResponseKnob responseHttp = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
                final int httpStatus = responseHttp != null ? responseHttp.getStatus() : 500;
                // MimeResult can't use context, request, or response because they are closed by the time
                // the user calls any methods on it, so we'll save the DOM if we have one otherwise
                // we'll save the MIME body bytes (Bug #4166)
                final boolean isXml = response.isXml();
                final Document responseDoc = isXml ? response.getXmlKnob().getDocumentReadOnly() : null;
                final byte[] responseBytes = isXml ? null : getResponseBytes(response);
                final String responseContentType = response.getMimeKnob().getOuterContentType().getFullValue();
                final long responseContentLength = response.getMimeKnob().getContentLength();

                return new MimeResult() {
                    public int getHttpStatus() {
                        return httpStatus;
                    }

                    public Document getResponse() throws SAXException, IOException {
                        if (responseDoc == null)
                            throw new SAXException("Response is not XML");
                        return responseDoc;
                    }

                    public boolean isResponseXml() throws IOException {
                        return isXml;
                    }

                    public String getContentType() throws IOException {
                        return responseContentType;
                    }

                    public long getContentLength() throws IOException {
                        return responseContentLength;
                    }

                    public InputStream getResponseStream() throws IOException {
                        // Someday we'll find a way to properly support streaming large responses (and maybe attachments)
                        // with the Bridge API
                        return new ByteArrayInputStream(getResponseBytes());
                    }

                    public byte[] getResponseBytes() throws IOException {
                        if (responseBytes != null)
                            return responseBytes;
                        if (responseDoc != null)
                            return XmlUtil.nodeToString(responseDoc).getBytes("UTF-8");
                        throw new IllegalStateException("Response has neither bytes nor a Document"); // can't happen
                    }
                };
            } catch (com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException e) {
                throw new CausedCertificateAlreadyIssuedException(e);
            } catch (ClientCertificateException e) {
                throw new CausedSendException(e);
            } catch (CredentialsUnavailableException e) {
                throw new CausedBadCredentialsException(e);
            } catch (OperationCanceledException e) {
                throw new CausedSendException(e);
            } catch (ConfigurationException e) {
                throw new CausedSendException(e);
            } catch (GeneralSecurityException e) {
                throw new CausedSendException(e);
            } catch (IOException e) {
                throw new CausedSendException(e);
            } catch (SAXException e) {
                throw new CausedSendException(e); // can't happen -- no parsing of request in embedded mode
            } catch (ResponseValidationException e) {
                throw new CausedSendException(e);
            } catch (HttpChallengeRequiredException e) {
                throw new CausedSendException(e); // can't happen -- no HTTP credential chaining in embedded mode
            } catch (PolicyAssertionException e) {
                throw new CausedSendException(e);
            } catch (InvalidDocumentFormatException e) {
                throw new CausedSendException(e);
            } catch (ProcessorException e) {
                throw new CausedSendException(e);
            } catch (PolicyLockedException e) {
                throw new CausedSendException(e);
            } catch (NoSuchPartException e) {
                throw new CausedSendException(e); // can't happen -- currently no parts are ever read destructively
            } finally {
                ResourceUtils.closeQuietly(context);
                CurrentSslPeer.clear();
            }
        }

        /**
         * Get the bytes of the complete body of the specified response.
         *
         * @param response  the response to examine. Must have a MimeKnob.
         * @return the bytes of the entire message body.
         * @throws IOException if there was a problem reading from the message stream
         * @throws NoSuchPartException if any part's body is unavailable, e.g. because it was read destructively
         */
        private byte[] getResponseBytes(Message response) throws NoSuchPartException, IOException {
            InputStream stream = response.getMimeKnob().getEntireMessageBodyAsInputStream();
            try {
                return IOUtils.slurpStream(stream);
            } finally {
                ResourceUtils.closeQuietly(stream);
            }
        }

        public SecureSpanBridge.Result send(String soapAction, String message) throws SecureSpanBridge.SendException, IOException, SAXException, CausedBadCredentialsException, CausedCertificateAlreadyIssuedException {
            return send(soapAction, XmlUtil.stringToDocument(message));
        }

        public String getUriLocalPart() {
            return localUri;
        }

        public void setUriLocalPart(String uriLocalPart) {
            if (uriLocalPart == null) uriLocalPart = "";
            if (!uriLocalPart.startsWith("/")) uriLocalPart = "/" + uriLocalPart;
            this.localUri = uriLocalPart;
        }

        public X509Certificate getServerCert() throws IOException {
            return ssg.getServerCertificate();
        }

        public X509Certificate getClientCert() throws IOException {
            return ssg.getClientCertificate();
        }

        public PrivateKey getClientCertPrivateKey() throws CausedBadCredentialsException, IOException {
            try {
                return ssg.getClientCertificatePrivateKey();
            } catch (com.l7tech.proxy.datamodel.exceptions.BadCredentialsException e) {
                throw new CausedBadCredentialsException(e);
            } catch (HttpChallengeRequiredException e) {
                throw new CausedBadCredentialsException(e);
            } catch (OperationCanceledException e) {
                throw new CausedBadCredentialsException(e);
            }
        }

        public void ensureCertificatesAreAvailable() throws
                IOException, CausedBadCredentialsException, GeneralSecurityException,
                CausedCertificateAlreadyIssuedException
        {
            try {
                if (ssg.getServerCertificate() == null)
                    ssg.getRuntime().getSsgKeyStoreManager().installSsgServerCertificate(ssg, pw);
            } catch (com.l7tech.proxy.datamodel.exceptions.BadCredentialsException e) {
                throw new CausedBadCredentialsException(e);
            } catch (OperationCanceledException e) {
                throw new CausedBadCredentialsException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }

            try {
                if (ssg.getClientCertificate() == null)
                    ssg.getRuntime().getSsgKeyStoreManager().obtainClientCertificate(pw);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            } catch (com.l7tech.proxy.datamodel.exceptions.BadCredentialsException e) {
                throw new CausedBadCredentialsException(e);
            } catch (com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException e) {
                throw new CausedCertificateAlreadyIssuedException(e);
            } catch (ServerFeatureUnavailableException e) {
                throw new CausedIOException(e); // TODO document a new exception for this, but without breaking preexisting code
            }
        }

        public void importServerCert(X509Certificate serverCert) throws IOException {
            try {
                synchronized (ssg) {
                    ssg.getRuntime().getSsgKeyStoreManager().saveSsgCertificate(serverCert);
                    ssg.getRuntime().resetSslContext();
                }
            } catch (KeyStoreException e) {
                throw new CausedIOException(e);
            } catch (IOException e) {
                throw new CausedIOException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            } catch (CertificateException e) {
                throw new CausedIOException(e);
            }

        }

        public void importClientCert(X509Certificate clientCert, PrivateKey clientKey) throws IOException {
            try {
                if (pw == null)
                    throw new CausedIOException("Unable to import a client certificate -- no credentials were set for this Bridge instance.");
                synchronized (ssg) {
                    ssg.getRuntime().getSsgKeyStoreManager().saveClientCertificate(clientKey, clientCert, pw.getPassword());
                    ssg.getRuntime().resetSslContext();
                }
            } catch (KeyStoreException e) {
                throw new CausedIOException(e);
            } catch (IOException e) {
                throw new CausedIOException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            } catch (CertificateException e) {
                throw new CausedIOException(e);
            }
        }

        public void importClientCert(File pkcs12Path, final String alias, char[] pkcs12Password) throws IOException {
            try {
                if (pw == null)
                    throw new CausedIOException("Unable to import a client certificate -- no credentials were set for this Bridge instance.");
                synchronized (ssg) {
                    CertUtils.AliasPicker aliasPicker = new CertUtils.AliasPicker() {
                        public String selectAlias(String[] options) {
                            if (alias != null)
                                return alias;
                            if (options == null || options.length < 1)  // sanity check
                                return null;
                            return options[0];
                        }
                    };
                    ssg.getRuntime().getSsgKeyStoreManager().importClientCertificate(pkcs12Path, pkcs12Password, aliasPicker, pw.getPassword());
                    ssg.getRuntime().resetSslContext();
                }
            } catch (GeneralSecurityException e) {
                throw new CausedIOException(e);
            } catch (AliasNotFoundException e) {
                throw new CausedIOException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }
        }

        public void destroyClientCertificate() throws IOException {
            try {
                ssg.getRuntime().getSsgKeyStoreManager().deleteClientCert();
            } catch (KeyStoreException e) {
                throw new CausedIOException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }
        }

    }

}
