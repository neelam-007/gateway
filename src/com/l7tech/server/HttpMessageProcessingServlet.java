/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.message.*;
import com.l7tech.policy.assertion.PolicyAssertionException;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.*;

import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpMessageProcessingServlet extends HttpServlet {
    public void init( ServletConfig config ) throws ServletException {
        super.init(config);
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        // TODO: REST support? ;)
        response.setContentType( "text/html" );
        response.sendError( 501, "GET not supported" );
        PrintWriter out = response.getWriter();
        out.println( "<html>" );
        out.println( "<head><title>GET not supported!</title></head>" );
        out.println( "<body><h1>GET not supported!</h1>Use POST instead!</body>" );
        out.close();
    }

    public void doPut( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        // TODO: REST support? ;)
        response.sendError( 501, "Method not implemented" );
    }

    public void doDelete( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        // TODO: REST support? ;)
        response.sendError( 501, "Method not implemented" );
    }

    public void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        // TODO: How do we know it's a SOAP request?
        HttpTransportMetadata htm = new HttpTransportMetadata( request, response );
        SoapRequest sreq = new SoapRequest( htm );
        SoapResponse sresp = new SoapResponse( htm );
        BufferedReader reqReader = null;
        PrintWriter respWriter = null;
        try {
            MessageProcessor.getInstance().processMessage( sreq, sresp );

            reqReader = new BufferedReader( sresp.getResponseReader() );
            respWriter = response.getWriter();

            char[] buf = new char[4096];
            int num;
            while ( ( num = reqReader.read( buf ) ) != -1 )
                respWriter.write( buf, 0, num );
        } catch ( PolicyAssertionException pae ) {
            // TODO: Detect authentication failure and send challenge!
            _log.error( pae );
        } catch ( MessageProcessingException mpe ) {
            // TODO: Barf a SOAP Fault
            _log.error( mpe );
        } finally {
            if ( reqReader != null ) reqReader.close();
            if ( respWriter != null ) respWriter.close();
        }
    }

    private Category _log = Category.getInstance( getClass() );
}
