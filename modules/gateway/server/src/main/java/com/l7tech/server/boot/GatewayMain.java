package com.l7tech.server.boot;

import com.l7tech.util.ExceptionUtils;

/**
 * An entry point that starts the Gateway server process and runs it until it is shut down.
 * @noinspection UseOfSystemOutOrSystemErr,CallToSystemExit
 */
public class GatewayMain {
    public static void main(String[] args) {
        try {
            new GatewayBoot().runUntilShutdown();
            System.exit(0); // force exit even if there are non-daemon threads created by mistake (Bug #4384)
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.err.println("\n\n\n**** Unable to start the server: " + ExceptionUtils.getMessage(e) + "\n\n\n");
            System.exit(77);
        }
    }
}
