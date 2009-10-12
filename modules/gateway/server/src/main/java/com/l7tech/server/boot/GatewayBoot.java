package com.l7tech.server.boot;

import com.l7tech.gateway.common.Component;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.JceUtil;
import com.l7tech.security.prov.StrongCryptoNotAvailableException;
import com.l7tech.server.BootProcess;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.log.JdkLogConfig;
import com.l7tech.server.util.FirewallUtils;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.SyspropUtil;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.PooledDataSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Security;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Object that represents a complete, running Gateway instance.
 * 
 * TODO: merge this with BootProcess
 */
public class GatewayBoot {
    protected static final Logger logger = Logger.getLogger(GatewayBoot.class.getName());
    private static final long DB_CHECK_DELAY = 30;
    private static final String SYSPROP_STARTUPCHECKS = "com.l7tech.server.performStartupChecks";

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean destroyRequested = new AtomicBoolean(false);

    private ClassPathXmlApplicationContext applicationContext;
    private ShutdownWatcher shutdowner;

    //code required to ensure that if the HSM is enabled, the correct security providers get set up.
    //we'll do this here so it gets done early enough that any calls to DefaultKey.getSslInfo() result in using the HSM
    //if it's enabled.
    public static final String SYSPROP_ENABLE_HSM = "com.l7tech.server.sca.enable";

    public static final String PKCS11_CFG_FILE = "/opt/SecureSpan/Appliance/etc/pkcs11_linux.cfg";

