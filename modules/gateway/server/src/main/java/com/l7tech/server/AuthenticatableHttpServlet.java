package com.l7tech.server;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.IncludeAssertionDereferenceTranslator;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.token.http.HttpClientCertToken;
import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.util.ServletUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.Charsets;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions.Unary;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.grepFirst;

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
    private static final boolean checkCertificatePresent = ConfigFactory.getBooleanProperty("com.l7tech.server.services.checkCertificatePresent", true);

    private WebApplicationContext applicationContext;

    protected Config config;
    protected PolicyManager policyManager;
    protected ServiceCache serviceCache;
    protected ClientCertManager clientCertManager;
    protected WspReader wspReader;
    protected IdentityProviderFactory identityProviderFactory;
    private LicenseManager licenseManager;
    protected PasswordHasher passwordHasher;
    private AdminSessionManager adminSessionManager;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }

        config = getBean("serverConfig", Config.class);
        clientCertManager = getBean("clientCertManager", ClientCertManager.class);
        identityProviderFactory = getBean("identityProviderFactory", IdentityProviderFactory.class);
        licenseManager = getBean("licenseManager",LicenseManager.class);
        serviceCache = getBean("serviceCache", ServiceCache.class);
        policyManager = getBean("policyManager", PolicyManager.class);
        wspReader = getBean("wspReader", WspReader.class);
        passwordHasher = getBean("passwordHasher", PasswordHasher.class);
        adminSessionManager = getBean("adminSessionManager", AdminSessionManager.class);
    }

    protected Object getBean(String name) throws ServletException {
        return getBean(name, null);
    }

    @SuppressWarnings({"unchecked"})
    protected <T> T getBean(String name, Class<T> clazz) throws ServletException {
        T bean = applicationContext.getBean(name, clazz);
        if (bean == null)
            throw new ServletException("Configuration error; could not get " + name);
        if (clazz != null && !clazz.isAssignableFrom(bean.getClass()))
            throw new ServletException("Configuration error; bean \'" + name + "\' was unexpected type " + bean.getClass());
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

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            requireEndpoint(req);
        } catch (ListenerException e) {
            resp.sendError(502, "Service unavailable on this port");
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
     * @throws com.l7tech.server.transport.ListenerException if this request could not be
     *                            verified as having arrived over a connector that is configured to allow
     *                            access to the {@link SsgConnector.Endpoint} returned by {@link #getRequiredEndpoint}.
     */
    protected AuthenticationResult[] authenticateRequestBasic(HttpServletRequest req)
            throws BadCredentialsException, MissingCredentialsException, IssuedCertNotPresentedException, LicenseException, ListenerException {

        return authenticateRequestBasic(req, false);
    }

    /**
     *
     * @param requiresAdminUser If true, then an admin user is expected. When true authentication is delegated to the
     * AdminSessionManager.
     */
    protected AuthenticationResult[] authenticateRequestBasic(HttpServletRequest req, boolean requiresAdminUser)
            throws BadCredentialsException, MissingCredentialsException, IssuedCertNotPresentedException, LicenseException, ListenerException {
        licenseManager.requireFeature(getFeature());

        try {
            AhsAuthResult ahsResult = authenticateRequestAgainstAllIdProviders(req, requiresAdminUser);
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

    /**
     * Send a response with an HTTP Basic challenge (401).
     *
     * <p>The response is written by calling this method.</p>
     *
     * @param response The servlet response to use.
     * @throws IOException If an error occurs writing the response.
     */
    protected void doHttpBasicChallenge( final HttpServletResponse response ) throws IOException {
        response.setStatus(HttpConstants.STATUS_UNAUTHORIZED);
        response.setHeader(HttpConstants.HEADER_WWW_AUTHENTICATE, "Basic realm=\"" + ServerHttpBasic.REALM + "\"");
        response.setContentType("text/plain; charset=utf-8");
        response.getWriter().println("Authentication Required.");
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
    private AhsAuthResult authenticateRequestAgainstAllIdProviders(final HttpServletRequest req,
                                                                   final boolean requiresAdminUser)
            throws FindException, IssuedCertNotPresentedException {
        Collection<AuthenticationResult> authResults = new ArrayList<AuthenticationResult>();
        Collection<IdentityProvider> providers = identityProviderFactory.findAllIdentityProviders();
        final LoginCredentials creds = findCredentialsBasic(req);
        final boolean sawCreds = creds != null;
        if (creds == null) {
            return new AhsAuthResult(sawCreds, new AuthenticationResult[0]);
        }

        if (!req.isSecure() && !isCleartextAllowed()) {
            logger.info("HTTP Basic authentication is not allowed without SSL");
            return new AhsAuthResult(sawCreds, new AuthenticationResult[0]);
        }

        boolean userAuthenticatedButDidNotPresentHisCert = false;

        for (IdentityProvider provider : providers) {
            try {
                final AuthenticationResult authResult;
                if(requiresAdminUser){
                    //authenticate with basic creds only. Cert will be checked later in method if it is required and is present.
                    authResult = adminSessionManager.authenticate(creds, false);
                } else {
                    authResult = ((AuthenticatingIdentityProvider)provider).authenticate(creds);
                }

                if (authResult == null && !requiresAdminUser) {
                    continue;//try the next identity provider
                } else if (authResult == null){
                    //authenticating using the admin session manager tries all applicable identity providers.
                    break;
                }

                final User u = authResult.getUser();
                provider = getProviderForUser( authResult.getUser(), providers );
                logger.fine("Authentication success for user " + creds.getLogin() +
                        " on identity provider: " + provider.getConfig().getName());

                // if this request comes through SSL, and the authenticated client possess a valid
                // client cert, then we enforce that he USES the client cert as part of the SSL
                // handshake (this is to prevent dictionary attacks against accounts that possess
                // a valid client cert)

                if (!req.isSecure()) {
                    logger.info("HTTP Basic is being permitted without SSL");
                    authResults.add(authResult);
                } else if ( !checkCertificatePresent ) {
                    logger.finest("Certificate check is disabled.");
                    authResults.add(authResult);
                }else {
                    X509Certificate requestCert = ServletUtils.getRequestCert(req);
                    Certificate dbCert = isLdapCerts(provider) ?
                            ((LdapUser) u).getCertificate() :
                            clientCertManager.getUserCert(u);
                    if (dbCert == null) {
                        logger.finest("User " + u.getLogin() + " does not have a cert in database, but authenticated successfully with HTTP Basic over SSL");
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
                            continue;
                        }
                        if ( CertUtils.certsAreEqual(requestCert, dbCertX509)) {
                            logger.finest("Valid client cert presented as part of request.");
                            authResults.add( new AuthenticationResult(
                                    authResult.getUser(),
                                    new HttpClientCertToken(dbCertX509),
                                    dbCertX509,
                                    clientCertManager.isCertPossiblyStale(dbCertX509)) );
                        } else {
                            logger.warning("the authenticated user has a valid cert but he presented a different" +
                                    "cert to the servlet");
                        }
                    }
                }
            } catch (FailAttemptsExceededException faee) {
                logger.warning("Max. number of failed attempts has been reached for user '" + creds.getLogin() + "'");
            } catch (Exception e) {
                logger.fine("Authentication failed for user " + creds.getLogin() +
                        " on identity provider: " + provider.getConfig().getName());
            }
            if(requiresAdminUser){
                //don't loop for admin users, as admin session manager has already searched all applicable providers.
                break;
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

    private IdentityProvider getProviderForUser( final User user, final Collection<IdentityProvider> providers ) throws AuthenticationException {
        IdentityProvider provider = null;

        if ( user != null ) {
            provider = grepFirst( providers, providerOidPredicate( user.getProviderId() ) );
        }

        if ( provider == null ) {
            throw new AuthenticationException("Provider not found for user");
        }

        return provider;
    }

    private Unary<Boolean, IdentityProvider> providerOidPredicate( final long providerOid ) {
        return new Unary<Boolean,IdentityProvider>(){
            @Override
            public Boolean call( final IdentityProvider identityProvider ) {
                return providerOid == identityProvider.getConfig().getOid();
            }
        };
    }

    private boolean isLdapCerts( final IdentityProvider provider ) {
        return provider.getConfig() instanceof LdapIdentityProviderConfig &&
               ((LdapIdentityProviderConfig)provider.getConfig()).getUserCertificateUseType() != LdapIdentityProviderConfig.UserCertificateUseType.NONE ;     
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
     * and the assertion is either, registered as {@link com.l7tech.policy.assertion.ext.Category#ACCESS_CONTROL} <code>Category</code> or,
     * implements the {@link com.l7tech.policy.assertion.ext.CustomCredentialSource CustomCredentialSource} interface and returns <code>true</code> for
     * {@link com.l7tech.policy.assertion.ext.CustomCredentialSource#isCredentialSource() CustomCredentialSource.isCredentialSource()} method,
     * then we let that request through, since the custom assertion is responsible for validating the credentials.
     * If no custom assertion is found throws <code>BadCredentialsException</code>.
     * <p/>
     * Custom identity authentication assertions may not provide user management API (list users etc.) therefore
     * we just do this check and delegate. This is how Netegrity Siteminder works for example.
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
     * @throws com.l7tech.server.transport.ListenerException if this request could not be
     *                            verified as having arrived over a connector that is configured to allow
     *                            access to the {@link SsgConnector.Endpoint} returned by {@link #getRequiredEndpoint}.
     */
    protected AuthenticationResult[] authenticateRequestBasic(HttpServletRequest req, PublishedService service)
            throws IOException, BadCredentialsException, MissingCredentialsException, IssuedCertNotPresentedException, LicenseException, ListenerException {
        licenseManager.requireFeature(getFeature());
        requireEndpoint(req);

        // Try to authenticate against identity providers
        final boolean sawCreds;
        try {
            AhsAuthResult ahsResult = authenticateRequestAgainstAllIdProviders(req, false);
            sawCreds = ahsResult.isSawCredentials();
            AuthenticationResult[] results = ahsResult.getAuthResults();
            if (results != null && results.length > 0) {
                return results;
            }
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Error getting users.", e);
            return new AuthenticationResult[0]; // cannot continue here ...
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
                    if (ch.isCustomCredentialSource()) { // bingo
                        UserBean user = new UserBean();
                        user.setProviderId(Long.MAX_VALUE);
                        user.setLogin(creds.getLogin());
                        final String passwordHashed = passwordHasher.hashPassword(new String(creds.getCredentials()).getBytes(Charsets.UTF8));
                        //todo there does not appear to be a need for setting the hashed password. Only client of this logic is WsdlProxyServlet.
                        user.setHashedPassword(passwordHashed);
                        return new AuthenticationResult[] { new AuthenticationResult(user, creds.getSecurityTokens()) };
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

    private void requireEndpoint(HttpServletRequest req) throws ListenerException {
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

        ServerHttpBasic httpBasic = new ServerHttpBasic(new HttpBasic());
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
     * Put includes inline
     */
    private Assertion inlineIncludes(final Assertion assertion) throws PolicyAssertionException {
        final Assertion rootWithIncludes;

        if ( Assertion.contains(assertion, Include.class) ) {
            Set<String> guids = new HashSet<String>();
            final AssertionTranslator translator = new IncludeAssertionDereferenceTranslator(policyManager, guids, false, false);
            try {
                rootWithIncludes = Assertion.translate(WspReader.getDefault().parsePermissively(WspWriter.getPolicyXml(assertion), WspReader.OMIT_DISABLED), translator);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            rootWithIncludes = assertion;
        }

        return rootWithIncludes;
    }

    /**
     * Parse the given policy XML, inlining any includes.
     *
     * @param policyXml The XML to process.
     * @return The expanded policy with includes inlined
     * @throws IOException if an exception occurs
     */
    protected Assertion parsePolicy( final String policyXml ) throws IOException {
        Assertion rootassertion;
        try {
            rootassertion = inlineIncludes( wspReader.parsePermissively( policyXml, WspReader.OMIT_DISABLED) );
        } catch (PolicyAssertionException e) {
            throw new CausedIOException("Policy error", e);
        }
        return rootassertion;
    }

    /**
     * Decides whether a policy should be downloadable without providing credentials. This will return true if the
     * service described by this policy could be consumed anonymously.
     */
    protected boolean policyAllowAnonymous(Policy policy) throws IOException {
        // logic: a policy allows anonymous if and only if it does not contains any CredentialSourceAssertion
        // com.l7tech.policy.assertion.credential.CredentialSourceAssertion
        Assertion rootassertion = parsePolicy(policy.getXml());
        if (rootassertion == null)
            return true;

        Iterator it = rootassertion.preorderIterator();
        boolean allIdentitiesAreFederated = true;
        while (it.hasNext()) {
            Assertion a = (Assertion)it.next();
            if (!a.isEnabled()) {
                logger.fine("Assertion is disabled, ignore assertion.");
                continue;
            }

            if (a instanceof CustomAssertionHolder) {
                CustomAssertionHolder ca = (CustomAssertionHolder)a;
                if (ca.isCustomCredentialSource()) {
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

        if (allIdentitiesAreFederated && isAllowAnonymousPolicyIfAllIdentitiesFederated()) {
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
     * Check if this servlet wants to treat a policy containing nothing but federated identities
     * as a policy that allows anonymous access, for purposes of checking whether a user should
     * be given information relating to this policy or service.
     * <p/>
     * This is used in the identity bridging use case to allow an XVC from a different trust domain
     * to download the policy from the Gateway so that it will know what kind of SAML token to apply for
     * and how to consume the service.
     *
     * @return true iff. a policy with nothing but federated identity assertions should be treated by
     *              {@link #policyAllowAnonymous(com.l7tech.policy.Policy)} as though it had no identity assertions.
     */
    protected boolean isAllowAnonymousPolicyIfAllIdentitiesFederated() {
        return config.getBooleanProperty( "service.anonFederatedPolicies", false );
    }

    /**
     * Look for an assertion extending CredentialSourceAssertion in the assertion passed
     * and all it's decendents.
     * Returns null if not there.
     * (recursive method)
     */
    private Assertion findCredentialAssertion(Assertion arg) {
        if (arg.isEnabled() && arg.isCredentialSource()) {
            return arg;
        }
        if (arg.isEnabled() && arg instanceof CompositeAssertion) {
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
         return serviceCache.getCachedService(oid);
    }
}
