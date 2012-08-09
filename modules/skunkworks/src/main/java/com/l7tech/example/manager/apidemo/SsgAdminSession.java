package com.l7tech.example.manager.apidemo;

import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.HeavySsmPreferences;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.common.io.SslCertificateSniffer;
import com.l7tech.util.SyspropUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.security.auth.login.LoginException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

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

//        SsmApplication app = new SsmApplication() {
//            public void showHelpTopicsRoot() {}
//            public void run() {}
//            public boolean isApplet() {
//                return false;
//            }
//        };
//        app.setApplicationContext(applicationContext);
//        TopComponents.getInstance().registerComponent("mainWindow", new MainWindow(app));

        try {
            credentialManager.getAuthenticationProvider().login(new PasswordAuthentication(this.adminLogin, this.adminPass.toCharArray()), this.ssgHost, false, null);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not login to SSG (hint: possible version problem | is gateway running?): " +e.getMessage(), e);
        }
    }

    private ApplicationContext createApplicationContext() {
        String ctxName = SyspropUtil.getProperty( "ssm.application.context" );
        if (ctxName == null) {
            ctxName = "com/l7tech/console/resources/beans-context.xml";
        }
        String ctxHeavy = "com/l7tech/console/resources/beans-application.xml";

        ApplicationContext context = new ClassPathXmlApplicationContext(new String[]{ctxHeavy, ctxName});
        Registry.setDefault( context.getBean("registry", Registry.class) );

        checkCertStoreDependency(ssgHost);

        return context;
    }

    /**
     * This workaround blocking cert trust issue by adding the ssg certificate in the local trust store. To avoid
     * trust bootstrap workaround, run the SSM GUI from running host once.
     */
    private void checkCertStoreDependency(String host) {
        try {
            String url = "https://" + host + ":8443";
            X509Certificate[] cert = SslCertificateSniffer.retrieveCertFromUrl(url, true);
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] trustStorPassword = "password".toCharArray();
            String trustStoreFile = System.getProperties().getProperty("user.home") + File.separator + ".l7tech" +  File.separator + "trustStore";
            try {
                FileInputStream ksfis = new FileInputStream(trustStoreFile);
                try {
                    ks.load(ksfis, trustStorPassword);
                } finally {
                    ksfis.close();
                }
            } catch (FileNotFoundException e) {
                // Create a new one.
                ks.load(null, trustStorPassword);
            }

            logger.info("Found certs: " + cert.length);
            ks.setCertificateEntry(host, cert[0]);

            FileOutputStream ksfos = null;
            try {
                ksfos = new FileOutputStream(trustStoreFile);
                ks.store(ksfos, trustStorPassword);
            } finally {
                if (ksfos != null)
                    ksfos.close();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot bootstrap trusted cert", e);
        }
    }



    public ClusterStatusAdmin getClusterStatusAdmin() {
        return Registry.getDefault().getClusterStatusAdmin();
    }

    public ServiceAdmin getServiceAdmin() {
        return Registry.getDefault().getServiceManager();
    }

    public FolderAdmin getFolderAdmin(){
        return Registry.getDefault().getFolderAdmin();
    }
    public IdentityAdmin getIdentityAdmin() throws RemoteException {
        return Registry.getDefault().getIdentityAdmin();
	}

    public UDDIRegistryAdmin getUDDIRegistryAdmin(){
        return Registry.getDefault().getUDDIRegistryAdmin();
    }
}
