/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.MultipartUtil;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.message.*;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.service.PublishedService;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    public static final String DEFAULT_CONTENT_TYPE = XmlUtil.TEXT_XML + "; charset=utf-8";
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

        BufferedWriter respWriter = null;
        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            try {
                status = MessageProcessor.getInstance().processMessage(sreq, sresp);

                sresp.setHeadersIn(hresponse, sresp, status);
                String protRespXml = sresp.getResponseXml();

                if (status == AssertionStatus.NONE) {
                    logger.fine("servlet transport returning 200");
                    if (protRespXml == null) {
                        logger.fine("Sending empty response");
                    } else {
                        String ctype = (String)sresp.getParameter(Response.PARAM_HTTP_CONTENT_TYPE);
                        if (ctype == null || ctype.length() == 0) {
                            ctype = DEFAULT_CONTENT_TYPE;
                            hresponse.setContentType(ctype);
                        }

                        respWriter = new BufferedWriter(new OutputStreamWriter(hresponse.getOutputStream(), ENCODING));

                        if(sresp.isMultipart()) {
                            StringBuffer sb = new StringBuffer();

                            // add modified SOAP part
                            MultipartUtil.addModifiedSoapPart(sb,
                                    protRespXml,
                                    sresp.getSoapPart(),
                                    sresp.getMultipartBoundary());

                            // add all Attachments
                            PushbackInputStream pbis = sresp.getMultipartReader().getPushbackInputStream();

                            // push the modified SOAP part back to the input stream
                            pbis.unread(sb.toString().getBytes());

                            BufferedReader reader = new BufferedReader(new InputStreamReader(pbis, ENCODING));

                            char[] buf = new char[1024];
                            int read;

                            while ((read = reader.read(buf, 0, buf.length)) > 0) {
                                respWriter.write(buf, 0, read);
                            }

                        } else {
                            respWriter.write(protRespXml);
                        }
                    }
                } else if (sresp.isAuthenticationMissing() || status.isAuthProblem()) {
                    logger.fine("servlet transport returning challenge");
                    sendChallenge(sreq, sresp, hrequest, hresponse);
                } else if (sresp.getFaultDetail() != null) {
                    logger.fine("returning special soap fault");
                    sendFault(sreq, sresp, sresp.getFaultDetail(), hrequest, hresponse);
                } else {
                    logger.fine("servlet transport returning 500");
                    sendFault(sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                              status.getSoapFaultCode(), status.getMessage());
                }
            } catch (PolicyAssertionException pae) {
                logger.log(Level.SEVERE, pae.getMessage(), pae);
                sendFault(sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          SoapFaultUtils.FC_SERVER, pae.toString());
            } catch (PolicyVersionException pve) {
                String msg = "Request referred to an outdated version of policy";
                logger.log(Level.INFO, msg );
                sendFault(sreq, sresp, hrequest, hresponse, HttpServletResponse.SC_EXPECTATION_FAILED,
                          SoapFaultUtils.FC_CLIENT, msg);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            try {
                sendFault(sreq,
                          sresp,
                          hrequest,
                          hresponse,
                          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          SoapFaultUtils.FC_SERVER,
                          e.getMessage());
            } catch (SAXException e1) {
                throw new ServletException(e);
            }
        } finally {
            PersistenceContext pc = PersistenceContext.peek();
            if (pc != null) {
                try {
                    pc.commitIfPresent();
                } catch (TransactionException e) {
                    logger.log(Level.SEVERE, "Unable to commit transaction", e);
                }
                pc.close();
            }

            try { if (respWriter != null) respWriter.close(); } catch (Throwable t) {}
            try { if (sreq != null) sreq.close(); } catch (Throwable t) {}
            try { if (sresp != null) sresp.close(); } catch (Throwable t) {}
        }
    }

    private void sendFault(SoapRequest sreq, SoapResponse sresp,
                           SoapFaultDetail faultDetail, HttpServletRequest req,
                           HttpServletResponse res) throws IOException, SAXException {
        OutputStream responseStream = null;
        try {
            responseStream = res.getOutputStream();
            String actor = req.getRequestURL().toString();
            res.setContentType(DEFAULT_CONTENT_TYPE);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            PublishedService pserv = (PublishedService)sreq.getParameter(Request.PARAM_SERVICE);
            if (pserv != null && sresp.isPolicyViolated()) {
                String purl = makePolicyUrl(req, pserv.getOid());
                res.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
            }

            responseStream.write(SoapFaultUtils.generateRawSoapFault(faultDetail, actor).getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
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

    private void sendFault(SoapRequest sreq, SoapResponse sresp,
                           HttpServletRequest hreq, HttpServletResponse hresp,
                           int httpStatus, String faultCode, String faultString) throws IOException, SAXException {
        OutputStream responseStream = null;
        try {
            responseStream = hresp.getOutputStream();
            String actor = hreq.getRequestURL().toString();
            hresp.setContentType(DEFAULT_CONTENT_TYPE);
            // todo, fla soap faults should always return 500
            hresp.setStatus(httpStatus);

            PublishedService pserv = (PublishedService)sreq.getParameter(Request.PARAM_SERVICE);
            String purl = "";
            if (pserv != null && sresp.isPolicyViolated()) {
                purl = makePolicyUrl(hreq, pserv.getOid());
                hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
            }

            responseStream.write(SoapFaultUtils.generateRawSoapFault(faultCode, faultString, purl, actor).getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private void sendChallenge(SoapRequest sreq, SoapResponse sresp,
                               HttpServletRequest hreq, HttpServletResponse hresp) throws IOException, SAXException {
        sendFault(sreq, sresp, hreq, hresp,
                HttpServletResponse.SC_UNAUTHORIZED, SoapFaultUtils.FC_CLIENT, "Authentication Required");
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
