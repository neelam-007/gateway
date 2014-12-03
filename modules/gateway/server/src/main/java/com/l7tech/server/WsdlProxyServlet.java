package com.l7tech.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.IssuedCertNotPresentedException;
import com.l7tech.identity.MissingCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.xmlsec.WssVersionAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.wssp.WsspWriter;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.policy.filter.FilterManager;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.server.policy.filter.IdentityRule;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.service.resolution.SoapActionResolver;
import com.l7tech.server.service.resolution.UrnResolver;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.util.*;
import com.l7tech.wsdl.WsdlUtil;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/**
 * Provides access to WSDL for published services of this SSG.
 * <p/>
 * When calling this without specifying a reference to a service, this servlet
 * will return a WSIL document containing URLs to actual WSDL resources.
 * <p/>
 * When providing a reference to a service, this servlet will return an actual WSDL
 * document. This document is based on the WSDL of the protected service. This base
 * document is filtered so that the service endpoints point to this ssg's MessageProcessor
 * URL.
 * <p/>
 * Requests to this servlet can provide credentials or not. If valid credentials are
 * provided, the requestor will receive service information based on what his credentials
 * allow him to consume at run time. Anonymous requestors will only see service descriptions
 * for published services that allow anonymous access on this ssg.
 * <p/>
 * Authenticated requests must secured.
 * <p/>
 * For URL pattern, consult web.xml
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Sep 15, 2003<br/>
 */
public class WsdlProxyServlet extends AuthenticatableHttpServlet {
    private static final String PARAM_ANONYMOUS = "anon";
    private static final String PROPERTY_WSSP_ATTACH = "com.l7tech.server.wssp";
    private static final String PROPERTY_CLEARTEXT = "wsdlProxy.allowInsecureHttpBasic";
    private static final String PROPERTY_WSDL_IMPORT_PROXY = "wsdlImportProxyEnabled";

    private FilterManager wsspFilterManager;
    private FilterManager clientPolicyFilterManager;
    private SoapActionResolver sactionResolver;
    private UrnResolver nsResolver;
    private ServiceDocumentManager serviceDocumentManager;
    private PolicyPathBuilder policyPathBuilder;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        WebApplicationContext appcontext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        clientPolicyFilterManager = appcontext.getBean("policyFilterManager", FilterManager.class);
        wsspFilterManager = appcontext.getBean("wsspolicyFilterManager", FilterManager.class);
        serviceDocumentManager = appcontext.getBean("serviceDocumentManager", ServiceDocumentManager.class);
        AuditFactory auditorFactory = appcontext.getBean("auditFactory", AuditFactory.class);
        PolicyPathBuilderFactory pathBuilderFactory = appcontext.getBean("policyPathBuilderFactory", PolicyPathBuilderFactory.class);
        policyPathBuilder = pathBuilderFactory.makePathBuilder();

