/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.identity.UserBean;
import com.l7tech.logging.LogManager;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.saml.SamlAssertionGenerator;
import com.l7tech.service.PublishedService;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side implementation of HTTP routing assertion.
 */
public class ServerHttpRoutingAssertion extends ServerRoutingAssertion {
    public static final String USER_AGENT = "User-Agent";
    public static final String HOST = "Host";

    public ServerHttpRoutingAssertion(HttpRoutingAssertion data) {
        _data = data;
        _connectionManager = new MultiThreadedHttpConnectionManager();
        int max = data.getMaxConnections();
        _connectionManager.setMaxConnectionsPerHost(max);
        _connectionManager.setMaxTotalConnections(max * 10);
        //_connectionManager.setConnectionStaleCheckingEnabled( false );
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
            PublishedService service = (PublishedService)request.getParameter(Request.PARAM_SERVICE);
            URL url;
            URL wsdlUrl = service.serviceUrl(request);
            String psurl = _data.getProtectedServiceUrl();
            if (psurl == null) {
                url = wsdlUrl;
            } else {
                url = new URL(psurl);
            }

            HttpClient client = new HttpClient(_connectionManager);

            postMethod = new PostMethod(url.toString());

            // TODO: Attachments
            postMethod.setRequestHeader(CONTENT_TYPE, TEXT_XML + "; charset=" + ENCODING.toLowerCase());

            String userAgent = _data.getUserAgent();
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

            String login = _data.getLogin();
            String password = _data.getPassword();

            if (login != null && password != null) {
                logger.fine("Using login '" + login + "'");
                HttpState state = client.getState();
                postMethod.setDoAuthentication(true);
                state.setAuthenticationPreemptive(true);
                state.setCredentials(null, null, new UsernamePasswordCredentials(login, new String(password)));
            }

            String requestXml = request.getRequestXml();
            if (_data.isAttachSamlSenderVouches()) {
                Document document = XmlUtil.stringToDocument(requestXml);
                SamlAssertionGenerator ag = new SamlAssertionGenerator();
//                SignerInfo si = new com.l7tech.common.security.Keys().asSignerInfo("CN="+ServerConfig.getInstance().getHostname());
                SignerInfo si = KeystoreUtils.getInstance().getSignerInfo();
                UserBean ub = new UserBean();
                ub.setName("CN="+login);
                ag.attachSenderVouches(document, ub, si);
                requestXml = XmlUtil.documentToString(document);
            }
            attachCookies(client, request.getTransportMetadata());
            postMethod.setRequestBody(requestXml);
            client.executeMethod(postMethod);

            int status = postMethod.getStatusCode();
            if (status == 200)
                logger.fine("Request routed successfully");
            else
                logger.info("Protected service responded with status " + status);

            response.setParameter(Response.PARAM_HTTP_STATUS, new Integer(status));

            // TODO: Attachments
            InputStream responseStream = postMethod.getResponseBodyAsStream();
            String ctype = postMethod.getResponseHeader(CONTENT_TYPE).getValue();
            response.setParameter(Response.PARAM_HTTP_CONTENT_TYPE, ctype);
            if (ctype.indexOf(TEXT_XML) >= 0) {
                // Note that this will consume the first part of the stream...
                BufferedReader br = new BufferedReader(new InputStreamReader(responseStream, ENCODING));
                String line;
                StringBuffer responseXml = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    responseXml.append(line);
                }
                response.setResponseXml(responseXml.toString());
            }
            response.setProtectedResponseStream(responseStream);

            request.setRoutingStatus(RoutingStatus.ROUTED);

            return AssertionStatus.NONE;
        } catch (WSDLException we) {
            logger.log(Level.SEVERE, null, we);
            return AssertionStatus.FAILED;
        } catch (MalformedURLException mfe) {
            logger.log(Level.SEVERE, null, mfe);
            return AssertionStatus.FAILED;
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            logger.log(Level.SEVERE, null, ioe);
            return AssertionStatus.FAILED;
        } catch (SAXException e) {
            logger.log(Level.SEVERE, null, e);
            return AssertionStatus.FAILED;
        } catch (SignatureException e) {
            logger.log(Level.SEVERE, null, e);
            return AssertionStatus.FAILED;
        } finally {
            if (postMethod != null) {
                MethodCloser mc = new MethodCloser(postMethod);
                response.runOnClose(mc);
            }
        }
    }

    /**
     * Attach cookies received by the client to the protected service
     * @param client the http client sender
     * @param transportMetadata the transport metadata
     */
    private void attachCookies(HttpClient client, TransportMetadata transportMetadata)  {
        if (!(transportMetadata instanceof HttpTransportMetadata)) {
            return;
        }
        HttpTransportMetadata httpTransportMetaData = (HttpTransportMetadata)transportMetadata;
        HttpServletRequest req = httpTransportMetaData.getRequest();
        HttpState state = client.getState();

        Cookie[] cookies = req.getCookies();
        for (int i = 0; cookies !=null && i < cookies.length; i++) {
            Cookie incomingCookie = cookies[i];
            org.apache.commons.httpclient.Cookie cookieOut = new org.apache.commons.httpclient.Cookie();
            cookieOut.setDomain(incomingCookie.getDomain());
            cookieOut.setPath(incomingCookie.getPath());
            cookieOut.setName(incomingCookie.getName());
            cookieOut.setValue(incomingCookie.getValue());
            cookieOut.setSecure(incomingCookie.getSecure());
            cookieOut.setVersion(incomingCookie.getVersion());
            cookieOut.setComment(incomingCookie.getComment());
            // cookieOut.setExpiryDate(??); // how to translate the getMaxAge() to the date? em
            logger.fine("Adding outgoing cookie "+cookieOut);
            state.addCookie(cookieOut);
        }

    }

    private static class MethodCloser implements Runnable {
        MethodCloser(HttpMethod method) {
            _method = method;
        }

        public void run() {
            _method.releaseConnection();
        }

        private HttpMethod _method;
    }


    protected HttpRoutingAssertion _data;

    protected transient MultiThreadedHttpConnectionManager _connectionManager;
    protected transient HttpState _httpState;
    protected transient UsernamePasswordCredentials _httpCredentials;
    Logger logger = LogManager.getInstance().getSystemLogger();
}
