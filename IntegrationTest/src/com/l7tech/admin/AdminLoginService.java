package com.l7tech.admin;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.log.LogSinkAdmin;
import com.l7tech.common.policy.PolicyAdmin;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.security.InvalidHostCertificateException;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.manager.automator.Main;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.spring.remoting.http.ConfigurableHttpInvokerRequestExecutor;
import com.l7tech.spring.remoting.rmi.NamingURL;
import com.l7tech.spring.remoting.rmi.ResettableRmiProxyFactoryBean;
import com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler;
import com.l7tech.spring.remoting.rmi.ssl.SslRMIClientSocketFactory;
import org.springframework.context.ApplicationContext;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Properties;

/**
 * Connects to the SSG sever, trusts the certificate and can login/logout. When it logs into the
 * SSG, it creates a new AdminContext, which can be used to access the other services.
 *
 * @auther Norman Jordan
 */
public class AdminLoginService {
    private static final String INVALID_PEER_HOST_PREFIX = "Invalid peer: ";
    private final String TRUST_STORE_FILE = System.getProperties().getProperty("user.home") + File.separator + ".l7tech" + File.separator + "trustStore";
    private final String TRUST_STORE_PASSWORD = "password";

    private ApplicationContext applicationContext;
    private AdminLogin adminLogin;
    private X509Certificate[] serverCertificateChain;

    /**
     * Creates a new instance of AdminLoginService and initializes the trust store.
     */
    public AdminLoginService() {
        applicationContext = Main.getApplicationContext();
        try {
            getTrustStore();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the trust store by reading the file ~/.l7tech/trustStore
     *
     * @return The new key store
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     */
    private KeyStore getTrustStore()
      throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        File storeFile = new File(TRUST_STORE_FILE);
        String defaultKeystoreType = KeyStore.getDefaultType();
        KeyStore ts = KeyStore.getInstance(defaultKeystoreType);
        final char[] password = TRUST_STORE_PASSWORD.toCharArray();

        if (!storeFile.exists()) {
            ts.load(null, null);
            FileOutputStream fo = null;
            fo = new FileOutputStream(storeFile);
            try {
                ts.store(fo, password);
            } finally {
                fo.close();
            }
            return ts;
        }
        final FileInputStream fis = new FileInputStream(storeFile);
        try {
            ts.load(fis, password);
        } finally {
            fis.close();
        }
        return ts;
    }

    /**
     * Retrieves a remote reference to the AdminLogin service for the SSG.
     *
     * @return A remote reference to the AdminLogin service
     * @throws SecurityException
     * @throws MalformedURLException
     */
    private AdminLogin getAdminLoginRemoteReference() throws SecurityException, MalformedURLException {
        Properties props = Main.getProperties();
        
        Object beanFactory = applicationContext.getBean("&adminLogin");
        if (beanFactory instanceof ResettableRmiProxyFactoryBean) {
            ResettableRmiProxyFactoryBean bean = (ResettableRmiProxyFactoryBean) beanFactory;
            NamingURL adminServiceNamingURL = NamingURL.parse(NamingURL.DEFAULT_SCHEME + "://" + props.getProperty("ssg.host") + "/AdminLogin");
            bean.setServiceUrl(adminServiceNamingURL.toString());
            bean.resetStub();
        }
        else {
            getConfigurableHttpInvokerRequestExecutor().setSession(props.getProperty("ssg.host"), Integer.parseInt(props.getProperty("ssg.port")), null);
        }

        return (AdminLogin) applicationContext.getBean("adminLogin");
    }

    /**
     * Returns the HttpRequestExecutor object to use.
     * @return The HttpRequestExecutor object to use
     */
    private ConfigurableHttpInvokerRequestExecutor getConfigurableHttpInvokerRequestExecutor() {
        return (ConfigurableHttpInvokerRequestExecutor) applicationContext.getBean("httpRequestExecutor");
    }

    /**
     * Setup the SSL trust handler, with a handler that will accept the key.
     *
     * @param host The name of the host that the key should be for (only needed if validate is true)
     * @param validate If true then verify that the hostname from the key matches the expected hostname
     */
    private void setPermissiveSslTrustHandler(String host, boolean validate) {
        final StringBuffer hostBuffer = new StringBuffer();
        if(validate) hostBuffer.append(host);

        SSLTrustFailureHandler handler = new SSLTrustFailureHandler() {
            public boolean handle(CertificateException e, X509Certificate[] chain, String authType, boolean failure) {
                if (chain == null || chain.length == 0) {
                    return false;
                }
                final String peerHost = CertUtils.getCn(chain[0]);

                if(e!=null && failure) serverCertificateChain = chain;

                if (hostBuffer.length()==0 || hostBuffer.toString().equals(peerHost)) {
                    return true;
                }

                throw new RuntimeException(INVALID_PEER_HOST_PREFIX + peerHost);
            }
        };
        SslRMIClientSocketFactory.setTrustFailureHandler(handler);
        getConfigurableHttpInvokerRequestExecutor().setTrustFailureHandler(handler);
    }

    /**
     * Ensure that the server knows our password and the cert matches the SSL cert.
     */
    private void validateServer(PasswordAuthentication credentials, X509Certificate serverCertificate, AdminLogin adminLogin, String host)
      throws SecurityException {
        byte[] certificate = adminLogin.getServerCertificate(credentials.getUserName());
        try {
            String password = new String(credentials.getPassword());
            String encodedPassword = HexUtils.encodePasswd(credentials.getUserName(), password);
            java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-1");
            final byte[] bytes = encodedPassword.getBytes();
            d.update(bytes);
            d.update(serverCertificate.getEncoded());
            d.update(bytes);
            byte[] digested = d.digest();
            if (!Arrays.equals(certificate, digested)) {
                throw new InvalidHostCertificateException("Unable to verify the server certificate at "+host);
            }
        } catch (NoSuchAlgorithmException e) {
            throw (SecurityException) new SecurityException().initCause(e);
        } catch (CertificateEncodingException e) {
            throw (SecurityException) new SecurityException().initCause(e);
        }
    }

    /**
     * Imports the SSL certificate into the trust store.
     *
     * @param cert The certificate to import
     * @param hostname The hostname from the certificate
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     */
    public void importSsgCert(X509Certificate cert, String hostname) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] trustStorPassword = TRUST_STORE_PASSWORD.toCharArray();
        String trustStoreFile = TRUST_STORE_FILE;
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

