package com.l7tech.proxy;

import org.mortbay.util.InetAddrPort;
import org.mortbay.util.MultiException;
import org.mortbay.http.HttpServer;
import org.mortbay.http.HttpContext;
import org.mortbay.http.SocketListener;

import java.io.IOException;

import com.l7tech.proxy.gui.Gui;

/**
 * Begin execution of client proxy.
 * User: mike
 * Date: May 15, 2003
 * Time: 3:33:14 PM
 */
public class Main {

    /** Hardcoded (for now) URL of SSG. */
    private static String serverUrl = "http://localhost:9898/server";

    private static Gui gui = new Gui();

    /** Start a client proxy and run it until it's shut down. */
    public static void main(String[] argv) {
        HttpServer server = new HttpServer();
        SocketListener socketListener = new SocketListener();
        socketListener.setMaxThreads(100);
        socketListener.setMinThreads(6);
        socketListener.setPort(5555);
        HttpContext context = new HttpContext(server, "/");
        RequestHandler requestHandler = new RequestHandler(serverUrl);
        context.addHandler(requestHandler);
        server.addContext(context);

        // Hook up the Message Viewer window
        requestHandler.setRequestInterceptor(gui.getRequestInterceptor());

        try {
            server.addListener(socketListener);
            server.start();
        } catch (MultiException e) {
            System.err.println("Unable to start server");
            e.printStackTrace();
        }

        // Run gui until shutdown
        try {
            gui.run();
            server.stop(true);
        } catch (InterruptedException e) {
        } catch (Exception e) {
            System.err.println("Exception while running GUI");
            e.printStackTrace();
        }
    }
}

