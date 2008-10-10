package com.l7tech.server.policy;

import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.Policy;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.CertificateCheckInfo;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.event.system.PolicyServiceEvent;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.gateway.common.service.PublishedService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.security.cert.CertificateException;


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
    private static final String DEFAULT_CONTENT_TYPE = XmlUtil.TEXT_XML + "; charset=utf-8";

    private AuditContext auditContext;
    private SoapFaultManager soapFaultManager;
    private byte[] serverCertificate;
    private ServerConfig serverConfig;
    private ClusterPropertyCache clusterPropertyCache;
    private PolicyPathBuilder policyPathBuilder;
    private PolicyCache policyCache;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            ApplicationContext applicationContext = getApplicationContext();
            auditContext = (AuditContext)applicationContext.getBean("auditContext", AuditContext.class);
            soapFaultManager = (SoapFaultManager)applicationContext.getBean("soapFaultManager");
            DefaultKey ku = (DefaultKey)applicationContext.getBean("defaultKey", DefaultKey.class);
            serverCertificate = ku.getSslInfo().getCertificate().getEncoded();
            serverConfig = (ServerConfig)applicationContext.getBean("serverConfig");
            clusterPropertyCache = (ClusterPropertyCache)applicationContext.getBean("clusterPropertyCache");
            PolicyPathBuilderFactory pathBuilderFactory = (PolicyPathBuilderFactory) applicationContext.getBean("policyPathBuilderFactory");
            policyPathBuilder = pathBuilderFactory.makePathBuilder();
            policyCache = (PolicyCache) applicationContext.getBean( "policyCache", PolicyCache.class );
        } catch (BeansException be) {
            throw new ServletException(be);
        }catch (IOException e) {
            throw new ServletException(e);
        } catch (CertificateException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_POLICYDISCO;
    }

    @Override
    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.POLICYDISCO;
    }

    /**
     * Soapy policy downloads
     */
    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws ServletException, IOException {
        PolicyEnforcementContext context = null;
        try {
            // check content type
            if (!servletRequest.getContentType().startsWith("text/xml")) {
                logger.warning("Bad content type " + servletRequest.getContentType());
                return;
            }

            Message request = new Message();
            request.initialize(new ByteArrayStashManager(),
                               ContentTypeHeader.parseValue(servletRequest.getContentType()),
                               servletRequest.getInputStream());
            request.attachHttpRequestKnob(new HttpServletRequestKnob(servletRequest));

            Message response = new Message();
            response.attachHttpResponseKnob(new HttpServletResponseKnob(servletResponse));

            context = new PolicyEnforcementContext(request, response);
            context.setAuditContext(auditContext);
            context.setSoapFaultManager(soapFaultManager);
            context.setClusterPropertyCache(clusterPropertyCache);
            boolean success = false;

            try {
                // pass over to the service
                PolicyService service = getPolicyService();

                try {
                    service.respondToPolicyDownloadRequest(context, true, normalPolicyGetter(true));
                }
                catch (IllegalStateException ise) { // throw by policy getter on policy not found
                    sendExceptionFault(context, ise, servletResponse);
                }

                // Ensure headers are written (e.g. invalid client cert header)
                HttpServletResponseKnob httpServletResponseKnob = (HttpServletResponseKnob) response.getHttpResponseKnob();
                httpServletResponseKnob.beginResponse();
                if ( httpServletResponseKnob.getStatus() == 0 ) {
                    servletResponse.setStatus(HttpConstants.STATUS_OK);
                }

                if (context.getPolicyResult() != AssertionStatus.NONE) {
                    returnFault(context, servletResponse);
                } else {
                    Document responseDoc;
                    try {
                        responseDoc = response.getXmlKnob().getDocumentReadOnly();
                    } catch (SAXException e) {
                        sendExceptionFault(context, e, servletResponse);
                        return;
                    }  catch (IllegalStateException e) {
                        sendExceptionFault(context, e, servletResponse);
                        return;
                    }
                    outputPolicyDoc(servletResponse, responseDoc);
                    success = true;
                }
            }
            finally {
                try {
                    String message = success ? "Policy Service: Success" : "Policy Service: Failed";
                    User user = getUser(context);
                    getApplicationContext().publishEvent(new PolicyServiceEvent(this, Level.INFO, servletRequest.getRemoteAddr(), message, user.getProviderId(), getName(user), user.getId()));
                }
                catch(Exception e) {
                    logger.log(Level.WARNING, "Error publishing event", e);
                }
            }
        } catch (Exception e) { // this is to avoid letting the servlet engine returning ugly html error pages.
            logger.log(Level.SEVERE, "Unexpected exception:", e);
            sendExceptionFault(context, e, servletResponse);
        }
        finally {
            ResourceUtils.closeQuietly(context);
        }
    }

    protected PolicyService getPolicyService() {
        return (PolicyService)getApplicationContext().getBean("policyService");
    }

    protected PolicyService.PolicyGetter normalPolicyGetter(final boolean inlineIncludes) {
        return new PolicyService.PolicyGetter() {
            public PolicyService.ServiceInfo getPolicy(String serviceId) {
                try {
                    final PublishedService targetService = resolveService(Long.parseLong(serviceId));
                    if (targetService == null) throw new IllegalStateException("Service not found ("+serviceId+")"); // caught by us in doGet and doPost

                    final Assertion servicePolicy = targetService.getPolicy().getAssertion();
                    final String servicePolicyVersion = policyCache.getUniquePolicyVersionIdentifer( targetService.getPolicy().getOid() );

                    return new PolicyService.ServiceInfo() {
                        // if not processing then initialize with the service policy, else we'll process it when required 
                        private Assertion policy = inlineIncludes ? null : servicePolicy;

                        public synchronized Assertion getPolicy() throws PolicyAssertionException {
                            // process if not already initialized
                            if (policy == null) {
                                try {
                                    policy = Policy.simplify(policyPathBuilder.inlineIncludes(servicePolicy, null));
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e); // Not possible on server side (hopefully)
                                }
                            }
                            return policy;
                        }

                        public String getVersion() {
                            return servicePolicyVersion;
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
    @Override
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

        // get credentials and check that they are valid for this policy
        AuthenticationResult[] results;
        try {
            results = authenticateRequestBasic(req);
        } catch (AuthenticationException e) {
            logger.log(Level.FINE, "Authentication exception: " + e.getMessage());
            results = new AuthenticationResult[0];
        } catch (LicenseException e) {
            logger.log(Level.WARNING, "Service is unlicensed, returning 500", e);
            returnError(res, "Gateway policy discovery service not enabled by license");
            return;
        } catch (TransportModule.ListenerException e) {
            logger.log(Level.WARNING, "Service is not permitted on this port, returning 500", e);
            returnError(res, "Gateway policy discovery service not enabled on this port");
            return;
        }

        boolean isFullDoc = false;
        if (fullDoc != null && fullDoc.length() > 0) {
            isFullDoc = "yes".equals(fullDoc) || "Yes".equals(fullDoc) || "YES".equals(fullDoc) || Boolean.parseBoolean(fullDoc);
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
        Document response;
        try {
            switch (results.length) {
                case 0:
                    response = service.respondToPolicyDownloadRequest(str_oid, null, this.normalPolicyGetter(!isFullDoc), isFullDoc);
                    break;
                case 1:
                    response = service.respondToPolicyDownloadRequest(str_oid, results[0].getUser(), this.normalPolicyGetter(!isFullDoc), isFullDoc);
                    break;
                default:
                    // todo use the best response (?)
                    throw new UnsupportedOperationException("not implemented");
            }
        } catch (FilteringException e) {
            logger.log(Level.WARNING, "Error in PolicyService", e);
            returnError(res, "internal error" + e.getMessage());
            return;
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Error in PolicyService", e);
            returnError(res, "internal error" + e.getMessage());
            return;
        } catch(IllegalStateException ise) { // invalid service
            returnError(res, "internal error" + ise.getMessage());
            return;
        } catch(UnsupportedOperationException uoe) {
            returnError(res, "internal error" + uoe.getMessage());
            return;
        } catch (PolicyAssertionException e) {
            returnError(res, "internal error" + e.getMessage());
            return;
        }

        if (response == null && (results == null || results.length < 1)) {
            logger.finest("sending challenge");
            sendAuthChallenge(req, res);
        } else if (response == null) {
            logger.info("this policy download is refused.");
            returnError(res, "Policy not found or download unauthorized");
        } else {
            logger.finest("returning policy");
            outputPolicyDoc(res, response);
        }
    }

    private boolean systemAllowsAnonymousDownloads(HttpServletRequest req) {
        // split strings into seperate values
        // check whether any of those can match start of
        String allPassthroughs = serverConfig.getPropertyCached("passthroughDownloads");
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
        //byte[] cert = DefaultKey.getInstance().readRootCert();
        String pemEncodedServerCertificateString = CertUtils.encodeAsPEM(serverCertificate);
        byte[] pemEncodedServerCertificate = pemEncodedServerCertificateString.getBytes("UTF-8");

        // Insert Cert-Check-NNN: headers if we can.
        if (username != null && nonce != null) {
            Collection<CertificateCheckInfo> checks = findCheckInfos(username, pemEncodedServerCertificate, nonce);
            for (CertificateCheckInfo info : checks) {
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

    private void outputPolicyDoc(HttpServletResponse res, Document doc) throws IOException {
        res.setContentType(XmlUtil.TEXT_XML);
        res.setStatus(HttpServletResponse.SC_OK);
        OutputStream os = res.getOutputStream();
        XmlUtil.nodeToOutputStream(doc,os);
        os.close();
    }

    /**
     * Given a username, find all matching users in every registered ID provider
     * and return the corresponding {@link com.l7tech.common.http.CertificateCheckInfo} instance.
     *
     * @param username The username to use
     * @return A collection of {@link CertificateCheckInfo} instances.
     * @throws com.l7tech.objectmodel.FindException
     *          if the ID Provider list could not be determined.
     */
    private Collection<CertificateCheckInfo> findCheckInfos(String username, byte[] certBytes, String nonce) throws FindException {
        ArrayList<CertificateCheckInfo> checkInfos = new ArrayList<CertificateCheckInfo>();
        final String trimmedUsername = username.trim();

        Collection<IdentityProvider> idps = identityProviderFactory.findAllIdentityProviders();
        for (IdentityProvider provider : idps) {
            try {
                User user = provider.getUserManager().findByLogin(trimmedUsername);
                if (user != null) {
                    String password = user instanceof InternalUser ? ((InternalUser)user).getHashedPassword() : null;
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
        if (car != null && !car.getAssertions(Category.ACCESS_CONTROL).isEmpty()) {
            checkInfos.add(new CertificateCheckInfo(Long.toString(Long.MAX_VALUE), SecureSpanConstants.NOPASS, null));
        }

        return checkInfos;
    }

    private void sendAuthChallenge(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        // send error back with a hint that credentials should be provided
        String newUrl = "https://" + httpServletRequest.getServerName();
        int httpPort = serverConfig.getIntPropertyCached(ServerConfig.PARAM_HTTPPORT, 8080, 10000L);
        int httpsPort = serverConfig.getIntPropertyCached(ServerConfig.PARAM_HTTPSPORT, 8443, 10000L);
        if (httpServletRequest.getServerPort() == httpPort || httpServletRequest.getServerPort() == httpsPort) {
            newUrl += ":" + httpsPort;
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
            user = context.getLastAuthenticatedUser();
        }

        if(user==null) {
            user = new UserBean();
        }

        return user;
    }

    private String getName(User user) {
        return user.getName()!=null ? user.getName() : user.getLogin();
    }

    private void returnFault(PolicyEnforcementContext context, HttpServletResponse hresp) throws IOException {
        OutputStream responseStream = null;
        String faultXml;
        try {
            responseStream = hresp.getOutputStream();
            hresp.setContentType(DEFAULT_CONTENT_TYPE);
            hresp.setStatus(500); // soap faults "MUST" be sent with status 500 per Basic profile

            SoapFaultLevel faultLevelInfo = context.getFaultlevel();
            faultXml = soapFaultManager.constructReturningFault(faultLevelInfo, context);
            responseStream.write(faultXml.getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private void returnError(HttpServletResponse hresp, String error) throws IOException {
        OutputStream responseStream = null;
        try {
            responseStream = hresp.getOutputStream();
            hresp.setContentType("text/plain");
            hresp.setStatus(500);
            responseStream.write(error.getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private void sendExceptionFault(PolicyEnforcementContext context, Throwable e,
                                    HttpServletResponse hresp) throws IOException {
        OutputStream responseStream = null;
        String faultXml;
        try {
            responseStream = hresp.getOutputStream();
            hresp.setContentType(DEFAULT_CONTENT_TYPE);
            hresp.setStatus(500); // soap faults "MUST" be sent with status 500 per Basic profile
            //context.setVariable("request.url", hreq.getRequestURL().toString());
            faultXml = soapFaultManager.constructExceptionFault(e, context);
            responseStream.write(faultXml.getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }
}

