package com.l7tech.proxy;

import org.apache.axis.AxisEngine;
import org.apache.axis.encoding.Base64;
import org.apache.axis.encoding.DeserializationContextImpl;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPHandler;
import org.apache.axis.message.SOAPHeader;
import org.mortbay.http.*;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.util.MultiException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.net.ssl.*;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A "test" Ssg that can be controlled programmatically.  Used to test the Client Proxy.
 * Implements a simple echo server.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 11:17:24 AM
 */
public class SsgFaker {
    private static final Logger log = Logger.getLogger(SsgFaker.class.getName());
    private static final String CERT_PATH = "com/l7tech/proxy/resources/selfsigned.ks";

    private HttpServer httpServer;
    private int maxThreads = 4;
    private int minThreads = 1;
    private int localPort = 7566;
    private int sslPort = 7443;
    private String ssgUrl = "http://localhost:" + localPort;
    private String sslUrl = "https://localhost:" + sslPort;
    private boolean destroyed = false;

    /**
     * Create an SsgFaker with default settings.
     */
    SsgFaker() {
    }

    /**
     * Start the test SSG.
     *
     * @return The SSG's soap URL.
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

        httpServer = new HttpServer();

        HttpContext context = new HttpContext(httpServer, "/");
        HttpHandler requestHandler = new SsgFakerHandler();
        context.addHandler(requestHandler);
        httpServer.addContext(context);

        // Set up HTTP listener
        SocketListener socketListener = new SocketListener();
        socketListener.setMaxThreads(maxThreads);
        socketListener.setMinThreads(minThreads);
        socketListener.setPort(localPort);
        httpServer.addListener(socketListener);

        // Set up SSL listener
        SunJsseListener sslListener = new SunJsseListener() {
            protected SSLServerSocketFactory createFactory()
              throws Exception {
                SSLContext sslc = SSLContext.getInstance("SSL");
                final KeyStore ks = loadKeyStore();
                final X509Certificate cert = (X509Certificate)ks.getCertificate("testcert");
                final PrivateKey key = (PrivateKey)ks.getKey("testcert", new char[0]);

                KeyManager km = new X509KeyManager() {
                    public PrivateKey getPrivateKey(String s) {
                        return key;
                    }

                    public X509Certificate[] getCertificateChain(String s) {
                        return new X509Certificate[]{cert};
                    }

                    public String[] getClientAliases(String s, Principal[] principals) {
                        return new String[]{"testcert"};
                    }

                    public String[] getServerAliases(String s, Principal[] principals) {
                        return new String[]{"testcert"};
                    }

                    public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
                        return "testcert";
                    }

                    /**
                     * 1.5 compatibility stub
                     */
                    public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
                        return "testcert";
                    }

                    public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                        return "testcert";
                    }

                    /**
                     * 1.5 compatibility stub
                     */ 
                    public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
                        return "testcert";
                    }
                };

                TrustManager tm = new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    }

                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    }
                };

                sslc.init(new KeyManager[]{km}, new TrustManager[]{tm}, new SecureRandom());
                return sslc.getServerSocketFactory();
            }
        };
        sslListener.setMaxThreads(maxThreads);
        sslListener.setMinThreads(minThreads);
        sslListener.setPort(sslPort);
        httpServer.addListener(sslListener);

        try {
            httpServer.start();
        } catch (MultiException e) {
            log.log(Level.SEVERE, "Unable to start up test SSG", e);
            throw e.getException(0);
        }
        log.info("SsgFaker started; listening for http connections on " + ssgUrl);
        log.info("SsgFaker listeneing for https connections on " + sslUrl);
        return ssgUrl;
    }

    private KeyStore loadKeyStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        URL url = getClass().getClassLoader().getResource(CERT_PATH);
        if (url == null)
            throw new RuntimeException("Certificate is missing: " + CERT_PATH);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream is = url.openConnection().getInputStream();
        ks.load(is, new char[0]);
        return ks;
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
    private synchronized void stop() {
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
          throws HttpException, IOException {
            log.info("SsgFakerHandler: incoming request: pathInContext=" + pathInContext);

            //log.info("Got request: " + new String(HexUtils.slurpStream(request.getInputStream(), 16384)));

            SOAPEnvelope requestEnvelope;
            try {
                requestEnvelope = new SOAPEnvelope(request.getInputStream());
            } catch (SAXException e) {
                log.log(Level.SEVERE, "SAX error", e);
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
                String s = (String)fields.nextElement();
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
                responseEnvelope.setBody((SOAPBody)requestEnvelope.getBody());
                ;
            } catch (SOAPException e) {
                log.log(Level.SEVERE, "SOAP eror", e);
                throw new HttpException(400, "Internal error: " + e.getMessage());
            }

            response.addField("Content-Type", "text/xml");
            response.getOutputStream().write(responseEnvelope.toString().getBytes());
            response.commit();
        }
    }

    /**
     * Fire up an SsgFaker for testing purposes.
     */
    public static void main(String[] args) throws Exception {
        SsgFaker ssgFaker = new SsgFaker();
        String url = ssgFaker.start();
        System.out.println("SSG Faker is now listening on " + url);

        Object forever = new Object();
        synchronized (forever) {
            try {
                forever.wait();
            } catch (InterruptedException e) {
            }
        }
    }
}
