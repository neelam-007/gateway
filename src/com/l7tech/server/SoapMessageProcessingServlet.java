/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.message.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.SoapUtil;
import com.l7tech.service.PublishedService;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.xml.soap.*;
import java.io.*;

import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapMessageProcessingServlet extends HttpServlet {
    public static final String POLICYURL_HEADER = "PolicyUrl";
    public static final String POLICYURL_TAG = "policy-url";
    public static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    public static final String PARAM_POLICYSERVLET_URI = "PolicyServletUri";
    public static final String DEFAULT_POLICYSERVLET_URI = "/policy/disco.modulator?serviceoid=";

    public void init( ServletConfig config ) throws ServletException {
        super.init(config);
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        response.setContentType( "text/html" );
        response.sendError( 501, "GET not supported" );
        PrintWriter out = response.getWriter();
        out.println( "<html>" );
        out.println( "<head><title>GET not supported!</title></head>" );
        out.println( "<body><h1>GET not supported!</h1>Use POST instead!</body>" );
        out.close();
    }

    public void doPost( HttpServletRequest hrequest, HttpServletResponse hresponse ) throws ServletException, IOException {
        HttpTransportMetadata htm = new HttpTransportMetadata( hrequest, hresponse );
        SoapRequest sreq = new SoapRequest( htm );
        SoapResponse sresp = new SoapResponse( htm );

        // TODO: SOAP-with-attachments!

        BufferedInputStream protRespStream = null;
        OutputStream respStream = null;
        try {
            try {
                AssertionStatus stat = MessageProcessor.getInstance().processMessage( sreq, sresp );

                if ( sresp.isAuthenticationMissing() ) {
                    sendChallenge( sreq, sresp, hrequest, hresponse );
                } else if ( stat != AssertionStatus.NONE ) {
                    sendFault( sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, stat.getSoapFaultCode(), stat.getMessage() );
                } else {
                    hresponse.setContentType( CONTENT_TYPE );
                    protRespStream = new BufferedInputStream( sresp.getProtectedResponseStream() );
                    respStream = hresponse.getOutputStream();

                    byte[] buf = new byte[4096];
                    int num;

                    while ( ( num = protRespStream.read( buf ) ) != -1 )
                        respStream.write( buf, 0, num );
                }

            } catch ( PolicyAssertionException pae ) {
                _log.error( pae );
                sendFault( sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SoapUtil.FC_SERVER, pae.toString() );
            } catch ( MessageProcessingException mpe ) {
                _log.error( mpe );
                sendFault( sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SoapUtil.FC_SERVER, mpe.toString() );
            }
        } catch ( SOAPException se ) {
            _log.error( se );
        } finally {
            if ( protRespStream != null ) protRespStream.close();
            if ( respStream != null ) respStream.close();
            if ( sresp != null ) sresp.close();
        }
    }

    private String makePolicyUrl( HttpServletRequest hreq, long oid ) {
        StringBuffer policyUrl = new StringBuffer( hreq.getScheme() );
        policyUrl.append( "://" );
        policyUrl.append( hreq.getServerName() );
        policyUrl.append( ":" );
        policyUrl.append( hreq.getServerPort() );
        policyUrl.append( hreq.getContextPath() );
        String policyServletUri = getServletConfig().getInitParameter( PARAM_POLICYSERVLET_URI );
        if ( policyServletUri == null || policyServletUri.length() == 0 )
            policyServletUri = DEFAULT_POLICYSERVLET_URI;

        policyUrl.append( policyServletUri );
        policyUrl.append( oid );

        return policyUrl.toString();
    }

    private void sendFault( SoapRequest sreq, SoapResponse sresp, HttpServletRequest hreq, HttpServletResponse hresp, int httpStatus, String faultCode, String faultString ) throws SOAPException, IOException {
        OutputStream responseStream = null;

        try {
            hresp.setContentType( CONTENT_TYPE );
            hresp.setStatus( httpStatus );

            SOAPMessage msg = SoapUtil.makeMessage();
            SOAPPart spart = msg.getSOAPPart();
            SOAPEnvelope senv = spart.getEnvelope();
            SOAPFault fault = SoapUtil.addFaultTo( msg, faultCode, faultString );

            PublishedService pserv = (PublishedService)sreq.getParameter( Request.PARAM_SERVICE );
            if ( pserv != null && sresp.isPolicyViolated() ) {
                String purl = makePolicyUrl( hreq, pserv.getOid() );

                hresp.setHeader( POLICYURL_HEADER, purl );

                Detail detail = fault.addDetail();
                DetailEntry entry = detail.addDetailEntry( senv.createName( POLICYURL_TAG ) );
                entry.addTextNode( purl );
            }

            responseStream = hresp.getOutputStream();
            msg.writeTo( responseStream );
        } finally {
            if ( responseStream != null ) responseStream.close();
        }
    }

    private void sendChallenge( SoapRequest sreq, SoapResponse sresp, HttpServletRequest hreq, HttpServletResponse hresp ) throws SOAPException, IOException {
        sendFault( sreq, sresp, hreq, hresp, HttpServletResponse.SC_UNAUTHORIZED, SoapUtil.FC_CLIENT, "Authentication Required" );
    }

    private Category _log = Category.getInstance( getClass() );
}
