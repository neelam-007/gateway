/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.panels.AppletContentStolenPanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.SheetHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import java.util.logging.Level;
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

    private static ApplicationContext applicationContext = null;
    private static SsmApplication application = null;
    private static Container appletPanel = null;
    private static JMenuBar menuBar = null;
    private static AppletMain currentPanelOwner = null;

    private String helpRootUrl = DEFAULT_HELP_ROOT_RELATIVE_URL;
    private String helpTarget = "managerAppletHelp";

    public void init() {
        super.init();

        gatherAppletParameters();
        getApplication().setAutoLookAndFeel();
        getApplication().run();
        setJMenuBar(getAppletMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getAppletPanel(), BorderLayout.CENTER);
        if (currentPanelOwner != null) currentPanelOwner.notifyContentPaneStolen();
        currentPanelOwner = this;

        Frame appletContainer = findAppletContainerFrame();
        if (appletContainer != null)
                TopComponents.getInstance().registerComponent("topLevelParent", appletContainer);

        TopComponents.getInstance().unregisterComponent(COMPONENT_NAME);
        TopComponents.getInstance().registerComponent(COMPONENT_NAME, AppletMain.this);

        // Help DialogDisplayer find the right applet instance
        DialogDisplayer.putDefaultSheetHolder(appletContainer, this);

        repaint();
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

    public void destroy() {
        // Can't actually destroy anything, because it can't be destroyed completely (some singleton beans),
        // and next call to init() would thus fail.
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

    private Container getAppletPanel() {
        if (appletPanel != null) return appletPanel;

        // Get the main window, and steal it's content pane
        MainWindow mainWindow = getApplication().getMainWindow();
        menuBar = mainWindow.getJMenuBar();
        mainWindow.setJMenuBar(null);
        setJMenuBar(menuBar);
        appletPanel = mainWindow.getContentPane();
        mainWindow.setContentPane(new JPanel());
        return appletPanel;
    }

    private void notifyContentPaneStolen() {
        logger.info("Applet content has been moved to a different browser frame.");
        getContentPane().removeAll();
        getContentPane().add(new AppletContentStolenPanel(), BorderLayout.CENTER);
        setJMenuBar(null);
        SwingUtilities.getWindowAncestor(this).pack();
        invalidate();
        repaint();
    }

    private JMenuBar getAppletMenuBar() {
        getAppletPanel();
        return menuBar;
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
}