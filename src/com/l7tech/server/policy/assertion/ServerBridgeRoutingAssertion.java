/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.message.*;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.SecurityActor;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import com.l7tech.proxy.ssl.SslPeer;
import com.l7tech.proxy.ssl.SslPeerHttpClient;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSG imlementation of a routing assertion that uses the SSB.
 */
public class ServerBridgeRoutingAssertion extends ServerRoutingAssertion {
    private static final Logger logger = Logger.getLogger(ServerBridgeRoutingAssertion.class.getName());
    public static final URL DEFAULT_ORIG_URL;
    static {
        try {
            DEFAULT_ORIG_URL = new URL("http://layer7tech.com/no-original-uri");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private final BridgeRoutingAssertion bridgeRoutingAssertion;
    private final SignerInfo signerInfo;
    private final Ssg ssg;
    private final MessageProcessor messageProcessor;
    private X509Certificate serverCert;

    public ServerBridgeRoutingAssertion(BridgeRoutingAssertion assertion, ApplicationContext ctx) {
        super(ctx);
        this.bridgeRoutingAssertion = assertion;

        final KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore");
        try {
            signerInfo = ku.getSslSignerInfo();
        } catch (IOException e) {
            throw new RuntimeException("Can't read the keystore for signing outbound SAML", e);
        }

        String gatewayHostname = null;
        final URL url;
        try {
            url = new URL(assertion.getProtectedServiceUrl());
        } catch (MalformedURLException e) {
            logger.warning("BridgeRoutingAssertion: URL is invalid; assertion is therefore nonfunctional.");
            ssg = null;
            messageProcessor = null;
            return;
            //throw (IllegalArgumentException)new IllegalArgumentException("Bad protected service URL").initCause(e);
        }

        String policyXml = bridgeRoutingAssertion.getPolicyXml();
        final Policy hardcodedPolicy;
        if (policyXml != null) {
            try {
                Assertion a = WspReader.parse(policyXml);
                hardcodedPolicy = new Policy(a, null);
            } catch (IOException e) {
                logger.log(Level.WARNING,
                           "BridgeRoutingAssertion: hardcoded policy is invalid; assertion is therefore nonfunctional.",
                           e);
                ssg = null;
                messageProcessor = null;
                return;
            }
        } else
            hardcodedPolicy = null;

        gatewayHostname = url.getHost();

        String username = assertion.getLogin();
        final String pass = assertion.getPassword();
        char[] password = pass == null ? null : pass.toCharArray();

        ssg = new Ssg(1, gatewayHostname) {
            public synchronized String getKeyStorePath() {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a key store path");
            }

            public void setKeyStorePath(String keyStorePath) {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a key store path");
            }

            public synchronized String getTrustStorePath() {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a trust store path");
            }

            public void setTrustStorePath(String trustStorePath) {
                throw new IllegalStateException("BridgeRoutingAssertion does not have a trust store path");
            }

            public X509Certificate getServerCertificate() {
                return getRuntime().getCachedServerCert();
            }

            public X509Certificate getClientCertificate() {
                return signerInfo.getCertificateChain()[0];
            }

            public PrivateKey getClientCertificatePrivateKey() {
                return signerInfo.getPrivate();
            }
        };

        ssg.getRuntime().setCachedServerCert(null);
        ssg.setUsername(username);
        ssg.getRuntime().setCachedPassword(password);

        final SslClientTrustManager trustManager = (SslClientTrustManager)applicationContext.getBean("httpRoutingAssertionTrustManager");
        ssg.getRuntime().setTrustManager(trustManager);
        ssg.getRuntime().setCredentialManager(new CredentialManagerImpl() {
            public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException {
                if (!(sslPeer instanceof Ssg))
                    throw new OperationCanceledException("Unable to approve certificate import for SslPeer of unexpected type " + sslPeer.getClass().getName());
                Ssg ssg1 = (Ssg)sslPeer;
                if (ssg1 != ssg)
                    throw new OperationCanceledException("Unable to approve certificate import for Ssg other than the one managed by this ServerBridgeRoutingAssertion");
                try {
                    // TODO support DSA certificates
                    trustManager.checkServerTrusted(new X509Certificate[] { untrustedCertificate }, "RSA");
                } catch (CertificateException e) {
                    throw new OperationCanceledException("Unable to approve certificate import for Ssg: " + e, e);
                }
                // Looks good.  Allow the certificate to be imported.
                serverCert = untrustedCertificate;
                ssg.getRuntime().setCachedServerCert(serverCert);
            }

            public PasswordAuthentication getNewCredentials(Ssg ssg, boolean displayBadPasswordMessage) throws OperationCanceledException {
                AuditContext auditContext = (AuditContext)applicationContext.getBean("auditContext");
                Auditor auditor = new Auditor(auditContext, logger);
                auditor.logAndAudit(AssertionMessages.ACCESS_DENIED);
                throw new OperationCanceledException(((AuditDetailMessage)AssertionMessages.ACCESS_DENIED).getMessage());
            }
        });

        ssg.getRuntime().setSsgKeyStoreManager(new BridgeRoutingKeyStoreManager());


        SimpleHttpClient httpClient = new SimpleHttpClient(new SslPeerHttpClient(new CommonsHttpClient(ssg.getRuntime().getHttpConnectionManager()),
                                                                                 ssg,
                                                                                 ClientProxySecureProtocolSocketFactory.getInstance()));
        ssg.getRuntime().setHttpClient(httpClient);

        if (hardcodedPolicy != null) {
            ssg.getRuntime().setPolicyManager(new PolicyManager() {
                public void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
                    // No action needed
                }

                public Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
                    return hardcodedPolicy;
                }

                public void setPolicy(PolicyAttachmentKey key, Policy policy) {
                    throw new IllegalStateException("Unable to store new policy: this Bridge Routing Assertion has a hardcoded policy.");
                }

                public Set getPolicyAttachmentKeys() {
                    return Collections.EMPTY_SET;
                }

                public void clearPolicies() {
                    // No action needed
                }
            });
        }

        // TODO use this trust manager somehow

        final int port = url.getPort();
        int normalPort;
        int sslPort;

        // TODO make ports fully configurable
        // TODO move this heuristic elsewhere
        if ("https".equalsIgnoreCase(url.getProtocol())) {
            sslPort = port;
            normalPort = port - 443 + 80;
            if (normalPort < 0 || normalPort > 65535)
                normalPort = 80;
        } else {
            normalPort = port;
            sslPort = normalPort - 80 + 443;
            if (sslPort < 0 || sslPort > 65535)
                sslPort = 443;
        }

        ssg.setSsgPort(normalPort);
        ssg.setSslPort(sslPort);

        // TODO make this fully configurable
        ssg.setUseSslByDefault(true);
        messageProcessor = new MessageProcessor();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);

