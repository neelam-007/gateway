/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.service.PublishedService;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.wsdl.WSDLException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

/**
 * @author alex
 */
public class RoutingAssertion extends Assertion implements Cloneable, Serializable {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String HOST = "Host";
    public static final String SOAPACTION = "SOAPAction";
    public static final String TEXT_XML = "text/xml";
    public static final String ENCODING = "UTF-8";

    public RoutingAssertion() {
        this(null, null, null, null);

    }

    public RoutingAssertion(String protectedServiceUrl) {
        this(protectedServiceUrl, null, null, null);
    }

    /**
     * Full constructor.
     *
     * @param protectedServiceUrl the service url
     * @param login protected service login
     * @param password protected service password
     * @param realm protected servcie realm
     */
    public RoutingAssertion(String protectedServiceUrl, String login, String password, String realm) {
        _protectedServiceUrl = protectedServiceUrl;
        _login = login;
        _password = password;
        _realm = realm;
        _connectionManager = new MultiThreadedHttpConnectionManager();
    }

    public Object clone() throws CloneNotSupportedException {
        RoutingAssertion n = (RoutingAssertion)super.clone();
        return n;
    }

    public String getLogin() {
        return _login;
    }

    public void setLogin(String login) {
        _login = login;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public String getRealm() {
        return _realm;
    }

    public void setRealm(String realm) {
        _realm = realm;
    }

    public String getProtectedServiceUrl() {
        return _protectedServiceUrl;
    }

    public void setProtectedServiceUrl(String protectedServiceUrl) {
        this._protectedServiceUrl = protectedServiceUrl;
    }

    /**
     * Forwards the request along to a ProtectedService at the configured URL.
     * @param request The request to be forwarded.
     * @param response The response that was received from the ProtectedService.
     * @return an AssertionStatus indicating the success or failure of the request.
     * @throws PolicyAssertionException if some error preventing the execution of the PolicyAssertion has occurred.
     */
    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        HttpClient client = new HttpClient(_connectionManager);

        PostMethod postMethod = null;

        try {
            PublishedService service = (PublishedService)request.getParameter(Request.PARAM_SERVICE);
            URL url;
            URL serviceUrl = service.serviceUrl(request);
            if (_protectedServiceUrl == null) {
                url = serviceUrl;
            } else {
                url = new URL(_protectedServiceUrl);
            }

            postMethod = new PostMethod(url.toString());

            // TODO: Attachments
            postMethod.setRequestHeader(CONTENT_TYPE, TEXT_XML + "; charset=" + ENCODING.toLowerCase());
            int port = serviceUrl.getPort();
            StringBuffer hostValue = new StringBuffer(serviceUrl.getHost());
            if (port != -1) {
                hostValue.append(":");
                hostValue.append(port);
            }
            postMethod.setRequestHeader(HOST, hostValue.toString());
            postMethod.setRequestHeader(SOAPACTION, (String)request.getParameter(Request.PARAM_HTTP_SOAPACTION));

            if (_login != null && _password != null) {
                synchronized (this) {
                    if (_httpCredentials == null)
                        _httpCredentials = new UsernamePasswordCredentials(_login, _password);

                    if (_httpState == null) {
                        _httpState = new HttpState();
                        _httpState.setCredentials(url.getHost(),
                          _realm,
                          _httpCredentials);
                    }
                    client.setState(_httpState);
                }
            }

            String requestXml = request.getRequestXml();
            postMethod.setRequestBody(requestXml);
            client.executeMethod(postMethod);

            // TODO: Attachments
            InputStream responseStream = postMethod.getResponseBodyAsStream();
            String ctype = postMethod.getRequestHeader(CONTENT_TYPE).getValue();
            if (ctype.indexOf(TEXT_XML) > 0) {
                // Note that this will consume the first part of the stream...
                BufferedReader br = new BufferedReader(new InputStreamReader(responseStream));
                String line;
                StringBuffer responseXml = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    responseXml.append(line);
                }
                response.setResponseXml(responseXml.toString());
            }
            response.setProtectedResponseStream(responseStream);

            request.setRouted(true);

            return AssertionStatus.NONE;
        } catch (WSDLException we) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, we);
            return AssertionStatus.FAILED;
        } catch (MalformedURLException mfe) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, mfe);
            return AssertionStatus.FAILED;
        } catch (IOException ioe) {
            // TODO: Worry about what kinds of exceptions indicate failed routing, and which are "unrecoverable"
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, ioe);
            return AssertionStatus.FAILED;
        } finally {
            if (postMethod != null) {
                MethodCloser mc = new MethodCloser(postMethod);
                response.runOnClose(mc);
            }
        }
    }

    private class MethodCloser implements Runnable {
        MethodCloser(HttpMethod method) {
            _method = method;
        }

        public void run() {
            _method.releaseConnection();
        }

        private HttpMethod _method;
    }

    /** Client-side doesn't know or care about server-side routing. */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        return AssertionStatus.NOT_APPLICABLE;
    }

    protected String _protectedServiceUrl;
    protected String _login;
    protected String _password;
    protected String _realm;

    protected transient MultiThreadedHttpConnectionManager _connectionManager;
    protected transient HttpState _httpState;
    protected transient UsernamePasswordCredentials _httpCredentials;
}
