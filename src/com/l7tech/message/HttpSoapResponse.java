/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

/**
 * Encapsulates a SOAP response using HTTP transport.  Not thread-safe.
 *
 * @author alex
 * @version $Revision$
 */
public class HttpSoapResponse extends SoapResponse {
    public HttpSoapResponse( HttpTransportMetadata htm ) {
        super( htm );
    }

    public void setHeadersIn( HttpServletResponse hresponse, HttpSoapResponse sresp, AssertionStatus status ) throws IOException {

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

        String name;
        Object ovalue;
        for (Iterator i = _params.keySet().iterator(); i.hasNext();) {
            name = (String)i.next();
            if ( name.startsWith( PREFIX_HTTP_HEADER ) ) {
                ovalue = _params.get(name);
                if ( name == null || ovalue == null ) continue;

                if ( PARAM_HTTP_CONTENT_TYPE.equals( name ) ) {
                    hresponse.setContentType( sresp.getOuterContentType().getValue() );
                    continue;
                }

                String hname = name.substring( PREFIX_HTTP_HEADER.length() + 1 );

                if ( ovalue instanceof String[] ) {
                    String[] vals = (String[])ovalue;
                    for ( int j = 0; j < vals.length; j++ ) {
                        String val = vals[j];
                        hresponse.addHeader( hname, val );
                    }
                } else if ( ovalue instanceof String ) {
                    hresponse.setHeader( hname, (String)ovalue );
                } else {
                    // Somebody else's problem, HTTP can't handle it.
                    continue;
                }
            }
        }
    }

    public Object doGetParameter( String name ) {
        return _transportMetadata.getResponseParameter(name);
    }
}
