package com.l7tech.policy.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.message.HttpSoapRequest;
import com.l7tech.message.HttpSoapResponse;
import com.l7tech.message.HttpTransportMetadata;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.server.filter.FilterManager;
import com.l7tech.policy.server.filter.FilteringException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.AuthenticatableHttpServlet;
import com.l7tech.server.identity.IdProvConfManagerServer;
import com.l7tech.server.policy.PolicyService;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.*;
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
    // format is policyId|policyVersion (seperated with char '|')

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
    }

    /**
     * Soapy policy downloads
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            // check content type
            if (!req.getContentType().startsWith("text/html")) {
                logger.warning("Bad content type " + req.getContentType());
                generateFaultAndSendAsResponse(res, "content type not supported", req.getContentType());
                return;
            }

            // get document
            Document payload = null;
            try {
                payload = extractXMLPayload(req);
            } catch (ParserConfigurationException e) {
                String msg = "Could not parse payload as xml.";
                logger.log(Level.SEVERE, msg, e);
                generateFaultAndSendAsResponse(res, msg, e.getMessage());
                return;
            } catch (SAXException e) {
                String msg = "Could not parse payload as xml.";
                logger.log(Level.WARNING, msg, e);
                generateFaultAndSendAsResponse(res, msg, e.getMessage());
                return;
            }

            // pass over to the service
            PolicyService service = null;
            try {
                service = getService();
            } catch (CertificateException e) {
                logger.log(Level.SEVERE, "configuration exception, cannot get server cert.", e);
                generateFaultAndSendAsResponse(res, "Internal error", e.getMessage());
                return;
            } catch (KeyStoreException e) {
                logger.log(Level.SEVERE, "configuration exception, cannot get server key.", e);
                generateFaultAndSendAsResponse(res, "Internal error", e.getMessage());
                return;
            }

            HttpTransportMetadata htm = new HttpTransportMetadata(req, res);
            HttpSoapRequest sreq = new HttpSoapRequest(htm);
            sreq.setDocument(payload);
            HttpSoapResponse sres = new HttpSoapResponse(htm);
            service.respondToPolicyDownloadRequest(sreq, sres, normalPolicyGetter());

            Document response = null;
            try {
                response = sres.getDocument();
            } catch (SAXException e) {
                generateFaultAndSendAsResponse(res, "No response available", e.getMessage());
            }
            if (response == null) {
                if (sres.getFaultDetail() != null) {
                    generateFaultAndSendAsResponse(res, sres.getFaultDetail());
                    return;
                } else {
                    generateFaultAndSendAsResponse(res, "No response available", "");
                }
            }

            // check if the response is already a soap fault
            Element bodyChild = null;
            try {
                bodyChild = XmlUtil.findFirstChildElement(SoapUtil.getBodyElement(response));
            } catch (InvalidDocumentFormatException e) {
                generateFaultAndSendAsResponse(res, "Response is not soap", e.getMessage());
            }
            if ("Fault".equals(bodyChild.getLocalName())) {
                outputSoapFault(res, response);
                return;
            } else {
                outputPolicyDoc(res, response);
                return;
            }
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Could not get current persistence context to close.", e);
            }
        }
    }

    protected PolicyService getService() throws CertificateException, KeyStoreException, IOException {
        X509Certificate cert = null;
        PrivateKey key = null;
        cert = KeystoreUtils.getInstance().getSslCert();
        key = KeystoreUtils.getInstance().getSSLPrivateKey();
        return new PolicyService(key, cert);
    }

    protected PolicyService.PolicyGetter normalPolicyGetter() {
        return new PolicyService.PolicyGetter() {
            public PolicyService.ServiceInfo getPolicy(String serviceId) {
                final PublishedService targetService = resolveService(Long.parseLong(serviceId));
                if (targetService == null) return null;
                try {
                    final Assertion policy = targetService.rootAssertion();
                    return new PolicyService.ServiceInfo() {
                        public Assertion getPolicy() {
                            return policy;
                        }
                        public String getVersion() {
                            return Integer.toString(targetService.getVersion());
                        }
                    };
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "cannot parse policy", e);
                    return null;
                }
            }
        };
    }


    // todo, plug this in, get rid of old doGet()
    protected void doGetNew(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
        try {
            // GET THE PARAMETERS PASSED
            String str_oid = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID);
            String getCert = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_GETCERT);
            String username = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_USERNAME);
            String nonce = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_NONCE);

            // See if it's actually a certificate download request
            if (getCert != null) {
                try {
                    doCertDownload(res, username, nonce);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to fulfil certificate discovery request", e);
                    throw new ServletException("Unable to fulfil cert request", e);
                }
                return;
            }

            // get credentials and check that they are valid for this policy
            List users;
            try {
                users = authenticateRequestBasic(req);
            } catch (AuthenticationException e) {
                generateFaultAndSendAsResponse(res, "Authentication exception", e.getMessage());
                return;
            }

            // pass over to the service
            PolicyService service = null;
            try {
                service = getService();
            } catch (CertificateException e) {
                logger.log(Level.SEVERE, "configuration exception, cannot get server cert.", e);
                generateFaultAndSendAsResponse(res, "Internal error", e.getMessage());
                return;
            } catch (KeyStoreException e) {
                logger.log(Level.SEVERE, "configuration exception, cannot get server key.", e);
                generateFaultAndSendAsResponse(res, "Internal error", e.getMessage());
                return;
            }
            Document response = null;
            try {
                switch (users.size()) {
                    case 0:
                        response = service.respondToPolicyDownloadRequest(str_oid, null, this.normalPolicyGetter());
                        break;
                    case 1:
                        response = service.respondToPolicyDownloadRequest(str_oid, (User)(users.get(0)), this.normalPolicyGetter());
                        break;
                    default:
                        // todo use the best response (?)
                        throw new UnsupportedOperationException("not implemented");
                }
            } catch (FilteringException e) {
                logger.log(Level.WARNING, "Error in PolicyService", e);
                generateFaultAndSendAsResponse(res, "internal error", e.getMessage());
                return;
            } catch (SAXException e) {
                logger.log(Level.WARNING, "Error in PolicyService", e);
                generateFaultAndSendAsResponse(res, "internal error", e.getMessage());
                return;
            }
            if (response == null && users.size() < 1) {
                sendAuthChallenge(req, res);
                return;
            } else if (response == null) {
                logger.info("this policy download is refused.");
                generateFaultAndSendAsResponse(res, "Policy not found or download unauthorized", "");
                return;
            } else {
                outputPolicyDoc(res, response);
                return;
            }
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Could not get current persistence context to close.", e);
            }
        }
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
      throws ServletException, IOException {
        try {
            // GET THE PARAMETERS PASSED
            String str_oid = httpServletRequest.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID);
            String getCert = httpServletRequest.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_GETCERT);
            String username = httpServletRequest.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_USERNAME);
            String nonce = httpServletRequest.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_NONCE);

            // See if it's actually a certificate download request
            if (getCert != null) {
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
                String err = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + " parameter is required";
                logger.info(err);
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, err);
                return;
            } else {
                targetService = resolveService(Long.parseLong(str_oid));
            }

            if (targetService == null) {
                String err = "Incomplete request or service does not exist.";
                logger.info(err);
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
                users = authenticateRequestBasic(httpServletRequest, targetService);
            } catch (AuthenticationException e) {
                if (!anonymousok) {
                    logger.info("Returning 401 to requestor because invalid credentials were provided");
                    httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                    return;
                } else users = Collections.EMPTY_LIST;
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
                    httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    logger.fine("sending back authentication challenge");
                    // in this case, send an authentication challenge
                    httpServletResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + ServerHttpBasic.REALM + "\"");
                    httpServletResponse.getOutputStream().close();
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
                        logger.info("requestor tried to download policy that " +
                          "he should not be allowed to see - will return error");
                    }
                } else {
                    // even if the policy is anonymous, we might want to hide stuff
                    targetService = FilterManager.getInstance().applyAllFilters(null, targetService);
                }
            } catch (FilteringException e) {
                logger.log(Level.WARNING, "Could not filter policy", e);
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                  "Could not process policy. Consult server logs.");
                return;
            }

            // OUTPUT THE POLICY
            if (anonymousok && targetService == null) {
                outputEmptyPolicy(httpServletResponse, serviceid, serviceversion);
            } else
                outputPublishedServicePolicy(targetService, httpServletResponse);
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Could not get current persistence context to close.", e);
            }
        }
    }

    /**
     * Look up our certificate and transmit it to the client in PKCS#7 format.
     * If a username is given, we'll include a "Cert-Check-provId: " header containing
     * MD5(H(A1) . nonce . provId . cert . H(A1)), where H(A1) is the MD5 of "username:realm:password"
     * and provId is the ID of the identity provider that contained a matching username.
     * @param username username for automatic cert checking, or null to disable cert check
     * @param nonce nonce for automatic cert checking, or null to disable cert check
     */
    private void doCertDownload(HttpServletResponse response, String username, String nonce)
      throws FindException, IOException, NoSuchAlgorithmException {
        logger.finest("Request for root cert");
        // Find our certificate
        //byte[] cert = KeystoreUtils.getInstance().readRootCert();
        byte[] cert = KeystoreUtils.getInstance().readSSLCert();

        // Insert Cert-Check-NNN: headers if we can.
        if (username != null && nonce != null) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            ArrayList checks = findCheckInfos(username);
            for (Iterator i = checks.iterator(); i.hasNext();) {
                CheckInfo info = (CheckInfo)i.next();

                if (info != null) {
                    String hash = null;
                    if (info.ha1 == null) {
                        logger.info("Server does not have access to requestor's password and cannot send a cert check.");
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

    private void generateFaultAndSendAsResponse(HttpServletResponse res, String msg, String details) throws IOException {
        Document fault = null;
        try {
            fault = SoapFaultUtils.generateSoapFault(SoapFaultUtils.FC_SERVER,
                                                     msg,
                                                     details,
                                                     "");
        } catch (SAXException e) {
            throw new RuntimeException(e); // should not happen
        }
        outputSoapFault(res, fault);
        return;
    }

    private void generateFaultAndSendAsResponse(HttpServletResponse res, SoapFaultDetail sfd) throws IOException {
        Document fault = null;
        try {
            fault = SoapFaultUtils.generateSoapFault(sfd, "");
        } catch (SAXException e) {
            throw new RuntimeException(e); // should not happen
        }
        outputSoapFault(res, fault);
        return;
    }

    private void outputSoapFault(HttpServletResponse res, Document fault) throws IOException {
        logger.fine("Returning soap fault");
        res.setContentType(XmlUtil.TEXT_XML);
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        OutputStream os = res.getOutputStream();
        os.write(XmlUtil.nodeToString(fault).getBytes());
        os.close();
    }

    private void outputPolicyDoc(HttpServletResponse res, Document doc) throws IOException {
        logger.fine("Returning soap fault");
        res.setContentType(XmlUtil.TEXT_XML);
        res.setStatus(HttpServletResponse.SC_OK);
        OutputStream os = res.getOutputStream();
        os.write(XmlUtil.nodeToString(doc).getBytes());
        os.close();
    }

    private void outputEmptyPolicy(HttpServletResponse res, long serviceid, int serviceversion) throws IOException {
        res.addHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION,
          Long.toString(serviceid) + '|' + Long.toString(serviceversion));
        res.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
        res.getOutputStream().println(WspWriter.getPolicyXml(null));
        logger.fine("sent back empty policy");
    }

    private void outputPublishedServicePolicy(PublishedService service, HttpServletResponse response) throws IOException {
        if (service == null) {
            logger.fine("sending back 404 (either user has no business with this policy or " +
                        "the service could not be found)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
              "ERROR cannot resolve target service or you are not authorized to consult it");
            return;
        } else {
            logger.fine("sending back filtered policy for service " + service.getOid() + " version " +
                        service.getVersion());
            response.addHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION,
              Long.toString(service.getOid()) + '|' + Long.toString(service.getVersion()));
            response.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
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
            endTransaction();
        }
        // we smelt something (maybe netegrity?)
        CustomAssertionsRegistrar car = (CustomAssertionsRegistrar)Locator.getDefault().lookup(CustomAssertionsRegistrar.class);
        try {
            if (car != null && !car.getAssertions(Category.ACCESS_CONTROL).isEmpty()) {
                checkInfos.add(new CheckInfo(Long.MAX_VALUE, null, null));
            }
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Custom assertions error", e);
        }

        return checkInfos;
    }

    private Document extractXMLPayload(HttpServletRequest req)
            throws IOException, ParserConfigurationException, SAXException {
        BufferedReader reader = new BufferedReader(req.getReader());
        DocumentBuilder parser = getDomParser();
        return parser.parse( new InputSource(reader));
    }

    private DocumentBuilder getDomParser() throws ParserConfigurationException {
        DocumentBuilder builder = dbf.newDocumentBuilder();
        builder.setEntityResolver(XmlUtil.getSafeEntityResolver());
        return builder;
    }

    private void sendAuthChallenge(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        // send error back with a hint that credentials should be provided
        String newUrl = "https://" + httpServletRequest.getServerName();
        if (httpServletRequest.getServerPort() == 8080 || httpServletRequest.getServerPort() == 8443) {
            newUrl += ":8443";
        }
        newUrl += httpServletRequest.getRequestURI() + "?" + httpServletRequest.getQueryString();
        httpServletResponse.setHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER, newUrl);
        httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        logger.fine("sending back authentication challenge");
        // in this case, send an authentication challenge
        httpServletResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + ServerHttpBasic.REALM + "\"");
        httpServletResponse.getOutputStream().close();
        return;
    }

    private DocumentBuilderFactory dbf;
}

