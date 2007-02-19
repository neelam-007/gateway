/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.DefaultStashManagerFactory;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.IdentityBindingHttpClientFactory;
import com.l7tech.server.util.HttpForwardingRuleEnforcer;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side implementation of HTTP routing assertion.
 */
public final class ServerHttpRoutingAssertion extends AbstractServerHttpRoutingAssertion<HttpRoutingAssertion> {
    public static final String USER_AGENT = HttpConstants.HEADER_USER_AGENT;
    public static final String HOST = HttpConstants.HEADER_HOST;

    private static final Logger logger = Logger.getLogger(ServerHttpRoutingAssertion.class.getName());
    private static final String IV_USER = "IV_USER";

    private final SignerInfo senderVouchesSignerInfo;
    private final GenericHttpClientFactory httpClientFactory;
    private final StashManagerFactory stashManagerFactory;
    private final HostnameVerifier hostnameVerifier;
    private final FailoverStrategy failoverStrategy;
    private final String[] varNames;
    private final int maxFailoverAttempts;
    private final SSLSocketFactory socketFactory;
    private final boolean urlUsesVariables;
    private final URL protectedServiceUrl;

    public ServerHttpRoutingAssertion(HttpRoutingAssertion assertion, ApplicationContext ctx) {
        super(assertion, ctx, logger);

        // remember if we need to resolve the url at runtime
        String tmp = data.getProtectedServiceUrl();
        if (tmp != null) {
            urlUsesVariables = tmp.indexOf("${") > -1;
        } else {
            logger.info("this http routing assertion has null url");
            urlUsesVariables = false;
        }
        if (urlUsesVariables || data.getProtectedServiceUrl()==null) {
            protectedServiceUrl = null;
        } else {
            URL url = null;
            try {
                url = new URL(data.getProtectedServiceUrl());
            } catch (MalformedURLException murle) {
                logger.log(Level.WARNING, "Invalid protected service URL.", murle);
            }
            protectedServiceUrl = url;
        }

        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            final X509TrustManager trustManager = (X509TrustManager)applicationContext.getBean("trustManager");
            hostnameVerifier = (HostnameVerifier)applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
            final KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore");
            sslContext.init(ku.getSSLKeyManagerFactory().getKeyManagers(), new TrustManager[]{trustManager}, null);
            final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT, HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            socketFactory = sslContext.getSocketFactory();
            senderVouchesSignerInfo = ku.getSslSignerInfo();
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED, null, e);
            throw new RuntimeException(e);
        }

        GenericHttpClientFactory factory;
        try {
            factory = (GenericHttpClientFactory) applicationContext.getBean("httpRoutingHttpClientFactory", GenericHttpClientFactory.class);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not create HTTP client factory.", e);
            factory = new IdentityBindingHttpClientFactory();
        }
        httpClientFactory = factory;

        StashManagerFactory smFactory;
        try {
            smFactory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not create stash manager factory.", e);
            smFactory = DefaultStashManagerFactory.getInstance();
        }
        stashManagerFactory = smFactory;

        final String[] addrs = data.getCustomIpAddresses();
        if (addrs != null && addrs.length > 0 && areValidUrlHostnames(addrs)) {
            final String stratName = assertion.getFailoverStrategyName();
            FailoverStrategy strat;
            try {
                strat = FailoverStrategyFactory.createFailoverStrategy(stratName, addrs);
            } catch (IllegalArgumentException e) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_BAD_STRATEGY_NAME, new String[] { stratName }, e);
                strat = new StickyFailoverStrategy(addrs);
            }
            failoverStrategy = AbstractFailoverStrategy.makeSynchronized(strat);
            maxFailoverAttempts = addrs.length;
        } else {
            failoverStrategy = null;
            maxFailoverAttempts = 1;
        }

        varNames = assertion.getVariablesUsed();
    }


    public static final String PRODUCT = "Layer7-SecureSpan-Gateway";

    public static final String DEFAULT_USER_AGENT = PRODUCT + "/v" + BuildInfo.getProductVersion() + "-b" + BuildInfo.getBuildNumber();

    /**
     * Forwards the request along to a ProtectedService at the configured URL.
     *
     * @param context
     * @return an AssertionStatus indicating the success or failure of the request.
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          if some error preventing the execution of the PolicyAssertion has occurred.
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        URL u;
        try {
            PublishedService service = context.getService();
            context.routingStarted();
            try {
                u = getProtectedServiceUrl(service, context);
            } catch (WSDLException we) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, we);
                return AssertionStatus.FAILED;
            }

            applicationContext.publishEvent(new PreRoutingEvent(this, context, u));
            if (failoverStrategy == null)
                return tryUrl(context, u);

            String failedHost = null;
            for (int tries = 0; tries < maxFailoverAttempts; tries++) {
                String host = (String)failoverStrategy.selectService();
                if (host == null) {
                    // strategy says it's time to give up
                    break;
                }
                if (failedHost != null)
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_FAILOVER_FROM_TO,
                                        new String[] { failedHost, host });
                URL url = new URL(u.getProtocol(), host, u.getPort(), u.getFile());
                AssertionStatus result = tryUrl(context, url);
                if (result == AssertionStatus.NONE) {
                    failoverStrategy.reportSuccess(host);
                    return result;
                }
                failedHost = host;
                failoverStrategy.reportFailure(host);
            }

            auditor.logAndAudit(AssertionMessages.HTTPROUTE_TOO_MANY_ATTEMPTS);
            return AssertionStatus.FAILED;
        } finally {
            context.routingFinished();
        }
    }

    private AssertionStatus tryUrl(PolicyEnforcementContext context, URL url) throws PolicyAssertionException {
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
        context.setRoutedServiceUrl(url);

        Throwable thrown = null;
        try {
            GenericHttpRequestParams routedRequestParams = new GenericHttpRequestParams(url);
            routedRequestParams.setSslSocketFactory(socketFactory);
            routedRequestParams.setHostnameVerifier(hostnameVerifier);

            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            handleProcessedSecurityHeader(context, data.getCurrentSecurityHeaderHandling(),
                                          data.getXmlSecurityActorToPromote());

            String userAgent = data.getUserAgent();
            if (userAgent == null || userAgent.length() == 0) userAgent = DEFAULT_USER_AGENT;
            routedRequestParams.addExtraHeader(new GenericHttpHeader(USER_AGENT, userAgent));

            StringBuffer hostValue = new StringBuffer(url.getHost());
            int port = url.getPort();
            if (port != -1) {
                hostValue.append(":");
                hostValue.append(port);
            }
            routedRequestParams.addExtraHeader(new GenericHttpHeader(HOST, hostValue.toString()));

            HttpRequestKnob httpRequestKnob = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);

            if (data.isTaiCredentialChaining()) {
                doTaiCredentialChaining(context, routedRequestParams, url);
            }

            String login = data.getLogin();
            String password = data.getPassword();
            String domain = data.getRealm();
            String host = data.getNtlmHost();

            Map vars = null;
            if (login != null && login.length() > 0 && password != null && password.length() > 0) {
                if (vars == null) {
                    vars = context.getVariableMap(varNames, auditor);
                }
                login = ExpandVariables.process(login, vars);
                password = ExpandVariables.process(password, vars);
                if (domain != null) domain = ExpandVariables.process(domain, vars);
                if (host != null) host = ExpandVariables.process(host, vars);

                auditor.logAndAudit(AssertionMessages.HTTPROUTE_LOGIN_INFO, new String[] {login});
                if (domain != null && domain.length() > 0) {
                    if (host == null) {
                        host = System.getProperty("java.rmi.server.hostname", "");
                    }
                    routedRequestParams.setNtlmAuthentication(new NtlmAuthentication(login, password.toCharArray(), domain, host));
                } else {
                    routedRequestParams.setPreemptiveAuthentication(true);
                    routedRequestParams.setPasswordAuthentication(new PasswordAuthentication(login, password.toCharArray()));
                }
            }

            if (data.isAttachSamlSenderVouches()) {
                doAttachSamlSenderVouches(context, senderVouchesSignerInfo);
            } else if (data.isPassthroughHttpAuthentication() && httpRequestKnob != null) {
                String[] authHeaders = httpRequestKnob.getHeaderValues(HttpConstants.HEADER_AUTHORIZATION);
                boolean passed = false;
                for (String authHeader : authHeaders) {
                    passed = true;
                    routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, authHeader));
                }
                if (passed) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_REQUEST);
                } else {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_REQUEST_NC);
                }
            }

            return reallyTryUrl(context, routedRequestParams, url, true, vars);
        } catch (MalformedURLException mfe) {
            thrown = mfe;
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, mfe);
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            thrown = ioe;
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, ioe);
        } catch (SAXException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
        } catch (SignatureException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
        } catch (CertificateException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
        }
        finally {
            if(context.getRoutingStatus()!=RoutingStatus.ROUTED) {
                RoutingResultListener rrl = context.getRoutingResultListener();
                rrl.failed(url, thrown, context);
            }
        }
        return AssertionStatus.FAILED;
    }

    /**
     *
     */
    private void doTaiCredentialChaining(PolicyEnforcementContext context, GenericHttpRequestParams routedRequestParams, URL url) {
        String chainId = null;
        if (!context.isAuthenticated()) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_NOT_AUTHENTICATED);
        } else {
            User clientUser = context.getAuthenticatedUser();
            if (clientUser != null) {
                String id = clientUser.getLogin();
                if (id == null || id.length() < 1) id = clientUser.getName();
                if (id == null || id.length() < 1) id = clientUser.getId();

                if (id != null && id.length() > 0) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_CHAIN_USERNAME, new String[] {id});
                    chainId = id;
                } else
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_NO_USER_ID, new String[] {id});
            } else {
                final String login = context.getCredentials().getLogin();
                if (login != null && login.length() > 0) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_CHAIN_LOGIN, new String[] {login});
                    chainId = login;
                } else
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_NO_USER);
            }

            if (chainId != null && chainId.length() > 0) {
                routedRequestParams.addExtraHeader(new GenericHttpHeader(IV_USER, chainId));
                HttpCookie ivUserCookie = new HttpCookie(IV_USER, chainId, 0, url.getPath(), url.getHost());
                Collection cookies = Collections.singletonList(ivUserCookie);
                routedRequestParams.addExtraHeader(
                        new GenericHttpHeader(HttpConstants.HEADER_COOKIE,
                                              HttpCookie.getCookieHeader(cookies)));

                // there is no defined quoting or escape mechanism for HTTP cookies so we'll use URLEncoding
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADD_OUTGOING_COOKIE, new String[] {IV_USER});
            }
        }
    }

    private GenericHttpClient.GenericHttpMethod methodFromRequest(PolicyEnforcementContext context, GenericHttpRequestParams routedRequestParams) {
        if (context.getRequest().isHttpRequest()) {
            HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();
            // Check the request method
            String requestMethod = httpRequestKnob.getMethod();
            if (requestMethod.equals("GET")) {
                routedRequestParams.setFollowRedirects(assertion.isFollowRedirects());
                return GenericHttpClient.GET;
            } else if (requestMethod.equals("POST")) {
                // redirects not supported under POST
                return GenericHttpClient.POST;
            } else if (requestMethod.equals("PUT")) {
                // redirects not supported under PUT
                return GenericHttpClient.PUT;
            }  else if (requestMethod.equals("DELETE")) {
                routedRequestParams.setFollowRedirects(assertion.isFollowRedirects());
                return GenericHttpClient.DELETE;
            } else {
                logger.severe("Unexpected method " + requestMethod);
            }
        } else {
            logger.info("assuming http method for downstream service (POST) because " +
                        "there is no incoming http method to base this on");
        }
        return GenericHttpClient.POST;
    }

    private AssertionStatus reallyTryUrl(PolicyEnforcementContext context, GenericHttpRequestParams routedRequestParams,
                                         URL url, boolean allowRetry, Map vars) throws PolicyAssertionException {
        GenericHttpRequest routedRequest = null;
        GenericHttpResponse routedResponse = null;
        int status = -1;
        try {
            // Set the HTTP version 1.0 for not accepting the chunked Transfer Encoding
            // todo: check if we need to support HTTP 1.1.

            // Serialize the request
            final MimeKnob reqMime = context.getRequest().getMimeKnob();

            // Fix for Bug #1282 - Must set a content-length on PostMethod or it will try to buffer the whole thing
            final long contentLength = reqMime.getContentLength();
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Body content is too long to be processed -- maximum is " + Integer.MAX_VALUE + " bytes");

            Object connectionId = null;
            if (!data.isPassthroughHttpAuthentication() &&
                routedRequestParams.getNtlmAuthentication() == null &&
                routedRequestParams.getPasswordAuthentication() == null) {
                routedRequestParams.setContentLength(contentLength);
            }
            else if (data.isPassthroughHttpAuthentication() && context.getRequest().isHttpRequest()){
                connectionId = context.getRequest().getHttpRequestKnob().getConnectionIdentifier();
            }

            // this will forward soapaction, content-type, cookies, etc based on assertion settings
            HttpForwardingRuleEnforcer.handleRequestHeaders(routedRequestParams, context, url.getHost(),
                                                            data.getRequestHeaderRules(), auditor, vars, varNames);

            GenericHttpClient httpClient = httpClientFactory.createHttpClient(
                                                                 getMaxConnectionsPerHost(),
                                                                 getMaxConnectionsAllHosts(),
                                                                 getConnectionTimeout(),
                                                                 getTimeout(),
                                                                 connectionId);
            GenericHttpClient.GenericHttpMethod method = methodFromRequest(context, routedRequestParams);

            // dont add content-type for get and deletes
            if (method == GenericHttpClient.PUT || method == GenericHttpClient.POST) {
                routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, reqMime.getOuterContentType().getFullValue()));
            }
            routedRequest = httpClient.createRequest(method, routedRequestParams);

            List<HttpForwardingRuleEnforcer.Param> paramRes = HttpForwardingRuleEnforcer.
                    handleRequestParameters(context, assertion.getRequestParamRules(), auditor, vars, varNames);


            if (paramRes != null && paramRes.size() > 0) {
                for (HttpForwardingRuleEnforcer.Param p : paramRes) {
                    routedRequest.addParameter(p.name, p.value);
                }
            } else {
                // only include payload if the method is POST or PUT
                if (method == GenericHttpClient.POST || method == GenericHttpClient.PUT) {
                    if (routedRequest instanceof RerunnableHttpRequest) {
                        RerunnableHttpRequest rerunnableHttpRequest = (RerunnableHttpRequest) routedRequest;
                        rerunnableHttpRequest.setInputStreamFactory(new RerunnableHttpRequest.InputStreamFactory() {
                            public InputStream getInputStream() {
                                try {
                                    return reqMime.getEntireMessageBodyAsInputStream();
                                } catch (NoSuchPartException nspe) {
                                    return new IOExceptionThrowingInputStream(new CausedIOException("Cannot access mime part.", nspe));
                                } catch (IOException ioe) {
                                    return new IOExceptionThrowingInputStream(ioe);
                                }
                            }
                        });
                    } else {
                        final InputStream bodyInputStream = reqMime.getEntireMessageBodyAsInputStream();
                        routedRequest.setInputStream(bodyInputStream);
                    }
                }
            }
            long latencyTimerStart = System.currentTimeMillis();
            routedResponse = routedRequest.getResponse();

            status = routedResponse.getStatus();

            boolean readOk = readResponse(context, routedResponse, status);
            long latencyTimerEnd = System.currentTimeMillis();
            if (readOk) {
                long latency = latencyTimerEnd - latencyTimerStart;
                context.setVariable(HttpRoutingAssertion.VAR_ROUTING_LATENCY, ""+latency);
            }

            RoutingResultListener rrl = context.getRoutingResultListener();
            boolean retryRequested = allowRetry && rrl.reroute(url, status, routedResponse.getHeaders(), context); // only call listeners if retry is allowed

            if (status != HttpConstants.STATUS_OK && retryRequested) {
                // retry after if requested by a routing result listener
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS_HANDLED, new String[] {url.getPath(), String.valueOf(status)});
                return reallyTryUrl(context, routedRequestParams, url, false, vars);
            }

            if (status == HttpConstants.STATUS_OK)
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_OK);
            else if (data.isPassthroughHttpAuthentication() && status == HttpConstants.STATUS_UNAUTHORIZED) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_CHALLENGE);
            } else {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS, new String[] {url.getPath(), String.valueOf(status)});
            }

            HttpResponseKnob httpResponseKnob = (HttpResponseKnob) context.getResponse().getKnob(HttpResponseKnob.class);
            if (httpResponseKnob != null) {
                httpResponseKnob.setStatus(status);

                HttpForwardingRuleEnforcer.handleResponseHeaders(routedResponse, httpResponseKnob, auditor,
                                                                 data.getResponseHeaderRules(), context,
                                                                 routedRequestParams, vars, varNames);
            }
            if (data.isPassthroughHttpAuthentication()) {
                boolean passed = false;
                List wwwAuthValues = routedResponse.getHeaders().getValues(HttpConstants.HEADER_WWW_AUTHENTICATE);
                if (wwwAuthValues != null) {
                    for (Object wwwAuthValue : wwwAuthValues) {
                        String value = (String) wwwAuthValue;
                        httpResponseKnob.addChallenge(value);
                        passed = true;
                    }
                }
                if (passed) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_RESPONSE);
                } else if (status != HttpConstants.STATUS_UNAUTHORIZED) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_RESPONSE_NC);
                } else {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS, new String[] {url.getPath(), String.valueOf(status)});
                }
            }

            context.setRoutingStatus(RoutingStatus.ROUTED);

            // notify listeners
            rrl.routed(url, status, routedResponse.getHeaders(), context);

            if (!readOk) return AssertionStatus.FALSIFIED;

            return AssertionStatus.NONE;
        } catch (MalformedURLException mfe) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, mfe);
        } catch (UnknownHostException uhe) {
            auditor.logAndAudit(AssertionMessages.HTTP_UNKNOWN_HOST, new String[]{uhe.getMessage()});
            return AssertionStatus.FAILED;
        } catch (SocketException se) {
            auditor.logAndAudit(AssertionMessages.HTTP_SOCKET_EXCEPTION, new String[]{se.getMessage()});
            return AssertionStatus.FAILED;
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, ioe);
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
        } finally {
            if (routedRequest != null || routedResponse != null) {
                final GenericHttpRequest req = routedRequest;
                final GenericHttpResponse resp = routedResponse;
                context.runOnClose(new Runnable() {
                    public void run() {
                        if (resp != null) resp.close();
                        if (req != null) req.close();
                    }
                });
            }
            applicationContext.publishEvent(new PostRoutingEvent(this, context, url, status));
        }

        return AssertionStatus.FAILED;
    }

    /**
     * Read the routing response and copy into the SSG response.
     *
     * @return false if falsified (if a non-xml response was wrapped)
     */
    private boolean readResponse(PolicyEnforcementContext context, GenericHttpResponse routedResponse, int status) {
        boolean responseOk = true;
        try {
            InputStream responseStream = routedResponse.getInputStream();
            String ctype = routedResponse.getHeaders().getOnlyOneValue(HttpConstants.HEADER_CONTENT_TYPE);
            ContentTypeHeader outerContentType = ctype !=null ? ContentTypeHeader.parseValue(ctype) : null;
            // Handle missing content type error
            if (status == HttpConstants.STATUS_OK && outerContentType==null) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_NOCONTENTTYPE, new String[]{Integer.toString(status)});
                responseOk = false;
            } else if (data.isPassthroughHttpAuthentication() &&
                       status == HttpConstants.STATUS_UNAUTHORIZED) {
                context.getResponse().initialize(stashManagerFactory.createStashManager(), outerContentType, responseStream);
                responseOk = false;
            } else if (status != HttpConstants.STATUS_OK && outerContentType!=null &&
                       (outerContentType.isText() || outerContentType.isHtml()) && !outerContentType.isXml()) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_NOXML, new String[] {Integer.toString(status)});
                responseOk = false;
            }
            // response OK
            else if(outerContentType!=null)
            {
                StashManager stashManager = stashManagerFactory.createStashManager();
                context.getResponse().initialize(stashManager, outerContentType, responseStream);
            }
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_ERROR_READING_RESPONSE, null, e);
        }
        return responseOk;
    }

    private URL getProtectedServiceUrl(PublishedService service, PolicyEnforcementContext context) throws WSDLException, MalformedURLException {
        URL url = protectedServiceUrl; // protectedServiceUrl only set if we are no using variables and url is valid

        if (url == null) {
            String psurl;
            if (urlUsesVariables) {
               psurl = ExpandVariables.process(data.getProtectedServiceUrl(), context.getVariableMap(varNames, auditor));
            } else {
               psurl = data.getProtectedServiceUrl();
            }

            if (psurl == null) {
                logger.info("assertion's url was null, falling back on service's url value");
                url = service.serviceUrl();
            } else {
                url = new URL(psurl);
            }
        }

        return url;
    }
}
