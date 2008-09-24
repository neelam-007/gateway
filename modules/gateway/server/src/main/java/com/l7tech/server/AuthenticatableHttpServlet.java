package com.l7tech.server;

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
import com.l7tech.policy.Policy;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.util.ServletUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.common.io.CertUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
    protected WspReader wspReader;
    protected IdentityProviderFactory identityProviderFactory;
    private LicenseManager licenseManager;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }

        clientCertManager = (ClientCertManager)getBean("clientCertManager");
        identityProviderFactory = (IdentityProviderFactory)getBean("identityProviderFactory");
        licenseManager = (LicenseManager)getBean("licenseManager");
        serviceManager = (ServiceManager)getBean("serviceManager");
        wspReader = (WspReader)getBean("wspReader");
    }

    private Object getBean(String name) throws ServletException {
        Object bean = applicationContext.getBean(name);
        if (bean == null) throw new ServletException("Configuration error; could not get " + name);
        return bean;
    }

    /**
     * @return the license Feature that must be required for an authenticated request to succeed.
     *         For example, {@link com.l7tech.server.GatewayFeatureSets#SERVICE_PASSWD}.
     */
    protected abstract String getFeature();

    /**
     * @return the endpoint name that must be enabled on the SsgConnector that received the
     *         request in order for the request to be allowed, or null if this endpoint should accept
     *         requests regardless of what endpoints are enabled on the SsgConnector that received the request.
     *         For example, {@link SsgConnector.Endpoint#PASSWD}.
     */
    protected abstract SsgConnector.Endpoint getRequiredEndpoint();

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            requireEndpoint(req);
        } catch (TransportModule.ListenerException e) {
            resp.sendError(404, "Service unavailable on this port");
            return;
        }
        super.service(req, resp);
    }

    /**
     * Look for basic creds in the request and authenticate them against id providers available in this ssg.
     * If credentials are provided but they are invalid, this will throw a BadCredentialsException
     *
     * @param req the request.  required
     * @return a Map&lt;User, X509Certificate&gt; containing authenticated users, and their cert from the database if any.
     *          May be empty, but never null.
     * @throws BadCredentialsException  if credentials were not presented with the request but could not be authenticated or authorized
     * @throws MissingCredentialsException  if credentials were not presented with the request
     * @throws IssuedCertNotPresentedException  if the identity in question is recorded as possessing a client
     *                                          certificate, and the connection came in over SSL, but the
     *                                          client failed to present this client certificate during the
     *                                          SSL handshake.
     * @throws LicenseException   if the currently installed license does not enable use of the feature set
                                  whose name is returned by {@link #getFeature}
     * @throws com.l7tech.server.transport.TransportModule.ListenerException if this request could not be
     *                            verified as having arrived over a connector that is configured to allow
     *                            access to the {@link SsgConnector.Endpoint} returned by {@link #getRequiredEndpoint}.
     */
    protected AuthenticationResult[] authenticateRequestBasic(HttpServletRequest req)
            throws BadCredentialsException, MissingCredentialsException, IssuedCertNotPresentedException, LicenseException, TransportModule.ListenerException {

        licenseManager.requireFeature(getFeature());

        try {
            AhsAuthResult ahsResult = authenticateRequestAgainstAllIdProviders(req);
            AuthenticationResult[] results = ahsResult.getAuthResults();
            if (results == null || results.length < 1) {
                String msg = "Creds do not authenticate against any registered id provider.";
                logger.warning(msg);
                if (ahsResult.isSawCredentials())
                    throw new BadCredentialsException(msg);
                else
                    throw new MissingCredentialsException();
            }
            return results;
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Exception getting id providers.", e);
            return new AuthenticationResult[0];
        }
    }

    protected static class AhsAuthResult {
        private final boolean sawCredentials;
        private final AuthenticationResult[] authResults;

        public AhsAuthResult(boolean sawCredentials, AuthenticationResult[] authResults) {
            if (authResults == null) throw new IllegalArgumentException();
            this.sawCredentials = sawCredentials;
            this.authResults = authResults;
        }

        /** @return true if credentials were found in the request; false if credentials were missing. */
        public boolean isSawCredentials() {
            return sawCredentials;
        }

        /** @return an array of authentication results.  Never null. If empty, no identies were authenticated. */
        public AuthenticationResult[] getAuthResults() {
            return authResults;
        }
    }

    /**
     * @return a list of auth results.
     * @noinspection UnnecessaryLabelOnContinueStatement
     */
    private AhsAuthResult authenticateRequestAgainstAllIdProviders(HttpServletRequest req) throws FindException, IssuedCertNotPresentedException {
        Collection<AuthenticationResult> authResults = new ArrayList<AuthenticationResult>();
        Collection<IdentityProvider> providers = identityProviderFactory.findAllIdentityProviders();
        LoginCredentials creds = findCredentialsBasic(req);
        boolean sawCreds = creds != null;
        if (creds == null) {
            return new AhsAuthResult(sawCreds, new AuthenticationResult[0]);
        }

        if (!req.isSecure() && !isCleartextAllowed()) {
            logger.info("HTTP Basic authentication is not allowed without SSL");
            return new AhsAuthResult(sawCreds, new AuthenticationResult[0]);
        }

        boolean userAuthenticatedButDidNotPresentHisCert = false;

        nextIdentityProvider:
        for (IdentityProvider provider : providers) {
            try {
                AuthenticationResult authResult = ((AuthenticatingIdentityProvider)provider).authenticate(creds);
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
                    authResult.setAuthenticatedCert(null); // avoid implying that cert was authenticated
                    authResults.add(authResult);
                } else {
                    X509Certificate requestCert = ServletUtils.getRequestCert(req);
                    Certificate dbCert = clientCertManager.getUserCert(u);
                    if (dbCert == null) {
                        logger.finest("User " + u.getLogin() + " does not have a cert in database, but authenticated successfully with HTTP Basic over SSL");
                        authResult.setAuthenticatedCert(null);
                        authResults.add(authResult);
                    } else {
                        // there is a valid cert. make sure it was presented
                        if (requestCert == null) {
                            logger.info("User " + creds.getLogin() + " has valid basic credentials but is " +
                                    "refused authentication because he did not prove possession of his client cert.");
                            userAuthenticatedButDidNotPresentHisCert = true;
                        }
                        X509Certificate dbCertX509;
                        if (dbCert instanceof X509Certificate) {
                            dbCertX509 = (X509Certificate) dbCert;
                        } else {
                            logger.warning("Client cert in database is not X.509");
                            continue nextIdentityProvider;
                        }
                        if ( CertUtils.certsAreEqual(requestCert, dbCertX509)) {
                            logger.finest("Valid client cert presented as part of request.");
                            authResult = new AuthenticationResult(authResult.getUser(),
                                    dbCertX509,
                                    clientCertManager.isCertPossiblyStale(dbCertX509));
                            authResults.add(authResult);
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
        
        if (authResults.isEmpty() && userAuthenticatedButDidNotPresentHisCert) {
            String msg = "Basic credentials are valid but the client did not present " +
              "his client cert as part of the ssl handshake";
            logger.warning(msg);
            throw new IssuedCertNotPresentedException(msg);
        }
        return new AhsAuthResult(sawCreds, authResults.toArray(new AuthenticationResult[authResults.size()]));
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
     * @return the list of successful {@link AuthenticationResult}s.
     * @throws java.io.IOException on io error
     * @throws com.l7tech.identity.BadCredentialsException
     *                             on invalid credentials
     * @throws MissingCredentialsException if the request did not include any credentials
     * @throws com.l7tech.identity.IssuedCertNotPresentedException
     *                            if this user is known to have a client cert, and had the opportunity to present it
     *                            in an SSL handshake, but failed to do so
     * @throws LicenseException   if the currently installed license does not enable use of auxilary servlets.
     * @throws com.l7tech.server.transport.TransportModule.ListenerException if this request could not be
     *                            verified as having arrived over a connector that is configured to allow
     *                            access to the {@link SsgConnector.Endpoint} returned by {@link #getRequiredEndpoint}.
     */
    protected AuthenticationResult[] authenticateRequestBasic(HttpServletRequest req, PublishedService service)
            throws IOException, BadCredentialsException, MissingCredentialsException, IssuedCertNotPresentedException, LicenseException, TransportModule.ListenerException {
        licenseManager.requireFeature(getFeature());
        requireEndpoint(req);

        // Try to authenticate against identity providers
        final boolean sawCreds;
        try {
            AhsAuthResult ahsResult = authenticateRequestAgainstAllIdProviders(req);
            sawCreds = ahsResult.isSawCredentials();
            AuthenticationResult[] results = ahsResult.getAuthResults();
            if (results != null && results.length > 0) {
                return results;
            }
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Error getting users.", e);
            return new AuthenticationResult[0]; // cannot conitinue here ...
        }

        // Try to authenticate custom assertions (through TAM etc)
        LoginCredentials creds = findCredentialsBasic(req);
        if (creds == null) {
            return new AuthenticationResult[0];
        }

        if (service != null) {
            Iterator it = service.getPolicy().getAssertion().preorderIterator();
            while (it.hasNext()) {
                Assertion ass = (Assertion)it.next();
                if (ass instanceof CustomAssertionHolder) {
                    CustomAssertionHolder ch = (CustomAssertionHolder)ass;
                    if (Category.ACCESS_CONTROL.equals(ch.getCategory())) { // bingo
                        UserBean user = new UserBean();
                        user.setProviderId(Long.MAX_VALUE);
                        user.setLogin(creds.getLogin());
                        user.setCleartextPassword(new String(creds.getCredentials()));
                        return new AuthenticationResult[] { new AuthenticationResult(user) };
                    }
                }
            }
        }

        String msg = "Creds do not authenticate against any registered id provider or custom assertion.";
        logger.warning(msg);

        if (sawCreds)
            throw new BadCredentialsException(msg);
        else
            throw new MissingCredentialsException();
    }

    private void requireEndpoint(HttpServletRequest req) throws TransportModule.ListenerException {
        final SsgConnector.Endpoint endpoint = getRequiredEndpoint();
        if (endpoint != null)
            HttpTransportModule.requireEndpoint(req, endpoint);
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
     * Get the name for the identity provider.
     *
     * @return The name or "Unknown" if not found or an error occurs.
     */
    protected String getIdentityProviderName( final long oid ) {
        String name = "Unknown";

        try {
            IdentityProvider provider = identityProviderFactory.getProvider(oid);
            if (provider != null) {
                name = provider.getConfig().getName();
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error finding identity provider.", e);
        }

        return name;
    }

    /**
     * Decides whether a policy should be downloadable without providing credentials. This will return true if the
     * service described by this policy could be consumed anonymouly.
     */
    protected boolean policyAllowAnonymous(Policy policy) throws IOException {
        // logic: a policy allows anonymous if and only if it does not contains any CredentialSourceAssertion
        // com.l7tech.policy.assertion.credential.CredentialSourceAssertion
        Assertion rootassertion = wspReader.parsePermissively(policy.getXml());

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
            //noinspection WhileLoopReplaceableByForEach
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
