package com.l7tech.server.transport.http;

import com.l7tech.common.security.MasterPasswordManager;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.tomcat.*;
import com.l7tech.server.transport.SsgConnectorManager;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Embedded;
import org.apache.naming.resources.FileDirContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.xml.sax.SAXException;

import javax.naming.directory.DirContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that owns the Tomcat servlet container and all the HTTP/HTTPS connectors.
 * <p/>
 * It listens for entity chagne events for SsgConnector to know when to start/stop HTTP connectors.
 */
public class HttpTransportModule implements InitializingBean, ApplicationListener, DisposableBean {
    protected static final Logger logger = Logger.getLogger(HttpTransportModule.class.getName());

    public static final String RESOURCE_PREFIX = "com/l7tech/server/resources/";
    public static final String CONNECTOR_ATTR_KEYSTORE_PASS = "keystorePass";

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ServerConfig serverConfig;
    private final MasterPasswordManager masterPasswordManager;
    private final KeystoreUtils keystoreUtils;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final SsgConnectorManager ssgConnectorManager;
    private final Map<Long, Connector> activeConnectors = new ConcurrentHashMap<Long, Connector>();
    private final Map<Long, Throwable> connectorErrors = new ConcurrentHashMap<Long, Throwable>();
    private Embedded embedded;
    private StandardContext context;
    private Engine engine;
    private Host host;

    public HttpTransportModule(ServerConfig serverConfig,
                               MasterPasswordManager masterPasswordManager,
                               KeystoreUtils keystoreUtils,
                               SsgKeyStoreManager ssgKeyStoreManager,
                               SsgConnectorManager ssgConnectorManager)
    {
        this.serverConfig = serverConfig;
        this.masterPasswordManager = masterPasswordManager;
        this.keystoreUtils = keystoreUtils;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.ssgConnectorManager = ssgConnectorManager;
    }

    private void initializeServletEngine() throws LifecycleException {
        if (!initialized.compareAndSet(false, true)) {
            // Already initialized
            return;
        }

        embedded = new Embedded();
        engine = embedded.createEngine();
        engine.setName("ssg");
        engine.setDefaultHost(getListenAddress());
        embedded.addEngine(engine);

        File ssgHome = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_SSG_HOME_DIRECTORY, null, false);
        if (ssgHome == null) throw new LifecycleException("No ssgHome set");
        File inf = new File(ssgHome, "etc" + File.separator + "inf");
        if (!inf.exists() || !inf.isDirectory()) throw new LifecycleException("No such directory: " + inf.getPath());

        final String s = inf.getAbsolutePath();
        host = embedded.createHost(getListenAddress(), s);
        host.getPipeline().addValve(new ConnectionIdValve());
        host.getPipeline().addValve(new ResponseKillerValve());
        engine.addChild(host);

        context = (StandardContext)embedded.createContext("", s);
        context.setName("");
        context.setResources(createHybridDirContext(inf));

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
     * but everything else from the specified inf directory on disk.
     *
     * @param inf  the /ssg/etc/inf directory.  required
     * @return     a new DirContext that will contain a virtual WEB-INF in addition to the real contents of /ssg/etc/inf/ssg
     */
    private DirContext createHybridDirContext(File inf) {
        // Splice the real on-disk ssg/ subdirectory into our virtual filesystem under /ssg
        File ssgFile = new File(inf, "ssg");
        FileDirContext ssgFileContext = new FileDirContext();
        ssgFileContext.setDocBase(ssgFile.getAbsolutePath());
        VirtualDirContext ssgContext = new VirtualDirContext("ssg", ssgFileContext);

        // Set up our virtual WEB-INF subdirectory and virtual favicon and index.html
        List<VirtualDirEntry> webinfEntries = new ArrayList<VirtualDirEntry>();
        webinfEntries.add(createDirEntryFromClassPathResource("web.xml"));
        File extraDir = new File(inf, "extra");
        if (extraDir.isDirectory())
            addDirEntriesFromDirectory(webinfEntries, extraDir);

        VirtualDirContext webInfContext = new VirtualDirContext("WEB-INF", webinfEntries.toArray(new VirtualDirEntry[0]));
        VirtualDirEntry favicon = createDirEntryFromClassPathResource("favicon.ico");
        VirtualDirEntry indexhtml = createDirEntryFromClassPathResource("index.html");

        // Splice it all together
        return new VirtualDirContext("VirtualInf", webInfContext, favicon, indexhtml, ssgContext);
    }

