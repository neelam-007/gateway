package com.l7tech.server.admin;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.admin.AdminLoginResult;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.identity.FailAttemptsExceededException;
import com.l7tech.identity.FailInactivityPeriodExceededException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.http.HttpClientCertToken;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.FailedAdminLoginEvent;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.Config;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO clean up any common code between this class and AdminLoginImpl
 */
public class AdminLoginHelper extends ApplicationObjectSupport {

    //- PUBLIC

    public AdminLoginHelper( final AdminSessionManager sessionManager,
                             final Config config ) {
        this.sessionManager = sessionManager;
        this.config = config;
    }

    public AdminLoginResult login( final X509Certificate cert ) throws AccessControlException, LoginException {

        //the implementation of this method will be very similar to the one that uses the username/password as
        //login credentials, except that we'll use the client certificate as the login credentials instead
        if (cert == null) throw new AccessControlException("Client certificate required.");

        String remoteIp = null;
        LoginCredentials creds = null;
        try {
            //make certificate login credentials
            creds = LoginCredentials.makeLoginCredentials( new HttpClientCertToken(cert), null);
            User user = sessionManager.authenticate( creds );

            boolean remoteLogin = true;

            try {
                remoteIp = RemoteUtils.getClientHost();
            } catch ( ServerNotActiveException snae) {
                remoteLogin = false;
            }

            if (user == null) {
                getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + creds.getLogin() + "'"));
                throw new FailedLoginException("'" + creds.getLogin() + "'" + " could not be authenticated");
            }

            if (remoteLogin) {
                logger.info("User '" + user.getLogin() + "' logged in from IP '" + remoteIp + "'.");
            } else {
                logger.finer("User '" + user.getLogin() + "' logged in locally.");
            }

            String cookie = "-";
            if (remoteLogin) {
                // If local, caller is responsible for generating event/session if required
                getApplicationContext().publishEvent(new LogonEvent(user, LogonEvent.LOGON));
                cookie = sessionManager.createSession(user, null);
            }

            return new AdminLoginResult(user, cookie, SecureSpanConstants.ADMIN_PROTOCOL_VERSION, BuildInfo.getProductVersion(), getLogonWarningBanner());
        } catch ( ObjectModelException e) {
            logger.log( Level.WARNING, "Authentication provider error", e);
            throw buildAccessControlException("Authentication failed", e);
        } catch ( FailAttemptsExceededException faee) {
            //shouldn't happen for certificates
            getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + creds.getLogin() + "'"));
            throw new AccountLockedException("'" + creds.getLogin() + "'" + " exceeded max. failed logon attempts.");
        } catch ( FailInactivityPeriodExceededException fipee) {
            getApplicationContext().publishEvent(new FailedAdminLoginEvent(this, remoteIp, "Failed admin login for login '" + creds.getLogin() + "'"));
            throw new AccountLockedException("'" + creds.getLogin() + "'" + " exceeded inactivity period.");
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( AdminLoginHelper.class.getName() );

    private final AdminSessionManager sessionManager;
    private final Config config;

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    private AccessControlException buildAccessControlException( final String message, final Throwable cause ) {
        return (AccessControlException)new AccessControlException(message).initCause(cause);
    }

    private String getLogonWarningBanner() {
        String prop = config.getProperty( ServerConfig.PARAM_LOGON_WARNING_BANNER, null );

        // If the banner prop value just contains whitespace, then set the prop as null.
        if (prop != null && prop.trim().isEmpty()) prop = null;

        return prop;
    }

}
