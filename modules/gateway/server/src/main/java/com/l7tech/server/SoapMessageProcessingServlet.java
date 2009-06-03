/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_HTTP_MESSAGE_INPUT;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.util.DelegatingServletInputStream;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Receives SOAP requests via HTTP POST, passes them into the <code>MessageProcessor</code>
 * and formats the response as a reasonable approximation of an HTTP response.
 * <p/>
 * The name of this class has not been accurate since non-SOAP web services were added in SecureSpan version 3.0.
 */
public class SoapMessageProcessingServlet extends HttpServlet {
    public static final String DEFAULT_CONTENT_TYPE = XmlUtil.TEXT_XML + "; charset=utf-8";
    public static final String SOAP_1_2_CONTENT_TYPE = SOAPConstants.SOAP_1_2_CONTENT_TYPE + "; charset=utf-8";
    public static final String PARAM_POLICYSERVLET_URI = "PolicyServletUri";
    public static final String DEFAULT_POLICYSERVLET_URI = "/policy/disco?serviceoid=";

    private static final String GZIP_REQUESTS_FORBIDDEN_SOAP_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                     "    <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                                                     "    <soapenv:Body>\n" +
                                                                     "        <soapenv:Fault>\n" +
                                                                     "            <faultcode>soapenv:Server</faultcode>\n" +
                                                                     "            <faultstring>Rejecting GZIP compressed request</faultstring>\n" +
                                                                     "            <faultactor>http://soong:8080/xml/blub</faultactor>\n" +
                                                                     "            <detail>This server does not accept GZIP compressed requests.</detail>\n" +
                                                                     "        </soapenv:Fault>\n" +
                                                                     "    </soapenv:Body>\n" +
                                                                     "</soapenv:Envelope>";

    private final Logger logger = Logger.getLogger(getClass().getName());

