package com.l7tech.server.ems;

import com.l7tech.identity.User;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.io.Serializable;

import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.apache.wicket.Component;

/**
 * Security for web application access
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public interface EmsSecurityManager {
    
    /**
     * Login the user
     *
     * @param session The current HttpSession
     * @param username Username for login
     * @param password Password for login
     * @return True if logged in
     * @throws NotLicensedException if not licensed
     */
    boolean login( HttpSession session, String username, String password );

    /**
     * Log out the current session.
     *
     * @param session  The HttpSession to log out
     * @return True if session was logged out
     */
    boolean logout( HttpSession session );

    /**
     * Check if the current request is authenticated.
     *
     * @return True if authenticated
     */
    boolean isAuthenticated();

    /**
     * Check if the current request is authenticated for the given component.
     *
     * @return True if authenticated
     */
    boolean isAuthenticated( final Component component );

    /**
     * Check if the current request is authenticated for the given class.
     *
     * @return True if authenticated
     */
    boolean isAuthenticated( final Class componentClass );

    /**
     * Check if the current request is authorized for the given component.
     *
     * @return True if authorized
     */
    boolean isAuthorized( final Component component );

    /**
     * Check if the ESM is licensed or the component does not require a license.
     *
     * <p>If the given component does not specify any licensing requirement then
     * parent components are checked until a definitive source is found.</p>
     *
     * @param component The component (heriarchy) to check. 
     * @return True if the component is licensed or does not require a license.
     */
    boolean isLicensed( final Component component );

    /**
     * Check if the ESM is licensed for the given class.
     *
     * @param componentClass The component class to check.
     * @return True if the component is licensed or does not require a license.
     */
    boolean isLicensed( final Class componentClass );

    /**
     * Check if the user of the current session is permitted to access the given page.
     *
     * @param session The HttpSession for the request
     * @param request The HttpServletRequest being attempted
     * @return True if access is permitted
     */
    @Transactional(propagation=SUPPORTS,readOnly=true)
    boolean canAccess(  HttpSession session, HttpServletRequest request );

    /**
     * Check if the user of the current session is permitted to perform an operation.
     *
     * @param ao The attempted operation
     * @return true if permitted
     */
    @Transactional(propagation=SUPPORTS,readOnly=true)
    boolean hasPermission( AttemptedOperation ao );

    /**
     * Change password for the current user.
     *
     * @param session The Session for the user
     * @param password The current password
     * @param newPassword The new password
     * @return True if the password was updated
     */
    boolean changePassword( HttpSession session, String password, String newPassword );

    /**
     * Access login information for the current session.
     *
     * @param session The Session for the user
     * @return The session information or null if not set
     */
    @Transactional(propagation=SUPPORTS,readOnly=true)
    LoginInfo getLoginInfo( HttpSession session );

    /**
     * Login information
     */
    public final class LoginInfo implements Serializable {
        private final String login;
        private final Date date;
        private final User user;

        public LoginInfo( final String login, final Date date, final User user ) {
            this.login = login;
            this.date = date;
            this.user = user;
        }

        public String getLogin() {
            return login;
        }

        public Date getDate() {
            return date;
        }

        public User getUser() {
            return user;
        }
    }

    public final class NotLicensedException extends RuntimeException{}
}
