package com.l7tech.proxy;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.SsgFinderImpl;
import com.l7tech.proxy.processor.MessageProcessor;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Begin execution of daemon-mode (no UI at all) client proxy.
 * User: mike
 * Date: May 15, 2003
 * Time: 3:33:14 PM
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());
    protected static final int DEFAULT_PORT = 7700;
    protected static final int MIN_THREADS = 5;
    protected static final int MAX_THREADS = 300;

    private static ClientProxy clientProxy = null;
    public static final String PROPERTY_LISTENER_PORT = "com.l7tech.proxy.listener.port";
    public static final String PROPERTY_LISTENER_MINTHREADS = "com.l7tech.proxy.listener.minthreads";
    public static final String PROPERTY_LISTENER_MAXTHREADS = "com.l7tech.proxy.listener.maxthreads";

    /** @return the specified system property as an int, with the specified default. */
    protected static int getIntProperty(String name, int def) {
        try {
            String p = System.getProperty(name);
            if (p == null || p.length() < 1) {
                log.log(Level.FINE, "System property " + name + " is not set.  Using default of " + def);
                return def;
            }
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            log.log(Level.WARNING, "System property " + name + " is not a valid int.  Using default of " + def);
            return def;
        }
    }

    /**
     * Initialize logging.  Attempts to mkdir ClientProxy.PROXY_CONFIG first, so the log file
     * will have somewhere to go.  Also calls JceProvider.init().
     */
    protected static void initLogging() {
        // apache logging layer to use the jdk logger
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

        // Prepare .l7tech directory before initializing logging (Bug #1288)
        new File(ClientProxy.PROXY_CONFIG).mkdirs(); // expected to fail on all but the very first execution

        JdkLoggerConfigurator.configure("com.l7tech.proxy", "com/l7tech/proxy/resources/logging.properties");
        JceProvider.init();
    }

    private static void deleteOldAttachments(File attachmentDir) {
        File[] goners = attachmentDir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                String local = pathname.getName();
                return local != null && local.startsWith("att") && local.endsWith(".part");
            }
        });

        for (int i = 0; i < goners.length; i++) {
            File goner = goners[i];
            log.info("Deleting leftover attachment cache file: " + goner.toString());
            goner.delete();
        }
    }

    /**
     * Create the client proxy.  Uses the system properties for port and threads.
     * @param ssgFinder the SsgFinder to use for the new ClientProxy.
     * @return the new ClientProxy instance, which is also saved in a member field.
     * @throws IllegalStateException if a client proxy instance has already been created.
     */
    protected static ClientProxy createClientProxy(SsgFinder ssgFinder) {
        if (clientProxy != null) throw new IllegalStateException("client proxy already created");
        int port = getIntProperty(PROPERTY_LISTENER_PORT, DEFAULT_PORT);
        int minThreads = getIntProperty(PROPERTY_LISTENER_MINTHREADS, MIN_THREADS);
        int maxThreads = getIntProperty(PROPERTY_LISTENER_MAXTHREADS, MAX_THREADS);

        clientProxy = new ClientProxy(ssgFinder,
          new MessageProcessor(Managers.getPolicyManager()),
          port,
          minThreads,
          maxThreads);

        // Clean out attachment directory after we've established that no other Client Proxy was running
        if (!ClientProxy.ATTACHMENT_DIR.exists())
            ClientProxy.ATTACHMENT_DIR.mkdir();
        deleteOldAttachments(ClientProxy.ATTACHMENT_DIR);

        return clientProxy;
    }

    /** @return the current clientProxy instance if createClientProxy() has been called; otherwise null. */
    protected static ClientProxy getClientProxy() {
        return clientProxy;
    }

    /**
     * Start a text-only client proxy and then return immediately.
     * This method will either start the client proxy and then return immediately (while background threads
     * hang around to do the work), or will log an error and exit the process with System.exit(2) if the
     * proxy could not be started.
     */
    public static void main(final String[] argv) {
        initLogging();
        log.info("Starting SecureSpan Bridge in non-interactive mode; " + BuildInfo.getLongBuildString());

        createClientProxy(SsgFinderImpl.getSsgFinderImpl());

        // Hook up the Message Logger facility
        getClientProxy().getRequestHandler().setRequestInterceptor(new MessageLogger());

        // Start the client proxy or exit.
        try {
            getClientProxy().start();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unable to start Layer 7 SecureSpan Bridge", e);
            System.exit(2);
        }

        // We have nothing else for the main thread to do, so we'll just allow it to die.
        return;
    }
}
