/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.proxy.datamodel.CurrentRequest;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.datamodel.PolicyManagerImpl;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.processor.MessageProcessor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Obtain a SecureSpanAgent implementation.
 *
 * @author mike
 * @version 1.0
 */
public class SecureSpanAgentFactory {
    private static class CausedSendException extends SecureSpanAgent.SendException {
        CausedSendException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    private static class CausedBadCredentialsException extends SecureSpanAgent.BadCredentialsException {
        CausedBadCredentialsException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    private static class CausedCertificateAlreadyIssuedException extends SecureSpanAgent.CertificateAlreadyIssuedException {
        CausedCertificateAlreadyIssuedException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    private static class CausedIOException extends IOException {
        CausedIOException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    public static SecureSpanAgent createSecureSpanAgent(String gatewayHostname,
                                                        int gatewayPort,
                                                        final String username,
                                                        final char[] password) {
        final Ssg ssg = new Ssg(1, gatewayHostname);
        ssg.setSsgPort(gatewayPort);
        ssg.setUsername(username);
        ssg.cmPassword(password);
        CurrentRequest.setCurrentSsg(ssg);
        //ssg.setKeyStoreFile(keystorePathname);
        PolicyManager policyManager = PolicyManagerImpl.getInstance();
        final MessageProcessor mp = new MessageProcessor(policyManager);
        final RequestInterceptor nri = NullRequestInterceptor.INSTANCE;
        return new SecureSpanAgent() {
            public SecureSpanAgent.Result send(String soapAction, Document message) throws SendException, IOException {
                PendingRequest pr = new PendingRequest(message, ssg, nri, new URL("http://foo.bar.baz"), null);
                pr.setSoapAction(soapAction);
                try {
                    final SsgResponse response = mp.processMessage(pr);
                    return new Result() {
                        public int getHttpStatus() {
                            return response.getHttpStatus();
                        }

                        public Document getResponse() throws IOException, SAXException {
                            return response.getResponseAsDocument();
                        }
                    };
                } catch (ClientCertificateException e) {
                    throw new CausedSendException(e);
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
                }
            }

            public SecureSpanAgent.Result send(String soapAction, String message) throws SecureSpanAgent.SendException, IOException, SAXException {
                return send(soapAction, XmlUtil.stringToDocument(message));
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
                PasswordAuthentication pw = new PasswordAuthentication(username, password);
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
        };
    }
}
