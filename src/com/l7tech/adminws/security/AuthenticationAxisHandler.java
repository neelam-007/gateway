package com.l7tech.adminws.security;

import org.apache.axis.MessageContext;
import org.apache.axis.AxisFault;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.axis.encoding.Base64;
import org.apache.axis.transport.http.HTTPConstants;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.Locator;

import java.util.logging.Level;
import java.util.Iterator;
import java.sql.SQLException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: June 2, 2003
 *
 * This axis handler class does the following
 * 1. reads a basic auth header from the http request
 * 2. unbase64 it
 * 3. MD5s it
 * 4. compare it to the hash value for this user in the database
 * 5. if authentication succeeds, it checks that user is member of SSGAdmin local group
 *
 * The password has in the database is expected to have the following format:
 * MD5(username::password) hex encoded
 *
 * It must be declared in the server-config.wsdd file in order to be invoked as per below:
 *
 *
 * <transport name="http">
 *   <requestFlow>
 *     <handler type="URLMapper"/>
 *     <handler type="java:com.l7tech.adminws.security.AuthenticationAxisHandler"/>
 *   </requestFlow>
 * </transport>
 */
public class AuthenticationAxisHandler extends org.apache.axis.handlers.BasicHandler {
    public static final long SESSION_MAX_LENGTH = 120000; // two minutes

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

            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "cannot retrieve servlet request from axis context");
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
        if (tmp != null ) tmp = tmp.trim();

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
        if (authenticatedUser == null) throw new AxisFault("Server.Unauthorized", "com.l7tech.adminws.security.AuthenticationAxisHandler failed", null, null );

        // NOW, PROCEED TO AUTHORIZATION
        if (authorizeAdminMembership(authenticatedUser)) {
            // CREATE SESSION
            HttpSession session = servletrequest.getSession(true);
            session.setAttribute(AUTHENTICATED_USER, authenticatedUser);
            session.setMaxInactiveInterval(30); // 30 seconds times you out
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "created admin session for user " + authenticatedUser.getLogin());
        }
        else {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "authorization failure for user " + authenticatedUser.getLogin());
            throw new AxisFault("Server.Unauthorized", "com.l7tech.adminws.security.AuthenticationAxisHandler failed", null, null );
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private boolean authorizeAdminMembership(User adminUser) {
        //return userIsMemberOfGroup(adminUser.getOid(), Group.ADMIN_GROUP_NAME);
        if (adminUser == null || adminUser.getGroups() == null) return false;
        for (Iterator i = adminUser.getGroups().iterator(); i.hasNext();) {
            Group grp = (Group)i.next();
            if (grp.getName() != null && grp.getName().equals(Group.ADMIN_GROUP_NAME)) return true;
        }
        return false;
    }

    private User authenticateBasicToken(String value) throws AxisFault {
        String login = null;
        int i = value.indexOf( ':' );
        if (i == -1) login = value;
        else login = value.substring( 0, i);

        // MD5 IT
        java.security.MessageDigest md5Helper = null;
        try {
            md5Helper = java.security.MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AxisFault(e.getMessage());
        }
        byte[] digest = md5Helper.digest(value.getBytes());
        String md5edDecodedAuthValue = encodeDigest(digest);

        // COMPARE TO THE VALUE IN INTERNAL DB
        com.l7tech.identity.User internalUser = findUserByLogin(login);
        if (internalUser == null) {
            throw new AxisFault("User " + login + " not registered in internal id provider");
        }
        if (internalUser.getPassword() == null) {
            throw new AxisFault("User " + login + "does not have a password");
        }
        if (internalUser.getPassword().equals(md5edDecodedAuthValue)) {
            // AUTHENTICATION SUCCESS, move on to authorization
            return internalUser;
        } else {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "authentication failure for user " + login + " with credentials " + md5edDecodedAuthValue);
        }
        return null;
    }

    /**
     * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
     *
     * @param binaryData Array containing the digest
     * @return Encoded MD5, or null if encoding failed
     */
    private static String encodeDigest(byte[] binaryData) {
        if (binaryData == null) return "";

        char[] hexadecimal ={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (binaryData.length != 16) return "";

        char[] buffer = new char[32];

        for (int i = 0; i < 16; i++) {
            int low = (int) (binaryData[i] & 0x0f);
            int high = (int) ((binaryData[i] & 0xf0) >> 4);
            buffer[i*2] = hexadecimal[high];
            buffer[i*2 + 1] = hexadecimal[low];
        }
        return new String(buffer);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception finding user by login", e);
            return null;
        } catch (SQLException e) {
            // not throwing this on purpose, a FindException might just mean that the user does not exist,
            // in which case null is a valid answer
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception closing context", e);
            return null;
        } catch (ObjectModelException e) {
            // not throwing this on purpose, a FindException might just mean that the user does not exist,
            // in which case null is a valid answer
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception closing context", e);
            return null;
        }
    }

    private static final String AUTHENTICATED_USER = "Authenticated_com.l7tech.identity.User";
    IdentityProviderConfigManager identityProviderConfigManager = null;
}
