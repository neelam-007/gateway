package com.l7tech.proxy;

import org.apache.axis.AxisEngine;
import org.apache.axis.encoding.DeserializationContextImpl;
import org.apache.axis.encoding.Base64;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPHandler;
import org.apache.axis.message.SOAPHeader;
import org.apache.log4j.Category;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.util.MultiException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.util.Enumeration;

import com.l7tech.common.util.HexUtils;

/**
 * A "test" Ssg that can be controlled programmatically.  Used to test the Client Proxy.
 * Implements a simple echo server.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 11:17:24 AM
 */
public class SsgFaker {
    private static final Category log = Category.getInstance(SsgFaker.class);

    private HttpServer httpServer;
    private int maxThreads = 4;
    private int minThreads = 1;
    private int localPort = 7566;
    private int sslPort = 443;
    private String ssgUrl = "http://localhost:" + localPort;
    private String sslUrl = "https://localhost:" + sslPort;
    private boolean destroyed = false;
    //private static final String KEYSTORE = "com/l7tech/proxy/resources/ssgfaker-sv.keystore";

    /**
     * Create an SsgFaker with default settings.
     */
    SsgFaker() {
    }

    /**
     * Start the test SSG.
     * @return The SSG's soap URL.
     */
    public synchronized String start() throws IOException {
        if (destroyed)
            throw new IllegalStateException("this SsgFaker is no more");

        httpServer = new HttpServer();

        HttpContext context = new HttpContext(httpServer, "/");
        HttpHandler requestHandler = new SsgFakerHandler();
        context.addHandler(requestHandler);
        httpServer.addContext(context);

        //URL ksUrl = getClass().getClassLoader().getResource(KEYSTORE);
        //if (ksUrl == null)
        //    throw new FileNotFoundException("Can't find keystore file: " + KEYSTORE);

        // Set up HTTP listener
        SocketListener socketListener = new SocketListener();
        socketListener.setMaxThreads(maxThreads);
        socketListener.setMinThreads(minThreads);
        socketListener.setPort(localPort);
        httpServer.addListener(socketListener);

        // Set up SSL listener
        //SunJsseListener sslListener = new SunJsseListener();
        //sslListener.setMaxThreads(maxThreads);
        //sslListener.setMinThreads(minThreads);
        //sslListener.setPort(sslPort);
        //sslListener.setKeystore(ksUrl.getPath());
        //sslListener.setKeyPassword("password");
        //sslListener.setPassword("password");
        //httpServer.addListener(sslListener);

        try {
            httpServer.start();
        } catch (MultiException e) {
            log.error("Unable to start up test SSG", e);
            throw new IOException("Failed to start the SSG: " + e.toString());
        }
        log.info("SsgFaker started; listening for http connections on " + ssgUrl);
        log.info("SsgFaker listeneing for https connections on " + sslUrl);
        return ssgUrl;
    }

    public String getSsgUrl() {
        return ssgUrl;
    }

    public String getSslUrl() {
        return sslUrl;
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
            log.info("SsgFakerHandler: incoming request: pathInContext=" + pathInContext);

            //log.info("Got request: " + new String(HexUtils.slurpStream(request.getInputStream(), 16384)));

            SOAPEnvelope requestEnvelope;
            try {
                requestEnvelope = new SOAPEnvelope(request.getInputStream());
            } catch (SAXException e) {
                log.error(e);
                throw new HttpException(400, "Couldn't parse SOAP envelope: " + e.getMessage());
            }

            boolean isBasicAuth = false;
            String authHeader = request.getField("Authorization");
            if (authHeader != null && "Basic ".equalsIgnoreCase(authHeader.substring(0, 6))) {
                String authStuff = new String(Base64.decode(authHeader.substring(6)));
                log.info("Found HTTP Basic auth stuff: " + authStuff);
                isBasicAuth = true;
            }

            Enumeration fields = request.getFieldNames();
            while (fields.hasMoreElements()) {
                String s = (String) fields.nextElement();
                log.info("Request header: " + s + ": " + request.getField(s));
            }

            log.info("SsgFaker: request to path: " + pathInContext);
            if ("/soap/ssg".equalsIgnoreCase(pathInContext)) {
                handlerPing(requestEnvelope, response);
            } else if ("/soap/ssg/basicauth".equalsIgnoreCase(pathInContext)) {
                if (!isBasicAuth) {
                    response.addField("WWW-Authenticate", "Basic realm=\"business\"");
                    response.setReason("Unauthorized");
                    response.addField("Content-Type", "text/html");
                    response.setStatus(401);
                    response.getOutputStream().write("<title>Uh oh</title>Uh oh".getBytes());
                    response.commit();
                } else
                    handlerPing(requestEnvelope, response);
            } else if ("/soap/ssg/throwfault".equalsIgnoreCase(pathInContext)) {
                response.setStatus(200);
                response.addField("Content-Type", "text/xml");
                response.getOutputStream().write(("<soapenv:Envelope" +
                                                 " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                                                 " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
                                                 " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                                 " <soapenv:Body>\n" +
                                                 "  <soapenv:Fault>\n" +
                                                 "   <faultcode>soapenv:Server</faultcode>\n" +
                                                 "   <faultstring>Assertion Falsified</faultstring>\n" +
                                                 "   <faultactor></faultactor>\n" +
                                                 "   <detail/>\n" +
                                                 "  </soapenv:Fault>\n" +
                                                 " </soapenv:Body>\n" +
                                                 "</soapenv:Envelope>\n").getBytes());
                response.commit();
            } else
                throw new HttpException(404, "No service with that URI in this SsgFaker");
        }

        private void handlerPing(SOAPEnvelope requestEnvelope, HttpResponse response) throws IOException, HttpException {
            String namespace = requestEnvelope.getNamespaceURI();

            SOAPEnvelope responseEnvelope = new SOAPEnvelope();
            responseEnvelope.setHeader(new SOAPHeader(namespace,
                                                      "",
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
            response.getOutputStream().write(responseEnvelope.toString().getBytes());
            response.commit();
        }
    }

    /** Fire up an SsgFaker for testing purposes. */
    public static void main(String[] args) throws IOException {
        SsgFaker ssgFaker = new SsgFaker();
        String url = ssgFaker.start();
        System.out.println("SSG Faker is now listening on " + url);

        Object forever = new Object();
        synchronized(forever) {
            try {
                forever.wait();
            } catch (InterruptedException e) {
            }
        }
    }
}
