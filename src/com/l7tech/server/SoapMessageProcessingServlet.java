/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.service.PublishedService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Set;
import java.net.URI;

/**
 * Receives SOAP requests via HTTP POST, passes them into the <code>MessageProcessor</code>
 * and formats the response as a reasonable approximation of an HTTP response.
 *
 * @author alex
 * @version $Revision$
 */
public class SoapMessageProcessingServlet extends HttpServlet {
    public static final String DEFAULT_CONTENT_TYPE = XmlUtil.TEXT_XML + "; charset=utf-8";
    public static final String PARAM_POLICYSERVLET_URI = "PolicyServletUri";
    public static final String DEFAULT_POLICYSERVLET_URI = "/policy/disco?serviceoid=";
    private WebApplicationContext applicationContext;
    private MessageProcessor messageProcessor;
    private AuditContext auditContext;
    private ServerConfig serverConfig;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        messageProcessor = (MessageProcessor)applicationContext.getBean("messageProcessor");
        auditContext = (AuditContext)applicationContext.getBean("auditContext");
        serverConfig = (ServerConfig)applicationContext.getBean("serverConfig");
    }

    public void doGet(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        throwBadMethod(hresponse, "GET");
    }

    private void throwBadMethod(HttpServletResponse hresponse, String method) throws IOException {
        hresponse.setContentType("text/html");
        hresponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        PrintWriter out = hresponse.getWriter();
        out.println("<html>");
        out.print("<head><title>");
        out.print(method);
        out.print(" not supported!</title></head>");
        out.print("<body><h1>");
        out.print(method);
        out.print(" not supported!</h1>Use POST instead!</body>");
        out.close();
    }

    protected void doHead(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        throwBadMethod(hresponse, "HEAD");
    }

    protected void doPut(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        throwBadMethod(hresponse, "PUT");
    }

    protected void doDelete(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        throwBadMethod(hresponse, "DELETE");
    }

    protected void doOptions(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        throwBadMethod(hresponse, "OPTIONS");
    }

    protected void doTrace(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        throwBadMethod(hresponse, "TRACE");
    }

    public void doPost(final HttpServletRequest hrequest,
                       final HttpServletResponse hresponse)
      throws ServletException, IOException {
        // Initialize processing context
        final Message response = new Message();
        final Message request = new Message();

        final String rawct = hrequest.getContentType();
        ContentTypeHeader ctype = rawct != null && rawct.length() > 0
          ? ContentTypeHeader.parseValue(rawct)
          : ContentTypeHeader.XML_DEFAULT;

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(hrequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(hresponse);
        response.attachHttpResponseKnob(respKnob);

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setReplyExpected(true); // HTTP always expects to receive a reply

        initCookies(hrequest.getCookies(), context);

        final StashManager stashManager = StashManagerFactory.createStashManager();

        try {
            // Process message
            request.initialize(stashManager, ctype, hrequest.getInputStream());
            AssertionStatus status = AssertionStatus.UNDEFINED;
            status = messageProcessor.processMessage(context);

            // if the policy is not successful AND the stealth flag is on, drop connection
            if (status != AssertionStatus.NONE && context.isStealthResponseMode()) {
                logger.info("Policy returned error and stealth mode is set. " +
                            "Instructing valve to drop connection completly.");
                hrequest.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME,
                                      ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                return;
            } else if (status == AssertionStatus.SERVICE_NOT_FOUND && isGlobalSettingStealthPolicyNotFound()) {
                logger.info("No policy found and global setting is to go stealth in this case. " +
                            "Instructing valve to drop connection completly.");
                hrequest.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME,
                                      ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                return;
            }

            // Send response headers
            propagateCookies(context, (HttpServletRequestKnob) reqKnob, respKnob);
            respKnob.beginResponse();

            int routeStat = respKnob.getStatus();
            if (routeStat < 1) {
                if (status == AssertionStatus.NONE) {
                    routeStat = HttpServletResponse.SC_OK;
                } else {
                    // Request wasn't routed
                    routeStat = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
                }
            }

            if (status == AssertionStatus.NONE) {
                if (response.getKnob(MimeKnob.class) == null) {
                    // Routing successful, but no actual response received, probably due to a one-way JMS send.
                    hresponse.setStatus(200);
                    hresponse.setContentType(null);
                    hresponse.setContentLength(0);
                    hresponse.getOutputStream().close();
                    logger.fine("servlet transport returning a placeholder empty response to a successful one-way message");
                    return;
                }

                // Transmit the response and return
                hresponse.setStatus(routeStat);
                hresponse.setContentType(response.getMimeKnob().getOuterContentType().getFullValue());
                OutputStream responseos = hresponse.getOutputStream();
                HexUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), responseos);
                responseos.close();
                logger.fine("servlet transport returned status " + routeStat +
                            ". content-type " + response.getMimeKnob().getOuterContentType().getFullValue());

                return;
            } else if (context.getFaultDetail() != null) {
                logger.fine("returning special soap fault");
                sendFault(context, context.getFaultDetail(), hrequest, hresponse);
                return;
            } else if (respKnob.hasChallenge()) {
                logger.fine("servlet transport returning challenge");
                respKnob.beginChallenge();
                sendChallenge(context, hrequest, hresponse);
                return;
            } else {
                logger.fine("servlet transport returning 500");
                sendFault(context, hrequest, hresponse,
                  status.getSoapFaultCode(), status.getMessage());
                return;
            }
        } catch (Throwable e) {
            // if the policy throws AND the stealth flag is set, drop connection
            if (context.isStealthResponseMode()) {
                logger.log(Level.INFO, "Policy threw error and stealth mode is set. " +
                                       "Instructing valve to drop connection completly.",
                                       e);
                hrequest.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME,
                                      ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                return;
            }
            try {
                if (e instanceof PolicyAssertionException) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    sendFault(context, hrequest, hresponse,
                              SoapFaultUtils.FC_SERVER, e.toString());
                    return;
                } else if (e instanceof PolicyVersionException) {
                    String msg = "Request referred to an outdated version of policy";
                    logger.log(Level.INFO, msg);
                    sendFault(context, hrequest, hresponse,
                              SoapFaultUtils.FC_CLIENT, msg);
                    return;
                } else if (e instanceof NoSuchPartException) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    sendFault(context, hrequest, hresponse,
                              SoapFaultUtils.FC_CLIENT, e.toString());
                    return;
                } else {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    //? if (e instanceof Error) throw (Error)e;
                    sendFault(context,
                              hrequest,
                              hresponse,
                              SoapFaultUtils.FC_SERVER,
                              e.getMessage());
                }
            } catch (SAXException e1) {
                throw new ServletException(e1);
            }
        } finally {
            try {
                auditContext.flush();
            } finally {
                context.close();
            }
        }
    }

    private void initCookies(Cookie[] cookies, PolicyEnforcementContext context) {
        if(cookies!=null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Adding request cookie to context; name='"+cookie.getName()+"'.");
                }
                context.addCookie(CookieUtils.fromServletCookie(cookie,false));
            }
        }
    }

    private void propagateCookies(PolicyEnforcementContext context, HttpRequestKnob reqKnob, HttpResponseKnob resKnob) {
        Set cookies = context.getCookies();
        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
            HttpCookie cookie = (HttpCookie) iterator.next();
            if(cookie.isNew()) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Adding new cookie to response; name='"+cookie.getCookieName()+"'.");
                }
                URI url = URI.create(reqKnob.getRequestUrl());
                resKnob.addCookie(CookieUtils.ensureValidForDomainAndPath(cookie, url.getHost(), url.getPath()));
            }
        }
    }

    private void sendFault(PolicyEnforcementContext context,
                           SoapFaultDetail faultDetail, HttpServletRequest req,
                           HttpServletResponse res) throws IOException, SAXException {
        OutputStream responseStream = null;
        try {
            responseStream = res.getOutputStream();
            String actor = req.getRequestURL().toString();
            res.setContentType(DEFAULT_CONTENT_TYPE);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            PublishedService pserv = context.getService();
            if (pserv != null && shouldSendBackPolicyUrl(context)) {
                String purl = makePolicyUrl(req, pserv.getOid());
                res.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
            }

            responseStream.write(SoapFaultUtils.generateSoapFaultXml(faultDetail.getFaultCode(),
              faultDetail.getFaultString(),
              faultDetail.getFaultDetail(),
              actor).getBytes());
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

    private void sendFault(PolicyEnforcementContext context,
                           HttpServletRequest hreq, HttpServletResponse hresp,
                           String faultCode, String faultString) throws IOException, SAXException {
        OutputStream responseStream = null;
        try {
            responseStream = hresp.getOutputStream();
            String actor = hreq.getRequestURL().toString();
            hresp.setContentType(DEFAULT_CONTENT_TYPE);
            hresp.setStatus(500); // soap faults "MUST" be sent with status 500 per Basic profile

            PublishedService pserv = context.getService();
            String purl = "";
            if (pserv != null && shouldSendBackPolicyUrl(context)) {
                purl = makePolicyUrl(hreq, pserv.getOid());
                hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
            }
            Element exceptiondetails = SoapFaultUtils.makeFaultDetailsSubElement("policyURL", purl);
            responseStream.write(SoapFaultUtils.generateSoapFaultXml(faultCode, faultString,
              exceptiondetails, actor).getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private boolean shouldSendBackPolicyUrl(PolicyEnforcementContext context) throws IOException {
        if (context.isRequestPolicyViolated()) {
            return true;
        }
        String requestorVersion = context.getRequest().
                                        getHttpRequestKnob().
                                            getHeaderSingleValue(SecureSpanConstants.HttpHeaders.POLICY_VERSION);
        if (requestorVersion == null || requestorVersion.length() < 1) {
            return true;
        }
        return false;
    }

    private void sendChallenge(PolicyEnforcementContext context,
                               HttpServletRequest hreq, HttpServletResponse hresp) throws IOException {
        ServletOutputStream sos = null;
        try {
            // the challenge http header is supposed to already been appended at that point-ah
            hresp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PublishedService pserv = context.getService();
            String purl = "";
            if (pserv != null && shouldSendBackPolicyUrl(context)) {
                purl = makePolicyUrl(hreq, pserv.getOid());
                hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
            }
            sos = hresp.getOutputStream();
            sos.print("Authentication Required");
        } finally {
            if (sos != null) sos.close();
        }
    }

    private boolean isGlobalSettingStealthPolicyNotFound() {
        // todo, use cluster property instead in 3.4
        // serverConfig properties are already cached so no need to cache here
        String property = serverConfig.getProperty("noServiceResolvedStealthResponse");
        logger.finest("noServiceResolvedStealthResponse has value " + property);
        if (property != null && Boolean.parseBoolean(property)) {
            return true;
        }
        return false;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
