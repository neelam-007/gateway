/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.service.PublishedService;
import com.l7tech.logging.LogManager;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerRoutingAssertion implements ServerAssertion {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String HOST = "Host";
    public static final String SOAPACTION = "SOAPAction";
    public static final String TEXT_XML = "text/xml";
    public static final String ENCODING = "UTF-8";

    public ServerRoutingAssertion( RoutingAssertion data ) {
        _data = data;
        _connectionManager = new MultiThreadedHttpConnectionManager();
        int max = data.getMaxConnections();
        _connectionManager.setMaxConnectionsPerHost( max );
        _connectionManager.setMaxTotalConnections( max * 10 );
    }

    /**
     * Forwards the request along to a ProtectedService at the configured URL.
     * @param request The request to be forwarded.
     * @param response The response that was received from the ProtectedService.
     * @return an AssertionStatus indicating the success or failure of the request.
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if some error preventing the execution of the PolicyAssertion has occurred.
     */
    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        HttpClient client = new HttpClient(_connectionManager);

        PostMethod postMethod = null;

        try {
            PublishedService service = (PublishedService)request.getParameter(Request.PARAM_SERVICE);
            URL url;
            URL wsdlUrl = service.serviceUrl(request);
            String psurl = _data.getProtectedServiceUrl();
            if ( psurl == null) {
                url = wsdlUrl;
            } else {
                url = new URL( psurl );
            }

            postMethod = new PostMethod(url.toString());

            // TODO: Attachments
            postMethod.setRequestHeader(CONTENT_TYPE, TEXT_XML + "; charset=" + ENCODING.toLowerCase());
            int port = wsdlUrl.getPort();
            StringBuffer hostValue = new StringBuffer(wsdlUrl.getHost());
            if (port != -1) {
                hostValue.append(":");
                hostValue.append(port);
            }
            postMethod.setRequestHeader(HOST, hostValue.toString());
            postMethod.setRequestHeader(SOAPACTION, (String)request.getParameter(Request.PARAM_HTTP_SOAPACTION));

            String login = _data.getLogin();
            String password = _data.getPassword();

            if ( login != null && password != null) {
                synchronized (this) {
                    if (_httpCredentials == null)
                        _httpCredentials = new UsernamePasswordCredentials( login, password );

                    if (_httpState == null) {
                        _httpState = new HttpState();
                        _httpState.setCredentials(url.getHost(),
                          _data.getRealm(),
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
            _data.incrementRequestCount();

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


    protected RoutingAssertion _data;

    protected transient MultiThreadedHttpConnectionManager _connectionManager;
    protected transient HttpState _httpState;
    protected transient UsernamePasswordCredentials _httpCredentials;
}