        sactionResolver = new SoapActionResolver(auditorFactory);
        nsResolver = new UrnResolver(auditorFactory);
    }

    @Override
    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_WSDLPROXY;
    }

    @Override
    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.WSDLPROXY;
    }

    @Override
    protected boolean isCleartextAllowed() {
        return config.getBooleanProperty( PROPERTY_CLEARTEXT, false );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PublishedService ps;
        try {
            ps = getRequestedService(req);
        } catch (FindException e) {
            // if they ask for an invalid services WSDL return 404 since that WSDL doc does not exist
            logger.log(Level.INFO, "Invalid service requested (" + e.getMessage() + ")", ExceptionUtils.getDebugException(e));
            sendBackError(res, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        } catch (AmbiguousServiceException e) {
            logger.log(Level.INFO, "Service request ambiguous: "  + ExceptionUtils.getMessage( e ) );
            sendBackError(res, HttpServletResponse.SC_MULTIPLE_CHOICES, e.getMessage());
            return;
        }

        // let's see if we can get credentials...
        AuthenticationResult[] results = null;
        if ( credentialsPermittedForTransport( req ) ) try { // Don't check for credentials over HTTP if we'll ignore them, see bug 11701
            if (ps != null) {
                results = authenticateRequestBasic(req, ps);
            } else {
                results = authenticateRequestBasic(req);
            }
        } catch (BadCredentialsException | IssuedCertNotPresentedException e) {
            // Authentication failed (bug #4338)
            logger.log(Level.INFO, "WSDL proxy request authentication failed: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.getOutputStream().print("Authentication failed");
            res.flushBuffer();
            return;
        } catch (MissingCredentialsException e) {
            logger.log(Level.INFO, "Credentials do not authenticate against any of the providers, assuming anonymous");
            results = null;
            /* FALLTHROUGH and handle anonymous download */
        } catch ( LicenseException e) {
            logger.log(Level.WARNING, "Service is unlicensed, returning 500", e);
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.getOutputStream().print("Gateway WSDL proxy service not enabled by license");
            res.flushBuffer();
            return;
        } catch (ListenerException e) {
            logger.log(Level.WARNING, "Service not permitted on this port, returning 500", e);
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.getOutputStream().print("Gateway WSDL proxy service not permitted on this port");
            res.flushBuffer();
            return;
        }

        // NOTE: By default sending credentials over insecure channel is treated as an anonymous request
        // (i dont care if you send me credentials in non secure manner, that is your problem
        // however, i will not send sensitive information unless the channel is secure)
        if ( results != null && results.length > 0 && credentialsPermittedForTransport( req ) ) {
            doAuthenticated(req, res, ps, results);
        } else {
            if ( credentialsPermittedForTransport( req ) && req.getParameter(PARAM_ANONYMOUS)!=null && !Boolean.valueOf(req.getParameter(PARAM_ANONYMOUS)) ) {
                doHttpBasicChallenge( res );
            } else {
                doAnonymous(req, res, ps);
            }
        }
    }

    private boolean credentialsPermittedForTransport( final HttpServletRequest req ) {
        return req.isSecure() || isCleartextAllowed();
    }

    class AmbiguousServiceException extends Exception {
        AmbiguousServiceException(String s) {
            super(s);
        }
    }

    /**
     * HTTP GET request can contain parameters that tells us which service is desired. 'serviceoid', 'uri', 'ns',
     * 'soapaction' or a combination of the later 3.
     *
     * @param req the http get that contains the parameters that hint towards which service is wanted
     * @return either the requested service or null if no service appear to be requested
     * @throws AmbiguousServiceException thrown if the parameters resolve more than one possible service
     * @throws com.l7tech.objectmodel.FindException when nothing resolves
     */
    private PublishedService getRequestedService(HttpServletRequest req) throws AmbiguousServiceException, FindException {
        final boolean permitDisabledService = systemAllowsDisabledServiceDownloads(req);

        // resolution using traditional serviceoid param
        String serviceStr = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID);
        if (serviceStr != null) {
            PublishedService ps;
            Goid serviceId = null;
            Long serviceOid = null;
            try {
                serviceId = Goid.parseGoid(serviceStr);
            } catch (IllegalArgumentException e) {
                try {
                    serviceOid = Long.parseLong(serviceStr);
                } catch (NumberFormatException e2) {
                    throw new FindException("cannot parse service id from " + serviceStr, e);
                }
            }
            ps = serviceId != null ? resolveService(serviceId) : resolveService(serviceOid);
            if (ps == null || (ps.isDisabled() && !permitDisabledService)) {
                throw new FindException("Service id " + serviceStr + " did not resolve any service.");
            }
            return ps;
        }

        // resolution using alternative parameters
        String uriparam = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_URI);
        String nsparam = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_NS);
        String sactionparam = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_SACTION);
        if (uriparam == null && nsparam == null && sactionparam == null) {
            // no service seems to be requested
            return null;
        }

        Collection<PublishedService> services;
        try {
            services = serviceCache.resolve( uriparam, sactionparam, nsparam );
        } catch ( ServiceResolutionException e ) {
            throw new FindException("Error resolving service.", e);
        }
        services = removeDisabledServices( services );

        if (services.size() == 1) {
            return services.iterator().next();
        } else if ( services.isEmpty() ) {
            final StringBuilder builder = new StringBuilder();
            builder.append( "Parameters " );
            if ( uriparam != null ) builder.append( "uri='" ).append( uriparam ).append( "', " );
            if ( nsparam != null ) builder.append( "ns='" ).append( nsparam ).append( "', " );
            if ( sactionparam != null ) builder.append( "soapaction='" ).append( sactionparam ).append( "', " );
            builder.setLength( builder.length()-2 ); // trim last ", "
            builder.append( " did not resolve any service." );
            throw new FindException( builder.toString() );
        }

        // could not narrow it down enough -> throw AmbiguousServiceException
        final StringBuilder names = new StringBuilder();
        for (PublishedService service : services) {
            names.append(service.getName()).append(" ");
        }
        logger.info("service query too wide: " + names.toString());
        throw new AmbiguousServiceException("Too many services fit the mold: " + names.toString());
    }

    private Collection<PublishedService> removeDisabledServices( final Collection<PublishedService> services ) {
        List<PublishedService> filteredServices = new ArrayList<>();

        for ( PublishedService service : services ) {
            if ( !service.isDisabled() ) {
                filteredServices.add( service );
            }
        }

        return filteredServices;
    }

    private void doAnonymous(HttpServletRequest req, HttpServletResponse res, PublishedService ps) throws IOException {
        doAuthenticated(req, res, ps, null);
    }

    private void doAuthenticated(HttpServletRequest req,
                                 HttpServletResponse res,
                                 PublishedService svc,
                                 AuthenticationResult[] results)
            throws IOException
    {
        // HANDLE REQUEST FOR ONE SERVICE DESCRIPTION
        if (svc != null) {
            // check svc type
            if (!svc.isSoap()) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "service has no wsdl");
                return;
            }
            // make sure this service is indeed anonymously accessible
            //noinspection StatementWithEmptyBody
            if (systemAllowsAnonymousDownloads(req)) {
            } else if (results == null || results.length < 1) {
                if (!policyAllowAnonymous(svc.getPolicy())) {
                    logger.info("user denied access to service description");
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "you need to provide valid credentials to see this service's description");
                    return;
                }
            } else { // otherwise make sure the requestor is authorized for this policy
                boolean ok = false;
                if (!policyAllowAnonymous(svc.getPolicy())) {
                    User requestor = null;
                    for (AuthenticationResult result : results) {
                        requestor = result.getUser();
                        if (userCanSeeThisService(requestor, svc)) ok = true;
                    }
                    if (!ok) {
                        logger.info("user denied access to service description " + requestor + " " + svc.getPolicy().getXml());
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "these credentials do not grant you access to this service description.");
                        return;
                    }
                }
            }
            outputServiceDescription(req, res, svc, results);
            logger.info("Returned description for service, " + svc.getGoid());
        } else { // HANDLE REQUEST FOR LIST OF SERVICES
            ListResults listres;

            try {
                if (systemAllowsAnonymousDownloads(req)) {
                    listres = listAllServices();
                } else if (results == null || results.length < 1)
                    listres = listAnonymouslyViewableServices();
                else
                    listres = listAnonymouslyViewableAndProtectedServices(results);
            } catch (FindException e) {
                logger.log(Level.SEVERE, "cannot list services", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
            // Is there anything to show?
            Collection<PublishedService> services = listres.allowed();
            if (services.size() < 1) {
                if ((results == null || results.length < 1) && listres.anyServices() && credentialsPermittedForTransport( req )) {
                    sendAuthChallenge(res);
                } else {
                    //res.sendError(HttpServletResponse.SC_NOT_FOUND, "no services or not authorized");
                    // return empty wsil
                    String emptywsil = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<?xml-stylesheet type=\"text/xsl\" href=\"" + styleURL() + "\"?>\n" +
                      "<inspection xmlns=\"http://schemas.xmlsoap.org/ws/2001/10/inspection/\" />\n";
                    res.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
                    res.getOutputStream().println(emptywsil);
                }
                return;
            }
            outputServiceDescriptions(req, res, services);
            logger.info("Returned list of service description targets");
        }
    }

    private boolean systemAllowsAnonymousDownloads(final HttpServletRequest req) {
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

    private void sendAuthChallenge(HttpServletResponse res) throws IOException {
        // send error back with a hint that credentials should be provided
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        logger.fine("sending back authentication challenge");
        // in this case, send an authentication challenge
        //res.setHeader("WWW-Authenticate", "Basic realm=\"" + ServerHttpBasic.REALM + "\"");
        res.setHeader("WWW-Authenticate", "Basic realm=\"\"");
        res.getOutputStream().close();
    }

    private String styleURL() {
        return "/ssg/wsil2xhtml.xml";
    }

    private void rewriteReferences(final PublishedService service,
                                   final Document wsdlDoc,
                                   final Collection<ServiceDocument> documents,
                                   final String wsdlProxyUri,
                                   final Functions.UnaryVoid<Exception> errorHandler) {
        final HashMap<String, Pair<String,String>> dependencies = new HashMap<>();

        for (ServiceDocument document : documents) {
            dependencies.put(document.getUri(), new Pair<>(document.getId(), document.getContents()));
        }

        WsdlUtil.rewriteReferences(service.getId(), service.getWsdlUrl(),
                wsdlDoc, dependencies, wsdlProxyUri, errorHandler);
    }

    private void addSecurityPolicy(Document wsdl, PublishedService svc) {
        if (!config.getBooleanProperty( PROPERTY_WSSP_ATTACH, true )) {
            logger.fine("WS-SecurityPolicy decoration not enabled.");
            return;
        }

        try{
            Assertion assertion = policyPathBuilder.inlineIncludes(wspReader.parsePermissively(svc.getPolicy().getXml(), WspReader.OMIT_DISABLED), null, false);
            if (assertion == null)
                return;
            Assertion rootassertion = Policy.simplify(assertion, false);

            //we should ignore wssp assertion if the wssp assertion is disabled
            WsspAssertion wsspAssertion = Assertion.find(rootassertion, WsspAssertion.class, true);
            if (wsspAssertion == null) {
                logger.fine("No WSSP Assertion in policy, not adding policy to WSDL.");
                return;
            }

            // remove any existing policy
            XmlUtil.stripNamespace(wsdl.getDocumentElement(), SoapConstants.WSP_NAMESPACE2);

            boolean wss11 = Assertion.contains(rootassertion, WssVersionAssertion.class);
            Assertion effectivePolicy = wsspFilterManager.applyAllFilters(null, rootassertion);
            if (effectivePolicy != null) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "Effective policy for user: \n" + WspWriter.getPolicyXml(effectivePolicy));
                    }

                    WsspWriter.decorate(wsdl,
                            effectivePolicy,
                            wss11,
                            wsspAssertion.getBasePolicyXml(),
                            wsspAssertion.getInputPolicyXml(),
                            wsspAssertion.getOutputPolicyXml());
            }
            else {
                logger.info("No policy to add!");
            }

        } catch (InterruptedException | PolicyAssertionException | IOException | FilteringException | SAXException e) {
            logger.log(Level.WARNING, "Could not add policy to WSDL: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch(Exception e) {
            logger.log(Level.WARNING, "Could not add policy to WSDL: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private boolean proxyRequired( PublishedService service )  {
        boolean required = false;

        if ( service.getWsdlUrl() != null && service.getWsdlUrl().startsWith("file:") ) {
            required = true;
        }

        return required;
    }

    private void outputServiceDescription(final HttpServletRequest req,
                                          final HttpServletResponse res,
                                          final PublishedService svc,
                                          final AuthenticationResult[] results) throws IOException {
        final boolean enableImportProxy = svc.isInternal() || proxyRequired(svc) || config.getBooleanProperty(PROPERTY_WSDL_IMPORT_PROXY, false);
        final Collection<ServiceDocument> documents;
        Goid serviceId = null;
        Document wsdlDoc = null;
        try {
            documents = enableImportProxy ?
                    serviceDocumentManager.findByServiceIdAndType(svc.getGoid(), "WSDL-IMPORT") :
                    Collections.<ServiceDocument>emptyList();

            String documentOid = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEDOCOID);
            if ( documentOid != null ) {
                for ( ServiceDocument document : documents ) {
                    if ( documentOid.equals(document.getId()) ) {
                        wsdlDoc =  parse(document.getUri(), document.getContents());
                    }
                }

                if ( wsdlDoc == null ) {
                    logger.log(Level.WARNING, "Cannot find imported document with oid '"+documentOid+"' for service '"+svc.getGoid()+"'.");
                    res.sendError(HttpServletResponse.SC_NOT_FOUND, "service has no wsdl");
                    return;
                }
            } else {
                serviceId = svc.getGoid();
                wsdlDoc = parse(svc.getWsdlUrl(), svc.getWsdlXml());
            }
        } catch (SAXException | FindException e) {
            logger.log(Level.WARNING, "cannot parse wsdl", e);
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "service has no wsdl");
            return;
        }

        // change url of the wsdl
        // direct to http by default. choose http port based on port used for this request.
        final boolean secureRequest = req.isSecure();
        int port = req.getServerPort();
        String proto = secureRequest ? "https" : "http";

        int httpPort = config.getIntProperty( ServerConfigParams.PARAM_HTTPPORT, 8080 );
        int httpsPort = config.getIntProperty( ServerConfigParams.PARAM_HTTPSPORT, 8443 );
        if (("http".equals(proto) && (port==80 || port==httpPort)) ||
            ("https".equals(proto) && (port==443 || port==httpsPort))) {
            // then see if we should switch protocols for the endpoint
            User user = null;
            if (results != null && results.length == 1) {
                user = results[0].getUser();
            }

            if (results != null && results.length > 1) {
                logger.warning("Cannot determine if HTTPS is required for effective policy (multiple users).");
            }

            try {
                Assertion rootAssertion = parsePolicy(svc.getPolicy().getXml());
                Assertion effectivePolicy = clientPolicyFilterManager.applyAllFilters(user, rootAssertion);
                SslAssertion sslAssertion = (SslAssertion) getFirstChild(effectivePolicy, SslAssertion.class);
                if (!secureRequest && sslAssertion != null && sslAssertion.getOption()==SslAssertion.REQUIRED) {
                    port = port==80 ? 443 : httpsPort;
                    proto = "https";
                }
                else if(secureRequest && sslAssertion != null && sslAssertion.getOption()==SslAssertion.FORBIDDEN) {
                    port = port==443 ? 80 : httpPort;
                    proto = "http";
                }
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Could not determine if HTTP/HTTPS is required for endpoint.", e);
            }
        }

        String portStr = "";
        if (!("https".equals(proto) && port == 443) &&
            !("http".equals(proto) && port == 80)) {
            portStr = ":" + port;
        }

        URL ssgurl;
        String routinguri = svc.getRoutingUri();
        if (routinguri == null || routinguri.length() < 1) {
            ssgurl = new URL(proto + "://" + InetAddressUtil.getHostForUrl(req.getServerName()) +
                             portStr + SecureSpanConstants.SERVICE_FILE +
                             Goid.toString(svc.getGoid()));
        } else {
            ssgurl = new URL(proto + "://" + InetAddressUtil.getHostForUrl(req.getServerName()) +
                             portStr + routinguri);
        }

        String wsdlProxyUrl = req.getServletPath();  // use servlet path to skip any import name
        try {
            wsdlProxyUrl = new URI(req.getRequestURL().toString()).resolve(req.getServletPath()).toString();
        } catch ( Exception e ) {
            logger.warning("Unable to determine absolute URL for wsdl proxy '"+ExceptionUtils.getMessage(e)+"'.");            
        }

        rewriteReferences(svc, wsdlDoc, documents, wsdlProxyUrl, new Functions.UnaryVoid<Exception>() {
            @Override
            public void call(Exception e) {
                logger.log(Level.WARNING, "Error rewriting WSDL url for service '" + svc.getId() + "'..", e);
            }
        });

        WsdlUtil.addOrUpdateEndpoints(wsdlDoc, ssgurl, null == serviceId ? null : serviceId.toString());
        addSecurityPolicy(wsdlDoc, svc);

        // output the wsdl
        res.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
        try {
            XmlUtil.nodeToOutputStream(wsdlDoc, res.getOutputStream());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error outputing wsdl", e);
            throw new IOException(e.getMessage());
        }
    }

    private Document parse( final String uri, final String content ) throws IOException, SAXException {
        InputSource input = new InputSource();
        input.setSystemId( uri );
        input.setCharacterStream( new StringReader(content) );
        return XmlUtil.parse( input, false );
    }

    private void outputServiceDescriptions(HttpServletRequest req, HttpServletResponse res, Collection<PublishedService> services) throws IOException {
        String uri = req.getRequestURI();
        uri = uri.replaceAll("wsil", "wsdl");
        String output = createWSILDoc(req, services, req.getServerName(), req.getServerPort(), uri);
        res.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
        ServletOutputStream os = res.getOutputStream();
        os.print(output);
        os.close();
    }

    interface ListResults {
        Collection<PublishedService> allowed();
        boolean anyServices();
    }

    private ListResults listAllServices() throws FindException {
        final Collection<PublishedService> allServices = Functions.grep(serviceCache.getCachedServices(), new Functions.Unary<Boolean, PublishedService>() {
            @Override
            public Boolean call(PublishedService publishedService) {
                return !publishedService.isDisabled();
            }
        });
        return new ListResults() {
            @Override
            public Collection<PublishedService> allowed() {
                return allServices;
            }
            @Override
            public boolean anyServices() {
                return allServices.size() >= 1;
            }
        };
    }

    private ListResults listAnonymouslyViewableServices() throws IOException, FindException {
        return listAnonymouslyViewableAndProtectedServices(null);
    }

    private ListResults listAnonymouslyViewableAndProtectedServices(AuthenticationResult[] results)
            throws IOException, FindException
    {
        // get all services
        final Collection<PublishedService> allServices = serviceCache.getCachedServices();

        // prepare output collection
        final Collection<PublishedService> output = new ArrayList<>();

        // decide which ones make the cut
        for (PublishedService svc : allServices) {
            if ( svc.isDisabled() ) {
                continue;
            }

            if (policyAllowAnonymous(svc.getPolicy())) {
                output.add(svc);
            } else if (results != null) {
                for (AuthenticationResult result : results) {
                    User requestor = result.getUser();
                    if (userCanSeeThisService(requestor, svc))
                        output.add(svc);
                }
            }
        }

        return new ListResults() {
            @Override
            public Collection<PublishedService> allowed() {
                return output;
            }
            @Override
            public boolean anyServices() {
                return allServices.size() >= 1;
            }
        };
    }

    private boolean userCanSeeThisService(User requestor, PublishedService svc) throws IOException {
        // start at the top
        Assertion rootassertion;
        rootassertion = parsePolicy(svc.getPolicy().getXml());
        return checkForIdPotential(rootassertion, requestor);
    }

    /**
     * returns true if at least one of the id assertions in this tree can be met by the requestor
     */
    private boolean checkForIdPotential(Assertion assertion, User requestor) {
        if (assertion instanceof IdentityAssertion) {
            try {
                if (IdentityRule.canUserPassIDAssertion((IdentityAssertion)assertion, requestor, identityProviderFactory)) {
                    return true;
                }
            } catch (FilteringException e) {
                logger.log(Level.SEVERE, "cannot check for id assertion", e);
            }
            return false;
        } else if (assertion instanceof CustomAssertionHolder) {
            CustomAssertionHolder ch = (CustomAssertionHolder)assertion;
            return ch.isCustomCredentialSource();
        } else if (assertion instanceof CompositeAssertion) {
            CompositeAssertion root = (CompositeAssertion)assertion;
            Iterator i = root.getChildren().iterator();
            //noinspection WhileLoopReplaceableByForEach
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                if (checkForIdPotential(kid, requestor)) return true;
            }
            return false;
        } else
            return false;
    }

    private String resModeQueryString(PublishedService service) throws ServiceResolutionException {
        String urival = service.getRoutingUri();
        if (urival == null) urival = "";
        String nsval = "";
        Set<String> nsparams = nsResolver.getDistinctParameters(service);
        // pick the first one not null or empty
        for (String s : nsparams) {
            if (s != null) nsval = s;
            if (s != null && s.length() > 0) break;
        }
        String sactionval = "";
        Set<String> sactionparams = sactionResolver.getDistinctParameters(service);
        // pick the first one not null or empty
        for (String s : sactionparams) {
            if (s != null) sactionval = s;
            if (s != null && s.length() > 0) break;
        }
        return SecureSpanConstants.HttpQueryParameters.PARAM_URI + "=" + urival + "&amp;" +
               SecureSpanConstants.HttpQueryParameters.PARAM_SACTION + "=" + sactionval + "&amp;" +
               SecureSpanConstants.HttpQueryParameters.PARAM_NS + "=" + nsval;
    }

    private boolean isResModeRequested(HttpServletRequest req) {
        String modeparam = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_MODE);
        if (modeparam != null) {
            if (modeparam.toLowerCase().startsWith("res")) return true;
        }
        return false;
    }

    private String createWSILDoc(HttpServletRequest req, Collection<PublishedService> services, String host, int port, String uri) {
        /*  Format of document:
            <?xml version="1.0"?>
            <inspection xmlns="http://schemas.xmlsoap.org/ws/2001/10/inspection/">
              <service>
                <description referencedNamespace="http://schemas.xmlsoap.org/wsdl/"
                             location="http://example.com/stockquote.wsdl" />
              </service>
            </inspection>
        */
        boolean isResModeRequested = isResModeRequested(req);
        String protocol = req.isSecure() ? "https" : "http";
        StringBuilder outDoc = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<?xml-stylesheet type=\"text/xsl\" href=\"" + styleURL() + "\"?>\n" +
          "<inspection xmlns=\"http://schemas.xmlsoap.org/ws/2001/10/inspection/\">\n");
        // for each service
        for (PublishedService svc : services) {
            if (svc.isSoap()) {
                try {
                    String query;
                    if (isResModeRequested) {
                        query = resModeQueryString(svc);
                    } else {
                        query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + Goid.toString(svc.getGoid());
                    }
                    outDoc.append("\t<service>\n");
                    outDoc.append("\t\t<abstract>").append(svc.getName()).append("</abstract>\n");
                    outDoc.append("\t\t<description referencedNamespace=\"http://schemas.xmlsoap.org/wsdl/\" ");
                    outDoc.append("location=\"");
                    outDoc.append(protocol).append("://").append(InetAddressUtil.getHostForUrl(host)).append(":").append(port).append(uri).append("?").append(query);
                    outDoc.append("\"/>\n");
                    outDoc.append("\t</service>\n");
                } catch (ServiceResolutionException sre) {
                    logger.log(Level.WARNING, "Could not process service with goid '" + svc.getGoid() + "'.", sre);
                }
            }
        }
        outDoc.append("</inspection>");
        return outDoc.toString();
    }

    protected void sendBackError(HttpServletResponse res, int status, String msg) throws IOException {
        res.setStatus(status);
        res.getOutputStream().print(msg);
        res.getOutputStream().close();
    }

    /**
     * Get the first descendant assertion of the given type.
     *
     * @param in The assertion to check
     * @param assertionClass The type to find
     * @return the given assertion or one of its descendants of the given type or null if no such descendant exists
     */
    private static Assertion getFirstChild(Assertion in, Class assertionClass) {
        Assertion assertion = null;

        if (assertionClass.isInstance(in)) {
            assertion = in;
        }
        else  if (in instanceof CompositeAssertion) {
            CompositeAssertion comp = (CompositeAssertion) in;
            List kids = comp.getChildren();
            //noinspection ForLoopReplaceableByForEach
            for (Iterator iterator = kids.iterator(); iterator.hasNext();) {
                Assertion current = (Assertion) iterator.next();
                assertion = getFirstChild(current, assertionClass);
                if (assertion != null) {
                    break;
                }
            }
        }

        return assertion;
    }
}
