package com.l7tech.client;

import com.l7tech.common.http.*;
import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.message.HttpHeadersKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.proxy.Constants;
import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.util.*;
import com.l7tech.wsdl.WsdlUtil;
import com.l7tech.xml.SoapFaultDetail;
import com.l7tech.xml.SoapFaultDetailImpl;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.mortbay.jetty.EofException;
import org.mortbay.jetty.HttpException;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.handler.AbstractHandler;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.wsdl.WsdlConstants.ATTR_LOCATION;

/**
 * Handle an incoming HTTP request, and proxy it if it's a SOAP request we know how to deal with.
 * User: mike
 * Date: May 15, 2003
 * Time: 5:14:26 PM
 */
public class RequestHandler extends AbstractHandler {
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());

    public static final String PROP_IGNORE_PAYLOAD_NS = "com.l7tech.proxy.ignorePayloadNS";
    public static final String STYLESHEET_SUFFIX = "/ssg/wsil2xhtml.xml";

    /**
     * If set, Policy Attachment Keys generated for requests always have null for the payload namespace URI.
     * Turn this on to avoid unnecessary parsing of the request if you know all services used with the same
     * Gateway account will have unique SOAPAction headers or URIs.
     */
    private static final boolean IGNORE_PAYLOAD_NS = Boolean.valueOf( ConfigFactory.getProperty( PROP_IGNORE_PAYLOAD_NS, "false" ) );

    private SsgFinder ssgFinder;
    private MessageProcessor messageProcessor;
    private RequestInterceptor interceptor = NullRequestInterceptor.INSTANCE;

    /**
     * Client proxy HTTP handler.  Proxies all incoming SOAP calls to the given
     * server URL.
     *
     * @param ssgFinder the list of SSGs (SSG URLs and local endpoints) we support, so we can route requests appropriately.  Required.
     * @param messageProcessor the (client-side) MessageProcessor to which requests are to be submitted.  Required.
     */
    public RequestHandler(final SsgFinder ssgFinder, final MessageProcessor messageProcessor) {
        this.ssgFinder = ssgFinder;
        this.messageProcessor = messageProcessor;
    }

    // TODO what if the document includes multiple payload elements?  should probably fail if there's more than one nsuri used at once
    private PolicyAttachmentKey gatherPolicyAttachmentKey(final Request request, Document requestEnvelope, URL originalUrl) {
        String sa = request.getHeader("SOAPAction");

        QName[] names = null;

        if (!IGNORE_PAYLOAD_NS && requestEnvelope != null) {
            names = SoapUtil.getPayloadNames(requestEnvelope);
        }

        String nsUri = names == null || names.length < 1 ? null :names[0].getNamespaceURI();
        return new PolicyAttachmentKey(nsUri, sa, originalUrl.getFile());
    }

    private HttpHeaders gatherHeaders(final Request request) {
        List<GenericHttpHeader> got = new ArrayList<GenericHttpHeader>();

        //noinspection unchecked
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            //noinspection unchecked
            Enumeration<String> values = request.getHeaders(name);
            StringBuffer sb = new StringBuffer();
            boolean isFirst = true;
            while (values.hasMoreElements()) {
                String value = values.nextElement();
                if (!isFirst)
                    sb.append("; ");
                sb.append(value);
                isFirst = false;
            }
            got.add(new GenericHttpHeader(name, sb.toString()));
        }
        HttpHeader[] headers = got.toArray(new HttpHeader[got.size()]);

        return new GenericHttpHeaders(headers);
    }

    private ContentTypeHeader gatherContentTypeHeader(Request request) {
        ContentTypeHeader ctype;
        try {
            ctype = ContentTypeHeader.parseValue(request.getContentType());
        } catch (IOException e) {
            ctype = ContentTypeHeader.XML_DEFAULT;
            log.warning("Incoming message had missing or invalid outer Content-Type header; assuming " + ctype.getMainValue());
        }
        return ctype;
    }

    private URL getOriginalUrl(Request request, String endpoint) {
        try {
            int port = request.getLocalPort();
            if (!endpoint.startsWith("/"))
                endpoint = "/" + endpoint;
            return new URL("http", request.getLocalName(), port, endpoint);
        } catch (MalformedURLException e) {
            // can't happen
            log.log(Level.WARNING, "Malformed URL from client", e);
            throw new RuntimeException("Malformed URL from client", e);
        }
    }

    /*
     * Handle an HTTP request.
     */
    @Override
    public void handle(String target, HttpServletRequest servRequest, HttpServletResponse servResponse, int dispatch) throws IOException, ServletException {
        final Request httpRequest = (Request)servRequest;
        final Response httpResponse = (Response)servResponse;
        try {
            doHandle(httpRequest, httpResponse);
        } catch (HttpException e) {
            Message reply = new Message();
            reply.initialize(exceptionToFault(e,
                                              e.getStatus() == 404 ? "Client" : "Server",
                                              getOriginalUrl(httpRequest, httpRequest.getUri().toString())));
            transmitResponse(e.getStatus(), httpResponse, reply, false, null);
        } catch (Error e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
    }

    private void doHandle(final Request httpRequest, final Response httpResponse)
      throws IOException
    {
        log.info("Incoming request: " + httpRequest.getUri().getPath());

        // Find endpoint, and see if this is a WSDL request
        String endpoint = httpRequest.getUri().getPath().substring(1); // skip leading slash
        boolean isWsdl = false;
        if (endpoint.endsWith(ClientProxy.WSDL_SUFFIX)) {
            endpoint = endpoint.substring(0, endpoint.length() - ClientProxy.WSDL_SUFFIX.length());
            isWsdl = true;
        }
        if (endpoint.endsWith(ClientProxy.WSIL_SUFFIX)) {
            endpoint = endpoint.substring(0, endpoint.length() - ClientProxy.WSIL_SUFFIX.length());
            isWsdl = true;
        }

        boolean nonPost = httpRequest.getMethod().compareToIgnoreCase("POST") != 0;
        if (!isWsdl && nonPost) {
            handleNonPostMethod(httpRequest, httpResponse);
            return;
        }

        String[] endpointIO = new String[]{endpoint};
        final Ssg ssg = getDesiredSsg(endpointIO);
        endpoint = endpointIO[0]; // use translated endpoint
        log.fine("Mapped to Gateway: " + ssg);
        CurrentSslPeer.clear();

        if (isWsdl && ssg.isWsdlProxySupported()) {
            handleWsdlRequest(httpRequest, httpResponse, ssg);
            return;
        }

        if (nonPost) {
            handleNonPostMethod(httpRequest, httpResponse);
            return;
        }

        String reqUsername = null;
        String reqPassword = null;
        HttpBasicToken hbt = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && "Basic ".equalsIgnoreCase(authHeader.substring(0, 6))) {
            byte[] authStuff = HexUtils.decodeBase64(authHeader.substring(6));
            for (int i = 0; i < authStuff.length - 1; ++i) {
                if (authStuff[i] == ':') {
                    reqUsername = new String(authStuff, 0, i);
                    reqPassword = new String(authStuff, i + 1, authStuff.length - i - 1);
                }
            }
        }
        // We no longer send a challenge at this point -- instead  (Bug #2689)

        if (!ssg.isChainCredentialsFromClient() && reqUsername != null && reqPassword != null) {
            hbt = new HttpBasicToken(new PasswordAuthentication(reqUsername, reqPassword.toCharArray()));
        }

        PolicyApplicationContext context = null;
        final Message prequest = new Message();
        final Message ssgresponse = new Message();
        try {
            try {
                ContentTypeHeader outerContentType = gatherContentTypeHeader(httpRequest);

                prequest.initialize(Managers.createStashManager(),
                                    outerContentType,
                                    httpRequest.getInputStream());
                prequest.attachKnob(HttpHeadersKnob.class, new HttpHeadersKnob(gatherHeaders(httpRequest)));

                URL originalUrl = getOriginalUrl(httpRequest, endpoint);
                PolicyAttachmentKey pak = gatherPolicyAttachmentKey(httpRequest, prequest.isXml() ? prequest.getXmlKnob().getDocumentReadOnly() : null, originalUrl);

                if (hbt != null) prequest.getSecurityKnob().addSecurityToken(hbt);

                context = new PolicyApplicationContext(ssg,
                                                       prequest,
                                                       ssgresponse,
                                                       interceptor,
                                                       pak,
                                                       originalUrl);

                context.setClientSocket((Socket)httpRequest.getConnection().getEndPoint().getTransport());

                if (ssg.isChainCredentialsFromClient() && reqUsername != null && reqPassword != null)
                    context.setRequestCredentials(LoginCredentials.makeLoginCredentials(
                            new HttpBasicToken(reqUsername, reqPassword.toCharArray()), null) );
                interceptor.onFrontEndRequest(context);
            } catch (Exception e) {
                interceptor.onMessageError(e);
                log.log(Level.WARNING, "unable to parse incoming request", e);
                throw new HttpException(500, "Invalid SOAP envelope: " + e);
            }

            try {
                getServerResponse(context);
            } catch (HttpChallengeRequiredException e) {
                interceptor.onMessageError(e);
                sendChallenge(httpResponse);
                log.info("Send HTTP Basic auth challenge back to the client");
                return;
            }

            int status = 200;
            HttpResponseKnob respHttp = context.getResponse().getKnob(HttpResponseKnob.class);
            if (respHttp != null && respHttp.getStatus() > 0)
                status = respHttp.getStatus();
            if (respHttp != null)
                status = respHttp.getStatus();
            transmitResponse(status, httpResponse, context.getResponse(), ssg.isHttpHeaderPassthrough(), ssg);
        } finally {
            if (context != null)
                context.close();
        }
    }

    /*
     * Handle a GET request for the WSIL stylesheet.
     */
    private void handleStylesheet(Request request, Response response) throws IOException {
        InputStream ss = RequestHandler.class.getClassLoader().getResourceAsStream("com/l7tech/common/resources/wsil2xhtml.xml");
        if (ss == null) throw new HttpException(404);
        response.setContentType("text/xml");
        OutputStream os = response.getOutputStream();
        byte[] chunk = BufferPool.getBuffer(16384);
        try {
            int got;
            while ((got = ss.read(chunk)) > 0)
                os.write(chunk, 0, got);
            request.setHandled(true);
            response.complete();
        } catch (IOException e) {
            final String msg = "Unable to read WSIL style sheet: " + e.getMessage();
            log.log(Level.SEVERE, msg, e);
            throw new HttpException(500, msg);
        } finally {
            BufferPool.returnBuffer(chunk);
            try {
                ss.close();
            } catch (IOException e) { /* can't happen */ }
            if (os != null)
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Unable to close connection to client: " + e.getMessage(), e);
                }
        }
    }

    private void sendChallenge(Response response) throws IOException {
        response.addHeader("WWW-Authenticate", "Basic realm=\"SecureSpan Bridge\"");
        response.setStatus(401, "Unauthorized");
        response.addHeader(MimeUtil.CONTENT_TYPE, "text/html");
        response.getOutputStream().write("<title>A user name and password are required</title>A user name and password are required.".getBytes());
        response.complete();
    }

    /*
     * Send the reponse SOAPEnvelope back to the client.
     *
     * @param httpResponse    the interested client's Response
     * @param response the response we are to send them
     * @param copyHeaders  true if headers in the response should be copied over
     * @param ssg          ignored unless copyHeaders.  Otherwise, is consulted with shouldCopyHeader() for each header,
     *                     to see if this specific header should be copied over.
     */
    private void transmitResponse(int status, final Response httpResponse, Message response, boolean copyHeaders, Ssg ssg) throws IOException {
        try {
            OutputStream os = httpResponse.getOutputStream();

            httpResponse.setStatus(status);
            httpResponse.setContentType(response.getMimeKnob().getOuterContentType().getFullValue());

            if (copyHeaders) {
                HttpHeadersKnob headerKnob = response.getKnob(HttpHeadersKnob.class);
                if (headerKnob != null) {
                    HttpHeader[] headers = headerKnob.getHeaders().toArray();
                    for (HttpHeader header : headers) {
                        String name = header.getName();
                        if (ssg != null && !ssg.shouldCopyHeader(name))
                            continue;
                        httpResponse.addHeader(name, header.getFullValue());
                    }
                }
            }

            final long contentLength = response.getMimeKnob().getContentLength();
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Resposne from Gateway was too large to be processed; maximum size is " + Integer.MAX_VALUE + " bytes");
            httpResponse.setContentLength((int) contentLength);
            IOUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), os);
            httpResponse.complete();
        } catch (IOException e) {
            interceptor.onReplyError(e);
            throw e;
        } catch (NoSuchPartException e) {
            interceptor.onReplyError(e);
            throw new CausedIllegalStateException("At least one multipart parts content was lost", e);
        }
    }

    private void handleNonPostMethod(Request request, Response response) throws IOException {
        if (request.getMethod().compareToIgnoreCase("GET") != 0) {
            throw new HttpException(405);
        }

        handleGetRequest(request, response);
    }

    /*
     * Handle a GET request to this Bridge's message processing port.  We'll try to return a useful
     * HTML document including links to the WSIL proxies for each configured SSG.
     */
    private void handleGetRequest(Request request, Response response) throws IOException {
        if (request.getUri().getPath().equalsIgnoreCase(STYLESHEET_SUFFIX)) {
            handleStylesheet(request, response);
            return;
        }

        response.addHeader(MimeUtil.CONTENT_TYPE, "text/html");
        PrintStream o = new PrintStream(response.getOutputStream());
        o.println("<html><head><title>SecureSpan "+ Constants.APP_NAME+"</title></head>" +
          "<body><h2>SecureSpan "+ Constants.APP_NAME+"</h2>");
        List<Ssg> ssgs = ssgFinder.getSsgList();
        if (ssgs.isEmpty()) {
            o.println("<p>There are currently no Gateways registered with SecureSpan "+ Constants.APP_NAME+".");
        } else {
            o.println("<p>SecureSpan "+ Constants.APP_NAME+" is ready to proxy services provided by the following Gateways:</p><ul>");
            int port = request.getLocalPort();
            for (Ssg ssg : ssgs) {
                if (ssg.isWsdlProxySupported()) {
                    String wsilUrl = "http://" + InetAddressUtil.getHostForUrl(request.getLocalName()) + ":" + port + "/" +
                                     ssg.getLocalEndpoint() + ClientProxy.WSIL_SUFFIX;
                    o.println("<li><a href=\"" + wsilUrl + "\">" + ssg.getSsgAddress() + " (" + ssg.getUsername() + ")</a></li>");
                }
            }
            o.println("</ul>");
        }
        o.println("</body></html>");
        response.complete();
    }

    /*
     * Figure out which SSG the client is trying to reach.
     *
     * @param endpoint the endpoint string sent by the client (ie, "/ssg3"), with any WSDL suffix stripped.
     *                 on return, will be replaced with the modified endpoint.
     * @return the Ssg to route it to
     */
    private Ssg getDesiredSsg(String[] endpoint) throws HttpException {
        // Figure out which SSG is being invoked.
        try {
            try {
                String[] splitted = endpoint[0].split("/", 2);
                log.finest("Looking for " + splitted[0]);
                Ssg ssg = ssgFinder.getSsgByEndpoint(splitted[0]);
                endpoint[0] = endpoint[0].substring(splitted[0].length());
                return ssg;
            } catch (SsgNotFoundException e) {
                return ssgFinder.getDefaultSsg();
            }
        } catch (SsgNotFoundException e) {
            HttpException t = new HttpException(404,
              "This Client Proxy has no Gateway mapped to the endpoint " + endpoint[0]);
            interceptor.onMessageError(t);
            throw t;
        }
    }

    /*
     * Send a request to an SSG and return its response.
     *
     * @param context the request to process
     */
    private void getServerResponse(PolicyApplicationContext context)
      throws HttpChallengeRequiredException {
        log.fine("Processing message to Gateway " + context.getSsg());

        try {
            messageProcessor.processMessage(context);
            interceptor.onFrontEndReply(context);
            log.fine("Returning result");
        } catch (HttpChallengeRequiredException e) {
            log.fine("Returning challenge");
            throw e;
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);
            interceptor.onReplyError(e);
            log.info("Returning fault");
            boolean haveFault = false;
            try {
                if (context.getResponse().isXml()) {
                    SoapFaultDetail sfd = SoapFaultUtils.gatherSoapFaultDetail(context.getResponse().getXmlKnob().getDocumentReadOnly());
                    if (sfd != null) {
                        String ourMess = ExceptionUtils.getMessage(e);
                        String ssgMess = sfd.getFaultString();
                        final String actor = context.getOriginalUrl().toExternalForm();
                        sfd = new SoapFaultDetailImpl(sfd.getFaultCode(),
                                                      ourMess + "\n    Gateway error message: " + ssgMess,
                                                      sfd.getFaultDetail(),
                                                      actor);
                        String namespaceUri = context.getResponse().getXmlKnob().getDocumentReadOnly().getDocumentElement().getNamespaceURI();
                        context.getResponse().initialize(SoapFaultUtils.generateSoapFaultDocument(SoapVersion.namespaceToSoapVersion(namespaceUri), sfd, actor));
                        haveFault = true;
                    }
                }
            } catch (IOException e1) {
                // Fallthrough and generate a new fault from our own exception, ignoring the SSG response
            } catch (InvalidDocumentFormatException e1) {
                // Fallthrough and generate a new fault from our own exception, ignoring the SSG response
            } catch (SAXException e1) {
                // Fallthrough and generate a new fault from our own exception, ignoring the SSG response
            }
            if (!haveFault) {
                Message response = context.getResponse();
                response.initialize(exceptionToFault(e, null, context.getOriginalUrl()));
                // For some errors the MessageProcessor will not have set a status code which causes
                // Jetty to fail writing the response with an EOF exception.
                try {
                    response.getHttpResponseKnob().setStatus(500);
                } catch ( IllegalStateException ise ) {
                    // don't set status
                }
            }
        } finally {
            CurrentSslPeer.clear();
        }
    }

    /*
     * Convert the specified context's response message into a SOAP fault representing the specified exception.
     *
     * @param t  the exception to turn into a fault.  Must not be null.
     * @param actorUrl the URL to use as the actor for the fault.  Must not be null.
     */
    private static Document exceptionToFault(Throwable t, String faultCode, URL actorUrl) {
        try {
            return SoapFaultUtils.generateSoapFaultDocument(SoapVersion.UNKNOWN,
                                                            faultCode != null ? faultCode : "Server",
                                                            t.getMessage(),
                                                            null,
                                                            actorUrl.toExternalForm());
        } catch (IOException e1) {
            throw new RuntimeException(e1); // can't happen
        } catch (SAXException e1) {
            throw new RuntimeException(e1); // can't happen
        }
    }

    /**
     * Set the RequestInterceptor, which is called as messages come and go.
     *
     * @param requestInterceptor an interceptor which will be invoked whenever a request is received from a client,
     *                           or a response is sent back to the client, or null to remove any current interceptor.
     */
    public void setRequestInterceptor(final RequestInterceptor requestInterceptor) {
        if (requestInterceptor == null)
            clearRequestInterceptor();
        else
            interceptor = requestInterceptor;
    }

    /**
     * Turn off message interception.
     */
    public void clearRequestInterceptor() {
        setRequestInterceptor(NullRequestInterceptor.INSTANCE);
    }

    /*
     * Handle a WSDL proxying request.
     */
    private void handleWsdlRequest( final Request request,
                                    final Response response,
                                    final Ssg ssg ) throws HttpException {
        final String oidStr = request.getParameter("serviceoid");
        final Map<String,String[]> queryParameters = ParameterizedString.parseQueryString( request.getQueryString() );
        if ( oidStr != null ) {
            handleWsdlRequestForOid(request, response, ssg, oidStr, queryParameters);
            return;
        }

        handleWsdlRequestForWsil(request, response, ssg);
    }

    private void handleWsdlRequestForWsil(Request request, Response response, Ssg ssg)
      throws HttpException {
        try {
            Document wsil = WsdlProxy.obtainWsilForServices(ssg);

            // Rewrite the wsdl URLs
            int port = request.getLocalPort();
            String host = request.getLocalName();
            if (InetAddressUtil.isValidIpv6Address(host))
                host = "[" + host + "]";
            String newUrl = "http://" + InetAddressUtil.getHostForUrl(host) + ":" + port + "/" +
              ssg.getLocalEndpoint() + ClientProxy.WSDL_SUFFIX + "?serviceoid=";
            NodeList descList = wsil.getElementsByTagName("description");
            Pattern replaceService = Pattern.compile("http.*serviceoid=(-?[a-fA-F0-9]+)");
            for (int i = 0; i < descList.getLength(); ++i) {
                Node desc = descList.item(i);
                NamedNodeMap attrs = desc.getAttributes();
                Node location = attrs.getNamedItem("location");
                String origUrl = location.getNodeValue();
                String newval = replaceService.matcher(origUrl).replaceAll(newUrl + "$1");
                location.setNodeValue(newval);
            }
            response.setCharacterEncoding("utf-8");
            response.setContentType(XmlUtil.TEXT_XML);
            XmlUtil.nodeToFormattedOutputStream(wsil, response.getOutputStream());
            response.getOutputStream().close();
            response.complete();
        } catch (WsdlProxy.ServiceNotFoundException e) {
            log.log(Level.WARNING, "WSIL proxy request failed: ", e);
            throw new HttpException(404, e.getMessage());
        } catch (Exception e) {
            log.log(Level.WARNING, "WSIL proxy request failed: ", e);
            throw new HttpException(500, "WSIL proxy request failed: " + e);
        }
    }

    /*
     * Download a WSDL from the SSG and rewrite the port URL to point to our proxy endpoint for that SSG.
     */
    private void handleWsdlRequestForOid( final Request request,
                                          final Response response,
                                          final Ssg ssg,
                                          final String serviceId,
                                          final Map<String,String[]> queryParameters ) throws HttpException {
        try {
            final Document wsdlDoc = WsdlProxy.obtainWsdlForService(ssg, serviceId, queryParameters);

            // Rewrite WSDL and Schema references
            rewriteReferences( request, ssg, serviceId, wsdlDoc );

            // Rewrite the wsdl URLs
            WsdlUtil.rewriteAddressLocations(wsdlDoc, new WsdlUtil.LocationBuilder() {
                public String buildLocation(Element address) throws MalformedURLException {
                    return RequestHandler.this.buildLocation(request.getLocalName(), ssg.getLocalEndpoint(), request.getLocalPort(), serviceId, address.getAttribute(ATTR_LOCATION));
                }
            });

            response.addHeader(MimeUtil.CONTENT_TYPE, XmlUtil.TEXT_XML);
            XmlUtil.nodeToOutputStream( wsdlDoc, response.getOutputStream() );
            response.complete();
        } catch (WsdlProxy.ServiceNotFoundException e) {
            log.log(Level.WARNING, "WSDL proxy request failed", e);
            throw new HttpException(404, e.getMessage());
        } catch (EofException e) {
            log.log(Level.INFO, "WSDL proxy request failed - client closed connection.");
            throw new HttpException(500, "WSDL proxy request failed: " + e);
        } catch (Exception e) {
            log.log(Level.WARNING, "WSDL proxy request failed", e);
            throw new HttpException(500, "WSDL proxy request failed: " + e);
        }
    }

    /*
     * Rewrite WSDL and Schema references to download via the XVC
     *
     * <p>Only references that would be to the Gateway are rewritten, any
     * external references are not changed.</p>
     */
    private void rewriteReferences( final Request request,
                                    final Ssg ssg,
                                    final String oidStr,
                                    final Document wsdlDoc ) {
        final String urlPrefix = "http://" + InetAddressUtil.getHostForUrl(request.getLocalName()) + ":" + request.getLocalPort();
        final DocumentReferenceProcessor documentReferenceProcessor = new DocumentReferenceProcessor();
        documentReferenceProcessor.processDocumentReferences( wsdlDoc, new DocumentReferenceProcessor.ReferenceCustomizer() {
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                String uri = null;

                if ( documentUrl != null && referenceInfo.getReferenceUrl() != null ) {
                    try {
                        final URI base = new URI(documentUrl);
                        final URI referenceUrl = base.resolve(new URI(referenceInfo.getReferenceUrl()));
                        if ( referenceUrl.getHost().equals( ssg.getSsgAddress() ) &&
                             referenceUrl.getPort() == ssg.getSslPort() &&
                             referenceUrl.getPath().startsWith( SecureSpanConstants.WSDL_PROXY_FILE ) &&
                             referenceUrl.getQuery() != null ) {
                            uri = urlPrefix + request.getUri().getPath() + "?" + referenceUrl.getQuery();
                        }
                    } catch (Exception e) {
                        log.log( Level.WARNING, "Error rewriting WSDL url for service '"+oidStr+"'..", e );
                    }
                }

                return uri;
            }
        } );
    }

    /*
     * Build the location to use for service consumption.
     *
     * <p>If the Gateway service is published with a resolution path then that
     * is used, else a path is constructed for the service identifier.</p>
     */
    private String buildLocation( final String host,
                                  final String localEndpoint,
                                  final int port,
                                  final String serviceId,
                                  final String existingLocation ) throws MalformedURLException {
        URL newUrl = null;

        final String protocol = "http";
        try {
            if ( existingLocation != null ) {
                final URI existingUri = new URI(existingLocation);
                if ( !SecureSpanConstants.SSG_FILE.equals( existingUri.getPath() )) {
                    newUrl = new URL(protocol, host, port, "/" + localEndpoint + existingUri.getPath());
                }
            }
        } catch ( URISyntaxException e ) {
            //noinspection ThrowableResultOfMethodCallIgnored
            log.log(Level.WARNING,
                    "Error processing location '"+existingLocation+"' when rewriting WSDL references.",
                    ExceptionUtils.getDebugException( e) );
        }

        if ( newUrl == null ) {
            newUrl = new URL(protocol, host, port,  "/" + localEndpoint + "/service/" + serviceId);
        }

        return newUrl.toString();
    }
}
