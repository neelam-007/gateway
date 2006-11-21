/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.admin;

import com.l7tech.common.LicenseException;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.HttpServletResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.server.identity.IdProvConfManagerServer;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.identity.User;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.beans.BeansException;
import org.w3c.dom.Document;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Principal;

/**
 * Authentication filter for the Manager applet servlet (and serving the manager applet jarfiles).
 */
public class ManagerAppletFilter implements Filter {
    private static final Logger logger = Logger.getLogger(ManagerAppletFilter.class.getName());
    public static final String PROP_CREDS = "ManagerApplet.authenticatedCredentials";
    public static final String PROP_USER = "ManagerApplet.authenticatedUser";
    public static final String SESSION_ID_COOKIE_NAME = "sessionId";
    public static final String DEFAULT_CODEBASE_PREFIX = "/ssg/webadmin/";

    private enum AuthResult { OK, CHALLENGED, FAIL }

    private WebApplicationContext applicationContext;
    private CustomAssertionsRegistrar customAssertionsRegistrar;
    private AdminSessionManager adminSessionManager;

    private ServerAssertion dogfoodPolicy;
    private Document fakeDoc;
    private String codebasePrefix;

    private Object getBean(String name, Class clazz) throws ServletException {
        try {
            return applicationContext.getBean(name, clazz);
        } catch (BeansException beansException) {
            throw new ServletException("Configuration error; could not get bean " + name, beansException);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        customAssertionsRegistrar = (CustomAssertionsRegistrar)getBean("customAssertionsAdmin", CustomAssertionsRegistrar.class);
        ServerPolicyFactory serverPolicyFactory = (ServerPolicyFactory)getBean("policyFactory", ServerPolicyFactory.class);
        adminSessionManager = (AdminSessionManager)getBean("adminSessionManager", AdminSessionManager.class);

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

        String codebasePrefix = filterConfig.getInitParameter("codebaseprefix");
        if (codebasePrefix == null)
            codebasePrefix = DEFAULT_CODEBASE_PREFIX;
        this.codebasePrefix = codebasePrefix;
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

        // Note that the user is authenticated before this is run
        if (handleCustomAssertionClassRequest(hreq, hresp))
            return;

        filterChain.doFilter(req, resp);
    }

    public void destroy() {
        // No action required at this time
    }

    private AuthResult authenticate(HttpServletRequest hreq, HttpServletResponse hresp) throws IOException {
        // Check for provided session ID and, if its valid and arrived over SSL, bypass authentication
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) {
            if (SESSION_ID_COOKIE_NAME.equalsIgnoreCase(cookie.getName())) {
                String sessionId = cookie.getValue();
                if (sessionId != null && sessionId.length() > 0 && hreq.isSecure()) {
                    Principal userObj = adminSessionManager.resumeSession(sessionId);
                    if (userObj instanceof User) {
                        User user = (User) userObj;
                        LoginCredentials creds = new LoginCredentials(user.getLogin(),
                                sessionId.toCharArray(),
                                CredentialFormat.OPAQUETOKEN,
                                CookieCredentialSourceAssertion.class);
                        hreq.setAttribute(PROP_CREDS, creds);
                        hreq.setAttribute(PROP_USER, user);
                        return AuthResult.OK;
                    }
                }
            }
        }

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
                hreq.setAttribute(PROP_USER, context.getAuthenticatedUser());
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
            sendChallenge(hresp);
            return AuthResult.CHALLENGED;
        }

        // Nope, it just done did failed
        logger.log(Level.WARNING, "Failed authentication for admin applet (status " + result + ")");
        return AuthResult.FAIL;
    }

    private void sendChallenge(HttpServletResponse hresp) throws IOException
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

    private void sendClass(HttpServletResponse hresp, byte[] data) throws IOException {
        ServletOutputStream sos = null;
        try {
            hresp.setContentType("application/java");
            hresp.setContentLength(data.length);
            sos = hresp.getOutputStream();
            sos.write(data);
            hresp.flushBuffer();
        } finally {
            if (sos != null) sos.close();
        }
    }

    /**
     * Handle request for custom assertion classes.
     *
     * <p>The user MUST be authenticated before calling this method.</p>
     *
     * @param hreq The HttpServletRequest
     * @param hresp The HttpServletResponse
     * @return true if the request has been handled (so no further action should be taken)
     */
    private boolean handleCustomAssertionClassRequest(final HttpServletRequest hreq,
                                                      final HttpServletResponse hresp) throws IOException {
        boolean handled = false;
        String filePath = hreq.getRequestURI();
        String contextPath = hreq.getContextPath();
        if (isRequestForCustomAssertionClass(contextPath, filePath)) {
            String className = getCustomAssertionClassName(contextPath, filePath);
            if (className != null) {
                byte[] data = customAssertionsRegistrar.getAssertionClass(className);
                if (data != null) {
                    handled = true;
                    sendClass(hresp, data);
                }
            }
        }

        return handled;
    }

    /**
     * We don't know package names here, so just check if the request looks like a class name.
     *
     * @param contextPath The context path
     * @param filePath The request path
     * @return true if the request could be for a Java class
     */
    private boolean isRequestForCustomAssertionClass(final String contextPath, final String filePath) {
        return filePath != null && filePath.startsWith(contextPath) && filePath.endsWith(".class");
    }

    /**
     * Get the class name for the requested class.
     *
     * @param contextPath The context path (prefix)
     * @param filePath The file path (request URI)
     * @return The class name or null if interpretation failed
     */
    private String getCustomAssertionClassName(final String contextPath, final String filePath) {
        String name = null;

        int prefixLength = contextPath==null ? 0 : contextPath.length();
        prefixLength += codebasePrefix.length();
        if (prefixLength+6 < filePath.length()) {
            String className = filePath.substring(prefixLength);
            className = className.substring(0, className.length() - 6); // remove .class
            name = className.replace('/', '.');
        }

        return name;
    }    
}
