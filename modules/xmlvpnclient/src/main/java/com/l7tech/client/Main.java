package com.l7tech.client;

import com.l7tech.kerberos.KerberosClient;
import com.l7tech.proxy.Constants;
import com.l7tech.proxy.MessageLogger;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.SsgFinderImpl;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.TimerTask;
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
    protected static final int CONFIG_CHECK_TIME = 5000;  // check for config file changes every 5 seconds
    protected static final int DEFAULT_PORT = 7700;
    protected static final int MIN_THREADS = 5;
    protected static final int MAX_THREADS = 300;

    public static final String PROPERTY_LISTENER_PORT = "com.l7tech.proxy.listener.port";
    public static final String PROPERTY_LISTENER_MINTHREADS = "com.l7tech.proxy.listener.minthreads";
    public static final String PROPERTY_LISTENER_MAXTHREADS = "com.l7tech.proxy.listener.maxthreads";

    /**
     * Initialize logging.  Attempts to mkdir ClientProxy.PROXY_CONFIG first, so the log file
     * will have somewhere to go.  Also calls JceProvider.init().
     */
    protected static void initLogging() {
        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");

        // Prepare .l7tech directory before initializing logging (Bug #1288)
        new File(Ssg.PROXY_CONFIG).mkdirs(); // expected to fail on all but the very first execution

        String instDirPath = new File(Ssg.PROXY_CONFIG, "../logging.properties").getAbsolutePath();
        JdkLoggerConfigurator.configure("com.l7tech.proxy", "com/l7tech/proxy/resources/logging.properties", instDirPath);
        JceProvider.init();
    }

    /**
     * Set up kerberos configuration.
     */
    protected static void initConfig() {
        File configDir = new File(Ssg.PROXY_CONFIG);
        File loginConfig = new File(configDir, "login.config");

        System.setProperty("java.security.auth.login.config", loginConfig.getAbsolutePath());

        // ensures any missing configuration is initialized.
        new KerberosClient();

        // read properties file from an optional settings file
        String propertiesPath = Ssg.PROXY_CONFIG + File.separator + "xvc-system.properties";
        File propsf = new File(propertiesPath);
        if (propsf.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(propsf);
                Properties props = new Properties();
                props.load( fis );
                if ( props.size() > 0 ) {
                    for ( Object key : props.keySet() ) {
                        System.setProperty(key.toString(), props.getProperty(key.toString()));
                    }
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Error reading " + propertiesPath, e);
            } finally {
                ResourceUtils.closeQuietly( fis );
            }
        }
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
     * Create a new client proxy.  Uses the system properties for port and threads.
     *
     * @param ssgFinder the SsgFinder to use for the new ClientProxy.
     * @return the new ClientProxy instance, which is also saved in a member field.
     */
    protected static ClientProxy createClientProxy(SsgFinder ssgFinder) {
        int port = getBindPort();
        int minThreads = ConfigFactory.getIntProperty( PROPERTY_LISTENER_MINTHREADS, MIN_THREADS );
        int maxThreads = ConfigFactory.getIntProperty( PROPERTY_LISTENER_MAXTHREADS, MAX_THREADS );

        ClientProxy clientProxy = new ClientProxy(ssgFinder,
                                                  new MessageProcessor(),
                                                  port,
                                                  minThreads,
                                                  maxThreads);

        // Clean out attachment directory after we've established that no other Client Proxy was running
        if (!Ssg.ATTACHMENT_DIR.exists())
            Ssg.ATTACHMENT_DIR.mkdir();
        deleteOldAttachments(Ssg.ATTACHMENT_DIR);

        return clientProxy;
    }

    protected static int getBindPort() {
        return ConfigFactory.getIntProperty( PROPERTY_LISTENER_PORT, DEFAULT_PORT );
    }

    /**
     * Start a text-only client proxy and then return immediately.
     * This method will either start the client proxy and then return immediately (while background threads
     * hang around to do the work), or will log an error and exit the process with System.exit(2) if the
     * proxy could not be started.
     */
    public static void main(final String[] argv) {
        initLogging();
        initConfig();
        log.info("Starting CA API Gateway "+ Constants.APP_NAME +" in non-interactive mode; " + BuildInfo.getLongBuildString());

        final SsgFinderImpl ssgFinderImpl = SsgFinderImpl.getSsgFinderImpl();
        final ClientProxy clientProxy = createClientProxy(ssgFinderImpl);

        // Watch for config file changes
        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                ssgFinderImpl.loadIfChanged();
            }
        }, CONFIG_CHECK_TIME, CONFIG_CHECK_TIME);

        // Hook up the Message Logger facility
        clientProxy.getRequestHandler().setRequestInterceptor(new MessageLogger());

        // Start the client proxy or exit.
        try {
            clientProxy.start();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unable to start CA API Gateway " + Constants.APP_NAME, e);
            System.exit(2);
        }

        // We have nothing else for the main thread to do, so we'll just allow it to die.
        return;
    }
}
