/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.panels.AppletContentStolenPanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.logging.AwtErrorHandler;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.WeakSet;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.SheetHolder;
import com.l7tech.common.gui.ExceptionDialog;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;

/**
 * Entry point for applet-based version of SSM.
 */
public class AppletMain extends JApplet implements SheetHolder {
    private static final Logger logger = Logger.getLogger(AppletMain.class.getName());

    /** Name we use to register the most recent instance of ourself under TopComponents. */
    public static final String COMPONENT_NAME = "appletMain";

    /** Url we try to use for help topics if we aren't given an override as an applet param. */
    private static final String DEFAULT_HELP_ROOT_RELATIVE_URL = "/ssg/webadmin/help/_start.htm";

    private static boolean errorHandlerInstalled = false;
    private static ApplicationContext applicationContext = null;
    private static SsmApplication application = null;
    private static JRootPane appletRootPane = null;
    private static AppletMain currentRootPaneOwner = null;
    private static WeakSet instances = new WeakSet();

    private String helpRootUrl = DEFAULT_HELP_ROOT_RELATIVE_URL;
    private String helpTarget = "managerAppletHelp";
    private JRootPane placeholderRootPane = null;

    public AppletMain() throws HeadlessException {
        instances.add(this);
    }

    public synchronized void init() {
        super.init();

        gatherAppletParameters();
        initializeErrorHandling();

        getApplication().setAutoLookAndFeel();
        getApplication().run();

        initMainWindowContent();
        AppletMain oldRootPaneOwner = currentRootPaneOwner;
        currentRootPaneOwner = this;
        if (oldRootPaneOwner != null && oldRootPaneOwner != this) {
            notifyContentPaneStolen();
        }
        setRootPane(appletRootPane);
        placeholderRootPane = null;

        final Frame appletContainer = findAppletContainerFrame();
        if (appletContainer != null) {
            TopComponents.getInstance().unregisterComponent("topLevelParent");
            TopComponents.getInstance().registerComponent("topLevelParent", appletContainer);
        }

        TopComponents.getInstance().unregisterComponent(COMPONENT_NAME);
        TopComponents.getInstance().registerComponent(COMPONENT_NAME, AppletMain.this);

        // Help DialogDisplayer find the right applet instance
        DialogDisplayer.putDefaultSheetHolder(appletContainer, this);

        getLayeredPane().updateUI();
        validate();
    }

    private Frame findAppletContainerFrame() {
        Component c = this;
        while (c != null) {
            if (c instanceof Frame) {
                logger.info("Found applet container frame");
                return (Frame)c;
            }
            c = c.getParent();
        }
        logger.warning("Did not find applet container frame");
        return null;
    }


    public void start() {
        setFocusable(true);
        if (!getApplication().getMainWindow().isConnected()) {
            getApplication().getMainWindow().disconnectFromGateway();
            getApplication().getMainWindow().activateLogonDialog();
        }
    }

    private ApplicationContext getApplicationContext() {
        if (applicationContext != null) return applicationContext;
        applicationContext = createApplicationContext();
        return applicationContext;
    }

    private SsmApplication getApplication() {
        if (application != null) return application;
        application = (SsmApplication)getApplicationContext().getBean("ssmApplication");
        return application;
    }

    public void destroy() {
        // Can't actually destroy anything, because it can't be destroyed completely (some singleton beans),
        // and next call to init() would thus fail.
    }

    private static void notifyContentPaneStolen() {
        for (Iterator i = instances.iterator(); i.hasNext();) {
            AppletMain applet = (AppletMain)i.next();
            if (applet != null && currentRootPaneOwner != applet) {
                applet.replaceContentPaneWithPlaceholder();
            }
        }
    }

