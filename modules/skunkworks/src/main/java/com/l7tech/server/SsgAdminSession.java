/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.gateway.common.admin.AdminContext;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.admin.AdminLoginResult;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.console.util.History;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.UserBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.security.auth.Subject;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class SsgAdminSession {
    protected static final Logger logger = Logger.getLogger(SsgAdminSession.class.getName());
    private ApplicationContext applicationContext;
    private AdminContext adminContext;

    public SsgAdminSession() throws Exception {
        this(new String[]{});
    }

    public SsgAdminSession(String[] args) throws Exception {
        if (args.length >= 1) {
            hostPort = args[0];

            if (args.length >= 3) {
                adminlogin = args[1];
                adminpass = args[2];
            }
        }
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) {
            logger.warning("The subject is null; the Admin Services will probably not be accessible");
        } else {
            subject.getPrincipals().clear();
            final UserBean u = new UserBean();
            u.setLogin(adminlogin);
            u.setName(adminlogin);
            subject.getPrincipals().add(u);
            subject.getPrivateCredentials().clear();
            subject.getPrivateCredentials().add(adminpass);
        }
        JdkLoggerConfigurator.configure("com.l7tech.console", "com/l7tech/console/resources/logging.properties");
        // initialize preferences
        SsmPreferences preferences = TopComponents.getInstance().getPreferences();
        preferences.updateFromProperties(System.getProperties(), false);
        preferences.updateSystemProperties();
        // application context
        applicationContext = createApplicationContext();
        System.out.println("Connecting to " + hostPort);
        AdminLogin adminLogin = (AdminLogin)applicationContext.getBean("adminLogin");
        AdminLoginResult loginResult = adminLogin.login(adminlogin, adminpass);

        subject.getPrincipals().clear();
        UserBean u = new UserBean();
        u.setLogin(loginResult.getSessionCookie());
        u.setName(loginResult.getSessionCookie());
        subject.getPrincipals().add(u);
        subject.getPrivateCredentials().clear();
    }

    public AdminContext getAdminContext() {
        return adminContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private ApplicationContext createApplicationContext() {
        String ctxName = System.getProperty("ssm.application.context");
        if (ctxName == null) {
            ctxName = "com/l7tech/console/resources/beans-context.xml";
        }
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{ctxName});
        return context;
    }

    private String adminlogin = "admin";
    private String adminpass = "password";
    private String hostPort = defaultHostPort();

    private String defaultHostPort() {
        SsmPreferences preferences = TopComponents.getInstance().getPreferences();

        History serverUrlHistory = preferences.getHistory(SsmPreferences.SERVICE_URL);
        Object[] urls = serverUrlHistory.getEntries();
        for (int i = 0; i < urls.length; i++) {
            String surl = urls[i].toString();
            try {
                URL url = new URL(surl);
                return url.getHost() + ":" + url.getPort();
            } catch (MalformedURLException e) {
            }
            return surl;
        }
        return "localhost:2124";
    }
}
