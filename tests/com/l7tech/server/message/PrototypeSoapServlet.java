/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.message;

import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This was the prototype replacement for SoapMesProcServlet.
 *<p>
 * We'll keep it around for awhile in case there's something we forgot.
 */
public class PrototypeSoapServlet extends HttpServlet {
    public void doPost(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        PolicyEnforcementContext context = null;
        try {
            try {
                // Initialize request
                String rawct = hrequest.getContentType();
                ContentTypeHeader ctype;
                if (rawct != null && rawct.length() > 0)
                    ctype = ContentTypeHeader.parseValue(rawct);
                else
                    ctype = ContentTypeHeader.XML_DEFAULT;

                HttpRequestKnob reqKnob = new HttpServletRequestKnob(hrequest);
                HttpServletResponseKnob respKnob = new HttpServletResponseKnob(hresponse);
                Message reqMsg = new Message(StashManagerFactory.createStashManager(), ctype, hrequest.getInputStream());
                reqMsg.attachHttpRequestKnob(reqKnob);
                Message respMsg = new Message();
                respMsg.attachHttpResponseKnob(respKnob);

                context = new PolicyEnforcementContext(reqMsg, respMsg);
                AssertionStatus status = MessageProcessor.getInstance().processMessage(context);

                int routeStat = respKnob.getStatus();
                if (routeStat < 1) {
                    if ( status == AssertionStatus.NONE ) {
                        routeStat = HttpServletResponse.SC_OK;
                    } else {
                        // Request wasn't routed
                        routeStat = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
                    }
                }

                if (status == AssertionStatus.NONE) {
                    logger.fine("servlet transport returning 200");
                    MimeKnob responseMimeKnob = (MimeKnob)respMsg.getKnob(MimeKnob.class);
                    if (responseMimeKnob == null) {
                        logger.fine("Policy was successful but no response was produced; sending empty response");
                    } else {
                        hresponse.setContentType(respMsg.getMimeKnob().getOuterContentType().getFullValue());
                    }

                    HexUtils.copyStream(responseMimeKnob.getEntireMessageBodyAsInputStream(), hresponse.getOutputStream());
                    return;
                } else if (context.isAuthenticationMissing() || status.isAuthProblem()) {
                    logger.fine("servlet transport returning challenge");
                    sendChallenge(context, hrequest, hresponse);
                    return;
                } else if (context.getFaultDetail() != null) {
                    logger.fine("returning special soap fault");
                    sendFault(context, hrequest, hresponse);
                    return;
                } else {
                    logger.fine("servlet transport returning 500");
                    sendFault(context, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                              status.getSoapFaultCode(), status.getMessage());
                    return;
                }
            } catch (PolicyAssertionException pae) {
                logger.log(Level.SEVERE, pae.getMessage(), pae);
                sendFault(context, hrequest, hresponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          SoapFaultUtils.FC_SERVER, pae.toString());
                return;
            } catch (PolicyVersionException pve) {
                String msg = "Request referred to an outdated version of policy";
                logger.log(Level.INFO, msg);
                sendFault(context, hrequest, hresponse, HttpServletResponse.SC_EXPECTATION_FAILED,
                          SoapFaultUtils.FC_CLIENT, msg);
                return;
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            if (e instanceof Error) throw (Error)e;
            try {
                sendFault(context,
                          hrequest,
                          hresponse,
                          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          SoapFaultUtils.FC_SERVER,
                          e.getMessage());
            } catch (SAXException e1) {
                throw new ServletException(e);
            }
        } finally {
            AuditContext auditContext = AuditContext.peek();
            if (auditContext != null && !auditContext.isClosed()) auditContext.close();
            PersistenceContext pc = PersistenceContext.peek();
            if (pc != null) {
                try {
                    pc.commitIfPresent();
                } catch (ObjectModelException e) {
                    logger.log(Level.SEVERE, "Unable to commit transaction", e);
                }
                pc.close();
            }

            if (context != null) context.close();
        }
    }

    private void sendFault(PolicyEnforcementContext context, HttpServletRequest req, HttpServletResponse res)
            throws IOException, SAXException
    {
        OutputStream responseStream = null;
        try {
            responseStream = res.getOutputStream();
            String actor = req.getRequestURL().toString();
            res.setContentType(SoapMessageProcessingServlet.DEFAULT_CONTENT_TYPE);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            PublishedService pserv = context.getService();
            if (pserv != null && context.isPolicyViolated()) {
                String purl = makePolicyUrl(req, pserv.getOid());
                res.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
            }

            responseStream.write(SoapFaultUtils.generateRawSoapFault(context.getFaultDetail(), actor).getBytes());
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
        String policyServletUri = getServletConfig().getInitParameter(SoapMessageProcessingServlet.PARAM_POLICYSERVLET_URI);
        if (policyServletUri == null || policyServletUri.length() == 0)
            policyServletUri = SoapMessageProcessingServlet.DEFAULT_POLICYSERVLET_URI;

        policyUrl.append(policyServletUri);
        policyUrl.append(oid);

        return policyUrl.toString();
    }

    private void sendFault(PolicyEnforcementContext context,
                           HttpServletRequest hreq, HttpServletResponse hresp,
                           int httpStatus, String faultCode, String faultString) throws IOException, SAXException {
        OutputStream responseStream = null;
        try {
            responseStream = hresp.getOutputStream();
            String actor = hreq.getRequestURL().toString();
            hresp.setContentType(SoapMessageProcessingServlet.DEFAULT_CONTENT_TYPE);
            // todo, fla soap faults should always return 500
            hresp.setStatus(httpStatus);

            PublishedService pserv = context.getService();
            String purl = "";
            if (pserv != null && context.isPolicyViolated()) {
                purl = makePolicyUrl(hreq, pserv.getOid());
                hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
            }
            Element exceptiondetails = SoapFaultUtils.makeFaultDetailsSubElement("policyURL", purl);
            responseStream.write(SoapFaultUtils.generateRawSoapFault(faultCode, faultString,
                                                                     exceptiondetails, actor).getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private void sendChallenge(PolicyEnforcementContext context, HttpServletRequest hreq, HttpServletResponse hresp) throws IOException {
        // the challenge http header is supposed to already been appended at that point-ah
        hresp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        PublishedService pserv = context.getService();
        String purl = "";
        if (pserv != null && context.isPolicyViolated()) {
            purl = makePolicyUrl(hreq, pserv.getOid());
            hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
        }
        hresp.setContentType("text/plain; charset=\"utf-8\"");
        PrintWriter w = hresp.getWriter();
        w.print("Authentication Required");
        w.close();
    }

    private static final java.util.logging.Logger logger = Logger.getLogger(PrototypeSoapServlet.class.getName());

}
