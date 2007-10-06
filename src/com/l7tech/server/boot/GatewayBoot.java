package com.l7tech.server.boot;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.security.MasterPasswordManager;
import com.l7tech.server.BootProcess;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.tomcat.*;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Embedded;
import org.apache.naming.resources.FileDirContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.xml.sax.SAXException;

import javax.naming.directory.DirContext;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.text.ParseException;

/**
 * Object that represents a complete, running Gateway instance.
 * TODO: merge this with BootProcess, after demoting Tomcat to an ordinary Spring bean, after ensuring nothing depends on WebApplicationContext (as opposed to a plain ApplicationContext).
 */
public class GatewayBoot {
    protected static final Logger logger = Logger.getLogger(GatewayBoot.class.getName());
    public static final String SHUTDOWN_FILENAME = "SHUTDOWN.NOW";
    private static final long SHUTDOWN_POLL_INTERVAL = 1987L;

    private final File ssgHome;
    private final File inf;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Embedded embedded;
    private StandardContext context;
    private ApplicationContext applicationContext;
    private ServerConfig serverConfig;
    private MasterPasswordManager masterPasswordManager;
    private Engine engine;
    private Host host;

    /**
     * Create a Gateway instance but do not initialize or start it.
     * Set the com.l7tech.server.partitionName system property if the partition name is other than "default_".
     *
     * @param ssgHome        the home directory of the Gateway instance (ie, "/ssg")
     */
    public GatewayBoot(File ssgHome) {
        this.ssgHome = ssgHome;
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
        engine.setName("ssg");
        engine.setDefaultHost(getListenAddress());
        embedded.addEngine(engine);

        final String s = inf.getAbsolutePath();
        host = embedded.createHost(getListenAddress(), s);
        host.getPipeline().addValve(new ConnectionIdValve());
        host.getPipeline().addValve(new ResponseKillerValve());
        engine.addChild(host);

        context = (StandardContext)embedded.createContext("", s);
        context.setName("");
        context.setResources(makeResources(inf));

        StandardWrapper dflt = (StandardWrapper)context.createWrapper();
        dflt.setServletClass(DefaultServlet.class.getName());
        dflt.setName("default");
        dflt.setLoadOnStartup(1);
        context.addChild(dflt);
        context.addServletMapping("/", "default");

        host.addChild(context);
    }

    /**
     * Build the hybrid virtual/real DirContext that gets WEB-INF virtually from the class path
     * but everything else from disk
     *
     * @param inf  the /ssg/etc/inf directory.  required
     * @return     a new DirContext that will contain a virtual WEB-INF in addition to the real contents of /ssg/etc/inf/ssg
     */
    private DirContext makeResources(File inf) {
        // Splice the real on-disk ssg/ subdirectory into our virtual filesystem under /ssg
        File ssgFile = new File(inf, "ssg");
        FileDirContext ssgFileContext = new FileDirContext();
        ssgFileContext.setDocBase(ssgFile.getAbsolutePath());
        VirtualDirContext ssgContext = new VirtualDirContext("ssg", ssgFileContext);

        // Set up our virtual WEB-INF subdirectory and virtual favicon and index.html
        List<VirtualDirEntry> webinfEntries = new ArrayList<VirtualDirEntry>();
        webinfEntries.add(loadEntry("web.xml"));
        File extraDir = new File(inf, "extra");
        if (extraDir.isDirectory())
            addEntriesFromDirectory(webinfEntries, extraDir);

        VirtualDirContext webInfContext = new VirtualDirContext("WEB-INF", webinfEntries.toArray(new VirtualDirEntry[0]));
        VirtualDirEntry favicon = loadEntry("favicon.ico");
        VirtualDirEntry indexhtml = loadEntry("index.html");

        // Splice it all together
        return new VirtualDirContext("VirtualInf", webInfContext, favicon, indexhtml, ssgContext);
    }

