package com.l7tech.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.policy.filter.FilteringException;
import com.l7tech.server.policy.filter.IdentityRule;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
 * $Id$
 */
public class WsdlProxyServlet extends AuthenticatableHttpServlet {

    public static final String SOAP_PROCESSING_SERVLET_URI = SecureSpanConstants.SSG_FILE;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String serviceId = req.getParameter(SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID);

        // let's see if we can get credentials...
        List users;
        try {
            if (serviceId != null) {
                users = authenticateRequestBasic(req, resolveService(Long.parseLong(serviceId)));
            } else {
                users = authenticateRequestBasic(req);
            }

        } catch (AuthenticationException e) {
            logger.log(Level.INFO, "Credentials do not authenticate against any of the providers, assuming anonymous");
            users = null;
        }

        // NOTE: sending credentials over insecure channel is treated as an anonymous request
        // (i dont care if you send me credentials in non secure manner, that is your problem
        // however, i will not send sensitive information unless the channel is secure)
        if (users != null && !users.isEmpty() && req.isSecure()) {
            doAuthenticated(req, res, serviceId, users);
        } else {
            doAnonymous(req, res, serviceId);
        }
    }

    private void doAnonymous(HttpServletRequest req, HttpServletResponse res, String svcId) throws IOException {
        doAuthenticated(req, res, svcId, null);
    }

    private void doAuthenticated(HttpServletRequest req, HttpServletResponse res, String svcId, List users) throws IOException {
        // HANDLE REQUEST FOR ONE SERVICE DESCRIPTION
        if (svcId != null && svcId.length() > 0) {
            // get this service
            ServiceManager manager = getServiceManager();
            try {
                beginTransaction();
            } catch (TransactionException e) {
                logger.log(Level.SEVERE, "cannot begin transaction", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
            PublishedService svc = null;
            try {
                svc = manager.findByPrimaryKey(Long.parseLong(svcId));
            } catch (FindException e) {
                logger.log(Level.SEVERE, "cannot find service", e);
                svc = null;
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, "cannot parse service id", e);
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "wrong service id: " + svcId);
                return;
            }
            if (svc == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "service does not exist or not authorized");
                return;
            }
            // make sure this service is indeed anonymously accessible
            if (users == null || users.isEmpty()) {
                if (!policyAllowAnonymous(svc)) {
                    logger.info("user denied access to service description");
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "you need to provide valid credentials to see this service's description");
                    return;
                }
            } else { // otherwise make sure the requestor is authorized for this policy
                boolean ok = false;
                if (!policyAllowAnonymous(svc)) {
                    User requestor = null;
                    for (Iterator i = users.iterator(); i.hasNext();) {
                        requestor = (User)i.next();
                        if (userCanSeeThisService(requestor, svc)) ok = true;
                    }
                    if (!ok) {
                        logger.info("user denied access to service description " + requestor + " " + svc.getPolicyXml());
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "these credentials do not grant you access to this service description.");
                        return;
                    }
                }
            }
            outputServiceDescription(req, res, svc);
            endTransaction();
            logger.info("Returned description for service, " + svcId);
        } else { // HANDLE REQUEST FOR LIST OF SERVICES
            Collection services = null;
            try {
                if (users == null || users.isEmpty())
                    services = listAnonymouslyViewableServices();
                else
                    services = listAnonymouslyViewableAndProtectedServices(users);
            } catch (TransactionException e) {
                logger.log(Level.SEVERE, "cannot list services", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            } catch (FindException e) {
                logger.log(Level.SEVERE, "cannot list services", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
            // Is there anything to show?
            if (services.size() < 1) {
                //res.sendError(HttpServletResponse.SC_NOT_FOUND, "no services or not authorized");
                // return empty wsil
                String emptywsil = "<?xml version=\"1.0\"?>\n" +
                  "<inspection xmlns=\"http://schemas.xmlsoap.org/ws/2001/10/inspection/\" />\n";
                res.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
                res.getOutputStream().println(emptywsil);
                return;
            }
            outputServiceDescriptions(req, res, services);
            logger.info("Returned list of service description targets");
        }
    }

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

    private void outputServiceDescription(HttpServletRequest req, HttpServletResponse res, PublishedService svc) throws IOException {
        Document wsdlDoc = null;
        try {
            wsdlDoc = XmlUtil.stringToDocument(svc.getWsdlXml());
        } catch (SAXException e) {
            logger.log(Level.WARNING, "cannot parse wsdl", e);
        }

        // change url of the wsdl
        // direct to http by default. choose http port based on port used for this request.
        int port = req.getServerPort();
        if (port == 8443)
            port = 8080;
        else if (port == 443) port = 80;
        URL ssgurl = new URL("http" + "://" + req.getServerName() + ":" + port + SOAP_PROCESSING_SERVLET_URI + "?" + SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + Long.toString(svc.getOid()));
        substituteSoapAddressURL(wsdlDoc, ssgurl);

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
        String output = createWSILDoc(services, req.getServerName(), Integer.toString(req.getServerPort()), uri);
        res.setContentType(XmlUtil.TEXT_XML + "; charset=utf-8");
        res.getOutputStream().println(output);
    }

    private Collection listAnonymouslyViewableServices() throws TransactionException, IOException, FindException {
        return listAnonymouslyViewableAndProtectedServices(null);
    }

    private Collection listAnonymouslyViewableAndProtectedServices(List users) throws TransactionException, IOException, FindException {

        ServiceManager manager = getServiceManager();
        beginTransaction();
        // get all services
        Collection allServices = null;
        allServices = manager.findAll();

        // prepare output collection
        Collection output = new ArrayList();

        // decide which ones make the cut
        for (Iterator i = allServices.iterator(); i.hasNext();) {
            PublishedService svc = (PublishedService)i.next();
            if (users == null || users.isEmpty()) {
                if (policyAllowAnonymous(svc)) {
                    output.add(svc);
                }
            } else {
                if (policyAllowAnonymous(svc)) {
                    output.add(svc);
                } else {
                    for (Iterator j = users.iterator(); j.hasNext();) {
                        User requestor = (User)j.next();
                        if (userCanSeeThisService(requestor, svc))
                            output.add(svc);
                    }
                }
            }
        }

        endTransaction();
        return output;
    }

    private boolean userCanSeeThisService(User requestor, PublishedService svc) throws IOException {
        // start at the top
        Assertion rootassertion = null;
        rootassertion = WspReader.parse(svc.getPolicyXml());
        return checkForIdPotential(rootassertion, requestor);
    }

    /**
     * returns true if at least one of the id assertions in this tree can be met by the requestor
     */
    private boolean checkForIdPotential(Assertion assertion, User requestor) {
        if (assertion instanceof IdentityAssertion) {
            try {
                if (IdentityRule.canUserPassIDAssertion((IdentityAssertion)assertion, requestor)) {
                    return true;
                }
            } catch (FilteringException e) {
                logger.log(Level.SEVERE, "cannot check for id assertion", e);
            }
            return false;
        } else if (assertion instanceof CustomAssertionHolder) {
            CustomAssertionHolder ch = (CustomAssertionHolder)assertion;
            if (Category.ACCESS_CONTROL.equals(ch.getCategory())) { // bingo
                return true;
            }
            return false;
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

    private ServiceManager getServiceManager() {
        ServiceManager output = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        if (output == null) throw new RuntimeException("Cannot instantiate the ServiceManager");
        return output;
    }

    private void beginTransaction() throws TransactionException {
        try {
            PersistenceContext.getCurrent().beginTransaction();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "cannot begin transaction", e);
            throw new TransactionException("cannot begin transaction", e);
        }
    }


    private String createWSILDoc(Collection services, String host, String port, String uri) {
        /*  Format of document:
            <?xml version="1.0"?>
            <inspection xmlns="http://schemas.xmlsoap.org/ws/2001/10/inspection/">
              <service>
                <description referencedNamespace="http://schemas.xmlsoap.org/wsdl/"
                             location="http://example.com/stockquote.wsdl" />
              </service>
            </inspection>
        */
        String protocol = "http";
        if (port.equals("8443") || port.equals("443")) protocol = "https";
        StringBuffer outDoc = new StringBuffer("<?xml version=\"1.0\"?>\n" +
          "<inspection xmlns=\"http://schemas.xmlsoap.org/ws/2001/10/inspection/\">\n");
        // for each service
        for (Iterator i = services.iterator(); i.hasNext();) {
            PublishedService svc = (PublishedService)i.next();
            outDoc.append("\t<service>\n");
            outDoc.append("\t\t<abstract>" + svc.getName() + "</abstract>\n");
            outDoc.append("\t\t<description referencedNamespace=\"http://schemas.xmlsoap.org/wsdl/\" ");
            outDoc.append("location=\"");
            String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + Long.toString(svc.getOid());
            outDoc.append(protocol + "://" + host + ":" + port + uri + "?" + query);
            outDoc.append("\"/>\n");
            outDoc.append("\t</service>\n");
        }
        outDoc.append("</inspection>");
        return outDoc.toString();
    }
}
