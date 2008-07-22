package com.l7tech.proxy;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import org.mortbay.jetty.*;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.security.SslSocketConnector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPConstants;
import java.io.Closeable;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A "test" Ssg that can be controlled programmatically.  Used to test the Client Proxy.
 * Implements a simple echo server.
 */
public class SsgFaker implements Closeable {
    private static final Logger log = Logger.getLogger(SsgFaker.class.getName());
    public static final String PING_NS = "http://services.l7tech.com/soap/demos/Ping";

    private Server httpServer;
    private int localPort = 7566;
    private int sslPort = 7443;
    private String ssgUrl = "http://localhost:" + localPort;
    private String sslUrl = "https://localhost:" + sslPort;
    private boolean destroyed = false;
    private String wsdlProxyResponseBody = "";
    private PasswordAuthentication gotCreds;

    /**
     * Create an SsgFaker with default settings.
     */
    public SsgFaker() {
    }

    /**
     * Start the test SSG.
     *
     * @return The SSG's soap URL.
     * @throws Exception on error
     */
    public String start() throws Exception {
        try {
            return doStart();
        } catch (Exception e) {
            log.log(Level.SEVERE, "SsgFaker: unable to start: " + e, e);
            throw e;
        }
    }

    public synchronized String doStart() throws Exception {
        if (destroyed)
            throw new IllegalStateException("this SsgFaker is no more");

        httpServer = new Server();
        httpServer.addHandler(new SsgFakerHandler());

        // Set up HTTP listener
        Connector socketListener = new SocketConnector();
        socketListener.setPort(localPort);
        httpServer.addConnector(socketListener);

        // Set up SSL listener
        SslSocketConnector sslListener = new SslSocketConnector() {
            final SSLContext ctx; {
                X509Certificate cert = TestDocuments.getDotNetServerCertificate();
                PrivateKey key = TestDocuments.getDotNetServerPrivateKey();
                SingleCertX509KeyManager km = new SingleCertX509KeyManager(new X509Certificate[]{cert}, key);
                ctx = SSLContext.getInstance("SSL");
                ctx.init(new KeyManager[] { km }, null, null);
            }

            protected SSLServerSocketFactory createFactory() throws Exception {
                return ctx.getServerSocketFactory();
            }
        };        
        sslListener.setPort(sslPort);
        sslListener.setConfidentialPort(sslPort);
        sslListener.setConfidentialScheme("https");
        httpServer.addConnector(sslListener);

        httpServer.start();

        log.info("SsgFaker started; listening for http connections on " + ssgUrl);
        log.info("SsgFaker listening for https connections on " + sslUrl);
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
     * @throws Exception if shutdown fails
     */
    private synchronized void stop() throws Exception {
        log.info("SsgFaker shutting down");
        httpServer.stop();
    }

    /**
     * Free resources used by the test SSG.
     */
    public synchronized void destroy() {
        if (destroyed)
            return;
        try {
            stop();
        } catch (Exception e) {
            log.log(Level.WARNING, "Error shutting down SsgFaker: " + ExceptionUtils.getMessage(e), e);
        }
        httpServer.destroy();
        destroyed = true;
    }

    public void close() {
        destroy();
    }

    public static Document makePingRequest(String payload) throws IOException {
        Document doc = XmlUtil.createEmptyDocument("Envelope", "soap", SOAPConstants.URI_NS_SOAP_ENVELOPE);
        Element body = XmlUtil.createAndAppendElementNS(doc.getDocumentElement(), "Body", SOAPConstants.URI_NS_SOAP_ENVELOPE, "soap");
        Element ping = XmlUtil.createAndAppendElementNS(body, "ping", PING_NS + "#ping", "p");
        Element payloadEl = XmlUtil.createAndAppendElementNS(ping, "pingData", ping.getNamespaceURI(), ping.getPrefix());
        payloadEl.appendChild(XmlUtil.createTextNode(payloadEl, payload));
        return doc;
    }

    /** @noinspection SerializableNonStaticInnerClassWithoutSerialVersionUID,SerializableInnerClassWithNonSerializableOuterClass,NonStaticInnerClassInSecureContext */
    private class SsgFakerHandler extends AbstractHandler {
        public void handle(String pathInContext, HttpServletRequest servRequest, HttpServletResponse servResponse, int dispatch) throws IOException, ServletException {
            Request request = (Request)servRequest;
            Response response = (Response)servResponse;

            log.info("SsgFakerHandler: incoming request: pathInContext=" + pathInContext);

            boolean isBasicAuth = isBasicAuth(request);

            Enumeration fields = request.getHeaderNames();
            while (fields.hasMoreElements()) {
                String s = (String)fields.nextElement();
                log.info("Request header: " + s + ": " + request.getHeader(s));
            }

            log.info("SsgFaker: request to path: " + pathInContext);

            // Handle requests that do NOT require a request document
            if (pathInContext.startsWith(SecureSpanConstants.WSDL_PROXY_FILE)) {
                handleWsdlProxyRequest(response);
                return;
            } else if ("/soap/ssg/throwfault".equalsIgnoreCase(pathInContext)) {
                handleThrowFault(response);
                return;
            }

            // Get the request document
            final Document requestEnvelope;
            try {
                requestEnvelope = XmlUtil.parse(request.getInputStream());
            } catch (Exception e) {
                log.log(Level.WARNING, "Unable to parse request", e);
                throw new HttpException(400, "Couldn't parse SOAP envelope: " + e.getMessage());
            }

            // Handle requests that DO require a request document
            if ("/soap/ssg".equalsIgnoreCase(pathInContext)) {
                handlerPing(requestEnvelope, response);
            } else if ("/soap/ssg/basicauth".equalsIgnoreCase(pathInContext)) {
                if (isBasicAuth) {
                    handlerPing(requestEnvelope, response);
                } else {
                    response.setStatus(401, "Unauthorized");
                    response.setContentType("text/xml");
                    response.addHeader("WWW-Authenticate", "Basic realm=\"business\"");
                    response.getOutputStream().write("<title>Uh oh</title>".getBytes());
                    response.complete();
                }
            } else if ("/".equals(pathInContext)) {
                response.setStatus(200);
                response.setContentType("text/xml");
                response.getOutputStream().write("<title>Heyas</title>".getBytes());
                response.complete();
            } else {
                log.info("URI didn't match anything -- returning 404");
                throw new HttpException(404, "No service with that URI in this SsgFaker");
            }
        }

        private boolean isBasicAuth(Request request) throws IOException {
            boolean isBasicAuth = false;
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && "Basic ".equalsIgnoreCase(authHeader.substring(0, 6))) {
                String authStuff = new String(HexUtils.decodeBase64(authHeader.substring(6)));
                log.info("Found HTTP Basic auth stuff: " + authStuff);
                isBasicAuth = true;
                String[] stuffs = authStuff.split(":");
                gotCreds = new PasswordAuthentication(stuffs[0], stuffs[1].toCharArray());
            }
            return isBasicAuth;
        }

        private void handlerPing(Document requestEnvelope, Response response) throws IOException {
            response.setContentType("text/xml");
            XmlUtil.nodeToOutputStream(requestEnvelope, response.getOutputStream());
            response.complete();
        }
    }

    public String getWsdlProxyResponseBody() {
        return wsdlProxyResponseBody;
    }

    public void setWsdlProxyResponseBody(String wsdlProxyResponseBody) {
        if (wsdlProxyResponseBody == null) throw new NullPointerException();
        this.wsdlProxyResponseBody = wsdlProxyResponseBody;
    }

    public PasswordAuthentication getGotCreds() {
        return gotCreds;
    }

    protected void handleWsdlProxyRequest(Response response) throws IOException {
        response.setStatus(200);
        response.setContentType("text/xml");
        response.getOutputStream().write(wsdlProxyResponseBody.getBytes());
        response.complete();
    }

    protected static void handleThrowFault(Response response) throws IOException {
        response.setStatus(200);
        response.setContentType("text/xml");
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
        response.complete();
    }

    /**
     * Fires up an SsgFaker for testing purposes.
     * @param args ignored
     * @throws Exception on error
     * @noinspection UseOfSystemOutOrSystemErr
     */
    public static void main(String[] args) throws Exception {
        SsgFaker ssgFaker = new SsgFaker();
        String url = ssgFaker.start();
        System.out.println("SSG Faker is now listening on " + url);

        Object forever = new Object();
        synchronized (forever) {
            forever.wait();
        }
    }
}
