package com.l7tech.server.boot;

import com.l7tech.server.LifecycleException;
import com.l7tech.server.RuntimeLifecycleException;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An entry point that starts the Gateway server process and runs it until it is shut down.
 * @noinspection UseOfSystemOutOrSystemErr,CallToSystemExit
 */
public class GatewayMain {
    private static final String JAXB_CLASS_TAILOR = "com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize";

    public static void main(String[] args) {
        try {
            if ( System.getProperty("java.util.logging.manager") == null ) {
                System.setProperty("java.util.logging.manager", GatewayBoot.GatewayLogManager.class.getName());
            }
            configureSecurityManager();
            configureOtherSystemProperties();
            new GatewayBoot().runUntilShutdown();
            System.exit(0); // force exit even if there are non-daemon threads created by mistake (Bug #4384)
        } catch (Throwable e) {
            // init logger here to avoid initializing logging framework early on startup
            Logger logger = Logger.getLogger(GatewayMain.class.getName());
            String message = "Error starting server : " + ExceptionUtils.getMessage(e);
            if ( e instanceof LifecycleException && e.getCause()==null ) {
                logger.log(Level.WARNING, message, ExceptionUtils.getDebugException(e));
            } else if (ExceptionUtils.getCauseIfCausedBy(e, RuntimeLifecycleException.class) != null) {
                RuntimeLifecycleException rle = ExceptionUtils.getCauseIfCausedBy(e, RuntimeLifecycleException.class);
                message = rle.getMessage();
            } else {
                logger.log(Level.WARNING, message, e);
                e.printStackTrace(System.err);
            }
            System.err.println("\n\n\n**** Unable to start the server: " + message + "\n\n\n");
            System.exit(77);
        }
    }

    private static void configureOtherSystemProperties() {
        // Configure system properties here that must be set extremely early, before we start creating the application context
        // Other system property defaults can be set in the resource file com/l7tech/server/resources/system.properties
        if ( null == System.getProperty( JAXB_CLASS_TAILOR ) ) {
            // This could probably be moved to the system.properties resource file
            System.setProperty( JAXB_CLASS_TAILOR, "true" );
        }
    }

    private static void configureSecurityManager() {
        if (!Boolean.getBoolean("com.l7tech.server.sm.noSecurityManager") && System.getSecurityManager() == null) {
            boolean f = org.apache.catalina.Globals.IS_SECURITY_ENABLED; // Ensure tomcat gets locked into non-SecurityManager behavior before we enable the SecurityManager
            if (Boolean.getBoolean("com.l7tech.server.sm.logOnly")) {
                configureLoggingSecurityManager(f);
            } else {
                configureDefaultSecurityManager();
            }
        }
    }

    private static void configureDefaultSecurityManager() {
        // Install the default SecurityManager -- assumes a policy has already been provided by environment or on command line
        System.setSecurityManager(new GatewaySecurityManager());
    }

    private static void configureLoggingSecurityManager(boolean f) {
        // Install a logging security manager
        final GatewayPermissiveLoggingSecurityManager sm = new GatewayPermissiveLoggingSecurityManager(f);
        System.setSecurityManager(sm);

        // Start logging
        final File logdir = new File(ConfigFactory.getProperty("logDirectory", "/tmp"));
        sm.periodicLog(new File(logdir, "ssgPermsDump"), 20611L);
    }
}
