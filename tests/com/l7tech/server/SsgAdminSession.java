/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.admin.AdminContext;
import com.l7tech.admin.AdminLogin;
import com.l7tech.admin.AdminLoginResult;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.console.util.History;
import com.l7tech.console.util.Preferences;
import com.l7tech.identity.UserBean;
import com.l7tech.spring.remoting.rmi.NamingURL;
import com.l7tech.spring.remoting.rmi.ResettableRmiProxyFactoryBean;
import com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler;
import com.l7tech.spring.remoting.rmi.ssl.SslRMIClientSocketFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.security.auth.Subject;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
        Preferences preferences = Preferences.getPreferences();
        preferences.updateFromProperties(System.getProperties(), false);
        preferences.updateSystemProperties();
        // application context
        applicationContext = createApplicationContext();
        System.out.println("Connecting to " + hostPort);
        NamingURL adminServiceNamingURL = NamingURL.parse(NamingURL.DEFAULT_SCHEME + "://" + hostPort + "/AdminLogin");
        ResettableRmiProxyFactoryBean bean = (ResettableRmiProxyFactoryBean)applicationContext.getBean("&adminLogin");
        SslRMIClientSocketFactory.setTrustFailureHandler(new SSLTrustFailureHandler() {
            public boolean handle(CertificateException e, X509Certificate[] chain, String authType) {
                return true;
            }
        });
        bean.setServiceUrl(adminServiceNamingURL.toString());
        bean.resetStub();
        AdminLogin adminLogin = (AdminLogin)applicationContext.getBean("adminLogin");
        AdminLoginResult loginResult = adminLogin.login(adminlogin, adminpass);

        subject.getPrivateCredentials().clear();
        subject.getPrivateCredentials().add(loginResult.getSessionCookie());
        adminContext = loginResult.getAdminContext();
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
