package com.l7tech.server.policy.assertion.credential;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.message.XmlKnob;
import com.l7tech.message.SecurityKnob;
import com.l7tech.security.token.*;
import com.l7tech.security.wsfederation.FederationPassiveClient;
import com.l7tech.security.wsfederation.InvalidHtmlException;
import com.l7tech.security.wsfederation.InvalidTokenException;
import com.l7tech.security.wsfederation.ResponseStatusException;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.server.message.PolicyContextCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server implementation of the WS-Federation PRP (http://msdn.microsoft.com/ws/2003/07/ws-passive-profile/).
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerWsFederationPassiveTokenRequest extends AbstractServerWsFederationPassiveRequestProfile {

    //- PUBLIC

    /**
     *
     */
    public ServerWsFederationPassiveTokenRequest(WsFederationPassiveTokenRequest assertion, ApplicationContext springContext) {
        super(assertion, CACHE_SAML_KEY, springContext);
        this.assertion = assertion;
        this.auditor = new Auditor(this, springContext, logger);
        this.httpClient = new UrlConnectionHttpClient();

        try {
            if (assertion.getIpStsUrl() == null) {
                logger.warning("Null IP/STS URL, assertion is non-functional");
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
            if(ipStsUrl==null) throw new StopAndAuditException(AssertionMessages.WSFEDPASS_CONFIG_INVALID);
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

    private static final Logger logger = Logger.getLogger(ServerWsFederationPassiveTokenRequest.class.getName());

    /**
     * Key for cached SAML assertion
     */
    private static final String CACHE_SAML_KEY = ServerWsFederationPassiveTokenRequest.class.getName() + ".SAML";

    private final Auditor auditor;
    private final GenericHttpClient httpClient;
    private final URL ipStsUrl;
    private final WsFederationPassiveTokenRequest assertion;

    /**
     *
     */
    private UsernameToken getToken(SecurityKnob secKnob) throws StopAndAuditException {
        UsernameToken token = null;

        if(secKnob!=null) {
            ProcessorResult wssProcResult = secKnob.getProcessorResult();
            if (wssProcResult != null) {
                XmlSecurityToken[] tokens = wssProcResult.getXmlSecurityTokens();
                for (int i = 0; i < tokens.length; i++) {
                    XmlSecurityToken currentToken = tokens[i];
                    if (currentToken instanceof UsernameToken) {
                        if (token == null) {
                            token = (UsernameToken) currentToken;
                        } else {
                            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_MULTI_TOKENS);
                        }
                    }
                }
            }
        }

        if(logger.isLoggable(Level.FINEST)) logger.finest("Got XML security token?: " + (token!=null));

        return token;
    }

    /**
     *
     */
    private UsernameToken getToken(PolicyEnforcementContext context) {
        UsernameToken token = null;

        LoginCredentials creds = context.getLastCredentials();
        if (creds != null) {
            if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
                token = new UsernameTokenImpl(creds.getLogin(), creds.getCredentials());
            }
            else {
                if(logger.isLoggable(Level.FINEST)) logger.finest("Credentials are not clear text, ignoring.");
            }
        }

        if(logger.isLoggable(Level.FINEST)) logger.finest("Got non-XML security token?: " + (token!=null));

        return token;
    }

    /**
     *
     */
    private SamlAssertion getFederationToken(SecurityToken untok) throws AuthRequiredException, StopAndAuditException {
        SamlAssertion samlAssertion;

        try {
            String username;
            char[] pass;
            if (untok instanceof HasUsernameAndPassword) {
                HasUsernameAndPassword usernameAndPassword = (HasUsernameAndPassword)untok;
                username = usernameAndPassword.getUsername();
                pass = usernameAndPassword.getPassword();
            } else {
                throw new InvalidTokenException("Input token wasn't username/password");
            }
            if (pass == null) throw new InvalidTokenException("Input token didn't contain a password");

            GenericHttpRequestParams params = new GenericHttpRequestParams(ipStsUrl);
            initParams(params);
            params.setPasswordAuthentication(new PasswordAuthentication(username, pass));

            // replyUrl is the AUTH POST url not the routing url (could be the same thing)
            XmlSecurityToken token = FederationPassiveClient.obtainFederationToken(httpClient, params, assertion.getRealm(), assertion.getReplyUrl(), assertion.getContext(), assertion.isTimestamp());
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
    private void updateRequestXml(PolicyEnforcementContext context, XmlKnob requestXml, UsernameToken existingToken, SamlAssertion samlAssertion, boolean tokenFromRequest) throws StopAndAuditException {
        try {
            Document requestDoc = requestXml.getDocumentWritable(); // Don't actually want the document; just want to invalidate bytes
            if (!tokenFromRequest) {
                auditor.logAndAudit(AssertionMessages.WSFEDPASS_ORIGINAL_TOKEN_NOT_XML);
            } else {
                //NOTE: the token is in the requestDoc Document
                Element tokenElement = existingToken.asElement();
                Node securityEl = tokenElement.getParentNode();
                securityEl.removeChild(tokenElement);
                // Check for empty Security header, remove
                // NOTE: it is possible that we will end up with a header with just a timestamp
                if (securityEl.getFirstChild() == null) {
                    securityEl.getParentNode().removeChild(securityEl);
                }
            }

            // create the security knob if it did not previously exist
            SecurityKnob secKnob = context.getRequest().getSecurityKnob();
            updateRequestXml(context, requestXml, secKnob, requestDoc, samlAssertion, assertion);
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

        // Try to get credentials from WSS processor results
        boolean tokenFromRequest = true;
        UsernameToken existingToken = getToken(requestSec);

        // Try to get non-WSS credentials
        if (existingToken == null) {
            tokenFromRequest = false;
            existingToken = getToken(context);
        }

        // Error if there is no token at all
        if (existingToken == null) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_NO_SUITABLE_CREDENTIALS);
        }

        // Attempt to get from cache
        PolicyContextCache cache = context.getCache();
        boolean usingCachedToken = true;
        SamlAssertion samlAssertion = (SamlAssertion) getCachedSecurityToken(cache);

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
        updateRequestXml(context, requestXml, existingToken, samlAssertion, tokenFromRequest);

        // POST to endpoint (AUTH), GET COOKIES
        if(assertion.isAuthenticate()) {
            doAuth(context, samlAssertion, usingCachedToken);
        }

        // add cache check to run after routing
        addCacheInvalidator(context);

        // add response fault customizer
        addResponseAuthFailureDecorator(context);

        // success
        return AssertionStatus.NONE;
    }
}
