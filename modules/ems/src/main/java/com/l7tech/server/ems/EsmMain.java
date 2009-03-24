package com.l7tech.server.ems;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.SyspropUtil;
import com.l7tech.server.ServerConfig;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.concurrent.CountDownLatch;
import java.io.File;

/**
 * Process entry point that starts the EMS server.
 */
public class EsmMain {

    public static void main(String[] args) {
        if ( System.getProperty("java.util.logging.manager") == null ) {
            System.setProperty("java.util.logging.manager", EsmLogManager.class.getName());
        }
        try {
            BuildInfo.setProduct( EsmMain.class.getPackage(), "Layer 7 Enterprise Service Manager" );
            System.setProperty("org.apache.cxf.nofastinfoset", "true");

            // initialize config location
            System.setProperty(ServerConfig.PROPS_RESOURCE_PROPERTY, "/com/l7tech/server/ems/resources/emconfig.properties");
            if ( new File("var").exists() ) {
                System.setProperty(ServerConfig.PROPS_OVER_PATH_PROPERTY, "var/emconfig.properties");
            }

            // configure logging if the logs directory is found, else leave console output
            if ( new File("var/logs").exists() ) {
                JdkLoggerConfigurator.configure("com.l7tech.server.ems", "com/l7tech/server/ems/resources/logging.properties", "etc/logging.properties", true, true);
            }

            // create Logger after initializing logging framework
            Logger logger = Logger.getLogger(EsmMain.class.getName());
            logger.info("Starting " + BuildInfo.getLongBuildString());

            if ( SyspropUtil.getBoolean("com.l7tech.ems.development") ) {
                System.setProperty("com.l7tech.ems.showExceptions", "true");
                System.setProperty("com.l7tech.ems.enableHttpListener", "true");
                ServerConfig serverConfig = ServerConfig.getInstance();
                if ( serverConfig.getProperty("em.admin.user") == null ) {
                    logger.info("Creating default administration account on startup.");
                    serverConfig.putProperty("em.admin.user", "admin");
                    serverConfig.putProperty("em.admin.pass", "a41306e4b1b5858d3e3d705dd2e738e2");
                }
            }

            // derby init
            if ( System.getProperty("derby.system.home") == null ) {
                System.setProperty("derby.system.home", "var/db");
            }

            // add shutdown handler
            final CountDownLatch shutdown = new CountDownLatch(1);
            final CountDownLatch exit = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
                @Override
                public void run() {
                    synchronized(shutdown) {
                        shutdown.countDown();
                        try {
                            exit.await(); // wait for shutdown to complete
                        } catch(InterruptedException ie) {
                            // exit
                        }
                    }
                }
            }));

            final Esm ems = new Esm();
            try {
                ems.start();
            } catch (Throwable t) {
                final String msg = "Unable to start EMS: " + ExceptionUtils.getMessage(t);
                logger.log(Level.SEVERE, msg, t);
                System.err.println(msg);
                t.printStackTrace(System.err);
                resetLogs();
                System.exit(1);
            }

            try {
                waitForShutdown(shutdown);
                logger.info("Shutting down.");
                ems.stop();
                logger.info("Main thread exiting.");
            } catch (Exception t) {
                final String msg = "Exception while waiting for EMS shutdown: " + ExceptionUtils.getMessage(t);
                logger.log(Level.SEVERE, msg, t);
                System.err.println(msg);
                t.printStackTrace(System.err);
                resetLogs();
                System.exit(2);
            } finally {
                exit.countDown();
            }
        } finally {
            resetLogs();
        }
    }

    private static void resetLogs() {
        LogManager manager = LogManager.getLogManager();
        if ( manager instanceof EsmLogManager ) {
            ((EsmLogManager)manager).resetLogs();
        }
    }

    private static void waitForShutdown(final CountDownLatch shutdown) throws Exception {
        // Wait forever until server is shut down
        try {
            shutdown.await();
        } catch (InterruptedException e) {
            throw new Exception("thread interrupted");
        }
    }

    /**
     * This prevents JDK logging shutdown when the JUL shutdown hook is invoked.
     *
     * <p>The ESM will reset the underlying manager when shutdown of components
     * is completed.</p>
     *
     * Perhaps inspired by JBossJDKLogManager.
     */
    public static final class EsmLogManager extends LogManager implements JdkLoggerConfigurator.ResettableLogManager {
        @Override
        public void reset() throws SecurityException {
        }

        @Override
        public void resetLogs() throws SecurityException {
            super.reset();
        }
    }

}
