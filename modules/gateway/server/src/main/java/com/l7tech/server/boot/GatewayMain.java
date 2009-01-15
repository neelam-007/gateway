package com.l7tech.server.boot;

import com.l7tech.util.ExceptionUtils;

import java.util.logging.Logger;
import java.util.logging.Level;

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
            new GatewayBoot().runUntilShutdown();
            System.exit(0); // force exit even if there are non-daemon threads created by mistake (Bug #4384)
        } catch (Throwable e) {
            // init logger here to avoid initializing logging framework early on startup
            Logger.getLogger(GatewayMain.class.getName()).log(Level.WARNING, "Error starting server.", e);
            e.printStackTrace(System.err);
            System.err.println("\n\n\n**** Unable to start the server: " + ExceptionUtils.getMessage(e) + "\n\n\n");
            System.exit(77);
        }
    }
}
