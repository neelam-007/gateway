package com.l7tech.policy.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.server.filter.FilterManager;
import com.l7tech.policy.server.filter.FilteringException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.util.KeystoreUtils;
import com.l7tech.util.Locator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 11, 2003
 *
 * This servlet returns policy documents (type xml).
 * The following parameters can be passed to resolve the PublishedService:
 * serviceoid : the internal object identifier of the PublishedService. if specified, this parameter is sufficient to
 *              retrieve the policy
 * urn : the urn of the service. if more than one service have the same urn, at least one more paramater will be
 *       necessary
 * soapaction : the soapaction of the PublishedService
 *
 * Pass the parameters as part of the url as in the samples below
 * http://localhost:8080/ssg/policy/disco.modulator?serviceoid=666
 *
 */
public class PolicyServlet extends HttpServlet {
    public static final String PARAM_SERVICEOID = "serviceoid";
    public static final String PARAM_GETCERT = "getcert";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_NONCE = "nonce";
    // format is policyId|policyVersion (seperated with char '|')

    public void init( ServletConfig config ) throws ServletException {
        logger = LogManager.getInstance().getSystemLogger();
        super.init( config );
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        // GET THE PARAMETERS PASSED
        String str_oid = httpServletRequest.getParameter(PARAM_SERVICEOID);
        String getCert = httpServletRequest.getParameter(PARAM_GETCERT);
        String username = httpServletRequest.getParameter(PARAM_USERNAME);
        String nonce = httpServletRequest.getParameter(PARAM_NONCE);

        // See if it's actually a certificate download request
        if (getCert != null) {
            try {
                doCertDownload(httpServletRequest, httpServletResponse, username, nonce);
            } catch (Exception e) {
                  throw new ServletException("Unable to fulfil cert request", e);
            }
            return;
        }

        // RESOLVE THE PUBLISHED SERVICE
        PublishedService targetService = null;
        if ( str_oid == null || str_oid.length() == 0 ) {
            String err = PARAM_SERVICEOID + " parameter is required";
            logger.warning( err );
            httpServletResponse.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, err );
            return;
        } else {
            targetService = resolveService(Long.parseLong(str_oid));
        }

