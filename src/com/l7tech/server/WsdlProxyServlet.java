package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.FindException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.sql.SQLException;


/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Sep 15, 2003
 * Time: 4:20:19 PM
 * $Id$
 *
 * Provides access to WSDL for published services of this SSG
 *
 * When calling this without specifying a reference to a service, this servlet
 * will return a WSIL document containing URLs to actual WSDL resources.
 *
 * When providing a reference to a service, this servlet will return an actual WSDL
 * document. This document is based on the WSDL of the protected service. This base
 * document is filtered so that the service endpoints point to this ssg's MessageProcessor
 * URL.
 *
 * Requests to this servlet can provide credentials or not. If valid credentials are
 * provided, the requestor will receive service information based on what his credentials
 * allow him to consume at run time. Anonymous requestors will only see service descriptions
 * for published services that allow anonymous access on this ssg.
 *
 * Authenticated requests must secured.
 *
 * For URL pattern, consult web.xml
 */
public class WsdlProxyServlet extends AuthenticatableHttpServlet {

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String serviceId = req.getParameter(PARAM_SERVICEOID);

        // let's see if we can get credentials...
        User user = authenticateRequestBasic(req);

        // NOTE: sending credentials over insecure channel is treated as an anonymous request
        // (i dont care if you send me credentials in non secure manner, that is your problem
        // however, i will not send sensitive information unless the channel is secure)
        if (user != null && req.isSecure()) {
            doAuthenticated(req, res, serviceId, user);
        } else {
            doAnonymous(req, res, serviceId);
        }
    }

    private void doAnonymous(HttpServletRequest req, HttpServletResponse res, String svcId) throws IOException {
        doAuthenticated(req, res, svcId, null);
    }

    private void doAuthenticated(HttpServletRequest req, HttpServletResponse res, String svcId, User requestor) throws IOException {
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
            if (requestor == null) {
                if (!policyAllowAnonymous(svc)) {
                    logger.info("user denied access to service description");
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "you need to provide valid credentials to see this service's description");
                    return;
                }
            } else { // otherwise make sure the requestor is authorized for this policy
                if (!userCanSeeThisService(requestor, svc)) {
                    logger.info("user denied access to service description");
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "these credentials do not grant you access to this service description.");
                    return;
                }
            }
            outputServiceDescription(res, svc);
            endTransaction();
        } else { // HANDLE REQUEST FOR LIST OF SERVICES
            Collection services = null;
            try {
                if (requestor == null) services = listAnonymouslyViewableServices();
                else services = listAnonymouslyViewableAndProtectedServices(requestor);
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
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "no services or not authorized");
                return;
            }
            outputServiceDescriptions(req, res, services);
        }
    }

    private void outputServiceDescription(HttpServletResponse res, PublishedService svc) throws IOException {
        // first, apply the necessary modifications to the wsdl before sending it back
        String output = svc.getWsdlXml();
        // todo, plug in mangler
        res.setContentType("text/xml; charset=utf-8");
        res.getOutputStream().println(output);
    }

    private void outputServiceDescriptions(HttpServletRequest req, HttpServletResponse res, Collection services) {
        String output = createWSILDoc(services, req.getServerName(), req.getProtocol(), Integer.toString(req.getServerPort()), req.getRequestURI());
    }

    private Collection listAnonymouslyViewableServices() throws TransactionException, IOException, FindException {
        return listAnonymouslyViewableAndProtectedServices(null);
    }

    private Collection listAnonymouslyViewableAndProtectedServices(User requestor) throws TransactionException, IOException, FindException {

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
            if (requestor == null) {
                if (policyAllowAnonymous(svc)) {
                    output.add(svc);
                }
            } else {
                if (userCanSeeThisService(requestor, svc)) {
                    output.add(svc);
                }
            }
        }

        endTransaction();
        return output;
    }

    private boolean userCanSeeThisService(User requestor, PublishedService svc) {
        // todo
        return true;
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

    private void endTransaction() {
        try {
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error closing transaction", e);
        }
    }


    private String createWSILDoc(Collection services, String host, String protocol, String port, String uri) {
        /*  Format of document:
            <?xml version="1.0"?>
            <inspection xmlns="http://schemas.xmlsoap.org/ws/2001/10/inspection/">
              <service>
                <description referencedNamespace="http://schemas.xmlsoap.org/wsdl/"
                             location="http://example.com/stockquote.wsdl" />
              </service>
            </inspection>
        */
        StringBuffer outDoc = new StringBuffer("<?xml version=\"1.0\"?>\n" +
                        "<inspection xmlns=\"http://schemas.xmlsoap.org/ws/2001/10/inspection/\">\n");
        // for each service
        for (Iterator i = services.iterator(); i.hasNext();) {
            PublishedService svc = (PublishedService)i.next();
            outDoc.append("\t<description referencedNamespace=\"http://schemas.xmlsoap.org/wsdl/\" ");
            outDoc.append("location=\"");
            outDoc.append(protocol + "://" + host + ":" + port + uri + "?" + PARAM_SERVICEOID + "=" + Long.toString(svc.getOid()));
            outDoc.append("\"/>\n");
        }
        outDoc.append("</inspection>");
        return outDoc.toString();
    }

    private static final String PARAM_SERVICEOID = "serviceoid";
}
