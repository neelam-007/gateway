/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.proxy.ssl.SslPeer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
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

/**
 * Obtain a SecureSpanBridge implementation.
 *
 * @author mike
 * @version 1.0
 */
public class SecureSpanBridgeFactory {
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

    private static Map credentialManagerMap = Collections.synchronizedMap(new HashMap());
    static {
        Managers.setCredentialManager(new CredentialManagerImpl() {
            public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException 
            {
                // TODO if this is a third-party token service, can't look up the SSG
                if (sslPeer == null)
                    throw new OperationCanceledException("Unable to determine trustworthiness of non-Gateway peer certificate");
                CredentialManager cm = (CredentialManager)credentialManagerMap.get(sslPeer);
                if (cm != null)
                    cm.notifySslCertificateUntrusted(sslPeer, "the Gateway " + sslPeer, untrustedCertificate);
                else
                    super.notifySslCertificateUntrusted(sslPeer, "the Gateway " + sslPeer, untrustedCertificate);
            }
        });
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
            final char[] pass = options.getPassword() == null ? new char[0] : (char[]) options.getPassword().clone();
            pw = new PasswordAuthentication(options.getUsername(),
                                                                                     pass);
            ssg.setUsername(pw.getUserName());
            ssg.getRuntime().setCachedPassword(pw.getPassword());
        } else
            pw = null;
        if (options.getGatewayPort() != 0)
            ssg.setSsgPort(options.getGatewayPort());
        if (options.getGatewaySslPort() != 0)
            ssg.setSslPort(options.getGatewaySslPort());
        if (options.getKeyStorePath() != null)
            ssg.setKeyStorePath(options.getKeyStorePath());
        if (options.getCertStorePath() != null)
            ssg.setTrustStorePath(options.getCertStorePath());
        if (options.getUseSslByDefault() != null)
            ssg.setUseSslByDefault(options.getUseSslByDefault().booleanValue());
        if (options.getTrustedGateway() != null) {
            SecureSpanBridgeImpl trustedBridge = (SecureSpanBridgeImpl)options.getTrustedGateway();
            ssg.setTrustedGateway(trustedBridge.getSsg());
        }
        if (options.getGatewayCertificateTrustManager() != null) {
            final SecureSpanBridgeOptions.GatewayCertificateTrustManager tm = options.getGatewayCertificateTrustManager();
            credentialManagerMap.put(ssg, new CredentialManagerImpl() {
                final String msgNoTrust = "Bridge API user's GatewayCertificateTrustManager rejected Gateway certificate";
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
        final RequestInterceptor nri = NullRequestInterceptor.INSTANCE;
        return new SecureSpanBridgeImpl(ssg, nri, mp, pw);
    }

    static class SecureSpanBridgeImpl implements SecureSpanBridge {
        private final Ssg ssg;
        private final RequestInterceptor nri;
        private final MessageProcessor mp;
        private final PasswordAuthentication pw;

        private String localUri = "/bridge/api/NoOriginalUri";

        SecureSpanBridgeImpl(Ssg ssg, RequestInterceptor nri, MessageProcessor mp, PasswordAuthentication pw) {
            this.ssg = ssg;
            this.nri = nri;
            this.mp = mp;
            this.pw = pw;
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

        public SecureSpanBridge.Result send(String soapAction, Document message) throws SendException, IOException, CausedBadCredentialsException, CausedCertificateAlreadyIssuedException {
            final URL origUrl = new URL("http://layer7tech.com" + localUri);
            String namespaceUri = SoapUtil.getPayloadNamespaceUri(message);
            PolicyAttachmentKey pak = new PolicyAttachmentKey(namespaceUri, soapAction, origUrl.getFile());
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
                final Document doc = context.getResponse().getXmlKnob().getDocumentReadOnly(); // we no longer care about sync with underlying MIME part
                return new Result() {
                    public int getHttpStatus() {
                        return httpStatus;
                    }

                    public Document getResponse() {
                        return doc;
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
            } catch (BadSecurityContextException e) {
                throw new CausedSendException(e);
            } finally {
                if (context != null)
                    context.close();
                CurrentSslPeer.clear();
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
            }
        }

        public void ensureCertificatesAreAvailable() throws
                IOException, CausedBadCredentialsException, GeneralSecurityException,
                CausedCertificateAlreadyIssuedException
        {
            try {
                if (ssg.getServerCertificate() == null)
                    SsgKeyStoreManager.installSsgServerCertificate(ssg, pw);
            } catch (com.l7tech.proxy.datamodel.exceptions.BadCredentialsException e) {
                throw new CausedBadCredentialsException(e);
            } catch (OperationCanceledException e) {
                throw new CausedBadCredentialsException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }

            try {
                if (ssg.getClientCertificate() == null)
                    SsgKeyStoreManager.obtainClientCertificate(ssg, pw);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            } catch (com.l7tech.proxy.datamodel.exceptions.BadCredentialsException e) {
                throw new CausedBadCredentialsException(e);
            } catch (com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException e) {
                throw new CausedCertificateAlreadyIssuedException(e);
            }
        }

        public void importServerCert(X509Certificate serverCert) throws IOException {
            try {
                synchronized (ssg) {
                    SsgKeyStoreManager.saveSsgCertificate(ssg, serverCert);
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
                    SsgKeyStoreManager.saveClientCertificate(ssg, clientKey, clientCert, pw.getPassword());
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
                    SsgKeyStoreManager.AliasPicker aliasPicker = new SsgKeyStoreManager.AliasPicker() {
                        public String selectAlias(String[] options) {
                            if (alias != null)
                                return alias;
                            if (options == null || options.length < 1)  // sanity check
                                return null;
                            return options[0];
                        }
                    };
                    SsgKeyStoreManager.importClientCertificate(ssg, pkcs12Path, pkcs12Password, aliasPicker, pw.getPassword());
                    ssg.getRuntime().resetSslContext();
                }
            } catch (GeneralSecurityException e) {
                throw new CausedIOException(e);
            } catch (SsgKeyStoreManager.AliasNotFoundException e) {
                throw new CausedIOException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }
        }

        public void destroyClientCertificate() throws IOException {
            try {
                SsgKeyStoreManager.deleteClientCert(ssg);
            } catch (KeyStoreException e) {
                throw new CausedIOException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }
        }
    }
}
