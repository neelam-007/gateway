package com.l7tech.proxy;

import org.apache.axis.client.Call;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.log4j.Category;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;

import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgNotFoundException;

/**
 * Handle an incoming HTTP request, and proxy it if it's a SOAP request we know how to deal with.
 * User: mike
 * Date: May 15, 2003
 * Time: 5:14:26 PM
 * To change this template use Options | File Templates.
 */
public class RequestHandler extends AbstractHttpHandler {
    private final Category log = Category.getInstance(RequestHandler.class);
    private SsgFinder ssgFinder;
    private RequestInterceptor interceptor = NullRequestInterceptor.INSTANCE;

    /**
     * Client proxy HTTP handler.  Proxies all incoming SOAP calls to the given
     * server URL.
     *
     * @param ssgFinder the list of SSGs (SSG URLs and local endpoints) we support.
     */
    public RequestHandler(final SsgFinder ssgFinder) {
        this.ssgFinder = ssgFinder;
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
        log.info("Incoming request: " + request);

        validateRequestMethod(request);

        final Ssg ssg = getDesiredSsg(request);
        log.info("Mapped to SSG: " + ssg);

        final SOAPEnvelope requestEnvelope = getRequestEnvelope(request);

        final SOAPEnvelope responseEnvelope = getServerResponse(ssg, requestEnvelope);

        transmitResponse(response, responseEnvelope);
    }

    /**
     * Send the reponse SOAPEnvelope back to the client.
     * @param response          the interested client's HttpResponse
     * @param responseEnvelope  the SOAPEnvelope we are to send them
     * @throws IOException      if something went wrong
     */
    private void transmitResponse(final HttpResponse response, final SOAPEnvelope responseEnvelope) throws IOException {
        try {
            response.addField("Content-Type", "text/xml");
            response.commitHeader();
            response.getOutputStream().write(responseEnvelope.toString().getBytes());
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
     * @throws HttpException    if no valid SOAPEnvelope could be extracted
     */
    private SOAPEnvelope getRequestEnvelope(final HttpRequest request) throws HttpException {
        // Read the envelope
        final SOAPEnvelope requestEnvelope;
        try {
            requestEnvelope = new SOAPEnvelope(request.getInputStream());
            interceptor.onReceiveMessage(requestEnvelope);
        } catch (SAXException e) {
            final HttpException t = new HttpException(400, "Couldn't parse SOAP envelope: " + e.getMessage());
            interceptor.onMessageError(t);
            throw t;
        }
        return requestEnvelope;
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
        final String endpoint = request.getURI().getPath();
        final Ssg ssg;
        try {
            ssg = ssgFinder.getSsgByEndpoint(endpoint);
        } catch (SsgNotFoundException e) {
            final HttpException t = new HttpException(401, "This Client Proxy has no SSG mapped to the endpoint " + endpoint);
            interceptor.onMessageError(t);
            throw t;
        }
        return ssg;
    }

    /**
     * Send a request to an SSG and return its response.
     * @param ssg               the SSG to bother
     * @param requestEnvelope   the message to send it
     * @return                  the response it sends back
     * @throws HttpException    if there was Trouble
     */
    private SOAPEnvelope getServerResponse(final Ssg ssg, final SOAPEnvelope requestEnvelope) throws HttpException {
        log.info("Passing request on to " + ssg.getServerUrl());

        Call call;
        try {
            call = new Call(ssg.getServerUrl());
        } catch (MalformedURLException e) {
            final HttpException t = new HttpException(500, "Client Proxy: this SSG has an invalid server url: " +
                                                     ssg.getServerUrl());
            interceptor.onReplyError(t);
            throw t;
        }

        final SOAPEnvelope responseEnvelope;
        try {
            responseEnvelope = call.invoke(requestEnvelope);
        } catch (Exception e) {
            interceptor.onReplyError(e);
            throw new HttpException(500, "Unable to obtain response from server: " + e.getMessage());
        }

        interceptor.onReceiveReply(responseEnvelope);
        log.info("Returning result");

        return responseEnvelope;
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
