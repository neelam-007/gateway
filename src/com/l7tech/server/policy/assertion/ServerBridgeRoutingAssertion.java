/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.*;
import com.l7tech.proxy.SecureSpanBridge;
import com.l7tech.proxy.SecureSpanBridgeFactory;
import com.l7tech.proxy.SecureSpanBridgeOptions;
import com.l7tech.server.AssertionMessages;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * SSG imlementation of a routing assertion that uses the SSB.
 */
public class ServerBridgeRoutingAssertion extends ServerRoutingAssertion {
    private static final Logger logger = Logger.getLogger(ServerBridgeRoutingAssertion.class.getName());

    private final BridgeRoutingAssertion bridgeRoutingAssertion;
    private final SignerInfo senderVouchesSignerInfo;
    private final SecureSpanBridge bridge;

    public ServerBridgeRoutingAssertion(BridgeRoutingAssertion assertion, ApplicationContext ctx) {
        super(ctx);
        this.bridgeRoutingAssertion = assertion;

        final KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore");
        try {
            senderVouchesSignerInfo = ku.getSslSignerInfo();
        } catch (IOException e) {
            throw new RuntimeException("Can't read the keystore for signing outbound SAML", e);
        }

        String gatewayHostname = null;
        final URL url;
        try {
            url = new URL(assertion.getProtectedServiceUrl());
        } catch (MalformedURLException e) {
            logger.warning("BridgeRoutingAssertion: URL is invalid; assertion is therefore nonfunctional.");
            bridge = null;
            return;
            //throw (IllegalArgumentException)new IllegalArgumentException("Bad protected service URL").initCause(e);
        }
        gatewayHostname = url.getHost();

        String username = assertion.getLogin();
        final String pass = assertion.getPassword();
        char[] password = pass == null ? null : pass.toCharArray();
        SecureSpanBridgeOptions opt = new SecureSpanBridgeOptions(gatewayHostname, username, password);

        final SslClientTrustManager trustManager = (SslClientTrustManager)applicationContext.getBean("httpRoutingAssertionTrustManager");

        opt.setGatewayCertificateTrustManager(new SecureSpanBridgeOptions.GatewayCertificateTrustManager() {
            public boolean isGatewayCertificateTrusted(X509Certificate[] chain) throws CertificateException {
                trustManager.checkServerTrusted(chain, "RSA"); // TODO support diffie-helman certs
                return true;
            }
        });

        opt.setCertStorePath("/NOWHERE/INVALID/PATH");
        opt.setKeyStorePath("/NOWHERE/INVALID/PATH");

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

        opt.setGatewayPort(normalPort);
        opt.setGatewaySslPort(sslPort);

        // TODO make this fully configurable
        opt.setUseSslByDefault(Boolean.TRUE);
        bridge = SecureSpanBridgeFactory.createSecureSpanBridge(opt);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);

        Auditor auditor = new Auditor(context.getAuditContext(), logger);
        final SecureSpanBridge.Result result;

        if (bridge == null) {
            auditor.logAndAudit(AssertionMessages.BAD_PROTECTED_SERVICE_URL);
            return AssertionStatus.FAILED;
        }

        try {
            try {
                PublishedService service = context.getService();
                URL url = getProtectedServiceUrl(service);

                if (context.getService().isSoap()) {
                    int whatToDoWithSecHeader = bridgeRoutingAssertion.getCurrentSecurityHeaderHandling();

                    // DELETE CURRENT SECURITY HEADER IF NECESSARY
                    if (whatToDoWithSecHeader == RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER ||
                        whatToDoWithSecHeader == RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER) {
                        Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
                        Element defaultSecHeader = null;
                        try {
                            defaultSecHeader = SoapUtil.getSecurityElement(doc);
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
                }


                if (bridgeRoutingAssertion.isTaiCredentialChaining()) {
                    throw new PolicyAssertionException("BridgeRoutingAssertion unable to support TAI credential chaining");
                }

                if (bridgeRoutingAssertion.isAttachSamlSenderVouches()) {
                    Document document = context.getRequest().getXmlKnob().getDocumentWritable();
                    SamlAssertionGenerator ag = new SamlAssertionGenerator(senderVouchesSignerInfo);
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
                // TODO support cookies on SSB API
                // attachCookies(client, context, url, auditor);

                // TODO support non-SOAP messaging with SSB api
                // TODO support SOAP-with-attachments with SSB api
                String soapAction = context.getRequest().getHttpRequestKnob().getHeaderSingleValue(SoapUtil.SOAPACTION);
                Document message = context.getRequest().getXmlKnob().getDocumentReadOnly();

                if (context.getRequest().getMimeKnob().isMultipart())
                    auditor.logAndAudit(AssertionMessages.BRIDGE_NO_ATTACHMENTS);

                try {
                    result = bridge.send(soapAction, message);
                } catch (SecureSpanBridge.SendException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                    return AssertionStatus.FAILED;
                } catch (SecureSpanBridge.BadCredentialsException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                    return AssertionStatus.FAILED;
                } catch (SecureSpanBridge.CertificateAlreadyIssuedException e) {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                    return AssertionStatus.FAILED;
                }

                int status = result.getHttpStatus();
                if (status == 200)
                    auditor.logAndAudit(AssertionMessages.ROUTED_OK);
                else
                    auditor.logAndAudit(AssertionMessages.RESPONSE_STATUS, new String[] {url.getPath(), String.valueOf(status)});

                context.getResponse().getHttpResponseKnob().setStatus(status);

                context.setRoutingStatus(RoutingStatus.ROUTED);

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
            // BEYOND THIS POINT, WE DONT RETURN FAILURE
            try {
                context.getResponse().initialize(result.getResponse());
            } catch (Exception e) {
                auditor.logAndAudit(AssertionMessages.ERROR_READING_RESPONSE, null, e);
                // here we dont return error because we already routed
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

}
