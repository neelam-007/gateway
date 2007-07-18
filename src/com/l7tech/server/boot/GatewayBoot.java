package com.l7tech.server.boot;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.server.BootProcess;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.tomcat.ConnectionIdValve;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.tomcat.ClassLoaderLoader;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Embedded;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Object that represents a complete, running Gateway instance.
 * TODO: merge this with BootProcess, after demoting Tomcat to an ordinary Spring bean, after ensuring nothing depends on WebApplicationContext (as opposed to a plain ApplicationContext).
 */
public class GatewayBoot {
    protected static final Logger logger = Logger.getLogger(GatewayBoot.class.getName());

    private final File inf;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Embedded embedded;
    private StandardContext context;
    private ApplicationContext applicationContext;
    private Engine engine;
    private Host host;

    /**
     * Create a Gateway instance but do not initialize or start it.
     * Set the com.l7tech.server.partitionName system property if the partition name is other than "default_".
     *
     * @param ssgHome        the home directory of the Gateway instance (ie, "/ssg")
     */
    public GatewayBoot(File ssgHome) {
        mustBeADirectory(ssgHome);
        inf = new File(ssgHome, "etc" + File.separator + "inf");
    }

    private void mustBeADirectory(File ssgHome) {
        if (ssgHome == null || !ssgHome.exists() || !ssgHome.isDirectory())
            throw new IllegalArgumentException("Not a directory: " + ssgHome);
    }

    public void init() {
        if (!initialized.compareAndSet(false, true)) {
            // Already initialized
            return;
        }

        embedded = new Embedded();
        engine = embedded.createEngine();
        engine.setDefaultHost(getListenAddress());
        embedded.addEngine(engine);

        final String s = inf.getAbsolutePath();
        host = embedded.createHost(getListenAddress(), s);
        host.getPipeline().addValve(new ConnectionIdValve());
        host.getPipeline().addValve(new ResponseKillerValve());

        context = (StandardContext)embedded.createContext(s, s);
        context.setName("");
        context.setLoader(new ClassLoaderLoader(getClass().getClassLoader()));
        host.addChild(context);
    }

    public void start() throws LifecycleException {
        if (!running.compareAndSet(false, true)) {
            // Already started
            return;
        }

        // This thread is responsible for attempting to start the server, and for clearing "running" flag if it fails
        boolean itworked = false;
        try {
            init();

            embedded.start();
            context.start();

            findApplicationContext();
            startBootProcess();
            KeystoreUtils keystoreUtils = (KeystoreUtils)applicationContext.getBean("keystore", KeystoreUtils.class);
            String addr;
            addr = getListenAddress();
            startInitialConnectors(addr, keystoreUtils);
            itworked = true;
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        } catch (ListenerException e) {
            throw new LifecycleException(e);
        } finally {
            if (!itworked)
                running.set(false);
        }
    }

    private String getListenAddress() {
        String addr;
        try {
            addr = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            addr = "0.0.0.0";
        }
        return addr;
    }

    /**
     * Stop the running Gateway.
     * This Gateway instance can not be started again once it is stopped and should be destroyed and discarded.
     *
     * @throws LifecycleException if there is a problem shutting down the server
     */
    public void stop() throws LifecycleException {
        if (!running.get())
            return;
        try {
            embedded.stop();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        }
    }

    /**
     * Destroy this Gateway instance.
     * This Gateway instance can not be started again once it is destroyed and should be discarded.
     *
     * @throws LifecycleException if there is a problem shutting down the server
     */
    public void destroy() throws LifecycleException {
        stop();
        try {
            embedded.destroy();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        }
    }

    /**
     * Initialize and start the Gateway and run it until it is shut down.
     * This method does not return until the Gateway has shut down normally.
     *
     * @throws LifecycleException if the Gateway could not be started due to a failure of some kind,
     *                                              or if there was an exception while attempting a normal shutdown.
     */
    public void runUntilShutdown() throws LifecycleException {
        init();
        start();

        // TODO: implement shutdown listener
        Object w = new Object();
        synchronized (w) {
            try {
                w.wait();
            } catch (InterruptedException e) {
                logger.info("Main thread interrupted - shutting down");
                destroy();
            }
        }
    }

    private void findApplicationContext() throws LifecycleException {
        //hack: remove what catalina added as java.protocol.handler.pkgs and let the default
        // jdk protocol handler resolution
        System.getProperties().remove("java.protocol.handler.pkgs");
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(context.getServletContext());
        if (applicationContext == null)
            throw new LifecycleException("Configuration error; could not get application context");
    }

    private void startBootProcess() throws LifecycleException {
        BootProcess boot = (BootProcess)applicationContext.getBean("ssgBoot", BootProcess.class);
        boot.start();
    }

    private void startInitialConnectors(String address, KeystoreUtils keyinfo) throws ListenerException {
        // TODO get all this info from the database
        addHttpConnector(address, 8080);
        addHttpsConnector(address, 8443, true, keyinfo);
        addHttpsConnector(address, 9443, false, keyinfo);
    }

    private static final class ListenerException extends Exception {
        public ListenerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void addHttpConnector(String address, int port) throws ListenerException {
        Connector c = embedded.createConnector(address, port, "http");
        // TODO other attributes from server.xml like disableUploadTimeout
        embedded.addConnector(c);
        try {
            c.start();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new ListenerException("Unable to start HTTPS listener: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public void addHttpsConnector(String address, int port, boolean wantClientAuth, KeystoreUtils keyinfo) throws ListenerException {
        Connector c = embedded.createConnector(address, port, "https");
        c.setScheme("https");
        c.setProperty("SSLEnabled","true");
        c.setAttribute("sslProtocol", "TLS");
        c.setSecure(true);

        c.setAttribute("keystoreFile", keyinfo.getSslKeystorePath());
        c.setAttribute("keystorePass", keyinfo.getSslKeystorePasswd());
        c.setAttribute("keystoreType", keyinfo.getSslKeyStoreType());
        c.setAttribute("keyAlias", keyinfo.getSSLAlias());
        c.setAttribute("clientAuth", wantClientAuth  ? "want" : "false");
        c.setAttribute("SSLImplementation", "com.l7tech.server.tomcat.SsgSSLImplementation");
        // TODO other attributes from server.xml like disableUploadTimeout
        embedded.addConnector(c);
        try {
            c.start();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new ListenerException("Unable to start HTTPS listener: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
