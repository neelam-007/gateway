/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.admin;

import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.HttpServletResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.identity.IdProvConfManagerServer;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication filter for the Manager applet servlet (and serving the manager applet jarfiles).
 */
public class ManagerAppletFilter implements Filter {
    private static final Logger logger = Logger.getLogger(ManagerAppletFilter.class.getName());
    public static final String PROP_CREDS = "ManagerApplet.authenticatedCredentials";

    private enum AuthResult { OK, CHALLENGED, FAIL }

    private WebApplicationContext applicationContext;
    private AuditContext auditContext;
    private ServerConfig serverConfig;
    private ClusterPropertyManager clusterPropertyManager;
    private LicenseManager licenseManager;

    private ServerAssertion dogfoodPolicy;
    private Document fakeDoc;


    public void init(FilterConfig filterConfig) throws ServletException {
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        auditContext = (AuditContext)applicationContext.getBean("auditContext");
        serverConfig = (ServerConfig)applicationContext.getBean("serverConfig");
        clusterPropertyManager = (ClusterPropertyManager)applicationContext.getBean("clusterPropertyManager");
        licenseManager = (LicenseManager)applicationContext.getBean("licenseManager");
        ServerPolicyFactory serverPolicyFactory = (ServerPolicyFactory)applicationContext.getBean("policyFactory");

        CompositeAssertion dogfood = new AllAssertion();
        dogfood.addChild(new HttpBasic());
        dogfood.addChild(new AuthenticationAssertion(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID));
        fakeDoc = XmlUtil.createEmptyDocument("placeholder", "l7", "http://www.l7tech.com/ns/placeholder");

        try {
            dogfoodPolicy = serverPolicyFactory.compilePolicy(dogfood, false);
        } catch (ServerPolicyException e) {
            throw new ServletException("Configuration error; could not compile dogfood policy");
        } catch (LicenseException e) {
            throw new RuntimeException(e); // can't happen, license enforcement is disabled
        }
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain) throws IOException, ServletException {
        if (!(req instanceof HttpServletRequest))
            throw new ServletException("Request not HTTP request");
        if (!(resp instanceof HttpServletResponse))
            throw new ServletException("Response not HTTP response");

        HttpServletResponse hresp = (HttpServletResponse)resp;
        HttpServletRequest hreq = (HttpServletRequest)req;

        AuthResult authResult = authenticate(hreq, hresp);
        if (authResult == AuthResult.CHALLENGED) {
            // Already sent challenge
            return;
        }

        if (authResult != AuthResult.OK) {
            hresp.setStatus(401);
            hresp.sendError(401, "Not authorized");
            return;
        }

        if (!hreq.isSecure()) {
            hresp.setStatus(404);
            hresp.sendError(404, "Request must arrive over SSL.");
            return;
        }

        filterChain.doFilter(req, resp);
    }

    public void destroy() {
        // No action required at this time
    }

    private AuthResult authenticate(HttpServletRequest hreq, HttpServletResponse hresp) throws IOException {
        String auth = hreq.getHeader(HttpConstants.HEADER_AUTHORIZATION);

        Message request = new Message();
        request.initialize(fakeDoc);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hreq));

        Message response = new Message();
        HttpServletResponseKnob hsrespKnob = new HttpServletResponseKnob(hresp);
        response.attachHttpResponseKnob(hsrespKnob);

        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);

        final AssertionStatus result;
        try {
            result = dogfoodPolicy.checkRequest(context);
            if (result == AssertionStatus.NONE && context.getCredentials() != null) {
                hreq.setAttribute(PROP_CREDS, context.getCredentials());
                return AuthResult.OK;
            }

        } catch (PolicyAssertionException e) {
            // TODO audit this!
            logger.log(Level.WARNING, "Dogfood policy failed for admin applet: " + ExceptionUtils.getMessage(e), e);
            return AuthResult.FAIL;
        }

        // See if a challenge is indicated
        if (hsrespKnob.hasChallenge()) {
            hsrespKnob.beginChallenge();
            sendChallenge(context, hreq, hresp);
            return AuthResult.CHALLENGED;
        }

        // Nope, it just done did failed
        logger.log(Level.WARNING, "Failed authentication for admin applet (status " + result + ")");
        return AuthResult.FAIL;
    }

    private void sendChallenge(PolicyEnforcementContext context,
                               HttpServletRequest hreq,
                               HttpServletResponse hresp) throws IOException
    {
        ServletOutputStream sos = null;
        try {
            // the challenge http header is supposed to already been appended at that point-ah
            hresp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            sos = hresp.getOutputStream();
            sos.print("Authentication Required");
        } finally {
            if (sos != null) sos.close();
        }
    }

}
