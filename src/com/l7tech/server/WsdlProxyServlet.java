package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.logging.LogManager;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;
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
        User user = authenticateRequestBasic(req);
        if (user != null && req.isSecure()) {
            doAuthenticated(req, res, user);
        } else {
            doAnonymous(req, res);
        }
    }

    private void doAnonymous(HttpServletRequest req, HttpServletResponse res) {
        // todo
    }

    private void doAuthenticated(HttpServletRequest req, HttpServletResponse res, User requestor) {
        // todo
    }
}
