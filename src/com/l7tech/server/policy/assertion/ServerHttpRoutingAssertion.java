/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.service.PublishedService;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Server-side implementation of HTTP routing assertion.
 */
public class ServerHttpRoutingAssertion extends ServerRoutingAssertion {
    public static final String USER_AGENT = "User-Agent";
    public static final String HOST = "Host";
    public static final String PROP_SSL_SESSION_TIMEOUT = HttpRoutingAssertion.class.getName() +
      ".sslSessionTimeoutSeconds";
    public static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;
    private SignerInfo senderVouchesSignerInfo;

    public ServerHttpRoutingAssertion(HttpRoutingAssertion assertion, ApplicationContext ctx) {
        super(ctx);
        this.httpRoutingAssertion = assertion;

        int max = httpRoutingAssertion.getMaxConnections();

        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.setMaxConnectionsPerHost(max);
        connectionManager.setMaxTotalConnections(max * 10);
        //connectionManager.setConnectionStaleCheckingEnabled( false );

        Auditor auditor = new Auditor((AuditContext)applicationContext.getBean("auditContext"), logger);
        try {
            sslContext = SSLContext.getInstance("SSL");
            final SslClientTrustManager trustManager = (SslClientTrustManager)applicationContext.getBean("httpRoutingAssertionTrustManager");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            final int timeout = Integer.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
            final KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore");
            senderVouchesSignerInfo = ku.getSslSignerInfo();
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.SSL_CONTEXT_INIT_FAILED, null, e);
            throw new RuntimeException(e);
        }
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
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);

        PostMethod postMethod = null;
        InputStream inputStream = null;
        Auditor auditor = new Auditor(context.getAuditContext(), logger);

        try {
            try {
                PublishedService service = context.getService();
                URL url = getProtectedServiceUrl(service);

                HttpClient client = new HttpClient(connectionManager);
                HostConfiguration hconf = null;

                if ("https".equals(url.getProtocol())) {
                    final int port = url.getPort() == -1 ? 443 : url.getPort();
                    hconf = new HostConfiguration();
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

                    hconf.setHost(url.getHost(), port, protocol);
                }

                postMethod = new PostMethod(url.toString());

                // Set the HTTP version 1.0 for not accepting the chunked Transfer Encoding
                // todo: check if we need to support HTTP 1.1.
                postMethod.setHttp11(false);

                // DELETE CURRENT SECURITY HEADER IF NECESSARY
                handleProcessedSecurityHeader(context,
                                              httpRoutingAssertion.getCurrentSecurityHeaderHandling(),
                                              httpRoutingAssertion.getXmlSecurityActorToPromote(),
                                              auditor);

                final MimeKnob reqMime = context.getRequest().getMimeKnob();
                postMethod.addRequestHeader(MimeUtil.CONTENT_TYPE, reqMime.getOuterContentType().getFullValue());

                // Fix for Bug #1282 - Must set a content-length on PostMethod or it will try to buffer the whole thing
                final long contentLength = reqMime.getContentLength();
                if (contentLength > Integer.MAX_VALUE)
                    throw new IOException("Body content is too long to be processed -- maximum is " + Integer.MAX_VALUE + " bytes");
                postMethod.setRequestContentLength((int)contentLength);

                String userAgent = httpRoutingAssertion.getUserAgent();
                if (userAgent == null || userAgent.length() == 0) userAgent = DEFAULT_USER_AGENT;
                postMethod.setRequestHeader(USER_AGENT, userAgent);

                StringBuffer hostValue = new StringBuffer(url.getHost());
                int port = url.getPort();
                if (port != -1) {
                    hostValue.append(":");
                    hostValue.append(port);
                }
                postMethod.setRequestHeader(HOST, hostValue.toString());
                postMethod.setRequestHeader(SoapUtil.SOAPACTION, context.getRequest().getHttpRequestKnob().getHeaderSingleValue(SoapUtil.SOAPACTION));

                if (httpRoutingAssertion.isTaiCredentialChaining()) {
                    String chainId = null;
                    if (!context.isAuthenticated()) {
                        auditor.logAndAudit(AssertionMessages.TAI_REQUEST_NOT_AUTHENTICATED);
                    } else {
                        User clientUser = context.getAuthenticatedUser();
                        if (clientUser != null) {
                            String id = clientUser.getLogin();
                            if (id == null || id.length() < 1) id = clientUser.getName();
                            if (id == null || id.length() < 1) id = clientUser.getUniqueIdentifier();

                            if (id != null && id.length() > 0) {
                                auditor.logAndAudit(AssertionMessages.TAI_REQUEST_CHAIN_USERNAME, new String[] {id});
                                chainId = id;
                            } else
                                auditor.logAndAudit(AssertionMessages.TAI_REQUEST_USER_ID_NOT_UNIQUE, new String[] {id});
                        } else {
                            final String login = context.getCredentials().getLogin();
                            if (login != null && login.length() > 0) {
                                auditor.logAndAudit(AssertionMessages.TAI_REQUEST_CHAIN_LOGIN, new String[] {login});
                                chainId = login;
                            } else
                                auditor.logAndAudit(AssertionMessages.TAI_REQUEST_NO_USER_OR_LOGIN);
                        }

                        if (chainId != null && chainId.length() > 0) {
                            postMethod.setRequestHeader("IV_USER", chainId);

                            // there is no defined quoting or escape mechanism for HTTP cookies so we'll use URLEncoding
                            org.apache.commons.httpclient.Cookie cookieOut = new org.apache.commons.httpclient.Cookie();
                            cookieOut.setName("IV_USER");
                            cookieOut.setValue(chainId);
                            cookieOut.setDomain(url.getHost());
                            cookieOut.setPath(url.getPath());
                            auditor.logAndAudit(AssertionMessages.ADD_OUTGOING_COOKIE, new String[] {cookieOut.getName()});
                            client.getState().addCookie(cookieOut);
                        }
                    }
                }

                String login = httpRoutingAssertion.getLogin();
                String password = httpRoutingAssertion.getPassword();

                if (login != null && login.length() > 0
                  && password != null && password.length() > 0) {
                    auditor.logAndAudit(AssertionMessages.LOGIN_INFO, new String[] {login});
                    HttpState state = client.getState();
                    postMethod.setDoAuthentication(true);
                    state.setAuthenticationPreemptive(true);
                    state.setCredentials(null, null, new UsernamePasswordCredentials(login, new String(password)));
                }

                if (httpRoutingAssertion.isAttachSamlSenderVouches()) {
                    Document document = context.getRequest().getXmlKnob().getDocumentWritable();
                    SamlAssertionGenerator ag = new SamlAssertionGenerator(senderVouchesSignerInfo);
                    SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
                    TcpKnob requestTcp = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
                    if (requestTcp != null) {
                        try {
                            InetAddress clientAddress = InetAddress.getByName(requestTcp.getRemoteAddress());
                            samlOptions.setClientAddress(clientAddress);
                        } catch (UnknownHostException e) {
                            auditor.logAndAudit(AssertionMessages.CANNOT_RESOLVE_IP_ADDRESS, null, e);
                        }
                    }
                    samlOptions.setExpiryMinutes(httpRoutingAssertion.getSamlAssertionExpiry());
                    SubjectStatement statement = SubjectStatement.createAuthenticationStatement(context.getCredentials(), SubjectStatement.SENDER_VOUCHES);
                    ag.attachStatement(document, statement, samlOptions);
                }
                attachCookies(client, context, url, auditor);

                // Serialize the request
                final InputStream bodyInputStream = reqMime.getEntireMessageBodyAsInputStream();
                postMethod.setRequestBody(bodyInputStream);

                if (hconf == null) {
                    client.executeMethod(postMethod);
                } else {
                    client.executeMethod(hconf, postMethod);
                }

                int status = postMethod.getStatusCode();
                if (status == 200)
                    auditor.logAndAudit(AssertionMessages.ROUTED_OK);
                else
                    auditor.logAndAudit(AssertionMessages.RESPONSE_STATUS, new String[] {url.getPath(), String.valueOf(status)});

                context.getResponse().getHttpResponseKnob().setStatus(status);

                context.setRoutingStatus(RoutingStatus.ROUTED);

            } catch (WSDLException we) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, we);
                return AssertionStatus.FAILED;
            } catch (MalformedURLException mfe) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, mfe);
                return AssertionStatus.FAILED;
            } catch (IOException ioe) {
                // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, ioe);
                return AssertionStatus.FAILED;
            } catch (SAXException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                return AssertionStatus.FAILED;
            } catch (SignatureException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                return AssertionStatus.FAILED;
            } catch (CertificateException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                return AssertionStatus.FAILED;
            } catch (NoSuchPartException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
                return AssertionStatus.FAILED;
            }
            // BEYOND THIS POINT, WE DONT RETURN FAILURE
            try {
                InputStream responseStream = postMethod.getResponseBodyAsStream();
                String ctype = postMethod.getResponseHeader(MimeUtil.CONTENT_TYPE).getValue();
                ContentTypeHeader outerContentType = ContentTypeHeader.parseValue(ctype);
                final StashManager stashManager = StashManagerFactory.createStashManager();
                context.getResponse().initialize(stashManager, outerContentType, responseStream);
            } catch (Exception e) {
                auditor.logAndAudit(AssertionMessages.ERROR_READING_RESPONSE, null, e);
                // here we dont return error because we already routed
            }
        } finally {
            if (postMethod != null) {
                MethodCloser mc = new MethodCloser(postMethod);
                context.runOnClose(mc);
            }

            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        }

        return AssertionStatus.NONE;
    }

    private URL getProtectedServiceUrl(PublishedService service) throws WSDLException, MalformedURLException {
        URL url;
        String psurl = httpRoutingAssertion.getProtectedServiceUrl();
        if (psurl == null) {
            URL wsdlUrl = service.serviceUrl();
            url = wsdlUrl;
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
     * @param url
     * @param auditor
     */
    private void attachCookies(HttpClient client, PolicyEnforcementContext context, URL url, final Auditor auditor)
    {
        HttpServletRequestKnob hsRequestKnob = (HttpServletRequestKnob)context.getRequest().getKnob(HttpServletRequestKnob.class);
        HttpServletRequest req = hsRequestKnob == null ? null : hsRequestKnob.getHttpServletRequest();
        if (req == null)
            return;
        Vector updatedCookies = context.getUpdatedCookies();
        HttpState state = client.getState();
        Cookie updatedCookie = null;

        javax.servlet.http.Cookie[] cookies = req.getCookies();
        org.apache.commons.httpclient.Cookie cookieOut = null;

        // if no cookies found in the request but there is cookies in the udpatedCookies list (i.e. new cookies)
        if ((cookies == null || cookies.length == 0)) {
            if (updatedCookies.size() > 0) {
                for (int i = 0; i < updatedCookies.size(); i++) {
                    Object o = (Object)updatedCookies.elementAt(i);
                    if (o instanceof Cookie) {
                        Cookie newCookie = (Cookie)o;
                        cookieOut = new org.apache.commons.httpclient.Cookie();
                        cookieOut.setDomain(url.getHost());
                        cookieOut.setPath(url.getPath());
                        cookieOut.setName(newCookie.getName());
                        cookieOut.setSecure(newCookie.getSecure());
                        cookieOut.setVersion(newCookie.getVersion());
                        cookieOut.setComment(newCookie.getComment());
                        // cookieOut.setExpiryDate(??); // how to translate the getMaxAge() to the date? em
                        auditor.logAndAudit(AssertionMessages.ADD_OUTGOING_COOKIE_WITH_VERSION, new String[] {cookieOut.getName(), String.valueOf(cookieOut.getVersion())});
                        state.addCookie(cookieOut);
                    }
                }
            }
        } else {
            for (int i = 0; cookies != null && i < cookies.length; i++) {
                javax.servlet.http.Cookie incomingCookie = cookies[i];
                cookieOut = new org.apache.commons.httpclient.Cookie(url.getHost(), incomingCookie.getName(), incomingCookie.getValue());
                cookieOut.setPath(url.getPath());

                // override the old cookie if the new one is found
                updatedCookie = findCookieByName(updatedCookies, incomingCookie.getName());
                if (updatedCookie != null) {
                    cookieOut.setValue(updatedCookie.getValue());
                    auditor.logAndAudit(AssertionMessages.UPDATE_COOKIE, new String[] {updatedCookie.getName()});
                } else {
                    cookieOut.setValue(incomingCookie.getValue());
                }
                cookieOut.setSecure(incomingCookie.getSecure());
                cookieOut.setVersion(incomingCookie.getVersion());
                cookieOut.setComment(incomingCookie.getComment());
                // cookieOut.setExpiryDate(??); // how to translate the getMaxAge() to the date? em
                auditor.logAndAudit(AssertionMessages.ADD_OUTGOING_COOKIE_WITH_VERSION, new String[] {cookieOut.getName(), String.valueOf(cookieOut.getVersion())});
                state.addCookie(cookieOut);
            }
        }

    }

    /**
     * Find the cookie given the name.
     *
     * @param updatedCookies the list of cookies
     * @param cookieName     the given cookie name
     * @return Cookie  the cookie object that matches the given name, null otherwise.
     */
    private Cookie findCookieByName(Vector updatedCookies, String cookieName) {

        Cookie cookie = null;

        for (int i = 0; i < updatedCookies.size(); i++) {
            Object o = (Object)updatedCookies.elementAt(i);
            if (o instanceof Cookie) {
                cookie = (Cookie)o;
                if (cookie.getName().equals(cookieName)) {
                    break;
                } else {
                    cookie = null;
                }
            }
        }
        return cookie;
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
