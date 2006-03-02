/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.io.failover.AbstractFailoverStrategy;
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
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
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
    public static final String PROP_SSL_SESSION_TIMEOUT =
            HttpRoutingAssertion.class.getName() + ".sslSessionTimeoutSeconds";
    public static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;
    private SignerInfo senderVouchesSignerInfo;
    private final Auditor auditor;
    private final FailoverStrategy failoverStrategy;
    private final String[] varNames;
    private final int maxFailoverAttempts;

    public ServerHttpRoutingAssertion(HttpRoutingAssertion assertion, ApplicationContext ctx) {
        super(ctx);
        this.httpRoutingAssertion = assertion;

        int max = httpRoutingAssertion.getMaxConnections();

        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.setMaxConnectionsPerHost(max);
        connectionManager.setMaxTotalConnections(max * 10);
        //connectionManager.setConnectionStaleCheckingEnabled( false );

        auditor = new Auditor(this, applicationContext, logger);
        try {
            sslContext = SSLContext.getInstance("SSL");
            final SslClientTrustManager trustManager = (SslClientTrustManager)applicationContext.getBean("httpRoutingAssertionTrustManager");
            final KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore");
            sslContext.init(ku.getSSLKeyManagerFactory().getKeyManagers(), new TrustManager[]{trustManager}, null);
            final int timeout = Integer.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            senderVouchesSignerInfo = ku.getSslSignerInfo();
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SSL_INIT_FAILED, null, e);
            throw new RuntimeException(e);
        }

        final String[] addrs = httpRoutingAssertion.getCustomIpAddresses();
        if (addrs != null && addrs.length > 0 && areValidUrlHostnames(addrs, auditor)) {
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

    private boolean areValidUrlHostnames(String[] addrs, Auditor auditor) {
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

    /**
     * Lazy protocol init
     */
    private void initProtocol(URL url, int port) {
        if(protocol == null) {
            synchronized (this) {
                if (protocol == null) {
                    protocol = new Protocol(url.getProtocol(), new SecureProtocolSocketFactory() {
                        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
                            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
                        }

                        public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException, UnknownHostException {
                            return sslContext.getSocketFactory().createSocket(host, port, clientAddress, clientPort);
                        }

                        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                            return sslContext.getSocketFactory().createSocket(host, port);
                        }
                    }, port);
                }
            }
        }
    }

    /**
     *
     */
    private AssertionStatus tryUrl(PolicyEnforcementContext context, URL url) throws PolicyAssertionException
    {
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);

        Throwable thrown = null;
        try {
            HttpClient client = new HttpClient(connectionManager);
            client.setConnectionTimeout(getConnectionTimeout());
            client.setTimeout(getTimeout());
            HostConfiguration hconf = null;

            if ("https".equals(url.getProtocol())) {
                final int port = url.getPort() == -1 ? 443 : url.getPort();
                hconf = new HostConfiguration();
                initProtocol(url, port);
                hconf.setHost(url.getHost(), port, protocol);
            }

            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            handleProcessedSecurityHeader(context,
                                          httpRoutingAssertion.getCurrentSecurityHeaderHandling(),
                                          httpRoutingAssertion.getXmlSecurityActorToPromote());

            List httpHeaders = new ArrayList();

            String userAgent = httpRoutingAssertion.getUserAgent();
            if (userAgent == null || userAgent.length() == 0) userAgent = DEFAULT_USER_AGENT;
            httpHeaders.add(new GenericHttpHeader(USER_AGENT, userAgent));

            StringBuffer hostValue = new StringBuffer(url.getHost());
            int port = url.getPort();
            if (port != -1) {
                hostValue.append(":");
                hostValue.append(port);
            }
            httpHeaders.add(new GenericHttpHeader(HOST, hostValue.toString()));

            HttpRequestKnob httpRequestKnob = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
            String soapAction = httpRequestKnob == null ? null : httpRequestKnob.getHeaderSingleValue(SoapUtil.SOAPACTION);
            if (httpRequestKnob == null || soapAction == null) {
                httpHeaders.add(new GenericHttpHeader(SoapUtil.SOAPACTION, "\"\""));
            } else {
                httpHeaders.add(new GenericHttpHeader(SoapUtil.SOAPACTION, soapAction));
            }

            if (httpRoutingAssertion.isTaiCredentialChaining()) {
                doTaiCredentialChaining(context, client, httpHeaders, url);
            }

            String login = httpRoutingAssertion.getLogin();
            String password = httpRoutingAssertion.getPassword();

            boolean doAuth = false;
            if (login != null && login.length() > 0
              && password != null && password.length() > 0) {
                Map vars = context.getVariableMap(varNames, auditor);
                login = ExpandVariables.process(login, vars);
                password = ExpandVariables.process(password, vars);

                auditor.logAndAudit(AssertionMessages.HTTPROUTE_LOGIN_INFO, new String[] {login});
                HttpState state = client.getState();
                doAuth = true;
                state.setAuthenticationPreemptive(true);
                state.setCredentials(null, null, new UsernamePasswordCredentials(login, password));
            }

            if (httpRoutingAssertion.isAttachSamlSenderVouches()) {
                doAttachSamlSenderVouches(context);
            }

            return reallyTryUrl(context, client, hconf, Collections.unmodifiableList(httpHeaders), doAuth, url, true);
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
    private void doTaiCredentialChaining(PolicyEnforcementContext context, HttpClient client, List headers, URL url) {
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
                headers.add(new GenericHttpHeader("IV_USER", chainId));

                // there is no defined quoting or escape mechanism for HTTP cookies so we'll use URLEncoding
                Cookie cookieOut = new Cookie();
                cookieOut.setName("IV_USER");
                cookieOut.setValue(chainId);
                cookieOut.setDomain(url.getHost());
                cookieOut.setPath(url.getPath());
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADD_OUTGOING_COOKIE, new String[] {cookieOut.getName()});
                client.getState().addCookie(cookieOut);
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

    /**
     *
     */
    private AssertionStatus reallyTryUrl(PolicyEnforcementContext context, HttpClient client, HostConfiguration hconf, List headers, boolean doAuth, URL url, boolean allowRetry) throws PolicyAssertionException
    {
        PostMethod postMethod = null;
        try {
            postMethod = new PostMethod(url.toString());

            // Set the HTTP version 1.0 for not accepting the chunked Transfer Encoding
            // todo: check if we need to support HTTP 1.1.
            postMethod.setHttp11(false);

            for (Iterator iterator = headers.iterator(); iterator.hasNext();) {
                HttpHeader httpHeader = (HttpHeader) iterator.next();
                postMethod.setRequestHeader(httpHeader.getName(), httpHeader.getFullValue());
            }

            if(doAuth) postMethod.setDoAuthentication(true);

            // Serialize the request
            final MimeKnob reqMime = context.getRequest().getMimeKnob();
            postMethod.setRequestHeader(HttpConstants.HEADER_CONTENT_TYPE, reqMime.getOuterContentType().getFullValue());

            // Fix for Bug #1282 - Must set a content-length on PostMethod or it will try to buffer the whole thing
            final long contentLength = reqMime.getContentLength();
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Body content is too long to be processed -- maximum is " + Integer.MAX_VALUE + " bytes");
            postMethod.setRequestContentLength((int)contentLength);

            Collection sentCookies = Collections.EMPTY_LIST;
            if(httpRoutingAssertion.isCopyCookies())
                sentCookies = attachCookies(client, context, auditor, url.getHost());

            final InputStream bodyInputStream = reqMime.getEntireMessageBodyAsInputStream();
            postMethod.setRequestBody(bodyInputStream);

            long latencyTimerStart = System.currentTimeMillis();
            if (hconf == null) {
                client.executeMethod(postMethod);
            } else {
                client.executeMethod(hconf, postMethod);
            }

            int status = postMethod.getStatusCode();

            boolean readOk = readResponse(context, postMethod, status);
            long latencyTimerEnd = System.currentTimeMillis();
            if (readOk) {
                long latency = latencyTimerEnd - latencyTimerStart;
                context.setVariable(HttpRoutingAssertion.ROUTING_LATENCY, ""+latency);
            }

            RoutingResultListener rrl = context.getRoutingResultListener();
            boolean retryRequested = allowRetry && rrl.reroute(url, status, toHeaders(postMethod.getResponseHeaders()), context); // only call listeners if retry is allowed

            if(status != HttpConstants.STATUS_OK && retryRequested) {
                // retry after if requested by a routing result listener
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS_HANDLED, new String[] {url.getPath(), String.valueOf(status)});
                return reallyTryUrl(context, client, hconf, headers, doAuth, url, false);
            }

            if (status == HttpConstants.STATUS_OK)
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_OK);
            else
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_RESPONSE_STATUS, new String[] {url.getPath(), String.valueOf(status)});

            HttpResponseKnob httpResponseKnob = (HttpResponseKnob) context.getResponse().getKnob(HttpResponseKnob.class);
            if (httpResponseKnob != null)
                httpResponseKnob.setStatus(status);

            if(httpRoutingAssertion.isCopyCookies())
                returnCookies(client, context, sentCookies);

            context.setRoutingStatus(RoutingStatus.ROUTED);

            // notify listeners
            rrl.routed(url, status, toHeaders(postMethod.getResponseHeaders()), context);

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
            if (postMethod != null) {
                MethodCloser mc = new MethodCloser(postMethod);
                context.runOnClose(mc);
            }
        }

        return AssertionStatus.FAILED;
    }

    private HttpHeaders toHeaders(final Header[] headers) {
        HttpHeader[] sevenHeads;
        if(headers==null) {
            sevenHeads = new HttpHeader[0];
        }
        else {
            sevenHeads = new HttpHeader[headers.length];
            for (int i = 0; i < sevenHeads.length; i++) {
                sevenHeads[i] = new GenericHttpHeader(headers[i].getName(), headers[i].getValue());
            }
        }
        return new GenericHttpHeaders(sevenHeads);
    }

    /**
     * Read the routing response and copy into the SSG response.
     *
     * @return false if falsified (if a non-xml response was wrapped)
     */
    private boolean readResponse(PolicyEnforcementContext context, PostMethod postMethod, int status) {
        boolean responseOk = true;
        try {
            InputStream responseStream = postMethod.getResponseBodyAsStream();
            Header ctheader = postMethod.getResponseHeader(HttpConstants.HEADER_CONTENT_TYPE);
            String ctype = ctheader!=null ? ctheader.getValue() : null;
            ContentTypeHeader outerContentType = ctype!=null ? ContentTypeHeader.parseValue(ctype) : null;

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
            }
            // Special case for bugzilla #1406, we encapsulate downstream ugly html error pages in a neat soapfault
            else if (status != HttpConstants.STATUS_OK
             && outerContentType!=null
             && (outerContentType.isText() || outerContentType.isHtml())
             && !outerContentType.isXml()) {
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
     * Attach cookies received by the client to the protected service
     *
     * @param client  the http client sender
     * @param context the context for this request
     * @param auditor used to record cookie actions
     * @return the collection of attached Cookies
     */
    private Collection attachCookies(HttpClient client, PolicyEnforcementContext context, Auditor auditor, String targetDomain) {
        List attached = new ArrayList();
        HttpState state = client.getState();
        Set contextCookies = context.getCookies();

        for (Iterator iterator = contextCookies.iterator(); iterator.hasNext();) {
            HttpCookie cookie = (HttpCookie) iterator.next();

            if(CookieUtils.isPassThroughCookie(cookie)) {
                if(cookie.isNew()) {
                   auditor.logAndAudit(AssertionMessages.HTTPROUTE_UPDATECOOKIE, new String[] {cookie.getCookieName()});
                }

                auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADDCOOKIE_VERSION, new String[] {cookie.getCookieName(), String.valueOf(cookie.getVersion())});

                // create HTTP Client version of cookie
                Cookie httpClientCookie = CookieUtils.toHttpClientCookie(cookie);

                // modify for target
                httpClientCookie.setPathAttributeSpecified(true);
                httpClientCookie.setPath("/");
                httpClientCookie.setDomainAttributeSpecified(true);
                httpClientCookie.setDomain(targetDomain);

                // attach and record
                attached.add(httpClientCookie);
                state.addCookie(httpClientCookie);
            }
        }

        return attached;
    }

    /**
     * Get new cookies from the http state, and add them to the response.
     *
     * @param client the client whose cookies are to be returned
     * @param context the context to which the cookies should be added
     * @param originalCookies the cookies that are known (not newly set)
     */
    private void returnCookies(HttpClient client, PolicyEnforcementContext context, Collection originalCookies) {
        HttpState state = client.getState();
        Cookie[] cookies = state.getCookies();

        Set newCookies = new LinkedHashSet(Arrays.asList(cookies));
        newCookies.removeAll(originalCookies);

        for (Iterator iterator = newCookies.iterator(); iterator.hasNext();) {
            Cookie cookie = (Cookie) iterator.next();

            // modify for client
            cookie.setDomain(null);
            cookie.setDomainAttributeSpecified(false);
            cookie.setPath(null);
            cookie.setPathAttributeSpecified(false);

            context.addCookie(CookieUtils.fromHttpClientCookie(cookie, true));
        }
    }

    private static class MethodCloser implements Runnable {
        MethodCloser(HttpMethod method) {
            this.method = method;
        }

        public void run() {
            method.releaseConnection();
        }

        private HttpMethod method;
    }


    private final HttpRoutingAssertion httpRoutingAssertion;
    private final MultiThreadedHttpConnectionManager connectionManager;
    private final SSLContext sslContext;

    final Logger logger = Logger.getLogger(getClass().getName());
    private Protocol protocol;
}
