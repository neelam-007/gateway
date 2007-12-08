/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.ErrorMessageDialog;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.SheetHolder;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.WeakSet;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.AppletContentStolenPanel;
import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.logging.CascadingErrorHandler;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        try {
            String feelName = looksLikeWindows()
                                ? UIManager.getSystemLookAndFeelClassName()
                                : UIManager.getCrossPlatformLookAndFeelClassName();
            UIManager.setLookAndFeel(feelName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to set system look and feel: " + ExceptionUtils.getMessage(e), e);
        }
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

        initHelpKeyBinding();
    }

    private boolean looksLikeWindows() {
        try {
            String osn = System.getProperty("os.name");
            return osn != null && osn.toLowerCase().startsWith("windows");
        } catch (SecurityException se) {
            return false;
        }
    }

    private void initHelpKeyBinding() {
        logger.finer("Installing help key binding");
        KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
        String aname = "showHelpTopics";

        AbstractAction helpAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                logger.fine("Attempting to show help topics root in response to F1 key");
                showHelpTopicsRoot();
            }
        };

        helpAction.putValue(Action.NAME, aname);
        helpAction.putValue(Action.ACCELERATOR_KEY, accel);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, aname);
        getRootPane().getActionMap().put(aname, helpAction);
        getLayeredPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, aname);
        getLayeredPane().getActionMap().put(aname, helpAction);
        ((JComponent)getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, aname);
        ((JComponent)getContentPane()).getActionMap().put(aname, helpAction);
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
        if (! LogonDialog.isSameApplet()) {
            getApplication().getMainWindow().disconnectFromGateway();
            getApplication().getMainWindow().activateLogonDialog();
        }
    }

    public void redirectToServlet() {
        try {
            // In the url adding a time is for solving the problem in IE (since IE caches applet page). 
            URL url = new URL(getDocumentBase().toString() + "?" + System.currentTimeMillis());
            getAppletContext().showDocument(url, "_self");
        } catch (MalformedURLException e) {
            DialogDisplayer.showMessageDialog(findAppletContainerFrame(), null,
                    "The SecureSpan Manager internal error: invalid servlet URL.  Please contact your administrator.", e);
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
        instances.remove(this);

        // if instance is not empty, this means there are multiple ssg/tabs opened.
        if (instances.isEmpty()) {
            getApplication().getMainWindow().getDisconnectAction().actionPerformed(null);
            getApplication().getMainWindow().unregisterComponents();
            errorHandlerInstalled = false;
            appletRootPane = null;
            currentRootPaneOwner = null;
        }
    }

    private static void notifyContentPaneStolen() {
        for (Object instance : instances) {
            AppletMain applet = (AppletMain)instance;
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
        if (LogonDialog.isSameApplet()) return;

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
        Runnable shutdownTask = new Runnable() {
            public void run() {
                // Remove applet content
                getApplication().getMainWindow().disconnectFromGateway();
                setRootPane(new JRootPane());
                setGlassPane(getGlassPane());
                setLayeredPane(getLayeredPane());
                setContentPane(getContentPane());
                setJMenuBar(null);
                if (currentRootPaneOwner == AppletMain.this) {
                    AppletMain.this.destroy();
                }

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
        };
        ExceptionDialog.setShutdownHandler(shutdownTask);
        ErrorMessageDialog.setShutdownHandler(shutdownTask);
        CascadingErrorHandler.setShutdownHandler(shutdownTask);

        // Set AWT error handler
        try {
            System.setProperty("sun.awt.exception.handler", com.l7tech.console.logging.AwtErrorHandler.class.getName());
        }
        catch(SecurityException se) {
            logger.warning("Could not install AWT exception handler.");
        }
    }

    /**
     * The class is for saving a error report file in the client side using a browser save as dialog.
     * The super class is {@link com.l7tech.common.gui.ErrorMessageDialog.SaveStrategy}
     */
    public class AppletSaveStrategy extends ErrorMessageDialog.SaveStrategy {

        public void saveErrorReportFile() {
            String fileContent = getReportContent();
            String urlStr = getBasicURL(AppletMain.this.getDocumentBase().toString()) + "/ssg/webadmin/filedownload";

            try {
                // Uploading the report
                URL url = new URL(urlStr + "?filename=" + getSuggestedFileName());
                GenericHttpClient client = new UrlConnectionHttpClient();
                SimpleHttpClient sClient = new SimpleHttpClient(client);
                GenericHttpRequestParams params = new GenericHttpRequestParams(url);
                params.setContentType(ContentTypeHeader.TEXT_DEFAULT);
                SimpleHttpClient.SimpleHttpResponse response = sClient.post(params, fileContent.getBytes("UTF-8"));
                String key = new String(response.getBytes());

                // Downloading the report
                url = new URL(urlStr + "?key=" + key);
                AppletMain.this.getAppletContext().showDocument(url, "_self");
            }
            catch (IOException ex) {
                logger.warning("Could not make request to upload or download an error report file.");
            }
        }

        private String getBasicURL(String currURL) {
            int idx = currURL.indexOf("9443");
            return currURL.substring(0, (idx + 4));
        }
    }
}