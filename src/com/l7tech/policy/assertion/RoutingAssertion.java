/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.service.PublishedService;
import com.l7tech.credential.PrincipalCredentials;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Category;

import javax.wsdl.*;

/**
 * @author alex
 */
public class RoutingAssertion extends Assertion implements Cloneable, Serializable {
    public RoutingAssertion() {
        super();
        _connectionManager = new MultiThreadedHttpConnectionManager();
    }

    public RoutingAssertion(String protectedServiceUrl) {
        this();
        setProtectedServiceUrl(protectedServiceUrl);
    }

    public RoutingAssertion(String protectedServiceUrl, PrincipalCredentials protectedServiceCredentials) {
        this(protectedServiceUrl);
        setPrincipalCredentials(protectedServiceCredentials);
    }

    public Object clone() throws CloneNotSupportedException {
        RoutingAssertion n = (RoutingAssertion)super.clone();
        return n;
    }

    public PrincipalCredentials getPrincipalCredentials() {
        return _principalCredentials;
    }

    public void setPrincipalCredentials( PrincipalCredentials pc ) {
        _principalCredentials = pc;
        try {
            _httpCredentials = new UsernamePasswordCredentials( pc.getPrincipal().toString(),
                                                                new String( pc.getCredentials(), "UTF-8" ) );
        } catch ( UnsupportedEncodingException uee ) {
            _log.error( uee );
            throw new RuntimeException( uee );
        }
    }

    public String getProtectedServiceUrl() {
        return _protectedServiceUrl;
    }

    public void setProtectedServiceUrl( String protectedServiceUrl ) {
        this._protectedServiceUrl = protectedServiceUrl;
    }

    /**
     * Forwards the request along to a ProtectedService at the configured URL.
     * @param request The request to be forwarded.
     * @param response The response that was received from the ProtectedService.
     * @return an AssertionStatus indicating the success or failure of the request.
     * @throws PolicyAssertionException if some error preventing the execution of the PolicyAssertion has occurred.
     */
    public AssertionStatus checkRequest( Request request, Response response ) throws PolicyAssertionException {
      	HttpClient client = new HttpClient( _connectionManager );

        PostMethod postMethod = null;

        try {
            PublishedService service = (PublishedService)request.getParameter( Request.PARAM_SERVICE );
            URL url;
            if ( _protectedServiceUrl == null ) {
                url = service.serviceUrl( request );
            } else {
                url = new URL( _protectedServiceUrl );
            }

            postMethod = new PostMethod( url.toString() );
            synchronized( _httpState ) {
                if ( _httpState == null ) {
                    _httpState = new HttpState();
                    _httpState.setCredentials( url.getHost(),
                                               _principalCredentials.getRealm(),
                                               _httpCredentials );

                }
            }

            client.setState( _httpState );

            postMethod.setRequestBody( request.getRequestStream() );
            client.executeMethod( postMethod );
            InputStream responseStream = postMethod.getResponseBodyAsStream();
            response.setProtectedResponseStream( responseStream );

            request.setRouted( true );

            return AssertionStatus.NONE;
        } catch ( WSDLException we ) {
            _log.error( we );
            return AssertionStatus.FAILED;
        } catch ( MalformedURLException mfe ) {
            _log.error( mfe );
            return AssertionStatus.FAILED;
        } catch ( IOException ioe ) {
            _log.warn( ioe );
            return AssertionStatus.FAILED;
        } finally {
            if ( postMethod != null ) postMethod.releaseConnection();
        }
    }

    /** Client-side doesn't know or care about server-side routing. */
    public AssertionStatus decorateRequest( PendingRequest request ) throws PolicyAssertionException {
        return AssertionStatus.NOT_APPLICABLE;
    }

    protected String _protectedServiceUrl;
    protected PrincipalCredentials _principalCredentials;

    protected transient MultiThreadedHttpConnectionManager _connectionManager;
    protected transient Category _log = Category.getInstance( getClass() );
    protected transient HttpState _httpState;
    protected transient UsernamePasswordCredentials _httpCredentials;
}
