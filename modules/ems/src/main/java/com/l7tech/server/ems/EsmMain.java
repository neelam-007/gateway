package com.l7tech.server.ems;

import static com.l7tech.server.util.SchemaUpdater.SchemaException;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.SyspropUtil;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.ManagedTimer;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.ConsoleHandler;
import java.util.concurrent.CountDownLatch;
import java.io.File;

/**
 * Process entry point that starts the EMS server.
 */
public class EsmMain {

    public static void main(String[] args) {
        if ( SyspropUtil.getProperty( "java.util.logging.manager" ) == null ) {
            System.setProperty("java.util.logging.manager", EsmLogManager.class.getName());
        }
        try {
            BuildInfo.setProduct( EsmMain.class.getPackage(), "Layer 7 Enterprise Service Manager" );
            System.setProperty("org.apache.cxf.nofastinfoset", "true");

            // configure logging if the logs directory is found, else leave console output
            if ( new File("var/logs").exists() ) {
                JdkLoggerConfigurator.configure("com.l7tech.server.ems", "com/l7tech/server/ems/resources/logging.properties", "etc/logging.properties", true);
            }
            if ( SyspropUtil.getBoolean( "com.l7tech.server.log.console", false ) ) {
                Logger.getLogger( "" ).addHandler( new ConsoleHandler() );
            }

            // create Logger after initializing logging framework
            Logger logger = Logger.getLogger(EsmMain.class.getName());
            logger.info("Starting " + BuildInfo.getLongBuildString());

            // configure background timer
            Background.installTimer( new ManagedTimer( "Default background timer" ) );

            if ( SyspropUtil.getBoolean( "com.l7tech.ems.development", false ) ) {
                System.setProperty( "com.l7tech.ems.showExceptions", "true" );
                System.setProperty( "com.l7tech.ems.enableHttpListener", "true" );
                ServerConfig serverConfig = ServerConfig.getInstance();
                if ( serverConfig.getProperty( "em.admin.user" ) == null ) {
                    logger.info( "Creating default administration account on startup." );
                    serverConfig.putProperty( "em.admin.user", "admin" );
                    serverConfig.putProperty( "em.admin.pass", "$6$S7Z3HcudYNsObgs8$SjwZ3xtCkSjXOK2vHfOVEg2dJES3cgvtIUdHbEN/KdCBXoI6uuPSbxTEwcH.av6lpcb1p6Lu.gFeIX04FBxiJ." );
                }
            }

            // derby init
            if ( SyspropUtil.getProperty( "derby.system.home" ) == null ) {
                System.setProperty("derby.system.home", "var/db");
            }

            // add shutdown handler
            final CountDownLatch shutdown = new CountDownLatch(1);
            final CountDownLatch exit = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
                @Override
                public void run() {
                    shutdown.countDown();
                    try {
                        exit.await(); // wait for shutdown to complete
                    } catch(InterruptedException ie) {
                        // exit
                    }
                }
            }));

            final Esm ems = new Esm();
            try {
                ems.start();
            } catch (Throwable t) {
                final SchemaException schemaException = ExceptionUtils.getCauseIfCausedBy( t, SchemaException.class );
                final Throwable messageThrowable;
                final Throwable stackThrowable;
                if ( schemaException != null ) {
                    messageThrowable = schemaException;
                    stackThrowable = schemaException.getCause();
                } else {
                    messageThrowable = stackThrowable = t;
                }

                final String msg = "Unable to start EMS: " + ExceptionUtils.getMessage(messageThrowable);
                logger.log(Level.SEVERE, msg, stackThrowable);
                System.err.println(msg);
                if (stackThrowable!=null) stackThrowable.printStackTrace(System.err);
                resetLogs();
                exit.countDown();
                System.exit(1);
                return;
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
