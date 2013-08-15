package com.l7tech.server.policy;

import com.l7tech.common.http.CertificateCheck2Info;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.Pre60CertificateCheckInfo;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.AuthenticatableHttpServlet;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.event.system.PolicyServiceEvent;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultLevel;
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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.logging.Level;


/**
 * This servlet returns policy documents (type xml).
 * The following parameters can be passed to resolve the PublishedService:
 * serviceoid : the internal object identifier of the PublishedService. if specified, this parameter is sufficient to
 * retrieve the policy
 * <br/>
 * urn : the urn of the service. if more than one service have the same urn, at least one more parameter will be
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

    private static final String DUMMY_ID_PROVIDER_OID = Goid.toString(new Goid(0,Long.MAX_VALUE)); // Dummy ID provider OID for NOPASS headers

    private AuditContextFactory auditContextFactory;
    private SoapFaultManager soapFaultManager;
    private byte[] serverCertificate;
    private PolicyPathBuilder policyPathBuilder;
    private PolicyCache policyCache;
    private SecureRandom secureRandom;
    private String nodeId;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        try {
            ApplicationContext applicationContext = getApplicationContext();
            auditContextFactory = applicationContext.getBean("auditContextFactory", AuditContextFactory.class);
            soapFaultManager = applicationContext.getBean("soapFaultManager", SoapFaultManager.class);
            DefaultKey ku = applicationContext.getBean("defaultKey", DefaultKey.class);
            nodeId = applicationContext.getBean("clusterNodeId", String.class);
            serverCertificate = ku.getSslInfo().getCertificate().getEncoded();
            PolicyPathBuilderFactory pathBuilderFactory = applicationContext.getBean("policyPathBuilderFactory",PolicyPathBuilderFactory.class);
            policyPathBuilder = pathBuilderFactory.makePathBuilder();
            policyCache = applicationContext.getBean( "policyCache", PolicyCache.class );
            secureRandom = getBean("secureRandom", SecureRandom.class);
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
    protected void doPost(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse)
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
                               servletRequest.getInputStream(),
                               Message.getMaxBytes());
            request.attachHttpRequestKnob(new HttpServletRequestKnob(servletRequest));

            final Message response = new Message();
            response.attachHttpResponseKnob(new HttpServletResponseKnob(servletResponse));

            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);
            final boolean[] success = {false};

            final PolicyEnforcementContext finalContext = context;
            final SystemAuditRecord record = new SystemAuditRecord(Level.INFO, nodeId, Component.GW_POLICY_SERVICE, "Policy request", false, null , null, null, "GET", servletRequest.getRemoteAddr());
            auditContextFactory.doWithNewAuditContext(record, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        // pass over to the service
                        PolicyService service = getPolicyService();

                        try {
                            boolean allowDisabled = systemAllowsDisabledServiceDownloads(servletRequest);
                            service.respondToPolicyDownloadRequest(finalContext, true, normalPolicyGetter(true, allowDisabled, false));
                        }
                        catch (IllegalStateException ise) { // throw by policy getter on policy not found
                            sendExceptionFault(finalContext, ise, servletResponse);
                        }

                        // Ensure headers are written (e.g. invalid client cert header)
                        HttpServletResponseKnob httpServletResponseKnob = (HttpServletResponseKnob) response.getHttpResponseKnob();
                        httpServletResponseKnob.beginResponse();
                        if ( httpServletResponseKnob.getStatus() == 0 ) {
                            servletResponse.setStatus(HttpConstants.STATUS_OK);
                        }

                        if (finalContext.getPolicyResult() != AssertionStatus.NONE) {
                            returnFault(finalContext, servletResponse);
                        } else {
                            Document responseDoc;
                            try {
                                responseDoc = response.getXmlKnob().getDocumentReadOnly();
                            } catch (SAXException e) {
                                sendExceptionFault(finalContext, e, servletResponse);
                                return null;
                            }  catch (IllegalStateException e) {
                                sendExceptionFault(finalContext, e, servletResponse);
                                return null;
                            }
                            outputPolicyDoc(servletResponse, responseDoc);
                            success[0] = true;
                        }
                    }
                    finally {
                        try {
                            String message = success[0] ? "Policy Service: Success" : "Policy Service: Failed";
                            User user = getUser(finalContext);
                            getApplicationContext().publishEvent(new PolicyServiceEvent(this, Level.INFO, servletRequest.getRemoteAddr(), message, user.getProviderId(), getName(user), user.getId()));
                        }
                        catch(Exception e) {
                            logger.log(Level.WARNING, "Error publishing event", e);
                        }
                    }
                    return null;
                }
            });

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

    protected PolicyService.PolicyGetter normalPolicyGetter(final boolean inlineIncludes, final boolean allowDisabled, final boolean includeComments) {
        return new PolicyService.PolicyGetter() {
            @Override
            public PolicyService.ServiceInfo getPolicy(String serviceId) {
                try {
                    Goid serviceGoid = null;
                    Long serviceOid = null;
                    try {
                        serviceGoid = Goid.parseGoid(serviceId);
                    } catch (IllegalArgumentException e) {
                        try {
                            serviceOid = Long.parseLong(serviceId);
                        } catch (NumberFormatException e2) {
                            logger.log(Level.INFO, "cannot parse service id: " + serviceId);
                            throw new IllegalStateException("Service not found (" + serviceId + ")"); // caught by us in doGet and doPost
                        }
                    }
                    final PublishedService targetService = serviceGoid != null ? resolveService(serviceGoid) : resolveService(serviceOid);
                    if (targetService == null || (targetService.isDisabled() && !allowDisabled)) 
                        throw new IllegalStateException("Service not found ("+serviceId+")"); // caught by us in doGet and doPost

                    final Assertion servicePolicy = new Policy(targetService.getPolicy()).getAssertion(); // copy policy since we may be modifying it (simplification, filters, etc)
                    if (servicePolicy == null)
                        return null;
                    final String servicePolicyVersion = policyCache.getUniquePolicyVersionIdentifer( targetService.getPolicy().getGoid() );

                    return new PolicyService.ServiceInfo() {
                        // if not processing then initialize with the service policy, else we'll process it when required
                        private Assertion policy = inlineIncludes ? null : Policy.simplify(servicePolicy, includeComments);

                        @Override
                        public synchronized Assertion getPolicy() throws PolicyAssertionException {
                            // process if not already initialized
                            if (policy == null) {
                                try {
                                    policy = Policy.simplify(policyPathBuilder.inlineIncludes(servicePolicy, null, false), includeComments);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e); // Not possible on server side (hopefully)
                                }
                            }
                            return policy;
                        }

                        @Override
                        public String getVersion() {
                            return servicePolicyVersion;
                        }
                    };
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "cannot parse policy", e);
                    return null;
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
        String inline = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_INLINE);
        String includeComments = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_INCLUDE_COMMENTS);

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
        } catch (ListenerException e) {
            logger.log(Level.WARNING, "Service is not permitted on this port, returning 500", e);
            returnError(res, "Gateway policy discovery service not enabled on this port");
            return;
        }

        boolean isFullDoc = false;
        if (fullDoc != null && fullDoc.length() > 0) {
            isFullDoc = "yes".equalsIgnoreCase(fullDoc) || Boolean.parseBoolean(fullDoc);
            logger.finest("Passed value for " + SecureSpanConstants.HttpQueryParameters.PARAM_FULLDOC + " was " + fullDoc);
            if (isFullDoc)
                logger.finest("Will passthrough and return full policy document");
        }

        boolean isIncludeComments = false;
        if (includeComments != null && includeComments.length() > 0) {
            isIncludeComments = "yes".equalsIgnoreCase(includeComments) || Boolean.parseBoolean(fullDoc);
            logger.finest("Passed value for " + SecureSpanConstants.HttpQueryParameters.PARAM_INCLUDE_COMMENTS + " was " + includeComments);
            if (isIncludeComments)
                logger.finest("Will include comments with full policy document");
        }

        //check for inline setting
        boolean isInline = false;
        if (inline != null && inline.length() > 0) {
            isInline = "yes".equalsIgnoreCase(inline) || Boolean.parseBoolean(inline);
            logger.finest("Passed value for " + SecureSpanConstants.HttpQueryParameters.PARAM_INLINE + " was " + inline);
            if (isInline)
                logger.finest("Will passthrough and return inlined policy document");
        }

        // if user asking for full doc, or non-inlined, make sure it's allowed
        if ( !systemAllowsAnonymousDownloads(req) ) {
            isFullDoc = false;
        }
        if ( !isFullDoc ) {
            isInline = true; // inline option only available when full doc 
        }

        boolean allowDisabled = systemAllowsDisabledServiceDownloads(req);
        // pass over to the service
        PolicyService service = getPolicyService();
        Document response;
        try {
            switch (results.length) {
                case 0:
                    response = service.respondToPolicyDownloadRequest(str_oid, null, null, this.normalPolicyGetter(isInline, allowDisabled, isIncludeComments && isFullDoc), isFullDoc);
                    break;
                case 1:
                    response = service.respondToPolicyDownloadRequest(str_oid, null, results[0].getUser(), this.normalPolicyGetter(isInline, allowDisabled, isIncludeComments && isFullDoc), isFullDoc);
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
            returnError(res, "Error: " + ise.getMessage());
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
        String allPassthroughs = config.getProperty( "passthroughDownloads" );
        StringTokenizer st = new StringTokenizer(allPassthroughs);
        String remote = req.getRemoteAddr();
        while (st.hasMoreTokens()) {
            String passthroughVal = st.nextToken();
            if (InetAddressUtil.patternMatchesAddress(passthroughVal, InetAddressUtil.getAddress(remote))) {
                logger.fine("remote ip " + remote + " was authorized by passthrough value " + passthroughVal);
                return true;
            }
        }
        logger.finest("remote ip " + remote + " was not authorized by any passthrough in " + allPassthroughs);
        return false;
    }

    private boolean systemAllowsDisabledServiceDownloads(final HttpServletRequest req) {
        boolean permitted = false;

        String disabledDownloads = config.getProperty( "service.disabledServiceDownloads" );
        if ( "all".equalsIgnoreCase( disabledDownloads ) ) {
            permitted = true;
        } else if ( "passthrough".equalsIgnoreCase(disabledDownloads)) {
            permitted = systemAllowsAnonymousDownloads( req );
        }

        return permitted;
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
        byte[] pemEncodedServerCertificate = pemEncodedServerCertificateString.getBytes(Charsets.UTF8);

        // Insert Cert-Check-NNN: headers if we can.
        boolean certificateDiscoveryEnabled = ConfigFactory.getBooleanProperty( ServerConfigParams.PARAM_CERTIFICATE_DISCOVERY_ENABLED, false );
        if (certificateDiscoveryEnabled && username != null && nonce != null) {
            Collection<HttpHeader> checkInfoHeaders = findCheckInfoHeaders(username, pemEncodedServerCertificate, nonce);
            for (HttpHeader header : checkInfoHeaders) {
                response.addHeader(header.getName(), header.getFullValue());
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
     * and return the corresponding {@link com.l7tech.common.http.Pre60CertificateCheckInfo} instance.
     *
     * @param username The username to use to look up the hashed password.  Required.
     * @param certBytes  encoded certificate bytes that the client will be verifying.   Required.
     * @param clientNonce  the nonce value provided by the client.  Required.
     * @return A collection of HTTP headers to add to the response, where each header is a check or check2 header.  May be empty.
     * @throws com.l7tech.objectmodel.FindException
     *          if the ID Provider list could not be determined.
     */
    private Collection<HttpHeader> findCheckInfoHeaders(String username, byte[] certBytes, String clientNonce) throws FindException {
        List<HttpHeader> ret = new ArrayList<HttpHeader>();
        final String trimmedUsername = username.trim();
        final byte[] clientNonceBytes = clientNonce.getBytes(Charsets.UTF8);

        boolean sawUncheckableUser = false;
        Collection<IdentityProvider> idps = identityProviderFactory.findAllIdentityProviders();
        for (IdentityProvider provider : idps) {
            try {
                User user = provider.getUserManager().findByLogin(trimmedUsername);
                if (user != null) {
                    boolean addedHeader = false;
                    String hashedPassword = user instanceof InternalUser ? ((InternalUser)user).getHashedPassword() : null;
                    if (hashedPassword != null && passwordHasher.isVerifierRecognized(hashedPassword)) {
                        final byte[] userSaltBytes = passwordHasher.extractSaltFromVerifier(hashedPassword);
                        if (userSaltBytes != null) {
                            String userSaltHex = HexUtils.hexDump(userSaltBytes);
                            String oid = Goid.toString(provider.getConfig().getGoid());

                            byte[] serverNonceBytes = new byte[16];
                            secureRandom.nextBytes(serverNonceBytes);
                            String serverNonceHex = HexUtils.hexDump(serverNonceBytes);

                            byte[] checkHashBytes = CertUtils.getVerifierBytes(hashedPassword.getBytes(Charsets.UTF8), clientNonceBytes, serverNonceBytes, certBytes);
                            String checkHashHex = HexUtils.hexDump(checkHashBytes);

                            ret.add(new CertificateCheck2Info(oid, serverNonceHex, checkHashHex, userSaltHex).asHttpHeader());
                            addedHeader = true;
                        }
                    }
                    if (!addedHeader) {
                        sawUncheckableUser = true;
                    }
                }
            } catch (FindException e) {
                // Log it and continue
                //noinspection ThrowableResultOfMethodCallIgnored
                logger.log(Level.WARNING, "Unable to to retrieve user information: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (NoSuchAlgorithmException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                logger.log(Level.WARNING, "Unable to to compute cert verifier: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
        // we smelt something (maybe netegrity?)
        CustomAssertionsRegistrar car = (CustomAssertionsRegistrar)getApplicationContext().getBean("customAssertionRegistrar");
        if (sawUncheckableUser || car != null && car.hasCustomCredentialSource()) {
            ret.add(new CertificateCheck2Info(DUMMY_ID_PROVIDER_OID, "", SecureSpanConstants.NOPASS, "").asHttpHeader());
        }

        if (!ret.isEmpty()) {
            // Add backward compat NOPASS for older XVCs
            ret.add(new Pre60CertificateCheckInfo(DUMMY_ID_PROVIDER_OID, SecureSpanConstants.NOPASS, "dummy").asHttpHeader());
        }

        return ret;
    }

    private void sendAuthChallenge(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        // send error back with a hint that credentials should be provided
        String newUrl = "https://" + InetAddressUtil.getHostForUrl(httpServletRequest.getServerName());
        int httpPort = config.getIntProperty( ServerConfigParams.PARAM_HTTPPORT, 8080 );
        int httpsPort = config.getIntProperty( ServerConfigParams.PARAM_HTTPSPORT, 8443 );
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

        if(context.getDefaultAuthenticationContext().isAuthenticated()) {
            user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
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
        try {
            responseStream = hresp.getOutputStream();
            hresp.setStatus(500); // soap faults "MUST" be sent with status 500 per Basic profile

            final SoapFaultLevel faultLevelInfo = context.getFaultlevel();
            final SoapFaultManager.FaultResponse fault = soapFaultManager.constructReturningFault(faultLevelInfo, context);
            soapFaultManager.sendExtraHeaders(fault, hresp);
            hresp.setContentType(fault.getContentType().getFullValue());
            responseStream.write(fault.getContentBytes());
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
        try {
            final SoapFaultManager.FaultResponse faultInfo = soapFaultManager.constructExceptionFault(e, null, context);
            soapFaultManager.sendExtraHeaders(faultInfo, hresp);
            responseStream = hresp.getOutputStream();
            hresp.setContentType(faultInfo.getContentType().getFullValue());
            hresp.setStatus(500); // soap faults "MUST" be sent with status 500 per Basic profile
            responseStream.write(faultInfo.getContentBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }
}