        Auditor auditor = new Auditor(context.getAuditContext(), logger);

        if (messageProcessor == null || ssg == null) {
            auditor.logAndAudit(AssertionMessages.BRIDGE_BAD_CONFIG);
            return AssertionStatus.FAILED;
        }

        try {
            try {
                PublishedService service = context.getService();
                URL url = getProtectedServiceUrl(service);

                final SoapKnob requestSoapKnob;
                try {
                    requestSoapKnob = context.getRequest().getSoapKnob();
                } catch (MessageNotSoapException e) {
                    auditor.logAndAudit(AssertionMessages.NON_SOAP_NOT_SUPPORTED_WRONG_FORMAT);
                    return AssertionStatus.FAILED;
                }

                int whatToDoWithSecHeader = bridgeRoutingAssertion.getCurrentSecurityHeaderHandling();

                // DELETE CURRENT SECURITY HEADER IF NECESSARY
                // todo, move this to util class to avoid duplication of code
                if (whatToDoWithSecHeader == RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER ||
                        whatToDoWithSecHeader == RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER) {
                    Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
                    Element defaultSecHeader = null;
                    try {
                        ProcessorResult pr = context.getRequest().getXmlKnob().getProcessorResult();
                        if (pr != null && pr.getProcessedActor() == SecurityActor.L7ACTOR) {
                            defaultSecHeader = SoapUtil.getSecurityElement(doc, SecurityActor.L7ACTOR.getValue());
                        } else {
                            defaultSecHeader = SoapUtil.getSecurityElement(doc);
                        }
                    } catch (InvalidDocumentFormatException e) {
                        String msg = "this option is not supported for non-soap messages. this message is " +
                                "supposed to be soap but does not appear to be";
                        auditor.logAndAudit(AssertionMessages.NON_SOAP_NOT_SUPPORTED_WRONG_FORMAT, null, e);
                        throw new PolicyAssertionException(msg);
                    }
                    if (defaultSecHeader != null) {
                        defaultSecHeader.getParentNode().removeChild(defaultSecHeader);

                        // we should not leave an empty header element
                        Element header = null;
                        try {
                            header = SoapUtil.getHeaderElement(doc);
                        } catch (InvalidDocumentFormatException e) {
                            String msg = "this option is not supported for non-soap messages. this message is " +
                                    "supposed to be soap but does not appear to be";
                            auditor.logAndAudit(AssertionMessages.NON_SOAP_NOT_SUPPORTED_WRONG_FORMAT, null, e);
                            throw new PolicyAssertionException(msg);
                        }
                        if (header != null) {
                            if (XmlUtil.elementIsEmpty(header)) {
                                header.getParentNode().removeChild(header);
                            }
                        }
                    }
                } else if (whatToDoWithSecHeader == RoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS) {
                    Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
                    try {
                        ProcessorResult pr = context.getRequest().getXmlKnob().getProcessorResult();
                        // leaving the processed security header for passthrough means that if the processed
                        // actor was l7, we need to promote to default (unless there is a default present)
                        if (pr != null && pr.getProcessedActor() == SecurityActor.L7ACTOR) {
                            Element defaultSecHeader = SoapUtil.getSecurityElement(doc, SecurityActor.L7ACTOR.getValue());
                            if (defaultSecHeader != null) {
                                Element noActorSecHeader = SoapUtil.getSecurityElement(doc);
                                if (noActorSecHeader != null && noActorSecHeader != defaultSecHeader) {
                                    logger.info("we can't promote l7 sec header as no actor because " +
                                                "there is already a noactor one present. there may be " +
                                                "something wrong with this policy");
                                } else {
                                    SoapUtil.removeSoapAttr(defaultSecHeader, SoapUtil.ACTOR_ATTR_NAME);
                                }
                            }
                        }
                    } catch (InvalidDocumentFormatException e) {
                        String msg = "this option is not supported for non-soap messages. this message is " +
                                     "supposed to be soap but does not appear to be";
                        auditor.logAndAudit(AssertionMessages.NON_SOAP_NOT_SUPPORTED_WRONG_FORMAT, null, e);
                        throw new PolicyAssertionException(msg);
                    }
                }

                // PROMOTE ANOTHER ONE IF NECESSARY
                if (whatToDoWithSecHeader == RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER &&
                        bridgeRoutingAssertion.getXmlSecurityActorToPromote() != null) {
                    Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
                    String actorDeservingPromotion = bridgeRoutingAssertion.getXmlSecurityActorToPromote();
                    // check if that actor is present
                    Element secHeaderToPromote = null;
                    try {
                        secHeaderToPromote = SoapUtil.getSecurityElement(doc, actorDeservingPromotion);
                    } catch (InvalidDocumentFormatException e) {
                        // the manager does not allow you to set this
                        // option for non-soap service therefore this
                        // should not hapen
                        String msg = "this option is not supported for non-soap messages. " +
                                "something is wrong with this policy";
                        auditor.logAndAudit(AssertionMessages.NON_SOAP_NOT_SUPPORTED_WRONG_POLICY, null, e);
                        throw new PolicyAssertionException(msg);
                    }
                    if (secHeaderToPromote != null) {
                        // do it
                        auditor.logAndAudit(AssertionMessages.PROMOMTING_ACTOR, new String[] {actorDeservingPromotion});
                        SoapUtil.nukeActorAttribute(secHeaderToPromote);
                    } else {
                        // this is not a big deal but might indicate something wrong
                        // with the assertion => logging as info
                        auditor.logAndAudit(AssertionMessages.NO_SECURITY_HEADER, new String[] {actorDeservingPromotion});
                    }
                }



                if (bridgeRoutingAssertion.isTaiCredentialChaining()) {
                    throw new PolicyAssertionException("BridgeRoutingAssertion unable to support TAI credential chaining");
                }

                if (bridgeRoutingAssertion.isAttachSamlSenderVouches()) {
                    Document document = context.getRequest().getXmlKnob().getDocumentWritable();
                    SamlAssertionGenerator ag = new SamlAssertionGenerator(signerInfo);
                    SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
                    TcpKnob requestTcp = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
                    if (requestTcp != null) {
                        try {
                            InetAddress clientAddress = InetAddress.getByName(requestTcp.getRemoteAddress());
                            samlOptions.setClientAddress(clientAddress);
                        } catch (UnknownHostException e) {
                            auditor.logAndAudit(AssertionMessages.CANNOT_RESOLVE_IP_ADDRESS, null, e);
                        }
                    }
                    samlOptions.setExpiryMinutes(bridgeRoutingAssertion.getSamlAssertionExpiry());
                    SubjectStatement statement = SubjectStatement.createAuthenticationStatement(context.getCredentials(), SubjectStatement.SENDER_VOUCHES);
                    ag.attachStatement(document, statement, samlOptions);
                }

                // TODO decide whether to pass through cookies from the client to the back end service
                // attachCookies(client, context, url, auditor);

                try {
                    // TODO support non-SOAP messaging with SSB api
                    // TODO support SOAP-with-attachments with SSB api
                    String soapAction = context.getRequest().getHttpRequestKnob().getHeaderSingleValue(SoapUtil.SOAPACTION);
                    String nsUri = "";
                    if (context.getRequest().isSoap()) {
                        nsUri = requestSoapKnob.getPayloadNamespaceUri();
                    }

                    if (context.getRequest().getMimeKnob().isMultipart())
                        auditor.logAndAudit(AssertionMessages.BRIDGE_NO_ATTACHMENTS);

                    final HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();

                    URL origUrl = DEFAULT_ORIG_URL;
                    try {
                        origUrl = httpRequestKnob != null ? new URL(httpRequestKnob.getRequestUrl()) : new URL("http://layer7tech.com/no-original-uri");
                    } catch (MalformedURLException e) {
                        auditor.logAndAudit(AssertionMessages.BAD_ORIGINAL_REQUEST_URL);
                    }

                    PolicyAttachmentKey pak = new PolicyAttachmentKey(nsUri, soapAction, origUrl.getPath());
                    Message bridgeRequest = context.getRequest(); // TODO see if it is unsafe to reuse this

                    // The response will need to be re-initialized
                    Message bridgeResponse = context.getResponse(); // TODO see if it is unsafe to reuse this


                    PolicyApplicationContext pac = new PolicyApplicationContext(ssg, bridgeRequest, bridgeResponse, NullRequestInterceptor.INSTANCE, pak, origUrl);
                    messageProcessor.processMessage(pac);

                    final HttpResponseKnob hrk = bridgeResponse.getHttpResponseKnob();
                    int status = hrk == null ? 500 : hrk.getStatus();
                    if (status == 200)
                        auditor.logAndAudit(AssertionMessages.ROUTED_OK);
                    else
                        auditor.logAndAudit(AssertionMessages.RESPONSE_STATUS, new String[] {url.getPath(), String.valueOf(status)});

                    context.getResponse().getHttpResponseKnob().setStatus(status);

                    context.setRoutingStatus(RoutingStatus.ROUTED);


                } catch (ConfigurationException e) {
                    auditor.logAndAudit(AssertionMessages.ACCESS_DENIED, null, e);
                    return AssertionStatus.SERVER_AUTH_FAILED;
                } catch (BadSecurityContextException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                    return AssertionStatus.FAILED;
                } catch (InvalidDocumentFormatException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                    return AssertionStatus.FAILED;
                } catch (OperationCanceledException e) {
                    auditor.logAndAudit(AssertionMessages.ACCESS_DENIED, null, e);
                    return AssertionStatus.SERVER_AUTH_FAILED;
                } catch (GeneralSecurityException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                    return AssertionStatus.FAILED;
                } catch (HttpChallengeRequiredException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e); // can't happen
                    return AssertionStatus.FAILED;
                } catch (ResponseValidationException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                    return AssertionStatus.FAILED;
                } catch (ClientCertificateException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e); // can't happen
                    return AssertionStatus.FAILED;
                } catch (ProcessorException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                    return AssertionStatus.FAILED;
                }
            } catch (WSDLException we) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, we);
                return AssertionStatus.FAILED;
            } catch (MalformedURLException mfe) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, mfe);
                return AssertionStatus.FAILED;
            } catch (IOException ioe) {
                // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, ioe);
                return AssertionStatus.FAILED;
            } catch (SAXException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                return AssertionStatus.FAILED;
            } catch (SignatureException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                return AssertionStatus.FAILED;
            } catch (CertificateException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                return AssertionStatus.FAILED;
            }
        } finally {
        }

        return AssertionStatus.NONE;
    }

    private URL getProtectedServiceUrl(PublishedService service) throws WSDLException, MalformedURLException {
        URL url;
        String psurl = bridgeRoutingAssertion.getProtectedServiceUrl();
        if (psurl == null) {
            URL wsdlUrl = service.serviceUrl();
            url = wsdlUrl;
        } else {
            url = new URL(psurl);
        }
        return url;
    }

    private class BridgeRoutingKeyStoreManager extends SsgKeyStoreManager {

        public boolean isClientCertUnlocked() throws KeyStoreCorruptException {
            return true;
        }

        public void deleteClientCert() throws IOException, KeyStoreException, KeyStoreCorruptException {
            throw new UnsupportedOperationException();
        }

        public boolean isPasswordWorkedForPrivateKey() {
            return true;
        }

        public void deleteStores() {
            throw new UnsupportedOperationException();
        }

        public void saveSsgCertificate(X509Certificate cert) throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            serverCert = cert;
            ssg.getRuntime().setCachedServerCert(cert);
        }

        public void saveClientCertificate(PrivateKey privateKey, X509Certificate cert, char[] privateKeyPassword) throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            throw new UnsupportedOperationException();
        }

        public void obtainClientCertificate(PasswordAuthentication credentials) throws BadCredentialsException, GeneralSecurityException, KeyStoreCorruptException, CertificateAlreadyIssuedException, IOException {
            throw new CertificateAlreadyIssuedException("Unable to apply for client certificate; the Gateway uses its SSL certificate as its client certificate.");
        }

        public String lookupClientCertUsername() {
            try {
                return CertUtils.extractCommonNameFromClientCertificate(getClientCert());
            } catch (KeyStoreCorruptException e) {
                throw new CausedIllegalStateException(e); // can't happen
            }
        }

        protected X509Certificate getServerCert() throws KeyStoreCorruptException {
            return serverCert;
        }

        protected X509Certificate getClientCert() throws KeyStoreCorruptException {
            return signerInfo.getCertificateChain()[0];
        }

        public PrivateKey getClientCertPrivateKey() throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException {
            return signerInfo.getPrivate();
        }

        protected boolean isClientCertAvailabile() throws KeyStoreCorruptException {
            return true;
        }

        protected KeyStore getKeyStore(char[] password) throws KeyStoreCorruptException {
            throw new UnsupportedOperationException("Gateway does not have an accessible key store file");
        }

        protected KeyStore getTrustStore() throws KeyStoreCorruptException {
            throw new UnsupportedOperationException("Gateway does not have an accessible cert store file");
        }

        public void importClientCertificate(File certFile, char[] pass, AliasPicker aliasPicker, char[] ssgPassword) throws IOException, GeneralSecurityException, KeyStoreCorruptException, AliasNotFoundException {
            throw new UnsupportedOperationException("Gateway is unable to import a client certificate");
        }
    }
}
