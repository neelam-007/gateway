package com.l7tech.server;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpHeaderUtil;
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
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.util.DelegatingServletInputStream;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.ServletUtils;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.xml.sax.SAXException;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.l7tech.common.http.HttpConstants.*;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_HTTP_MESSAGE_INPUT;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_HTTP_RESPONSE_STREAMING;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_HTTP_RESPONSE_STREAM_UNLIMITED;
import static com.l7tech.server.tomcat.ResponseKillerValve.ATTRIBUTE_FLAG_NAME;
import static java.util.Collections.list;

/**
 * TODO: update class name and javadoc as this class processes non soap requests e.g. XML and JSON
 * Receives SOAP requests via HTTP POST, passes them into the <code>MessageProcessor</code>
 * and formats the response as a reasonable approximation of an HTTP response.
 * <p/>
 * The name of this class has not been accurate since non-SOAP web services were added in SecureSpan version 3.0.
 */
public class SoapMessageProcessingServlet extends HttpServlet {
    private static final String DEFAULT_CONTENT_TYPE = XmlUtil.TEXT_XML + "; charset=utf-8";
    private static final Charset DEFAULT_CONTENT_ENCODING = Charsets.UTF8;
    private static final String SOAP_1_2_CONTENT_TYPE = SOAPConstants.SOAP_1_2_CONTENT_TYPE + "; charset=utf-8";
    private static final Charset SOAP_1_2_CONTENT_ENCODING = Charsets.UTF8;
    private static final String PARAM_POLICYSERVLET_URI = "PolicyServletUri";
    private static final String DEFAULT_POLICYSERVLET_URI = "/policy/disco?serviceoid=";

    private final Logger logger = Logger.getLogger(getClass().getName());

