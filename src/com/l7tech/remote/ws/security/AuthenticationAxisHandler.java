package com.l7tech.remote.ws.security;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.encoding.Base64;
import org.apache.axis.transport.http.HTTPConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Axis handler that authenticates admin requests.
 * <p/>
 * This axis handler class does the following
 * 1. reads a basic auth header from the http request
 * 2. unbase64 it
 * 3. MD5s it
 * 4. compare it to the hash value for this user in the database
 * 5. if authentication succeeds, it checks that user is member of SSGAdmin local group
 * <p/>
 * The password has in the database is expected to have the following format:
 * MD5(username::password) hex encoded
 * <p/>
 * It must be declared in the server-config.wsdd file in order to be invoked as per below:
 * <p/>
 * <p/>
 * <transport name="http">
 * <requestFlow>
 * <handler type="URLMapper"/>
 * <handler type="java:com.l7tech.remote.ws.security.AuthenticationAxisHandler"/>
 * </requestFlow>
 * </transport>
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: June 2, 2003
 */
public class AuthenticationAxisHandler extends org.apache.axis.handlers.BasicHandler {
    public static final long SESSION_MAX_LENGTH = 120000; // two minutes

    public AuthenticationAxisHandler() {
    }

    /**
     * Invoked by the axis engine. if successful, will feed the messageContext with
     * a property whose key is AuthenticationAxisHandler.AUTHENTICATED_USER and whose
     * value is the authenticated user object. If authentication fails, this handler
     * will throw an exception (org.apache.axis.AxisFault).
     */
    public void invoke(MessageContext messageContext) throws AxisFault {
        // CHECK FOR SESSION
        // get the HTTPRequest object
        HttpServletRequest servletrequest = (HttpServletRequest)messageContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        if (servletrequest == null) {
            logger.log(Level.SEVERE, "cannot retrieve servlet request from axis context");
        } else {
            // check out for a session
            HttpSession session = servletrequest.getSession(false);
            if (session != null && session.getAttribute(AUTHENTICATED_USER) != null) {
                if (System.currentTimeMillis() - session.getCreationTime() > SESSION_MAX_LENGTH) {
                    // session reset
                    session.removeAttribute(AUTHENTICATED_USER);
                    session.invalidate();
                } else {
                    // SESSION PASSTHROUGH HERE. THE REQUEST IS BYPASSING AUTHENTICATION
                    return;
                }
            }
        }

        // Process the Auth stuff in the headers
        String tmp = (String)messageContext.getProperty(HTTPConstants.HEADER_AUTHORIZATION);
        if (tmp != null) tmp = tmp.trim();

        User authenticatedUser = null;

        // TRY BASIC AUTHENTICATION
        if (tmp != null && tmp.startsWith("Basic ")) {
            String decodedAuthValue = new String(Base64.decode(tmp.substring(6)));
            if (decodedAuthValue == null) {
                throw new AxisFault("cannot decode basic header");
            }
            authenticatedUser = authenticateBasicToken(decodedAuthValue);
            // AUTHENTICATION SUCCESS
            if (authenticatedUser != null) messageContext.setUsername(authenticatedUser.getLogin());
        }

        // WAS AUTHENTICATION COMPLETE?
        if (authenticatedUser == null) throw new AxisFault("Server.Unauthorized", "com.l7tech.remote.ws.security.AuthenticationAxisHandler failed", null, null);

        // NOW, PROCEED TO AUTHORIZATION
        if (authorizeAdminMembership(authenticatedUser)) {
            // CREATE SESSION
            HttpSession session = servletrequest.getSession(true);
            session.setAttribute(AUTHENTICATED_USER, authenticatedUser);
            session.setMaxInactiveInterval(30); // 30 seconds times you out
            logger.info("created admin session for user " + authenticatedUser.getLogin());
        } else {
            logger.log(Level.SEVERE, "authorization failure for user " + authenticatedUser.getLogin());
            throw new AxisFault("Server.Unauthorized", "com.l7tech.remote.ws.security.AuthenticationAxisHandler failed", null, null);
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private boolean authorizeAdminMembership(User adminUser) {
        //return userIsMemberOfGroup(adminUser.getOid(), Group.ADMIN_GROUP_NAME);
        GroupManager gman = identityProviderConfigManager.getInternalIdentityProvider().getGroupManager();
        try {
            if (adminUser == null) return false;
            Set groupHeaders = gman.getGroupHeaders(adminUser);
            for (Iterator i = groupHeaders.iterator(); i.hasNext();) {
                Group grp = (Group)i.next();
                if (grp.getName() != null && grp.getName().equals(Group.ADMIN_GROUP_NAME)) return true;
            }
            return false;
        } catch (FindException fe) {
            logger.log(Level.SEVERE, fe.getMessage(), fe);
            return false;
        }
    }

    private User authenticateBasicToken(String value) throws AxisFault {
        String login = null;
        String clearTextPasswd = null;

        int i = value.indexOf(':');
        if (i == -1) {
            throw new AxisFault("invalid basic credentials " + value);
        } else {
            login = value.substring(0, i);
            clearTextPasswd = value.substring(i + 1);
        }

        LoginCredentials creds = new LoginCredentials(login, clearTextPasswd.toCharArray(), null);

        if (identityProviderConfigManager == null) {
            identityProviderConfigManager = (IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
        }

        try {
            try {
                return identityProviderConfigManager.getInternalIdentityProvider().authenticate(creds);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "authentication failed for " + login, e);
                return null;
            }
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "error closing context", e);
                throw new AxisFault("", e);
            }
        }
    }

    /**
     * returns null if user does not exist
     */
    protected User findUserByLogin(String login) {
        try {
            if (identityProviderConfigManager == null) {
                identityProviderConfigManager = (IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
            }
            User output = identityProviderConfigManager.getInternalIdentityProvider().getUserManager().findByLogin(login);
            PersistenceContext.getCurrent().close();
            return output;
        } catch (FindException e) {
            // not throwing this on purpose, a FindException might just mean that the user does not exist,
            // in which case null is a valid answer
            logger.log(Level.SEVERE, "exception finding user by login", e);
            return null;
        } catch (SQLException e) {
            // not throwing this on purpose, a FindException might just mean that the user does not exist,
            // in which case null is a valid answer
            logger.log(Level.SEVERE, "exception closing context", e);
            return null;
        } catch (ObjectModelException e) {
            // not throwing this on purpose, a FindException might just mean that the user does not exist,
            // in which case null is a valid answer
            logger.log(Level.SEVERE, "exception closing context", e);
            return null;
        }
    }

    private static final String AUTHENTICATED_USER = "Authenticated_com.l7tech.identity.User";
    IdentityProviderConfigManager identityProviderConfigManager = null;
    final Logger logger = Logger.getLogger(getClass().getName());
}
