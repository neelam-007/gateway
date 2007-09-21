package com.l7tech.server.boot;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.server.LifecycleException;

import java.io.File;

/**
 * An entry point that starts the Gateway server process and runs it until it is shut down.
 */
public class GatewayMain {
    private static final String PROP_SERVER_HOME = "com.l7tech.server.home";

    private static File getServerHome() throws LifecycleException {
        String rootPath = System.getProperty(PROP_SERVER_HOME);
        if (rootPath == null || rootPath.trim().length() < 1) {
            // Try using the current directory
            File cwd = new File("").getAbsoluteFile();
            if (cwd.isDirectory()) {
                File inf = new File(cwd, "etc/inf");
                if (inf.isDirectory()) {
                    // Current directory it is.
                    rootPath = cwd.getAbsolutePath();
                    System.setProperty(PROP_SERVER_HOME, rootPath);
                }
            }

        }
        if (rootPath == null || rootPath.trim().length() < 1)
            throw new LifecycleException("System property not set: " + PROP_SERVER_HOME);
        if (!rootPath.endsWith("/"))
            rootPath = rootPath + "/";
        File file = new File(rootPath);
        if (!file.exists() || !file.isDirectory())
            throw new LifecycleException("Not a directory: " + rootPath + "   Check value of " + PROP_SERVER_HOME);
        return file;
    }

    public static void main(String[] args) {
        try {
            new GatewayBoot(getServerHome()).runUntilShutdown();            
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.err.println("\n\n\n**** Unable to start the server: " + ExceptionUtils.getMessage(e) + "\n\n\n");
            System.exit(77);
        }
    }
}
