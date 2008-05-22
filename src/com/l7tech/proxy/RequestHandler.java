package com.l7tech.proxy;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.common.message.HttpHeadersKnob;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.security.token.http.HttpBasicToken;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.SoapFaultDetailImpl;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import org.mortbay.jetty.HttpException;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.handler.AbstractHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.wsdl.Port;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handle an incoming HTTP request, and proxy it if it's a SOAP request we know how to deal with.
 * User: mike
 * Date: May 15, 2003
 * Time: 5:14:26 PM
 */
public class RequestHandler extends AbstractHandler {
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());
    private SsgFinder ssgFinder;
    private MessageProcessor messageProcessor;
    private RequestInterceptor interceptor = NullRequestInterceptor.INSTANCE;
    public static final String STYLESHEET_SUFFIX = "/ssg/wsil2xhtml.xml";

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
        QName[] names = SoapUtil.getPayloadNames(requestEnvelope);
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
                // TODO: PERF: doing full XML parsing for every request is causing a performance bottleneck
                ContentTypeHeader outerContentType = gatherContentTypeHeader(httpRequest);

                prequest.initialize(Managers.createStashManager(),
                                    outerContentType,
                                    httpRequest.getInputStream());
                prequest.getXmlKnob(); // assert request is XML.  Will throw SAXException early, otherwise.
                prequest.attachKnob(HttpHeadersKnob.class, new HttpHeadersKnob(gatherHeaders(httpRequest)));

                URL originalUrl = getOriginalUrl(httpRequest, endpoint);
                PolicyAttachmentKey pak = gatherPolicyAttachmentKey(httpRequest, prequest.getXmlKnob().getDocumentReadOnly(), originalUrl);

                if (hbt != null) prequest.getSecurityKnob().addSecurityToken(hbt);

                context = new PolicyApplicationContext(ssg,
                                                       prequest,
                                                       ssgresponse,
                                                       interceptor,
                                                       pak,
                                                       originalUrl);

                if (ssg.isChainCredentialsFromClient() && reqUsername != null && reqPassword != null)
                    context.setRequestCredentials(new LoginCredentials(reqUsername,
                            reqPassword.toCharArray(),
                            CredentialFormat.CLEARTEXT,
                            null));
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
            HttpResponseKnob respHttp = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
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
                HttpHeadersKnob headerKnob = (HttpHeadersKnob)response.getKnob(HttpHeadersKnob.class);
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
            HexUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), os);
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
                    String wsilUrl = "http://" + request.getLocalName() + ":" + port + "/" +
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
                        context.getResponse().initialize(SoapFaultUtils.generateSoapFaultDocument(sfd, actor));
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
            if (!haveFault)
                context.getResponse().initialize(exceptionToFault(e, null, context.getOriginalUrl()));
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
            return SoapFaultUtils.generateSoapFaultDocument(faultCode != null ? faultCode : "Server",
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
    private void handleWsdlRequest(Request request, Response response, Ssg ssg) throws HttpException {
        String oidStr = request.getParameter("serviceoid");
        if (oidStr != null) {
            handleWsdlRequestForOid(request, response, ssg, oidStr);
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
            String newUrl = "http://" + request.getLocalName() + ":" + port + "/" +
              ssg.getLocalEndpoint() + ClientProxy.WSDL_SUFFIX + "?serviceoid=";
            NodeList descList = wsil.getElementsByTagName("description");
            Pattern replaceService = Pattern.compile("http.*serviceoid=(-?\\d+)");
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
    private void handleWsdlRequestForOid(Request request, Response response, Ssg ssg, String oidStr) throws HttpException {
        try {
            long oid = Long.parseLong(oidStr);
            Wsdl wsdl = WsdlProxy.obtainWsdlForService(ssg, oid);

            // Rewrite the wsdl URL
            int port = request.getLocalPort();

            Port soapPort = wsdl.getSoapPort();
            if (soapPort != null) {
                String existinglocation = wsdl.getPortUrl(soapPort);
                URL newUrl;
                if (existinglocation != null && existinglocation.lastIndexOf("/ssg/soap") == -1) {
                    newUrl = new URL(existinglocation);
                    newUrl = new URL("http", request.getLocalName(), port,
                      "/" + ssg.getLocalEndpoint() + newUrl.getPath());
                } else {
                    newUrl = new URL("http", request.getLocalName(), port,
                      "/" + ssg.getLocalEndpoint() + "/service/" + oid);
                }
                if (soapPort != null)
                    wsdl.setPortUrl(soapPort, newUrl);
            }
            response.addHeader(MimeUtil.CONTENT_TYPE, XmlUtil.TEXT_XML);
            wsdl.toOutputStream(response.getOutputStream());
            response.complete();
        } catch (WsdlProxy.ServiceNotFoundException e) {
            log.log(Level.WARNING, "WSDL proxy request failed", e);
            throw new HttpException(404, e.getMessage());
        } catch (Exception e) {
            log.log(Level.WARNING, "WSDL proxy request failed", e);
            throw new HttpException(500, "WSDL proxy request failed: " + e);
        }
    }
}
