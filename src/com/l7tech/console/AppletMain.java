/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.console.panels.LogonDialog;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.CertUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Entry point for applet-based version of SSM.
 */
public class AppletMain extends JApplet {
    private static final Logger logger = Logger.getLogger(AppletMain.class.getName());

    private static ApplicationContext applicationContext = null;
    private static SsmApplication application = null;
    private static Container appletPanel = null;
    private static JMenuBar menuBar = null;

    public void init() {
        super.init();

        configureCredentials();
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

    private void configureCredentials() {
        String hostname = getParameter("hostname");

        if (hostname == null || hostname.length() < 1) {
            URL codebase = getCodeBase();
            if (codebase != null) hostname = codebase.getHost();
        }

        if (hostname != null && hostname.length() > 0) {
            LogonDialog.setPreconfiguredGatewayHostname(hostname);
            logger.info("Preconfigured server hostname: " + hostname);
        }

        String serverCertB64 = getParameter("gatewayCert");
        if (serverCertB64 != null && serverCertB64.length() > 0) {
            try {
                byte[] certBytes;
                try {
                    certBytes = HexUtils.unHexDump(serverCertB64);
                } catch (IOException e) {
                    certBytes = HexUtils.decodeBase64(serverCertB64, true);
                }
                X509Certificate serverCert = CertUtils.decodeCert(certBytes);
                LogonDialog.setPreconfiguredServerCert(serverCert);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse preconfigured server cert bytes: " + ExceptionUtils.getMessage(e), e);
            } catch (CertificateException e) {
                logger.log(Level.WARNING, "Unable to parse preconfigured server cert bytes: " + ExceptionUtils.getMessage(e), e);
            }
        }

        String sessionId = getParameter("sessionId");
        if (sessionId != null && sessionId.length() > 0)
            LogonDialog.setPreconfiguredSessionId(sessionId);
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
}