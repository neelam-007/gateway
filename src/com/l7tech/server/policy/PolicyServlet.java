package com.l7tech.server.policy;

import com.l7tech.common.LicenseException;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.HttpServletResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.server.AuthenticatableHttpServlet;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.config.KeystoreUtils;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.event.system.PolicyServiceEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
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
 * http://localhost:8080/ssg/policy/disco?serviceoid=666
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 11, 2003
 */
public class PolicyServlet extends AuthenticatableHttpServlet {
    private AuditContext auditContext;
    private byte[] serverCertificate;
    private ServerConfig serverConfig;

    /** A serviceoid request that comes in via this URI should be served a compatibility-mode policy. */
    private static final String PRE32_DISCO_URI = "disco.modulator";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            ApplicationContext applicationContext = getApplicationContext();
            auditContext = (AuditContext)applicationContext.getBean("auditContext", AuditContext.class);
            KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore", KeystoreUtils.class);
            serverCertificate = ku.readSSLCert();
            serverConfig = (ServerConfig)applicationContext.getBean("serverConfig");
        }
        catch (BeansException be) {
            throw new ServletException(be);
        }
        catch (IOException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Soapy policy downloads
     */
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws ServletException, IOException {
        try {
            // check content type
            if (!servletRequest.getContentType().startsWith("text/xml")) {
                logger.warning("Bad content type " + servletRequest.getContentType());
                generateFaultAndSendAsResponse(servletResponse, "content type not supported", servletRequest.getContentType());
                return;
            }

            boolean pre32PolicyCompat = false;
            if (servletRequest.getRequestURI().indexOf(PRE32_DISCO_URI) >= 0) {
                // Emit policies in pre-3.2 compatibility mode
                pre32PolicyCompat = true;
            }

            Message request = new Message();
            request.initialize(new ByteArrayStashManager(),
                               ContentTypeHeader.parseValue(servletRequest.getContentType()),
                               servletRequest.getInputStream());
            request.attachHttpRequestKnob(new HttpServletRequestKnob(servletRequest));

            Message response = new Message();
            response.attachHttpResponseKnob(new HttpServletResponseKnob(servletResponse));

            PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
            context.setAuditContext(auditContext);
            boolean success = false;

            try {
                // pass over to the service
                PolicyService service = getPolicyService();

                try {
                    service.respondToPolicyDownloadRequest(context, true, normalPolicyGetter(), pre32PolicyCompat);
                }
                catch(IllegalStateException ise) { // throw by policy getter on policy not found
                    generateFaultAndSendAsResponse(servletResponse, "Service not found", ise.getMessage());
                }

                Document responseDoc = null;
                try {
                    responseDoc = response.getXmlKnob().getDocumentReadOnly();
                } catch (SAXException e) {
                    generateFaultAndSendAsResponse(servletResponse, "No response available", e.getMessage());
                    return;
                }

                if (response == null) {
                    if (context.getFaultDetail() != null) {
                        generateFaultAndSendAsResponse(servletResponse, context.getFaultDetail());
                    } else {
                        generateFaultAndSendAsResponse(servletResponse, "No response available", "");
                    }
                }
                else {
                    // check if the response is already a soap fault
                    if (isDocFault(responseDoc)) {
                        outputSoapFault(servletResponse, responseDoc);
                    } else {
                        outputPolicyDoc(servletResponse, responseDoc);
                        success = true;
                    }
                }
            }
            finally {
                try {
                    String message = success ? "Policy Service: Success" : "Policy Service: Failed";
                    User user = getUser(context);
                    getApplicationContext().publishEvent(new PolicyServiceEvent(this, Level.INFO, servletRequest.getRemoteAddr(), message, user.getProviderId(), getName(user), user.getUniqueIdentifier()));
                }
                catch(Exception e) {
                    logger.log(Level.WARNING, "Error publishing event", e);
                }
                finally {
                    context.close();
                }
            }
        } catch (Exception e) { // this is to avoid letting the servlet engine returning ugly html error pages.
            logger.log(Level.SEVERE, "Unexpected exception:", e);
            generateFaultAndSendAsResponse(servletResponse, "Internal error", e.getMessage());
        }
    }

    protected PolicyService getPolicyService() {
        return (PolicyService)getApplicationContext().getBean("policyService");
    }

    protected PolicyService.PolicyGetter normalPolicyGetter() {
        return new PolicyService.PolicyGetter() {
            public PolicyService.ServiceInfo getPolicy(String serviceId) {
                try {
                    final PublishedService targetService = resolveService(Long.parseLong(serviceId));
                    if (targetService == null) throw new IllegalStateException("Service not found ("+serviceId+")"); // caught by us in doGet and doPost

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
                } catch (NumberFormatException e) {
                    logger.log(Level.INFO, "cannot parse service id: " + serviceId);
                    throw new IllegalStateException("Service not found ("+serviceId+")"); // caught by us in doGet and doPost
                }
            }
        };
    }


    /**
     * HTTP Get policy downloads for those who want to see policies in their browser
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
        // GET THE PARAMETERS PASSED
        String str_oid = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID);
        String getCert = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_GETCERT);
        String username = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_USERNAME);
        String nonce = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_NONCE);
        String fullDoc = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_FULLDOC);

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

        boolean pre32PolicyCompat = false;
        if (req.getRequestURI().indexOf(PRE32_DISCO_URI) >= 0) {
            // Emit policies in pre-3.2 compatibility mode
            pre32PolicyCompat = true;
        }

        // get credentials and check that they are valid for this policy
        AuthenticationResult[] results;
        try {
            results = authenticateRequestBasic(req);
        } catch (AuthenticationException e) {
            logger.log(Level.FINE, "Authentication exception: " + e.getMessage());
            results = new AuthenticationResult[0];
        } catch (LicenseException e) {
            logger.log(Level.WARNING, "Service is unlicensed, returning 500", e);
            generateFaultAndSendAsResponse(res, "Gateway policy discovery service not enabled by license", e.getMessage());
            return;
        }

        boolean isFullDoc = false;
        if (fullDoc != null && fullDoc.length() > 0) {
            if ("yes".equals(fullDoc) || "Yes".equals(fullDoc) || "YES".equals(fullDoc)) {
                isFullDoc = true;
            } else {
                isFullDoc = Boolean.parseBoolean(fullDoc);
            }
            logger.finest("Passed value for " + SecureSpanConstants.HttpQueryParameters.PARAM_FULLDOC + " was " + fullDoc);
            if (isFullDoc)
                logger.finest("Will passthrough and return full policy document");
        }
        // if user asking for full doc, make sure it's allowed
        if (isFullDoc) {
            isFullDoc = systemAllowsAnonymousDownloads(req);
        }

        // pass over to the service
        PolicyService service = getPolicyService();
        Document response = null;
        try {
            switch (results.length) {
                case 0:
                    response = service.respondToPolicyDownloadRequest(str_oid, null, this.normalPolicyGetter(), pre32PolicyCompat, isFullDoc);
                    break;
                case 1:
                    response = service.respondToPolicyDownloadRequest(str_oid, results[0].getUser(), this.normalPolicyGetter(), pre32PolicyCompat, isFullDoc);
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
        } catch(IllegalStateException ise) { // invalid service
            generateFaultAndSendAsResponse(res, "Service not found.", ise.getMessage());
            return;
        } catch(UnsupportedOperationException uoe) {
            generateFaultAndSendAsResponse(res, "internal error", "");
            return;
        }

        if (response == null && (results == null || results.length < 1)) {
            logger.finest("sending challenge");
            sendAuthChallenge(req, res);
        } else if (response == null) {
            logger.info("this policy download is refused.");
            generateFaultAndSendAsResponse(res, "Policy not found or download unauthorized", "");
        } else {
            logger.finest("returning policy");
            outputPolicyDoc(res, response);
        }
    }

    private boolean systemAllowsAnonymousDownloads(HttpServletRequest req) {
        // split strings into seperate values
        // check whether any of those can match start of
        String allPassthroughs = serverConfig.getProperty("passthroughDownloads");
        StringTokenizer st = new StringTokenizer(allPassthroughs);
        String remote = req.getRemoteAddr();
        while (st.hasMoreTokens()) {
            String passthroughVal = st.nextToken();
            if (remote.startsWith(passthroughVal)) {
                logger.fine("remote ip " + remote + " was authorized by passthrough value " + passthroughVal);
                return true;
            }
        }
        logger.finest("remote ip " + remote + " was not authorized by any passthrough in " + allPassthroughs);
        return false;
    }

    private boolean isDocFault(Document doc) {
        if (doc == null) return false;
        Element bodyChild = null;
        try {
            Element body = SoapUtil.getBodyElement(doc);
            if(body!=null) {
                bodyChild = XmlUtil.findFirstChildElement(body);
            }
        } catch (InvalidDocumentFormatException e) {
            logger.log(Level.WARNING, "cannot inspect document for fault", e);
            return false;
        }
        return bodyChild!=null && "Fault".equals(bodyChild.getLocalName());
    }

    /**
     * Look up our certificate and transmit it to the client in PKCS#7 format.
     * If a username is given, we'll include a "Cert-Check-provId: " header containing
     * MD5(H(A1) . nonce . provId . cert . H(A1)), where H(A1) is the MD5 of "username:realm:password"
     * and provId is the ID of the identity provider that contained a matching username.
     *
     * @param username username for automatic cert checking, or null to disable cert check
     * @param nonce    nonce for automatic cert checking, or null to disable cert check
     */
    private void doCertDownload(HttpServletResponse response, String username, String nonce)
      throws FindException, IOException {
        logger.finest("Request for root cert");
        // Find our certificate
        //byte[] cert = KeystoreUtils.getInstance().readRootCert();
        byte[] pemEncodedServerCertificate = CertUtils.encodeAsPEM(serverCertificate);

        // Insert Cert-Check-NNN: headers if we can.
        if (username != null && nonce != null) {
            Collection checks = findCheckInfos(username, pemEncodedServerCertificate, nonce);
            for (Iterator i = checks.iterator(); i.hasNext();) {
                CertificateCheckInfo info = (CertificateCheckInfo)i.next();
                if (info != null) {
                    HttpHeader header = info.asHttpHeader();
                    response.addHeader(header.getName(), header.getFullValue());
                }
            }
        }

        response.setStatus(200);
        response.setContentType("application/x-x509-server-cert");
        response.setContentLength(pemEncodedServerCertificate.length);
        response.getOutputStream().write(pemEncodedServerCertificate);
        response.flushBuffer();
    }


    private void generateFaultAndSendAsResponse(HttpServletResponse res, String msg, String details) throws IOException {
        Document fault;
        try {
            Element exceptiondetails = null;
            if (details != null && details.length() > 0) {
                exceptiondetails = SoapFaultUtils.makeFaultDetailsSubElement("more", details);
            }
            fault = SoapFaultUtils.generateSoapFaultDocument(SoapFaultUtils.FC_SERVER,
                                                             msg,
                                                             exceptiondetails,
                                                             "");
        } catch (SAXException e) {
            throw new RuntimeException(e); // should not happen
        }
        outputSoapFault(res, fault);
    }

    private void generateFaultAndSendAsResponse(HttpServletResponse res, SoapFaultDetail sfd) throws IOException {
        Document fault;
        try {
            fault = SoapFaultUtils.generateSoapFaultDocument(sfd, "");
        } catch (SAXException e) {
            throw new RuntimeException(e); // should not happen
        }
        outputSoapFault(res, fault);
    }

    private void outputSoapFault(HttpServletResponse res, Document fault) throws IOException {
        logger.fine("Returning soap fault\n" + XmlUtil.nodeToFormattedString(fault));
        res.setContentType(XmlUtil.TEXT_XML);
        // fla changed this so that soap faults are always 500 to comply to WSI 1305
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        res.setContentType(ContentTypeHeader.XML_DEFAULT.getFullValue());
        OutputStream os = res.getOutputStream();
        XmlUtil.nodeToOutputStream(fault, os);
        os.close();
    }

    private void outputPolicyDoc(HttpServletResponse res, Document doc) throws IOException {
        res.setContentType(XmlUtil.TEXT_XML);
        res.setStatus(HttpServletResponse.SC_OK);
        OutputStream os = res.getOutputStream();
        XmlUtil.nodeToOutputStream(doc,os);
        os.close();
    }

    /**
     * Given a username, find all matching users in every registered ID provider
     * and return the corresponding {@link com.l7tech.common.util.CertificateCheckInfo} instance.
     *
     * @param username
     * @return A collection of {@link CertificateCheckInfo} instances.
     * @throws com.l7tech.objectmodel.FindException
     *          if the ID Provider list could not be determined.
     */
    private Collection findCheckInfos(String username, byte[] certBytes, String nonce) throws FindException {
        ArrayList checkInfos = new ArrayList();
        final String trimmedUsername = username.trim();

        Collection idps = providerConfigManager.findAllIdentityProviders();
        for (Iterator i = idps.iterator(); i.hasNext();) {
            IdentityProvider provider = (IdentityProvider)i.next();
            try {
                User user = provider.getUserManager().findByLogin(trimmedUsername);
                if (user != null) {
                    String password = user.getPassword();
                    String oid = Long.toString(provider.getConfig().getOid());
                    String realm = provider.getAuthRealm();
                    checkInfos.add(new CertificateCheckInfo(certBytes, trimmedUsername, password, nonce, oid, realm));
                }
            } catch (FindException e) {
                // Log it and continue
                logger.log(Level.WARNING, null, e);
            }
        }
        // we smelt something (maybe netegrity?)
        CustomAssertionsRegistrar car = (CustomAssertionsRegistrar)getApplicationContext().getBean("customAssertionRegistrar");
        try {
            if (car != null && !car.getAssertions(Category.ACCESS_CONTROL).isEmpty()) {
                checkInfos.add(new CertificateCheckInfo(Long.toString(Long.MAX_VALUE), SecureSpanConstants.NOPASS, null));
            }
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Custom assertions error", e);
        }

        return checkInfos;
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
    }


    private User getUser(PolicyEnforcementContext context) {
        User user = null;

        if(context.isAuthenticated()) {
            user = context.getAuthenticatedUser();
        }

        if(user==null) {
            user = new UserBean();
        }

        return user;
    }

    private String getName(User user) {
        return user.getName()!=null ? user.getName() : user.getLogin();
    }
}