    public static final String[] HSM_SECURITY_PROVIDERS =
            {
                "sun.security.pkcs11.SunPKCS11 " + PKCS11_CFG_FILE,
                "sun.security.provider.Sun",
                "com.sun.net.ssl.internal.ssl.Provider",
                "com.sun.crypto.provider.SunJCE"
            };
    static {
        JdkLogConfig.configureLogging();
        setupSecurityProviders();
    }
    private static void setupSecurityProviders() {
        //setup the security providers needed by the SCA6000 HSM if it's enabled (via a system property)
        boolean hsmEnabled = Boolean.getBoolean(SYSPROP_ENABLE_HSM);

        if (hsmEnabled) {

            for (Provider provider : Security.getProviders()) {
                Security.removeProvider(provider.getName());
            }

            try {
                prepareProviders(HSM_SECURITY_PROVIDERS);
                System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.PKCS11_ENGINE);
            } catch (LifecycleException e) {
                throw new RuntimeException("Could not start the server. The HSM is enabled, but there was an error preparing the security providers. ", e);
            }
        }
    }

    private static void prepareProviders(String[] securityProviders) throws LifecycleException {
        for (String providerName : securityProviders) {
            Provider p;
            try {
                if (providerName.contains(" ")) {
                    String[] splitz = providerName.split(" ");
                    logger.info("Adding " + splitz[0]);
                    Class providerClass = Class.forName(splitz[0]);
                    Constructor ctor = providerClass.getConstructor(String.class);
                    p = (Provider) ctor.newInstance(splitz[1]);

                } else {
                    p = (Provider) Class.forName(providerName).newInstance();
                }
                Security.addProvider(p);
            } catch (ClassNotFoundException e) {
                throw new LifecycleException("Error while instantiating the required security provider: " + providerName + ": " + ExceptionUtils.getMessage(e), e);
            } catch (InvocationTargetException e) {
                throw new LifecycleException("Error while instantiating the required security provider: " + providerName + ": " + ExceptionUtils.getMessage(e), e);
            } catch (NoSuchMethodException e) {
                throw new LifecycleException("Error while instantiating the required security provider: " + providerName + ": " + ExceptionUtils.getMessage(e), e);
            } catch (IllegalAccessException e) {
                throw new LifecycleException("Error while instantiating the required security provider: " + providerName + ": " + ExceptionUtils.getMessage(e), e);
            } catch (InstantiationException e) {
                throw new LifecycleException("Error while instantiating the required security provider: " + providerName + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
    }


    /**
     * Create a Gateway instance but do not initialize or start it.
     * Set the com.l7tech.server.partitionName system property if the partition name is other than "default_".
     */
    public GatewayBoot() {
    }

    public void start() throws LifecycleException {
        if (!running.compareAndSet(false, true)) {
            // Already started
            return;
        }

        logger.info("Starting " + BuildInfo.getLongBuildString());
        destroyRequested.set(false);

        // This thread is responsible for attempting to start the server, and for clearing "running" flag if it fails
        boolean itworked = false;
        try {
            ServerConfig.getInstance().getLocalDirectoryProperty(ServerConfig.PARAM_LOG_DIRECTORY, true);
            FirewallUtils.initializeFirewall();
            spawnDbWarner();
            createApplicationContext();
            String ipAddress = startBootProcess();
            startListeners(ipAddress);
            itworked = true;
        } finally {
            if (!itworked)
                running.set(false);
        }
    }

    /**
     * Stop and destroy this Gateway instance.
     * This Gateway instance can not be started again once it is destroyed and should be discarded.
     *
     * @throws LifecycleException if there is a problem shutting down the server
     */
    public void destroy() throws LifecycleException {
        destroyRequested.set(true);
        if (!running.get())
            return;

        final int shutdownDelay = ServerConfig.getInstance().getIntProperty("ssg.shutdownDelay", 3);
        logger.info("Starting shutdown.");
        stopBootProcess();
        try {
            for ( int i=0; i<shutdownDelay; i++ ) {
                logger.info("Continuing shutdown in " +(shutdownDelay-i)+ "s.");
                Thread.sleep(1000);
            }
        } catch ( InterruptedException e ) {
            logger.info("Continuing shutdown (interrupted).");                        
        }
        applicationContext.close();
        running.set(false);
    }

    /**
     * Start the Gateway and run it until it is shut down.
     * This method does not return until the Gateway has shut down normally.
     *
     * @throws LifecycleException if the Gateway could not be started due to a failure of some kind,
     *                                              or if there was an exception while attempting a normal shutdown.
     */
    public void runUntilShutdown() throws LifecycleException {
        final CountDownLatch shutdownlatch = new CountDownLatch(1);
        final CountDownLatch exitLatch = new CountDownLatch(1);

        preFlightCheck();
        start();
        addShutdownHook( shutdownlatch, exitLatch );
        waitForShutdown( shutdownlatch );
        try {
            destroy();
        } finally {
            notifyExit( exitLatch );
        }
    }

    // Launch a background thread that warns if no DB connections appear within a reasonable period of time (Bug #4271)
    private void spawnDbWarner() {
        Thread dbcheck = new Thread("Database Check") {
            @Override
            public void run() {
                try {
                    Thread.sleep(DB_CHECK_DELAY * 1000L);
                    if (destroyRequested.get())
                        return;
                    int connections = getNumDbConnections();
                    if (connections >= 1) {
                        logger.log(Level.FINE, "Database check: " + connections + " database connections open after " + DB_CHECK_DELAY + " seconds");
                        return;
                    }

                    logger.log(Level.SEVERE, "WARNING: No database connections open after " + DB_CHECK_DELAY + " seconds; possible DB connection failure?");
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Unable to check for database connections: " + ExceptionUtils.getMessage(t), t);
                }
            }
        };
        dbcheck.setDaemon(true);
        dbcheck.start();
    }

    private static int getNumDbConnections() throws SQLException {
        int connections = 0;
        //noinspection unchecked
        Set<PooledDataSource> pools = C3P0Registry.getPooledDataSources();
        for ( PooledDataSource pool : pools )
            connections += pool.getNumConnectionsAllUsers();
        return connections;
    }

    private void createApplicationContext() {
        applicationContext = new ClassPathXmlApplicationContext(new String[]{
                "com/l7tech/server/resources/dataAccessContext.xml",
                "com/l7tech/server/resources/ssgApplicationContext.xml",
                "com/l7tech/server/resources/adminContext.xml",
                "com/l7tech/server/resources/cxfSupportContext.xml",
        });
        shutdowner = (ShutdownWatcher)applicationContext.getBean("ssgShutdown", ShutdownWatcher.class);
    }

    private String startBootProcess() throws LifecycleException {
        BootProcess boot = (BootProcess)applicationContext.getBean("ssgBoot", BootProcess.class);
        boot.start();
        return boot.getIpAddress();
    }

    private void stopBootProcess() {
        try {
            BootProcess boot = (BootProcess)applicationContext.getBean("ssgBoot", BootProcess.class);
            boot.stop();
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error shutting down boot process '"+ExceptionUtils.getMessage(e)+"'.", e );
        }
    }

    private void startListeners(String ipAddress) {
        applicationContext.publishEvent(new ReadyForMessages(this, Component.GW_SERVER, ipAddress));
    }

    private void addShutdownHook( final CountDownLatch shutdown, final CountDownLatch exitSync ) {
        // add hook for shutdown bean
        shutdowner.setShutdownLatch( shutdown );

        // add shutdown handler
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            /**
             * Run before shutdown, we need to be ready to exit when this thread dies.
             */
            @Override
            public void run() {
                // notify to start shutdown process
                shutdown.countDown();

                // wait for shutdown to complete, we'll assume that the process is killed if it takes too long
                // but we don't want to block forever so timeout after 5 mins
                try {
                    exitSync.await( 5, TimeUnit.MINUTES );
                } catch (InterruptedException e) {
                    // thread exits immediately
                }

                LogManager logManager = LogManager.getLogManager();
                if ( logManager instanceof GatewayLogManager ) {
                    ((GatewayLogManager)logManager).resetLogs();
                }                
            }
        }));
    }

    private void preFlightCheck() throws LifecycleException {
        if (SyspropUtil.getBoolean(SYSPROP_STARTUPCHECKS, true)) {
            // check strong crypto is available
            try {
                JceUtil.requireStrongCryptoEnabledInJvm();
            } catch (StrongCryptoNotAvailableException e) {
                throw new LifecycleException("Strong cryptography not available. Please update JDK to enable strong cryptography.");
            } catch (GeneralSecurityException e) {
                logger.log(Level.WARNING, "Unexpected error when checking for strong cryptography in JDK '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
            }
        }
    }

    private static void notifyExit( final CountDownLatch exitSync ){
        // notify to start shutdown process
        exitSync.countDown();
    }

    private static void waitForShutdown( final CountDownLatch shutdown ) {
        // Wait forever until server is shut down
        try {
            shutdown.await();
        } catch (InterruptedException e) {
            logger.info("Shutting down due to interrruped.");
        }
    }

    /**
     * This prevents JDK logging shutdown when the JUL shutdown hook is invoked.
     *
     * <p>The Gateway will reset the underlying manager when shutdown of components
     * is completed.</p>
     *
     * Perhaps inspired by JBossJDKLogManager.
     */
    public static final class GatewayLogManager extends LogManager implements JdkLoggerConfigurator.ResettableLogManager {
        @Override
        public void reset() throws SecurityException {
        }

        @Override
        public void resetLogs() throws SecurityException {
            super.reset();
        }
    }
}