    /**
     * Create a VirtualDirEntry out of an item in the server resources classpath.
     *
     * @param name the local name, ie "web.xml".  We will try to load com/l7tech/server/resources/web.xml
     * @return a VirtualDirEntry.  never null
     */
    private VirtualDirEntry createDirEntryFromClassPathResource(String name) {
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
    private void addDirEntriesFromDirectory(List<VirtualDirEntry> collector, File directory) {
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

    private void startServletEngine() throws LifecycleException {
        if (!running.compareAndSet(false, true)) {
            // Already started
            return;
        }

        // This thread is responsible for attempting to start the server, and for clearing "running" flag if it fails
        boolean itworked = false;
        try {
            initializeServletEngine();

            embedded.start();
            context.start();

            ServerXmlParser serverXml = new ServerXmlParser();
            serverXml.load(findServerXml(serverConfig));
            startInitialConnectors();

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
     * Destroy the transport module.
     * This transport module instance can not be started again once it is destroyed and should be discarded.
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

    private void startInitialConnectors() throws ListenerException {
        try {
            Collection<SsgConnector> connectors = ssgConnectorManager.findAll();
            for (SsgConnector connector : connectors) {
                addConnector(connector);
            }

        } catch (FindException e) {
            throw new ListenerException("Unable to find initial connectors: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void addConnector(SsgConnector connector) throws ListenerException {
        removeConnector(connector.getOid());

        Map<String, Object> connectorAttrs = asTomcatConnectorAttrs(connector);
        connectorAttrs.remove("scheme");
        final String scheme = connector.getScheme();
        if (SsgConnector.SCHEME_HTTP.equals(scheme)) {
            addHttpConnector(connector.getPort(), connectorAttrs);
        } else if (SsgConnector.SCHEME_HTTPS.equals(scheme)) {
            addHttpsConnector(connector.getPort(), connectorAttrs);
        } else {
            // It's not an HTTP connector; ignore it
            logger.fine("HttpTransportModule is ignoring non-HTTP connector with scheme " + scheme);
        }
    }

    private Map<String, Object> asTomcatConnectorAttrs(SsgConnector c) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();

        m.put("maxThreads", "150");
        m.put("minSpareThreads", "25");
        m.put("maxSpareThreads", "75");
        m.put("disableUploadTimeout", "true");
        m.put("acceptCount", "100");

        String bindAddress = c.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        if (bindAddress == null || bindAddress.equals("*") || bindAddress.equals("0.0.0.0")) {
            m.remove("address");
        } else {
            m.put("address", bindAddress);
        }

        if (SsgConnector.SCHEME_HTTPS.equals(c.getScheme())) {
            // SSL
            m.put("sslProtocol", "TLS");
            m.put("SSLImplementation", "com.l7tech.server.tomcat.SsgSSLImplementation");
            m.put("secure", Boolean.toString(c.isSecure()));

            final int clientAuth = c.getClientAuth();
            if (clientAuth == SsgConnector.CLIENT_AUTH_ALWAYS)
                m.put("clientAuth", "true");
            else if (clientAuth == SsgConnector.CLIENT_AUTH_OPTIONAL)
                m.put("clientAuth", "want");
            else if (clientAuth == SsgConnector.CLIENT_AUTH_NEVER)
                m.put("clientAuth", "false");

            if (c.getKeystoreOid() == null) {
                m.put("keystoreFile", keystoreUtils.getSslKeystorePath());
                m.put("keystorePass", keystoreUtils.getSslKeystorePasswd());
                m.put("keystoreType", keystoreUtils.getSslKeyStoreType());
                m.put("keyAlias", keystoreUtils.getSSLAlias());
            } else {
                m.put(SsgJSSESocketFactory.ATTR_KEYALIAS, c.getKeyAlias());
                m.put(SsgJSSESocketFactory.ATTR_KEYSTOREOID, c.getKeystoreOid().toString());
                m.put(SsgJSSESocketFactory.ATTR_SSGKEYSTOREMANAGER, ssgKeyStoreManager);
            }

            String cipherList = c.getProperty(SsgConnector.PROP_CIPHERLIST);
            if (cipherList != null)
                m.put(SsgJSSESocketFactory.ATTR_CIPHERNAMES, cipherList);

        } else {
            // Not SSL
            m.put("connectionTimeout", "20000");
            m.put("socketFactory", "com.l7tech.server.tomcat.SsgServerSocketFactory");
        }

        // Allow overrides of any parameters we didn't already handle
        List<String> extraPropNames = c.getPropertyNames();
        for (String name : extraPropNames) {
            if (name.equals(SsgConnector.PROP_BIND_ADDRESS) ||
                name.equals(SsgConnector.PROP_PORT_RANGE_START) ||
                name.equals(SsgConnector.PROP_PORT_RANGE_COUNT) ||
                name.equals(SsgConnector.PROP_CIPHERLIST))
                continue;
            String value = c.getProperty(name);
            m.put(name, value);
        }

        return m;
    }

    /**
     * Get the last error that occurred while starting a specified connector.
     * <p/>
     * This does not clear the error status for the specified connector.  To clear the error, call
     * clearConnectorError().
     *
     * @param oid  the connector to check.
     * @return  the last error encountered while starting (or stopping) this connector, or null if none recorded.
     */
    public Throwable getConnectorError(long oid) {
        return connectorErrors.get(oid);
    }

    /**
     * Clear the last error status for the specified connector.  If any error status is cleared,
     * it will be returned.
     *
     * @param oid the connector whose error status to clear.
     * @return the error status that was cleared, or null.
     */
    public Throwable clearConnectorError(long oid) {
        return connectorErrors.remove(oid);
    }

    private void removeConnector(long oid) {
        Connector connector = activeConnectors.remove(oid);
        if (connector == null) return;
        embedded.removeConnector(connector);
        // TODO do we need to do anything else here to ensure that it's really gone?
    }


    public void afterPropertiesSet() throws Exception {
        startServletEngine();
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent)applicationEvent;
            if (SsgConnector.class.equals(event.getEntityClass())) {
                long[] ids = event.getEntityIds();
                char[] ops = event.getEntityOperations();
                for (int i = 0; i < ids.length; i++) {
                    long id = ids[i];
                    try {
                        switch (ops[i]) {
                        case EntityInvalidationEvent.CREATE:
                        case EntityInvalidationEvent.UPDATE:
                            SsgConnector c = ssgConnectorManager.findByPrimaryKey(id);
                            addConnector(c);
                            break;
                        case EntityInvalidationEvent.DELETE:
                            removeConnector(id);
                            break;
                        }
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Error processing change for connector oid " + id + ": " + ExceptionUtils.getMessage(t), t);
                        connectorErrors.put(id, t);
                    }
                }
            }
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

    public void addHttpConnector(int port, Map<String, Object> attrs) throws ListenerException {
        Connector c = embedded.createConnector((String)null, port, "http");
        c.setEnableLookups(false);
        setConnectorAttributes(c, attrs);

        embedded.addConnector(c);
        try {
            c.start();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new ListenerException("Unable to start HTTPS listener: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public void addHttpsConnector(int port,
                                  Map<String, Object> attrs)
            throws ListenerException
    {
        Connector c = embedded.createConnector((String)null, port, "https");
        c.setScheme("https");
        c.setProperty("SSLEnabled","true");
        c.setSecure(true);
        c.setEnableLookups(false);

        // If the connector is trying to add an encrypted password, decrypt it first.
        // If it can't be decrypted, ignore it and fall back to the password from keystore.properties.
        Object kspass = attrs.get(HttpTransportModule.CONNECTOR_ATTR_KEYSTORE_PASS);
        if (kspass != null && masterPasswordManager.looksLikeEncryptedPassword(kspass.toString())) {
            try {
                char[] decrypted = masterPasswordManager.decryptPassword(kspass.toString());
                attrs.put(HttpTransportModule.CONNECTOR_ATTR_KEYSTORE_PASS, new String(decrypted));
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to decrypt encrypted password in server.xml -- falling back to password from keystore.properties: " + ExceptionUtils.getMessage(e));
                attrs.remove(HttpTransportModule.CONNECTOR_ATTR_KEYSTORE_PASS);
            }
        }

        attrs.putAll(attrs);
        setConnectorAttributes(c, attrs);

        embedded.addConnector(c);
        try {
            c.start();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new ListenerException("Unable to start HTTPS listener: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void setConnectorAttributes(Connector c, Map<String, Object> attrs) {
        for (Map.Entry<String, Object> entry : attrs.entrySet())
            c.setAttribute(entry.getKey(), entry.getValue());
    }

    Engine getEngine() {
        return engine;
    }

    Host getHost() {
        return host;
    }
}
