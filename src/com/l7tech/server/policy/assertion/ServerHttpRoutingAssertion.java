/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.http.prov.apache.IdentityBindingHttpConnectionManager;
import com.l7tech.common.io.failover.AbstractFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.service.PublishedService;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Server-side implementation of HTTP routing assertion.
 */
public class ServerHttpRoutingAssertion extends ServerRoutingAssertion {
    public static final String USER_AGENT = HttpConstants.HEADER_USER_AGENT;
    public static final String HOST = HttpConstants.HEADER_HOST;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final SignerInfo senderVouchesSignerInfo;

    private final Auditor auditor;
    private final FailoverStrategy failoverStrategy;
    private final String[] varNames;
    private final int maxFailoverAttempts;
    private final HttpRoutingAssertion httpRoutingAssertion;
    private final HttpConnectionManager connectionManager;
    private final SSLContext sslContext;
    private static final String IV_USER = "IV_USER";
    private final boolean urlUsesVariables;

    public ServerHttpRoutingAssertion(HttpRoutingAssertion assertion, ApplicationContext ctx) {
        super(assertion, ctx);
        this.httpRoutingAssertion = assertion;

        int hmax = httpRoutingAssertion.getMaxConnections();
        int tmax = hmax * 10;
        if (hmax <= 0) {
            hmax = CommonsHttpClient.getDefaultMaxConnectionsPerHost();
            tmax = CommonsHttpClient.getDefaultMaxTotalConnections();
        }

        // remember if we need to resolve the url at runtime
        String tmp = httpRoutingAssertion.getProtectedServiceUrl();
        if (tmp != null) {
            urlUsesVariables = tmp.indexOf("${") > -1;
        } else {
            logger.info("this http routing assertion has null url");
            urlUsesVariables = false;
        }

        IdentityBindingHttpConnectionManager connectionManager = new IdentityBindingHttpConnectionManager();
        connectionManager.setMaxConnectionsPerHost(hmax);
        connectionManager.setMaxTotalConnections(tmax);
        connectionManager.setPerHostStaleCleanupCount(getStaleCheckCount());
        this.connectionManager = connectionManager;

        auditor = new Auditor(this, applicationContext, logger);
        try {
            sslContext = SSLContext.getInstance("SSL");
            final SslClientTrustManager trustManager = (SslClientTrustManager)applicationContext.getBean("httpRoutingAssertionTrustManager");
            final KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore");
            sslContext.init(ku.getSSLKeyManagerFactory().getKeyManagers(), new TrustManager[]{trustManager}, null);
            final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT, HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            senderVouchesSignerInfo = ku.getSslSignerInfo();
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED, null, e);
            throw new RuntimeException(e);
        }

        final String[] addrs = httpRoutingAssertion.getCustomIpAddresses();
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

    private boolean areValidUrlHostnames(String[] addrs) {
        for (String addr : addrs) {
            try {
                new URL("http", addr, 777, "/foo/bar");
            } catch (MalformedURLException e) {
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, new String[]{addr});
                return false;
            }
        }
        return true;
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
            routedRequestParams.setSslSocketFactory(sslContext.getSocketFactory());
            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            handleProcessedSecurityHeader(context,
                                          httpRoutingAssertion.getCurrentSecurityHeaderHandling(),
                                          httpRoutingAssertion.getXmlSecurityActorToPromote());

            String userAgent = httpRoutingAssertion.getUserAgent();
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
            String soapAction = httpRequestKnob == null ? null : httpRequestKnob.getHeaderSingleValue(SoapUtil.SOAPACTION);
            if (httpRequestKnob == null || soapAction == null) {
                routedRequestParams.addExtraHeader(new GenericHttpHeader(SoapUtil.SOAPACTION, "\"\""));
            } else {
                routedRequestParams.addExtraHeader(new GenericHttpHeader(SoapUtil.SOAPACTION, soapAction));
            }

            if (httpRoutingAssertion.isTaiCredentialChaining()) {
                doTaiCredentialChaining(context, routedRequestParams, url);
            }

            String login = httpRoutingAssertion.getLogin();
            String password = httpRoutingAssertion.getPassword();
            String domain = httpRoutingAssertion.getRealm();
            String host = httpRoutingAssertion.getNtlmHost();

            if (login != null && login.length() > 0
              && password != null && password.length() > 0) {
                Map vars = context.getVariableMap(varNames, auditor);
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

            if (httpRoutingAssertion.isAttachSamlSenderVouches()) {
                doAttachSamlSenderVouches(context);
            } else if (httpRoutingAssertion.isPassthroughHttpAuthentication() && httpRequestKnob != null) {
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

            return reallyTryUrl(context, routedRequestParams, url, true);
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
                if (id == null || id.length() < 1) id = clientUser.getUniqueIdentifier();

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
                routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE, ivUserCookie.toExternalForm()));

