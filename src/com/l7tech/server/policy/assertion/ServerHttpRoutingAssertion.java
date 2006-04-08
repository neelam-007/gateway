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
import com.l7tech.common.util.*;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.SoapFaultDetailImpl;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.service.PublishedService;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
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
    private final CommonsHttpClient httpClient;
    private final SSLContext sslContext;
    private static final String IV_USER = "IV_USER";

    public ServerHttpRoutingAssertion(HttpRoutingAssertion assertion, ApplicationContext ctx) {
        super(assertion, ctx);
        this.httpRoutingAssertion = assertion;

        int max = httpRoutingAssertion.getMaxConnections();

        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.setMaxConnectionsPerHost(max);
        connectionManager.setMaxTotalConnections(max * 10);
        //connectionManager.setConnectionStaleCheckingEnabled( false );
        httpClient = new CommonsHttpClient(connectionManager, getConnectionTimeout(), getTimeout());

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
        for (int i = 0; i < addrs.length; i++) {
            String addr = addrs[i];
            try {
                new URL("http", addr, 777, "/foo/bar");
            } catch (MalformedURLException e) {
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, new String[] { addr });
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
        try {
            final URL u;

            PublishedService service = context.getService();
            context.routingStarted();
            try {
                u = getProtectedServiceUrl(service);
            } catch (WSDLException we) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, we);
                return AssertionStatus.FAILED;
            }

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
                if (domain != null) {
                    routedRequestParams.setNtlmAuthentication(new NtlmAuthentication(login, password.toCharArray(), domain, host));
                } else {
                    routedRequestParams.setPreemptiveAuthentication(true);
                    routedRequestParams.setPasswordAuthentication(new PasswordAuthentication(login, password.toCharArray()));
                }
            }

            if (httpRoutingAssertion.isAttachSamlSenderVouches()) {
                doAttachSamlSenderVouches(context);
            } else if (httpRoutingAssertion.isPassthroughHttpAuthentication()) {
                String[] authHeaders = httpRequestKnob.getHeaderValues(HttpConstants.HEADER_AUTHORIZATION);
                boolean passed = false;
                for (int i = 0; i < authHeaders.length; i++) {
                    passed = true;
                    routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, authHeaders[i]));
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

            if (!httpRoutingAssertion.isPassthroughHttpAuthentication() &&
                routedRequestParams.getNtlmAuthentication() == null &&
                routedRequestParams.getPasswordAuthentication() == null) {
                routedRequestParams.setContentLength(new Long(contentLength));
            }

            Collection cookiesToSend = Collections.EMPTY_LIST;
            if (httpRoutingAssertion.isCopyCookies())
                cookiesToSend = copyCookiesOutbound(routedRequestParams, context, url.getHost());

            routedRequest = httpClient.createRequest(GenericHttpClient.POST, routedRequestParams);
            final InputStream bodyInputStream = reqMime.getEntireMessageBodyAsInputStream();
            routedRequest.setInputStream(bodyInputStream);

            long latencyTimerStart = System.currentTimeMillis();
            routedResponse = routedRequest.getResponse();

            int status = routedResponse.getStatus();

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
            if(status == HttpConstants.STATUS_OK && outerContentType==null) {
                logger.warning("downstream service returned status (" +
                               status + ") missing content type header.");
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_ERROR_READING_RESPONSE, null, null);

                Document faultDetails = XmlUtil.stringToDocument("<downstreamResponse><status>" +
                                        status + "</status></downstreamResponse>");

                SoapFaultDetail sfd = new SoapFaultDetailImpl(SoapFaultUtils.FC_SERVER,
                                      "downstream service response has no content type.",
                                      faultDetails.getDocumentElement());

                context.setFaultDetail(sfd);
                responseOk = false;
            } else if (httpRoutingAssertion.isPassthroughHttpAuthentication() &&
                       status == HttpConstants.STATUS_UNAUTHORIZED) {
                context.getResponse().initialize(StashManagerFactory.createStashManager(), outerContentType, responseStream);
                responseOk = false;
            } else if (status != HttpConstants.STATUS_OK &&
                       outerContentType!=null &&
                       (outerContentType.isText() || outerContentType.isHtml()) &&
                       !outerContentType.isXml())
            {
                // Special case for bugzilla #1406, we encapsulate downstream ugly html error pages in a neat soapfault
                        logger.warning("downstream service returned error (" +
                                   status + ") with non-xml payload; encapsulating error in soapfault");

                    Document faultDetails = XmlUtil.stringToDocument("<downstreamResponse><status>" + status +
                                                                     "</status></downstreamResponse>");
                    if (responseStream != null) {
                        byte[] tmp = HexUtils.slurpStream(responseStream);
                        if (tmp != null) {
                            Element plEl = faultDetails.createElement("payload");
                            Text plTx = faultDetails.createTextNode(new String(tmp, outerContentType.getEncoding()));
                            plEl.appendChild(plTx);
                            faultDetails.getDocumentElement().appendChild(plEl);
                        }
                    }

                    SoapFaultDetail sfd = new SoapFaultDetailImpl(SoapFaultUtils.FC_SERVER,
                                                                  "downstream service " +
                                                                  "returned error with non-xml response",
                                                                  faultDetails.getDocumentElement());
                    context.setFaultDetail(sfd);
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

    /**
     *
     */
    private URL getProtectedServiceUrl(PublishedService service) throws WSDLException, MalformedURLException {
        URL url;
        String psurl = httpRoutingAssertion.getProtectedServiceUrl();
        if (psurl == null) {
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
        List attached = new ArrayList();
        Set contextCookies = context.getCookies();

        for (Iterator iterator = contextCookies.iterator(); iterator.hasNext();) {
            HttpCookie ssgc = (HttpCookie) iterator.next();

            if (CookieUtils.isPassThroughCookie(ssgc)) {
                if (ssgc.isNew()) {
                   auditor.logAndAudit(AssertionMessages.HTTPROUTE_UPDATECOOKIE, new String[] {ssgc.getCookieName()});
                }

                auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADDCOOKIE_VERSION, new String[] {ssgc.getCookieName(), String.valueOf(ssgc.getVersion())});

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
                routedRequestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_COOKIE, newCookie.toExternalForm()));
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
        List newCookies = new ArrayList();
        for (Iterator i = setCookieValues.iterator(); i.hasNext();) {
            String setCookieValue = (String)i.next();
            newCookies.add(new HttpCookie(routedRequestParams.getTargetUrl(), setCookieValue));
        }

        newCookies.removeAll(originalCookies);

        for (Iterator iterator = newCookies.iterator(); iterator.hasNext();) {
            HttpCookie routedCookie = (HttpCookie) iterator.next();
            HttpCookie ssgResponseCookie = new HttpCookie(routedCookie.getCookieName(), routedCookie.getCookieValue(), routedCookie.getVersion(), null, null);
            context.addCookie(ssgResponseCookie);
        }
    }


}
