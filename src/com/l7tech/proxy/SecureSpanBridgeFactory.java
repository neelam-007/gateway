/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.processor.MessageProcessor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
            public void notifySsgCertificateUntrusted(Ssg ssg, X509Certificate certificate) throws OperationCanceledException {
                CredentialManager cm = (CredentialManager)credentialManagerMap.get(ssg);
                if (cm != null)
                    cm.notifySsgCertificateUntrusted(ssg, certificate);
                else
                    super.notifySsgCertificateUntrusted(ssg, certificate);
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
        final PasswordAuthentication pw = new PasswordAuthentication(options.getUsername(), (char[]) options.getPassword().clone());
        ssg.setUsername(pw.getUserName());
        ssg.cmPassword(pw.getPassword());
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
                public void notifySsgCertificateUntrusted(Ssg ssg, X509Certificate certificate) throws OperationCanceledException {
                    try {
                        if (tm.isGatewayCertificateTrusted(new X509Certificate[] {certificate}))
                            return;
                    } catch (CertificateException e) {
                        throw new OperationCanceledException(msgNoTrust, e);
                    }
                    throw new OperationCanceledException(msgNoTrust);
                }
            });
        }
        CurrentRequest.setCurrentSsg(ssg);
        final PolicyManager policyManager = PolicyManagerImpl.getInstance();
        final MessageProcessor mp = new MessageProcessor(policyManager);
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
            String namespaceUri = SoapUtil.getNamespaceUri(message);
            PolicyAttachmentKey pak = new PolicyAttachmentKey(namespaceUri, soapAction, origUrl.getFile());
            PendingRequest pr = new PendingRequest(ssg, null, null, message, nri, pak, origUrl);
            try {
                final SsgResponse response = mp.processMessage(pr);
                return new Result() {
                    public int getHttpStatus() {
                        return response.getHttpStatus();
                    }

                    public Document getResponse() throws IOException, SAXException {
                        return response.getOriginalDocument();
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
            try {
                return SsgKeyStoreManager.getServerCert(ssg);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }
        }

        public X509Certificate getClientCert() throws IOException {
            try {
                return SsgKeyStoreManager.getClientCert(ssg);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }
        }

        public PrivateKey getClientCertPrivateKey() throws CausedBadCredentialsException, IOException {
            try {
                return SsgKeyStoreManager.getClientCertPrivateKey(ssg);
            } catch (com.l7tech.proxy.datamodel.exceptions.BadCredentialsException e) {
                throw new CausedBadCredentialsException(e);
            } catch (OperationCanceledException e) {
                throw new CausedBadCredentialsException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Unable to read private key: no such algorithm", e);
            }
        }

        public void ensureCertificatesAreAvailable() throws
                IOException, CausedBadCredentialsException, GeneralSecurityException,
                CausedCertificateAlreadyIssuedException
        {
            try {
                if (SsgKeyStoreManager.getServerCert(ssg) == null)
                    SsgKeyStoreManager.installSsgServerCertificate(ssg, pw);
            } catch (com.l7tech.proxy.datamodel.exceptions.BadCredentialsException e) {
                throw new CausedBadCredentialsException(e);
            } catch (OperationCanceledException e) {
                throw new CausedBadCredentialsException(e);
            } catch (KeyStoreCorruptException e) {
                throw new CausedIOException(e);
            }

            try {
                if (SsgKeyStoreManager.getClientCert(ssg) == null)
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
                    ssg.resetSslContext();
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
                synchronized (ssg) {
                    SsgKeyStoreManager.saveClientCertificate(ssg, clientKey, clientCert, pw.getPassword());
                    ssg.resetSslContext();
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
                synchronized (ssg) {
                    SsgKeyStoreManager.AliasPicker aliasPicker = new SsgKeyStoreManager.AliasPicker() {
                        public String selectAlias(String[] options) throws SsgKeyStoreManager.AliasNotFoundException {
                            if (alias != null)
                                return alias;
                            if (options == null || options.length < 1)  // sanity check
                                return null;
                            return options[0];
                        }
                    };
                    SsgKeyStoreManager.importClientCertificate(ssg, pkcs12Path, pkcs12Password, aliasPicker, pw.getPassword());
                    ssg.resetSslContext();
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