    private Config config;
    private MessageProcessor messageProcessor;
    private SoapFaultManager soapFaultManager;
    private LicenseManager licenseManager;
    private StashManagerFactory stashManagerFactory;
    private ApplicationEventPublisher messageProcessingEventChannel;
    private Auditor auditor;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        config = applicationContext.getBean("serverConfig", Config.class);
        messageProcessor = applicationContext.getBean("messageProcessor", MessageProcessor.class);
        soapFaultManager = applicationContext.getBean("soapFaultManager", SoapFaultManager.class);
        licenseManager = applicationContext.getBean("licenseManager", LicenseManager.class);
        stashManagerFactory = applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        messageProcessingEventChannel = applicationContext.getBean("messageProcessingEventChannel", EventChannel.class);
        auditor = new Auditor(this, applicationContext, logger);
    }

    /**
     * Backwards-ish entry point so that unit tests will work.
     */
    @Override
    public void doPost(HttpServletRequest hrequest, HttpServletResponse hresponse) throws ServletException, IOException {
        this.service(hrequest, hresponse);
    }

    @Override
    protected void service(HttpServletRequest hrequest, HttpServletResponse hresponse)
            throws ServletException, IOException
    {
        final SsgConnector connector;
        try {
            licenseManager.requireFeature(SERVICE_HTTP_MESSAGE_INPUT);
            connector = getConnector(hrequest);
            if (connector == null)
                throw new ListenerException("No connector was found for the specified request.");
            if (!connector.offersEndpoint(SsgConnector.Endpoint.MESSAGE_INPUT))
                throw new ListenerException("This request cannot be accepted on this port.");
        } catch (LicenseException e) {
            logger.log(Level.WARNING, "Published service message input is not licensed '"+ExceptionUtils.getMessage(e)+"'.");
            hresponse.sendError(503);
            return;
        } catch (ListenerException e) {
            logger.log(Level.WARNING, "Published service message input is not enabled on this port, " + hrequest.getServerPort());
            hresponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if ( logger.isLoggable( Level.FINE ) ) {
            logger.log( Level.FINE, "HTTP request URI [{0}]", new Object[]{ hrequest.getRequestURI() } );
            for ( final String headerName : safeList(hrequest.getHeaderNames()) ) {
                logger.log( Level.FINE, "HTTP request header [{0}]={1}", new Object[]{headerName, safeList(hrequest.getHeaders( headerName ))} );
            }
        }

        GZIPInputStream gis = null;
        String maybegzipencoding = hrequest.getHeader(HEADER_CONTENT_ENCODING);
        boolean gzipEncodedTransaction = false;
        boolean gzipResponse = false;
        final boolean allowGzipResponse = config.getBooleanProperty("response.compress.gzip.allow", true);
        if ( maybegzipencoding != null ) {
            if (maybegzipencoding.toLowerCase().contains("gzip")) {
                if( !config.getBooleanProperty("request.compress.gzip.allow", true) ) {
                    logger.log( Level.INFO, "Rejecting GZIP compressed request.");
                    rejectGzipRequest( hrequest, hresponse, STATUS_UNSUPPORTED_MEDIA_TYPE, "Rejecting GZIP compressed request" );
                    return;
                }

                gzipEncodedTransaction = true;
                gzipResponse = allowGzipResponse;
                logger.fine("request with gzip content-encoding detected " + hrequest.getContentLength());
                try {
                    final InputStream original = hrequest.getInputStream();
                    gis = new GZIPInputStream(original);
                } catch (Exception e) {
                    final String exceptionMessage = ExceptionUtils.getMessage(e);
                    logger.log(Level.INFO, "Cannot decompress the incoming request: " + exceptionMessage, ExceptionUtils.getDebugException( e ));
                    rejectGzipRequest( hrequest, hresponse, STATUS_BAD_REQUEST, "Invalid GZIP compressed request" );
                    return;
                }
            } else {
                logger.fine("content-encoding not gzip " + maybegzipencoding);
            }
        } else {
            logger.fine("no content-encoding specified");
        }

        gzipResponse = gzipResponse ||
                (allowGzipResponse && HttpHeaderUtil.acceptsGzipResponse(hrequest.getHeader( HEADER_ACCEPT_ENCODING)));

        // Initialize processing context
        final Message response = new Message();
        final Message request = new Message();

        ContentTypeHeader ctype = getRequestContentType( hrequest );

        final String overrideContentType = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        if (overrideContentType != null) {
            ctype = ContentTypeHeader.create(overrideContentType);
        }

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response,  true);
        context.setRequestWasCompressed(gzipEncodedTransaction);

        initCookies(hrequest.getCookies(), context);

        final StashManager stashManager = stashManagerFactory.createStashManager();

        AssertionStatus status;
        try {
            long maxBytes = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT,Message.getMaxBytes());

            final InputStream requestInput = gzipEncodedTransaction ? gis : hrequest.getInputStream();
            request.initialize(stashManager, ctype, requestInput, maxBytes);
            ServletUtils.loadHeaders(hrequest, request.getHeadersKnob());

            final Goid hardwiredServiceGoid = connector.getGoidProperty(EntityType.SERVICE, SsgConnector.PROP_HARDWIRED_SERVICE_ID, PersistentEntity.DEFAULT_GOID);
            if (!Goid.isDefault(hardwiredServiceGoid) ) {
                request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
            }

            final MimeKnob mk = request.getMimeKnob();
            HttpServletRequestKnob reqKnob = new HttpServletRequestKnob(new LazyInputStreamServletRequestWrapper(hrequest, new MimeKnobInputStreamHolder(mk)));
            request.attachHttpRequestKnob(reqKnob);

            final HttpServletResponseKnob respKnob = logger.isLoggable( Level.FINE ) ?
                    new HttpServletResponseKnob( new DebugHttpServletResponse( hresponse, logger ) ) :
                    new HttpServletResponseKnob(hresponse);
            response.attachHttpResponseKnob(respKnob);

            // Process message
            status = messageProcessor.processMessage(context);

            // if the policy is not successful AND the stealth flag is on, drop connection
            if (status != AssertionStatus.NONE) {
                final SoapFaultLevel faultLevelInfo = getSoapFaultLevel( context );
                logger.finest("checking for potential connection drop because status is " + status.getMessage());
                if (faultLevelInfo.getLevel() == SoapFaultLevel.DROP_CONNECTION) {
                    logger.info("No policy found and global setting is to go stealth in this case. " +
                                "Instructing valve to drop connection completly." + faultLevelInfo.toString());
                    hrequest.setAttribute( ATTRIBUTE_FLAG_NAME, ATTRIBUTE_FLAG_NAME );
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
            final Set<HttpCookie> cookies = getCookiesToPropagate(context, reqKnob);
            final HeadersKnob responseHeaders = context.getResponse().getHeadersKnob();
            respKnob.beginResponse(responseHeaders.getHeaders(), cookies);

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
                if (response.getKnob(MimeKnob.class) == null || !response.isInitialized()) {
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
                final MimeKnob responseMimeKnob = response.getMimeKnob();

                // Transmit the response and return
                hresponse.setStatus(routeStat);
                String[] ct = responseHeaders.getHeaderValues(HEADER_CONTENT_TYPE);
                if (ct == null || ct.length <= 0) {
                    final ContentTypeHeader mimeKnobCt = responseMimeKnob.getOuterContentType();
                    final String toset = mimeKnobCt == ContentTypeHeader.NONE ? null : mimeKnobCt.getFullValue();
                    hresponse.setContentType(toset);
                    if (toset == null) {
                        // Omit content length if no content type
                        hresponse.setHeader(HEADER_CONTENT_LENGTH, null);
                    }
                }
                OutputStream responseos = hresponse.getOutputStream();
                if (gzipResponse) {
                    logger.fine("zipping response back to requester");
                    hresponse.setHeader( HEADER_CONTENT_ENCODING, "gzip");
                    responseos = new GZIPOutputStream(responseos);
                }
                boolean destroyAsRead = canStreamResponse(context);
                if ( destroyAsRead && config.getBooleanProperty( PARAM_IO_HTTP_RESPONSE_STREAM_UNLIMITED, true ) ) {
                    // It is safe to clear the response size limit since we're
                    // not reading into memory. If an explicit size limit was
                    // set for the entire message then it would have been
                    // checked at that time.
                    responseMimeKnob.setContentLengthLimit( 0L );
                }
                IOUtils.copyStream(responseMimeKnob.getEntireMessageBodyAsInputStream(destroyAsRead), responseos);
                responseos.close();
                logger.fine("servlet transport returned status " + routeStat +
                            ". content-type " + responseMimeKnob.getOuterContentType().getFullValue());

            } else if (respKnob.hasChallenge()) {
                logger.fine("servlet transport returning challenge");
                respKnob.beginChallenge();
                sendChallenge(context, hrequest, hresponse);
            } else if (context.isMalformedRequest()) {
                logger.fine("Servlet transport returning 400 due to early parse failure");
                if (context.getFaultlevel() != null && context.getFaultlevel().isAlwaysReturnSoapFault())
                    returnFault(context, hrequest, hresponse);
                else
                    hresponse.sendError(HttpServletResponse.SC_BAD_REQUEST); // original behavior
            } else {
                logger.fine("servlet transport returning 500");
                returnFault(context, hrequest, hresponse);
            }
        } catch (Throwable e) {
            if (e instanceof PolicyAssertionException) {
                if (ExceptionUtils.causedBy(e, LicenseException.class)) {
                    // Unlicensed assertion; suppress stack trace (Bug #5499)
                    logger.log(Level.SEVERE, ExceptionUtils.getMessage(e));
                } else if (ExceptionUtils.causedBy(e, DecoratorException.class)) {
                    DecoratorException decoratorException =
                            ExceptionUtils.getCauseIfCausedBy(e, DecoratorException.class);
                    logger.log(Level.WARNING,
                            ExceptionUtils.getMessage(e) + ": " + ExceptionUtils.getMessage(decoratorException),
                            ExceptionUtils.getDebugException(e));
                }  else {
                    logger.log(Level.SEVERE, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            } else if (e instanceof PolicyVersionException) {
                logger.log(Level.INFO, "Request referred to an outdated version of policy");
            } else if (e instanceof MethodNotAllowedException) {
                logger.warning(ExceptionUtils.getMessage(e));
            } else if (e instanceof MessageProcessingSuspendedException) {
                logger.warning("Message processing suspended by the Audit Archiver");
                auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_MESSAGE_PROCESSING_SUSPENDED, ExceptionUtils.getMessage(e));
            } else if (e instanceof IOException &&
                       e.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")){
                logger.warning("Client closed connection.");
                return; // cannot send a response
            } else if (e instanceof MessageResponseIOException) {
                // already audited, custom fault sent below (unless stealth mode)
            } else if (e instanceof IOException) {
                logger.log(Level.WARNING, "I/O error while processing message: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } else if (ExceptionUtils.causedBy(e, SocketTimeoutException.class)) {
                auditor.logAndAudit(SystemMessages.SOCKET_TIMEOUT);
            } else {
                logger.log(Level.SEVERE, ExceptionUtils.getMessage(e), e);
            }

            // if the policy throws AND the stealth flag is set, drop connection
            final SoapFaultLevel faultLevelInfo = getSoapFaultLevel( context );
            if ( faultLevelInfo.getLevel() == SoapFaultLevel.DROP_CONNECTION ) {
                logger.log(Level.INFO, "Policy threw error and stealth mode is set. " +
                                       "Instructing valve to drop connection completely.");
                hrequest.setAttribute( ATTRIBUTE_FLAG_NAME, ATTRIBUTE_FLAG_NAME );
                return;
            }

            try {
                if ( !hresponse.isCommitted() ) { // reset the response if possible to clear headers/partial response data
                    hresponse.reset();
                } else {
                    // partial response written before error, we have to drop the connection
                    // we'll still generate the fault in case it is needed for auditing
                    hrequest.setAttribute( ATTRIBUTE_FLAG_NAME, ATTRIBUTE_FLAG_NAME );
                }
                if (e instanceof MessageResponseIOException) {
                    sendExceptionFault(context, e.getMessage(), null, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, hrequest, hresponse);
                } else {
                    sendExceptionFault(context, e, hrequest, hresponse);
                }
            } catch (SAXException e1) {
                throw new ServletException(e1);
            }
        } finally {
            context.close();
        }
    }

    /**
     * Restricted visibility connector getter - overridden in unit tests.
     */
    SsgConnector getConnector(final HttpServletRequest hrequest) {
        return HttpTransportModule.getConnector(hrequest);
    }

    void setLicenseManager(final LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    void setConfig(final Config config) {
        this.config = config;
    }

    void setStashManagerFactory(final StashManagerFactory stashManagerFactory) {
        this.stashManagerFactory = stashManagerFactory;
    }

    void setMessageProcessor(final MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    private boolean canStreamResponse(PolicyEnforcementContext context) {
        // It is OK to stream a response unless it is marked to be saved for auditing,
        // or response streaming is globally disabled,
        // or the "original main part" feature is enabled.
        return !context.isAuditSaveResponse() &&
                config.getBooleanProperty( PARAM_IO_HTTP_RESPONSE_STREAMING, true ) &&
               !config.getBooleanProperty( "audit.originalMainPart.enable", false );
    }

    private ContentTypeHeader getRequestContentType( final HttpServletRequest hrequest ) {
        final String rawct = hrequest.getContentType();
        return rawct != null && rawct.length() > 0
          ? ContentTypeHeader.create(rawct)
          : ContentTypeHeader.XML_DEFAULT;
    }

    private void rejectGzipRequest( final HttpServletRequest hrequest,
                                    final HttpServletResponse hresponse,
                                    final int statusCode,
                                    final String detail ) throws IOException {
        if ( config.getBooleanProperty( "request.compress.gzip.soapFaultRejection", true ) ) {
            final String actor = hrequest.getScheme() + "://" + InetAddressUtil.getHostForUrl( hrequest.getServerName() ) +
                    (hrequest.getServerPort() == 80 ? "" : ":" + hrequest.getServerPort()) +
                    hrequest.getRequestURI();
            final ContentTypeHeader ctype = getRequestContentType( hrequest );
            final SoapVersion version = SoapVersion.contentTypeToSoapVersion( ctype.getMainValue() );
            final SoapFaultManager.FaultResponse fault =
                    soapFaultManager.constructFault( version==SoapVersion.SOAP_1_2, actor, false, detail );
            writeFault( fault, hresponse );
        } else {
            hresponse.sendError( statusCode );
        }
    }

    private SoapFaultLevel getSoapFaultLevel( final PolicyEnforcementContext context ) {
        SoapFaultLevel faultLevelInfo = context.getFaultlevel();
        if ( faultLevelInfo==null ) faultLevelInfo = soapFaultManager.getDefaultBehaviorSettings();
        return faultLevelInfo;
    }

    private void initCookies(Cookie[] cookies, PolicyEnforcementContext context) {
        if(cookies!=null) {
            final Message request = context.getRequest();
            for (Cookie cookie : cookies) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Adding request cookie to context; name='" + cookie.getName() + "'.");
                }
                request.getHttpCookiesKnob().addCookie(CookieUtils.fromServletCookie(cookie, false));
            }
        }
    }

    private Set<HttpCookie> getCookiesToPropagate(PolicyEnforcementContext context, HttpRequestKnob reqKnob) {
        final Set<HttpCookie> cookies = new HashSet<>();
        Set<HttpCookie> knobCookies = context.getResponse().getHttpCookiesKnob().getCookies();
        for (HttpCookie cookie : knobCookies) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Adding new cookie to response; name='" + cookie.getCookieName() + "'.");
            }
            URI url = URI.create(reqKnob.getRequestUrl());
            //SSG-6881 Determine to overwrite the path using the SSG request path or the original path.
            if (cookie.isOverwritePath()) {
                cookies.add(CookieUtils.ensureValidForDomainAndPath(cookie, url.getHost(), url.getPath()));
            } else {
                cookies.add(CookieUtils.ensureValidForDomainAndPath(cookie, url.getHost(), null));
            }
        }
        return cookies;
    }

    private String makePolicyUrl(HttpServletRequest hreq, Goid goid) {
        StringBuilder policyUrl = new StringBuilder( hreq.getScheme() );
        policyUrl.append("://");
        policyUrl.append(InetAddressUtil.getHostForUrl(hreq.getServerName()));
        policyUrl.append(":");
        policyUrl.append(hreq.getServerPort());
        policyUrl.append(hreq.getContextPath());
        String policyServletUri = getServletConfig().getInitParameter(PARAM_POLICYSERVLET_URI);
        if (policyServletUri == null || policyServletUri.length() == 0)
            policyServletUri = DEFAULT_POLICYSERVLET_URI;

        policyUrl.append(policyServletUri);
        policyUrl.append(goid);

        return policyUrl.toString();
    }

    /**
     * the new way to return soap faults
     */
    private void returnFault(PolicyEnforcementContext context,
                             HttpServletRequest hreq,
                             HttpServletResponse hresp) throws IOException, SAXException {
        final SoapFaultLevel faultLevelInfo = getSoapFaultLevel( context );
        if (faultLevelInfo.isIncludePolicyDownloadURL()) {
            if (shouldSendBackPolicyUrl(context)) {
                PublishedService pserv = context.getService();
                if (pserv != null) {
                    String purl = makePolicyUrl(hreq, pserv.getGoid());
                    hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
                }
            }
        }
        final SoapFaultManager.FaultResponse fault = soapFaultManager.constructReturningFault(faultLevelInfo, context);
        final String faultXml = writeFault( fault, hresp );

        messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
    }

    private String writeFault( final SoapFaultManager.FaultResponse fault,
                               final HttpServletResponse hresp ) throws IOException {
        OutputStream responseStream = null;
        String faultXml = null;
        try {
            responseStream = hresp.getOutputStream();
            hresp.setStatus( fault.getHttpStatus() );
            if ( fault.getContentBytes() != null ) {
                faultXml = fault.getContent();
                if ( !hresp.isCommitted() ) {
                    soapFaultManager.sendExtraHeaders(fault, hresp);
                    hresp.setContentType(fault.getContentType().getFullValue());
                    responseStream.write(fault.getContentBytes());
                }
            }
        } finally {
            if (responseStream != null) responseStream.close();
        }
        return faultXml;
    }


    private void sendExceptionFault(PolicyEnforcementContext context, Throwable e,
                                    HttpServletRequest hreq, HttpServletResponse hresp) throws IOException, SAXException {
        final SoapFaultManager.FaultResponse faultInfo = soapFaultManager.constructExceptionFault( e, context.getFaultlevel(), context );

        if ( !hresp.isCommitted() ) {
            soapFaultManager.sendExtraHeaders(faultInfo, hresp);
        }

        sendExceptionFault(context,
                faultInfo.getContent(),
                faultInfo.getContentType().getEncoding(),
                faultInfo.getContentType().getFullValue(),
                faultInfo.getHttpStatus(),
                hreq,
                hresp);
    }

    private void sendExceptionFault( final PolicyEnforcementContext context,
                                     final String faultXml,
                                     Charset faultEncoding,
                                     final String contentType,
                                     final int httpStatus,
                                     final HttpServletRequest hreq,
                                     final HttpServletResponse hresp ) throws IOException, SAXException {
        OutputStream responseStream = null;
        try {
            responseStream = hresp.getOutputStream();
            if ( contentType != null ) {
                hresp.setContentType(contentType);
            } else if(context.getService() != null && context.getService().getSoapVersion() == SoapVersion.SOAP_1_2) {
                hresp.setContentType(SOAP_1_2_CONTENT_TYPE);
                faultEncoding = SOAP_1_2_CONTENT_ENCODING;
            } else {
                hresp.setContentType(DEFAULT_CONTENT_TYPE);
                faultEncoding = DEFAULT_CONTENT_ENCODING;
            }
            hresp.setStatus(httpStatus);

            final SoapFaultLevel faultLevelInfo = getSoapFaultLevel( context );
            if (faultLevelInfo.isIncludePolicyDownloadURL()) {
                if (shouldSendBackPolicyUrl(context)) {
                    PublishedService pserv = context.getService();
                    if (pserv != null) {
                        String purl = makePolicyUrl(hreq, pserv.getGoid());
                        hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
                    }
                }
            }

            if ( !faultEncoding.canEncode() ) {
                faultEncoding = Charsets.UTF8; // fallback to UTF-8 rather than failing    
            }

            if ( !hresp.isCommitted() ) {
                responseStream.write(faultXml.getBytes(faultEncoding));
            }
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
                purl = makePolicyUrl(hreq, pserv.getGoid());
                hresp.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, purl);
            }
            sos = hresp.getOutputStream();
            sos.print("Authentication Required");
        } finally {
            if (sos != null) sos.close();
        }
    }

    @SuppressWarnings({ "unchecked" })
    private Iterable<String> safeList( final Enumeration enumeration ) {
        return enumeration == null ?
                Collections.<String>emptyList() :
                list( (Enumeration<String>) enumeration );
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

    /**
     * HTTP Servlet response wrapper with debug logging for headers and status.
     */
    private static class DebugHttpServletResponse extends HttpServletResponseWrapper {
        private final Logger logger;

        private DebugHttpServletResponse( final HttpServletResponse response,
                                          final Logger logger ) {
            super( response );
            this.logger = logger;
        }

        /**
         * Don't allow response to be changed
         */
        @Override
        public void setResponse( final ServletResponse response ) {
        }

        /**
         * Don't allow response to be unwrapped
         */
        @Override
        public ServletResponse getResponse() {
            return this;
        }

        @Override
        public void setStatus( final int sc ) {
            logStatus( sc );
            super.setStatus( sc );
        }

        @Override
        public void setStatus( final int sc, final String sm ) {
            logStatus( sc );
            super.setStatus( sc, sm );
        }

        @Override
        public void addCookie( final Cookie cookie ) {
            logCookie(cookie);
            super.addCookie( cookie );
        }

        @Override
        public void addDateHeader( final String name, final long date ) {
            logAddHeader( name, Long.toString( date ) );
            super.addDateHeader( name, date );
        }

        @Override
        public void addHeader( final String name, final String value ) {
            logAddHeader( name, value );
            super.addHeader( name, value );
        }

        @Override
        public void addIntHeader( final String name, final int value ) {
            logAddHeader( name, Integer.toString( value ) );
            super.addIntHeader( name, value );
        }

        @Override
        public void setIntHeader( final String name, final int value ) {
            logSetHeader( name, Integer.toString(value) );
            super.setIntHeader( name, value );
        }

        @Override
        public void setHeader( final String name, final String value ) {
            logSetHeader( name, value );
            super.setHeader( name, value );
        }

        @Override
        public void setDateHeader( final String name, final long date ) {
            logSetHeader( name, Long.toString(date) );
            super.setDateHeader( name, date );
        }

        private void logStatus( final int statusCode ) {
            log( "HTTP response status [{0}]", statusCode );
        }

        private void logCookie( final Cookie cookie ) {
            if ( cookie != null ) {
                log( "HTTP response cookie [{0}]=[{1}]", cookie.getName(), cookie.getValue() );
            }
        }
        private void logAddHeader( final String name,
                                   final String value ) {
            log( "HTTP response header added [{0}]=[{1}]", name, value );
        }

        private void logSetHeader( final String name,
                                   final String value ) {
            log( "HTTP response header set [{0}]=[{1}]", name, value );
        }

        private void log( final String message, final Object... params ) {
            if ( logger.isLoggable( Level.FINE ) ) {
                logger.log( Level.FINE, message, params );
            }
        }
    }
}
