package com.l7tech.server;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.CustomAssertions;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for servlets that share the capability of authenticating requests against
 * id providers registered in this ssg server.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Sep 15, 2003<br/>
 * $Id$
 */
public abstract class AuthenticatableHttpServlet extends HttpServlet {
    protected ServiceManager serviceManagerInstance = null;
    protected Logger logger = null;

    public void init(ServletConfig config) throws ServletException {
        logger = LogManager.getInstance().getSystemLogger();
        super.init(config);
    }

    /**
     * Look for basic creds in the request and authenticate them against id providers available in this ssg.
     * If credentials are provided but they are invalid, this will throw a BadCredentialsException
     *
     * @return the authenticated user, null if no creds provided
     */
    protected List authenticateRequestBasic(HttpServletRequest req) throws IOException, BadCredentialsException {
        List users = new ArrayList();
        try {
            users = getUsers(req);
            if (users.isEmpty()) {
                String msg = "Creds do not authenticate against any registered id provider.";
                logger.warning(msg);
                throw new BadCredentialsException(msg);
            }
            return users;
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Exception getting id providers.", e);
            return users;
        }
    }

    private List getUsers(HttpServletRequest req) throws FindException {
        List users = new ArrayList();
        IdentityProviderConfigManager configManager = new IdProvConfManagerServer();
        Collection providers;
        providers = configManager.findAllIdentityProviders();
        LoginCredentials creds = findCredentialsBasic(req);
        if (creds == null) {
            return users;
        }
        for (Iterator i = providers.iterator(); i.hasNext();) {
            IdentityProvider provider = (IdentityProvider)i.next();
            try {
                User u = provider.authenticate(creds);
                logger.fine("Authentication success for user " + creds.getLogin() + " on identity provider: " +
                            provider.getConfig().getName());
                users.add(u);
            } catch (AuthenticationException e) {
                logger.fine("Authentication failed for user " + creds.getLogin() +
                            " on identity provider: " + provider.getConfig().getName());
                continue;
            }
        }
        return users;
    }

    /**
     * Authenticate the credential in the context of the service. This method supports
     * additional user resolution for custom assertions that provide custom identity
     * authentication.
     * <p/>
     * First do the the standard user check {@link AuthenticatableHttpServlet#authenticateRequestBasic(javax.servlet.http.HttpServletRequest)},
     * and if non empty list has been returned stop there. If the list of users is empty, retrieve
     * custom assertions and check if there is a custom assertion in the service policy. If true,
     * and the assertion is registered as <code>Category</code> {@link com.l7tech.policy.assertion.ext.Category#IDENTITY}
     * then we let that request through, since the custom assertion is responsible for validating the credentials.
     * If no custom assertion is found throws <code>BadCredentialsException</code>.
     * <p/>
     * Custom identity authentication assertions may not provide user management API (list users etc.) therefore
     * we just do this check and delegate. This is how Netegrity Siteminder works for exmaple.
     *
     * @param req     the servlet request
     * @param service the published service
     * @return the list of users
     * @throws java.io.IOException on io error
     * @throws com.l7tech.identity.BadCredentialsException
     *                             on invalid credentials
     */
    protected List authenticateRequestBasic(HttpServletRequest req, PublishedService service)
      throws IOException, BadCredentialsException {
        List users = new ArrayList();
        try {
            users = getUsers(req);
            if (!users.isEmpty()) {
                return users;
            }
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Error getting users.", e);
            return users; // cannot conitinue here ...
        }

        LoginCredentials creds = findCredentialsBasic(req);
        if (creds == null) {
            return users;
        }
        if (service != null) {
            Iterator it = service.rootAssertion().preorderIterator();
            while (it.hasNext()) {
                Assertion ass = (Assertion)it.next();
                if (ass instanceof CustomAssertionHolder) {
                    CustomAssertionHolder ch = (CustomAssertionHolder)ass;
                    CustomAssertionDescriptor cdesc = CustomAssertions.getDescriptor(ch);
                    if (cdesc == null) {
                        logger.warning("Unable to resolve the custom assertion " + ch);
                        continue;
                    }
                    if (Category.IDENTITY.equals(cdesc.getCategory())) { // bingo
                        UserBean user = new UserBean();
                        user.setProviderId(Long.MAX_VALUE);
                        user.setLogin(creds.getLogin());
                        user.setPassword(new String(creds.getCredentials()));
                        users.add(user);
                        break; // enough
                    }
                }
            }
        }
        if (users.isEmpty()) {
            String msg = "Creds do not authenticate against any registered id provider.";
            logger.warning(msg);
            throw new BadCredentialsException(msg);
        }
        return users;
    }

    /**
     * Find the credentials using http basic method.
     *
     * @param req the servlet request
     * @return the <code>LoginCredentials</code> or null if not found
     */
    protected LoginCredentials findCredentialsBasic(HttpServletRequest req) {
        String authorizationHeader = req.getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.length() < 1) {
            logger.warning("No authorization header found.");
            return null;
        }

        ServerHttpBasic httpBasic = new ServerHttpBasic(new HttpBasic());
        LoginCredentials creds = null;
        try {
            creds = httpBasic.findCredentials(authorizationHeader);
            if (creds == null) {
                logger.warning("No credentials found.");
            }
            return creds;
        } catch (CredentialFinderException e) {
            logger.log(Level.SEVERE, "Exception looking for exception.", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception looking for exception.", e);
        }
        return null;
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


    protected void endTransaction() {
        try {
            PersistenceContext context = PersistenceContext.getCurrent();
            context.commitTransaction();
            context.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "end transaction error", e);
        } catch (TransactionException e) {
            logger.log(Level.WARNING, "end transaction error", e);
        }
    }

    protected synchronized void initialiseServiceManager() throws ClassCastException, RuntimeException {
        serviceManagerInstance = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        if (serviceManagerInstance == null) {
            throw new RuntimeException("Cannot instantiate the ServiceManager");
        }
    }

    protected ServiceManager getServiceManagerAndBeginTransaction()
      throws SQLException, TransactionException {
        if (serviceManagerInstance == null) {
            initialiseServiceManager();
        }
        PersistenceContext.getCurrent().beginTransaction();
        return serviceManagerInstance;
    }

    protected PublishedService resolveService(long oid) {
        try {
            return getServiceManagerAndBeginTransaction().findByPrimaryKey(oid);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot retrieve service " + oid, e);
            return null;
        } finally {
            endTransaction();
        }
    }
}
