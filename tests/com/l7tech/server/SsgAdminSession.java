/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.admin.AdminContext;
import com.l7tech.admin.AdminLogin;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.console.util.History;
import com.l7tech.console.util.Preferences;
import com.l7tech.spring.remoting.rmi.NamingURL;
import com.l7tech.spring.remoting.rmi.ResettableRmiProxyFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author alex
 * @version $Revision$
 */
public class SsgAdminSession {
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

        JdkLoggerConfigurator.configure("com.l7tech.console", "com/l7tech/console/resources/logging.properties");
        // initialize preferences
        Preferences preferences = Preferences.getPreferences();
        preferences.updateFromProperties(System.getProperties(), false);
        preferences.updateSystemProperties();
        // application context
        applicationContext = createApplicationContext();
        System.out.println("Connecting to " + hostPort);
        NamingURL adminServiceNamingURL = NamingURL.parse(NamingURL.DEFAULT_SCHEME + "://" + hostPort + "/AdminLogin");
        ResettableRmiProxyFactoryBean bean = (ResettableRmiProxyFactoryBean)applicationContext.getBean("&adminLogin");
        bean.setServiceUrl(adminServiceNamingURL.toString());
        bean.resetStub();
        AdminLogin adminLogin = (AdminLogin)applicationContext.getBean("adminLogin");
        adminContext = adminLogin.login(adminlogin, adminpass);
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
        Preferences preferences = Preferences.getPreferences();

        History serverUrlHistory = preferences.getHistory(Preferences.SERVICE_URL);
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
