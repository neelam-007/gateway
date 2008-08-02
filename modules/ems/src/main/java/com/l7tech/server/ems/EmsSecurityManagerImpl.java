package com.l7tech.server.ems;

import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;
import com.l7tech.gateway.common.admin.ValidationRuntimeException;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.UpdateException;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manages which pages are secure and performs authentication
 */
public class EmsSecurityManagerImpl implements EmsSecurityManager {

    //- PUBLIC

    public EmsSecurityManagerImpl( final IdentityProviderFactory identityProviderFactory ) {
        this.identityProviderFactory = identityProviderFactory;
    }

    /**
     * Check if the current request is authenticated.
     *
     * @return True if authenticated
     */
    public static boolean isAuthenticated() {
        boolean authenticated = false;

        HttpServletRequest request = RemoteUtils.getHttpServletRequest();
        if ( request != null ) {
            HttpSession session = request.getSession(true);
            authenticated = session.getAttribute(ATTR_ID) != null;
        }

        return authenticated;
    }

    /**
     * Login the user
     *
     * @param session The current HttpSession
     * @param username Username for login
     * @param password Password for login
     * @return True if logged in
     */
    public boolean login( final HttpSession session, final String username, final String password ) {
        LoginCredentials creds = new LoginCredentials(username, password.toCharArray(), null);
        User user = null;

        logger.info("Authenticating user '"+username+"'.");    
        try {
            for ( IdentityProvider provider : identityProviderFactory.findAllIdentityProviders() ) {
                IdentityProviderConfig config = provider.getConfig();
                if ( config.isAdminEnabled() ) {
                    try {
                        AuthenticationResult authResult = ((AuthenticatingIdentityProvider)provider).authenticate(creds);
                        user = authResult == null ? null : authResult.getUser();
                    } catch (AuthenticationException e) {
                        logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
                    } catch (ValidationRuntimeException e) {
                        logger.info("Validation failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
                    }
                } else {
                    logger.info("Administrative users not enabled for provider '"+config.getName()+"'.");
                }
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Error loading identity providers", fe);            
        }

        boolean authenticated = user != null;
        if ( authenticated ) {
            session.setAttribute(ATTR_ID, user);
        }

        logger.info("Authenticating user '"+username+"', auth result is '"+authenticated+"'.");    

        return authenticated;
    }

    /**
     * Log out the current session.
     *
     * @param session  The HttpSession to log out
     * @return True if session was logged out
     */
    public boolean logout( final HttpSession session ) {
        boolean loggedOut = false;

        if ( session.getAttribute(ATTR_ID) != null ) {
            loggedOut = true;
            session.setAttribute(ATTR_ID, null);
        }

        return loggedOut;
    }

    /**
     * Change password for the current user.
     *
     * @param session The Session for the user
     * @param password The current password
     * @param newPassword The new password
     * @return True if the password was updated
     */
    public boolean changePassword( HttpSession session, String password, String newPassword ) {
        boolean passwordChanged = false;

        User user = (User) session.getAttribute(ATTR_ID);

        logger.info("Changing password for user '"+user.getLogin()+"'.");
        
        LoginCredentials creds = new LoginCredentials(user.getLogin(), password.toCharArray(), null);
        IdentityProvider provider = null;
        try {
            provider = identityProviderFactory.getProvider( user.getProviderId() );
            if ( provider instanceof InternalIdentityProvider ) {
                InternalIdentityProvider internalIdentityProvider = (InternalIdentityProvider) provider;
                AuthenticationResult authResult = internalIdentityProvider.authenticate(creds);
                if ( authResult != null ) {
                    InternalUser authUser = (InternalUser) authResult.getUser();
                    authUser.setCleartextPassword(newPassword);
                    internalIdentityProvider.getUserManager().update(authUser);
                    passwordChanged = true;
                }
            }
        } catch (InvalidPasswordException e) {
            logger.info("Invalid password on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
        } catch (AuthenticationException e) {
            logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
        } catch (ValidationRuntimeException e) {
            logger.info("Validation failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Error loading identity providers", fe);
        } catch (UpdateException ue) {
            logger.log(Level.WARNING, "Error updating user", ue);
        }

        return passwordChanged;
    }

    /**
     * Check if the user of the current session is permitted to access the given page.
     *
     * @param session The HttpSession for the request
     * @param request The HttpServletRequest being attempted
     * @return True if access is permitted
     */
    @SuppressWarnings({"unchecked"})
    public boolean canAccess(  final HttpSession session, final HttpServletRequest request ) {
        // TODO Clean this up (use secured and unsecured sections?)
        return session.getAttribute(ATTR_ID) != null ||
                request.getRequestURI().equals("/Login.html") ||
                (request.getRequestURI().equals("/") && TextUtils.toString(request.getQueryString()).contains("wicket:interface")) ||
                request.getRequestURI().startsWith("/css") ||
                request.getRequestURI().startsWith("/images") ||
                request.getRequestURI().startsWith("/js") ||
                request.getRequestURI().startsWith("/yui");
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EmsSecurityManagerImpl.class.getName() );

    private static final String ATTR_ID = "com.l7tech.loginid";

    private final IdentityProviderFactory identityProviderFactory;
}
