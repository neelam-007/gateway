/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.console.logging.CascadingErrorHandler;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.AppletContentStolenPanel;
import com.l7tech.console.util.AppletSsmPreferences;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.ErrorMessageDialog;
import com.l7tech.gui.ExceptionDialog;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.SaveErrorStrategy;
import com.l7tech.gui.util.SheetHolder;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.SyspropUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.mail.internet.MimeUtility;
import javax.swing.*;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
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

    private static final String SESSION_ID_LOG_OFF = "LOGGED OFF";
    private String otherSessionId;
    private String sessionId;
    private String hostAndPort;

    private static boolean errorHandlerInstalled = false;
    private static ApplicationContext applicationContext = null;
    private static SsmApplication application = null;
    private static JRootPane appletRootPane = null;
    private static final Map<AppletMain, Object> instances = Collections.synchronizedMap(new WeakHashMap<AppletMain, Object>());

    private String helpRootUrl = DEFAULT_HELP_ROOT_RELATIVE_URL;
    private String helpTarget = "managerAppletHelp";
    private JRootPane placeholderRootPane = null;
    private String serviceUrl;

    public AppletMain() throws HeadlessException {
        instances.put(this, null);
    }

    @Override
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

        SsmApplication application = getApplication();
        synchronized( application ) {
            // Get existing applet instance (if any)
            AppletMain otherMain = (AppletMain) TopComponents.getInstance().getComponent( COMPONENT_NAME );

            if ( appletRootPane == null || TopComponents.getInstance().getAssertionRegistry() == null ) {
                appletRootPane = null;
                application.run();
                initMainWindowContent();
            }

            if ( otherMain != null && otherMain != this ) {
                notifyContentPaneStolen();
                otherSessionId = otherMain.getSessionID();
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
            initBrowserSaveErrorStrategy();

        }
        TopComponents.getInstance().setServiceUrl(serviceUrl);
    }

    /**
     * Just in case, if the applet is untrusted, we need one other save-error strategy.
     */
    private void initBrowserSaveErrorStrategy() {
        logger.finer("Initialize a browser-save-error strategy");
        SaveErrorStrategy strategy = (SaveErrorStrategy)getApplicationContext().getBean("browserSaveErrorStategy");
        ErrorMessageDialog.setBrowserSaveErrorStrategy(strategy);
    }

    private boolean looksLikeWindows() {
        try {
            String osn = SyspropUtil.getProperty( "os.name" );
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
            @Override
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


    @Override
    public void start() {
        setFocusable(true);
        forceEarlyClassLoading();
        new ParserDelegator();  // Work around for Java plug-in bug 6993073

        if ( otherSessionId == null || !otherSessionId.equals(sessionId) ) {
            MainWindow mainWindow =  getApplication().getMainWindow();

            // reconnect with the new session id, ensure that the session id
            // is not invalidated when disconnecting the workspace
            if ( mainWindow.isConnected() ) {
                String sessionId = this.sessionId;
                mainWindow.disconnectFromGateway();
                this.sessionId = sessionId;
            }

            mainWindow.activateLogonDialog();
        }
    }

    // Reference a class from each signed jar that might be lazily accessed later,
    // to prevent any needed security dialogs from coming up at a fragile time and deadlocking Swing (Bug #5548)
    private void forceEarlyClassLoading() {
        // Ensure that dependency on signed mailapi jar is detected very early, so we don't get a landmind
        // security dialog at an inconvenient time, later
        try {
            MimeUtility.encodeText("bogus");
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e); // can't happen
        }
    }

    public void redirectToServlet() {
        try {
            // In the url adding a time is for solving the problem in IE (since IE caches applet page).
            URL url = new URL(getDocumentBase().toString() + "?logout=true&" + System.currentTimeMillis());
            appletRootPane = null;
            getAppletContext().showDocument(url, "_self");
        } catch (MalformedURLException e) {
            DialogDisplayer.showMessageDialog(findAppletContainerFrame(), null,
                    "The Policy Manager internal error: invalid servlet URL.  Please contact your administrator.", e);
        }
    }

    public boolean isValidSessionID( final String sessionID ) {
        logger.info("Validating session id: " + sessionID);
        return sessionID != null && !SESSION_ID_LOG_OFF.equals( sessionID );
    }

    public void invalidateSessionID() {
        logger.info("Invalidating session id.");
        sessionId = SESSION_ID_LOG_OFF;
    }

    public String getSessionID() {
        return sessionId;        
    }

    public String getHostAndPort() {
        return hostAndPort;
    }

    private ApplicationContext getApplicationContext() {
        if (applicationContext != null) return applicationContext;
        applicationContext = createApplicationContext();
        AppletSsmPreferences ssmPreferences = applicationContext.getBean( "preferences", AppletSsmPreferences.class );
        ssmPreferences.setContext( this.getAppletContext(), this.getDocumentBase() );
        return applicationContext;
    }

    private SsmApplication getApplication() {
        //todo add comment why this lazy initialization of application is required
        if (application != null) return application;
        application = getApplicationContext().getBean("ssmApplication", SsmApplication.class);
        return application;
    }


    @Override
    public void destroy() {
        instances.remove(this);

        // if instance is not empty, this means there are multiple ssg/tabs opened.
        if (instances.isEmpty()) {
            getApplication().getMainWindow().getDisconnectAction().actionPerformed(null);
            getApplication().getMainWindow().unregisterComponents();
            TopComponents.getInstance().unregisterComponent( COMPONENT_NAME );
            errorHandlerInstalled = false;
        }
    }

    private void notifyContentPaneStolen() {
        Collection<AppletMain> applets = new ArrayList<AppletMain>(instances.keySet());
        for (AppletMain applet : applets) {
            if (applet != null && this != applet) {
                applet.replaceContentPaneWithPlaceholder();
            }
        }
    }

    private void replaceContentPaneWithPlaceholder() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (placeholderRootPane != null && getRootPane() == placeholderRootPane) return;

                placeholderRootPane = new JRootPane();
                setRootPane(placeholderRootPane);
                getContentPane().add(new AppletContentStolenPanel(), BorderLayout.CENTER);

                Window window = SwingUtilities.getWindowAncestor(AppletMain.this);
                if (window != null) {
                    window.invalidate();
                    window.pack();
                }
                getLayeredPane().updateUI();
                validate();
            }
        });
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
        String hostname = null;
        try {
            hostname = URLDecoder.decode(getParameter("hostname"), HttpConstants.ENCODING_UTF8);
        } catch (UnsupportedEncodingException e) {
            logger.warning("Invalid encoding: " + HttpConstants.ENCODING_UTF8); // shouldn't happen
        }

        if ( hostname != null && hostname.length() > 0 ) {
            logger.info("Preconfigured server hostname '" + hostname + "'.");
        } else {
            URL codebase = getCodeBase();
            if (codebase != null) {
                hostname = codebase.getHost();
            } else {
                hostname = "";
            }
        }

        //register host name
        //System.out.println("Storing hostname : " + hostname);
        serviceUrl = hostname;

        String port = getParameter("port");
        if (port == null || port.length() < 1) {
            URL codebase = getCodeBase();
            if (codebase != null) port = Integer.toString(codebase.getPort());
        }
        if ( port != null && port.length() > 0 && isInt(port )) {
            hostname = InetAddressUtil.getHostForUrl(hostname.trim()) + ":" + port;
        }

        this.hostAndPort = hostname;
        this.helpTarget = "managerAppletHelp_" + hostname.replaceAll( "[^a-zA-Z0-9_]", "_" );

        String helpRootUrl = getParameter("helpRootUrl");
        if (helpRootUrl != null && helpRootUrl.trim().length() > 0) {
            this.helpRootUrl = helpRootUrl;
            logger.info("Help root URL: " + helpRootUrl);
        } else
            logger.info("Using default help root URL: " + this.helpRootUrl);

        String sessionId = getParameter("sessionId");
        if ( sessionId != null && sessionId.length() > 0 ) {
            try {
                this.sessionId = URLDecoder.decode(sessionId, "UTF-8");
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
        ApplicationContext context = new ClassPathXmlApplicationContext(new String[]{appletName, ctxName});

        Registry.setDefault( context.getBean("registry", Registry.class) );
        
        return context;
    }

    @Override
    public String getAppletInfo() {
        return "Title: Policy Manager Applet \n"
            + "Author: Layer 7 Technologies \n"
            + "Applet version of Policy Manager.";
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

    @Override
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
            @Override
            public void run() {
                // Remove applet content
                AppletMain otherMain = (AppletMain) TopComponents.getInstance().getComponent( COMPONENT_NAME );
                getApplication().getMainWindow().disconnectFromGateway();

                sessionId = null;
                hostAndPort = null;

                setRootPane(new JRootPane());
                setGlassPane(getGlassPane());
                setLayeredPane(getLayeredPane());
                setContentPane(getContentPane());
                if ( otherMain == AppletMain.this ) {
                    AppletMain.this.destroy();
                }

                getContentPane().removeAll();
                JLabel errorLabel = new JLabel("Layer 7 Technologies - Policy Manager (Error; Reload to restart)");
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
            ErrorManager.installUncaughtExceptionHandler();
        }
        catch(SecurityException se) {
            logger.warning("Could not install uncaught exception handler.");
            getApplication();//this method has the side affect of setting the application instance
            application.setTrusted(false);                        
        }
    }
}
