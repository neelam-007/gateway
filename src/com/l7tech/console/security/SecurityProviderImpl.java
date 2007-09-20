package com.l7tech.console.security;

import com.l7tech.admin.AdminContext;
import com.l7tech.admin.AdminContextBean;
import com.l7tech.admin.AdminLogin;
import com.l7tech.admin.AdminLoginResult;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.VersionException;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.console.action.ImportCertificateAction;
import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.spring.remoting.http.ConfigurableHttpInvokerRequestExecutor;
import com.l7tech.spring.remoting.rmi.NamingURL;
import com.l7tech.spring.remoting.rmi.ResettableRmiProxyFactoryBean;
import com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler;
import com.l7tech.spring.remoting.rmi.ssl.SslRMIClientSocketFactory;
import com.l7tech.common.transport.TransportAdmin;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.remoting.RemoteAccessException;

import javax.security.auth.login.LoginException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default SSM <code>SecurityProvider</code> implementaiton that is a central security
 * component in SSM.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SecurityProviderImpl extends SecurityProvider
  implements ApplicationContextAware, ApplicationListener {
    private static final Boolean suppressVersionCheck = SyspropUtil.getBoolean("com.l7tech.console.suppressVersionCheck");

    //- PUBLIC

    /**
     *
     */
    public SecurityProviderImpl() {
        hostBuffer = new StringBuffer();
        certsByHost = new HashMap<String, X509Certificate>();
        permissiveSSLTrustFailureHandler = getTrustFailureHandler(hostBuffer);
    }

    /**
     * Determines if the passed credentials will grant access to the admin service.
     * If successful, those credentials will be cached for future admin ws calls.
     */
    public void login(PasswordAuthentication creds, String host, boolean validate)
            throws LoginException, VersionException, RemoteException
    {
        boolean authenticated = false;
        serverCertificateChain = null;
        resetCredentials();

        try {
            setPermissiveSslTrustHandler(host, validate);

            AdminLogin adminLogin = getAdminLoginRemoteReference(host);

            // dummy call, just to establish SSL connection (if none)
            byte[] maybeSSGCert = adminLogin.getServerCertificate("admin");
            if (Thread.currentThread().isInterrupted()) throw new LoginException("Login interrupted.");

            // check cert if new
            X509Certificate serverCertificate;
            if(serverCertificateChain != null) {
                serverCertificate = serverCertificateChain[0];
                certsByHost.put(host, serverCertificate);
            }
            else {
                serverCertificate = certsByHost.get(host);
            }

            if (serverCertificate != null) {
                validateServer(creds, serverCertificate, adminLogin, host);
                importCertificate(serverCertificate);
                certsByHost.values().remove(serverCertificate);
            }

            AdminLoginResult result = adminLogin.login(creds.getUserName(), new String(creds.getPassword()));
            // Update the principal with the actual internal user

            String remoteVersion = checkRemoteProtocolVersion(result);
            String remoteSoftwareVersion = checkRemoteSoftwareVersion(result);

            authenticated = true;

            User user = result.getUser();
            String sessionCookie = result.getSessionCookie();
            setAuthenticated(sessionCookie, user, remoteSoftwareVersion, remoteVersion, host);
        }
        catch(RemoteAccessException e) {
            Throwable cause = ExceptionUtils.unnestToRoot(e);
            String message = cause.getMessage();
            if(cause instanceof RuntimeException && message!=null && message.startsWith(INVALID_PEER_HOST_PREFIX)) {
                throw new InvalidHostNameException(host, message.substring(INVALID_PEER_HOST_PREFIX.length()));
            }
            else {
                throw e;
            }
        }
        catch(MalformedURLException murle) {
            throw (LoginException) new LoginException("Invalid host '"+host+"'.").initCause(murle);
        }
        finally {
            if (SslRMIClientSocketFactory.hasTrustFailureHandler()) {
                resetSslTrustHandler();
                if (!authenticated) {
                    resetCredentials();
                }
            }
        }
    }

    private String checkRemoteSoftwareVersion(AdminLoginResult result) throws VersionException {
        String remoteSoftwareVersion = result.getAdminContext().getSoftwareVersion();
        if (!Boolean.TRUE.equals(suppressVersionCheck) && !BuildInfo.getProductVersion().equals(remoteSoftwareVersion)) {
            throw new VersionException("Version mismatch", BuildInfo.getProductVersion(), remoteSoftwareVersion);
        }
        return remoteSoftwareVersion;
    }

    private String checkRemoteProtocolVersion(AdminLoginResult result) throws VersionException {
        // version checks
        String remoteVersion = result.getAdminContext().getVersion();
        if (!SecureSpanConstants.ADMIN_PROTOCOL_VERSION.equals(remoteVersion)) {
            throw new VersionException("Version mismatch", SecureSpanConstants.ADMIN_PROTOCOL_VERSION, remoteVersion);
        }
        return remoteVersion;
    }

    // Called by the Applet to connect to the server
    public void login(String sessionId, String host)
            throws LoginException, VersionException, RemoteException
    {
        boolean authenticated = false;

        final AdminLogin adminLogin;
        try {
            adminLogin = getAdminLoginRemoteReference(host);

            AdminLoginResult result = adminLogin.resume(sessionId);

            String remoteVersion = checkRemoteProtocolVersion(result);
            String remoteSoftwareVersion = checkRemoteSoftwareVersion(result);

            authenticated = true;

            User user = result.getUser();
            String sessionCookie = result.getSessionCookie(); // Server is allowed to assign a new one if it wishes

            setAuthenticated(sessionCookie, user, remoteSoftwareVersion, remoteVersion, host);

        } catch (MalformedURLException e) {
            throw (LoginException) new LoginException("Invalid host '"+host+"'.").initCause(e);
        } catch (AuthenticationException e) {
            throw (LoginException) new LoginException("Session invalid or has lost admin permissions").initCause(e);
        } catch (SecurityException e) {
            throw (LoginException) new LoginException("Session invalid or has lost admin permissions").initCause(e);
        } finally {
            if (!authenticated) {
                resetCredentials();
            }
        }
    }

    /**
     * Enable the authenticated state.
     */
    private void setAuthenticated(String sessionCookie, User user, String remoteSoftwareVersion, String remoteVersion, String remoteHost)
    {
        resetCredentials();

        getConfigurableHttpInvokerRequestExecutor().setSession(getHost(remoteHost), getPort(remoteHost), sessionCookie);
        this.sessionCookie = sessionCookie;

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
                        "", "");

        synchronized (this) {
            this.user = user;
        }

        LogonDialog.setLastRemoteSoftwareVersion(remoteSoftwareVersion);
        LogonDialog.setLastRemoteProtocolVersion(remoteVersion);
        LogonEvent le = new LogonEvent(ac, LogonEvent.LOGON);
        applicationContext.publishEvent(le);
    }

    /**
     * Change admin password.
     */
    public void changePassword(final PasswordAuthentication auth, final PasswordAuthentication newAuth)
            throws LoginException, RemoteException {
        AdminLogin adminLogin = (AdminLogin) applicationContext.getBean("adminLogin", AdminLogin.class);
        adminLogin.changePassword(new String(auth.getPassword()), new String(newAuth.getPassword()));
    }

    /**
     * Logoff the session, explicitely
     */
    public void logoff() {
        LogonEvent le = new LogonEvent(this, LogonEvent.LOGOFF);
        applicationContext.publishEvent(le);
        if (sessionCookie != null && sessionCookie.trim().length() > 0) {
            final String cookie = sessionCookie;
            final AdminLogin adminLogin = (AdminLogin)applicationContext.getBean("adminLogin");
            if (adminLogin != null) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            adminLogin.logout();
                        } catch (RemoteException e) {
                            logger.log(Level.WARNING, "Error logging out old admin session: " + ExceptionUtils.getMessage(e), e);
                        } finally {
                            getConfigurableHttpInvokerRequestExecutor().clearSessionIfMatches(cookie);
                        }
                    }
                }).start();
            }
        }
        sessionCookie = null;
    }

    /**
     * Set the ApplicationContext that this object runs in.
     * Normally this call will be used to initialize the object.
     * <p>Invoked after population of normal bean properties but before an init
     * callback like InitializingBean's afterPropertiesSet or a custom init-method.
     * Invoked after ResourceLoaderAware's setResourceLoader.
     *
     * @param ctx ApplicationContext object to be used by this object
     * @throws org.springframework.context.ApplicationContextException
     *          in case of applicationContext initialization errors
     * @throws org.springframework.beans.BeansException
     *          if thrown by application applicationContext methods
     * @see org.springframework.beans.factory.BeanInitializationException
     */
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof LogonEvent) {
            LogonEvent le = (LogonEvent)event;
            if (le.getType() == LogonEvent.LOGOFF) {
                onLogoff();
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SecurityProviderImpl.class.getName());
    private static final String INVALID_PEER_HOST_PREFIX = "Invalid peer: ";

    private final SSLTrustFailureHandler permissiveSSLTrustFailureHandler;
    private final StringBuffer hostBuffer;
    private final Map<String, X509Certificate> certsByHost;
    private ApplicationContext applicationContext;
    private X509Certificate[] serverCertificateChain;
    private String sessionCookie;

    /**
     * Initialize the SSL logic around login. This registers the trust failure handler
     * that will be invoked if the server cert is not yet present.
     */
    private void setPermissiveSslTrustHandler(String host, boolean validate) {
        hostBuffer.setLength(0);
        if(validate) hostBuffer.append(getHost(host));
        SslRMIClientSocketFactory.setTrustFailureHandler(permissiveSSLTrustFailureHandler);
        getConfigurableHttpInvokerRequestExecutor().setTrustFailureHandler(permissiveSSLTrustFailureHandler);
    }

    private void resetSslTrustHandler() {
        SslRMIClientSocketFactory.setTrustFailureHandler(null);
        getConfigurableHttpInvokerRequestExecutor().setTrustFailureHandler(null);
    }

    /**
     * Get a trust failure handler that uses the given stringbuffer as its guide when trusting
     * hosts SSL certificates.
     *
     * If the hostBuffer is empty then ANY host will be trusted. Else the names must match.
     *
     * If the CertificateException is null then we are just performing a host name check.
     */
    private SSLTrustFailureHandler getTrustFailureHandler(final StringBuffer hostBuffer) {
        return new SSLTrustFailureHandler() {
            public boolean handle(CertificateException e, X509Certificate[] chain, String authType, boolean failure) {
                if (chain == null || chain.length == 0) {
                    return false;
                }
                final String peerHost = CertUtils.getCn(chain[0]);

                if(e!=null && failure) serverCertificateChain = chain;

                if (hostBuffer.length()==0 || hostBuffer.toString().equals(peerHost)) {
                    TopComponents.getInstance().setSSGCert(chain);
                    return true;
                }

                throw new RuntimeException(INVALID_PEER_HOST_PREFIX + peerHost);
            }
        };
    }

    /**
     * Invoked after the successful login.
     */
    private void importCertificate(X509Certificate serverCertificate) {
        // handle import of new certificate (gathered during trust failure handling)
        try {
            ImportCertificateAction.importSsgCertificate(serverCertificate);
        }
        catch(Exception e) {
            logger.log(Level.SEVERE, "Error importing new certifiate.", e);
        }
    }

    /**
     * Ensure that the server knows our password and the cert matches the SSL cert.
     */
    private void validateServer(PasswordAuthentication credentials, X509Certificate serverCertificate, AdminLogin adminLogin, String host)
      throws RemoteException, SecurityException {
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
                logger.warning("Unable to verify the server certificate at (could mean invalid password entered) " + host);
                throw new InvalidHostCertificateException("Unable to verify the server certificate at "+host);
            }
        } catch (NoSuchAlgorithmException e) {
            throw (SecurityException) new SecurityException().initCause(e);
        } catch (CertificateEncodingException e) {
            throw (SecurityException) new SecurityException().initCause(e);
        }
    }

    private AdminLogin getAdminLoginRemoteReference(String host) throws SecurityException, MalformedURLException {
        Object beanFactory = applicationContext.getBean("&adminLogin");
        if (beanFactory instanceof ResettableRmiProxyFactoryBean) {
            ResettableRmiProxyFactoryBean bean = (ResettableRmiProxyFactoryBean) beanFactory;
            NamingURL adminServiceNamingURL = NamingURL.parse(NamingURL.DEFAULT_SCHEME + "://" + host + "/AdminLogin");
            bean.setServiceUrl(adminServiceNamingURL.toString());
            bean.resetStub();
        }
        else {
            getConfigurableHttpInvokerRequestExecutor().setSession(getHost(host), getPort(host), null);
        }

        return (AdminLogin) applicationContext.getBean("adminLogin");
    }

    private ConfigurableHttpInvokerRequestExecutor getConfigurableHttpInvokerRequestExecutor() {
        return (ConfigurableHttpInvokerRequestExecutor) applicationContext.getBean("httpRequestExecutor");        
    }

    private void onLogoff() {
        logger.finer("Disconnect message received, invalidating service lookup reference");
        resetCredentials();
    }

    private String getHost(String hostAndPossiblyPort) {
        String host = hostAndPossiblyPort;

        if (host != null) {
            int sep = host.indexOf(':');
            if (sep > 0) {
                host = host.substring(0, sep);
            }
        }

        return host;
    }

    private int getPort(String hostAndPossiblyPort) {
        int port = 8443;

        if (hostAndPossiblyPort != null) {
            int sep = hostAndPossiblyPort.indexOf(':');
            if (sep > 0) {
                try {
                    port = Integer.parseInt(hostAndPossiblyPort.substring(sep+1).trim());
                }
                catch(NumberFormatException nfe) {
                    // use default
                }
            }
        }

        return port;
    }
}
