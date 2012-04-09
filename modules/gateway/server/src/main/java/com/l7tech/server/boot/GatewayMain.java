package com.l7tech.server.boot;

import com.l7tech.server.LifecycleException;
import com.l7tech.server.RuntimeLifecycleException;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An entry point that starts the Gateway server process and runs it until it is shut down.
 * @noinspection UseOfSystemOutOrSystemErr,CallToSystemExit
 */
public class GatewayMain {
    public static void main(String[] args) {
        try {
            if ( System.getProperty("java.util.logging.manager") == null ) {
                System.setProperty("java.util.logging.manager", GatewayBoot.GatewayLogManager.class.getName());
            }
            if (!Boolean.getBoolean("com.l7tech.server.sm.noSecurityManager")) {
                boolean f = org.apache.catalina.Globals.IS_SECURITY_ENABLED; // Ensure tomcat gets locked into non-SM behavior before we install the SM
                final GatewayPermissiveLoggingSecurityManager sm = new GatewayPermissiveLoggingSecurityManager(f);
                System.setSecurityManager(sm);
                periodicLog(sm);
            }
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

    private static void periodicLog(final GatewayPermissiveLoggingSecurityManager sm) {
        final Thread permsDump = new Thread("SM Permissions Dump") {
            @Override
            public void run() {
                for (;;) {
                    try {
                        Thread.sleep(20611L);
                        dumpPerms(sm.getGrantedPermissions());
                    } catch (InterruptedException e) {
                        System.err.println("SM Permissions Dump: interrupted");
                        return;
                    } catch (Exception e) {
                        System.err.println("SM Permissions Dump: error: " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                }
            }
        };
        permsDump.setDaemon(true);
        permsDump.start();
    }

    private static void dumpPerms(final Set<String> grantedPermissions) throws IOException {
        File logdir = new File(ConfigFactory.getProperty("logDirectory", "/tmp"));
        File dumpfile = new File(logdir, "ssgPermsDump");
        FileUtils.saveFileSafely(dumpfile.getCanonicalPath(), new FileUtils.Saver() {
            @Override
            public void doSave(FileOutputStream fos) throws IOException {
                XMLEncoder encoder = null;
                try {
                    encoder = new XMLEncoder(fos);
                    encoder.writeObject(grantedPermissions);
                    encoder.close();
                    encoder = null;
                } finally {
                    if (encoder != null)
                        encoder.close();
                }
            }
        });
    }
}