    /**
     * Create a VirtualDirEntry out of an item in the server resources classpath.
     *
     * @param name the local name, ie "web.xml".  We will try to load com/l7tech/server/resources/web.xml
     * @return a VirtualDirEntry.  never null
     */
    private VirtualDirEntry loadEntry(String name) {
        InputStream is = null;
        try {
            String fullname = HttpTransportModule.RESOURCE_PREFIX + name;
            is = getClass().getClassLoader().getResourceAsStream(fullname);
            if (is == null)
                throw new MissingResourceException("Missing resource: " + fullname, getClass().getName(), fullname);
            return new VirtualDirEntryImpl(name, HexUtils.slurpStream(is));
        } catch (IOException e) {
            throw (MissingResourceException)new MissingResourceException("Error reading resource: " + name, getClass().getName(), name).initCause(e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    /**
     * Check for any files in the specified directory and if any are present, add them to the virtual web-inf.
     * Does not search any subdirectories.
     *
     * @param collector   a list to which new VirtualDirEntryImpl instances will be added for each file (but not subdirectory)
     *                    in the specified directory.  Required.
     * @param directory   the directory to scan for new files to merge into collector.  Required.
     */
    private void addEntriesFromDirectory(List<VirtualDirEntry> collector, File directory) {
        File[] files = directory.listFiles();
        if (files == null || files.length < 1)
            return;
        for (File file : files) {
            try {
                byte[] fileBytes = HexUtils.slurpFile(file);
                collector.add(new VirtualDirEntryImpl(file.getName(), fileBytes));
                logger.info("Noted add-on configuration file: " + file.getName());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to read add-on configuration file (ignoring it): " + file.getAbsolutePath() + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
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
            ServerXmlParser serverXml = new ServerXmlParser();
            serverXml.load(findServerXml(serverConfig));
            startInitialConnectors(keystoreUtils, serverXml);

            itworked = true;
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        } catch (ListenerException e) {
            throw new LifecycleException(e);
        } catch (InvalidDocumentFormatException e) {
            throw new LifecycleException("Unable to get Connector info from server.xml: " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new LifecycleException("Unable to get Connector info from server.xml: " + ExceptionUtils.getMessage(e), e);
        } catch (SAXException e) {
            throw new LifecycleException("Unable to get Connector info from server.xml: " + ExceptionUtils.getMessage(e), e);
        } finally {
            if (!itworked)
                running.set(false);
        }
    }

    private File findServerXml(ServerConfig config) throws FileNotFoundException {
        String path = config.getProperty(ServerConfig.PARAM_SERVERXML);
        if (path == null)
            throw new FileNotFoundException("No server.xml path configured.");
        return new File(path);
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
        waitForShutdown();
        destroy();
    }

    private void waitForShutdown() {
        if (serverConfig == null)
            throw new IllegalStateException("Unable to wait for shutdown - no serverConfig available");
        File configDir = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_CONFIG_DIRECTORY, ssgHome.getAbsolutePath(), false);
        if (configDir == null || !configDir.isDirectory())
            throw new IllegalStateException("Config directory not found: " + configDir);
        File shutFile = new File(configDir, SHUTDOWN_FILENAME);

        do {
            try {
                Thread.sleep(SHUTDOWN_POLL_INTERVAL);
            } catch (InterruptedException e) {
                logger.info("Thread interrupted - treating as shutdown request");
                break;
            }
        } while (!shutFile.exists());

        logger.info("SHUTDOWN.NOW file detected - treating as shutdown request");
    }

    private void findApplicationContext() throws LifecycleException {
        //hack: remove what catalina added as java.protocol.handler.pkgs and let the default
        // jdk protocol handler resolution
        System.getProperties().remove("java.protocol.handler.pkgs");
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(context.getServletContext());
        if (applicationContext == null)
            throw new LifecycleException("Configuration error; could not get application context");
        serverConfig = (ServerConfig)applicationContext.getBean("serverConfig", ServerConfig.class);
        masterPasswordManager = (MasterPasswordManager)applicationContext.getBean("masterPasswordManager", MasterPasswordManager.class);
    }

    private void startBootProcess() throws LifecycleException {
        BootProcess boot = (BootProcess)applicationContext.getBean("ssgBoot", BootProcess.class);
        boot.start();
    }

    private void startInitialConnectors(KeystoreUtils keyinfo, ServerXmlParser serverXml) throws ListenerException {
        List<Map<String,String>> connectors = serverXml.getConnectors();
        for (Map<String, String> connectorAttrs : connectors) {
            String portStr = connectorAttrs.remove("port");
            final int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException nfe) {
                throw new ListenerException("Bad Connector port attribute: " + portStr);
            }

            String scheme = connectorAttrs.remove("scheme");
            if (scheme == null || "http".equals(scheme)) {
                addHttpConnector(port, connectorAttrs);
            } else if ("https".equals(scheme)) {
                addHttpsConnector(port, keyinfo, connectorAttrs);
            } else
                throw new ListenerException("Unsupported Connector scheme in server.xml: " + scheme);
        }
    }

    private static final class ListenerException extends Exception {
        public ListenerException(String message) {
            super(message);
        }

        public ListenerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private Map<String, String> getDefaultHttpConnectorAttrs() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("maxThreads", "150");
        m.put("minSpareThreads", "25");
        m.put("maxSpareThreads", "75");
        m.put("acceptCount", "100");
        m.put("connectionTimeout", "20000");
        m.put("disableUploadTimeout", "true");
        m.put("socketFactory", "com.l7tech.server.tomcat.SsgServerSocketFactory");
        return m;
    }

    public void addHttpConnector(int port,Map<String, String> overrideAttrs) throws ListenerException {
        Connector c = embedded.createConnector((String)null, port, "http");
        c.setRedirectPort(8443);
        c.setEnableLookups(false);

        Map<String, String> attrs = getDefaultHttpConnectorAttrs();
        attrs.putAll(overrideAttrs);
        setConnectorAttributes(c, attrs);

        embedded.addConnector(c);
        try {
            c.start();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new ListenerException("Unable to start HTTPS listener: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private Map<String, String> getDefaultHttpsConnectorAttrs(KeystoreUtils keyinfo) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("clientAuth", "want");
        m.put("sslProtocol", "TLS");
        m.put("keystoreFile", keyinfo.getSslKeystorePath());
        m.put("keystorePass", keyinfo.getSslKeystorePasswd());
        m.put("keystoreType", keyinfo.getSslKeyStoreType());
        m.put("keyAlias", keyinfo.getSSLAlias());
        m.put("clientAuth", "want");
        m.put("SSLImplementation", "com.l7tech.server.tomcat.SsgSSLImplementation");
        m.put("maxThreads", "150");
        m.put("minSpareThreads", "25");
        m.put("maxSpareThreads", "75");
        m.put("disableUploadTimeout", "true");
        m.put("acceptCount", "100");
        return m;
    }

    public void addHttpsConnector(int port,
                                  KeystoreUtils keyinfo,
                                  Map<String, String> overrideAttrs)
            throws ListenerException
    {
        Connector c = embedded.createConnector((String)null, port, "https");
        c.setScheme("https");
        c.setProperty("SSLEnabled","true");
        c.setSecure(true);
        c.setEnableLookups(false);

        Map<String, String> attrs = getDefaultHttpsConnectorAttrs(keyinfo);

        // If the connector is trying to add an encrypted password, decrypt it first.
        // If it can't be decrypted, ignore it and fall back to the password from keystore.properties.
        String kspass = overrideAttrs.get(HttpTransportModule.CONNECTOR_ATTR_KEYSTORE_PASS);
        if (masterPasswordManager.looksLikeEncryptedPassword(kspass)) {
            try {
                char[] decrypted = masterPasswordManager.decryptPassword(kspass);
                overrideAttrs.put(HttpTransportModule.CONNECTOR_ATTR_KEYSTORE_PASS, new String(decrypted));
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to decrypt encrypted password in server.xml -- falling back to password from keystore.properties: " + ExceptionUtils.getMessage(e));
                overrideAttrs.remove(HttpTransportModule.CONNECTOR_ATTR_KEYSTORE_PASS);
            }
        }

        attrs.putAll(overrideAttrs);
        setConnectorAttributes(c, attrs);

        embedded.addConnector(c);
        try {
            c.start();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new ListenerException("Unable to start HTTPS listener: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void setConnectorAttributes(Connector c, Map<String, String> attrs) {
        for (Map.Entry<String, String> entry : attrs.entrySet())
            c.setAttribute(entry.getKey(), entry.getValue());
    }

    /**
     * Provides access to the embedded Tomcat Engine.  It initially contains a single Host, which itself contains a
     * single Context.
     *
     * @return an Engine instance, as long as the Gateway has been started.
     */
    public Engine getEngine() {
        return engine;
    }

    /**
     * Provides access to the embedded Tomcat Host.  It initially contains a single Context.
     *
     * @return a Host instance, as long as the Gateway has been started.
     */
    public Host getHost() {
        return host;
    }

    /**
     * Provides access to the embedded Tomcat servlet context we are running.
     *
     * @return a StandardContext instance, as long as the Gateway has been started.
     */
    public StandardContext getContext() {
        return context;
    }
}
