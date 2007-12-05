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
import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.security.SingleCertX509KeyManager;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.DefaultStashManagerFactory;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.xmlsec.ServerResponseWssSignature;
import com.l7tech.server.util.HttpForwardingRuleEnforcer;
import com.l7tech.server.util.IdentityBindingHttpClientFactory;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side implementation of HTTP routing assertion.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 */
public final class ServerHttpRoutingAssertion extends AbstractServerHttpRoutingAssertion<HttpRoutingAssertion> {
    public static final String USER_AGENT = HttpConstants.HEADER_USER_AGENT;
    public static final String HOST = HttpConstants.HEADER_HOST;

    private static final Logger logger = Logger.getLogger(ServerHttpRoutingAssertion.class.getName());

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
            final SignerInfo signerInfo;
            signerInfo = ServerResponseWssSignature.getSignerInfo(ctx, assertion);

            final KeyManager[] keyManagers;
            if (!assertion.isUsesDefaultKeyStore()) {
                X509Certificate[] certChain = signerInfo.getCertificateChain();
                PrivateKey privateKey = signerInfo.getPrivate();
                keyManagers = new KeyManager[] { new SingleCertX509KeyManager(certChain, privateKey) };
            } else {
                final KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore");
                keyManagers = ku.getSSLKeyManagers();
            }
            SSLContext sslContext = SSLContext.getInstance("SSL");

