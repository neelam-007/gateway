package com.l7tech.server.ems;

import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.security.rbac.RoleManagerIdentitySourceSupport;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.Authorizer;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.EntityType;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Date;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.wicket.Component;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

/**
 * Manages which pages are secure and performs authentication
 */
public class EsmSecurityManagerImpl extends RoleManagerIdentitySourceSupport implements ApplicationContextAware, EsmSecurityManager {

    //- PUBLIC

    public EsmSecurityManagerImpl( final IdentityProviderFactory identityProviderFactory,
                                   final RoleManager roleManager,
                                   final LicenseManager licenseManager ) {
        this.identityProviderFactory = identityProviderFactory;
        this.roleManager = roleManager;
        this.licenseManager = licenseManager;
    }

    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        if ( this.applicationContext == null ) {
            this.applicationContext = applicationContext;
        }
    }

    /**
     * Check if the current request is authenticated.
     *
     * @return True if authenticated
     */
    @Override
    public boolean isAuthenticated() {
        boolean authenticated = false;

        HttpServletRequest request = RemoteUtils.getHttpServletRequest();
        if ( request != null ) {
            HttpSession session = request.getSession(true);
            authenticated = session.getAttribute(ATTR_ID) != null;
        }

        return authenticated;
    }

    @Override
    public boolean isAuthenticated( final Component component ) {
        boolean authenticated = isAuthenticated();

        if ( !authenticated ) {
            Component comp = component;
            while ( comp != null ) {
                Administrative admin = comp.getClass().getAnnotation( Administrative.class );
                if ( admin != null ) {
                    authenticated = !admin.authenticated(); // authenticated if the component does not require authentication ...
                    break;
                }
                comp = comp.getParent();
            }

        }

        return authenticated;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean isAuthenticated( final Class componentClass ) {
        boolean authenticated = isAuthenticated();

        if ( !authenticated ) {
            Administrative admin = (Administrative)componentClass.getAnnotation( Administrative.class );
            if ( admin != null ) {
                authenticated = !admin.authenticated(); // authenticated if the component does not require authentication ...
            }
        }

        return authenticated;
    }

    @Override
    public boolean isAuthorized( final Component component ) {
        boolean authorized = isAuthenticated();

        if ( authorized ) {
            Component comp = component;
            while ( comp != null ) {
                if ( comp instanceof SecureComponent) {
                    AttemptedOperation operation = ((SecureComponent)comp).getAttemptedOperation();
                    if ( operation != null ) {
                        authorized = hasPermission( operation );
                        break;
                    }
                }

                comp = comp.getParent();
            }
        }

        return authorized;
    }

    @Override
    public boolean isAuthorized( final Class componentClass ) {
        boolean authorized = isAuthenticated( componentClass );

        if ( authorized ) {
            authorized = hasPermission( componentClass );
        }

        return authorized;
    }

    /**
     * Check if the ESM is licensed or the component does not require a license.
     *
     * @return True if authenticated
     */
    @Override
    public boolean isLicensed( final Component component ) {
        boolean licensed = false;

        if ( licenseManager.isFeatureEnabled( "set:admin" ) ) {
            licensed = true;
        } else {
            Component comp = component;
            while ( comp != null ) {
                Administrative admin = comp.getClass().getAnnotation( Administrative.class );
                if ( admin != null ) {
                    licensed = !admin.licensed(); // licensed if the component does not require a license ...
                    break;
                }
                comp = comp.getParent();
            }
        }

        return licensed;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean isLicensed( final Class componentClass ) {
        boolean licensed = false;

        if ( licenseManager.isFeatureEnabled( "set:admin" ) ) {
            licensed = true;
        } else {
            Administrative admin = (Administrative)componentClass.getAnnotation( Administrative.class );
            if ( admin != null ) {
                licensed = !admin.licensed(); // licensed if the component does not require license ...
            }
        }

        return licensed;
    }

    /**
     * Login the user
     *
     * @param session The current HttpSession
     * @param username Username for login
     * @param password Password for login
     * @return True if logged in
     */
    @Override
    public boolean login( final HttpSession session, final String username, final String password ) {
        LoginCredentials creds = new LoginCredentials(username, password.toCharArray(), null);
        User user = null;

        logger.info("Authenticating user '"+username+"'.");
        for ( IdentityProvider provider : getAdminIdentityProviders() ) {
            try {
                AuthenticationResult authResult = ((AuthenticatingIdentityProvider)provider).authenticate(creds);
                user = authResult == null ? null : authResult.getUser();
                if ( applicationContext != null && user != null ) {
                    applicationContext.publishEvent( new LogonEvent(user, LogonEvent.LOGON) );
                }
            } catch (AuthenticationException e) {
                logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
            }
        }

        boolean authenticated = user != null;
        if ( authenticated ) {
            if ( !new UserAuthorizer(user).hasPermission( new AttemptedDeleteAll(EntityType.ANY) ) ) {
                // perform license check only if non-admin user
                if ( !licenseManager.isFeatureEnabled( "set:admin" ) ) {
                    throw new NotLicensedException();
                }
            }

            session.setAttribute(ATTR_ID, user);
            session.setAttribute(ATTR_DATE, new Date());
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
    @Override
    public boolean logout( final HttpSession session ) {
        boolean loggedOut = false;

        final Object user = session.getAttribute(ATTR_ID);
        if ( user != null ) {
            loggedOut = true;

            if ( applicationContext != null && user instanceof User ) {
                applicationContext.publishEvent( new LogonEvent(user, LogonEvent.LOGOFF) );
            }

            session.setAttribute(ATTR_ID, null);
            session.setAttribute(ATTR_DATE, null);
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
    @Override
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
    @Override
    @SuppressWarnings({"unchecked"})
    public boolean canAccess(  final HttpSession session, final HttpServletRequest request ) {
        // TODO Clean this up (use secured and unsecured sections?)
        return session.getAttribute(ATTR_ID) != null ||
                request.getRequestURI().equals("/favicon.ico") || // We don't have a "/favicon.ico" but browsers like to ask for this
                request.getRequestURI().equals("/Login.html") ||
                (request.getRequestURI().equals("/") && TextUtils.toString(request.getQueryString()).contains("wicket:interface")) ||
                request.getRequestURI().startsWith("/css") ||
                request.getRequestURI().startsWith("/images") ||
                request.getRequestURI().startsWith("/js") ||
                request.getRequestURI().startsWith("/yui") ||
                request.getRequestURI().startsWith("/resources/org.apache.wicket.markup.html.WicketEventReference/wicket-event.js") ||
                request.getRequestURI().startsWith("/resources/org.apache.wicket.ajax.WicketAjaxReference/wicket-ajax.js") ||
                request.getRequestURI().startsWith("/resources/org.apache.wicket.ajax.AbstractDefaultAjaxBehavior/wicket-ajax-debug.js") ||                
                request.getRequestURI().startsWith("/resources/com.l7tech.server.ems.pages.YuiCommon/$up$/resources/yui/button/assets/skins/sam/button.css") ||
                request.getRequestURI().startsWith("/resources/com.l7tech.server.ems.pages.YuiCommon/$up$/resources/yui/yahoo-dom-event/yahoo-dom-event.js") ||
                request.getRequestURI().startsWith("/resources/com.l7tech.server.ems.pages.YuiCommon/$up$/resources/yui/element/element-beta-min.js") ||
                request.getRequestURI().startsWith("/resources/com.l7tech.server.ems.pages.YuiCommon/$up$/resources/yui/button/button-min.js") ||
                request.getRequestURI().startsWith("/resources/com.l7tech.server.ems.pages.YuiCommon/$up$/resources/css/l7-yui-skin.css") ||
                request.getRequestURI().startsWith("/resources/com.l7tech.server.ems.pages.YuiCommon/$up$/resources/yui/assets/skins/sam/sprite.png");
    }

    /**
     * Check if the user of the current session is permitted to perform an operation.
     *
     * @param ao The attempted operation
     * @return true if permitted
     */
    @Override
    public boolean hasPermission( final AttemptedOperation ao ) {
        return authorizer.hasPermission( ao );
    }

    /**
     * Check if the user of the current session has the annotated permitted(s).
     *
     * @param clazz The annotated class
     * @return true if permitted
     */
    @Override
    public boolean hasPermission( final Class clazz ) {
        return authorizer.hasPermission( clazz );
    }

    @Override
    public LoginInfo getLoginInfo( final HttpSession session ) {
        final User user = (User) session.getAttribute(ATTR_ID);
        final Date date = (Date) session.getAttribute(ATTR_DATE);

        return user == null ?
                null :
                new LoginInfo(user.getLogin(), date, user);
    }

    //- PROTECTED
    
    @Override
    protected Set<IdentityProvider> getAdminIdentityProviders() {
        Set<IdentityProvider> providers = new LinkedHashSet<IdentityProvider>();

        try {
            for ( IdentityProvider provider : identityProviderFactory.findAllIdentityProviders() ) {
                IdentityProviderConfig config = provider.getConfig();
                if ( config.isAdminEnabled() ) {
                    providers.add( provider );
                } else {
                    logger.info("Administrative users not enabled for provider '"+config.getName()+"'.");
                }
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Error loading identity providers", fe);
        }

        return providers;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EsmSecurityManagerImpl.class.getName() );

    private static final String ATTR_ID = "com.l7tech.loginid";
    private static final String ATTR_DATE = "com.l7tech.logindate";

    private final IdentityProviderFactory identityProviderFactory;
    private final LicenseManager licenseManager;
    private final SessionAuthorizer authorizer = new SessionAuthorizer();
    private ApplicationContext applicationContext;

    private final class SessionAuthorizer extends Authorizer {
        @Override
        public Collection<Permission> getUserPermissions() {
            Set<Permission> perms = new HashSet<Permission>();

            User u = JaasUtils.getCurrentUser();

            if (u != null) {
                try {
                    final Collection<Role> assignedRoles = roleManager.getAssignedRoles(u);
                    for (Role role : assignedRoles) {
                        for (final Permission perm : role.getPermissions()) {
                            Permission perm2 = perm.getAnonymousClone();
                            perms.add(perm2);
                        }
                    }
                } catch ( FindException fe ) {
                    logger.log( Level.WARNING, "Error accessing roles for user '"+u.getId()+"'.", fe );                
                }
            }
            
            return perms;
        }
    }

    private final class UserAuthorizer extends Authorizer {
        private final User user;

        public UserAuthorizer( final User user ) {
            this.user = user;
        }

        @Override
        public Collection<Permission> getUserPermissions() {
            Set<Permission> perms = new HashSet<Permission>();

            User u = user;

            if (u != null) {
                try {
                    final Collection<Role> assignedRoles = roleManager.getAssignedRoles(u);
                    for (Role role : assignedRoles) {
                        for (final Permission perm : role.getPermissions()) {
                            Permission perm2 = perm.getAnonymousClone();
                            perms.add(perm2);
                        }
                    }
                } catch ( FindException fe ) {
                    logger.log( Level.WARNING, "Error accessing roles for user '"+u.getId()+"'.", fe );
                }
            }

            return perms;
        }
    }
}
