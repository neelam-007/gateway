package com.l7tech.server.policy.assertion.credential;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.security.wstrust.WsTrustConfigException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.http.*;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.wstrust.TokenServiceClient;
import com.l7tech.security.wstrust.WsTrustConfig;
import com.l7tech.security.wstrust.WsTrustConfigFactory;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessor;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerWsTrustCredentialExchange extends AbstractServerCachedSecurityTokenAssertion<WsTrustCredentialExchange> {
    private static final Logger logger = Logger.getLogger(ServerWsTrustCredentialExchange.class.getName());
    private static final String CACHE_SEC_TOKEN_KEY = ServerWsTrustCredentialExchange.class.getName() + ".TOKEN";

    private final Auditor auditor;
    private final SimpleHttpClient httpClient;
    private final URL tokenServiceUrl;
    private final WssProcessor trogdor = new WssProcessorImpl();
    private final SecurityTokenResolver securityTokenResolver;

    public ServerWsTrustCredentialExchange(WsTrustCredentialExchange assertion, ApplicationContext springContext) {
        super(assertion, CACHE_SEC_TOKEN_KEY);
        this.httpClient = new SimpleHttpClient(springContext.getBean( "anonUrlHttpClientFactory", GenericHttpClientFactory.class ).createHttpClient());
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
            securityTokenResolver = (SecurityTokenResolver)springContext.getBean("securityTokenResolver");
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED, null, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        XmlKnob requestXml = context.getRequest().getKnob(XmlKnob.class);
        if (requestXml == null) {
            auditor.logAndAudit(AssertionMessages.WSTRUST_NON_XML_MESSAGE);
            return AssertionStatus.FAILED;
        }
        SecurityKnob requestSec = context.getRequest().getKnob(SecurityKnob.class);

        // Try to get credentials from WSS processor results
        XmlSecurityToken originalToken = null;
        Element originalTokenElement = null;
        if(requestSec!=null) {
            ProcessorResult wssProcResult = requestSec.getProcessorResult();
            if (wssProcResult != null) {
                XmlSecurityToken[] tokens = wssProcResult.getXmlSecurityTokens();
                for (XmlSecurityToken token : tokens) {
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
        }

        // Try to get non-WSS credentials
        if (originalToken == null) {
            LoginCredentials creds = context.getAuthenticationContext(context.getRequest()).getLastCredentials();
            if (creds != null) {
                Object payload = creds.getPayload();
                if (payload instanceof XmlSecurityToken) {
                    originalToken = (XmlSecurityToken)payload;
                } else if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
                    originalToken = new UsernameTokenImpl(creds.getLogin(), creds.getCredentials());
                }
            }
        }

        if (originalToken == null) {
            auditor.logAndAudit(AssertionMessages.WSTRUST_NO_SUITABLE_CREDENTIALS);
            return AssertionStatus.FAILED;
        }

        SecurityToken secTok = super.getCachedSecurityToken(context.getCache());

        try {
            Object rstrObj;

            if(secTok==null) {
                // Create RST
                Document rstDoc;
                GenericHttpRequestParams params;
                WsTrustConfig wstConfig = assertion.getWsTrustNamespace()==null ?
                        WsTrustConfigFactory.getDefaultWsTrustConfig() :
                        WsTrustConfigFactory.getWsTrustConfigForNamespaceUri( assertion.getWsTrustNamespace() );
                TokenServiceClient tokenServiceClient = new TokenServiceClient(wstConfig);
                rstDoc = tokenServiceClient.createRequestSecurityTokenMessage(null,
                                                                              assertion.getRequestType(),
                                                                              originalToken,
                                                                              assertion.getAppliesTo(),
                                                                              assertion.getIssuer());

                params = new GenericHttpRequestParams(tokenServiceUrl);
                params.setContentType(ContentTypeHeader.XML_DEFAULT);

                params.setExtraHeaders(new HttpHeader[] { new GenericHttpHeader( SoapConstants.SOAPACTION, "\"\"") });

                // Get RSTR
                SimpleHttpClient.SimpleXmlResponse response = httpClient.postXml(params, rstDoc);
                int status = response.getStatus();
                if (status != 200) {
                    auditor.logAndAudit(AssertionMessages.WSTRUST_RSTR_STATUS_NON_200); // TODO use a better message
                    return AssertionStatus.AUTH_REQUIRED;
                }

                Document rstrDoc = response.getDocument();
                rstrObj = tokenServiceClient.parseUnsignedRequestSecurityTokenResponse(rstrDoc);
            }
            else {
                rstrObj = secTok;
            }

            Document requestDoc = requestXml.getDocumentWritable(); // Don't actually want the document; just want to invalidate bytes
            if (originalTokenElement == null) {
                auditor.logAndAudit(AssertionMessages.WSTRUST_ORIGINAL_TOKEN_NOT_XML);
            } else {
                Node securityEl = originalTokenElement.getParentNode();
                securityEl.removeChild(originalTokenElement);
                // Check for empty Security header, remove
                if (securityEl.getFirstChild() == null) {
                    securityEl.getParentNode().removeChild(securityEl);
                }
            }

            DecorationRequirements decoReq = new DecorationRequirements();
            WssDecorator deco = new WssDecoratorImpl();
            decoReq.setSecurityHeaderReusable(true);
            if(requestSec!=null) { // ensure we don't add duplicate timestamp
                ProcessorResult wssProcResult = requestSec.getProcessorResult();
                if(wssProcResult!=null && wssProcResult.getTimestamp()!=null) {
                    decoReq.setIncludeTimestamp(false);
                }
            }
            if (rstrObj instanceof SamlAssertion) {
                SamlAssertion samlAssertion = (SamlAssertion) rstrObj;
                setCachedSecurityToken(context.getCache(), samlAssertion, getSamlAssertionExpiry(samlAssertion));
                context.getAuthenticationContext(context.getRequest()).addCredentials(LoginCredentials.makeLoginCredentials(samlAssertion, assertion.getClass()));
                decoReq.setSenderSamlToken(samlAssertion, false);
            } else if (rstrObj instanceof UsernameToken) {
                UsernameToken ut = (UsernameToken) rstrObj;
                setCachedSecurityToken(context.getCache(), ut, getUsernameTokenExpiry(ut));
                decoReq.setUsernameTokenCredentials(new UsernameTokenImpl(ut.getUsername(), ut.getPassword()));
            } else {
                auditor.logAndAudit(AssertionMessages.WSTRUST_RSTR_BAD_TYPE);
                return AssertionStatus.AUTH_REQUIRED;
            }

            addCacheInvalidator(context); // remove cached credentials if routing fails

            try {
                deco.decorateMessage(new Message(requestDoc, 0), decoReq);
                requestXml.setDocument(requestDoc);

                Message reqMessage = context.getRequest();
                SecurityKnob secKnob = requestSec!=null ? requestSec : reqMessage.getSecurityKnob();
                secKnob.setProcessorResult(trogdor.undecorateMessage(reqMessage, null, securityTokenResolver));
                return AssertionStatus.NONE;
            } catch (Exception e) {
                auditor.logAndAudit(AssertionMessages.WSTRUST_DECORATION_FAILED, null, e);
                return AssertionStatus.FAILED;
            }
        } catch (GenericHttpException e) {
            auditor.logAndAudit(AssertionMessages.WSTRUST_SERVER_HTTP_FAILED, null, e);
            return AssertionStatus.FAILED;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_ERROR_READING_RESPONSE, null, e);
            return AssertionStatus.FAILED;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_ERROR_READING_RESPONSE, null, e);
            return AssertionStatus.FAILED;
        } catch ( WsTrustConfigException e ) {
            auditor.logAndAudit(AssertionMessages.WSTRUST_NOT_SUPPORTED, assertion.getWsTrustNamespace());
            return AssertionStatus.FAILED;
        }
    }
}
