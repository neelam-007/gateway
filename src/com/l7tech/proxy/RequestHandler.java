package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.apache.log4j.Category;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Handle an incoming HTTP request, and proxy it if it's a SOAP request we know how to deal with.
 * User: mike
 * Date: May 15, 2003
 * Time: 5:14:26 PM
 * To change this template use Options | File Templates.
 */
public class RequestHandler extends AbstractHttpHandler {
    private final Category log = Category.getInstance(RequestHandler.class);
    private ClientProxy clientProxy;
    private SsgFinder ssgFinder;
    private MessageProcessor messageProcessor;
    private RequestInterceptor interceptor = NullRequestInterceptor.INSTANCE;
    private int bindPort;

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

    /**
     * Construct a PendingRequest from the given request.
     * @param request
     * @return
     */
    private PendingRequest gatherRequest(HttpRequest request,
                                         Document requestEnvelope,
                                         Ssg ssg)
    {
        PendingRequest pr = new PendingRequest(clientProxy,
                                               requestEnvelope,
                                               ssg,
                                               interceptor,
                                               getOriginalUrl(request));
        String sa = request.getField("SOAPAction");
        if (sa != null)
            pr.setSoapAction(sa);

        pr.setUri(SoapUtil.getNamespaceUri(requestEnvelope));
        log.info("Request SOAPAction=" + pr.getSoapAction() + "   BodyURI=" + pr.getUri());
        return pr;
    }

    private URL getOriginalUrl(HttpRequest request) {
        try {
            int port = request.getPort();
            if (port == 0)
                port = bindPort;
            return new URL("http", request.getHost(), port, request.getURI().toString());
        } catch (MalformedURLException e) {
            // can't happen
            log.error("Malformed URL from client", e);
            throw new RuntimeException("Malformed URL from client", e);
        }
    }

    /**
     * Handle an HTTP request.
     *
     * @param pathInContext
     * @param pathParams
     * @param request
     * @param response
     * @throws HttpException
     * @throws IOException
     */
    public void handle(final String pathInContext,
                       final String pathParams,
                       final HttpRequest request,
                       final HttpResponse response)
            throws HttpException, IOException
    {
        log.info("Incoming request: " + request.getURI().getPath());

        validateRequestMethod(request);

        final Ssg ssg = getDesiredSsg(request);
        log.info("Mapped to SSG: " + ssg);

        final Document requestEnvelope;
        try {
            requestEnvelope = getRequestEnvelope(request);
        } catch (Exception e) {
            throw new HttpException(500, "Invalid SOAP envelope: " + e);
        }

        SsgResponse responseString = getServerResponse(request, ssg, requestEnvelope);

        transmitResponse(response, responseString);
    }

    /**
     * Send the reponse SOAPEnvelope back to the client.
     * @param response          the interested client's HttpResponse
     * @param ssgResponse  the response we are to send them
     * @throws IOException      if something went wrong
     */
    private void transmitResponse(final HttpResponse response, SsgResponse ssgResponse) throws IOException {
        try {
            response.addField("Content-Type", "text/xml");
            response.getOutputStream().write(ssgResponse.getResponseAsString().getBytes());
            response.commit();
        } catch (IOException e) {
            interceptor.onReplyError(e);
            throw e;
        }
    }

    /**
     * Extract the SOAPEnvelope from this request.
     * @param request   the Request to look at
     * @return          the SOAPEnvelope it contained
     */
    private Document getRequestEnvelope(final HttpRequest request) throws IOException, SAXException {
        try {
            Document doc = XmlUtil.parse(request.getInputStream());
            interceptor.onReceiveMessage(doc);
            return doc;
        } catch (RuntimeException e) {
            interceptor.onMessageError(e);
            throw e;
        } catch (IOException e) {
            interceptor.onMessageError(e);
            throw e;
        } catch (SAXException e) {
            interceptor.onMessageError(e);
            throw e;
        }
    }

    /**
     * Make sure this is a Post request.
     * @param request           the Request that should be an HTTP POST
     * @throws HttpException    thrown if it isn't
     */
    private void validateRequestMethod(final HttpRequest request) throws HttpException {
        if (request.getMethod().compareToIgnoreCase("POST") != 0) {
            final HttpException t = new HttpException(405); // "Method not allowed"
            interceptor.onMessageError(t);
            throw t;
        }
    }

    /**
     * Figure out which SSG the client is trying to reach.
     * @param request   the request sent by the client
     * @return          the Ssg to route it to
     * @throws HttpException    if there was Trouble
     */
    private Ssg getDesiredSsg(final HttpRequest request) throws HttpException {
        // Figure out which SSG is being invoked.
        final String endpoint = request.getURI().getPath().substring(1); // skip leading slash
        try {
            try {
                return ssgFinder.getSsgByEndpoint(endpoint);
            } catch (SsgNotFoundException e) {
                return ssgFinder.getDefaultSsg();
            }
        } catch (SsgNotFoundException e) {
            HttpException t = new HttpException(404,
                                                "This Client Proxy has no SSG mapped to the endpoint " + endpoint);
            interceptor.onMessageError(t);
            throw t;
        }
    }

    /**
     * Send a request to an SSG and return its response.
     * @param ssg               the SSG to bother
     * @param requestEnvelope   the message to send it
     * @return                  the response it sends back
     * @throws HttpException    if there was Trouble
     */
    private SsgResponse getServerResponse(final HttpRequest request,
                                          final Ssg ssg,
                                          final Document requestEnvelope)
            throws HttpException
    {
        log.info("Processing message to SSG " + ssg);

        try {
            PendingRequest pendingRequest = gatherRequest(request, requestEnvelope, ssg);
            SsgResponse reply = messageProcessor.processMessage(pendingRequest);
            interceptor.onReceiveReply(reply);
            log.info("Returning result");
            return reply;
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace(System.err);
            interceptor.onReplyError(e);
            throw new HttpException(500, e.toString());
        }
    }

    /**
     * Set the RequestInterceptor, which is called as messages come and go.
     * @param requestInterceptor
     */
    public void setRequestInterceptor(final RequestInterceptor requestInterceptor) {
        interceptor = requestInterceptor;
    }

    /** Turn off message interception. */
    public void clearRequestInterceptor() {
        setRequestInterceptor(NullRequestInterceptor.INSTANCE);
    }
}
