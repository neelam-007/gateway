package com.l7tech.policy.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.CustomAssertions;
import com.l7tech.policy.server.filter.FilterManager;
import com.l7tech.policy.server.filter.FilteringException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.AuthenticatableHttpServlet;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;


/**
 * This servlet returns policy documents (type xml).
 * The following parameters can be passed to resolve the PublishedService:
 * serviceoid : the internal object identifier of the PublishedService. if specified, this parameter is sufficient to
 * retrieve the policy
 * <br/>
 * urn : the urn of the service. if more than one service have the same urn, at least one more paramater will be
 * necessary
 * <br/>
 * soapaction : the soapaction of the PublishedService
 * <br/>
 * Pass the parameters as part of the url as in the samples below
 * http://localhost:8080/ssg/policy/disco.modulator?serviceoid=666
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 11, 2003
 */
public class PolicyServlet extends AuthenticatableHttpServlet {
    public static final String PARAM_SERVICEOID = "serviceoid";
    public static final String PARAM_GETCERT = "getcert";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_NONCE = "nonce";
    // format is policyId|policyVersion (seperated with char '|')

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
      throws ServletException, IOException {
        PersistenceContext pc = null;
        try {
            // GET THE PARAMETERS PASSED
            String str_oid = httpServletRequest.getParameter(PARAM_SERVICEOID);
            String getCert = httpServletRequest.getParameter(PARAM_GETCERT);
            String username = httpServletRequest.getParameter(PARAM_USERNAME);
            String nonce = httpServletRequest.getParameter(PARAM_NONCE);

            pc = PersistenceContext.getCurrent();

            // See if it's actually a certificate download request
            if (getCert != null) {
                if (nonce == null)
                    throw new ServletException("Unable to fulfil cert request: a nonce is required");
                if (username == null)
                    throw new ServletException("Unable to fulfil cert request: a username is required");
                try {
                    doCertDownload(httpServletResponse, username, nonce);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to fulfil certificate discovery request", e);
                    throw new ServletException("Unable to fulfil cert request", e);
                }
                return;
            }

            // RESOLVE THE PUBLISHED SERVICE
            PublishedService targetService = null;
            if (str_oid == null || str_oid.length() == 0) {
                String err = PARAM_SERVICEOID + " parameter is required";
                logger.warning(err);
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, err);
                return;
            } else {
                targetService = resolveService(Long.parseLong(str_oid));
            }

            if (targetService == null) {
                String err = "Incomplete request or service does not exist.";
                logger.warning(err);
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, err);
                return;
            }

            long serviceid = targetService.getOid();
            int serviceversion = targetService.getVersion();

