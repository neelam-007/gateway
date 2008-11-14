package com.l7tech.server.ems;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.SyspropUtil;
import com.l7tech.server.ServerConfig;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

/**
 * Process entry point that starts the EMS server.
 */
public class EmsMain {
    private static final Logger logger = Logger.getLogger(EmsMain.class.getName());

    public static void main(String[] args) {
        BuildInfo.setProduct( EmsMain.class.getPackage(), "Layer 7 Enterprise Service Manager" );
        System.setProperty("org.apache.cxf.nofastinfoset", "true");

        // initialize config location
        //TODO [steve] rename the ems_config.properties file to emconfig.properties for consistency
        System.setProperty(ServerConfig.PROPS_RESOURCE_PROPERTY, "/com/l7tech/server/ems/resources/ems_config.properties");
        if ( new File("var").exists() ) {
            System.setProperty(ServerConfig.PROPS_OVER_PATH_PROPERTY, "var/emconfig.properties");
        }

        // configure logging if the logs directory is found, else leave console output
        if ( new File("var/logs").exists() ) {
            JdkLoggerConfigurator.configure("com.l7tech.server.ems", "com/l7tech/server/ems/resources/logging.properties");
        }

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

        // add shutdown handler
        final Object shutdown = new Object();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            public void run() {
                synchronized(shutdown) {
                    shutdown.notify();
                }
            }
        }));

        final Ems ems = new Ems();
        try {
            ems.start();
        } catch (Throwable t) {
            final String msg = "Unable to start EMS: " + ExceptionUtils.getMessage(t);
            logger.log(Level.SEVERE, msg, t);
            System.err.println(msg);
            t.printStackTrace(System.err);
            System.exit(1);
        }

        try {
            waitForShutdown(shutdown);
            logger.info("Shutting down.");
            ems.stop();
        } catch (Exception t) {
            final String msg = "Exception while waiting for EMS shutdown: " + ExceptionUtils.getMessage(t);
            logger.log(Level.SEVERE, msg, t);
            System.err.println(msg);
            t.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static void waitForShutdown(final Object shutdown) throws Exception {
        // Wait forever until server is shut down
        synchronized (shutdown) {
            try {
                shutdown.wait();
            } catch (InterruptedException e) {
                throw new Exception("thread interrupted");
            }
        }
    }
}
