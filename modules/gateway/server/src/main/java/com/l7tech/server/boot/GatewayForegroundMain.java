package com.l7tech.server.boot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;

/**
 * An entry point that runs a Gateway in the foreground, doing a clean shutdown when the "stop" command
 * is read from STDIN.
 * <p/>
 * This entry point is intended to be most useful for starting the Gateway for testing from an IDE,
 * when it is desired to be able to perform a clean shutdown when finished.
 */
public class GatewayForegroundMain {
    public static void main(String[] args) throws Exception {
        final GatewayBoot gateway = new GatewayBoot();
        gateway.start();
        waitForShutdownCommand();
        gateway.destroy();

        System.out.println("Main thread terminating.");
    }

    private static void waitForShutdownCommand() throws IOException {
        PrintStream out = System.out;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        out.println("Enter the command 'stop' to trigger a clean shutdown.");
        for (;;) {
            out.print("SSG> ");
            out.flush();
            String line = in.readLine();
            if (line == null) // EOF
                break;

            if (line.matches("^\\s*$"))
                continue;

            if (line.matches("^\\s*stop\\s*$")) {
                out.println("Shutting down.");
                break;
            }

            out.println("Unknown command.");
        }
    }
}
