package com.l7tech.proxy.gui;

import com.l7tech.proxy.ClientProxy;
import java.util.logging.Logger;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.SsgManagerImpl;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.security.JceProvider;
import org.mortbay.util.MultiException;

import java.net.BindException;

/**
 * Begin execution of client proxy along with an attached GUI.
 * User: mike
 * Date: May 15, 2003
 * Time: 3:33:14 PM
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());
    private static final int DEFAULT_PORT = 7700;
    private static final int MIN_THREADS = 5;
    private static final int MAX_THREADS = 300;

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

    /** Start a GUI-equipped client proxy and run it until it's shut down. */
    public static void main(final String[] argv) {
        JdkLoggerConfigurator.configure("com.l7tech.proxy", "com/l7tech/proxy/resources/logging.properties");
        log.info("Starting Agent; " + BuildInfo.getLongBuildString());
        JceProvider.init();

        SsgManager ssgManager = SsgManagerImpl.getSsgManagerImpl();

        int port = getIntProperty("com.l7tech.proxy.listener.port", DEFAULT_PORT);
        int minThreads = getIntProperty("com.l7tech.proxy.listener.minthreads", MIN_THREADS);
        int maxThreads = getIntProperty("com.l7tech.proxy.listener.maxthreads", MAX_THREADS);

        final ClientProxy clientProxy = new ClientProxy(ssgManager,
                                                        new MessageProcessor(Managers.getPolicyManager()),
                                                        port,
                                                        minThreads,
                                                        maxThreads);

        // Set up the GUI
        Gui.setInstance(Gui.createGui(clientProxy, ssgManager));

        // Hook up the Message Viewer window
        clientProxy.getRequestHandler().setRequestInterceptor(Gui.getInstance().getRequestInterceptor());

        Managers.setCredentialManager(GuiCredentialManager.createGuiCredentialManager(ssgManager));

        try {
            clientProxy.start();
        } catch (Exception e) {
            String message = "Unable to start the Agent: " + e;
            if (e instanceof BindException ||
                    (e instanceof MultiException && ((MultiException)e).getException(0) instanceof BindException))
            {
                message = "The SecureSpan Agent is already running.  \nPlease shut down the existing " +
                        "Agent and try again.";
            }

            Gui.errorMessage(message);
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

