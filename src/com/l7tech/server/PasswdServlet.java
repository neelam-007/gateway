package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.common.protocol.SecureSpanConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;
import java.sql.SQLException;

import sun.misc.BASE64Decoder;

/**
 * This servlet lets a client change the password of his internal account.
 * This is only applicable for accounts of the internal id provider.
 * The request to this service must be secured (https) to be accepted. The requests must be authenticated
 * with the existing password and the existing client cert if it exists. As a result of changing the
 * password, any existing client cert is automatically revoked.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 17, 2004<br/>
 * $Id$<br/>
 */
public class PasswdServlet extends AuthenticatableHttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!req.isSecure()) {
            logger.warning("Request came over insecure channel (not https). Returning 403.");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Please come back over https");
            return;
        }
        try { // make sure we close the context on exit through finally statement
            // get credentials
            List users;
            try {
                users = authenticateRequestBasic(req);
            } catch (BadCredentialsException e) {
                logger.log(Level.WARNING, "Bad credentials, returning 401", e);
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bad credentials");
                return;
            }
            if (users.isEmpty()) {
                logger.warning("No valid credentials, returning 401.");
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No valid credentials");
                return;
            }
            User internalUser = null;
            for (Iterator i = users.iterator(); i.hasNext();) {
                User u = (User)i.next();
                if (u.getProviderId() == IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID) {
                    internalUser = u;
                    break;
                }
            }
            if (internalUser == null) {
                logger.warning("Subject of request is not internal user, returning 403.");
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Only users belonging to " +
                                                                "the internal identity provider " +
                                                                "are authorized to change their password");
                return;
            }
            // get the new password
            String str_newpasswd = req.getHeader(SecureSpanConstants.HttpHeaders.HEADER_NEWPASSWD);
            if (str_newpasswd == null || str_newpasswd.length() < 1) {
                logger.warning("The request did not include a new password, returning 400.");
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please provide new password in http header " +
                                                                  SecureSpanConstants.HttpHeaders.HEADER_NEWPASSWD);
                return;
            }
            // unbase 64 it
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                str_newpasswd = new String(decoder.decodeBuffer(str_newpasswd));
            } catch (IOException e) {
                logger.warning("The passed password could not be b64decoded, returning 400.");
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password should be b64ed.");
                return;
            }
            // make sure it's different from current one
            UserBean tmpUser = new UserBean();
            tmpUser.setLogin(internalUser.getLogin());
            try {
                tmpUser.setPassword(str_newpasswd);
            } catch (IllegalStateException e) {
                logger.log(Level.SEVERE, "could not compare password", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
            if (tmpUser.getPassword().equals(internalUser.getPassword())) {
                logger.warning("New password same as old one, returning 400.");
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please provide new password " +
                                                                  "(different from old one)");
                return;
            }
            // DO IT!
            try {
                // start transaction
                PersistenceContext pc = PersistenceContext.getCurrent();
                pc.beginTransaction();
                // save user
                internalUser.getUserBean().setPassword(str_newpasswd);
                IdentityProvider provider = IdentityProviderFactory.getProvider(
                                                IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
                UserManager userManager = provider.getUserManager();
                userManager.update(internalUser);
                logger.fine("Password changed for user " + internalUser.getLogin());
                // end transaction
                pc.commitTransaction();
                // return 200
                res.setStatus(HttpServletResponse.SC_OK);
                OutputStream output = res.getOutputStream();
                output.write("Password changed succesfully".getBytes());
                output.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (TransactionException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (FindException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (UpdateException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (ObjectNotFoundException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Could not get current persistence context to close.", e);
            }
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // GET, POST, who cares  ...
        doGet(req, res);
    }
}
