package com.l7tech.proxy.gui;

import com.l7tech.common.BuildInfo;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgManagerImpl;
import org.mortbay.util.MultiException;

import java.net.BindException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Begin execution of client proxy along with an attached GUI.
 * User: mike
 * Date: May 15, 2003
 * Time: 3:33:14 PM
 */
public final class Main extends com.l7tech.proxy.Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    /**
     * Start a GUI-equipped client proxy and then return immediately.
     * This method will either start the client proxy and then return immediately (while background threads
     * hang around to do the work), or will log an error and exit the process with System.exit(2) if the
     * proxy could not be started.
     */
    public static void main(final String[] argv) {
        initLogging();
        log.info("Starting SecureSpan Bridge in GUI mode; " + BuildInfo.getLongBuildString());

        final SsgManagerImpl ssgManager = SsgManagerImpl.getSsgManagerImpl();
        createClientProxy(ssgManager);

        // Set up the GUI
        Gui.setInstance(Gui.createGui(getClientProxy(), ssgManager));

        // Hook up the Message Viewer window
        getClientProxy().getRequestHandler().setRequestInterceptor(Gui.getInstance().getRequestInterceptor());

        Managers.setCredentialManager(GuiCredentialManager.createGuiCredentialManager(ssgManager));

        try {
            getClientProxy().start();
        } catch (Exception e) {
            String message = "Unable to start the Bridge: " + e;
            // Friendlier error message for starting multiple instances
            if (e instanceof BindException ||
              (e instanceof MultiException && ((MultiException)e).getException(0) instanceof BindException)) {
                message = "The SecureSpan Bridge is already running.  \nPlease shut down the existing " +
                  "Bridge and try again.";
            }

            Gui.errorMessage(message);
            log.log(Level.SEVERE, "Fatal: Unable to start HTTP listener", e);
            System.exit(2);
        }

        // Make sure the proxy stops when the GUI does.
        Gui.getInstance().setShutdownListener(new Gui.ShutdownListener() {
            public void guiShutdown() {
                getClientProxy().stop(); // orderly shutdown
                System.exit(0);
            }
        });

        Gui.getInstance().start();

        // We have nothing else for the main thread to do.
        return;
    }
}

