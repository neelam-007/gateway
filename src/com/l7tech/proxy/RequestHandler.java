package com.l7tech.proxy;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.proxy.datamodel.CurrentRequest;
import com.l7tech.proxy.datamodel.HttpHeaders;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.util.ClientLogger;
import com.l7tech.common.xml.Wsdl;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.wsdl.Port;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handle an incoming HTTP request, and proxy it if it's a SOAP request we know how to deal with.
 * User: mike
 * Date: May 15, 2003
 * Time: 5:14:26 PM
 */
public class RequestHandler extends AbstractHttpHandler {
    private static final ClientLogger log = ClientLogger.getInstance(RequestHandler.class);
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
    private PendingRequest gatherRequest(final HttpRequest request,
                                         Document requestEnvelope,
                                         Ssg ssg)
    {
        HttpHeaders headers = new HttpHeaders(request.getFieldNames(), new HttpHeaders.ValueProvider() {
            public String getHeaderValue(String headerName) {
                StringBuffer sb = new StringBuffer();
                Enumeration values = request.getFieldValues(headerName);
                boolean isFirst = true;
                while (values.hasMoreElements()) {
                    String value = (String) values.nextElement();
                    if (!isFirst)
                        sb.append("; ");
                    sb.append(value);
                    isFirst = false;
                }
                return sb.toString();
            }
        });
        PendingRequest pr = new PendingRequest(clientProxy,
                                               requestEnvelope,
                                               ssg,
                                               interceptor,
                                               getOriginalUrl(request),
                                               headers);
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

        // Find endpoint, and see if this is a WSDL request
        String endpoint = request.getURI().getPath().substring(1); // skip leading slash
        boolean isWsdl = false;
        if (endpoint.endsWith(ClientProxy.WSDL_SUFFIX)) {
            endpoint = endpoint.substring(0, endpoint.length() - ClientProxy.WSDL_SUFFIX.length());
            isWsdl = true;
        }
        if (endpoint.endsWith(ClientProxy.WSIL_SUFFIX)) {
            endpoint = endpoint.substring(0, endpoint.length() - ClientProxy.WSIL_SUFFIX.length());
            isWsdl = true;
        }

        final Ssg ssg = getDesiredSsg(endpoint);
        log.info("Mapped to SSG: " + ssg);
        CurrentRequest.setCurrentSsg(ssg);

        if (isWsdl) {
            handleWsdlRequest(request, response, ssg);
            return;
        }

        if (request.getMethod().compareToIgnoreCase("POST") != 0) {
            handleNonPostMethod(request, response);
            return;
        }

        PendingRequest pendingRequest;
        try {
            // TODO: PERF: this XML parsing is causing a performance bottleneck
            Document envelope = XmlUtil.parse(request.getInputStream());
            pendingRequest = gatherRequest(request, envelope, ssg);
            interceptor.onReceiveMessage(pendingRequest);
        } catch (Exception e) {
            interceptor.onMessageError(e);
            throw new HttpException(500, "Invalid SOAP envelope: " + e);
        }

        SsgResponse responseString = getServerResponse(pendingRequest);

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
     * Handle a request method other than POST.
     * @param request           the Request that isn't an HTTP POST
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
     * Handle a GET request to this Agent's message processing port.  We'll try to return a useful
     * HTML document including links to the WSIL proxies for each configured SSG.
     * @param request
     */
    private void handleGetRequest(HttpRequest request, HttpResponse response) throws IOException {
        response.addField("Content-Type", "text/html");
        PrintStream o = new PrintStream(response.getOutputStream());
        o.println("<html><head><title>SecureSpan Agent</title></head>" +
                  "<body><h2>SecureSpan Agent</h2>");
        List ssgs = ssgFinder.getSsgList();
        if (ssgs.isEmpty()) {
            o.println("<p>There are currently no Gateways registered with this Agent.");
        } else {
            o.println("<p>This Agent is ready to proxy services provided by the following Gateways:</p><ul>");
            int port = clientProxy.getBindPort();
            for (Iterator i = ssgs.iterator(); i.hasNext();) {
                Ssg ssg = (Ssg) i.next();
                String wsilUrl = "http://" + request.getHost() + ":" + port + "/" +
                    ssg.getLocalEndpoint() + ClientProxy.WSIL_SUFFIX;
                o.println("<li><a href=\"" + wsilUrl + "\">" + ssg.toString() + "</a></li>");
            }
            o.println("</ul>");
        }
        o.println("</body></html>");
        response.commit();
    }

    /**
     * Figure out which SSG the client is trying to reach.
     *
     * @param endpoint   the endpoint string sent by the client (ie, "/ssg3"), with any WSDL suffix stripped
     * @return          the Ssg to route it to
     * @throws HttpException    if there was Trouble
     */
    private Ssg getDesiredSsg(String endpoint) throws HttpException {
        // Figure out which SSG is being invoked.
        try {
            try {
                String[] splitted = endpoint.split("/", 2);
                log.info("Looking for " + splitted[0]);
                return ssgFinder.getSsgByEndpoint(splitted[0]);
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
     * @param request           the request to send it
     * @return                  the response it sends back
     * @throws HttpException    if there was Trouble
     */
    private SsgResponse getServerResponse(PendingRequest request)
            throws HttpException
    {
        log.info("Processing message to SSG " + request.getSsg());

        try {
            SsgResponse reply = messageProcessor.processMessage(request);
            interceptor.onReceiveReply(reply);
            log.info("Returning result");
            return reply;
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace(System.err);
            interceptor.onReplyError(e);
            throw new HttpException(500, e.toString());
        } finally {
            CurrentRequest.clearCurrentRequest();
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
            throws HttpException
    {
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

            response.addField("Content-Type", "text/xml");
            XmlUtil.documentToOutputStream(wsil, response.getOutputStream());
            response.commit();
            return;
        } catch (WsdlProxy.ServiceNotFoundException e) {
            log.error("WSIL proxy request failed: ", e);
            throw new HttpException(404, e.getMessage());
        } catch (Exception e) {
            log.error("WSIL proxy request failed: ", e);
            throw new HttpException(500, "WSIL proxy request failed: " + e);
        }
    }

    /** Download a WSDL from the SSG and rewrite the port URL to point to our proxy endpoint for that SSG. */
    private void handleWsdlRequestForOid(HttpRequest request, HttpResponse response, Ssg ssg, String oidStr) throws HttpException {
        try {
            long oid = Long.parseLong(oidStr);
            Wsdl wsdl = WsdlProxy.obtainWsdlForService(ssg, oid);

            // Rewrite the wsdl URL
            int port = clientProxy.getBindPort();
            URL newUrl = new URL("http", request.getHost(), port,
                                 "/" + ssg.getLocalEndpoint() + "/service/" + oid);
            Port soapPort = wsdl.getSoapPort();
            if (soapPort != null)
                wsdl.setPortUrl(soapPort, newUrl);

            response.addField("Content-Type", "text/xml");
            wsdl.toOutputStream(response.getOutputStream());
            response.commit();
            return;
        } catch (WsdlProxy.ServiceNotFoundException e) {
            log.error("WSDL proxy request failed: ", e);
            throw new HttpException(404, e.getMessage());
        } catch (Exception e) {
            log.error("WSDL proxy request failed: ", e);
            throw new HttpException(500, "WSDL proxy request failed: " + e);
        }
    }
}
