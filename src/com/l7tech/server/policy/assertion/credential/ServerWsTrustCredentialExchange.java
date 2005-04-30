/*
* Copyright (C) 2004 Layer 7 Technologies Inc.
*/
package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.wstrust.TokenServiceClient;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessor;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.util.SoapUtil;
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
 */
public class ServerWsTrustCredentialExchange implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerWsTrustCredentialExchange.class.getName());

    private final WsTrustCredentialExchange assertion;
    private final Auditor auditor;
    private final SimpleHttpClient httpClient = new SimpleHttpClient(new UrlConnectionHttpClient());
    private final URL tokenServiceUrl;
    private final SSLContext sslContext;
    private final WssProcessor trogdor = new WssProcessorImpl();

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
        XmlKnob requestXml = (XmlKnob)context.getRequest().getKnob(XmlKnob.class);
        if (requestXml == null) {
            auditor.logAndAudit(AssertionMessages.WSTRUST_NON_XML_MESSAGE);
            return AssertionStatus.FAILED;
        }

        // Try to get credentials from WSS processor results
        SecurityToken originalToken = null;
        Element originalTokenElement = null;
        ProcessorResult wssProcResult = requestXml.getProcessorResult();
        if (wssProcResult != null) {
            SecurityToken[] tokens = wssProcResult.getSecurityTokens();
            for (int i = 0; i < tokens.length; i++) {
                SecurityToken token = tokens[i];
                if (token instanceof SamlAssertion || token instanceof UsernameToken) {
                    if (originalToken == null) {
                        originalToken = token;
                        originalTokenElement = token.asElement();
                    } else {
                        auditor.logAndAudit(AssertionMessages.WSTRUST_MULTI_TOKENS);
                        return AssertionStatus.FAILED;
                    }
                }
            }
        }

        // Try to get non-WSS credentials
        if (originalToken == null) {
            LoginCredentials creds = context.getCredentials();
            if (creds != null) {
                Object payload = creds.getPayload();
                if (payload instanceof SecurityToken) {
                    originalToken = (SecurityToken)payload;
                } else if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
                    originalToken = new UsernameTokenImpl(creds.getLogin(), creds.getCredentials());
                }
            }
        }

        if (originalToken == null) {
            auditor.logAndAudit(AssertionMessages.WSTRUST_NO_SUITABLE_CREDENTIALS);
            return AssertionStatus.FAILED;
        }

        // Create RST
        Document rstDoc = TokenServiceClient.createRequestSecurityTokenMessage(null,
                                                                               assertion.getRequestType(),
                                                                               originalToken,
                                                                               assertion.getAppliesTo(),
                                                                               assertion.getIssuer());

        GenericHttpRequestParamsImpl params = new GenericHttpRequestParamsImpl(tokenServiceUrl);
        params.setContentType(ContentTypeHeader.XML_DEFAULT);
        params.setSslSocketFactory(sslContext.getSocketFactory());
        params.setExtraHeaders(new HttpHeader[] { new GenericHttpHeader(SoapUtil.SOAPACTION, "\"\"") });

        try {
            // Get RSTR
            SimpleHttpClient.SimpleXmlResponse response = httpClient.postXml(params, rstDoc);
            int status = response.getStatus();
            if (status != 200) {
                auditor.logAndAudit(AssertionMessages.WSTRUST_RSTR_STATUS_NON_200); // TODO use a better message
                return AssertionStatus.AUTH_REQUIRED;
            }

            Document rstrDoc = response.getDocument();
            Object rstrObj = TokenServiceClient.parseUnsignedRequestSecurityTokenResponse(rstrDoc);

            if (originalTokenElement == null) {
                auditor.logAndAudit(AssertionMessages.WSTRUST_ORIGINAL_TOKEN_NOT_XML);
                return AssertionStatus.NONE;
            }

            Document requestDoc = requestXml.getDocumentWritable(); // Don't actually want the document; just want to invalidate bytes
            Node securityEl = originalTokenElement.getParentNode();
            securityEl.removeChild(originalTokenElement);
            // Check for empty Security header, remove
            // TODO make this optional?
            // TODO what if Security header isn't empty?
            if (securityEl.getFirstChild() == null) {
                securityEl.getParentNode().removeChild(securityEl);
            }

            DecorationRequirements decoReq = new DecorationRequirements();
            WssDecorator deco = new WssDecoratorImpl();
            if (rstrObj instanceof SamlAssertion) {
                final SamlAssertion samlAssertion = (SamlAssertion) rstrObj;

                context.setCredentials(LoginCredentials.makeSamlCredentials(samlAssertion, assertion.getClass()));
                decoReq.setSenderSamlToken(samlAssertion.asElement(), false);
            } else if (rstrObj instanceof UsernameToken) {
                UsernameToken ut = (UsernameToken) rstrObj;
                LoginCredentials creds = ut.asLoginCredentials();
                context.setCredentials(creds);
                decoReq.setUsernameTokenCredentials(new UsernameTokenImpl(creds));
            } else {
                auditor.logAndAudit(AssertionMessages.WSTRUST_RSTR_BAD_TYPE);
                return AssertionStatus.AUTH_REQUIRED;
            }

            try {
                deco.decorateMessage(requestDoc, decoReq);
                requestXml.setDocument(requestDoc);
                requestXml.setProcessorResult(trogdor.undecorateMessage(context.getRequest(), null, null, null));
                return AssertionStatus.NONE;
            } catch (Exception e) {
                auditor.logAndAudit(AssertionMessages.WSTRUST_DECORATION_FAILED, null, e);
                return AssertionStatus.FAILED;
            }
        } catch (GenericHttpException e) {
            auditor.logAndAudit(AssertionMessages.WSTRUST_SERVER_HTTP_FAILED, null, e);
            return AssertionStatus.FAILED;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.ERROR_READING_RESPONSE, null, e);
            return AssertionStatus.FAILED;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.ERROR_READING_RESPONSE, null, e);
            return AssertionStatus.FAILED;
        }
    }
}
