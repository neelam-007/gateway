/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.logging.LogManager;
import com.l7tech.message.HttpTransportMetadata;
import com.l7tech.message.Request;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.service.PublishedService;
import com.l7tech.util.SoapUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.*;
import java.io.*;
import java.sql.SQLException;
import java.util.logging.Level;

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
    public static final String ENCODING = "UTF-8";

    public void init( ServletConfig config ) throws ServletException {
        super.init(config);
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        response.setContentType( "text/html" );
        response.setStatus( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
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

        BufferedWriter respWriter = null;
        OutputStream respStream = null;
        try {
            try {
                AssertionStatus status = MessageProcessor.getInstance().processMessage( sreq, sresp );

                if ( status == AssertionStatus.NONE ) {
                    hresponse.setContentType( CONTENT_TYPE );

                    String protRespXml = sresp.getResponseXml();
                    respWriter = new BufferedWriter( new OutputStreamWriter( hresponse.getOutputStream(), ENCODING ) );

                    respWriter.write( protRespXml );
                } else if ( sresp.isAuthenticationMissing() ||
                            status == AssertionStatus.AUTH_REQUIRED ||
                            status == AssertionStatus.AUTH_FAILED ) {
                    sendChallenge( sreq, sresp, hrequest, hresponse );
                } else {
                    sendFault( sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, status.getSoapFaultCode(), status.getMessage() );
                }
            } catch ( PolicyAssertionException pae ) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, pae);
                sendFault( sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SoapUtil.FC_SERVER, pae.toString() );
            } catch ( MessageProcessingException mpe ) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, mpe);
                sendFault( sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SoapUtil.FC_SERVER, mpe.toString() );
            }
        } catch ( SOAPException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch ( SQLException se ) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
                throw new ServletException( se );
            }

            if ( respWriter != null ) respWriter.close();
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
}
