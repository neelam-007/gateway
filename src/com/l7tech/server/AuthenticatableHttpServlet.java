package com.l7tech.server;

import com.l7tech.common.Feature;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.util.CertUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.ServletUtils;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
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
 */
public abstract class AuthenticatableHttpServlet extends HttpServlet {
    protected final Logger logger = Logger.getLogger(getClass().getName());

    private WebApplicationContext applicationContext;

    protected ServiceManager serviceManager;
    protected ClientCertManager clientCertManager;
    protected IdentityProviderConfigManager providerConfigManager;
    private IdentityProviderFactory identityProviderFactory;
    private LicenseManager licenseManager;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }

        clientCertManager = (ClientCertManager)getBean("clientCertManager");
        providerConfigManager = (IdentityProviderConfigManager)getBean("identityProviderConfigManager");
        identityProviderFactory = (IdentityProviderFactory)getBean("identityProviderFactory");
        licenseManager = (LicenseManager)getBean("licenseManager");
        serviceManager = (ServiceManager)getBean("serviceManager");
    }

    private Object getBean(String name) throws ServletException {
        Object bean = applicationContext.getBean(name);
        if (bean == null) throw new ServletException("Configuration error; could not get " + name);
        return bean;
    }

    /**
     * Look for basic creds in the request and authenticate them against id providers available in this ssg.
     * If credentials are provided but they are invalid, this will throw a BadCredentialsException
     *
     * @return a Map&lt;User, X509Certificate&gt; containing authenticated users, and any cert they might have presented. May be empty, but never null.
     * @throws BadCredentialsException  if authorized credentials were not presented with the request
     * @throws IssuedCertNotPresentedException  if the identity in question is recorded as possessing a client
     *                                          certificate, and the connection came in over SSL, but the
     *                                          client failed to present this client certificate during the
     *                                          SSL handshake.
     * @throws LicenseException   if the currently installed license does not enable use of auxilary servlets.
     */
    protected Map authenticateRequestBasic(HttpServletRequest req)
            throws BadCredentialsException, IssuedCertNotPresentedException, LicenseException
    {

        licenseManager.requireFeature(Feature.AUXILIARY_SERVLETS);

        Map users;
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
            return Collections.EMPTY_MAP;
        }
    }

    private Map getUsers(HttpServletRequest req) throws FindException, IssuedCertNotPresentedException {
        Map users = new HashMap();
        Collection providers;
        providers = providerConfigManager.findAllIdentityProviders();
        LoginCredentials creds = findCredentialsBasic(req);
        if (creds == null) {
            return users;
        }

        if (!req.isSecure() && !isCleartextAllowed()) {
            logger.info("HTTP Basic authentication is not allowed without SSL");
            return users;
        }

        boolean userAuthenticatedButDidNotPresentHisCert = false;

        nextIdentityProvider:
        for (Iterator i = providers.iterator(); i.hasNext();) {
            IdentityProvider provider = (IdentityProvider)i.next();
            try {
                AuthenticationResult authResult = provider.authenticate(creds);
                if (authResult == null) continue nextIdentityProvider;
                User u = authResult.getUser();
                logger.fine("Authentication success for user " + creds.getLogin() +
                        " on identity provider: " + provider.getConfig().getName());

                // if this request comes through SSL, and the authenticated client possess a valid
                // client cert, then we enforce that he USES the client cert as part of the SSL
                // handshake (this is to prevent dictionnary attacks against accounts that possess
                // a valid client cert)

                if (!req.isSecure()) {
                    logger.info("HTTP Basic is being permitted without SSL");
                    users.put(u, null);
                } else {
                    X509Certificate requestCert = ServletUtils.getRequestCert(req);
                    Certificate dbCert = clientCertManager.getUserCert(u);
                    if (dbCert == null) {
                        logger.finest("User " + u.getLogin() + " does not have a cert in database, but authenticated successfully with HTTP Basic over SSL");
                        users.put(u, null);
                    } else {
                        // there is a valid cert. make sure it was presented
                        if (requestCert == null) {
                            logger.info("User " + creds.getLogin() + " has valid basic credentials but is " +
                                        "refused authentication because he did not prove possession of his client cert.");
                            userAuthenticatedButDidNotPresentHisCert = true;
                        }
                        X509Certificate dbCertX509;
                        if (dbCert instanceof X509Certificate) {
                            dbCertX509 = (X509Certificate)dbCert;
                        } else {
                            logger.warning("Client cert in database is not X.509");
                            continue nextIdentityProvider;
                        }
                        if (CertUtils.certsAreEqual(requestCert, dbCertX509)) {
                            logger.finest("Valid client cert presented as part of request.");
                            users.put(u, requestCert);
                        } else {
                            logger.warning("the authenticated user has a valid cert but he presented a different" +
                              "cert to the servlet");
                        }
                    }
                }
            } catch (Exception e) {
                logger.fine("Authentication failed for user " + creds.getLogin() +
                  " on identity provider: " + provider.getConfig().getName());
            }
        }
        if (users.isEmpty() && userAuthenticatedButDidNotPresentHisCert) {
            String msg = "Basic credentials are valid but the client did not present " +
              "his client cert as part of the ssl handshake";
            logger.warning(msg);
            throw new IssuedCertNotPresentedException(msg);
        }
        return users;
    }

    /**
     * Override and return true if your implementation allows HTTP Basic without SSL
     */
    protected boolean isCleartextAllowed() {
        return false;
    }

    /**
     * Authenticate the credential in the context of the service. This method supports
     * additional user resolution for custom assertions that provide custom identity
     * authentication.
     * <p/>
     * First do the the standard user check {@link AuthenticatableHttpServlet#authenticateRequestBasic(javax.servlet.http.HttpServletRequest)},
     * and if non empty list has been returned stop there. If the list of users is empty, retrieve
     * custom assertions and check if there is a custom assertion in the service policy. If true,
     * and the assertion is registered as <code>Category</code> {@link com.l7tech.policy.assertion.ext.Category#ACCESS_CONTROL}
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
    protected Map authenticateRequestBasic(HttpServletRequest req, PublishedService service)
      throws IOException, BadCredentialsException, IssuedCertNotPresentedException {
        Map users = new HashMap();
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
                    if (Category.ACCESS_CONTROL.equals(ch.getCategory())) { // bingo
                        UserBean user = new UserBean();
                        user.setProviderId(Long.MAX_VALUE);
                        user.setLogin(creds.getLogin());
                        user.setPassword(new String(creds.getCredentials()));
                        users.put(user, null); // No certs for custom assertions
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

        ServerHttpBasic httpBasic = new ServerHttpBasic(new HttpBasic(), applicationContext);
        LoginCredentials creds;
        try {
            creds = httpBasic.findCredentials(authorizationHeader);
            if (creds == null) {
                logger.warning("No credentials found.");
            }
            return creds;
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
        Assertion rootassertion = WspReader.parsePermissively(policy.getPolicyXml());

        Iterator it = rootassertion.preorderIterator();
        boolean allIdentitiesAreFederated = true;
        while (it.hasNext()) {
            Assertion a = (Assertion)it.next();
            if (a instanceof CustomAssertionHolder) {
                CustomAssertionHolder ca = (CustomAssertionHolder)a;
                if (Category.ACCESS_CONTROL.equals(ca.getCategory())) {
                    return true;
                }
            } else if (a instanceof IdentityAssertion) {
                IdentityAssertion ia = (IdentityAssertion)a;
                final String msg = "Policy refers to a nonexistent identity provider";
                try {
                    IdentityProvider provider = identityProviderFactory.getProvider(ia.getIdentityProviderOid());
                    if (provider == null) {
                        logger.warning(msg);
                        return false;
                    }
                    if (provider.getConfig().type() != IdentityProviderType.FEDERATED) allIdentitiesAreFederated = false;
                } catch (FindException e) {
                    logger.warning(msg);
                    return false;
                }
            }
        }

        if (allIdentitiesAreFederated) {
            // TODO support federated credentials in PolicyServlet
            logger.info("All IdentityAssertions point to a Federated IDP. Treating as anonymous");
            return true;
        }

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
    private Assertion findCredentialAssertion(Assertion arg) {
        if (arg.isCredentialSource()) {
            return arg;
        }
        if (arg instanceof CompositeAssertion) {
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion child = (Assertion)i.next();
                Assertion res = findCredentialAssertion(child);
                if (res != null) return res;
            }
        }
        return null;
    }


    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected PublishedService resolveService(long oid) {
        try {
            return serviceManager.findByPrimaryKey(oid);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot retrieve service " + oid, e);
            return null;
        }
    }
}