                // there is no defined quoting or escape mechanism for HTTP cookies so we'll use URLEncoding
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADD_OUTGOING_COOKIE, new String[] {IV_USER});
            }
        }
    }

    /**
     *
     */
    private void doAttachSamlSenderVouches(PolicyEnforcementContext context) throws CertificateException, IOException, SAXException, SignatureException {
        LoginCredentials svInputCredentials = context.getCredentials();
        if (svInputCredentials == null) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SAML_SV_NOT_AUTH);
        } else {
            Document document = context.getRequest().getXmlKnob().getDocumentWritable();
            SamlAssertionGenerator ag = new SamlAssertionGenerator(senderVouchesSignerInfo);
            SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
            samlOptions.setAttestingEntity(senderVouchesSignerInfo);
            TcpKnob requestTcp = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
            if (requestTcp != null) {
                try {
                    InetAddress clientAddress = InetAddress.getByName(requestTcp.getRemoteAddress());
                    samlOptions.setClientAddress(clientAddress);
                } catch (UnknownHostException e) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_CANT_RESOLVE_IP, null, e);
                }
            }
            samlOptions.setExpiryMinutes(httpRoutingAssertion.getSamlAssertionExpiry());
            samlOptions.setUseThumbprintForSignature(httpRoutingAssertion.isUseThumbprintInSamlSignature());
            SubjectStatement statement = SubjectStatement.createAuthenticationStatement(svInputCredentials, SubjectStatement.SENDER_VOUCHES, httpRoutingAssertion.isUseThumbprintInSamlSubject());
            ag.attachStatement(document, statement, samlOptions);
        }

    }

    private AssertionStatus reallyTryUrl(PolicyEnforcementContext context,
                                         GenericHttpRequestParams routedRequestParams,
                                         URL url, boolean allowRetry)
            throws PolicyAssertionException
    {
        GenericHttpRequest routedRequest = null;
        GenericHttpResponse routedResponse = null;
        int status = -1;
        try {
            // Set the HTTP version 1.0 for not accepting the chunked Transfer Encoding
            // todo: check if we need to support HTTP 1.1.

            // Serialize the request
            final MimeKnob reqMime = context.getRequest().getMimeKnob();
            routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, reqMime.getOuterContentType().getFullValue()));

            // Fix for Bug #1282 - Must set a content-length on PostMethod or it will try to buffer the whole thing
            final long contentLength = reqMime.getContentLength();
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Body content is too long to be processed -- maximum is " + Integer.MAX_VALUE + " bytes");

            Object connectionId = null;
            if (!httpRoutingAssertion.isPassthroughHttpAuthentication() &&
                routedRequestParams.getNtlmAuthentication() == null &&
                routedRequestParams.getPasswordAuthentication() == null) {
                routedRequestParams.setContentLength(new Long(contentLength));
            }
            else if (httpRoutingAssertion.isPassthroughHttpAuthentication() && context.getRequest().isHttpRequest()){
                connectionId = context.getRequest().getHttpRequestKnob().getConnectionIdentifier();
            }

            Collection cookiesToSend = Collections.EMPTY_LIST;
            if (httpRoutingAssertion.isCopyCookies())
                cookiesToSend = copyCookiesOutbound(routedRequestParams, context, url.getHost());

            CommonsHttpClient httpClient = new CommonsHttpClient(connectionManager, getConnectionTimeout(), getTimeout(), connectionId);
            routedRequest = httpClient.createRequest(GenericHttpClient.POST, routedRequestParams);
            final InputStream bodyInputStream = reqMime.getEntireMessageBodyAsInputStream();
            routedRequest.setInputStream(bodyInputStream);

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

            if(status != HttpConstants.STATUS_OK && retryRequested) {
                // retry after if requested by a routing result listener
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS_HANDLED, new String[] {url.getPath(), String.valueOf(status)});
                return reallyTryUrl(context, routedRequestParams, url, false);
            }

            if (status == HttpConstants.STATUS_OK)
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_OK);
            else
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS, new String[] {url.getPath(), String.valueOf(status)});

            HttpResponseKnob httpResponseKnob = (HttpResponseKnob) context.getResponse().getKnob(HttpResponseKnob.class);
            if (httpResponseKnob != null)
                httpResponseKnob.setStatus(status);

            if (httpRoutingAssertion.isCopyCookies())
                copyCookiesInbound(routedRequestParams, routedResponse, context, cookiesToSend);

            if (httpRoutingAssertion.isPassthroughHttpAuthentication()) {
                boolean passed = false;
                List wwwAuthValues = routedResponse.getHeaders().getValues(HttpConstants.HEADER_WWW_AUTHENTICATE);
                if (wwwAuthValues != null) {
                    for (Iterator i = wwwAuthValues.iterator(); i.hasNext();) {
                        String value = (String)i.next();
                        httpResponseKnob.addChallenge(value);
                        passed = true;
                    }
                }
                if (passed) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_RESPONSE);
                } else {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_PASSTHROUGH_RESPONSE_NC);
                }
            }

            context.setRoutingStatus(RoutingStatus.ROUTED);

            // notify listeners
            rrl.routed(url, status, routedResponse.getHeaders(), context);

            if(!readOk) return AssertionStatus.FALSIFIED;

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
            } else if (httpRoutingAssertion.isPassthroughHttpAuthentication() &&
                       status == HttpConstants.STATUS_UNAUTHORIZED) {
                context.getResponse().initialize(StashManagerFactory.createStashManager(), outerContentType, responseStream);
                responseOk = false;
            } else if (status != HttpConstants.STATUS_OK && outerContentType!=null &&
                       (outerContentType.isText() || outerContentType.isHtml()) && !outerContentType.isXml()) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_NOXML, new String[] {Integer.toString(status)});
                responseOk = false;
            }
            // response OK
            else if(outerContentType!=null)
            {
                StashManager stashManager = StashManagerFactory.createStashManager();
                context.getResponse().initialize(stashManager, outerContentType, responseStream);
            }
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_ERROR_READING_RESPONSE, null, e);
        }
        return responseOk;
    }

    private URL getProtectedServiceUrl(PublishedService service, PolicyEnforcementContext context) throws WSDLException, MalformedURLException {
        String psurl = httpRoutingAssertion.getProtectedServiceUrl();
        if (urlUsesVariables) {
           psurl = ExpandVariables.process(psurl, context.getVariableMap(varNames, auditor));
        }
        URL url;
        if (psurl == null) {
            logger.info("assertion's url was null, falling back on service's url value");
            url = service.serviceUrl();
        } else {
            url = new URL(psurl);
        }
        return url;
    }

    /**
     * Copy cookies sent by the client to the protected service request
     *
     * @param routedRequestParams the request parameters
     * @param context the context for this request
     * @return the collection of attached Cookies
     */
    private Collection copyCookiesOutbound(GenericHttpRequestParams routedRequestParams,
                                           PolicyEnforcementContext context,
                                           String targetDomain)
    {
        List<HttpCookie> attached = new ArrayList<HttpCookie>();
        Set<HttpCookie> contextCookies = context.getCookies();

        for (HttpCookie ssgc : contextCookies) {
            if (CookieUtils.isPassThroughCookie(ssgc)) {
                if (ssgc.isNew()) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADDCOOKIE_VERSION, new String[]{ssgc.getCookieName(), String.valueOf(ssgc.getVersion())});
                } else {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_UPDATECOOKIE, new String[]{ssgc.getCookieName()});
                }
                HttpCookie newCookie = new HttpCookie(
                        ssgc.getCookieName(),
                        ssgc.getCookieValue(),
                        ssgc.getVersion(),
                        "/",
                        targetDomain,
                        ssgc.getMaxAge(),
                        ssgc.isSecure(),
                        ssgc.getComment()
                );

                // attach and record
                attached.add(newCookie);
                routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE, newCookie.getCookieValue()));
            }
        }

        return attached;
    }

    /**
     * Get new cookies from the routed response, and add them to the SSG response.
     *
     * @param routedResponse the response received from the protected service, where cookies will be copied from
     * @param context the context for the SSG request, to which the cookies should be copied
     * @param originalCookies the cookies that were already known at the time this request was sent (not newly set)
     */
    private void copyCookiesInbound(GenericHttpRequestParams routedRequestParams, GenericHttpResponse routedResponse, PolicyEnforcementContext context, Collection originalCookies) {
        List setCookieValues = routedResponse.getHeaders().getValues(HttpConstants.HEADER_SET_COOKIE);
        List<HttpCookie> newCookies = new ArrayList<HttpCookie>();
        for (Iterator i = setCookieValues.iterator(); i.hasNext();) {
            String setCookieValue = (String)i.next();
            try {
                newCookies.add(new HttpCookie(routedRequestParams.getTargetUrl(), setCookieValue));
            } catch (HttpCookie.IllegalFormatException hcife) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_INVALIDCOOKIE, new String[]{setCookieValue});
            }
        }

        newCookies.removeAll(originalCookies);

        for (HttpCookie routedCookie : newCookies) {
            HttpCookie ssgResponseCookie = new HttpCookie(routedCookie.getCookieName(), routedCookie.getCookieValue(), routedCookie.getVersion(), null, null);
            context.addCookie(ssgResponseCookie);
        }
    }


}
