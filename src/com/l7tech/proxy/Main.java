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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;

/**
 * Begin execution of client proxy.
 * User: mike
 * Date: May 15, 2003
 * Time: 3:33:14 PM
 */
public class Main {

    /** Hardcoded (for now) URL of SSG. */
    private static String serverUrl = "http://localhost/cgi-bin/soapserv";

    private static HttpServer httpServer;

    /** Start a client proxy and run it until it's shut down. */
    public static void main(String[] argv) {
        httpServer = new HttpServer();
        SocketListener socketListener = new SocketListener();
        socketListener.setMaxThreads(100);
        socketListener.setMinThreads(6);
        socketListener.setPort(5555);
        HttpContext context = new HttpContext(httpServer, "/");
        RequestHandler requestHandler = new RequestHandler(serverUrl);
        context.addHandler(requestHandler);
        httpServer.addContext(context);

        // Hook up the Message Viewer window
        requestHandler.setRequestInterceptor(Gui.getInstance().getRequestInterceptor());

        try {
            httpServer.addListener(socketListener);
            httpServer.start();
        } catch (MultiException e) {
            System.err.println("Unable to start httpServer");
            e.printStackTrace();
            System.exit(2);
        }

        // Make sure the proxy stops when the GUI does.
        Gui.getInstance().setShutdownListener(new Gui.ShutdownListener() {
            public void guiShutdown() {
                try {
                    httpServer.stop();
                } catch (InterruptedException e) {
                }
                System.exit(0);
            }
        });

        Gui.getInstance().start();

        // We have nothing else for the main thread to do.
        return;
    }
}

