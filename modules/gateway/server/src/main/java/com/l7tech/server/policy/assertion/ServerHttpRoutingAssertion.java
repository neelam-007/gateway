/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.DefaultStashManagerFactory;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.kerberos.KerberosRoutingClient;
import com.l7tech.server.util.HttpForwardingRuleEnforcer;
import com.l7tech.server.util.IdentityBindingHttpClientFactory;
import com.l7tech.util.*;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.wsdl.WSDLException;
import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
    private final FailoverStrategy<String> failoverStrategy;
    private final String[] varNames;
    private final int maxFailoverAttempts;
    private final SSLSocketFactory socketFactory;
    private final boolean urlUsesVariables;
    private final URL protectedServiceUrl;
    private boolean customURLList;

    public ServerHttpRoutingAssertion(HttpRoutingAssertion assertion, ApplicationContext ctx) {
        super(assertion, ctx, logger);

        // remember if we need to resolve the url at runtime
        String tmp = assertion.getProtectedServiceUrl();
        if (tmp != null) {
            urlUsesVariables = tmp.indexOf("${") > -1;
        } else {
            logger.info("this http routing assertion has null url");
            urlUsesVariables = false;
        }
        if (urlUsesVariables || assertion.getProtectedServiceUrl()==null) {
            protectedServiceUrl = null;
        } else {
            URL url = null;
            try {
                url = new URL(assertion.getProtectedServiceUrl());
            } catch (MalformedURLException murle) {
                logger.log(Level.WARNING, "Invalid protected service URL.", murle);
            }
            protectedServiceUrl = url;
        }

        try {
            final SignerInfo signerInfo;
            signerInfo = ServerAssertionUtils.getSignerInfo(ctx, assertion);

            final KeyManager[] keyManagers;
            if (!assertion.isUsesDefaultKeyStore()) {
                X509Certificate[] certChain = signerInfo.getCertificateChain();
                PrivateKey privateKey = signerInfo.getPrivate();
                keyManagers = new KeyManager[] { new SingleCertX509KeyManager(certChain, privateKey) };
            } else {
                final DefaultKey ku = (DefaultKey)applicationContext.getBean("defaultKey");
                keyManagers = ku.getSslKeyManagers();
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

        final String[] addrs = assertion.getCustomIpAddresses();
        customURLList = false;
        if (addrs != null && addrs.length > 0 && areValidUrlHostnames(addrs)) {
            final String stratName = assertion.getFailoverStrategyName();
            FailoverStrategy<String> strat;
            try {
                strat = FailoverStrategyFactory.createFailoverStrategy(stratName, addrs);
            } catch (IllegalArgumentException e) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_BAD_STRATEGY_NAME, new String[] { stratName }, e);
                strat = new StickyFailoverStrategy<String>(addrs);
            }
            failoverStrategy = AbstractFailoverStrategy.makeSynchronized(strat);
            maxFailoverAttempts = addrs.length;
        } else if (assertion.getCustomURLs() != null && assertion.getCustomURLs().length > 0) {
            customURLList = true;
            final String stratName = assertion.getFailoverStrategyName();
            FailoverStrategy<String> strat;
            try {
                strat = FailoverStrategyFactory.createFailoverStrategy(stratName, assertion.getCustomURLs());
            } catch (IllegalArgumentException e) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_BAD_STRATEGY_NAME, new String[] { stratName }, e);
                strat = new StickyFailoverStrategy<String>(assertion.getCustomURLs());
            }
            failoverStrategy = AbstractFailoverStrategy.makeSynchronized(strat);
            maxFailoverAttempts = assertion.getCustomURLs().length;
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

            firePreRouting(context, u);
            if (failoverStrategy == null)
                return tryUrl(context, getRequestMessage(context), u);

            String failedService = null;
            for (int tries = 0; tries < maxFailoverAttempts; tries++) {
                String failoverService = failoverStrategy.selectService();
                if (failoverService == null) {
                    // strategy says it's time to give up
                    break;
                }
                if (failedService != null)
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_FAILOVER_FROM_TO,
                            failedService, failoverService);
                URL url;
                if (customURLList) {
                    url = new URL(failoverService.indexOf("${") > -1 ? ExpandVariables.process(failoverService, context.getVariableMap(varNames, auditor), auditor) : failoverService);
                } else {
                    url = new URL(u.getProtocol(), failoverService, u.getPort(), u.getFile());
                }
                AssertionStatus result = tryUrl(context, getRequestMessage(context), url);
                if (result == AssertionStatus.NONE) {
                    failoverStrategy.reportSuccess(failoverService);
                    return result;
                }
                failedService = failoverService;
                failoverStrategy.reportFailure(failoverService);
            }

            auditor.logAndAudit(AssertionMessages.HTTPROUTE_TOO_MANY_ATTEMPTS);
            return AssertionStatus.FAILED;
        } finally {
            context.routingFinished();
        }
    }

    private AssertionStatus tryUrl(PolicyEnforcementContext context, Message requestMessage, URL url) throws PolicyAssertionException {
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
        context.setRoutedServiceUrl(url);
        setHttpRoutingUrlContextVariables(context);

        Throwable thrown = null;
        try {
            GenericHttpRequestParams routedRequestParams = new GenericHttpRequestParams(url);
            routedRequestParams.setSslSocketFactory(socketFactory);
            routedRequestParams.setHostnameVerifier(hostnameVerifier);

            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            handleProcessedSecurityHeader(requestMessage);

            String userAgent = assertion.getUserAgent();
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

            if (assertion.isTaiCredentialChaining()) {
                doTaiCredentialChaining(context, routedRequestParams, url);
            }

            String login = assertion.getLogin();
            String password = assertion.getPassword();
            String domain = assertion.getRealm();
            String host = assertion.getNtlmHost();

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

            if (assertion.isAttachSamlSenderVouches()) {
                doAttachSamlSenderVouches(requestMessage, context.getLastCredentials(), senderVouchesSignerInfo);
            } else if (assertion.isPassthroughHttpAuthentication() && httpRequestKnob != null) {
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

            // Outbound Kerberos support (for Windows Integrated Auth only)
            if (assertion.isKrbDelegatedAuthentication()) {
                // extract creds from request & get service ticket
                addKerberosServiceTicketToRequestParam(
                        getDelegatedKerberosTicket(context, url.getHost()), routedRequestParams);

            } else if (assertion.isKrbUseGatewayKeytab()) {
                // obtain a service ticket using the gateway's keytab
                KerberosRoutingClient client = new KerberosRoutingClient();
                String svcPrincipal = KerberosClient.getServicePrincipalName(url.getProtocol(), url.getHost());
                addKerberosServiceTicketToRequestParam(
                        client.getKerberosServiceTicket(svcPrincipal, true), routedRequestParams);

            } else if (assertion.getKrbConfiguredAccount() != null) {
                // obtain a service ticket using the configured account in the assertion
                KerberosRoutingClient client = new KerberosRoutingClient();
                addKerberosServiceTicketToRequestParam(
                        client.getKerberosServiceTicket(url, assertion.getKrbConfiguredAccount(), assertion.getKrbConfiguredPassword()),
                        routedRequestParams);
            }

            return reallyTryUrl(context, requestMessage, routedRequestParams, url, true, vars);
        } catch (MalformedURLException mfe) {
            thrown = mfe;
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            thrown = ioe;
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (SAXException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (SignatureException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (CertificateException e) {
            thrown = e;
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } catch (KerberosException kex) {
            thrown = kex;
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_USING_KERBEROS_ERROR, ExceptionUtils.getMessage(thrown));
            logger.log(Level.FINEST, "Problem routing: " + thrown.getMessage(), thrown);
        } finally {
            if(context.getRoutingStatus()!=RoutingStatus.ROUTED) {
                RoutingResultListener rrl = context.getRoutingResultListener();
                rrl.failed(url, thrown, context);
            }
        }
        return AssertionStatus.FAILED;
    }

    private HttpMethod methodFromRequest(PolicyEnforcementContext context, GenericHttpRequestParams routedRequestParams) {
        if (assertion.getRequestMsgSrc() != null) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_DEFAULT_METHOD_VAR);
            return HttpMethod.POST;
        }

        if (!context.getRequest().isHttpRequest()) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_DEFAULT_METHOD_NON_HTTP);
            return HttpMethod.POST;
        }

        final HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();
        final HttpMethod requestMethod = httpRequestKnob.getMethod();
        switch (requestMethod) {
            case GET:
                routedRequestParams.setFollowRedirects(assertion.isFollowRedirects());
                return HttpMethod.GET;
            case POST:
                // redirects not supported under POST
                return HttpMethod.POST;
            case PUT:
                // redirects not supported under PUT
                return HttpMethod.PUT;
            case DELETE:
                routedRequestParams.setFollowRedirects(assertion.isFollowRedirects());
                return HttpMethod.DELETE;
            default:
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_UNEXPECTED_METHOD, requestMethod.name());
                return HttpMethod.POST;
        }
    }

    class GZipOutput {
        public InputStream zippedIS;
        public long zippedContentLength;
    }

    private GZipOutput clearToGZipInputStream(final InputStream in) {
        //logger.info("Compression #1");
        logger.fine("compressing input stream for downstream target");

        try {
            //byte[] originalBytes = HexUtils.slurpStream(in);
            //in.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzos = new GZIPOutputStream(baos);
            IOUtils.copyStream(in, gzos);
            //gzos.write(originalBytes);
            in.close();
            gzos.close();
            baos.close();
            byte[] zippedBytes = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(zippedBytes);
            logger.fine("Zipped output to " + zippedBytes.length + " bytes");
            GZipOutput output = new GZipOutput();
            output.zippedIS = bais;
            output.zippedContentLength = zippedBytes.length;
            return output;
        } catch (Exception e) {
            logger.log(Level.WARNING, "error zipping payload", e);
        }
        GZipOutput output = new GZipOutput();
        output.zippedIS = in;
        return output;
    }

    private AssertionStatus reallyTryUrl(PolicyEnforcementContext context, Message requestMessage, final GenericHttpRequestParams routedRequestParams,
                                         URL url, boolean allowRetry, Map vars) throws PolicyAssertionException {
        GenericHttpRequest routedRequest = null;
        GenericHttpResponse routedResponse = null;
        int status = -1;
        try {
            // Set the HTTP version 1.0 for not accepting the chunked Transfer Encoding
            // todo: check if we need to support HTTP 1.1.

            final MimeKnob reqMime = requestMessage.getMimeKnob();

            // Fix for Bug #1282 - Must set a content-length on PostMethod or it will try to buffer the whole thing
            final long contentLength = reqMime.getContentLength();
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Body content is too long to be processed -- maximum is " + Integer.MAX_VALUE + " bytes");

            Object connectionId = null;
            if (assertion.isPassthroughHttpAuthentication() && context.getRequest().isHttpRequest()){
                connectionId = context.getRequest().getHttpRequestKnob().getConnectionIdentifier();
            }

            GenericHttpClient httpClient = httpClientFactory.createHttpClient(
                                                                 getMaxConnectionsPerHost(),
                                                                 getMaxConnectionsAllHosts(),
                                                                 getConnectionTimeout(),
                                                                 getTimeout(),
                                                                 connectionId);

            if (httpClient instanceof RerunnableGenericHttpClient ||
                (!assertion.isPassthroughHttpAuthentication() &&
                routedRequestParams.getNtlmAuthentication() == null &&
                routedRequestParams.getPasswordAuthentication() == null)) {
                routedRequestParams.setContentLength(contentLength);
            }

            // this will forward soapaction, content-type, cookies, etc based on assertion settings
            HttpForwardingRuleEnforcer.handleRequestHeaders(routedRequestParams, context, url.getHost(),
                                                            assertion.getRequestHeaderRules(), auditor, vars, varNames);

            final HttpMethod method = methodFromRequest(context, routedRequestParams);

            // dont add content-type for get and deletes
            if (method == HttpMethod.PUT || method == HttpMethod.POST) {
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

            GZipOutput resgz = null;
            if (assertion.isGzipEncodeDownstream()) {
                InputStream out = reqMime.getEntireMessageBodyAsInputStream();
                resgz = clearToGZipInputStream(out);
                routedRequestParams.addExtraHeader(new GenericHttpHeader("content-encoding", "gzip"));
                routedRequestParams.setContentLength(resgz.zippedContentLength);
            }
            final InputStream zippedInputStream = resgz != null ? resgz.zippedIS : null;

            routedRequest = httpClient.createRequest(method, routedRequestParams);

            List<HttpForwardingRuleEnforcer.Param> paramRes = HttpForwardingRuleEnforcer.
                    handleRequestParameters(context, assertion.getRequestParamRules(), auditor, vars, varNames);


            if (paramRes != null && paramRes.size() > 0) {
                for (HttpForwardingRuleEnforcer.Param p : paramRes) {
                    routedRequest.addParameter(p.name, p.value);
                }
            } else {
                // only include payload if the method is POST or PUT
                if (method == HttpMethod.POST || method == HttpMethod.PUT) {
                    if (routedRequest instanceof RerunnableHttpRequest) {
                        RerunnableHttpRequest rerunnableHttpRequest = (RerunnableHttpRequest) routedRequest;
                        rerunnableHttpRequest.setInputStreamFactory(new RerunnableHttpRequest.InputStreamFactory() {
                            public InputStream getInputStream() {
                                try {
                                    InputStream out = reqMime.getEntireMessageBodyAsInputStream();
                                    if (assertion.isGzipEncodeDownstream()) {
                                        out = zippedInputStream;
                                    }
                                    return out;
                                } catch (NoSuchPartException nspe) {
                                    return new IOExceptionThrowingInputStream(new CausedIOException("Cannot access mime part.", nspe));
                                } catch (IOException ioe) {
                                    return new IOExceptionThrowingInputStream(ioe);
                                }
                            }
                        });
                    } else {
                        if (assertion.isGzipEncodeDownstream()) {
                            routedRequest.setInputStream(zippedInputStream);
                        } else {
                            InputStream bodyInputStream = reqMime.getEntireMessageBodyAsInputStream();
                            routedRequest.setInputStream(bodyInputStream);
                        }
                    }
                }
            }
            long latencyTimerStart = System.currentTimeMillis();
            routedResponse = routedRequest.getResponse();

            status = routedResponse.getStatus();

            // Determines the routed response destination.
            Message routedResponseDestination = context.getResponse();
            boolean routedResponseDestinationIsContextVariable = false;
            if (assertion.getResponseMsgDest() != null) {
                routedResponseDestinationIsContextVariable = true;
                routedResponseDestination = new Message();
                routedResponseDestination.attachHttpResponseKnob(new AbstractHttpResponseKnob() {
                    public void addCookie(HttpCookie cookie) {
                        // TODO what to do with the cookie?
                    }
                });
                context.setVariable(assertion.getResponseMsgDest(), routedResponseDestination);
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
                return reallyTryUrl(context, requestMessage, routedRequestParams, url, false, vars);
            }

            if (status == HttpConstants.STATUS_OK)
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_OK);
            else if (assertion.isPassthroughHttpAuthentication() && status == HttpConstants.STATUS_UNAUTHORIZED) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_CHALLENGE);
            }

            HttpResponseKnob httpResponseKnob = (HttpResponseKnob) routedResponseDestination.getKnob(HttpResponseKnob.class);
            if (readOk && httpResponseKnob != null) {
                httpResponseKnob.setStatus(status);

                HttpForwardingRuleEnforcer.handleResponseHeaders(routedResponse,
                                                                 httpResponseKnob,
                                                                 auditor,
                                                                 assertion.getResponseHeaderRules(),
                                                                 routedResponseDestinationIsContextVariable,
                                                                 context,
                                                                 routedRequestParams,
                                                                 vars,
                                                                 varNames);
            }
            if (assertion.isPassthroughHttpAuthentication()) {
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
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(mfe));
            logger.log(Level.FINEST, "Problem routing: " + mfe.getMessage(), mfe);
        } catch (UnknownHostException uhe) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_UNKNOWN_HOST, ExceptionUtils.getMessage(uhe));
            return AssertionStatus.FAILED;
        } catch (SocketException se) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SOCKET_EXCEPTION, ExceptionUtils.getMessage(se));
            return AssertionStatus.FAILED;
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(ioe));
            logger.log(Level.FINEST, "Problem routing: " + ioe.getMessage(), ioe);
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_GENERIC_PROBLEM, url.toString(), ExceptionUtils.getMessage(e));
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
            firePostRouting(context, url, status);
        }

        return AssertionStatus.FAILED;
    }

    /**
     * @param context   the PEC
     * @return a request message object as configured by this assertion
     */
    protected Message getRequestMessage(final PolicyEnforcementContext context) {
        Message msg;
        if (assertion.getRequestMsgSrc() == null)
            return context.getRequest();

        final String variableName = assertion.getRequestMsgSrc();
        try {
            final Object requestSrc = context.getVariable(variableName);
            if (!(requestSrc instanceof Message)) {
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Request message source (\"" + variableName +
                        "\") is a context variable of the wrong type (expected=" + Message.class + ", actual=" + requestSrc.getClass() + ").");
            }
            return (Message)requestSrc;
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Request message source is a non-existent context variable (\"" + variableName + "\").");
        }
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
            InputStream responseStream = routedResponse.getInputStream();
            // compression addition
            final String maybegzipencoding = routedResponse.getHeaders().getOnlyOneValue("content-encoding");
            if (maybegzipencoding != null && maybegzipencoding.contains("gzip")) { // case of value ?
                if (responseStream != null ){
                    // logger.info("Compression #4");
                    logger.fine("detected compression on incoming response");
                    responseStream = new GZIPInputStream(responseStream);
                }
            }
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
            } else if (assertion.isPassthroughHttpAuthentication() && status == HttpConstants.STATUS_UNAUTHORIZED) {
                destination.initialize(stashManagerFactory.createStashManager(), outerContentType, responseStream);
                responseOk = false;
            } else if (status >= 400 && assertion.isFailOnErrorStatus() && !passthroughSoapFault) {
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
        } catch(EOFException eofe){
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_BAD_GZIP_STREAM);
            responseOk = false;
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
               psurl = ExpandVariables.process(assertion.getProtectedServiceUrl(), context.getVariableMap(varNames, auditor), auditor);
            } else {
               psurl = assertion.getProtectedServiceUrl();
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

    /**
     * Adds the kerberos service ticket (if not null) into the HTTP request parameters for
     * outbound kerberos support.
     *
     * @param serviceTicket the service ticket to add
     * @param routedRequestParams the pending HTTP request parameters
     * @throws KerberosException if either the serviceTicke or the routed request parameters
     */
    private void addKerberosServiceTicketToRequestParam(KerberosServiceTicket serviceTicket, GenericHttpRequestParams routedRequestParams)
        throws KerberosException
    {
        if (serviceTicket == null)
            throw new KerberosException("KerberosServiceTicket is null and cannot be added to the request for routing");
        if (routedRequestParams == null)
            throw new KerberosException("Missing Http routing parameters");

        routedRequestParams.addExtraHeader(new GenericHttpHeader(
                HttpConstants.HEADER_AUTHORIZATION,
                "Negotiate " + HexUtils.encodeBase64(serviceTicket.getGSSAPReqTicket().getSPNEGO(), true)));

        logger.log(Level.FINE, "Kerberos ticket added to Http request parameters ({0}|{1})",
                new String[] { serviceTicket.getServicePrincipalName(), serviceTicket.getClientPrincipalName() });
    }

    private void setHttpRoutingUrlContextVariables(PolicyEnforcementContext context) {
        URL url = context.getRoutedServiceUrl();
        if (url == null) return;
        try {
            url = new URL(url.toString());
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "URL cannot be parsed: {0}", new String[] {url.toString()});
            return;
        }
        context.setVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_URL, url.toString());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlHost(), url.getHost());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlProtocol(), url.getProtocol());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlPort(), getHttpRoutingUrlPort(url));
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlFile(), url.getFile());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlPath(), url.getPath());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlQuery(), url.getQuery() == null ? null : "?" + url.getQuery());
        context.setVariable(HttpRoutingAssertion.getVarHttpRoutingUrlFragment(), url.getRef());
    }

    private Integer getHttpRoutingUrlPort(URL url) {
        if (url == null) return null;
        try {
            url = new URL(url.toString());
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "URL cannot be parsed: {0}", new String[] {url.toString()});
            return null;
        }

        String protocol = url.getProtocol();
        int port = url.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(protocol)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(protocol)) {
                port = 443;
            } else if ("ftp".equalsIgnoreCase(protocol)) {
                port = 21;
            } else if ("smtp".equalsIgnoreCase(protocol)) {
                port = 25;
            } else if ("pop3".equalsIgnoreCase(protocol)) {
                port = 110;
            } else if ("imap".equalsIgnoreCase(protocol)) {
                port = 143;
            }
        }
        return port;
    }
}
