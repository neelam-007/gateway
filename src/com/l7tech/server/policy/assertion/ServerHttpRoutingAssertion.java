/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.saml.SamlAssertionGenerator;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.service.PublishedService;
import com.l7tech.identity.User;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.wsdl.WSDLException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Vector;
import java.util.logging.Level;
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

    public ServerHttpRoutingAssertion(HttpRoutingAssertion assertion) {
        this.httpRoutingAssertion = assertion;

        int max = httpRoutingAssertion.getMaxConnections();

        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.setMaxConnectionsPerHost(max);
        connectionManager.setMaxTotalConnections(max * 10);
        //connectionManager.setConnectionStaleCheckingEnabled( false );

        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { SslClientTrustManager.getInstance() }, null);
            final int timeout = Integer.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Couldn't initialize SSL Context", e);
            throw new RuntimeException(e);
        }
    }

    public static final String PRODUCT = "Layer7-SecureSpan-Gateway";

    public static final String DEFAULT_USER_AGENT = PRODUCT + "/v" + BuildInfo.getProductVersion() + "-b" + BuildInfo.getBuildNumber();

    /**
     * Forwards the request along to a ProtectedService at the configured URL.
     *
     * @param grequest  The request to be forwarded.
     * @param gresponse The response that was received from the ProtectedService.
     * @return an AssertionStatus indicating the success or failure of the request.
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          if some error preventing the execution of the PolicyAssertion has occurred.
     */
    public AssertionStatus checkRequest(Request grequest, Response gresponse) throws IOException, PolicyAssertionException {
        grequest.setRoutingStatus(RoutingStatus.ATTEMPTED);

        XmlRequest request;
        XmlResponse response;
        if (grequest instanceof XmlRequest && gresponse instanceof XmlResponse) {
            request = (XmlRequest)grequest;
            response = (XmlResponse)gresponse;
        } else
            throw new PolicyAssertionException("Only XML Requests are supported by ServerRoutingAssertion!");

        PostMethod postMethod = null;

        try {
            try {
                PublishedService service = (PublishedService)request.getParameter(Request.PARAM_SERVICE);
                URL url = getProtectedServiceUrl(service, request);

                HttpClient client = new HttpClient(connectionManager);
                HostConfiguration hconf = null;

                if ( "https".equals(url.getProtocol()) ) {
                    final int port = url.getPort() == -1 ? 443 : url.getPort();
                    hconf = new HostConfiguration();
                    synchronized( this ) {
                        if ( protocol == null ) {
                            protocol = new Protocol(url.getProtocol(), new SecureProtocolSocketFactory() {
                                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
                                    return sslContext.getSocketFactory().createSocket(socket,host,port,autoClose);
                                }

                                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException, UnknownHostException {
                                    return sslContext.getSocketFactory().createSocket(host,port,clientAddress,clientPort);
                                }

                                public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                                    return sslContext.getSocketFactory().createSocket(host,port);
                                }
                            }, port);
                        }
                    }

                    hconf.setHost(url.getHost(), port, protocol);
                }

                postMethod = new PostMethod(url.toString());

                // TODO: Attachments
                postMethod.setRequestHeader(XmlUtil.CONTENT_TYPE, XmlUtil.TEXT_XML + "; charset=" + ENCODING.toLowerCase());

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
                postMethod.setRequestHeader(SoapUtil.SOAPACTION, (String)request.getParameter(Request.PARAM_HTTP_SOAPACTION));

                if (httpRoutingAssertion.isTaiCredentialChaining()) {
                    String chainId = null;
                    if (!request.isAuthenticated()) {
                        logger.log(Level.FINE, "TAI credential chaining requested, but request was not authenticated.");
                    } else {
                        User clientUser = request.getUser();
                        if (clientUser != null) {
                            String id = clientUser.getLogin();
                            if (id == null || id.length() < 1) id = clientUser.getName();
                            if (id == null || id.length() < 1) id = clientUser.getUniqueIdentifier();

                            if (id != null && id.length() > 0) {
                                logger.log(Level.FINE, "TAI credential chaining requested; will chain username " + id);
                                chainId = id;
                            } else
                                logger.log(Level.WARNING, "TAI credential chaining requested, but request User did not have a unique identifier"); // can't happen
                        } else {
                            final String login = request.getPrincipalCredentials().getLogin();
                            if (login != null && login.length() > 0) {
                                logger.log(Level.FINE, "TAI credential chaining requested, but there is no User; " +
                                                       "will chain pc.login " + login);
                                chainId = login;
                            } else
                                logger.log(Level.WARNING, "TAI credential chaining requested, and request was authenticated, but had no User or pc.login");
                        }

                        if (chainId != null && chainId.length() > 0) {
                            postMethod.setRequestHeader("IV_USER", chainId);

                            // there is no defined quoting or escape mechanism for HTTP cookies so we'll use URLEncoding
                            org.apache.commons.httpclient.Cookie cookieOut = new org.apache.commons.httpclient.Cookie();
                            cookieOut.setName("IV_USER");
                            cookieOut.setValue(chainId);
                            cookieOut.setDomain(url.getHost());
                            cookieOut.setPath(url.getPath());
                            logger.fine("Adding outgoing cookie: name = " + cookieOut.getName());
                            client.getState().addCookie(cookieOut);
                        }
                    }
                }

                String login = httpRoutingAssertion.getLogin();
                String password = httpRoutingAssertion.getPassword();

                if (login != null && login.length() > 0
                    && password != null && password.length() > 0) {
                    logger.fine("Using login '" + login + "'");
                    HttpState state = client.getState();
                    postMethod.setDoAuthentication(true);
                    state.setAuthenticationPreemptive(true);
                    state.setCredentials(null, null, new UsernamePasswordCredentials(login, new String(password)));
                }

                String requestXml = request.getRequestXml();
                if (httpRoutingAssertion.isAttachSamlSenderVouches()) {
                    Document document = XmlUtil.stringToDocument(requestXml);
                    SamlAssertionGenerator ag = new SamlAssertionGenerator();
                    SignerInfo si = KeystoreUtils.getInstance().getSignerInfo();
                    SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
                    final TransportMetadata tm = request.getTransportMetadata();
                    if ( tm instanceof HttpTransportMetadata ) {
                        final HttpTransportMetadata htm = (HttpTransportMetadata)tm;
                        try {
                            InetAddress clientAddress = InetAddress.getByName(htm.getRequest().getRemoteAddr());
                            samlOptions.setClientAddress(clientAddress);
                        } catch (UnknownHostException e) {
                            logger.warning("Couldn't resolve client IP address");
                        }
                    }
                    samlOptions.setExpiryMinutes(5);
                    ag.attachSenderVouches(document, si, request.getPrincipalCredentials(), samlOptions);
                    requestXml = XmlUtil.nodeToString(document);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(requestXml);
                    }
                }
                attachCookies(client, request.getTransportMetadata(), url);
                postMethod.setRequestBody(requestXml);

                if ( hconf == null ) {
                    client.executeMethod(postMethod);
                } else {
                    client.executeMethod(hconf,postMethod);
                }

                int status = postMethod.getStatusCode();
                if (status == 200)
                    logger.fine("Request routed successfully");
                else
                    logger.info("Protected service responded with status " + status);

                response.setParameter(Response.PARAM_HTTP_STATUS, new Integer(status));

                // TODO: Attachments

                request.setRoutingStatus(RoutingStatus.ROUTED);

            } catch (WSDLException we) {
                logger.log(Level.SEVERE, null, we);
                return AssertionStatus.FAILED;
            } catch (MalformedURLException mfe) {
                logger.log(Level.SEVERE, null, mfe);
                return AssertionStatus.FAILED;
            } catch (IOException ioe) {
                // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
                logger.log(Level.SEVERE, ioe.getMessage(), ioe);
                return AssertionStatus.FAILED;
            } catch (SAXException e) {
                logger.log(Level.SEVERE, null, e);
                return AssertionStatus.FAILED;
            } catch (SignatureException e) {
                logger.log(Level.SEVERE, null, e);
                return AssertionStatus.FAILED;
            } catch (CertificateException e) {
                logger.log(Level.SEVERE, null, e);
                return AssertionStatus.FAILED;
            }
            // BEYOND THIS POINT, WE DONT RETURN FAILURE
            try {
                InputStream responseStream = postMethod.getResponseBodyAsStream();
                String ctype = postMethod.getResponseHeader(XmlUtil.CONTENT_TYPE).getValue();
                response.setParameter(Response.PARAM_HTTP_CONTENT_TYPE, ctype);
                // Note that this will consume the first part of the stream...
                BufferedReader br = new BufferedReader(new InputStreamReader(responseStream, ENCODING));
                StringBuffer responseXml = new StringBuffer();
                char[] buf = new char[1024];
                int read = br.read(buf);
                while (read > 0) {
                    responseXml.append(buf, 0, read);
                    read = br.read(buf);
                }
                response.setResponseXml(responseXml.toString());
                response.setProtectedResponseStream(responseStream);
            } catch (IOException e) {
                logger.log(Level.FINE, "error reading response", e);
                // here we dont return error because we already routed
            }
        } finally {
            if (postMethod != null) {
                MethodCloser mc = new MethodCloser(postMethod);
                response.runOnClose(mc);
            }
        }

        return AssertionStatus.NONE;
    }

    private URL getProtectedServiceUrl(PublishedService service, XmlRequest request) throws WSDLException, MalformedURLException {
        URL url;
        URL wsdlUrl = service.serviceUrl(request);
        String psurl = httpRoutingAssertion.getProtectedServiceUrl();
        if (psurl == null) {
            url = wsdlUrl;
        } else {
            url = new URL(psurl);
        }
        return url;
    }

    /**
     * Attach cookies received by the client to the protected service
     * @param client the http client sender
     * @param transportMetadata the transport metadata
     * @param url
     */
    private void attachCookies(HttpClient client, TransportMetadata transportMetadata, URL url)  {
        if (!(transportMetadata instanceof HttpTransportMetadata)) {
            return;
        }
        HttpTransportMetadata httpTransportMetaData = (HttpTransportMetadata)transportMetadata;
        HttpServletRequest req = httpTransportMetaData.getRequest();
        Vector updatedCookies = httpTransportMetaData.getUpdatedCookies();
        HttpState state = client.getState();
        Cookie updatedCookie = null;

        Cookie[] cookies = req.getCookies();
        org.apache.commons.httpclient.Cookie cookieOut = null;

        // if no cookies found in the request but there is cookies in the udpatedCookies list (i.e. new cookies)
        if ((cookies == null || cookies.length == 0)) {
            if (updatedCookies.size() > 0) {
                for (int i = 0; i < updatedCookies.size(); i++) {
                    Object o = (Object) updatedCookies.elementAt(i);
                    if (o instanceof Cookie) {
                        Cookie newCookie = (Cookie) o;
                        cookieOut = new org.apache.commons.httpclient.Cookie();
                        cookieOut.setDomain(url.getHost());
                        cookieOut.setPath(url.getPath());
                        cookieOut.setName(newCookie.getName());
                        cookieOut.setSecure(newCookie.getSecure());
                        cookieOut.setVersion(newCookie.getVersion());
                        cookieOut.setComment(newCookie.getComment());
                        // cookieOut.setExpiryDate(??); // how to translate the getMaxAge() to the date? em
                        logger.fine("Adding outgoing cookie: name = " + cookieOut.getName() + ", version = " + cookieOut.getVersion());
                        state.addCookie(cookieOut);
                    }
                }
            }
        } else {
            for (int i = 0; cookies != null && i < cookies.length; i++) {
                Cookie incomingCookie = cookies[i];
                cookieOut = new org.apache.commons.httpclient.Cookie(url.getHost(), incomingCookie.getName(), incomingCookie.getValue());
                cookieOut.setPath(url.getPath());

                // override the old cookie if the new one is found
                updatedCookie = findCookieByName(updatedCookies, incomingCookie.getName());
                if (updatedCookie != null) {
                    cookieOut.setValue(updatedCookie.getValue());
                    logger.fine("Updating cookie: name = " + updatedCookie.getName());
                } else {
                    cookieOut.setValue(incomingCookie.getValue());
                }
                cookieOut.setSecure(incomingCookie.getSecure());
                cookieOut.setVersion(incomingCookie.getVersion());
                cookieOut.setComment(incomingCookie.getComment());
                // cookieOut.setExpiryDate(??); // how to translate the getMaxAge() to the date? em
                logger.fine("Adding outgoing cookie: name = " + cookieOut.getName() + ", version = " + cookieOut.getVersion());
                state.addCookie(cookieOut);
            }
        }

    }

    /**
     * Find the cookie given the name.
     *
     * @param updatedCookies  the list of cookies
     * @param cookieName  the given cookie name
     * @return  Cookie  the cookie object that matches the given name, null otherwise.
     */ 
    private Cookie findCookieByName(Vector updatedCookies, String cookieName) {

        Cookie cookie = null;

        for (int i = 0; i < updatedCookies.size(); i++) {
            Object o = (Object) updatedCookies.elementAt(i);
            if(o instanceof Cookie) {
                cookie = (Cookie) o;
                if(cookie.getName().equals(cookieName))
                {
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
