/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.message.*;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.service.PublishedService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives SOAP requests via HTTP POST, passes them into the <code>MessageProcessor</code>
 * and formats the response as a reasonable approximation of an HTTP response.
 * 
 * @author alex
 * @version $Revision$
 */
public class SoapMessageProcessingServlet extends HttpServlet {
    public static final String POLICYURL_TAG = "policy-url";
    public static final String DEFAULT_CONTENT_TYPE = "text/xml; charset=utf-8";
    public static final String PARAM_POLICYSERVLET_URI = "PolicyServletUri";
    public static final String DEFAULT_POLICYSERVLET_URI = "/policy/disco.modulator?serviceoid=";
    public static final String ENCODING = "UTF-8";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>GET not supported!</title></head>");
        out.println("<body><h1>GET not supported!</h1>Use POST instead!</body>");
        out.close();
    }

    public void doPost(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        HttpTransportMetadata htm = new HttpTransportMetadata(hrequest, hresponse);
        HttpSoapRequest sreq = new HttpSoapRequest(htm);
        HttpSoapResponse sresp = new HttpSoapResponse(htm);

        // TODO: SOAP-with-attachments!

        BufferedWriter respWriter = null;
        OutputStream respStream = null;
        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            try {
                status = MessageProcessor.getInstance().processMessage(sreq, sresp);

                sresp.setHeadersIn(hresponse, status);
                String protRespXml = sresp.getResponseXml();

                if (status == AssertionStatus.NONE) {
                    if (protRespXml == null) {
                        logger.fine("Sending empty response");
                    } else {
                        String ctype = (String)sresp.getParameter(Response.PARAM_HTTP_CONTENT_TYPE);
                        if (ctype == null || ctype.length() == 0) {
                            ctype = DEFAULT_CONTENT_TYPE;
                            hresponse.setContentType(ctype);
                        }

                        respWriter = new BufferedWriter(new OutputStreamWriter(hresponse.getOutputStream(), ENCODING));
                        respWriter.write(protRespXml);
                    }
                } else if (sresp.isAuthenticationMissing() || status.isAuthProblem()) {
                    sendChallenge(sreq, sresp, hrequest, hresponse);
                } else {
                    sendFault(sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                              status.getSoapFaultCode(), status.getMessage());
                }
            } catch (PolicyAssertionException pae) {
                logger.log(Level.SEVERE, pae.getMessage(), pae);
                sendFault(sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          SoapUtil.FC_SERVER, pae.toString());
            } catch (PolicyVersionException pve) {
                String msg = "Request referred to an outdated version of policy";
                logger.log(Level.INFO, msg );
                sendFault(sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_EXPECTATION_FAILED,
                          SoapUtil.FC_CLIENT, msg);
            }
        } catch (SOAPException se) {
            logger.log(Level.SEVERE, se.getMessage(), se);
            try {
                sendFault(sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SoapUtil.FC_SERVER, se.getMessage());
            } catch (SOAPException se2) {
                logger.log(Level.SEVERE, "Second SOAPException while trying to send fault: " + se2.getMessage(), se2);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            try {
                sendFault(sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SoapUtil.FC_SERVER, e.getMessage());
            } catch (SOAPException se2) {
                logger.log(Level.SEVERE, "SOAPException while trying to send fault: " + se2.getMessage(), se2);
            }
        } finally {
            PersistenceContext pc = PersistenceContext.peek();
            if (pc != null) pc.close();

            // RequestAuditRecord rec = new RequestAuditRecord( "HTTP(s) SOAP Request", status );
            try { if (respWriter != null) respWriter.close(); } catch (Throwable t) {}
            try { if (respStream != null) respStream.close(); } catch (Throwable t) {}
            try { if (sreq != null) sreq.close(); } catch (Throwable t) {}
            try { if (sresp != null) sresp.close(); } catch (Throwable t) {}
        }
    }

    private String makePolicyUrl(HttpServletRequest hreq, long oid) {
        StringBuffer policyUrl = new StringBuffer(hreq.getScheme());
        policyUrl.append("://");
        policyUrl.append(hreq.getServerName());
        policyUrl.append(":");
        policyUrl.append(hreq.getServerPort());
        policyUrl.append(hreq.getContextPath());
        String policyServletUri = getServletConfig().getInitParameter(PARAM_POLICYSERVLET_URI);
        if (policyServletUri == null || policyServletUri.length() == 0)
            policyServletUri = DEFAULT_POLICYSERVLET_URI;

        policyUrl.append(policyServletUri);
        policyUrl.append(oid);

        return policyUrl.toString();
    }

    private void sendFault(SoapRequest sreq, SoapResponse sresp, HttpServletRequest hreq, HttpServletResponse hresp, int httpStatus, String faultCode, String faultString) throws SOAPException, IOException {
        OutputStream responseStream = null;

        try {
            hresp.setContentType(DEFAULT_CONTENT_TYPE);
            hresp.setStatus(httpStatus);

            SOAPMessage msg = SoapUtil.makeMessage();
            SOAPPart spart = msg.getSOAPPart();
            SOAPEnvelope senv = spart.getEnvelope();
            SOAPFault fault = SoapUtil.addFaultTo(msg, faultCode, faultString, null); // TODO use SSG url as faultactor

            PublishedService pserv = (PublishedService)sreq.getParameter(Request.PARAM_SERVICE);
            if (pserv != null && sresp.isPolicyViolated()) {
                String purl = makePolicyUrl(hreq, pserv.getOid());

                hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);

                Detail detail = fault.addDetail();
                DetailEntry entry = detail.addDetailEntry(senv.createName(POLICYURL_TAG));
                entry.addTextNode(purl);
            }

            responseStream = hresp.getOutputStream();
            msg.writeTo(responseStream);
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private void sendChallenge(SoapRequest sreq, SoapResponse sresp, HttpServletRequest hreq, HttpServletResponse hresp) throws SOAPException, IOException {
        sendFault(sreq, sresp, hreq, hresp, HttpServletResponse.SC_UNAUTHORIZED, SoapUtil.FC_CLIENT, "Authentication Required");
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
