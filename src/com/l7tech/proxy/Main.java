package com.l7tech.proxy;

import org.mortbay.util.InetAddrPort;
import org.mortbay.util.MultiException;
import org.mortbay.http.HttpServer;
import org.mortbay.http.HttpContext;
import org.mortbay.http.SocketListener;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

import com.l7tech.proxy.gui.Gui;
import com.l7tech.proxy.datamodel.Managers;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;

/**
 * Begin execution of client proxy along with an attached GUI.
 * User: mike
 * Date: May 15, 2003
 * Time: 3:33:14 PM
 */
public class Main {
    private static final int DEFAULT_PORT = 5555;
    private static final int MIN_THREADS = 4;
    private static final int MAX_THREADS = 20;

    private static ClientProxy clientProxy;

    /** Start a GUI-equipped client proxy and run it until it's shut down. */
    public static void main(final String[] argv) {
        clientProxy = new ClientProxy(Managers.getSsgManager(),
                                      DEFAULT_PORT,
                                      MIN_THREADS,
                                      MAX_THREADS);

        // Hook up the Message Viewer window
        clientProxy.getRequestHandler().setRequestInterceptor(Gui.getInstance().getRequestInterceptor());

        try {
            clientProxy.start();
        } catch (MultiException e) {
            Gui.errorMessage("Unable to start the Client Proxy: " + e);
            System.err.println("Unable to start httpServer");
            e.printStackTrace(System.err);
            System.exit(2);
        }

        // Make sure the proxy stops when the GUI does.
        Gui.getInstance().setShutdownListener(new Gui.ShutdownListener() {
            public void guiShutdown() {
                clientProxy.stop();
                System.exit(0);
            }
        });

        Gui.getInstance().start();

        // We have nothing else for the main thread to do.
        return;
    }
}

