package com.l7tech.server.boot;

import com.l7tech.common.util.ExceptionUtils;

/**
 * An entry point that starts the Gateway server process and runs it until it is shut down.
 */
public class GatewayMain {
    public static void main(String[] args) {
        try {
            new GatewayBoot().runUntilShutdown();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.err.println("\n\n\n**** Unable to start the server: " + ExceptionUtils.getMessage(e) + "\n\n\n");
            System.exit(77);
        }
    }
}
