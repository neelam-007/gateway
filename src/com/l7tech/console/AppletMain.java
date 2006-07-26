/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.console.panels.LogonDialog;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Entry point for applet-based version of SSM.
 */
public class AppletMain extends JApplet {
    private static final Logger logger = Logger.getLogger(AppletMain.class.getName());

    public void init() {
        super.init();

        if (System.getSecurityManager() != null) {
            System.setSecurityManager(null);
        }

        configureCredentials();

        ApplicationContext ctx = createApplicationContext();
        SsmApplication app = (SsmApplication)ctx.getBean("ssmApplication");
        app.run();

        MainWindow mainWindow = app.getMainWindow();

        JMenuBar menuBar = mainWindow.getJMenuBar();
        mainWindow.setJMenuBar(null);
        setJMenuBar(menuBar);

        Container pane = mainWindow.getContentPane();
        mainWindow.setContentPane(new JPanel());

        setContentPane(pane);
        repaint();
    }

    private void configureCredentials() {
        String hostname = getParameter("hostname");

        if (hostname != null && hostname.length() < 1) {
            URL codebase = getCodeBase();
            if (codebase != null) hostname = codebase.getHost();
        }

        if (hostname != null && hostname.length() > 0) LogonDialog.setPreconfiguredGatewayHostname(hostname);

        // TODO very important! do before tagging!
        // TODO very important! do before tagging!
        // TODO very important! do before tagging!
        // TODO make single-use logon token instead of using plaintext passwd!
        // TODO very important! do before tagging!
        // TODO very important! do before tagging!
        // TODO very important! do before tagging!
        String username = getParameter("username");
        String password = getParameter("password");

        if (username != null && username.length() > 0 && password != null && password.length() > 0) {
            LogonDialog.setPreconfiguredCredentials(username, password);
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
}