    private WebApplicationContext applicationContext;
    private ServerConfig serverConfig;
    private MessageProcessor messageProcessor;
    private AuditContext auditContext;
    private SoapFaultManager soapFaultManager;
    private ClusterPropertyCache clusterPropertyCache;
    private LicenseManager licenseManager;
    private StashManagerFactory stashManagerFactory;
    private ApplicationEventPublisher messageProcessingEventChannel;
    private Auditor auditor;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        serverConfig = (ServerConfig)applicationContext.getBean("serverConfig");
        messageProcessor = (MessageProcessor)applicationContext.getBean("messageProcessor");
        auditContext = (AuditContext)applicationContext.getBean("auditContext");
        soapFaultManager = (SoapFaultManager)applicationContext.getBean("soapFaultManager");
        clusterPropertyCache = (ClusterPropertyCache)applicationContext.getBean("clusterPropertyCache");
        licenseManager = (LicenseManager)applicationContext.getBean("licenseManager");
        stashManagerFactory = (StashManagerFactory)applicationContext.getBean("stashManagerFactory");
        messageProcessingEventChannel = (EventChannel)applicationContext.getBean("messageProcessingEventChannel", EventChannel.class);
        auditor = new Auditor(this, applicationContext, logger);
    }

    /**
     * Backwards-ish entry point so that unit tests will work.
     *
     * @param hrequest
     * @param hresponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        this.service(hrequest, hresponse);
    }

    @Override
    protected void service(HttpServletRequest hrequest, HttpServletResponse hresponse)
            throws ServletException, IOException
    {
        try {
            licenseManager.requireFeature(SERVICE_HTTP_MESSAGE_INPUT);
            HttpTransportModule.requireEndpoint(hrequest, SsgConnector.Endpoint.MESSAGE_INPUT);
        } catch (LicenseException e) {
            logger.log(Level.WARNING, "Published service message input is not licensed '"+ExceptionUtils.getMessage(e)+"'.");
            hresponse.sendError(503);
            return;
        } catch (ListenerException e) {
            logger.log(Level.WARNING, "Published service message input is not enabled on this port, " + hrequest.getServerPort());
            hresponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        GZIPInputStream gis = null;
        String maybegzipencoding = hrequest.getHeader("content-encoding");
        boolean gzipEncodedTransaction = false;
        if (maybegzipencoding != null) { // case of value ?
            if (maybegzipencoding.contains("gzip")) {
                if( !serverConfig.getBooleanProperty("request.compress.gzip.allow", true) ) {
                    logger.log(Level.WARNING, "Rejecting GZIP compressed request.");
                    String soapFault = GZIP_REQUESTS_FORBIDDEN_SOAP_FAULT.replace("http://soong:8080/xml/blub",
                            hrequest.getScheme() + "://" + hrequest.getServerName() +
                            (hrequest.getServerPort() == 80 ? "" : ":" + hrequest.getServerPort()) +
                            hrequest.getRequestURI());
                    OutputStream responseStream = null;
                    try {
                        responseStream = hresponse.getOutputStream();
                        hresponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        hresponse.setContentType(DEFAULT_CONTENT_TYPE);
                        responseStream.write(soapFault.getBytes("UTF-8"));
                    } finally {
                        if(responseStream != null) responseStream.close();
                    }
                    return;
                }

                gzipEncodedTransaction = true;
                logger.fine("request with gzip content-encoding detected " + hrequest.getContentLength());
                //logger.info("Compression #2");
                try {
                    InputStream original = hrequest.getInputStream();
                    gis = new GZIPInputStream(original);
                } catch (Exception e) {
                    String exceptionMessage = ExceptionUtils.getMessage(e);
                    logger.log(Level.WARNING, "Cannot decompress the incoming request. " + exceptionMessage);
                    if (logger.isLoggable(Level.FINE)) {
                        byte[] bytes = IOUtils.slurpStream(hrequest.getInputStream());
                        logger.fine("Read this instead: " + new String(bytes));
                    }
                    if(e instanceof IOException && exceptionMessage.contains("Not in GZIP format")){
                        gzipEncodedTransaction = false; //do this for all exceptions here?
                    }
                }
            } else {
                logger.fine("content-encoding not gzip " + maybegzipencoding);
            }
        } else {
            logger.fine("no content-encoding specified");
        }

        // Initialize processing context
        final Message response = new Message();
        final Message request = new Message();

        final String rawct = hrequest.getContentType();
        ContentTypeHeader ctype = rawct != null && rawct.length() > 0
          ? ContentTypeHeader.parseValue(rawct)
          : ContentTypeHeader.XML_DEFAULT;

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setReplyExpected(true); // HTTP always expects to receive a reply
        context.setRequestWasCompressed(gzipEncodedTransaction);

        initCookies(hrequest.getCookies(), context);

        final StashManager stashManager = stashManagerFactory.createStashManager();

        AssertionStatus status = null;
        try {
            context.setAuditContext(auditContext);
            context.setSoapFaultManager(soapFaultManager);
            context.setClusterPropertyCache(clusterPropertyCache);

            if (gzipEncodedTransaction) {
                request.initialize(stashManager, ctype, gis);
            } else {
                request.initialize(stashManager, ctype, hrequest.getInputStream());
            }

            final MimeKnob mk = request.getMimeKnob();
            HttpServletRequestKnob reqKnob = new HttpServletRequestKnob(new LazyInputStreamServletRequestWrapper(hrequest, new MimeKnobInputStreamHolder(mk)));
            request.attachHttpRequestKnob(reqKnob);

            final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(hresponse);
            response.attachHttpResponseKnob(respKnob);

            // Process message
            status = messageProcessor.processMessage(context);

            // if the policy is not successful AND the stealth flag is on, drop connection
            if (status != AssertionStatus.NONE) {
                SoapFaultLevel faultLevelInfo = context.getFaultlevel();
                logger.finest("checking for potential connection drop because status is " + status.getMessage());
                if (faultLevelInfo.getLevel() == SoapFaultLevel.DROP_CONNECTION) {
                    logger.info("No policy found and global setting is to go stealth in this case. " +
                                "Instructing valve to drop connection completly." + faultLevelInfo.toString());
                    hrequest.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME,
                                          ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                    return;
                }
            }

            // we're done if the response has already been sent
            if ( hresponse.isCommitted() ) {
                // could be due to custom assertion or templated early response, etc.
                if (logger.isLoggable(Level.FINE))
                    logger.log( Level.FINE, "Response already committed, not sending response." );

                return;
            }

            // Send response headers
            propagateCookies(context, reqKnob, respKnob);
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
                    // this could also be because of an empty payload response
                    if (routeStat >= 200 && routeStat < 400) {
                        logger.fine("returning empty payload with status " + routeStat);
                        hresponse.setStatus(routeStat);
                    } else {
                        logger.fine("servlet transport returning a placeholder empty response to " +
                                    "a successful one-way message");
                        hresponse.setStatus(200);
                    }
                    hresponse.setContentType(null);
                    hresponse.setContentLength(0);
                    hresponse.getOutputStream().close();
                    return;
                }

                // Transmit the response and return
                hresponse.setStatus(routeStat);
                String[] ct = response.getHttpResponseKnob().getHeaderValues("content-type");
                if (ct == null || ct.length <= 0) {
                    hresponse.setContentType(response.getMimeKnob().getOuterContentType().getFullValue());
                }
                OutputStream responseos = hresponse.getOutputStream();
                if (gzipEncodedTransaction) {
                    //logger.info("Compression #3");
                    logger.fine("zipping response back to requester");
                    hresponse.setHeader("content-encoding", "gzip");
                    responseos = new GZIPOutputStream(responseos);
                }
                IOUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), responseos);
                responseos.close();
                logger.fine("servlet transport returned status " + routeStat +
                            ". content-type " + response.getMimeKnob().getOuterContentType().getFullValue());

            } else if (respKnob.hasChallenge()) {
                logger.fine("servlet transport returning challenge");
                respKnob.beginChallenge();
                sendChallenge(context, hrequest, hresponse);
            } else if (context.isMalformedRequest()) {
                logger.fine("Servlet transport returning 400 due to early parse failure");
                hresponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                logger.fine("servlet transport returning 500");
                returnFault(context, hrequest, hresponse, status);
            }
        } catch (Throwable e) {
            // if the policy throws AND the stealth flag is set, drop connection
            if (context.isStealthResponseMode()) {
                logger.log(Level.INFO, "Policy threw error and stealth mode is set. " +
                                       "Instructing valve to drop connection completely.",
                                       e);
                hrequest.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME,
                                      ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                return;
            }
            try {
                if (e instanceof PolicyAssertionException) {
                    if (ExceptionUtils.causedBy(e, LicenseException.class)) {
                        // Unlicensed assertion; suppress stack trace (Bug #5499)
                        logger.log(Level.SEVERE, e.getMessage());
                    } else {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                    sendExceptionFault(context, e, hrequest, hresponse, status);
                } else if (e instanceof PolicyVersionException) {
                    String msg = "Request referred to an outdated version of policy";
                    logger.log(Level.INFO, msg);
                    sendExceptionFault(context, e, hrequest, hresponse, status);
                } else if (e instanceof NoSuchPartException) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    sendExceptionFault(context, e, hrequest, hresponse, status);
                } else if (e instanceof MethodNotAllowedException) {
                    logger.warning(e.getMessage());
                    sendExceptionFault(context, e, hrequest, hresponse, status);
                } else if (e instanceof MessageProcessingSuspendedException) {
                    auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_MESSAGE_PROCESSING_SUSPENDED, e.getMessage());
                    sendExceptionFault(context, e, hrequest, hresponse, status);
                } else if (e instanceof IOException &&
                           e.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")){
                    logger.warning("Client closed connection.");
                } else if (e instanceof MessageResponseIOException) {
                    sendExceptionFault(context, e.getMessage(), hrequest, hresponse, status);
                } else if (e instanceof IOException) {
                    logger.warning("I/O error while processing message: " + e.getMessage());
                    sendExceptionFault(context, e, hrequest, hresponse, status);
                } else if (ExceptionUtils.causedBy(e, SocketTimeoutException.class)) {
                    auditor.logAndAudit(SystemMessages.SOCKET_TIMEOUT);
                    sendExceptionFault(context, e, hrequest, hresponse, status);
                } else {
                    logger.log(Level.SEVERE, ExceptionUtils.getMessage(e), e);
                    //? if (e instanceof Error) throw (Error)e;
                    sendExceptionFault(context, e, hrequest, hresponse, status);
                }
            } catch (SAXException e1) {
                throw new ServletException(e1);
            }
        } finally {
            try {
                /*
                 * 5.0 Audit Request Id
                 * need to extract the required context variables from PEC used in the audit logging
                 */
                String[] ctxVariables = auditContext.getContextVariablesUsed();
                if (ctxVariables != null && ctxVariables.length > 0) {
                    auditContext.setContextVariables(context.getVariableMap(ctxVariables, auditor));
                }
                auditContext.flush();
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected exception when flushing audit data.", e);
            }
            finally {
                context.close();
            }
        }
    }

    private void initCookies(Cookie[] cookies, PolicyEnforcementContext context) {
        if(cookies!=null) {
            for (Cookie cookie : cookies) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Adding request cookie to context; name='" + cookie.getName() + "'.");
                }
                context.addCookie(CookieUtils.fromServletCookie(cookie, false));
            }
        }
    }

    private void propagateCookies(PolicyEnforcementContext context, HttpRequestKnob reqKnob, HttpResponseKnob resKnob) {
        Set<HttpCookie> cookies = context.getCookies();
        for (HttpCookie cookie : cookies) {
            if (cookie.isNew()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Adding new cookie to response; name='" + cookie.getCookieName() + "'.");
                }
                URI url = URI.create(reqKnob.getRequestUrl());
                resKnob.addCookie(CookieUtils.ensureValidForDomainAndPath(cookie, url.getHost(), url.getPath()));
            }
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

    /**
     * the new way to return soap faults
     */
    private void returnFault(PolicyEnforcementContext context,
                             HttpServletRequest hreq, HttpServletResponse hresp, AssertionStatus status) throws IOException, SAXException {
        OutputStream responseStream = null;
        String faultXml = null;
        try {
            responseStream = hresp.getOutputStream();
            hresp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // soap faults "MUST" be sent with status 500 per Basic profile

            SoapFaultLevel faultLevelInfo = context.getFaultlevel();
            if (faultLevelInfo.isIncludePolicyDownloadURL()) {
                if (shouldSendBackPolicyUrl(context)) {
                    PublishedService pserv = context.getService();
                    if (pserv != null) {
                        String purl = makePolicyUrl(hreq, pserv.getOid());
                        hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
                    }
                }
            }
            Pair<ContentTypeHeader, String> fault = soapFaultManager.constructReturningFault(faultLevelInfo, context);
            if (fault != null && fault.right != null) {
                hresp.setContentType(fault.left.getFullValue());
                faultXml = fault.right;
                responseStream.write(faultXml.getBytes());
            }
        } finally {
            if (responseStream != null) responseStream.close();
        }

        messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
    }

    private void sendExceptionFault(PolicyEnforcementContext context, Throwable e,
                                    HttpServletRequest hreq, HttpServletResponse hresp, AssertionStatus status) throws IOException, SAXException {
        sendExceptionFault(context, soapFaultManager.constructExceptionFault(e, context), hreq, hresp, status);
    }

    private void sendExceptionFault(PolicyEnforcementContext context, String faultXml,
                                    HttpServletRequest hreq, HttpServletResponse hresp, AssertionStatus status) throws IOException, SAXException {
        OutputStream responseStream = null;
        try {
            responseStream = hresp.getOutputStream();
            if(context.getService() != null && context.getService().getSoapVersion() == SoapVersion.SOAP_1_2) {
                hresp.setContentType(SOAP_1_2_CONTENT_TYPE);
            } else {
                hresp.setContentType(DEFAULT_CONTENT_TYPE);
            }
            hresp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // soap faults "MUST" be sent with status 500 per Basic profile

            SoapFaultLevel faultLevelInfo = context.getFaultlevel();
            if (faultLevelInfo.isIncludePolicyDownloadURL()) {
                if (shouldSendBackPolicyUrl(context)) {
                    PublishedService pserv = context.getService();
                    if (pserv != null) {
                        String purl = makePolicyUrl(hreq, pserv.getOid());
                        hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
                    }
                }
            }
            responseStream.write(faultXml.getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }

        messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
    }

    /**
     * We only return policy url in certain cases because the bridge only returns incoming faults if
     * there is no such url included.
     */
    private boolean shouldSendBackPolicyUrl(PolicyEnforcementContext context) throws IOException {
        String requestorVersion = null;
        try {
            // Did client claim a policy version?
            requestorVersion = context.getRequest().getHttpRequestKnob().
                    getHeaderFirstValue(SecureSpanConstants.HttpHeaders.POLICY_VERSION);
        } catch (IllegalStateException e) {
            // Didn't get as far as adding the HTTP knob, so assume not  (Bug #3002)
        }

        // If no version was specified, return policy url only if the policy was violated
        if (requestorVersion == null || requestorVersion.length() < 1) {
            return context.isRequestPolicyViolated();
        } else { // If a policy version was specified only return policy url if it was the wrong version
            return context.isRequestClaimingWrongPolicyVersion();
        }
    }

    private void sendChallenge(PolicyEnforcementContext context,
                               HttpServletRequest hreq, HttpServletResponse hresp) throws IOException {
        ServletOutputStream sos = null;
        try {
            // the challenge http header is supposed to already been appended at that point-ah
            hresp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PublishedService pserv = context.getService();
            String purl;
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

    private static interface InputStreamHolder {
        InputStream getInputStream() throws IOException;
    }

    private static class LazyInputStreamServletRequestWrapper extends HttpServletRequestWrapper {
        private final InputStreamHolder holder;

        private LazyInputStreamServletRequestWrapper(HttpServletRequest hrequest, InputStreamHolder holder) {
            super(hrequest);
            this.holder = holder;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new DelegatingServletInputStream(holder.getInputStream());
        }
    }

    private static class MimeKnobInputStreamHolder implements InputStreamHolder {
        private final MimeKnob mk;

        private MimeKnobInputStreamHolder(MimeKnob mk) {
            this.mk = mk;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                return mk.getEntireMessageBodyAsInputStream();
            } catch (NoSuchPartException e) {
                throw (IOException)new IOException(ExceptionUtils.getMessage(e)).initCause(e);
            }
        }
    }
}