/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.policy.assertion.AssertionStatus;

import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

/**
 * Encapsulates a SOAP response using HTTP transport.
 *
 * @author alex
 * @version $Revision$
 */
public class HttpSoapResponse extends SoapResponse {
    public HttpSoapResponse( HttpTransportMetadata htm ) {
        super( htm );
    }

    public void setHeadersIn( HttpServletResponse hresponse, AssertionStatus status ) {
        Integer irouteStat = (Integer)getParameter( Response.PARAM_HTTP_STATUS );
        int routeStat;
        if ( irouteStat == null ) {
            if ( status == AssertionStatus.NONE ) {
                routeStat = HttpServletResponse.SC_OK;
            } else {
                // Request wasn't routed
                routeStat = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            }
        } else
            routeStat = irouteStat.intValue();

        hresponse.setStatus( routeStat );

        String name, value;
        for (Iterator i = _params.keySet().iterator(); i.hasNext();) {
            name = (String)i.next();
            if ( name.startsWith( PREFIX_HTTP_HEADER ) ) {
                value = (String)_params.get(name);

                if ( name == null || value == null ) continue;

                String hname = name.substring( PREFIX_HTTP_HEADER.length() + 1 );
                hresponse.setHeader( hname, value );
            }
        }
    }
}
