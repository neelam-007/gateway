/**
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.*;
import com.l7tech.gateway.common.VersionException;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.io.CertUtils;
import com.l7tech.console.action.ImportCertificateAction;
import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.util.AdminContextFactory;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.gateway.common.admin.*;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.spring.remoting.http.ConfigurableHttpInvokerRequestExecutor;
import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;
import com.l7tech.objectmodel.InvalidPasswordException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;

import javax.security.auth.login.LoginException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.AccessControlException;
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
 * Default SSM <code>SecurityProvider</code> implementation that is a central security
 * component in SSM.
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

    @Override
    public void acceptServerCertificate( final X509Certificate certificate ) {
        certsByHost.values().remove(certificate);
    }

    /**
     * Determines if the passed credentials will grant access to the admin service.
     * If successful, those credentials will be cached for future admin ws calls.
     */
    @Override
    public void login(PasswordAuthentication creds, String host, boolean validate, String newPassword)
            throws LoginException, VersionException, InvalidPasswordException {
        boolean authenticated = false;
        serverCertificateChain = null;
        resetCredentials();

        ConfigurableHttpInvokerRequestExecutor chire = getConfigurableHttpInvokerRequestExecutor();
        try {
            setPermissiveSslTrustHandler(host, validate);
            Pair<String, Integer> hostAndPort = getHostAndPort(host);
            chire.setSession( hostAndPort.left, hostAndPort.right, null );
            AdminLogin adminLogin = getAdminLoginRemoteReference();

            // dummy call, just to establish SSL connection (if none)
            adminLogin.getServerCertificate("admin");
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

            AdminLoginResult result;
            //determine the type of logon process should be performed
            if (newPassword == null) {
                //proceed with normal logon process
                result = adminLogin.login(creds.getUserName(), new String(creds.getPassword()));
            } else {
                //proceed with password change and then logon process
                result = adminLogin.loginWithPasswordUpdate(creds.getUserName(), new String(creds.getPassword()), newPassword);
            }

            // Update the principal with the actual internal user

            String remoteVersion = checkRemoteProtocolVersion(result);
            String remoteSoftwareVersion = checkRemoteSoftwareVersion(result);

            authenticated = true;

            User user = result.getUser();
            String sessionCookie = result.getSessionCookie();
            setAuthenticated(sessionCookie, user, remoteSoftwareVersion, remoteVersion, host);
            TopComponents.getInstance().setLogonWarningBanner(result.getLogonWarningBanner());
        }
        catch( AccessControlException ace ) {
            throw new LoginException( ExceptionUtils.getMessage(ace) );
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
            resetSslTrustHandler();
            if (!authenticated) {
                resetCredentials();
            }
            chire.setSession( null, -1, null );
        }
    }

    private String checkRemoteSoftwareVersion(AdminLoginResult result) throws VersionException {
        String remoteSoftwareVersion = result.getSoftwareVersion();
        if (!Boolean.TRUE.equals(suppressVersionCheck) && !BuildInfo.getProductVersion().equals(remoteSoftwareVersion)) {
            throw new VersionException("Version mismatch", BuildInfo.getProductVersion(), remoteSoftwareVersion);
        }
        return remoteSoftwareVersion;
    }

    private String checkRemoteProtocolVersion(AdminLoginResult result) throws VersionException {
        // version checks
        String remoteVersion = result.getVersion();
        if (!SecureSpanConstants.ADMIN_PROTOCOL_VERSION.equals(remoteVersion)) {
            throw new VersionException("Version mismatch", SecureSpanConstants.ADMIN_PROTOCOL_VERSION, remoteVersion);
        }
        return remoteVersion;
    }

    // Called by the Applet to connect to the server
    @Override
    public void login(String sessionId, String host)
            throws LoginException, VersionException {
        boolean authenticated = false;

        ConfigurableHttpInvokerRequestExecutor chire = getConfigurableHttpInvokerRequestExecutor();
        final AdminLogin adminLogin;
        try {
            adminLogin = getAdminLoginRemoteReference();

            Pair<String, Integer> hostAndPort = getHostAndPort(host);
            chire.setSession( hostAndPort.left, hostAndPort.right, null );
            AdminLoginResult result = adminLogin.resume(sessionId);

            String remoteVersion = checkRemoteProtocolVersion(result);
            String remoteSoftwareVersion = checkRemoteSoftwareVersion(result);

            authenticated = true;

            User user = result.getUser();
            String sessionCookie = result.getSessionCookie(); // Server is allowed to assign a new one if it wishes
            setAuthenticated(sessionCookie, user, remoteSoftwareVersion, remoteVersion, host);
            TopComponents.getInstance().setLogonWarningBanner(result.getLogonWarningBanner());
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
            chire.setSession( null, -1, null );
        }
    }

    /**
     * Enable the authenticated state.
     */
    private void setAuthenticated(String sessionCookie, User user, String remoteSoftwareVersion, String remoteVersion, String remoteHost)
    {
        resetCredentials();

        this.sessionCookie = sessionCookie;
        this.sessionHost = remoteHost;

        final Pair<String, Integer> hostAndPort = getHostAndPort(remoteHost);
        try {
            final String displayHost = remoteHost.endsWith( ":" + hostAndPort.right ) ?
                    hostAndPort.left + ":" + hostAndPort.right :
                    hostAndPort.left;
            TopComponents.getInstance().setSsgURL(new URL("http://" + displayHost));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid SSG host or URL: " + remoteHost, e);
        }

        AdminContextFactory factory = applicationContext.getBean("adminContextFactory", AdminContextFactory.class);
        AdminContext ac = factory.buildAdminContext( hostAndPort.left, hostAndPort.right, sessionCookie );

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
    @Override
    public void changePassword(final PasswordAuthentication auth, final PasswordAuthentication newAuth)
            throws LoginException {
        if ( Registry.getDefault().isAdminContextPresent() ) {
            Registry.getDefault().getAdminLogin().changePassword(new String(auth.getPassword()), new String(newAuth.getPassword()));
        } else {
            throw new LoginException("Not logged in.");
        }
    }

    /**
     * Logoff the session, explicitely
     */
    @Override
    public void logoff() {
        LogonEvent le = new LogonEvent(this, LogonEvent.LOGOFF);
        applicationContext.publishEvent(le);
        if (sessionCookie != null && sessionCookie.trim().length() > 0) {
            final String cookie = sessionCookie;
            final String host = sessionHost;
            final AdminLogin adminLogin = (AdminLogin)applicationContext.getBean("adminLogin");
            if (adminLogin != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ConfigurableHttpInvokerRequestExecutor chire = getConfigurableHttpInvokerRequestExecutor();
                        try {
                            Pair<String, Integer> hostAndPort = getHostAndPort(host);
                            chire.setSession( hostAndPort.left, hostAndPort.right, cookie );
                            adminLogin.logout();
                        } catch (RuntimeException e) {
                            String msg = "Error logging out admin session";

                            if(ExceptionUtils.causedBy(e, AccessControlException.class)){
                                //Expected exception. The gateway most likely restarted or was shut down.
                                //Use the supplied exception message if available, as it likely more specific
                                AccessControlException causedBy = ExceptionUtils.getCauseIfCausedBy(e, AccessControlException.class);
                                final String logMsg = causedBy.getMessage() != null? causedBy.getMessage(): msg;
                                logger.log(Level.WARNING, logMsg, ExceptionUtils.getDebugException(e));
                            }
                            else if (ExceptionUtils.causedBy(e,RemoteConnectFailureException.class)){
                                ConnectException causedBy = ExceptionUtils.getCauseIfCausedBy(e, ConnectException.class);
                                final String logMsg = causedBy!=null||causedBy.getMessage() != null? causedBy.getMessage(): msg;
                                logger.log(Level.WARNING, logMsg, ExceptionUtils.getDebugException(e));
                            }
                            else
                            {
                                logger.log(Level.WARNING, msg + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));                                
                            }
                        } finally {
                            chire.setSession( null, -1, null );
                        }
                    }
                }).start();
            }
        }
        sessionCookie = null;
        sessionHost = null;
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
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
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
    private static final int DEFAULT_PORT= 8443;
    private static final String INVALID_PEER_HOST_PREFIX = "Invalid peer: ";

    private final SSLTrustFailureHandler permissiveSSLTrustFailureHandler;
    private final StringBuffer hostBuffer;
    private final Map<String, X509Certificate> certsByHost;
    private ApplicationContext applicationContext;
    private X509Certificate[] serverCertificateChain;
    private String sessionCookie;
    private String sessionHost;

    /**
     * Initialize the SSL logic around login. This registers the trust failure handler
     * that will be invoked if the server cert is not yet present.
     */
    private void setPermissiveSslTrustHandler(String host, boolean validate) {
        hostBuffer.setLength(0);
        if(validate) hostBuffer.append(getHostAndPort(host).left);
        getConfigurableHttpInvokerRequestExecutor().setTrustFailureHandler(permissiveSSLTrustFailureHandler);
    }

    private void resetSslTrustHandler() {
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
            @Override
            public boolean handle(CertificateException e, X509Certificate[] chain, String authType, boolean failure) {
                if (chain == null || chain.length == 0) {
                    return false;
                }
                final String peerHost = CertUtils.extractFirstCommonNameFromCertificate(chain[0]);

                if(e!=null && failure) serverCertificateChain = chain;

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
      throws SecurityException {
        byte[] certificate = adminLogin.getServerCertificate(credentials.getUserName());
        try {
            String password = new String(credentials.getPassword());
            String encodedPassword = HexUtils.encodePasswd(credentials.getUserName(), password, HexUtils.REALM);
            java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-1");
            final byte[] bytes = encodedPassword.getBytes();
            d.update(bytes);
            d.update(serverCertificate.getEncoded());
            d.update(bytes);
            byte[] digested = d.digest();
            if (!Arrays.equals(certificate, digested)) {
                logger.warning("Unable to verify the server certificate at (could mean invalid password entered) " + host);
                throw new InvalidHostCertificateException(serverCertificate, "Unable to verify the server certificate at "+host);
            }
        } catch (NoSuchAlgorithmException e) {
            throw (SecurityException) new SecurityException().initCause(e);
        } catch (CertificateEncodingException e) {
            throw (SecurityException) new SecurityException().initCause(e);
        }
    }

    private AdminLogin getAdminLoginRemoteReference() throws SecurityException, MalformedURLException {
        return (AdminLogin) applicationContext.getBean("adminLogin");
    }

    private ConfigurableHttpInvokerRequestExecutor getConfigurableHttpInvokerRequestExecutor() {
        return (ConfigurableHttpInvokerRequestExecutor) applicationContext.getBean("httpRequestExecutor");
    }

    private void onLogoff() {
        logger.finer("Disconnect message received, invalidating service lookup reference");
        resetCredentials();
    }

    private Pair<String,Integer> getHostAndPort(String hostAndPossiblyPort) {
        Pair<String, String> hostAndPort = InetAddressUtil.getHostAndPort(hostAndPossiblyPort, Integer.toString(DEFAULT_PORT));
        int port;
        try {
            port = Integer.valueOf(hostAndPort.right);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port: " + hostAndPort.right, e);
        }
        return new Pair<String, Integer>(hostAndPort.left, port);
    }
}
