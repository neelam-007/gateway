package com.l7tech.skunkworks.server;

import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Embedded;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import java.util.Arrays;
import java.util.List;
import java.net.InetAddress;

import com.l7tech.server.*;
import com.l7tech.server.tomcat.ConnectionIdValve;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.tomcat.ClassLoaderLoader;

/**
 * An entry point that starts up the SecureSpan Gateway in embedded mode.
 */
public class TestEmbeddedServerMain {
    private static final TestEmbeddedServerMain INSTANCE = new TestEmbeddedServerMain();
    private Embedded embedded;
    private Host host;
    private Wrapper messageProcessor;
    private StandardContext context;
    private Engine engine;
    private boolean started = false;
    private ApplicationContext applicationContext;

    public static TestEmbeddedServerMain getInstance() {
        return INSTANCE;
    }

    public static void main(String[] args) {
        try {
            getInstance().runForever();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Create a server, start it, and run it forever.
     *
     * @throws Exception on error starting the server
     */
    public void runForever() throws Exception {
        embedded = new Embedded();
        engine = embedded.createEngine();
        embedded.addEngine(engine);

        final String s = "c:/ssg/etc/inf";
        host = embedded.createHost(InetAddress.getLocalHost().getCanonicalHostName(), s);
        host.getPipeline().addValve(new ConnectionIdValve(null));
        host.getPipeline().addValve(new ResponseKillerValve());        

        context = (StandardContext)embedded.createContext(s, s);
        context.setName("");

        messageProcessor = context.createWrapper();
        messageProcessor.setName("MessageProcessingServlet");
        messageProcessor.setServletClass(TestEmbeddedMessageProcessingServlet.class.getName());
        messageProcessor.setLoader(new ClassLoaderLoader(TestEmbeddedServerMain.class.getClassLoader()));
        context.addChild(messageProcessor);
        context.addServletMapping("/ssg/test", "MessageProcessingServlet");
        host.addChild(context);


        embedded.start();
        context.start();
        messageProcessor.load();
        started = true;

        findApplicationContext();
        startBootProcess();
        startInitialConnectors();

        Object w = new Object();
        synchronized (w) {
            w.wait();
        }
    }

    private void findApplicationContext() throws ServletException {
        //hack: remove what catalina added as java.protocol.handler.pkgs and let the default
        // jdk protocol handler resolution
        System.getProperties().remove("java.protocol.handler.pkgs");
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(context.getServletContext());
        if (applicationContext == null)
            throw new ServletException("Configuration error; could not get application context");
    }

    private void startBootProcess() throws ServletException, com.l7tech.server.LifecycleException {
        BootProcess boot = (BootProcess)applicationContext.getBean("ssgBoot", BootProcess.class);
        boot.start();
    }

    private void startInitialConnectors() throws Exception {
        addHttpConnector(8080);
        addHttpsConnector(8443, true);
        addHttpsConnector(9443, false);
    }

    public List<Connector> getConnectors() {
        return Arrays.asList(embedded.findConnectors());
    }

    public void addHttpConnector(int port) throws Exception {
        Connector c = embedded.createConnector(InetAddress.getLocalHost(), port, "http");
        c.setAttribute("socketFactory", "com.l7tech.server.tomcat.SsgServerSocketFactory");
        // TODO other attributes from server.xml like disableUploadTimeout
        embedded.addConnector(c);
        if (started) c.start();
    }

    public void addHttpsConnector(int port, boolean wantClientAuth) throws Exception {
        Connector sslConnector = embedded.createConnector(InetAddress.getLocalHost(), port, "https");
        sslConnector.setSecure(true);
        sslConnector.setAttribute("sslProtocol", "TLS");
        sslConnector.setAttribute("keystoreFile", "c:/ssg/etc/conf/partitions/default_/keys/ssl.ks");
        sslConnector.setAttribute("keystorePass", "tralala");
        sslConnector.setAttribute("keystoreType", "PKCS12");
        sslConnector.setAttribute("keyAlias", "tomcat");
        sslConnector.setAttribute("clientAuth", wantClientAuth  ? "want" : "false");
        sslConnector.setAttribute("SSLImplementation", "com.l7tech.server.tomcat.SsgSSLImplementation");
        // TODO other attributes from server.xml like disableUploadTimeout
        embedded.addConnector(sslConnector);
        if (started) sslConnector.start();
    }

    public void removeHttpConnector(int port) throws Exception {
        Connector[] connectors = embedded.findConnectors();
        Connector toRemove = null;
        for (Connector connector : connectors) {
            if (port == connector.getPort()) {
                toRemove = connector;
                break;
            }
        }
        if (toRemove == null)
            throw new IllegalArgumentException("No connector running on port: " + port);
        toRemove.stop();
        embedded.removeConnector(toRemove);
        toRemove.destroy();
    }

}