        if (targetService == null) {
            String err = "Incomplete request or service does not exist.";
            logger.warning( err );
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, err );
            return;
        }

        // BEFORE SENDING BACK THIS POLICY, WE NEED TO DECIDE IF THE REQUESTOR IS ALLOWED TO SEE IT
        // if policy does not allow anonymous access, then it should not be accessible through http
        boolean anonymousok = policyAllowAnonymous(targetService);
        if (!anonymousok && !httpServletRequest.isSecure()) {
            // send error back axing to come back through ssl
            String newUrl = "https://"  + httpServletRequest.getServerName();
            if (httpServletRequest.getServerPort() == 8080) newUrl += ":8443";
            newUrl += httpServletRequest.getRequestURI() + "?" + httpServletRequest.getQueryString();
            httpServletResponse.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, newUrl);
            httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request must come through SSL. " + newUrl);
            logger.info("Non-anonymous policy requested on in-secure channel (http). Sending 401 back with secure URL to requestor: " + newUrl);
            return;
        }
        // get credentials and check that they are valid for this policy
        User user = null;
        if (!anonymousok) {
            user = authenticateRequest(httpServletRequest);
            if (user == null) {
                // send error back with a hint that credentials should be provided
                String newUrl = "https://"  + httpServletRequest.getServerName();
                if (httpServletRequest.getServerPort() == 8080 || httpServletRequest.getServerPort() == 8443) newUrl += ":8443";
                newUrl += httpServletRequest.getRequestURI() + "?" + httpServletRequest.getQueryString();
                httpServletResponse.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, newUrl);
                httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must provide valid credentials.");
                return;
            }
        }

        // THE POLICY SHOULD BE STRIPPED OUT OF ANYTHING THAT THE REQUESTOR SHOULD NOT BE ALLOWED TO SEE
        // (this may be everything, if the user has no business seeing this policy)
        try {
            // finer, not logged by default. change log level in web.xml to see these
            logger.finer("Policy before filtering: " + targetService.getPolicyXml());
            targetService = FilterManager.getInstance().applyAllFilters(user, targetService);
            // finer, not logged by default. change log level in web.xml to see these
            logger.finer("Policy after filtering: " + ((targetService == null) ? "null" : targetService.getPolicyXml()));
            if (targetService == null) logger.warning("requestor tried to download policy that he should not be allowed to see - will return error");
        } catch (FilteringException e) {
            logger.log(Level.SEVERE, "Could not filter policy", e);
            httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not process policy. Consult server logs.");
            return;
        }

        // OUTPUT THE POLICY
        outputPublishedServicePolicy(targetService, httpServletResponse);
    }

    /**
     * Look up our certificate and transmit it to the client in PKCS#7 format.
     * If a username is given, we'll include a "Cert-Check: " header containing
     * SHA1(cert . H(A1)).  (where H(A1) is the SHA1 of the Base64'ed "username:password".)
     */
    private void doCertDownload(HttpServletRequest request, HttpServletResponse response,
                                String username, String nonce)
            throws FindException, IOException, NoSuchAlgorithmException
    {
        // Find our certificate
        byte[] cert = KeystoreUtils.getInstance().readRootCert();
        logger.info("Sending root cert");

        // Insert Cert-Check-NNN: headers if we can.
        if (username != null) {
            ArrayList checks = findCheckInfos(username);
            for (Iterator i = checks.iterator(); i.hasNext();) {
                CheckInfo info = (CheckInfo) i.next();

                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.reset();
                if (nonce != null)
                    md5.update(nonce.getBytes());
                md5.update(String.valueOf(info.idProvider).getBytes());
                md5.update(cert);
                md5.update(info.ha1.getBytes());
                response.addHeader("Cert-Check-" + info.idProvider, HexUtils.encodeMd5Digest(md5.digest()));
            }
        }

        response.setStatus(200);
        response.setContentType("application/x-x509-ca-cert");
        response.setContentLength(cert.length);
        response.getOutputStream().write(cert);
        response.flushBuffer();
    }

    private PublishedService resolveService(long oid) {
        try {
            return getServiceManagerAndBeginTransaction().findByPrimaryKey(oid);
        } catch (Exception e) {
            e.printStackTrace(System.err);
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
    }

    private void outputPublishedServicePolicy(PublishedService service, HttpServletResponse response) throws IOException {
        if (service == null) {
            response.addHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION,
                               Long.toString(service.getOid()) + '|' + Long.toString(service.getVersion()));
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "ERROR cannot resolve target service or you are not authorized to consult it");
            return;
        } else {
            response.setContentType("text/xml; charset=utf-8");
            response.getOutputStream().println(service.getPolicyXml());
        }
    }

    private class CheckInfo {
        public CheckInfo(long idp, String h) { idProvider = idp; ha1 = h; }
        public final long idProvider;
        public final String ha1;
    }

    /**
     * Given a username, find all matching users in every registered ID provider
     * and return the corresponding IdProv OID and H(A1) string.
     *
     * @param username
     * @return A collection of CheckInfo instances.
     * @throws FindException if the ID Provider list could not be determined.
     */
    private ArrayList findCheckInfos(String username) throws FindException {
        IdentityProviderConfigManager configManager = new IdProvConfManagerServer();
        ArrayList checkInfos = new ArrayList();

        try {
            Collection idps = configManager.findAllIdentityProviders();
            for (Iterator i = idps.iterator(); i.hasNext();) {
                IdentityProvider provider = (IdentityProvider) i.next();
                try {
                    User user = provider.getUserManager().findByLogin(username.trim());
                    if (user != null)
                        checkInfos.add(new CheckInfo(provider.getConfig().getOid(), user.getPassword()));
                } catch (FindException e) {
                    // Log it and continue
                    logger.log(Level.WARNING, null, e);
                }
            }
        } finally {
            try {
                endTransaction();
            } catch (SQLException se) {
                logger.log(Level.WARNING, null, se);
            } catch (TransactionException te) {
                logger.log(Level.WARNING, null, te);
            }
        }

        return checkInfos;
    }

    private com.l7tech.service.ServiceManager getServiceManagerAndBeginTransaction() throws java.sql.SQLException, TransactionException {
        if (serviceManagerInstance == null){
            initialiseServiceManager();
        }
        PersistenceContext.getCurrent().beginTransaction();
        return serviceManagerInstance;
    }

    private void endTransaction() throws java.sql.SQLException, TransactionException {
        PersistenceContext context = PersistenceContext.getCurrent();
        context.commitTransaction();
        context.close();
    }

    private synchronized void initialiseServiceManager() throws ClassCastException, RuntimeException {
        serviceManagerInstance = (com.l7tech.service.ServiceManager)Locator.getDefault().lookup(com.l7tech.service.ServiceManager.class);
        if (serviceManagerInstance == null) throw new RuntimeException("Cannot instantiate the ServiceManager");
    }

    /**
     * Decides whether a policy should be downloadable without providing credentials. This will return true if the
     * service described by this policy could be consumed anonymouly.
     */
    private boolean policyAllowAnonymous(PublishedService policy) throws IOException {
        // todo, validate the following assumption
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

    /**
     * look for basic creds in the request and authenticate them
     */
    private User authenticateRequest(HttpServletRequest req) throws IOException {
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
                if (provider.authenticate(creds)) {
                    logger.info("Authentication successful for user " + creds.getUser().getLogin() + " on identity provider: " + provider.getConfig().getName());
                    return creds.getUser();
                }
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

    private ServiceManager serviceManagerInstance = null;
    private Logger logger = null;
}

