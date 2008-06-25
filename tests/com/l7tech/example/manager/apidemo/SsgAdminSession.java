/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.example.manager.apidemo;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.console.MainWindow;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.HeavySsmPreferences;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.service.ServiceAdmin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.security.auth.login.LoginException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SsgAdminSession {
	protected static final Logger logger = Logger.getLogger(SsgAdminSession.class.getName());
	private ApplicationContext applicationContext;
    private String ssgHost = Main.SSGHOST;
	private String adminLogin = Main.ADMINACCOUNT_NAME;
	private String adminPass = Main.ADMINACCOUNT_PASSWD;

    public SsgAdminSession(String ssgHost, String adminLogin, String adminPasswd) throws MalformedURLException, RemoteException, LoginException {
        this.ssgHost = ssgHost;
        this.adminLogin = adminLogin;
        this.adminPass = adminPasswd;

        // initialize preferences
        HeavySsmPreferences preferences = new HeavySsmPreferences();
	 	preferences.updateFromProperties(System.getProperties(), false);
        preferences.updateSystemProperties();

        // initialize session
        initialize();
	}

    public void initialize() throws MalformedURLException, LoginException, RemoteException {
        applicationContext = createApplicationContext();
        SecurityProvider credentialManager = Registry.getDefault().getSecurityProvider();

        SsmApplication app = new SsmApplication() {
            public void showHelpTopicsRoot() {}
            public void run() {}
            public boolean isApplet() {
                return false;
            }
        };
        app.setApplicationContext(applicationContext);
        TopComponents.getInstance().registerComponent("mainWindow", new MainWindow(app));

        try {
            credentialManager.getAuthenticationProvider().login(new PasswordAuthentication(this.adminLogin, this.adminPass.toCharArray()), this.ssgHost, false);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "version problem!", e);
        }
    }

    private ApplicationContext createApplicationContext() {
        String ctxName = System.getProperty("ssm.application.context");
        if (ctxName == null) {
            ctxName = "com/l7tech/console/resources/beans-context.xml";
        }
        String ctxHeavy = "com/l7tech/console/resources/beans-application.xml";
        return new ClassPathXmlApplicationContext(new String[]{ctxHeavy, ctxName});
    }

    public ClusterStatusAdmin getClusterStatusAdmin() {
        return Registry.getDefault().getClusterStatusAdmin();
    }

    public ServiceAdmin getServiceAdmin() {
        return Registry.getDefault().getServiceManager();
    }

    public IdentityAdmin getIdentityAdmin() throws RemoteException {
        return Registry.getDefault().getIdentityAdmin();
	}
}
