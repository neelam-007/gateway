package com.l7tech.proxy;

import org.mortbay.http.*;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.util.MultiException;
import org.apache.log4j.Category;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPHeader;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.message.SOAPHandler;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.encoding.DeserializationContextImpl;
import org.apache.axis.AxisEngine;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.soap.SOAPException;
import java.io.IOException;

/**
 * A "test" Ssg that can be controlled programmatically.  Used to test the Client Proxy.
 * Implements a simple echo server.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 11:17:24 AM
 */
public class SsgFaker {
    private Category log = Category.getInstance(SsgFaker.class);

    private HttpServer httpServer;
    private int maxThreads = 4;
    private int minThreads = 1;
    private int localPort = 7566;
    private String localEndpoint = "/soap/ssg";
    private String ssgUrl = "http://localhost:" + localPort + "/" + localEndpoint;
    private boolean destroyed = false;

    /**
     * Create an SsgFaker with default settings.
     */
    SsgFaker() {
    }

    /**
     * Start the test SSG.
     * @return The SSG's soap URL.
     */
    public synchronized String start() {
        if (destroyed)
            throw new IllegalStateException("this SsgFaker is no more");
        httpServer = new HttpServer();
        SocketListener socketListener = new SocketListener();
        socketListener.setMaxThreads(maxThreads);
        socketListener.setMinThreads(minThreads);
        socketListener.setPort(localPort);
        HttpContext context = new HttpContext(httpServer, "/");
        HttpHandler requestHandler = new SsgFakerHandler();
        context.addHandler(requestHandler);
        httpServer.addContext(context);
        httpServer.addListener(socketListener);
        try {
            httpServer.start();
        } catch (MultiException e) {
            log.error("Unable to start up test SSG", e);
        }
        log.info("SsgFaker started; listening on " + ssgUrl);
        return ssgUrl;
    }

    /**
     * Stop the test SSG.
     */
    public synchronized void stop() {
        if (destroyed)
            throw new IllegalStateException("this SsgFaker is no more");
        try {
            log.info("SsgFaker shutting down");
            httpServer.stop(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Free resources used by the test SSG.
     */
    public synchronized void destroy() {
        if (destroyed)
            throw new IllegalStateException("this SsgFaker is no more");
        stop();
        httpServer.destroy();
        destroyed = true;
    }

    private class SsgFakerHandler extends AbstractHttpHandler {
        public void handle(String pathInContext,
                           String pathParams,
                           HttpRequest request,
                           HttpResponse response)
                throws HttpException, IOException
        {
            log.info("SsgFakerHandler: incoming request");

            SOAPEnvelope requestEnvelope;
            try {
                requestEnvelope = new SOAPEnvelope(request.getInputStream());
            } catch (SAXException e) {
                log.error(e);
                throw new HttpException(400, "Couldn't parse SOAP envelope: " + e.getMessage());
            }

            String namespace = requestEnvelope.getNamespaceURI();

            SOAPEnvelope responseEnvelope = new SOAPEnvelope();
            responseEnvelope.setHeader(new SOAPHeader(requestEnvelope.getNamespaceURI(),
                                                      localEndpoint,
                                                      requestEnvelope.getPrefix(),
                                                      new AttributesImpl(),
                                                      new DeserializationContextImpl(AxisEngine.getCurrentMessageContext(),
                                                                                     new SOAPHandler()),
                                                      requestEnvelope.getSOAPConstants()));

            try {
                responseEnvelope.setBody((SOAPBody)requestEnvelope.getBody());;
            } catch (SOAPException e) {
                log.error(e);
                throw new HttpException(400, "Internal error: " + e.getMessage());
            }

            response.addField("Content-Type", "text/xml");
            response.commitHeader();
            response.getOutputStream().write(responseEnvelope.toString().getBytes());
            response.commit();
        }
    }
}
