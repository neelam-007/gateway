package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgFinderImpl;
import org.apache.log4j.Category;

/**
 * Begin execution of daemon-mode (no UI at all) client proxy.
 * User: mike
 * Date: May 15, 2003
 * Time: 3:33:14 PM
 */
public class Main {
    private static final Category log = Category.getInstance(Main.class);
    private static final int DEFAULT_PORT = 7700;
    private static final int MIN_THREADS = 4;
    private static final int MAX_THREADS = 20;

    private static ClientProxy clientProxy;

    private static int getIntProperty(String name, int def) {
        try {
            String p = System.getProperty(name);
            if (p == null || p.length() < 1)
                return def;
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Start a text-only client proxy and run it until it's shut down. */
    public static void main(final String[] argv) {
        log.info("Starting Layer7 Client Proxy in daemon mode");

        int port = getIntProperty("com.l7tech.proxy.listener.port", DEFAULT_PORT);
        int minThreads = getIntProperty("com.l7tech.proxy.listener.minthreads", MIN_THREADS);
        int maxThreads = getIntProperty("com.l7tech.proxy.listener.maxthreads", MAX_THREADS);

        clientProxy = new ClientProxy(SsgFinderImpl.getSsgFinderImpl(),
                                      new MessageProcessor(Managers.getPolicyManager()),
                                      port,
                                      minThreads,
                                      maxThreads);

        // Hook up the Message Logger facility
        clientProxy.getRequestHandler().setRequestInterceptor(new MessageLogger());

        try {
            clientProxy.start();
        } catch (Exception e) {
            log.error("Unable to start Layer7 Client Proxy: " + e);
            log.error(e);
            System.exit(2);
        }

        // We have nothing else for the main thread to do.
        return;
    }
}

