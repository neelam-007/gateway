/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlRequest;
import com.l7tech.message.XmlResponse;
import com.l7tech.service.PublishedService;
import com.l7tech.logging.LogManager;
import com.l7tech.common.BuildInfo;
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
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerRoutingAssertion implements ServerAssertion {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String USER_AGENT = "User-Agent";
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
        //_connectionManager.setConnectionStaleCheckingEnabled( false );
    }

    public static final String PRODUCT = "Layer7-SecureSpan-Gateway";

    public static final String DEFAULT_USER_AGENT = PRODUCT + "/v" + BuildInfo.getProductVersion() + "-b" + BuildInfo.getBuildNumber();



    /**
     * Forwards the request along to a ProtectedService at the configured URL.
     * @param grequest The request to be forwarded.
     * @param gresponse The response that was received from the ProtectedService.
     * @return an AssertionStatus indicating the success or failure of the request.
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if some error preventing the execution of the PolicyAssertion has occurred.
     */
    public AssertionStatus checkRequest( Request grequest, Response gresponse ) throws IOException, PolicyAssertionException {
        grequest.setRoutingStatus( RoutingStatus.ATTEMPTED );

        XmlRequest request;
        XmlResponse response;
        if ( grequest instanceof XmlRequest && gresponse instanceof XmlResponse ) {
            request = (XmlRequest)grequest;
            response = (XmlResponse)gresponse;
        } else
            throw new PolicyAssertionException( "Only XML Requests are supported by ServerRoutingAssertion!" );

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

            HttpClient client = new HttpClient(_connectionManager);

            postMethod = new PostMethod(url.toString());

            // TODO: Attachments
            postMethod.setRequestHeader( CONTENT_TYPE, TEXT_XML + "; charset=" + ENCODING.toLowerCase());

            String userAgent = _data.getUserAgent();
            if ( userAgent == null || userAgent.length() == 0 ) userAgent = DEFAULT_USER_AGENT;
            postMethod.setRequestHeader( USER_AGENT, userAgent );

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
                logger.fine( "Using login '" + login + "'" );
                HttpState state = client.getState();
                postMethod.setDoAuthentication(true);
                state.setAuthenticationPreemptive(true);
                state.setCredentials(null, null, new UsernamePasswordCredentials(login, new String(password)));
            }

            String requestXml = request.getRequestXml();
            postMethod.setRequestBody(requestXml);
            client.executeMethod(postMethod);

            int status = postMethod.getStatusCode();
            if ( status == 200 )
                logger.fine( "Request routed successfully" );
            else
                logger.info( "Protected service responded with status " + status );

            response.setParameter( Response.PARAM_HTTP_STATUS, new Integer( status ) );

            // TODO: Attachments
            InputStream responseStream = postMethod.getResponseBodyAsStream();
            String ctype = postMethod.getRequestHeader(CONTENT_TYPE).getValue();
            response.setParameter( Response.PARAM_HTTP_CONTENT_TYPE, ctype );
            if (ctype.indexOf(TEXT_XML) > 0) {
                // Note that this will consume the first part of the stream...
                BufferedReader br = new BufferedReader( new InputStreamReader(responseStream, ENCODING  ) );
                String line;
                StringBuffer responseXml = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    responseXml.append(line);
                }
                response.setResponseXml(responseXml.toString());
            }
            response.setProtectedResponseStream(responseStream);

            request.setRoutingStatus( RoutingStatus.ROUTED );
            _data.incrementRequestCount();

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
        } finally {
            if (postMethod != null) {
                MethodCloser mc = new MethodCloser(postMethod);
                response.runOnClose(mc);
            }
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


    protected RoutingAssertion _data;

    protected transient MultiThreadedHttpConnectionManager _connectionManager;
    protected transient HttpState _httpState;
    protected transient UsernamePasswordCredentials _httpCredentials;
    Logger logger = LogManager.getInstance().getSystemLogger();
}
