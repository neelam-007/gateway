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
import com.l7tech.service.Wsdl;

import java.io.*;
import java.util.*;

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

    public Object clone() throws CloneNotSupportedException {
        RoutingAssertion n = (RoutingAssertion)super.clone();
        return n;
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
        // TODO: Fix URL

        PostMethod postMethod = null;

        try {
            PublishedService service = (PublishedService)request.getParameter( Request.PARAM_SERVICE );
            Port wsdlPort = service.getWsdlPort( request );

            if ( wsdlPort == null ) {
                String err = "WSDL " + service.getWsdlUrl() + " has no Port!";
                _log.error( err );
                return AssertionStatus.FAILED;
            }

            postMethod = new PostMethod( _protectedServiceUrl );

            postMethod.setRequestBody( request.getRequestStream() );
            client.executeMethod( postMethod );
            InputStream responseStream = postMethod.getResponseBodyAsStream();
            response.setProtectedResponseStream( responseStream );

            return AssertionStatus.NONE;
        } catch ( WSDLException we ) {
            _log.error( we );
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

    protected transient MultiThreadedHttpConnectionManager _connectionManager;
    protected transient Category _log = Category.getInstance( getClass() );
}
