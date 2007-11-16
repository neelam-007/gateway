package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.http.*;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.security.wsfederation.FederationPassiveClient;
import com.l7tech.common.security.wsfederation.ResponseStatusException;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessor;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for WsFederation server assertions.
 */
public abstract class AbstractServerWsFederationPassiveRequestProfile extends AbstractServerCachedSecurityTokenAssertion {

    //- PROTECTED

    /**
     *
     */
    protected AbstractServerWsFederationPassiveRequestProfile(Assertion assertion, String samlCacheKey, ApplicationContext springContext) {
        super(assertion, samlCacheKey);

        this.authCookieSet = new CopyOnWriteArraySet();
        this.auditor = new Auditor(this, springContext, logger);

        try {
            sslContext = SSLContext.getInstance("SSL");
            final X509TrustManager trustManager = (X509TrustManager) springContext.getBean("trustManager");
            hostnameVerifier = (HostnameVerifier)springContext.getBean("hostnameVerifier", HostnameVerifier.class);
            final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT,
                                                   HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            sslContext.init(null, new TrustManager[]{trustManager}, null);

            trogdor = new WssProcessorImpl();

            securityTokenResolver = (SecurityTokenResolver)springContext.getBean("securityTokenResolver");
        }
        catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED, null, e);
            throw new IllegalStateException("Error during initialization of SSL context", e);
        }
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
    protected void initParams(GenericHttpRequestParams params) {
        params.setSslSocketFactory(sslContext.getSocketFactory());
        params.setHostnameVerifier(hostnameVerifier);
    }

    /**
     *
     */
    protected void updateRequestXml(PolicyEnforcementContext context, XmlKnob requestXml, SecurityKnob requestSec, Document requestDoc, SamlAssertion samlAssertion, Assertion credSource) throws Exception {
        WssDecorator deco = new WssDecoratorImpl();
        context.addCredentials(LoginCredentials.makeSamlCredentials(samlAssertion, credSource.getClass()));

        ProcessorResult processorResult = requestSec.getProcessorResult();
        DecorationRequirements decoReq = new DecorationRequirements();
        decoReq.setSecurityHeaderReusable(true);
        if(processorResult!=null && processorResult.getTimestamp()!=null) {
            // don't add a timestamp if there's one already
            decoReq.setIncludeTimestamp(false);
        }
        decoReq.setSenderSamlToken(samlAssertion.asElement(), false);
        deco.decorateMessage(new Message(requestDoc), decoReq);
        requestXml.setDocument(requestDoc);
        requestSec.setProcessorResult(trogdor.undecorateMessage(context.getRequest(), null, null, securityTokenResolver));
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
            public boolean reroute(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
                return false;
            }

            public void routed(URL attemptedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
                if (status == HttpConstants.STATUS_FOUND || status == HttpConstants.STATUS_SEE_OTHER) {
                    try {
                        String redirectUrl = headers.getOnlyOneValue(HttpConstants.HEADER_LOCATION);
                        if (redirectUrl == null) {
                            logger.info("Invalid headers from service, missing redirect location.");
                        } else {
                            if (FederationPassiveClient.isFederationServerUrl(new URL(attemptedUrl, redirectUrl))) {
                                auditor.logAndAudit(AssertionMessages.WSFEDPASS_UNAUTHORIZED);
                            }
                        }
                    } catch (GenericHttpException e) {
                        logger.info("Invalid headers from service, multiple redirect locations.");
                    } catch (MalformedURLException murle) {
                        logger.log(Level.INFO, "Invalid redirect url.", murle);
                    }
                }
            }

            public void failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context) {
            }
        });


    }

    /**
     *
     */
    protected static class StopAndAuditException extends Exception {

        public StopAndAuditException(AssertionMessages.M message) {
            super(message.getMessage());
            this.message = message;
        }

        public StopAndAuditException(AssertionMessages.M message, Throwable throwable) {
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

    private static final Logger logger = Logger.getLogger(AbstractServerWsFederationPassiveRequestProfile.class.getName());

    private final Auditor auditor;
    private final SSLContext sslContext;
    private final HostnameVerifier hostnameVerifier;
    private final WssProcessor trogdor;
    private final SecurityTokenResolver securityTokenResolver;
    private final Set authCookieSet;

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
            Set availableCookieNames = toNames(context.getCookies());
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
            initParams(params);

            Set cookies = FederationPassiveClient.postFederationToken(httpClient, params, samlAssertion, contextUrl, false);

            for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
                HttpCookie cookie = (HttpCookie) iterator.next();
                context.addCookie(cookie);
                ensureKnown(cookie);
            }
        }
        catch(ResponseStatusException rse) {
            auditor.logAndAudit(AssertionMessages.WSFEDPASS_AUTH_FAILED);
            throw new AuthRequiredException();
        }
        catch(IOException ioe) {
            throw new StopAndAuditException(AssertionMessages.WSFEDPASS_SERVER_HTTP_FAILED, causeOrSelf(ioe));
        }
    }

    /**
     *
     */
    private void doAuthLater(final PolicyEnforcementContext context, final GenericHttpClient httpClient, final SamlAssertion samlAssertion, final String replyUrl, final String contextUrl) {
        context.addRoutingResultListener(new RoutingResultListener(){
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
                                    auditor.logAndAudit(saae.getAssertionMessage(), null, saae.getCause());
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

            public void routed(URL attemptedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
            }

            public void failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context) {
            }
        });
    }
}
