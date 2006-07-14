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
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.console.action.ImportCertificateAction;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.spring.remoting.http.SecureHttpClient;
import com.l7tech.spring.remoting.http.SecureHttpInvokerRequestExecutor;
import com.l7tech.spring.remoting.rmi.NamingURL;
import com.l7tech.spring.remoting.rmi.ResettableRmiProxyFactoryBean;
import com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler;
import com.l7tech.spring.remoting.rmi.ssl.SslRMIClientSocketFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.remoting.RemoteAccessException;
import sun.security.x509.X500Name;

import javax.security.auth.login.LoginException;
import java.io.IOException;
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

    //- PUBLIC

    /**
     *
     */
    public SecurityProviderImpl() {
        hostBuffer = new StringBuffer();
        certsByHost = new HashMap();
        permissiveSSLTrustFailureHandler = getTrustFailureHandler(hostBuffer);
    }

    /**
     * Determines if the passed credentials will grant access to the admin service.
     * If successful, those credentials will be cached for future admin ws calls.
     */
    public void login(PasswordAuthentication creds, String host, boolean validate)
      throws LoginException, VersionException, RemoteException {

        boolean authenticated = false;
        serverCertificateChain = null;
        resetCredentials();
        setCredentials(creds.getUserName(), creds.getPassword());

        try {
            setPermissiveSslTrustHandler(host, validate);

            AdminLogin adminLogin = getAdminLoginRemoteReference(host);

            // dummy call, just to establish SSL connection (if none)
            adminLogin.getServerCertificate("admin");
            if (Thread.currentThread().isInterrupted()) throw new LoginException("Login interrupted.");

            // check cert if new
            X509Certificate serverCertificate = null;
            if(serverCertificateChain != null) {
                serverCertificate = serverCertificateChain[0];
                certsByHost.put(host, serverCertificate);
            }
            else {
                serverCertificate = (X509Certificate) certsByHost.get(host);
            }

            if (serverCertificate != null) {
                validateServer(creds, serverCertificate, adminLogin, host);
                importCertificate(serverCertificate);
                certsByHost.values().remove(serverCertificate);
            }


            AdminLoginResult result = adminLogin.login(creds.getUserName(), new String(creds.getPassword()));

            SecureHttpInvokerRequestExecutor secureInvoker =
                    (SecureHttpInvokerRequestExecutor) applicationContext.getBean("httpRequestExecutor");

            secureInvoker.setSession(getHost(host), getPort(host), result.getSessionCookie());

            // version checks
            String remoteVersion = result.getAdminContext().getVersion();
            if (!SecureSpanConstants.ADMIN_PROTOCOL_VERSION.equals(remoteVersion)) {
                throw new VersionException("Version mismatch", SecureSpanConstants.ADMIN_PROTOCOL_VERSION, remoteVersion);
            }
            String remoteSoftwareVersion = result.getAdminContext().getSoftwareVersion();
            if (!BuildInfo.getProductVersion().equals(remoteSoftwareVersion)) {
                throw new VersionException("Version mismatch", BuildInfo.getProductVersion(), remoteSoftwareVersion);
            }

            authenticated = true;

            AdminContext ac = new AdminContextBean(
                            (IdentityAdmin) applicationContext.getBean("identityAdmin"),
                            (AuditAdmin) applicationContext.getBean("auditAdmin"),
                            (ServiceAdmin) applicationContext.getBean("serviceAdmin"),
                            (JmsAdmin) applicationContext.getBean("jmsAdmin"),
                            (TrustedCertAdmin) applicationContext.getBean("trustedCertAdmin"),
                            (CustomAssertionsRegistrar) applicationContext.getBean("customAssertionsRegistrar"),
                            (ClusterStatusAdmin) applicationContext.getBean("clusterStatusAdmin"),
                            (SchemaAdmin) applicationContext.getBean("schemaAdmin"),
                            (KerberosAdmin) applicationContext.getBean("kerberosAdmin"),
                            (RbacAdmin) applicationContext.getBean("rbacAdmin"),
                            "", "");

            LogonEvent le = new LogonEvent(ac, LogonEvent.LOGON);
            applicationContext.publishEvent(le);
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

    /**
     * Logoff the session, explicitely
     */
    public void logoff() {
        LogonEvent le = new LogonEvent(this, LogonEvent.LOGOFF);
        applicationContext.publishEvent(le);
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
                onLogoff(le);
            } else {
                onLogon(le);
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SecurityProviderImpl.class.getName());
    private static final String INVALID_PEER_HOST_PREFIX = "Invalid peer: ";

    private final SSLTrustFailureHandler permissiveSSLTrustFailureHandler;
    private final StringBuffer hostBuffer;
    private final Map certsByHost;
    private ApplicationContext applicationContext;
    private X509Certificate[] serverCertificateChain;

    /**
     * Initialize the SSL logic around login. This registers the trust failure handler
     * that will be invoked if the server cert is not yet present.
     */
    private void setPermissiveSslTrustHandler(String host, boolean validate) {
        hostBuffer.setLength(0);
        if(validate) hostBuffer.append(getHost(host));
        SslRMIClientSocketFactory.setTrustFailureHandler(permissiveSSLTrustFailureHandler);
        SecureHttpClient.setTrustFailureHandler(permissiveSSLTrustFailureHandler);
    }

    private void resetSslTrustHandler() {
        SslRMIClientSocketFactory.setTrustFailureHandler(null);
        SecureHttpClient.setTrustFailureHandler(null);
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
            public boolean handle(CertificateException e, X509Certificate[] chain, String authType) {
                if (chain == null || chain.length == 0) {
                    return false;
                }
                String peerHost = null;
                try {
                    peerHost = new X500Name(chain[0].getSubjectX500Principal().getName()).getCommonName();
                }
                catch (IOException e1) {
                    logger.log(Level.WARNING, "Could not obtain the CN from X500 Name in cert", e);
                    throw new RuntimeException(e1);
                }

                if(e!=null) serverCertificateChain = chain;

                if (hostBuffer.length()==0 || hostBuffer.toString().equals(peerHost)) {
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
            SecureHttpInvokerRequestExecutor secureInvoker =
                    (SecureHttpInvokerRequestExecutor) applicationContext.getBean("httpRequestExecutor");

            secureInvoker.setSession(getHost(host), getPort(host), null);
        }

        return (AdminLogin) applicationContext.getBean("adminLogin");
    }

    private void onLogoff(LogonEvent e) {
        logger.finer("Disconnect message received, invalidating service lookup reference");
        resetCredentials();
    }

    private void onLogon(LogonEvent le) {
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
