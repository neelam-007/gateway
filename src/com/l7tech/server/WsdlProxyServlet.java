package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.service.PublishedService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;

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

    private void doAnonymous(HttpServletRequest req, HttpServletResponse res, String svcId) {
        // todo
    }

    private void doAuthenticated(HttpServletRequest req, HttpServletResponse res, String svcId, User requestor) {
        // todo
    }

    private Collection listAnonymouslyViewableServices() {
        Collection output = new ArrayList();
        // todo, make list for public services only
        return output;
    }

    private Collection listAnonymouslyViewableAndProtectedServices(User requestor) {
        Collection output = new ArrayList(listAnonymouslyViewableServices());
        // todo, add "protected" services
        return output;
    }

    private static final String PARAM_SERVICEOID = "serviceoid";
}
