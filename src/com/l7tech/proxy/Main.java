package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.Managers;
import org.apache.log4j.Category;
import org.mortbay.util.MultiException;

/**
 * Begin execution of daemon-mode (no UI at all) client proxy.
 * User: mike
 * Date: May 15, 2003
 * Time: 3:33:14 PM
 */
public class Main {
    private static final Category log = Category.getInstance(Main.class);
    private static final int DEFAULT_PORT = 5555;
    private static final int MIN_THREADS = 4;
    private static final int MAX_THREADS = 20;

    private static ClientProxy clientProxy;

    /** Start a GUI-equipped client proxy and run it until it's shut down. */
    public static void main(final String[] argv) {
        log.info("Starting Layer7 Client Proxy in daemon mode");

        clientProxy = new ClientProxy(Managers.getSsgManager(),
                                      new MessageProcessor(Managers.getPolicyManager()),
                                      DEFAULT_PORT,
                                      MIN_THREADS,
                                      MAX_THREADS);

        // Hook up the Message Logger facility
        clientProxy.getRequestHandler().setRequestInterceptor(new MessageLogger());

        try {
            clientProxy.start();
        } catch (MultiException e) {
            log.error("Unable to start Layer7 Client Proxy: " + e);
            log.error(e);
            System.exit(2);
        }

        // We have nothing else for the main thread to do.
        return;
    }
}

