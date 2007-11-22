package com.l7tech.server;

import com.l7tech.common.LicenseException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.transport.TransportModule;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
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
 */
public class PasswdServlet extends AuthenticatableHttpServlet {
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_PASSWD;
    }

    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.PASSWD;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!req.isSecure()) {
            logger.warning("Request came over insecure channel (not https). Returning 403.");
            sendBackError(res, HttpServletResponse.SC_FORBIDDEN, "Please come back over https");
            return;
        }
        // get credentials
        final AuthenticationResult[] results;
        try {
            results = authenticateRequestBasic(req);
        } catch (AuthenticationException e) {
            logger.log(Level.WARNING, "Bad credentials, returning 401", e);
            sendBackError(res, HttpServletResponse.SC_UNAUTHORIZED, "Bad credentials");
            return;
        } catch (LicenseException e) {
            logger.log(Level.WARNING, "Service is unlicensed, returning 500", e);
            sendBackError(res, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Gateway auxiliary service not enabled by license");
            return;
        } catch (TransportModule.ListenerException e) {
            logger.log(Level.WARNING, "Service is not enabled on this port, returning 500", e);
            sendBackError(res, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Gateway auxiliary service not enabled on this port");
            return;
        }
        if (results == null || results.length < 1) {
            logger.warning("No valid credentials, returning 401.");
            sendBackError(res, HttpServletResponse.SC_UNAUTHORIZED, "No valid credentials");
            return;
        }
        InternalUser internalUser = null;
        InternalIdentityProvider internalProvider = null;
        for (AuthenticationResult result : results) {
            User u = result.getUser();
            try {
                IdentityProviderFactory ipf = (IdentityProviderFactory)getApplicationContext().getBean("identityProviderFactory");
                IdentityProvider provider = ipf.getProvider(u.getProviderId());

                if (provider.getConfig().getTypeVal() == IdentityProviderType.INTERNAL.toVal()) {
                    internalUser = (InternalUser)u;
                    internalProvider = (InternalIdentityProvider)provider;
                    break;
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "could not complete operation, returning 500", e);
                sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
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
        String newpasswd = req.getHeader(SecureSpanConstants.HttpHeaders.HEADER_NEWPASSWD);
        if (newpasswd == null || newpasswd.length() < 1) {
            logger.warning("The request did not include a new password in header " +
                           SecureSpanConstants.HttpHeaders.HEADER_NEWPASSWD + ", returning 400.");
            sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, "Missing or empty new password.");
            return;
        }
        // unbase 64 it
        try {
            newpasswd = new String(HexUtils.decodeBase64(newpasswd, true), "UTF-8");
        } catch (IOException e) {
            logger.warning("The passed password could not be b64decoded, returning 400.");
            sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, "Password should be b64ed.");
            return;
        }
        // make sure it's different from current one
        InternalUser tempUser;
        try {
            tempUser = new InternalUser(internalUser.getLogin());
            tempUser.setCleartextPassword(newpasswd);
        } catch (IllegalStateException e) {
            logger.log(Level.SEVERE, "could not compare password", e);
            sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        } catch (InvalidPasswordException e) {
            logger.log(Level.WARNING, "new password not valid", e);
            sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        if (tempUser.getHashedPassword().equals(internalUser.getHashedPassword())) {
            logger.warning("New password same as old one, returning 400.");
            sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, "Please provide new password (different from old one)");
            return;
        }
        // DO IT!
        try {
            InternalUser newInternalUser = new InternalUser();
            newInternalUser.copyFrom(internalUser);
            newInternalUser.setVersion(internalUser.getVersion());
            newInternalUser.setCleartextPassword(newpasswd);

            InternalUserManager userManager = internalProvider.getUserManager();
            userManager.update(newInternalUser);
            logger.fine("Password changed for user " + internalUser.getLogin());
            // end transaction
            // return 200
            res.setStatus(HttpServletResponse.SC_OK);
            OutputStream output = res.getOutputStream();
            output.write("Password changed succesfully".getBytes());
            output.close();
        } catch (FindException e) {
            logger.log(Level.WARNING, "could not complete operation, returning 500", e);
            sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (UpdateException e) {
            logger.log(Level.WARNING, "could not complete operation, returning 500", e);
            sendBackError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (InvalidPasswordException e) {
            logger.log(Level.SEVERE, "password was not valid", e);
            sendBackError(res, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private static void sendBackError(HttpServletResponse res, int status, String msg) throws IOException {
        res.setStatus(status);
        res.getOutputStream().print(msg);
        res.getOutputStream().close();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        throw new ServletException("Method not supported; cert revocation must use POST");
    }
}