        ks.setCertificateEntry(hostname, cert);

        FileOutputStream ksfos = null;
        try {
            ksfos = new FileOutputStream(trustStoreFile);
            ks.store(ksfos, trustStorPassword);
        } finally {
            if (ksfos != null)
                ksfos.close();
        }
    }

    /**
     * Logs into the AdminService and returns a new AdminContext.
     *
     * @param username The admin username to use
     * @param password The admin password to use
     * @return A new AdminContext
     * @throws MalformedURLException
     * @throws LoginException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     */
    public AdminContext login(String username, String password)
      throws MalformedURLException, LoginException, KeyStoreException, NoSuchAlgorithmException,
      IOException, CertificateException {
        setPermissiveSslTrustHandler(Main.getProperties().getProperty("ssg.host"), false);

        adminLogin = getAdminLoginRemoteReference();

        // dummy call, just to establish SSL connection (if none)
        adminLogin.getServerCertificate("admin");
        if (Thread.currentThread().isInterrupted()) throw new LoginException("Login interrupted.");

        if (serverCertificateChain != null && serverCertificateChain.length > 0 && serverCertificateChain[0] != null) {
            PasswordAuthentication creds = new PasswordAuthentication(username, password.toCharArray());
            validateServer(creds, serverCertificateChain[0], adminLogin, Main.getProperties().getProperty("ssg.host"));
            importSsgCert(serverCertificateChain[0], serverCertificateChain[0].getSubjectDN().getName());
        }

        AdminLoginResult result = adminLogin.login(username, password);

        String sessionCookie = result.getSessionCookie();
        User user = result.getUser();
        String remoteSoftwareVersion = result.getAdminContext().getSoftwareVersion();
        String remoteVersion = result.getAdminContext().getVersion();

        return setAuthenticated(sessionCookie, user, remoteSoftwareVersion, remoteVersion);
    }

    /**
     * Creates a new AdminContext using the provided values.
     *
     * @param sessionCookie The session cookie from the login response.
     * @param user The User object from the login response
     * @param remoteSoftwareVersion The version of the remote SSG
     * @param remoteVersion The protocol version of the remote SSG
     * @return A new AdminContext
     */
    private AdminContext setAuthenticated(String sessionCookie, User user, String remoteSoftwareVersion, String remoteVersion)
    {
        getConfigurableHttpInvokerRequestExecutor().setSession(Main.getProperties().getProperty("ssg.host"),
                Integer.parseInt(Main.getProperties().getProperty("ssg.port")), sessionCookie);

        AdminContext ac = new AdminContextBean(
                        (IdentityAdmin) applicationContext.getBean("identityAdmin"),
                        (AuditAdmin) applicationContext.getBean("auditAdmin"),
                        (ServiceAdmin) applicationContext.getBean("serviceAdmin"),
                        (JmsAdmin) applicationContext.getBean("jmsAdmin"),
                        (FtpAdmin) applicationContext.getBean("ftpAdmin"),
                        (TrustedCertAdmin) applicationContext.getBean("trustedCertAdmin"),
                        (CustomAssertionsRegistrar) applicationContext.getBean("customAssertionsRegistrar"),
                        (ClusterStatusAdmin) applicationContext.getBean("clusterStatusAdmin"),
                        (SchemaAdmin) applicationContext.getBean("schemaAdmin"),
                        (KerberosAdmin) applicationContext.getBean("kerberosAdmin"),
                        (RbacAdmin) applicationContext.getBean("rbacAdmin"),
                        (TransportAdmin) applicationContext.getBean("transportAdmin"),
                        (PolicyAdmin) applicationContext.getBean("policyAdmin"),
                        (LogSinkAdmin) applicationContext.getBean("logSinkAdmin"),
                        "", "");

        LogonDialog.setLastRemoteSoftwareVersion(remoteSoftwareVersion);
        LogonDialog.setLastRemoteProtocolVersion(remoteVersion);
        LogonEvent le = new LogonEvent(ac, LogonEvent.LOGON);
        applicationContext.publishEvent(le);

        return ac;
    }

    /**
     * Logs out from the AdminLogin service. This invalidates the AdminContext that was returned from
     * the login request.
     */
    public void logout() {
        adminLogin.logout();

        serverCertificateChain = null;
    }
}
