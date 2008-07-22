package com.l7tech.server.policy.assertion.credential;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.message.XmlKnob;
import com.l7tech.message.SecurityKnob;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.wsfederation.FederationPassiveClient;
import com.l7tech.security.wsfederation.InvalidHtmlException;
import com.l7tech.security.wsfederation.InvalidTokenException;
import com.l7tech.security.wsfederation.ResponseStatusException;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.server.message.PolicyContextCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server implementation of the WS-Federation PRP (http://msdn.microsoft.com/ws/2003/07/ws-passive-profile/).
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerWsFederationPassiveTokenExchange extends AbstractServerWsFederationPassiveRequestProfile {

    //- PUBLIC

    /**
     *
     */
    public ServerWsFederationPassiveTokenExchange(WsFederationPassiveTokenExchange assertion, ApplicationContext springContext) {
        super(assertion, CACHE_SAML_KEY, springContext);

        this.assertion = assertion;
        this.auditor = new Auditor(this, springContext, logger);
        this.httpClient = new UrlConnectionHttpClient();

        try {
            if (assertion.getIpStsUrl() == null || assertion.getIpStsUrl().length()==0) {
                if(!isAuthAssertion(assertion)) {
                    logger.warning("Null IP/STS URL, assertion is non-functional");
                }
                this.ipStsUrl = null;
            }
            else {
                this.ipStsUrl = new URL(assertion.getIpStsUrl());
                if(this.ipStsUrl.getQuery()!=null) {
                    throw new IllegalArgumentException("IP/STS URL has a query string");
                }
            }
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unable to parse IP/STS URL", e);
        }
    }

    /**
     *
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus result = AssertionStatus.FAILED;

        try {
            if(ipStsUrl==null && !isAuthAssertion(assertion)) throw new StopAndAuditException(AssertionMessages.WSFEDPASS_CONFIG_INVALID);
            result = doCheckRequest(context);
        }
        catch(AuthRequiredException are) {
            result = AssertionStatus.AUTH_REQUIRED;
        }
        catch(StopAndAuditException saae) {
            auditor.logAndAudit(saae.getAssertionMessage(), null, saae.getCause());
        }

        return result;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerWsFederationPassiveTokenExchange.class.getName());

    /**
     * Key for cached SAML assertion
     */
    private static final String CACHE_SAML_KEY = ServerWsFederationPassiveTokenExchange.class.getName() + ".SAML";

    private final Auditor auditor;
    private final GenericHttpClient httpClient;
    private final URL ipStsUrl;
    private final WsFederationPassiveTokenExchange assertion;

    /**
     * Return true if the given assertion is configured to authenticate
     */
    private boolean isAuthAssertion(WsFederationPassiveTokenExchange wsfedAssertion) {
        return wsfedAssertion.isAuthenticate() && wsfedAssertion.getReplyUrl()!=null;
    }

    /**
     *
     */
    private SamlAssertion getToken(SecurityKnob secKnob) throws StopAndAuditException {
        SamlAssertion token = null;

        ProcessorResult wssProcResult = secKnob.getProcessorResult();
        if (wssProcResult != null) {
            XmlSecurityToken[] tokens = wssProcResult.getXmlSecurityTokens();
            for (int i = 0; i < tokens.length; i++) {
                XmlSecurityToken currentToken = tokens[i];
                if (currentToken instanceof SamlAssertion) {
                    if (token == null) {
                        token = (SamlAssertion) currentToken;
                    } else {
                        throw new StopAndAuditException(AssertionMessages.WSFEDPASS_MULTI_TOKENS);
                    }
                }
            }
        }

        return token;
    }

    /**
     *
     */
    private SamlAssertion getFederationToken(SamlAssertion partnerAssertion) throws AuthRequiredException, StopAndAuditException {
        SamlAssertion samlAssertion;

        try {
            GenericHttpRequestParams params = new GenericHttpRequestParams(ipStsUrl);
            initParams(params);

            XmlSecurityToken token = FederationPassiveClient.exchangeFederationToken(httpClient, params, partnerAssertion, assertion.getContext(), false);
            if(token instanceof SamlAssertion) {
                samlAssertion = (SamlAssertion) token;
            }
            else {
                throw new InvalidTokenException("Unsupported token type");
            }
        }
        catch(ResponseStatusException rse) {
            auditor.logAndAudit(AssertionMessages.WSFEDPASS_RSTR_STATUS_NON_200); // TODO use a better message
            throw new AuthRequiredException();
        }
        catch(InvalidHtmlException ihe) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_SERVER_HTML_INVALID, causeOrSelf(ihe));
        }
        catch(InvalidTokenException ite) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_RSTR_BAD_TYPE, causeOrSelf(ite));
        }
        catch(IOException ioe) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_SERVER_HTTP_FAILED, causeOrSelf(ioe));
        }

        return samlAssertion;
    }


    /**
     *
     */
    private void doAuth(PolicyEnforcementContext context, SamlAssertion samlAssertion, boolean allowDeferred) throws AuthRequiredException, StopAndAuditException {
        doAuth(context, httpClient, samlAssertion, assertion.getReplyUrl(), assertion.getContext(), allowDeferred);
    }

    /**
     *
     */
    private void updateRequestXml(PolicyEnforcementContext context, XmlKnob requestXml, SecurityKnob requestSec, SamlAssertion existingToken, SamlAssertion samlAssertion) throws StopAndAuditException {
        try {
            Document requestDoc = requestXml.getDocumentWritable(); // Don't actually want the document; just want to invalidate bytes

            //TODO does this do anything? isn't the whole document replaced below?? (also in wstrust credential exchange)
            Element tokenElement = existingToken.asElement();
            Node securityEl = tokenElement.getParentNode();
            securityEl.removeChild(tokenElement);
            // Check for empty Security header, remove
            if (securityEl.getFirstChild() == null) {
                securityEl.getParentNode().removeChild(securityEl);
            }

            updateRequestXml(context, requestXml, requestSec, requestDoc, samlAssertion, assertion);
        } catch (Exception e) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_DECORATION_FAILED, e);
        }
    }


    /**
     *
     */
    private AssertionStatus doCheckRequest(PolicyEnforcementContext context) throws AuthRequiredException, StopAndAuditException {
        XmlKnob requestXml = (XmlKnob)context.getRequest().getKnob(XmlKnob.class);
        ensureXmlRequest(requestXml);
        SecurityKnob requestSec = (SecurityKnob)context.getRequest().getKnob(SecurityKnob.class);
        ensureSecurityKnob(requestSec);

        // Try to get credentials from WSS processor results
        SamlAssertion existingToken = getToken(requestSec);

        // Error if there is no token at all
        if (existingToken == null) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_NO_SUITABLE_CREDENTIALS);
        }

        SamlAssertion samlAssertion;
        boolean usingCachedToken = true;
        if(ipStsUrl!=null) { // else is auth only, no token exchange
            // Attempt to get from cache
            PolicyContextCache cache = context.getCache();
            samlAssertion = (SamlAssertion) getCachedSecurityToken(cache);

            // Get from server if necessary
            if(samlAssertion==null) {
                usingCachedToken = false;
                samlAssertion = getFederationToken(existingToken);
                setCachedSecurityToken(cache, samlAssertion, getSamlAssertionExpiry(samlAssertion));
            }
            else {
                if(logger.isLoggable(Level.FINE)) logger.fine("Using cached SAML assertion");
            }

            // Update request XML
            updateRequestXml(context, requestXml, requestSec, existingToken, samlAssertion);

            // add cache check to run after routing
            addCacheInvalidator(context);
        }
        else {
            samlAssertion = existingToken;
        }

        // POST to endpoint (AUTH), GET COOKIES
        if(assertion.isAuthenticate()) {
            doAuth(context, samlAssertion, usingCachedToken);
        }

        // add response fault customizer
        addResponseAuthFailureDecorator(context);

        // success
        return AssertionStatus.NONE;
    }
}
