package com.l7tech.proxy;

import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.SsgNotFoundException;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.log4j.Category;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.xml.sax.SAXException;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.util.Iterator;
import java.security.cert.CertificateException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;

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
    private MessageProcessor messageProcessor;
    private RequestInterceptor interceptor = NullRequestInterceptor.INSTANCE;

    /**
     * Client proxy HTTP handler.  Proxies all incoming SOAP calls to the given
     * server URL.
     *
     * @param ssgFinder the list of SSGs (SSG URLs and local endpoints) we support.
     */
    public RequestHandler(final SsgFinder ssgFinder, final MessageProcessor messageProcessor) {
        this.ssgFinder = ssgFinder;
        this.messageProcessor = messageProcessor;
    }

    /**
     * Construct a PendingRequest from the given request.
     * @param request
     * @return
     */
    private PendingRequest gatherRequest(HttpRequest request,
                                         SOAPEnvelope requestEnvelope,
                                         Ssg ssg)
            throws SOAPException
    {
        PendingRequest pr = new PendingRequest(requestEnvelope, ssg);
        String sa = request.getField("SOAPAction");
        if (sa != null)
            pr.setSoapAction(sa);
        SOAPBody soapBody = requestEnvelope.getBody();
        if (soapBody != null) {
            Iterator kids = soapBody.getChildElements();
            if (kids.hasNext()) {
                SOAPElement elm = (SOAPElement) kids.next();
                Name bodyName = elm.getElementName();
                if (bodyName != null) {
                    String uri = bodyName.getURI();
                    if (uri != null)
                        pr.setUri(uri);
                }
            }
        }
        log.info("Request SOAPAction=" + pr.getSoapAction() + "   BodyURI=" + pr.getUri());
        return pr;
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

        final SOAPEnvelope requestEnvelope = getRequestEnvelope(request);

        final String responseString = getServerResponse(request, ssg, requestEnvelope);

        transmitResponse(response, responseString);
    }

    /**
     * Send the reponse SOAPEnvelope back to the client.
     * @param response          the interested client's HttpResponse
     * @param responseString  the response we are to send them
     * @throws IOException      if something went wrong
     */
    private void transmitResponse(final HttpResponse response, final String responseString) throws IOException {
        try {
            response.addField("Content-Type", "text/xml");
            response.getOutputStream().write(responseString.getBytes());
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
    private String getServerResponse(final HttpRequest request,
                                           final Ssg ssg,
                                           final SOAPEnvelope requestEnvelope)
            throws HttpException
    {
        log.info("Processing message to SSG " + ssg.getName());

        try {
            PendingRequest pendingRequest = gatherRequest(request, requestEnvelope, ssg);
            String reply = messageProcessor.processMessage(pendingRequest);
            interceptor.onReceiveReply(reply);
            log.info("Returning result");
            return reply;
        } catch (IOException e) {
            interceptor.onReplyError(e);
            throw new HttpException(500, "Unable to obtain response from server: " + e.toString());
        } catch (PolicyAssertionException e) {
            interceptor.onReplyError(e);
            throw new HttpException(500, "Unable to obtain response from server: " + e.toString());
        } catch (SOAPException e) {
            interceptor.onMessageError(e);
            throw new HttpException(500, "Unable to find request body: " + e.toString());
        } catch (ConfigurationException e) {
            interceptor.onMessageError(e);
            throw new HttpException(500, "Invalid SSG configuration: " + e.toString());
        } catch (SAXException e) {
            interceptor.onMessageError(e);
            throw new HttpException(500, "The server's response was not a valid SOAP envelope: " + e.toString());
        } catch (CertificateException e) {
            interceptor.onReplyError(e);
            throw new HttpException(500, "The SSG provided an invalid security certificate: " + e.toString());
        } catch (NoSuchAlgorithmException e) {
            interceptor.onReplyError(e);
            throw new HttpException(500, "Internal error: " + e.toString());
        } catch (KeyStoreException e) {
            interceptor.onReplyError(e);
            throw new HttpException(500, "Unable to save the SSG's security certificate: " + e.toString());
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
