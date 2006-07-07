package com.l7tech.server;

import com.l7tech.common.LicenseException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.WsspAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.wssp.WsspWriter;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.server.policy.filter.IdentityRule;
import com.l7tech.server.policy.filter.FilterManager;
import com.l7tech.server.service.resolution.SoapActionResolver;
import com.l7tech.server.service.resolution.UrnResolver;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    public static final String PROPERTY_WSSP_ATTACH = "com.l7tech.server.wssp";
    private ServerConfig serverConfig;
    private FilterManager filterManager;
    SoapActionResolver sactionResolver = new SoapActionResolver();
    UrnResolver nsResolver = new UrnResolver();

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        WebApplicationContext appcontext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        serverConfig = (ServerConfig)appcontext.getBean("serverConfig");
        filterManager = (FilterManager)appcontext.getBean("wsspolicyFilterManager");
    }

    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_WSDLPROXY;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PublishedService ps = null;
        try {
            ps = getRequestedService(req);
        } catch (FindException e) {
            // if they ask for an invalid services WSDL return 404 since that WSDL doc does not exist
            logger.log(Level.INFO, "Invalid service requested", e);
            sendBackError(res, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        } catch (AmbiguousServiceException e) {
            logger.log(Level.INFO, "Service request ambiguous", e);
            sendBackError(res, HttpServletResponse.SC_MULTIPLE_CHOICES, e.getMessage());
            return;
        }

        // let's see if we can get credentials...
        AuthenticationResult[] results;
        try {
            if (ps != null) {
                results = authenticateRequestBasic(req, ps);
            } else {
                results = authenticateRequestBasic(req);
            }
        } catch (AuthenticationException e) {
            logger.log(Level.INFO, "Credentials do not authenticate against any of the providers, assuming anonymous");
            results = null;
        } catch (LicenseException e) {
            logger.log(Level.WARNING, "Service is unlicensed, returning 500", e);
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.getOutputStream().print("Gateway WSDL proxy service not enabled by license");
            res.flushBuffer();
            return;
        }

        // NOTE: sending credentials over insecure channel is treated as an anonymous request
        // (i dont care if you send me credentials in non secure manner, that is your problem
        // however, i will not send sensitive information unless the channel is secure)
        if (results != null && results.length > 0 && req.isSecure()) {
            doAuthenticated(req, res, ps, results);
        } else {
            doAnonymous(req, res, ps);
        }
    }

    class AmbiguousServiceException extends Exception {
        public AmbiguousServiceException(String s) {
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
     */
    private PublishedService getRequestedService(HttpServletRequest req) throws AmbiguousServiceException, FindException {
        // resolution using traditional serviceoid param
        String serviceStr = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID);
        if (serviceStr != null) {
            PublishedService ps;
            long serviceId;
            try {
                serviceId = Long.parseLong(serviceStr);
                ps = resolveService(serviceId);
                if (ps == null) {
                    throw new FindException("Service id " + serviceStr + " did not resolve any service.");
                }
            } catch (NumberFormatException e) {
                throw new FindException("cannot parse long from " + serviceStr, e);
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
        // get all current services
        Collection services = serviceManager.findAll();
        // if uri param provided, narrow down list using it
        if (uriparam != null) {
            for (Iterator iterator = services.iterator(); iterator.hasNext();) {
                PublishedService publishedService = (PublishedService) iterator.next();
                if (uriparam.length() <= 0 && publishedService.getRoutingUri() == null) continue;
                if (!uriparam.equals(publishedService.getRoutingUri())) iterator.remove();
            }
            if (services.size() == 1) {
                return (PublishedService)services.iterator().next();
            }
            if (services.size() == 0) {
                throw new FindException("URI param '" + uriparam + "' did not resolve any service.");
            }
        }
        // narrow it down using soapaction (if provided)
        if (sactionparam != null) {
            for (Iterator iterator = services.iterator(); iterator.hasNext();) {
                PublishedService publishedService = (PublishedService) iterator.next();
                Set sactionparams = sactionResolver.getDistinctParameters(publishedService);
                if (!sactionparams.contains(sactionparam)) iterator.remove();
            }
            if (services.size() == 1) {
                return (PublishedService)services.iterator().next();
            }
            if (services.size() == 0) {
                throw new FindException("SoapAction param '" + sactionparam + "' did not resolve any service.");
            }
        }
        // narrow it down using ns (if provided)
        if (nsparam != null) {
            for (Iterator iterator = services.iterator(); iterator.hasNext();) {
                PublishedService publishedService = (PublishedService) iterator.next();
                Set nsparams = nsResolver.getDistinctParameters(publishedService);
                if (!nsparams.contains(nsparam)) iterator.remove();
            }
            if (services.size() == 1) {
                return (PublishedService)services.iterator().next();
            }
            if (services.size() == 0) {
                throw new FindException("ns param '" + nsparam + "' did not resolve any service.");
            }
        }

        // could not narrow it down enough -> throw AmbiguousServiceException
        StringBuffer names = new StringBuffer();
        for (Iterator iterator = services.iterator(); iterator.hasNext();) {
            PublishedService publishedService = (PublishedService) iterator.next();
            names.append(publishedService.getName()).append(" ");
        }
        logger.info("service query too wide: " + names.toString());
        throw new AmbiguousServiceException("Too many services fit the mold: " + names.toString());
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
            if (systemAllowsAnonymousDownloads(req)) {
            } else if (results == null || results.length < 1) {
                if (!policyAllowAnonymous(svc)) {
                    logger.info("user denied access to service description");
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "you need to provide valid credentials to see this service's description");
                    return;
                }
            } else { // otherwise make sure the requestor is authorized for this policy
                boolean ok = false;
                if (!policyAllowAnonymous(svc)) {
                    User requestor = null;
                    for (int i = 0; i < results.length; i++) {
                        AuthenticationResult result = results[i];
                        requestor = result.getUser();
                        if (userCanSeeThisService(requestor, svc)) ok = true;
                    }
                    if (!ok) {
                        logger.info("user denied access to service description " + requestor + " " + svc.getPolicyXml());
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "these credentials do not grant you access to this service description.");
                        return;
                    }
                }
            }
            outputServiceDescription(req, res, svc, results);
            logger.info("Returned description for service, " + svc.getOid());
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
            Collection services = listres.allowed();
            if (services.size() < 1) {
                if ((results == null || results.length < 1) && listres.anyServices() && req.isSecure()) {
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

    private void sendAuthChallenge(HttpServletResponse res) throws IOException {
        // send error back with a hint that credentials should be provided
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        logger.fine("sending back authentication challenge");
        // in this case, send an authentication challenge
        //res.setHeader("WWW-Authenticate", "Basic realm=\"" + ServerHttpBasic.REALM + "\"");
        res.setHeader("WWW-Authenticate", "Basic realm=\"\"");
        res.getOutputStream().close();
    }

    /**
     * this must always resolve to the http port
     */
    private String styleURL() {
        return "/ssg/wsil2xhtml.xml";
    }

    /**
     * TODO what about other bindings?
     * TODO what about other namespaces (http://schemas.xmlsoap.org/wsdl/soap12/)
     */
    private void substituteSoapAddressURL(Document wsdl, URL newURL) {
        // get http://schemas.xmlsoap.org/wsdl/ 'port' element
        NodeList portlist = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "port");
        for (int i = 0; i < portlist.getLength(); i++) {
            Element portel = (Element)portlist.item(i);
            // get child http://schemas.xmlsoap.org/wsdl/soap/ 'address'
            List addresses = XmlUtil.findChildElementsByName(portel, "http://schemas.xmlsoap.org/wsdl/soap/", "address");
            // change the location attribute with new URL
            for (Iterator iterator = addresses.iterator(); iterator.hasNext();) {
                Element address = (Element) iterator.next();
                address.setAttribute("location", newURL.toString());
            }
        }
    }

    /**
     *
     */
    private void addSecurityPolicy(Document wsdl, PublishedService svc, AuthenticationResult[] results) {
        try{
            if (Boolean.getBoolean(PROPERTY_WSSP_ATTACH)) {
                Assertion rootassertion = WspReader.parsePermissively(svc.getPolicyXml());
                if (Assertion.contains(rootassertion, WsspAssertion.class)) {
                    // remove any existing policy
                    XmlUtil.stripNamespace(wsdl.getDocumentElement(), "http://schemas.xmlsoap.org/ws/2004/09/policy");
                    Assertion effectivePolicy = filterManager.applyAllFilters(null, rootassertion);
                    if (effectivePolicy != null) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Effective policy for user: \n" + WspWriter.getPolicyXml(effectivePolicy));
                            }

                            WsspWriter.decorate(wsdl, effectivePolicy);
                    }
                    else {
                        logger.info("No policy to add!");
                    }
                }
                else {
                    logger.info("No WSSP Assertion in policy, not adding policy to WSDL.");
                }
            }
            else {
                logger.fine("WS-SecurityPolicy decoration not enabled.");
            }
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Could not add policy to WSDL.", e);
        }
    }

    private void outputServiceDescription(HttpServletRequest req, HttpServletResponse res, PublishedService svc, AuthenticationResult[] results) throws IOException {
        Document wsdlDoc = null;
        try {
            wsdlDoc = XmlUtil.stringToDocument(svc.getWsdlXml());
        } catch (SAXException e) {
            logger.log(Level.WARNING, "cannot parse wsdl", e);
        }

        // TODO figure out if current user's effective policy requires SSl
        // For now, we'll just scan the policy for any SSL assertion
        boolean useSsl = svc.getPolicyXml().contains("SslAssertion");

        // change url of the wsdl
        // direct to http by default. choose http port based on port used for this request.
        int port = req.getServerPort();
        if (port == 8443 || port == 8080)
            port = useSsl ? 8443 : 8080;
        else if (port == 443 || port == 80) port = useSsl ? 443 : 80;
        else useSsl = false; // don't try to change the protocol if we don't recognize the port
        String proto = useSsl ? "https" : "http";
        URL ssgurl;
        String routinguri = svc.getRoutingUri();
        if (routinguri == null || routinguri.length() < 1) {
            ssgurl = new URL(proto + "://" + req.getServerName() + ":" +
                             port + SecureSpanConstants.SERVICE_FILE +
                             Long.toString(svc.getOid()));
        } else {
            ssgurl = new URL(proto + "://" + req.getServerName() + ":" +
                             port + routinguri);
        }
        substituteSoapAddressURL(wsdlDoc, ssgurl);
        addSecurityPolicy(wsdlDoc, svc, results);

        // output the wsdl
        res.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
        try {
            XmlUtil.nodeToOutputStream(wsdlDoc, res.getOutputStream());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error outputing wsdl", e);
            throw new IOException(e.getMessage());
        }
    }

    private void outputServiceDescriptions(HttpServletRequest req, HttpServletResponse res, Collection services) throws IOException {
        String uri = req.getRequestURI();
        uri = uri.replaceAll("wsil", "wsdl");
        String output = createWSILDoc(req, services, req.getServerName(), Integer.toString(req.getServerPort()), uri);
        res.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
        ServletOutputStream os = res.getOutputStream();
        os.print(output);
        os.close();
    }

    interface ListResults {
        Collection allowed();
        boolean anyServices();
    }

    private ListResults listAllServices() throws FindException {
        final Collection allServices = serviceManager.findAll();
        return new ListResults() {
            public Collection allowed() {
                return allServices;
            }
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
        final Collection allServices = serviceManager.findAll();

        // prepare output collection
        final Collection output = new ArrayList();

        // decide which ones make the cut
        for (Iterator i = allServices.iterator(); i.hasNext();) {
            PublishedService svc = (PublishedService)i.next();
            if (policyAllowAnonymous(svc)) {
                output.add(svc);
            }
            else if (results != null) {
                for (int j = 0; j < results.length; j++) {
                    AuthenticationResult result = results[j];
                    User requestor = result.getUser();
                    if (userCanSeeThisService(requestor, svc))
                        output.add(svc);
                }
            }
        }

        return new ListResults() {
            public Collection allowed() {
                return output;
            }
            public boolean anyServices() {
                return allServices.size() >= 1;
            }
        };
    }

    private boolean userCanSeeThisService(User requestor, PublishedService svc) throws IOException {
        // start at the top
        Assertion rootassertion;
        rootassertion = WspReader.parsePermissively(svc.getPolicyXml());
        return checkForIdPotential(rootassertion, requestor);
    }

    /**
     * returns true if at least one of the id assertions in this tree can be met by the requestor
     */
    private boolean checkForIdPotential(Assertion assertion, User requestor) {
        if (assertion instanceof IdentityAssertion) {
            try {
                if (IdentityRule.canUserPassIDAssertion((IdentityAssertion)assertion, requestor, providerConfigManager)) {
                    return true;
                }
            } catch (FilteringException e) {
                logger.log(Level.SEVERE, "cannot check for id assertion", e);
            }
            return false;
        } else if (assertion instanceof CustomAssertionHolder) {
            CustomAssertionHolder ch = (CustomAssertionHolder)assertion;
            return Category.ACCESS_CONTROL.equals(ch.getCategory());
        } else if (assertion instanceof CompositeAssertion) {
            CompositeAssertion root = (CompositeAssertion)assertion;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                if (checkForIdPotential(kid, requestor)) return true;
            }
            return false;
        } else
            return false;
    }

    private String resModeQueryString(PublishedService service) {
        String urival = service.getRoutingUri();
        if (urival == null) urival = "";
        String nsval = "";
        Set nsparams = nsResolver.getDistinctParameters(service);
        // pick the first one not null or empty
        for (Iterator iterator = nsparams.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            if (s != null) nsval = s;
            if (s != null && s.length() > 0) break;
        }
        String sactionval = "";
        Set sactionparams = sactionResolver.getDistinctParameters(service);
        // pick the first one not null or empty
        for (Iterator iterator = sactionparams.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
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

    private String createWSILDoc(HttpServletRequest req, Collection services, String host, String port, String uri) {
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
        String protocol = "http";
        if (port.equals("8443") || port.equals("443")) protocol = "https";
        StringBuffer outDoc = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<?xml-stylesheet type=\"text/xsl\" href=\"" + styleURL() + "\"?>\n" +
          "<inspection xmlns=\"http://schemas.xmlsoap.org/ws/2001/10/inspection/\">\n");
        // for each service
        for (Iterator i = services.iterator(); i.hasNext();) {
            PublishedService svc = (PublishedService)i.next();
            if (svc.isSoap()) {
                outDoc.append("\t<service>\n");
                outDoc.append("\t\t<abstract>").append(svc.getName()).append("</abstract>\n");
                outDoc.append("\t\t<description referencedNamespace=\"http://schemas.xmlsoap.org/wsdl/\" ");
                outDoc.append("location=\"");
                String query;
                if (isResModeRequested) {
                    query = resModeQueryString(svc);
                } else {
                    query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + Long.toString(svc.getOid());
                }
                outDoc.append(protocol).append("://").append(host).append(":").append(port).append(uri).append("?").append(query);
                outDoc.append("\"/>\n");
                outDoc.append("\t</service>\n");
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
}
