package com.l7tech.proxy.gui;

import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.MessageProcessor;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.SsgManagerImpl;
import org.apache.log4j.Category;
import org.mortbay.util.MultiException;

import java.net.BindException;

/**
 * Begin execution of client proxy along with an attached GUI.
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
        log.info("Starting Layer7 Client Proxy in GUI mode");

        SsgManager ssgManager = SsgManagerImpl.getSsgManagerImpl();

        clientProxy = new ClientProxy(ssgManager,
                                      new MessageProcessor(Managers.getPolicyManager()),
                                      DEFAULT_PORT,
                                      MIN_THREADS,
                                      MAX_THREADS);

        // Set up the GUI
        Gui.setInstance(Gui.createGui(clientProxy, ssgManager));

        // Hook up the Message Viewer window
        clientProxy.getRequestHandler().setRequestInterceptor(Gui.getInstance().getRequestInterceptor());

        try {
            clientProxy.start();
        } catch (Exception e) {
            String message = "Unable to start the Client Proxy: " + e;
            if (e instanceof BindException ||
                    (e instanceof MultiException && ((MultiException)e).getException(0) instanceof BindException))
            {
                message = "The Layer7 Client Proxy is already running.  \nPlease shut down the existing " +
                        "Layer7 Client Proxy and try again.";
            }

            Gui.getInstance().errorMessage(message);
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

        Managers.setCredentialManager(GuiCredentialManager.getInstance());

        Gui.getInstance().start();

        // We have nothing else for the main thread to do.
        return;
    }
}

