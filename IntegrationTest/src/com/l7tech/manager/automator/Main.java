package com.l7tech.manager.automator;

import com.l7tech.admin.AdminLoginService;
import com.l7tech.manager.automator.jaxb.JaxbEntityManager;
import com.l7tech.util.SyspropUtil;
import com.l7tech.gateway.common.admin.AdminContext;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * Connects an SSG and revokes the certificates for the users specified in the
 * properties: manager.automator.iip.userCertsToRevoke, manager.automator.msad.userCertsToRevoke, and
 * manager.automator.ldap.userCertsToRevoke. For the IIP users, their passwords are then set to
 * "password".
 *
 * @auther Norman Jordan
 */
public class Main {
    private static ApplicationContext applicationContext;
    private static Properties properties = new Properties();

    /**
     * Creates a new spring application context.
     */
    private static void createApplicationContext() {
        String ctxName = SyspropUtil.getProperty("ssm.application.context");
        if (ctxName == null) {
            ctxName = "com/l7tech/console/resources/beans-context.xml";
        }
        String ctxHeavy = "com/l7tech/console/resources/beans-application.xml";
        applicationContext = new ClassPathXmlApplicationContext(new String[]{ctxHeavy, ctxName});
    }

    /**
     * Returns the sprint application context.
     *
     * @return The spring application context
     */
    public static ApplicationContext getApplicationContext() {
        if(applicationContext == null) {
            createApplicationContext();
        }

        return applicationContext;
    }

    /**
     * Returns the properties for this application.
     *
     * @return The properties
     */
    public static Properties getProperties() {
        return properties;
    }

    /**
     * Logs into the SSG, and revokes the certificates.
     * @param args
     */
    public static void main(String[] args) {
        try {
            properties.load(new FileReader("IntegrationTest/src/manager_automator.properties"));
        } catch(IOException e) {
            System.err.println("Failed to load properties file.");
            System.exit(-1);
        }

        try {
            // Login to the SSG
            AdminLoginService adminLoginService = new AdminLoginService();
            AdminContext adminContext = adminLoginService.login("admin", "password");

            if(args.length == 0 || args[0].equals("revoke-certs")) {
                // Update the accounts
                AccountUpdater accountUpdater = new AccountUpdater(adminContext);
                accountUpdater.updateAccounts();
            } else if(args[0].equals("set-audit-threshold") && args.length == 2) {
                // Update the audit.messageThreshold cluster property
                String newThreshold = args[1];
                ClusterPropertyManager clusterPropertyManager = new ClusterPropertyManager(adminContext);
                clusterPropertyManager.setClusterProperty("audit.messageThreshold", newThreshold);
            } else if(args[0].equals("add-trusted-key") && args.length == 2) {
                TrustedKeyManager trustedKeyManager = new TrustedKeyManager(adminContext);
                trustedKeyManager.addTrustedCertificate(args[1]);
            } else if(args[0].equals("entity-manager") && args.length == 2) {
                JaxbEntityManager manager = new JaxbEntityManager(adminContext);
                //manager.doTests(args[1]);
                if(true)return;

                if(args[1].equals("download")){
                    manager.downloadAllEntities();
                }else if(args[1].equals("upload")){
                    manager.uploadAllEntities();
                }else if(args[1].equals("delete")){
                    manager.deleteAllEntities();
                }
            } else if(args[0].equals("setup-private-keys")) {
                TrustedKeyManager trustedKeyManager = new TrustedKeyManager(adminContext);
                trustedKeyManager.setupPrivateKeys(adminContext.getIdentityAdmin());
            } else if(args[0].equals("create-services")) {
                ServiceManager serviceManager = new ServiceManager(adminContext.getServiceAdmin());
                serviceManager.addServiceResolutionServices();
            }

            // Logout
            adminLoginService.logout();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
