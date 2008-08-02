package com.l7tech.server.ems;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

/**
 * Security for web application access
 */
public interface EmsSecurityManager {
    
    /**
     * Login the user
     *
     * @param session The current HttpSession
     * @param username Username for login
     * @param password Password for login
     * @return True if logged in
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
     * Check if the user of the current session is permitted to access the given page.
     *
     * @param session The HttpSession for the request
     * @param request The HttpServletRequest being attempted
     * @return True if access is permitted
     */
    boolean canAccess(  HttpSession session, HttpServletRequest request );

    /**
     * Change password for the current user.
     *
     * @param session The Session for the user
     * @param password The current password
     * @param newPassword The new password
     * @return True if the password was updated
     */
    boolean changePassword(  HttpSession session, String password, String newPassword );
}
