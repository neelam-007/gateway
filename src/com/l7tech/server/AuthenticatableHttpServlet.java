package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.logging.LogManager;
import com.l7tech.service.PublishedService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
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
     * If credentials are provided but they are invalid, this will throw a BadCredentialsException
     * @return the authenticated user, null if no creds provided
     */
    protected List authenticateRequestBasic(HttpServletRequest req) throws IOException, BadCredentialsException {
        // get the credentials
        List users = new ArrayList();

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
                    logger.fine("Authentication success for user " + creds.getUser().getLogin() + " on identity provider: " + provider.getConfig().getName());
                    users.add( creds.getUser() );
                } catch (AuthenticationException e) {
                    logger.fine("Authentication failed for user " + creds.getUser().getLogin() + " on identity provider: " + provider.getConfig().getName());
                    continue;
                }
            }

            if ( users.isEmpty() ) {
                String msg = "Creds do not authenticate against any registered id provider.";
                logger.warning(msg);
                throw new BadCredentialsException(msg);
            }

            return users;
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Exception getting id providers.", e);
            return null;
        } finally {
            try {
                rollbackTransaction();
            } catch ( SQLException se ) {
                logger.log(Level.WARNING, null, se);
            } catch ( TransactionException te ) {
                logger.log(Level.WARNING, null, te);
            }
        }
    }

    /**
     * Decides whether a policy should be downloadable without providing credentials. This will return true if the
     * service described by this policy could be consumed anonymouly.
     */
    protected boolean policyAllowAnonymous(PublishedService policy) throws IOException {
        // logic: a policy allows anonymous if and only if it does not contains any CredentialSourceAssertion
        // com.l7tech.policy.assertion.credential.CredentialSourceAssertion
        Assertion rootassertion = WspReader.parse(policy.getPolicyXml());
        if (findCredentialAssertion(rootassertion) != null) {
            logger.info("Policy does not allow anonymous requests.");
            return false;
        }
        logger.info("Policy does allow anonymous requests.");
        return true;
    }

    /**
     * Look for an assertion extending CredentialSourceAssertion in the assertion passed
     * and all it's decendents.
     * Returns null if not there.
     * (recursive method)
     */
    private CredentialSourceAssertion findCredentialAssertion(Assertion arg) {
        if (arg instanceof CredentialSourceAssertion) {
            return (CredentialSourceAssertion)arg;
        }
        if (arg instanceof CompositeAssertion) {
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion child = (Assertion)i.next();
                CredentialSourceAssertion res = findCredentialAssertion(child);
                if (res != null) return res;
            }
        }
        return null;
    }

    private void commitTransaction() throws java.sql.SQLException, TransactionException {
        PersistenceContext context = PersistenceContext.getCurrent();
        context.commitTransaction();
        context.close();
    }

    private void rollbackTransaction() throws java.sql.SQLException, TransactionException {
        PersistenceContext context = PersistenceContext.getCurrent();
        context.rollbackTransaction();
        context.close();
    }

    protected Logger logger = null;
}
