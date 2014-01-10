package com.l7tech.server.policy.assertion.credential;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.common.http.*;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.message.Message;
import com.l7tech.security.wsfederation.FederationPassiveClient;
import com.l7tech.security.wsfederation.ResponseStatusException;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessor;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

/**
 * Base class for WsFederation server assertions.
 */
public abstract class AbstractServerWsFederationPassiveRequestProfile<AT extends Assertion> extends AbstractServerCachedSecurityTokenAssertion<AT> {

    //- PROTECTED

    /**
     *
     */
    protected AbstractServerWsFederationPassiveRequestProfile(AT assertion, String samlCacheKey, ApplicationContext springContext) {
        super(assertion, samlCacheKey);

        this.authCookieSet = new CopyOnWriteArraySet<String>();
        this.trogdor = new WssProcessorImpl();
        this.securityTokenResolver = (SecurityTokenResolver)springContext.getBean("securityTokenResolver");
    }

    /**
     *
     */
    protected void ensureXmlRequest(XmlKnob knob) throws StopAndAuditException {
        if(knob==null) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_NON_XML_MESSAGE);
        }
    }

    protected void ensureSecurityKnob(SecurityKnob knob) throws StopAndAuditException {
        if (knob == null) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_NO_SUITABLE_CREDENTIALS);
        }
    }

    /**
     * Get the cause if there is one, else return the given throwable
     */
    protected Throwable causeOrSelf(Throwable throwable) {
        Throwable result = throwable.getCause();

        if(result==null) {
            result = throwable;
        }

        return result;
    }

    /**
     *
     */
    protected void updateRequestXml(PolicyEnforcementContext context, XmlKnob requestXml, SecurityKnob requestSec, Document requestDoc, SamlAssertion samlAssertion, Assertion credSource) throws Exception {
        WssDecorator deco = new WssDecoratorImpl();
        context.getAuthenticationContext(context.getRequest()).addCredentials(LoginCredentials.makeLoginCredentials(samlAssertion, credSource.getClass()));

        ProcessorResult processorResult = requestSec.getProcessorResult();
        DecorationRequirements decoReq = new DecorationRequirements();
        decoReq.setSecurityHeaderReusable(true);
        if(processorResult!=null && processorResult.getTimestamp()!=null) {
            // don't add a timestamp if there's one already
            decoReq.setIncludeTimestamp(false);
        }
        decoReq.setSenderSamlToken(samlAssertion, false);
        deco.decorateMessage(new Message(requestDoc), decoReq);
        requestXml.setDocument(requestDoc);
        requestSec.setProcessorResult(trogdor.undecorateMessage(context.getRequest(), null, securityTokenResolver));
    }

    /**
     *
     */
    protected void doAuth(final PolicyEnforcementContext context, final GenericHttpClient httpClient, final SamlAssertion samlAssertion, final String replyUrl, final String contextUrl, boolean allowDeferred) throws AuthRequiredException, StopAndAuditException {
        if(allowDeferred && haveAuthCookies(context)) {
            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Not POSTing token to service [using cookie(s)].");
            }
            doAuthLater(context, httpClient, samlAssertion, replyUrl, contextUrl);
        }
        else {
            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "POSTing token to service.");
            }
            doAuthNow(context, httpClient, samlAssertion, replyUrl, contextUrl);
        }
    }

    /**
     *
     */
    protected void addResponseAuthFailureDecorator(final PolicyEnforcementContext pec) {
        pec.addRoutingResultListener(new RoutingResultListener(){
            @Override
            public boolean reroute(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
                return false;
            }

            @Override
            public void routed(URL attemptedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
                if (status == HttpConstants.STATUS_FOUND || status == HttpConstants.STATUS_SEE_OTHER) {
                    try {
                        String redirectUrl = headers.getOnlyOneValue(HttpConstants.HEADER_LOCATION);
                        if (redirectUrl == null) {
                            logger.info("Invalid headers from service, missing redirect location.");
                        } else {
                            if (FederationPassiveClient.isFederationServerUrl(new URL(attemptedUrl, redirectUrl))) {
                                logAndAudit(AssertionMessages.WSFEDPASS_UNAUTHORIZED);
                            }
                        }
                    } catch (GenericHttpException e) {
                        logger.info("Invalid headers from service, multiple redirect locations.");
                    } catch (MalformedURLException murle) {
                        logger.log(Level.INFO, "Invalid redirect url.", murle);
                    }
                }
            }

            @Override
            public void failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context) {
            }
        });


    }

    /**
     *
     */
    protected static class StopAndAuditException extends Exception {

        protected StopAndAuditException(AssertionMessages.M message) {
            super(message.getMessage());
            this.message = message;
        }

        protected StopAndAuditException(AssertionMessages.M message, Throwable throwable) {
            super(message.getMessage(), throwable);
            this.message = message;
        }

        public AssertionMessages.M getAssertionMessage() {
            return message;
        }

        private AssertionMessages.M message;
    }

    /**
     *
     */
    protected static class AuthRequiredException extends Exception {
    }

    //- PRIVATE

    private final WssProcessor trogdor;
    private final SecurityTokenResolver securityTokenResolver;
    private final Set<String> authCookieSet;

    /**
     *
     */
    private void ensureKnown(HttpCookie authCookie) {
        String cookieName = authCookie.getCookieName();
        if(!authCookieSet.contains(cookieName)) {
            authCookieSet.add(cookieName);
        }
    }

    /**
     *
     */
    private Set<String> toNames(Set<HttpCookie> cookies) {
        Set<String> cookieNames = new HashSet<String>();

        for (HttpCookie cookie : cookies) {
            cookieNames.add(cookie.getCookieName());
        }

        return cookieNames;
    }

    /**
     *
     */
    private boolean haveAuthCookies(PolicyEnforcementContext context) {
        boolean haveCookies = false;

        if(!authCookieSet.isEmpty()) {
            Set availableCookieNames = toNames(context.getRequest().getHttpCookiesKnob().getCookies());
            haveCookies = availableCookieNames.containsAll(authCookieSet);
        }

        return haveCookies;
    }

    /**
     *
     */
    private void doAuthNow(final PolicyEnforcementContext context, final GenericHttpClient httpClient, final SamlAssertion samlAssertion, final String replyUrl, final String contextUrl) throws AuthRequiredException, StopAndAuditException {
        try {
            URL endpoint = new URL(replyUrl);
            GenericHttpRequestParams params = new GenericHttpRequestParams(endpoint);

            Set<HttpCookie> cookies = FederationPassiveClient.postFederationToken(httpClient, params, samlAssertion, contextUrl, false);

            for  (HttpCookie cookie : cookies ) {
                context.getRequest().getHttpCookiesKnob().addCookie(cookie);
                ensureKnown(cookie);
            }
        }
        catch(ResponseStatusException rse) {
            logAndAudit(AssertionMessages.WSFEDPASS_AUTH_FAILED);
            throw new AuthRequiredException();
        }
        catch(IOException ioe) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_SERVER_HTTP_FAILED, causeOrSelf(ioe));
        }
    }

    /**
     *
     */
    private void doAuthLater(final PolicyEnforcementContext policyEnforcementContext,
                             final GenericHttpClient httpClient,
                             final SamlAssertion samlAssertion,
                             final String replyUrl,
                             final String contextUrl) {
        policyEnforcementContext.addRoutingResultListener(new RoutingResultListener(){
            @Override
            public boolean reroute(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
                boolean retry = false;
                if(status==HttpConstants.STATUS_FOUND || status==HttpConstants.STATUS_SEE_OTHER) {
                    try {
                        String redirectUrl = headers.getOnlyOneValue(HttpConstants.HEADER_LOCATION);
                        if(redirectUrl==null) {
                            logger.info("Invalid headers from service, missing redirect location.");
                        }
                        else {
                            if(FederationPassiveClient.isFederationServerUrl(new URL(routedUrl, redirectUrl))) {
                                try {
                                    if(logger.isLoggable(Level.FINER)) {
                                        logger.log(Level.FINER, "POSTing token to service after auth failure.");
                                    }
                                    doAuthNow(context, httpClient, samlAssertion, replyUrl, contextUrl);
                                    retry = true;
                                }
                                catch(AuthRequiredException are) { // already audited
                                }
                                catch(StopAndAuditException saae) {
                                    logAndAudit(saae.getAssertionMessage(), null, saae.getCause());
                                }
                            }
                        }
                    }
                    catch(GenericHttpException e) {
                        logger.info("Invalid headers from service, multiple redirect locations.");
                    }
                    catch(MalformedURLException murle) {
                        logger.log(Level.INFO, "Invalid redirect url.", murle);
                    }
                }
                return retry;
            }

            @Override
            public void routed(URL attemptedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
            }

            @Override
            public void failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context) {
            }
        });
    }
}
