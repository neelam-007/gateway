package com.l7tech.console;

import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.console.util.History;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class SsgAdminSession {
    protected static final Logger logger = Logger.getLogger(SsgAdminSession.class.getName());
    private ApplicationContext applicationContext;

    public SsgAdminSession() throws Exception {
        this(new String[]{});
    }

    public SsgAdminSession(String[] args) throws Exception {
        System.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );
        if (args.length >= 1) {
            hostPort = args[0];

            if (args.length >= 3) {
                adminlogin = args[1];
                adminpass = args[2];
            }
        }

        JdkLoggerConfigurator.configure("com.l7tech.console", "com/l7tech/console/resources/logging.properties");
        // initialize preferences
        SsmPreferences preferences = TopComponents.getInstance().getPreferences();
        if ( preferences != null ) {
            preferences.updateFromProperties(System.getProperties(), false);
            preferences.updateSystemProperties();
        }
        // application context
        applicationContext = createApplicationContext();
        Registry.setDefault( applicationContext.getBean("registry", Registry.class) );

        System.out.println("Connecting to " + hostPort);
        SecurityProvider securityProvider = applicationContext.getBean("securityProvider", SecurityProvider.class);
        securityProvider.login(new PasswordAuthentication(adminlogin, adminpass.toCharArray()), hostPort, false, null);
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private ApplicationContext createApplicationContext() {
        String[] ctxNames = new String[]{System.getProperty("ssm.application.context")};
        if (ctxNames[0] == null) {
            ctxNames = new String[]{
                    "com/l7tech/console/resources/beans-context.xml",
                    "com/l7tech/console/resources/beans-application.xml",
            };
        }
        return new ClassPathXmlApplicationContext(ctxNames);
   }

    private String adminlogin = "admin";
    private String adminpass = "password";
    private String hostPort = defaultHostPort();

    private String defaultHostPort() {
        SsmPreferences preferences = TopComponents.getInstance().getPreferences();

        if ( preferences != null ) {
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
        }

        return "localhost:8443";
    }
}
