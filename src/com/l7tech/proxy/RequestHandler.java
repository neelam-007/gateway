package com.l7tech.proxy;

import org.apache.axis.client.Call;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Handle an incoming HTTP request, and proxy it if it's a SOAP request we know how to deal with.
 * User: mike
 * Date: May 15, 2003
 * Time: 5:14:26 PM
 * To change this template use Options | File Templates.
 */
public class RequestHandler extends AbstractHttpHandler {
    private String serverUrl;

    private static Logger logger  = Logger.getLogger(RequestHandler.class);
    private RequestInterceptor interceptor = NullRequestInterceptor.INSTANCE;

    /**
     * Client proxy HTTP handler.  Proxies all incoming SOAP calls to the given
     * server URL.
     *
     * @param serverUrl
     */
    public RequestHandler(String serverUrl) {
        this.serverUrl = serverUrl;
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
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
            throws HttpException, IOException
    {
        logger.info("Incoming request");

        // Only POST allowed
        if (request.getMethod().compareToIgnoreCase("POST") != 0)
            throw new HttpException(405);  // "Method not allowed"

        // Read the envelope
        SOAPEnvelope requestEnvelope;
        try {
            requestEnvelope = new SOAPEnvelope(request.getInputStream());
            interceptor.onReceiveMessage(requestEnvelope);
        } catch (SAXException e) {
            throw new HttpException(400, "Couldn't parse SOAP envelope: " + e.getMessage());
        }

        // Pass the request on to the proxy server
        logger.info("Passing request on to " + serverUrl);
        Call call = new Call(serverUrl);
        SOAPEnvelope responseEnvelope = call.invoke(requestEnvelope);
        interceptor.onReceiveReply(responseEnvelope);
        logger.info("Returning result");

        response.addField("Content-Type", "text/xml");

        response.commitHeader();
        response.getOutputStream().write(responseEnvelope.toString().getBytes());
        response.commit();
    }

    /**
     * Set the RequestInterceptor, which is called as messages come and go.
     * @param requestInterceptor
     */
    public void setRequestInterceptor(RequestInterceptor requestInterceptor) {
        interceptor = requestInterceptor;
    }

    /** Turn off message interception. */
    public void clearRequestInterceptor() {
        setRequestInterceptor(NullRequestInterceptor.INSTANCE);
    }
}
