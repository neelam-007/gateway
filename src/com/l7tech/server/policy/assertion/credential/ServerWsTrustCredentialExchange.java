/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.GenericHttpRequestParamsImpl;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.wstrust.TokenServiceClient;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.server.transport.http.SslClientTrustManager;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerWsTrustCredentialExchange implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerWsTrustCredentialExchange.class.getName());

    private final WsTrustCredentialExchange assertion;
    private final Auditor auditor;
    private final SimpleHttpClient httpClient = new SimpleHttpClient(new UrlConnectionHttpClient());
    private final URL tokenServiceUrl;
    private final SSLContext sslContext;

    public ServerWsTrustCredentialExchange(WsTrustCredentialExchange assertion, ApplicationContext springContext) {
        this.assertion = assertion;
        this.auditor = new Auditor(this, springContext, logger);
        try {
            if (assertion.getTokenServiceUrl() != null)
                this.tokenServiceUrl = new URL(assertion.getTokenServiceUrl());
            else {
                this.tokenServiceUrl = null;
                logger.warning("Token Service URL is null; assertion is non-functional");
            }
        } catch (MalformedURLException e) {
            throw (IllegalArgumentException)new IllegalArgumentException("Unable to parse WS-Trust URL").initCause(e);
        }

        try {
            sslContext = SSLContext.getInstance("SSL");
            final SslClientTrustManager trustManager = (SslClientTrustManager)springContext.getBean("httpRoutingAssertionTrustManager");
            final int timeout = Integer.getInteger(ServerHttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                                   ServerHttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            sslContext.init(null, new TrustManager[]{trustManager}, null);
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.SSL_CONTEXT_INIT_FAILED, null, e);
            throw new RuntimeException(e);
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        LoginCredentials creds = context.getCredentials();
        SecurityToken token = null;
        Object payload = creds.getPayload();
        if (payload instanceof SecurityToken) {
            token = (SecurityToken)payload;
        } else if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
            token = new UsernameTokenImpl(creds.getLogin(), creds.getCredentials());
        // TODO } else if (creds.getClientCert() != null) {
        } else {
            auditor.logAndAudit(AssertionMessages.WSTRUST_NO_SUITABLE_CREDENTIALS);
            return AssertionStatus.FAILED;
        }
        Document rstDoc = TokenServiceClient.createRequestSecurityTokenMessage(null,
                                                                               assertion.getRequestType(),
                                                                               token,
                                                                               assertion.getAppliesTo());

        GenericHttpRequestParamsImpl params = new GenericHttpRequestParamsImpl(tokenServiceUrl);
        params.setContentType(ContentTypeHeader.XML_DEFAULT);
        params.setSslSocketFactory(sslContext.getSocketFactory());

        try {
            SimpleHttpClient.SimpleXmlResponse response = httpClient.postXml(params, rstDoc);
            int status = response.getStatus();
            if (status == 200) {
                Document rstrDoc = response.getDocument();
                Object rstrObj = TokenServiceClient.parseUnsignedRequestSecurityTokenResponse(rstrDoc);
                if (rstrObj instanceof SamlAssertion) {
                    SamlAssertion samlAssertion = (SamlAssertion)rstrObj;
                    context.setCredentials(LoginCredentials.makeSamlCredentials(samlAssertion, assertion.getClass()));
                    // TODO remove credentials from message
                    Element tokenElement = token.asElement();
                    if (tokenElement == null) {
                        auditor.logAndAudit(AssertionMessages.WSTRUST_DECORATION_FAILED);
                        return AssertionStatus.FAILED;
                    } else {
                        XmlKnob xmlKnob = (XmlKnob)context.getRequest().getKnob(XmlKnob.class);
                        if (xmlKnob == null) {
                            auditor.logAndAudit(AssertionMessages.WSTRUST_NON_XML_MESSAGE);
                            return AssertionStatus.FAILED;
                        }
                        Document requestDoc = xmlKnob.getDocumentWritable(); // Don't actually want the document; just want to invalidate bytes
                        Node parent = tokenElement.getParentNode();
                        parent.removeChild(tokenElement);

                        DecorationRequirements decoReq = new DecorationRequirements();
                        decoReq.setSenderSamlToken(samlAssertion.asElement(), false);
                        WssDecorator deco = new WssDecoratorImpl();
                        try {
                            deco.decorateMessage(requestDoc, decoReq);
                            return AssertionStatus.NONE;
                        } catch (Exception e) {
                            auditor.logAndAudit(AssertionMessages.WSTRUST_DECORATION_FAILED, null, e);
                            return AssertionStatus.FAILED;
                        }
                    }
                } else {
                    auditor.logAndAudit(AssertionMessages.WSTRUST_RSTR_NOT_SAML);
                    return AssertionStatus.FAILED;
                }
            } else {
                auditor.logAndAudit(AssertionMessages.RESPONSE_STATUS); // TODO use a better message
                return AssertionStatus.FAILED;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.ERROR_READING_RESPONSE, null, e);
            return AssertionStatus.FAILED;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.ERROR_READING_RESPONSE, null, e);
            return AssertionStatus.FAILED;
        }
    }
}
