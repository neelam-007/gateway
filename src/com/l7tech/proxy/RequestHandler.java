package com.l7tech.proxy;

import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import com.l7tech.proxy.message.HttpHeadersKnob;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.wsdl.Port;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
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
public class RequestHandler extends AbstractHttpHandler {
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());
    private ClientProxy clientProxy;
    private SsgFinder ssgFinder;
    private MessageProcessor messageProcessor;
    private RequestInterceptor interceptor = NullRequestInterceptor.INSTANCE;
    private int bindPort;
    public static final String STYLESHEET_SUFFIX = "/ssg/wsil2xhtml.xml";

    /**
     * Client proxy HTTP handler.  Proxies all incoming SOAP calls to the given
     * server URL.
     *
     * @param ssgFinder the list of SSGs (SSG URLs and local endpoints) we support.
     */
    public RequestHandler(ClientProxy clientProxy, final SsgFinder ssgFinder, final MessageProcessor messageProcessor, int bindPort) {
        this.clientProxy = clientProxy;
        this.ssgFinder = ssgFinder;
        this.messageProcessor = messageProcessor;
        this.bindPort = bindPort;
    }

    private PolicyAttachmentKey gatherPolicyAttachmentKey(final HttpRequest request, Document requestEnvelope, URL originalUrl) {
        String sa = request.getField("SOAPAction");
        String namespaceUri = SoapUtil.getPayloadNamespaceUri(requestEnvelope);
        PolicyAttachmentKey pak = new PolicyAttachmentKey(namespaceUri, sa, originalUrl.getFile());
        return pak;
    }

    private HttpHeaders gatherHeaders(final HttpRequest request) {
        HttpHeaders headers = new HttpHeaders(request.getFieldNames(), new HttpHeaders.ValueProvider() {
            public String getHeaderValue(String headerName) {
                StringBuffer sb = new StringBuffer();
                Enumeration values = request.getFieldValues(headerName);
                boolean isFirst = true;
                while (values.hasMoreElements()) {
                    String value = (String)values.nextElement();
                    if (!isFirst)
                        sb.append("; ");
                    sb.append(value);
                    isFirst = false;
                }
                return sb.toString();
            }
        });
        return headers;
    }

    private ContentTypeHeader gatherContentTypeHeader(HttpRequest request) {
        ContentTypeHeader ctype;
        try {
            ctype = ContentTypeHeader.parseValue(request.getContentType());
        } catch (IOException e) {
            ctype = ContentTypeHeader.XML_DEFAULT;
            log.warning("Incoming message had missing or invalid outer Content-Type header; assuming " + ctype.getValue());
        }
        return ctype;
    }

    private URL getOriginalUrl(HttpRequest request, String endpoint) {
        try {
            int port = request.getPort();
            if (port == 0)
                port = bindPort;
            if (!endpoint.startsWith("/"))
                endpoint = "/" + endpoint;
            return new URL("http", request.getHost(), port, endpoint);
        } catch (MalformedURLException e) {
            // can't happen
            log.log(Level.WARNING, "Malformed URL from client", e);
            throw new RuntimeException("Malformed URL from client", e);
        }
    }

    /**
     * Handle an HTTP request.
     *
     * @param pathInContext
     * @param pathParams
     * @param httpRequest
     * @param httpResponse
     */
    public void handle(final String pathInContext,
                       final String pathParams,
                       final HttpRequest httpRequest,
                       final HttpResponse httpResponse)
      throws IOException
    {
        try {
            doHandle(httpRequest, httpResponse);
        } catch (HttpException e) {
            Message reply = new Message();
            reply.initialize(exceptionToFault(e,
                                              e.getCode() == 404 ? "Client" : "Server",
                                              getOriginalUrl(httpRequest, httpRequest.getURI().toString())));
            transmitResponse(e.getCode(), httpResponse, reply);
        } catch (Error e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
    }

    private void doHandle(final HttpRequest httpRequest, final HttpResponse httpResponse)
      throws HttpException, IOException
    {
        log.info("Incoming request: " + httpRequest.getURI().getPath());

        // Find endpoint, and see if this is a WSDL request
        String endpoint = httpRequest.getURI().getPath().substring(1); // skip leading slash
        boolean isWsdl = false;
        if (endpoint.endsWith(ClientProxy.WSDL_SUFFIX)) {
            endpoint = endpoint.substring(0, endpoint.length() - ClientProxy.WSDL_SUFFIX.length());
            isWsdl = true;
        }
        if (endpoint.endsWith(ClientProxy.WSIL_SUFFIX)) {
            endpoint = endpoint.substring(0, endpoint.length() - ClientProxy.WSIL_SUFFIX.length());
            isWsdl = true;
        }

        String[] endpointIO = new String[]{endpoint};
        final Ssg ssg = getDesiredSsg(endpointIO);
        endpoint = endpointIO[0]; // use translated endpoint
        log.fine("Mapped to Gateway: " + ssg);
        CurrentSslPeer.clear();

        if (isWsdl) {
            handleWsdlRequest(httpRequest, httpResponse, ssg);
            return;
        }

        if (httpRequest.getMethod().compareToIgnoreCase("POST") != 0) {
            handleNonPostMethod(httpRequest, httpResponse);
            return;
        }

        String reqUsername = null;
        String reqPassword = null;
        if (ssg.isChainCredentialsFromClient()) {
            String authHeader = httpRequest.getField("Authorization");
            if (authHeader != null && "Basic ".equalsIgnoreCase(authHeader.substring(0, 6))) {
                byte[] authStuff = HexUtils.decodeBase64(authHeader.substring(6));
                for (int i = 0; i < authStuff.length - 1; ++i) {
                    if (authStuff[i] == ':') {
                        reqUsername = new String(authStuff, 0, i);
                        reqPassword = new String(authStuff, i + 1, authStuff.length - i - 1);
                    }
                }
            } else {
                sendChallenge(httpResponse);
                log.info("Send HTTP Basic auth challenge back to the client");
                return;
            }
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

                context = new PolicyApplicationContext(ssg,
                                                       prequest,
                                                       ssgresponse,
                                                       interceptor,
                                                       pak,
                                                       originalUrl);

                if (ssg.isChainCredentialsFromClient())
                    context.setCredentials(new LoginCredentials(reqUsername,
                                                                reqPassword.toCharArray(),
                                                                CredentialFormat.CLEARTEXT,
                                                                null));
                interceptor.onReceiveMessage(context);
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
            }

            int status = 200;
            HttpResponseKnob respHttp = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
            if (respHttp != null && respHttp.getStatus() > 0)
                status = respHttp.getStatus();
            if (respHttp != null)
                status = respHttp.getStatus();
            transmitResponse(status, httpResponse, context.getResponse());
        } finally {
            if (context != null)
                context.close();
        }
    }

    /**
     * Handle a GET request for the WSIL stylesheet.
     */
    private void handleStylesheet(HttpRequest request, HttpResponse response) throws HttpException {
        InputStream ss = getClass().getClassLoader().getResourceAsStream("com/l7tech/common/resources/wsil2xhtml.xml");
        if (ss == null) throw new HttpException(404);
        response.setContentType("text/xml");
        OutputStream os = response.getOutputStream();
        byte[] chunk = new byte[8192];
        try {
            int got;
            while ((got = ss.read(chunk)) > 0)
                os.write(chunk, 0, got);
            request.setHandled(true);
            response.commit();
            return;
        } catch (IOException e) {
            final String msg = "Unable to read WSIL style sheet: " + e.getMessage();
            log.log(Level.SEVERE, msg, e);
            throw new HttpException(500, msg);
        } finally {
            if (ss != null) try { ss.close(); } catch (IOException e) { /* can't happen */ }
            if (os != null)
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Unable to close connection to client: " + e.getMessage(), e);
                }
        }
    }

    private void sendChallenge(HttpResponse response) throws IOException {
        response.addField("WWW-Authenticate", "Basic realm=\"SecureSpan Bridge\"");
        response.setReason("Unauthorized");
        response.addField(MimeUtil.CONTENT_TYPE, "text/html");
        response.setStatus(401);
        response.getOutputStream().write("<title>A user name and password are required</title>A user name and password are required.".getBytes());
        response.commit();
    }

    /**
     * Send the reponse SOAPEnvelope back to the client.
     *
     * @param httpResponse    the interested client's HttpResponse
     * @param response the response we are to send them
     * @throws IOException if something went wrong
     */
    private void transmitResponse(int status, final HttpResponse httpResponse, Message response) throws IOException {
        try {
            OutputStream os = httpResponse.getOutputStream();

            httpResponse.setStatus(status);
            httpResponse.setContentType(response.getMimeKnob().getOuterContentType().getFullValue());
            final long contentLength = response.getMimeKnob().getContentLength();
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Resposne from Gateway was too large to be processed; maximum size is " + Integer.MAX_VALUE + " bytes");
            httpResponse.setContentLength((int) contentLength);
            HexUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), os);
            httpResponse.commit();
        } catch (IOException e) {
            interceptor.onReplyError(e);
            throw e;
        } catch (NoSuchPartException e) {
            interceptor.onReplyError(e);
            throw new CausedIllegalStateException("At least one multipart parts content was lost", e);
        }
    }

    /**
     * Handle a request method other than POST.
     *
     * @param request the Request that isn't an HTTP POST
     * @throws HttpException if the request wasn't a GET either
     */
    private void handleNonPostMethod(HttpRequest request, HttpResponse response) throws IOException {
        if (request.getMethod().compareToIgnoreCase("GET") != 0) {
            final HttpException t = new HttpException(405); // "Method not allowed"
            throw t;
        }

        handleGetRequest(request, response);
    }

    /**
     * Handle a GET request to this Bridge's message processing port.  We'll try to return a useful
     * HTML document including links to the WSIL proxies for each configured SSG.
     *
     * @param request
     */
    private void handleGetRequest(HttpRequest request, HttpResponse response) throws IOException {
        if (request.getURI().getPath().equalsIgnoreCase(STYLESHEET_SUFFIX)) {
            handleStylesheet(request, response);
            return;
        }

        response.addField(MimeUtil.CONTENT_TYPE, "text/html");
        PrintStream o = new PrintStream(response.getOutputStream());
        o.println("<html><head><title>SecureSpan Bridge</title></head>" +
          "<body><h2>SecureSpan Bridge</h2>");
        List ssgs = ssgFinder.getSsgList();
        if (ssgs.isEmpty()) {
            o.println("<p>There are currently no Gateways registered with SecureSpan Bridge.");
        } else {
            o.println("<p>SecureSpan Bridge is ready to proxy services provided by the following Gateways:</p><ul>");
            int port = clientProxy.getBindPort();
            for (Iterator i = ssgs.iterator(); i.hasNext();) {
                Ssg ssg = (Ssg)i.next();
                String wsilUrl = "http://" + request.getHost() + ":" + port + "/" +
                  ssg.getLocalEndpoint() + ClientProxy.WSIL_SUFFIX;
                o.println("<li><a href=\"" + wsilUrl + "\">" + ssg.getSsgAddress() + " (" + ssg.getUsername() + ")</a></li>");
            }
            o.println("</ul>");
        }
        o.println("</body></html>");
        response.commit();
    }

    /**
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

    /**
     * Send a request to an SSG and return its response.
     *
     * @param context the request to process
     */
    private void getServerResponse(PolicyApplicationContext context)
      throws HttpChallengeRequiredException {
        log.fine("Processing message to Gateway " + context.getSsg());

        try {
            messageProcessor.processMessage(context);
            interceptor.onReceiveReply(context);
            log.fine("Returning result");
            return;
        } catch (HttpChallengeRequiredException e) {
            log.fine("Returning challenge");
            throw e;
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);
            interceptor.onReplyError(e);
            log.info("Returning fault");
            context.getResponse().initialize(exceptionToFault(e, null, context.getOriginalUrl()));
        } finally {
            CurrentSslPeer.clear();
        }
    }

    /**
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
     * @param requestInterceptor
     */
    public void setRequestInterceptor(final RequestInterceptor requestInterceptor) {
        interceptor = requestInterceptor;
    }

    /**
     * Turn off message interception.
     */
    public void clearRequestInterceptor() {
        setRequestInterceptor(NullRequestInterceptor.INSTANCE);
    }

    /**
     * Handle a WSDL proxying request.
     *
     * @param request
     * @param response
     */
    private void handleWsdlRequest(HttpRequest request, HttpResponse response, Ssg ssg) throws HttpException {
        String oidStr = request.getParameter("serviceoid");
        if (oidStr != null) {
            handleWsdlRequestForOid(request, response, ssg, oidStr);
            return;
        }

        handleWsdlRequestForWsil(request, response, ssg);
    }

    private void handleWsdlRequestForWsil(HttpRequest request, HttpResponse response, Ssg ssg)
      throws HttpException {
        try {
            Document wsil = WsdlProxy.obtainWsilForServices(ssg);

            // Rewrite the wsdl URLs
            int port = clientProxy.getBindPort();
            String newUrl = "http://" + request.getHost() + ":" + port + "/" +
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
            //response.addField(XmlUtil.CONTENT_TYPE, XmlUtil.TEXT_XML + "; charset=utf-8");
            response.setCharacterEncoding("utf-8", true);
            response.setContentType(XmlUtil.TEXT_XML);
            XmlUtil.nodeToFormattedOutputStream(wsil, response.getOutputStream());
            response.getOutputStream().close();
            response.commit();
            return;
        } catch (WsdlProxy.ServiceNotFoundException e) {
            log.log(Level.WARNING, "WSIL proxy request failed: ", e);
            throw new HttpException(404, e.getMessage());
        } catch (Exception e) {
            log.log(Level.WARNING, "WSIL proxy request failed: ", e);
            throw new HttpException(500, "WSIL proxy request failed: " + e);
        }
    }

    /**
     * Download a WSDL from the SSG and rewrite the port URL to point to our proxy endpoint for that SSG.
     */
    private void handleWsdlRequestForOid(HttpRequest request, HttpResponse response, Ssg ssg, String oidStr) throws HttpException {
        try {
            long oid = Long.parseLong(oidStr);
            Wsdl wsdl = WsdlProxy.obtainWsdlForService(ssg, oid);

            // Rewrite the wsdl URL
            int port = clientProxy.getBindPort();

            Port soapPort = wsdl.getSoapPort();
            if (soapPort != null) {
                String existinglocation = wsdl.getPortUrl(soapPort);
                URL newUrl = null;
                if (existinglocation != null && existinglocation.lastIndexOf("/ssg/soap") == -1) {
                    newUrl = new URL(existinglocation);
                    newUrl = new URL("http", request.getHost(), port,
                      "/" + ssg.getLocalEndpoint() + newUrl.getPath());
                } else {
                    newUrl = new URL("http", request.getHost(), port,
                      "/" + ssg.getLocalEndpoint() + "/service/" + oid);
                }
                if (soapPort != null)
                    wsdl.setPortUrl(soapPort, newUrl);
            }
            response.addField(MimeUtil.CONTENT_TYPE, XmlUtil.TEXT_XML);
            wsdl.toOutputStream(response.getOutputStream());
            response.commit();
            return;
        } catch (WsdlProxy.ServiceNotFoundException e) {
            log.log(Level.WARNING, "WSDL proxy request failed", e);
            throw new HttpException(404, e.getMessage());
        } catch (Exception e) {
            log.log(Level.WARNING, "WSDL proxy request failed", e);
            throw new HttpException(500, "WSDL proxy request failed: " + e);
        }
    }
}