            final X509TrustManager trustManager = (X509TrustManager)applicationContext.getBean("routingTrustManager");
            hostnameVerifier = (HostnameVerifier)applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
            sslContext.init(keyManagers, new TrustManager[]{trustManager}, null);
            final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT, HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            socketFactory = sslContext.getSocketFactory();
            senderVouchesSignerInfo = signerInfo;
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
                            failedHost, host);
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
                login = ExpandVariables.process(login, vars, auditor);
                password = ExpandVariables.process(password, vars, auditor);
                if (domain != null) domain = ExpandVariables.process(domain, vars, auditor);
                if (host != null) host = ExpandVariables.process(host, vars, auditor);

                auditor.logAndAudit(AssertionMessages.HTTPROUTE_LOGIN_INFO, login);
                if (domain != null && domain.length() > 0) {
                    if (host == null) {
                        host = ServerConfig.getInstance().getPropertyCached("clusterHost");
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
            auditor.logAndAudit(AssertionMessages.GENERIC_ROUTING_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            thrown = ioe;
            auditor.logAndAudit(AssertionMessages.GENERIC_ROUTING_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (SAXException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.GENERIC_ROUTING_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (SignatureException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.GENERIC_ROUTING_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (CertificateException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.GENERIC_ROUTING_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } finally {
            if(context.getRoutingStatus()!=RoutingStatus.ROUTED) {
                RoutingResultListener rrl = context.getRoutingResultListener();
                rrl.failed(url, thrown, context);
            }
        }
        return AssertionStatus.FAILED;
    }

    private GenericHttpClient.GenericHttpMethod methodFromRequest(PolicyEnforcementContext context, GenericHttpRequestParams routedRequestParams) {
        if (assertion.getRequestMsgSrc() == null) {
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
        } else {
            logger.info("assuming http method for downstream service (POST) when request message source is a context variable");
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
            MimeKnob reqMime_ = null;
            if (assertion.getRequestMsgSrc() == null) {
                reqMime_ = context.getRequest().getMimeKnob();
            } else {
                final String variableName = assertion.getRequestMsgSrc();
                try {
                    final Object requestSrc = context.getVariable(variableName);
                    if (!(requestSrc instanceof Message)) {
                        // Should never happen.
                        throw new RuntimeException("Request message source (\"" + variableName +
                                "\") is a context variable of the wrong type (expected=" + Message.class + ", actual=" + requestSrc.getClass() + ").");
                    }
                    final Message requestMsg = (Message)requestSrc;
                    reqMime_ = requestMsg.getMimeKnob();
                } catch (NoSuchVariableException e) {
                    // Should never happen.
                    throw new RuntimeException("Request message source is a non-existent context variable (\"" + variableName + "\").");
                }
            }
            final MimeKnob reqMime = reqMime_;

            // Fix for Bug #1282 - Must set a content-length on PostMethod or it will try to buffer the whole thing
            final long contentLength = reqMime.getContentLength();
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Body content is too long to be processed -- maximum is " + Integer.MAX_VALUE + " bytes");

            Object connectionId = null;
            if (data.isPassthroughHttpAuthentication() && context.getRequest().isHttpRequest()){
                connectionId = context.getRequest().getHttpRequestKnob().getConnectionIdentifier();
            }

            GenericHttpClient httpClient = httpClientFactory.createHttpClient(
                                                                 getMaxConnectionsPerHost(),
                                                                 getMaxConnectionsAllHosts(),
                                                                 getConnectionTimeout(),
                                                                 getTimeout(),
                                                                 connectionId);

            if (httpClient instanceof RerunnableGenericHttpClient ||
                (!data.isPassthroughHttpAuthentication() &&
                routedRequestParams.getNtlmAuthentication() == null &&
                routedRequestParams.getPasswordAuthentication() == null)) {
                routedRequestParams.setContentLength(contentLength);
            }

            // this will forward soapaction, content-type, cookies, etc based on assertion settings
            HttpForwardingRuleEnforcer.handleRequestHeaders(routedRequestParams, context, url.getHost(),
                                                            data.getRequestHeaderRules(), auditor, vars, varNames);

            GenericHttpClient.GenericHttpMethod method = methodFromRequest(context, routedRequestParams);

            // dont add content-type for get and deletes
            if (method == GenericHttpClient.PUT || method == GenericHttpClient.POST) {
                routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, reqMime.getOuterContentType().getFullValue()));
            }
            if ( Boolean.valueOf(ServerConfig.getInstance().getPropertyCached("ioHttpUseExpectContinue")) ) {
                routedRequestParams.setUseExpectContinue(true);
            }
            if ( Boolean.valueOf(ServerConfig.getInstance().getPropertyCached("ioHttpNoKeepAlive")) ) {
                routedRequestParams.setUseKeepAlives(false); // note that server config property is for NO Keep-Alives
            }
            if ( "1.0".equals(ServerConfig.getInstance().getPropertyCached("ioHttpVersion")) ) {
                routedRequestParams.setHttpVersion(GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0);
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

            // Determines the routed response destination.
            Message routedResponseDestination = null;
            final String variableToSaveResponse = assertion.getResponseMsgDest();
            if (variableToSaveResponse == null) {
                routedResponseDestination = context.getResponse();
            } else {
                routedResponseDestination = new Message();
                routedResponseDestination.attachHttpResponseKnob(new AbstractHttpResponseKnob() {
                    public void addCookie(HttpCookie cookie) {
                        // TODO what to do with the cookie?
                    }
                });
                context.setVariable(variableToSaveResponse, routedResponseDestination);
            }

            boolean readOk = readResponse(context, routedResponse, routedResponseDestination);
            long latencyTimerEnd = System.currentTimeMillis();
            if (readOk) {
                long latency = latencyTimerEnd - latencyTimerStart;
                context.setVariable(HttpRoutingAssertion.VAR_ROUTING_LATENCY, ""+latency);
            }

            RoutingResultListener rrl = context.getRoutingResultListener();
            boolean retryRequested = allowRetry && rrl.reroute(url, status, routedResponse.getHeaders(), context); // only call listeners if retry is allowed

            if (status != HttpConstants.STATUS_OK && retryRequested) {
                // retry after if requested by a routing result listener
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS_HANDLED, url.getPath(), String.valueOf(status));
                return reallyTryUrl(context, routedRequestParams, url, false, vars);
            }

            if (status == HttpConstants.STATUS_OK)
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_OK);
            else if (data.isPassthroughHttpAuthentication() && status == HttpConstants.STATUS_UNAUTHORIZED) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_CHALLENGE);
            }

            HttpResponseKnob httpResponseKnob = (HttpResponseKnob) routedResponseDestination.getKnob(HttpResponseKnob.class);
            if (readOk && httpResponseKnob != null) {
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
                }
            }

            context.setRoutingStatus(RoutingStatus.ROUTED);

            // notify listeners
            rrl.routed(url, status, routedResponse.getHeaders(), context);

            if (!readOk) return AssertionStatus.FALSIFIED;

            return AssertionStatus.NONE;
        } catch (MalformedURLException mfe) {
            auditor.logAndAudit(AssertionMessages.GENERIC_ROUTING_PROBLEM, url.toString(), ExceptionUtils.getMessage(mfe));
            logger.log(Level.FINEST, "Problem routing: " + mfe.getMessage(), mfe);
        } catch (UnknownHostException uhe) {
            auditor.logAndAudit(AssertionMessages.HTTP_UNKNOWN_HOST, ExceptionUtils.getMessage(uhe));
            return AssertionStatus.FAILED;
        } catch (SocketException se) {
            auditor.logAndAudit(AssertionMessages.HTTP_SOCKET_EXCEPTION, ExceptionUtils.getMessage(se));
            return AssertionStatus.FAILED;
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            auditor.logAndAudit(AssertionMessages.GENERIC_ROUTING_PROBLEM, url.toString(), ExceptionUtils.getMessage(ioe));
            logger.log(Level.FINEST, "Problem routing: " + ioe.getMessage(), ioe);
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.GENERIC_ROUTING_PROBLEM, url.toString(), ExceptionUtils.getMessage(e));
            logger.log(Level.FINEST, "Problem routing: " + e.getMessage(), e);
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
     * @return true if a valid response is read
     */
    private boolean readResponse(PolicyEnforcementContext context, GenericHttpResponse routedResponse, int status) {
        boolean responseOk = true;
        try {
            InputStream responseStream = routedResponse.getInputStream();
            String ctype = routedResponse.getHeaders().getOnlyOneValue(HttpConstants.HEADER_CONTENT_TYPE);
            ContentTypeHeader outerContentType = ctype !=null ? ContentTypeHeader.parseValue(ctype) : null;
            boolean passthroughSoapFault = false;
            if (status == 500 && context.getService() != null && context.getService().isSoap() &&
                outerContentType != null && outerContentType.isXml()) {
                passthroughSoapFault = true;
            }
            // Handle missing content type error
            if (status == HttpConstants.STATUS_OK && outerContentType == null) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_NOCONTENTTYPE, Integer.toString(status));
                responseOk = false;
            } else if (data.isPassthroughHttpAuthentication() && status == HttpConstants.STATUS_UNAUTHORIZED) {
                context.getResponse().initialize(stashManagerFactory.createStashManager(), outerContentType, responseStream);
                responseOk = false;
            } else if (status >= 400 && data.isFailOnErrorStatus() && !passthroughSoapFault) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_BADSTATUS, Integer.toString(status));
                responseOk = false;
            } else if (outerContentType != null) { // response OK
                if (responseStream == null) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_CTYPEWOUTPAYLOAD, outerContentType.getFullValue());
                } else {
                    StashManager stashManager = stashManagerFactory.createStashManager();
                    context.getResponse().initialize(stashManager, outerContentType, responseStream);
                }
            }
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_ERROR_READING_RESPONSE, null, e);
        }
        return responseOk;
    }

    /**
     * Read the routing response and copy into the destination message object.
     *
     * @param context           the policy enforcement context
     * @param routedResponse    response from back end
     * @param destination       the destination message object to copy <code>routedResponse</code> into
     * @return <code>false</code> if error reading <code>routedResponse</code> or it is an error response
     */
    private boolean readResponse(final PolicyEnforcementContext context,
                                 final GenericHttpResponse routedResponse,
                                 final Message destination) {
        boolean responseOk = true;
        try {
            final int status = routedResponse.getStatus();
            final InputStream responseStream = routedResponse.getInputStream();
            final String ctype = routedResponse.getHeaders().getOnlyOneValue(HttpConstants.HEADER_CONTENT_TYPE);
            final ContentTypeHeader outerContentType = ctype != null ? ContentTypeHeader.parseValue(ctype) : null;
            boolean passthroughSoapFault = false;
            if (status == 500 && context.getService() != null && context.getService().isSoap() &&
                outerContentType != null && outerContentType.isXml()) {
                passthroughSoapFault = true;
            }

            // Handle missing content type error
            if (status == HttpConstants.STATUS_OK && outerContentType == null) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_NOCONTENTTYPE, Integer.toString(status));
                responseOk = false;
            } else if (data.isPassthroughHttpAuthentication() && status == HttpConstants.STATUS_UNAUTHORIZED) {
                destination.initialize(stashManagerFactory.createStashManager(), outerContentType, responseStream);
                responseOk = false;
            } else if (status >= 400 && data.isFailOnErrorStatus() && !passthroughSoapFault) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_BADSTATUS, Integer.toString(status));
                responseOk = false;
            } else if (outerContentType != null) { // response OK
                if (responseStream == null) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_CTYPEWOUTPAYLOAD, outerContentType.getFullValue());
                } else {
                    StashManager stashManager = stashManagerFactory.createStashManager();
                    destination.initialize(stashManager, outerContentType, responseStream);
                }
            }
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_ERROR_READING_RESPONSE, null, e);
            responseOk = false;
        }
        return responseOk;
    }

    private URL getProtectedServiceUrl(PublishedService service, PolicyEnforcementContext context) throws WSDLException, MalformedURLException {
        URL url = protectedServiceUrl; // protectedServiceUrl only set if we are no using variables and url is valid

        if (url == null) {
            String psurl;
            if (urlUsesVariables) {
               psurl = ExpandVariables.process(data.getProtectedServiceUrl(), context.getVariableMap(varNames, auditor), auditor);
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
