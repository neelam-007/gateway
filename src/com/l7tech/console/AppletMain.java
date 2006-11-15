/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.CertUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.net.URLDecoder;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Entry point for applet-based version of SSM.
 */
public class AppletMain extends JApplet {
    private static final Logger logger = Logger.getLogger(AppletMain.class.getName());

    /** Name we use to register the most recent instance of ourself under TopComponents. */
    public static final String COMPONENT_NAME = "appletMain";

    /** Url we try to use for help topics if we aren't given an override as an applet param. */
    private static final String DEFAULT_HELP_ROOT_RELATIVE_URL = "/ssg/webadmin/help/_start.htm";

    private static ApplicationContext applicationContext = null;
    private static SsmApplication application = null;
    private static Container appletPanel = null;
    private static JMenuBar menuBar = null;

    private WindowListener windowListener = null;
    private String helpRootUrl = DEFAULT_HELP_ROOT_RELATIVE_URL;
    private String helpTarget = "managerAppletHelp";

    private void maybeAddWindowListener(final Frame f) {
        if (windowListener != null) return;

        windowListener = new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                f.toFront();
            }
        };
        f.addWindowListener(windowListener);
    }

    public void init() {
        super.init();

        gatherAppletParameters();
        getApplication().setAutoLookAndFeel();
        getApplication().run();
        setJMenuBar(getAppletMenuBar());
        setContentPane(getAppletPanel());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getApplication().getMainWindow().disconnectFromGateway();
                getApplication().getMainWindow().activateLogonDialog();
            }
        });

        TopComponents.getInstance().registerComponent("topLevelParent", new TopComponents.ComponentFinder() {
            public Component findComponent() {
                Component c = AppletMain.this;
                while (c != null) {
                    if (c instanceof Frame) {
                        logger.info("Found applet container frame");
                        maybeAddWindowListener((Frame)c);                        
                        return c;
                    }
                    c = c.getParent();
                }
                logger.warning("Did not find applet container frame");
                return null;
            }
        });

        TopComponents.getInstance().unregisterComponent(COMPONENT_NAME);
        TopComponents.getInstance().registerComponent(COMPONENT_NAME, AppletMain.this);

        repaint();
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

    private JMenuBar getAppletMenuBar() {
        getAppletPanel();
        return menuBar;
    }

    private void gatherAppletParameters() {
        String hostname = getParameter("hostname");

        if (hostname == null || hostname.length() < 1) {
            URL codebase = getCodeBase();
            if (codebase != null) hostname = codebase.getHost();
        }

        if (hostname != null && hostname.length() > 0) {
            LogonDialog.setPreconfiguredGatewayHostname(hostname);
            helpTarget = "managerAppletHelp_" + hostname;
            logger.info("Preconfigured server hostname: " + hostname);
        }

        String helpRootUrl = getParameter("helpRootUrl");
        if (helpRootUrl != null && helpRootUrl.trim().length() > 0) {
            this.helpRootUrl = helpRootUrl;
            logger.info("Help root URL: " + helpRootUrl);
        } else
            logger.info("Using default help root URL: " + helpRootUrl);

        String serverCertB64 = getParameter("gatewayCert");
        if (serverCertB64 != null && serverCertB64.length() > 0) {
            try {
                serverCertB64 = URLDecoder.decode(serverCertB64, "UTF-8");
                if (logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO, "Preconfigured server cert(base64): " + serverCertB64);
                byte[] certBytes = HexUtils.decodeBase64(serverCertB64, true);
                X509Certificate serverCert = CertUtils.decodeCert(certBytes);
                LogonDialog.setPreconfiguredServerCert(serverCert);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse preconfigured server cert bytes: " + ExceptionUtils.getMessage(e), e);
            } catch (CertificateException e) {
                logger.log(Level.WARNING, "Unable to parse preconfigured server cert bytes: " + ExceptionUtils.getMessage(e), e);
            }
        }

        String sessionId = getParameter("sessionId");
        if (sessionId != null && sessionId.length() > 0) {
            try {
                LogonDialog.setPreconfiguredSessionId(URLDecoder.decode(sessionId, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.WARNING, "Unable to decode preconfigured session ID: " + ExceptionUtils.getMessage(e), e);
            }
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

    public String[][] getParameterInfo() {
        return new String[][]{
            {"username", "string", "The adminstrator username.  Mandatory."},
            {"token", "string", "The authenticaton token.  Mandatory."},
        };
    }

    public void showHelpTopicsRoot() {
        try {
            URL cb = getDocumentBase();
            URL url = new URL(cb.getProtocol(), cb.getHost(), cb.getPort(), helpRootUrl);
            getAppletContext().showDocument(url, helpTarget);
        } catch (MalformedURLException e) {
            logger.warning("Unable to display webhelp: bad webhelp URL: " + ExceptionUtils.getMessage(e));
        }
    }
}