            // BEFORE SENDING BACK THIS POLICY, WE NEED TO DECIDE IF THE REQUESTOR IS ALLOWED TO SEE IT
            // if policy does not allow anonymous access, then it should not be accessible through http
            boolean anonymousok = policyAllowAnonymous(targetService);
            if (!anonymousok && !httpServletRequest.isSecure()) {
                // send error back axing to come back through ssl
                String newUrl = "https://" + httpServletRequest.getServerName();
                if (httpServletRequest.getServerPort() == 8080) newUrl += ":8443";
                newUrl += httpServletRequest.getRequestURI() + "?" + httpServletRequest.getQueryString();
                httpServletResponse.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, newUrl);
                httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                  "Request must come through SSL. " + newUrl);
                logger.info("Non-anonymous policy requested on in-secure channel (http). " +
                  "Sending 401 back with secure URL to requestor: " + newUrl);
                return;
            }

            // get credentials and check that they are valid for this policy
            List users;
            try {
                users = authenticateRequestBasic(httpServletRequest, serviceid);
            } catch (BadCredentialsException e) {
                logger.log(Level.SEVERE, "returning 401 to requestor because invalid creds were provided", e);
                httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                return;
            }

            if (!anonymousok) {
                if (users == null || users.isEmpty()) {
                    // send error back with a hint that credentials should be provided
                    String newUrl = "https://" + httpServletRequest.getServerName();
                    if (httpServletRequest.getServerPort() == 8080 || httpServletRequest.getServerPort() == 8443) {
                        newUrl += ":8443";
                    }
                    newUrl += httpServletRequest.getRequestURI() + "?" + httpServletRequest.getQueryString();
                    httpServletResponse.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, newUrl);
                    httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must provide valid credentials.");
                    return;
                }
            }

            // THE POLICY SHOULD BE STRIPPED OUT OF ANYTHING THAT THE REQUESTOR SHOULD NOT BE ALLOWED TO SEE
            // (this may be everything, if the user has no business seeing this policy)
            try {
                if (!anonymousok) {
                    boolean someonecanseethis = false;
                    for (Iterator i = users.iterator(); i.hasNext();) {
                        User user = (User)i.next();
                        // logger.finer("Policy before filtering: " + targetService.getPolicyXml());
                        PublishedService tempService = FilterManager.getInstance().applyAllFilters(user, targetService);
                        //logger.finer("Policy after filtering: " +
                        //             ((tempService == null) ? "null" : tempService.getPolicyXml()));
                        if (tempService != null) {
                            targetService = tempService;
                            someonecanseethis = true;
                            break;
                        }
                    }

                    if (!someonecanseethis) {
                        targetService = null;
                    }

                    if (targetService == null) {
                        logger.warning("requestor tried to download policy that " +
                          "he should not be allowed to see - will return error");
                    }
                } else {
                    // even if the policy is anonymous, we might want to hide stuff
                    targetService = FilterManager.getInstance().applyAllFilters(null, targetService);
                }
            } catch (FilteringException e) {
                logger.log(Level.SEVERE, "Could not filter policy", e);
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                  "Could not process policy. Consult server logs.");
                return;
            }

            // OUTPUT THE POLICY
            if (anonymousok && targetService == null) {
                outputEmptyPolicy(httpServletResponse, serviceid, serviceversion);
            } else
                outputPublishedServicePolicy(targetService, httpServletResponse);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (pc != null) pc.close();
        }
    }

    /**
     * Overriden method to support additional user resolution for custom
     * assertions that provide cusotm identity authentication.
     * <p/>
     * First do the the standard user check, and if the list of users is
     * empty, retrieve custom assertions. If hte user list is still empty
     * then check if there is a custom assertion in the policy. If true,
     * and the assertion is registered as <code>Category</code> {@link Category#IDENTITY}
     * then we let that user through, since the custom assertion is
     * responsible for validating the credentials. Custom identity authentication
     * assertions may not provide user management API (list users etc.) therefore
     * we just do this check and delegate. This is how Netegrity Siteminder works
     * for exmaple.
     *
     * @param req       the servlet request
     * @param serviceId the service oid
     * @return the list of users
     * @throws IOException             on io error
     * @throws BadCredentialsException on vad credentials
     */
    protected List authenticateRequestBasic(HttpServletRequest req, long serviceId)
      throws IOException, BadCredentialsException {
        List users = super.authenticateRequestBasic(req);
        if (!users.isEmpty()) {
            return users;
        }
        LoginCredentials creds = findCredentialsBasic(req);
        if (creds == null) {
            return users;
        }
        PublishedService svc = resolveService(serviceId);
        if (svc == null) {
            throw new IOException("Service not found id '" + serviceId + "'");
        }

        Iterator it = svc.rootAssertion().preorderIterator();
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

        if (users.isEmpty()) {
            String msg = "Creds do not authenticate against any registered id provider.";
            logger.warning(msg);
            throw new BadCredentialsException(msg);
        }
        return users;
    }

    /**
     * Look up our certificate and transmit it to the client in PKCS#7 format.
     * If a username is given, we'll include a "Cert-Check-provId: " header containing
     * MD5(H(A1) . nonce . provId . cert . H(A1)), where H(A1) is the MD5 of "username:realm:password"
     * and provId is the ID of the identity provider that contained a matching username.
     */
    private void doCertDownload(HttpServletResponse response, String username, String nonce)
      throws FindException, IOException, NoSuchAlgorithmException {
        logger.finest("Request for root cert");
        // Find our certificate
        byte[] cert = KeystoreUtils.getInstance().readRootCert();

        // Insert Cert-Check-NNN: headers if we can.
        if (username != null) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            ArrayList checks = findCheckInfos(username);
            for (Iterator i = checks.iterator(); i.hasNext();) {
                CheckInfo info = (CheckInfo)i.next();

                if (info != null) {
                    String hash = null;
                    if (info.ha1 == null) {
                        logger.warning("Server does not have access to requestor's password and cannot send a cert check.");
                        hash = SecureSpanConstants.NOPASS;
                    } else {
                        md5.reset();
                        md5.update(info.ha1.getBytes());
                        md5.update(nonce.getBytes());
                        md5.update(String.valueOf(info.idProvider).getBytes());
                        md5.update(cert);
                        md5.update(info.ha1.getBytes());
                        hash = HexUtils.encodeMd5Digest(md5.digest());
                    }

                    response.addHeader(SecureSpanConstants.HttpHeaders.CERT_CHECK_PREFIX + info.idProvider,
                      hash + "; " + info.realm);
                }
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
            } catch (SQLException se) {
                logger.log(Level.WARNING, null, se);
            } catch (TransactionException te) {
                logger.log(Level.WARNING, null, te);
            }
        }
    }

    private void outputEmptyPolicy(HttpServletResponse res, long serviceid, int serviceversion) throws IOException {
        res.addHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION,
          Long.toString(serviceid) + '|' + Long.toString(serviceversion));
        res.setContentType("text/xml; charset=utf-8");
        res.getOutputStream().println(WspWriter.getPolicyXml(null));
    }

    private void outputPublishedServicePolicy(PublishedService service, HttpServletResponse response) throws IOException {
        if (service == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
              "ERROR cannot resolve target service or you are not authorized to consult it");
            return;
        } else {
            response.addHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION,
              Long.toString(service.getOid()) + '|' + Long.toString(service.getVersion()));
            response.setContentType("text/xml; charset=utf-8");
            response.getOutputStream().println(service.getPolicyXml());
        }
    }

    private class CheckInfo {
        public CheckInfo(long idp, String h, String r) {
            idProvider = idp;
            ha1 = h;
            realm = r;
        }

        public final long idProvider;
        public final String ha1;
        public final String realm;
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
                IdentityProvider provider = (IdentityProvider)i.next();
                try {
                    User user = provider.getUserManager().findByLogin(username.trim());
                    if (user != null) {
                        checkInfos.add(new CheckInfo(provider.getConfig().getOid(),
                          user.getPassword(), provider.getAuthRealm()));
                    }
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
        // we smelt something (maybe netegrity?)
        if (!CustomAssertions.getAssertions(Category.IDENTITY).isEmpty()) {
            checkInfos.add(new CheckInfo(Long.MAX_VALUE, null, null));
        }

        return checkInfos;
    }

    private com.l7tech.service.ServiceManager getServiceManagerAndBeginTransaction()
      throws SQLException, TransactionException {
        if (serviceManagerInstance == null) {
            initialiseServiceManager();
        }
        PersistenceContext.getCurrent().beginTransaction();
        return serviceManagerInstance;
    }

    private void endTransaction() throws SQLException, TransactionException {
        PersistenceContext context = PersistenceContext.getCurrent();
        context.commitTransaction();
        context.close();
    }

    private synchronized void initialiseServiceManager() throws ClassCastException, RuntimeException {
        serviceManagerInstance = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        if (serviceManagerInstance == null) {
            throw new RuntimeException("Cannot instantiate the ServiceManager");
        }
    }


    private ServiceManager serviceManagerInstance = null;
}

