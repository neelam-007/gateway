package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.logging.LogManager;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;
import java.sql.SQLException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Sep 15, 2003
 * Time: 4:42:59 PM
 * $Id$
 *
 * Base class for servlets that share the capability of authenticating requests against
 * id providers registered in this ssg server.
 */
public abstract class AuthenticatableHttpServlet extends HttpServlet {

    public void init( ServletConfig config ) throws ServletException {
        logger = LogManager.getInstance().getSystemLogger();
        super.init( config );
    }

    /**
     * Look for basic creds in the request and authenticate them against id providers available in this ssg.
     * @return the authenticated user, null if authentication failed or no creds provided
     */
    protected User authenticateRequestBasic(HttpServletRequest req) throws IOException {
        // get the credentials
        String authorizationHeader = req.getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.length() < 1) {
            logger.warning("No authorization header found.");
            return null;
        }

        ServerHttpBasic httpBasic = new ServerHttpBasic( new HttpBasic() );
        PrincipalCredentials creds = null;
        try {
            creds = httpBasic.findCredentials(authorizationHeader);
        } catch (CredentialFinderException e) {
            logger.log(Level.SEVERE, "Exception looking for exception.", e);
            return null;
        }
        if (creds == null) {
            logger.warning("No credentials found.");
            return null;
        }
        // we have credentials, attempt to authenticate them
        IdentityProviderConfigManager configManager = new IdProvConfManagerServer();
        Collection providers = null;
        try {
            providers = configManager.findAllIdentityProviders();
            for (Iterator i = providers.iterator(); i.hasNext();) {
                IdentityProvider provider = (IdentityProvider) i.next();
                try {
                    provider.authenticate(creds);
                } catch (AuthenticationException e) {
                    logger.info("Authentication successful for user " + creds.getUser().getLogin() + " on identity provider: " + provider.getConfig().getName());
                    continue;
                }
                return creds.getUser();
            }
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Exception getting id providers.", e);
            return null;
        } finally {
            try {
                endTransaction();
            } catch ( SQLException se ) {
                logger.log(Level.WARNING, null, se);
            } catch ( TransactionException te ) {
                logger.log(Level.WARNING, null, te);
            }
        }
        logger.warning("Creds do not authenticate against any registered id provider.");
        return null;
    }

    private void endTransaction() throws java.sql.SQLException, TransactionException {
        PersistenceContext context = PersistenceContext.getCurrent();
        context.commitTransaction();
        context.close();
    }

    protected Logger logger = null;
}
