package com.l7tech.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdProvConfManagerServer;
import com.l7tech.server.identity.IdentityProviderFactory;
import sun.misc.BASE64Decoder;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

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
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!req.isSecure()) {
            logger.warning("Request came over insecure channel (not https). Returning 403.");
            sendBackError(res, HttpServletResponse.SC_FORBIDDEN, "Please come back over https");
            return;
        }
        try { // make sure we close the context on exit through finally statement
            // get credentials
            List users;
            try {
                users = authenticateRequestBasic(req);
            } catch (AuthenticationException e) {
                logger.log(Level.WARNING, "Bad credentials, returning 401", e);
                sendBackError(res, HttpServletResponse.SC_UNAUTHORIZED, "Bad credentials");
                return;
            }
            if (users.isEmpty()) {
                logger.warning("No valid credentials, returning 401.");
                sendBackError(res, HttpServletResponse.SC_UNAUTHORIZED, "No valid credentials");
                return;
            }
            InternalUser internalUser = null;
            for (Iterator i = users.iterator(); i.hasNext();) {
                User u = (User)i.next();
                if (u.getProviderId() == IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID) {
                    internalUser = (InternalUser)u;
                    break;
                }
            }
            if (internalUser == null) {
                logger.warning("Subject of request is not internal user, returning 403.");
                sendBackError(res, HttpServletResponse.SC_FORBIDDEN, "Only users belonging to " +
                                                                     "the internal identity provider " +
                                                                     "are authorized to change their password");
                return;
            }
            // get the new password
            String str_newpasswd = req.getHeader(SecureSpanConstants.HttpHeaders.HEADER_NEWPASSWD);
            if (str_newpasswd == null || str_newpasswd.length() < 1) {
                logger.warning("The request did not include a new password, returning 400.");
                sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, "Please provide new password in http header " +
                                                                  SecureSpanConstants.HttpHeaders.HEADER_NEWPASSWD);
                return;
            }
            // unbase 64 it
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                str_newpasswd = new String(decoder.decodeBuffer(str_newpasswd));
            } catch (IOException e) {
                logger.warning("The passed password could not be b64decoded, returning 400.");
                sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, "Password should be b64ed.");
                return;
            }
            // make sure it's different from current one
            UserBean tmpUser = new UserBean();
            tmpUser.setLogin(internalUser.getLogin());
            try {
                tmpUser.setPassword(str_newpasswd);
            } catch (IllegalStateException e) {
                logger.log(Level.SEVERE, "could not compare password", e);
                sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
            if (tmpUser.getPassword().equals(internalUser.getPassword())) {
                logger.warning("New password same as old one, returning 400.");
                sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, "Please provide new password (different from old one)");
                return;
            }
            // DO IT!
            try {
                // start transaction
                PersistenceContext pc = PersistenceContext.getCurrent();
                pc.beginTransaction();
                // save user
                InternalUser newInternalUser = new InternalUser();
                newInternalUser.copyFrom(internalUser);
                newInternalUser.setVersion(internalUser.getVersion());
                newInternalUser.setPassword(str_newpasswd);
                IdentityProviderFactory ipf = (IdentityProviderFactory)getApplicationContext().getBean("identityProviderFactory");
                IdentityProvider provider = ipf.getProvider(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
                UserManager userManager = provider.getUserManager();
                userManager.update(newInternalUser);
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
                sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (TransactionException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (FindException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (UpdateException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (ObjectNotFoundException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (InvalidPasswordException e) {
                logger.log(Level.SEVERE, "password was not valid", e);
                sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Could not get current persistence context to close.", e);
            }
        }
    }

    protected void sendBackError(HttpServletResponse res, int status, String msg) throws IOException {
        res.setStatus(status);
        res.getOutputStream().print(msg);
        res.getOutputStream().close();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        throw new ServletException("Method not supported; cert revocation must use POST");
    }
}
