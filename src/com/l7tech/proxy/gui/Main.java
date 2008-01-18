package com.l7tech.proxy.gui;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.Constants;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgManagerImpl;
import org.mortbay.util.MultiException;

import java.io.IOException;
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
        boolean config = false;
        boolean hideMenus = false;
        String quitLabel = null;

        for (int i = 0; i < argv.length; i++) {
            String s = argv[i].trim().toLowerCase();
            if ("-config".equalsIgnoreCase(s)) {
                config = true;
            } else if ("-hideMenus".equalsIgnoreCase(s)) {
                hideMenus = true;
            } else if ("-quitLabel".equalsIgnoreCase(s) && i + 1 < argv.length) {
                quitLabel = argv[++i];
            }
        }

        initConfig();

        if (config)
            startGuiConfigurator(hideMenus, quitLabel);
        else
            startGuiBridge();
    }

    private static void startGuiConfigurator(boolean hideMenus, String quitLabel) {
        // apache logging layer to use the jdk logger
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        
        JdkLoggerConfigurator.configure("com.l7tech.proxy", "com/l7tech/proxy/resources/cliLogging.properties");
        JceProvider.init();
        log.info("Starting SecureSpan "+ Constants.APP_NAME +" GUI Configuration Editor; " + BuildInfo.getLongBuildString());

        if (hideMenus && quitLabel == null) 
            quitLabel = "Quit";
        if (quitLabel != null)
            quitLabel = "      " + quitLabel + "      ";

        final SsgManagerImpl ssgManager = SsgManagerImpl.getSsgManagerImpl();
        Gui.setInstance(Gui.createGui(new Gui.GuiParams(ssgManager, getBindPort(), true, hideMenus, quitLabel)));
        Managers.setCredentialManager(GuiCredentialManager.createGuiCredentialManager(ssgManager));
        try {
            ssgManager.lockConfiguration();
        } catch (SsgManagerImpl.ConfigurationAlreadyLockedException e) {
            Gui.errorMessage("Unable to start " + Constants.APP_NAME + " GUI Configuration Editor: another instance may already be running: " + ExceptionUtils.getMessage(e));
            System.exit(2);
        } catch (IOException e) {
            Gui.errorMessage("Unable to Start", "Unable to start " + Constants.APP_NAME + " GUI Configuration Editor", ExceptionUtils.getMessage(e), e);
            System.exit(2);
        }                

        // Make sure the proxy stops when the GUI does.
        Gui.getInstance().setShutdownListener(new Gui.ShutdownListener() {
            public void guiShutdown() {
                System.exit(0);
            }
        });

        Gui.getInstance().start();

        // We have nothing else for the main thread to do.
        return;
    }

    private static void startGuiBridge() {
        initLogging();
        log.info("Starting SecureSpan "+ Constants.APP_NAME +" in GUI mode; " + BuildInfo.getLongBuildString());

        final SsgManagerImpl ssgManager = SsgManagerImpl.getSsgManagerImpl();
        final ClientProxy clientProxy = createClientProxy(ssgManager);

        // Set up the GUI
        Gui.setInstance(Gui.createGui(new Gui.GuiParams(ssgManager, clientProxy.getBindPort())));

        // Hook up the Message Viewer window
        clientProxy.getRequestHandler().setRequestInterceptor(Gui.getInstance().getRequestInterceptor());

        Managers.setCredentialManager(GuiCredentialManager.createGuiCredentialManager(ssgManager));

        try {
            ssgManager.lockConfiguration();
            clientProxy.start();
        } catch (Exception e) {
            String message = "Unable to start the "+ Constants.APP_NAME +": " + e;
            // Friendlier error message for starting multiple instances
            if (e instanceof BindException || e instanceof SsgManagerImpl.ConfigurationAlreadyLockedException ||
              (e instanceof MultiException && ((MultiException)e).getException(0) instanceof BindException)) {
                message = "The SecureSpan "+ Constants.APP_NAME +" is already running.  \nPlease shut down the existing " +
                  Constants.APP_NAME + " and try again.";
            }

            Gui.errorMessage(message);
            log.log(Level.SEVERE, "Fatal: Unable to start HTTP listener", e);
            System.exit(2);
        }

        // Make sure the proxy stops when the GUI does.
        Gui.getInstance().setShutdownListener(new Gui.ShutdownListener() {
            public void guiShutdown() {
                clientProxy.stop(); // orderly shutdown
                System.exit(0);
            }
        });

        Gui.getInstance().start();

        // We have nothing else for the main thread to do.
        return;
    }
}