    private void replaceContentPaneWithPlaceholder() {
        if (placeholderRootPane != null && getRootPane() == placeholderRootPane) return;

        placeholderRootPane = new JRootPane();
        setRootPane(placeholderRootPane);
        setLayeredPane(getLayeredPane());
        setJMenuBar(null);
        setContentPane(getContentPane());
        getContentPane().add(new AppletContentStolenPanel(), BorderLayout.CENTER);

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.invalidate();
            window.pack();
        }
        getLayeredPane().updateUI();
        validate();
    }

    private void initMainWindowContent() {
        if (appletRootPane != null) return;

        // Get the main window, and steal it's content pane
        MainWindow mainWindow = getApplication().getMainWindow();

        appletRootPane = mainWindow.getRootPane();

        // Make sure MainWindow knows we have taken itdads stuff
        mainWindow.notifyRootPaneStolen();
    }

    private void gatherAppletParameters() {
        String port = getParameter("port");

        if (port == null || port.length() < 1) {
            URL codebase = getCodeBase();
            if (codebase != null) port = Integer.toString(codebase.getPort());
        }

        String hostname = getParameter("hostname");

        if (hostname == null || hostname.length() < 1) {
            URL codebase = getCodeBase();
            if (codebase != null) hostname = codebase.getHost();
        }

        if (hostname != null && hostname.length() > 0) {
            if (port != null && port.length() > 0 && isInt(port)) {
                hostname = hostname.trim() + ":" + port;
            }

            LogonDialog.setPreconfiguredGatewayHostname(hostname);
            helpTarget = "managerAppletHelp_" + hostname;
            logger.info("Preconfigured server hostname: " + hostname);
        }

        String helpRootUrl = getParameter("helpRootUrl");
        if (helpRootUrl != null && helpRootUrl.trim().length() > 0) {
            this.helpRootUrl = helpRootUrl;
            logger.info("Help root URL: " + helpRootUrl);
        } else
            logger.info("Using default help root URL: " + this.helpRootUrl);

        String sessionId = getParameter("sessionId");
        if (sessionId != null && sessionId.length() > 0) {
            try {
                LogonDialog.setPreconfiguredSessionId(URLDecoder.decode(sessionId, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.WARNING, "Unable to decode preconfigured session ID: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    private boolean isInt(String port) {
        try {
            Integer.parseInt(port);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static ApplicationContext createApplicationContext() {
        String ctxName = "com/l7tech/console/resources/beans-context.xml";
        String appletName = "com/l7tech/console/resources/beans-applet.xml";
        return new ClassPathXmlApplicationContext(new String[]{appletName, ctxName});
    }

    public String getAppletInfo() {
        return "Title: SecureSpan Manager Applet \n"
            + "Author: Layer 7 Technologies \n"
            + "Applet version of SecureSpan Manager.";
    }

    public void showHelpTopicsRoot() {
        try {
            URL cb = getDocumentBase();
            URL url = new URL(cb, helpRootUrl);
            getAppletContext().showDocument(url, helpTarget);
        } catch (MalformedURLException e) {
            logger.warning("Unable to display webhelp: bad webhelp URL: " + ExceptionUtils.getMessage(e));
        }
    }

    public void showSheet(JInternalFrame sheet) {
        DialogDisplayer.showSheet(this, sheet);
    }


    /**
     * Event queue installation will work even in an untrusted applet
     * as long as the java plugin is used.
     */
    private void initializeErrorHandling() {
        synchronized (AppletMain.class) {
            if (errorHandlerInstalled)
                return;
            errorHandlerInstalled = true;
        }

        // Can't use System.exit on error (kills browser)
        ExceptionDialog.setShutdownHandler(new Runnable() {
            public void run() {
                // Remove applet content
                setRootPane(new JRootPane());
                setGlassPane(getGlassPane());
                setLayeredPane(getLayeredPane());
                setContentPane(getContentPane());
                setJMenuBar(null);
                if (currentRootPaneOwner == AppletMain.this)
                    currentRootPaneOwner = null;
                
                getContentPane().removeAll();
                JLabel errorLabel = new JLabel("Layer 7 Technologies - SecureSpan Manager (Error; Reload to restart)");
                errorLabel.setVerticalAlignment(JLabel.CENTER);
                getContentPane().add(errorLabel, BorderLayout.CENTER);
                validate();

                // Find and destroy dialogs
                Window[] owned = TopComponents.getInstance().getTopParent().getOwnedWindows();
                if (owned != null) {
                    for (Window window : owned) {
                        window.dispose();
                    }
                }

                // Find and dispose windows (gets windows our applet owns)
                Frame topFrame = TopComponents.getInstance().getTopParent();
                Frame[] frames = Frame.getFrames();
                if (frames != null) {
                    for (Frame frame : frames) {
                        if (frame != topFrame) {
                            frame.dispose();
                        }
                    }
                }
            }
        });

        // Install error handling event queue
        EventQueue queue = new EventQueue() {
            protected void dispatchEvent(AWTEvent e) {
                try {
                    super.dispatchEvent(e);
                } catch(Throwable throwable) {
                    new AwtErrorHandler().handle(throwable);
                }
            }
        };

        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().push(queue);
        } catch(SecurityException se) {
            logger.warning("Could not install event queue.");
        }
    }    
}