/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.admin;

import com.l7tech.common.LicenseException;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.ServiceMessages;
import com.l7tech.common.message.*;
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
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.event.system.AdminAppletEvent;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.spring.remoting.RemoteUtils;
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
import java.util.Set;
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

    private FilterConfig filterConfig;
    private WebApplicationContext applicationContext;
    private CustomAssertionsRegistrar customAssertionsRegistrar;
    private AdminSessionManager adminSessionManager;
    private RoleManager roleManager;
    private AuditContext auditContext;
    private SoapFaultManager soapFaultManager;

    private ServerAssertion dogfoodPolicy;
    private Document fakeDoc;
    private String codebasePrefix;

    private Object getBean(String name, Class clazz) throws ServletException {
        try {
            final Object o = applicationContext.getBean(name, clazz);
            if (o == null) throw new ServletException("Configurationerror; could not find bean " + name);
            return o;
        } catch (BeansException beansException) {
            throw new ServletException("Configuration error; could not get bean " + name, beansException);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        customAssertionsRegistrar = (CustomAssertionsRegistrar)getBean("customAssertionsAdmin", CustomAssertionsRegistrar.class);
        ServerPolicyFactory serverPolicyFactory = (ServerPolicyFactory)getBean("policyFactory", ServerPolicyFactory.class);
        adminSessionManager = (AdminSessionManager)getBean("adminSessionManager", AdminSessionManager.class);
        roleManager = (RoleManager)getBean("roleManager", RoleManager.class);
        auditContext = (AuditContext)getBean("auditContext", AuditContext.class);
        soapFaultManager = (SoapFaultManager)getBean("soapFaultManager", SoapFaultManager.class);

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

    public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain filterChain) throws IOException, ServletException {
        if (!(req instanceof HttpServletRequest))
            throw new ServletException("Request not HTTP request");
        if (!(resp instanceof HttpServletResponse))
            throw new ServletException("Response not HTTP response");

        HttpServletResponse hresp = (HttpServletResponse)resp;
        HttpServletRequest hreq = (HttpServletRequest)req;

        Auditor auditor = new Auditor(this, applicationContext, logger);

        PolicyEnforcementContext context = null;
        int status = 500;
        boolean passed = false;
        try {
            Message request = new Message();
            request.initialize(fakeDoc);
            request.attachHttpRequestKnob(new HttpServletRequestKnob(hreq));

            Message response = new Message();
            HttpServletResponseKnob hsrespKnob = new HttpServletResponseKnob(hresp);
            response.attachHttpResponseKnob(hsrespKnob);

            context = new PolicyEnforcementContext(request, response);
            context.setReplyExpected(true);
            context.setAuditContext(auditContext);
            context.setSoapFaultManager(soapFaultManager);

            AuthResult authResult = authenticate(hreq, hresp, context, auditor);
            if (authResult == AuthResult.CHALLENGED) {
                // Already audited a detail message and sent challenge
                passed = true;
                return;
            }

            if (authResult != AuthResult.OK) {
                // Already audited a detail message
                hresp.setStatus(status = 401);
                hresp.sendError(401, "Not authorized");
                return;
            }

            if (!hreq.isSecure()) {
                auditor.logAndAudit(ServiceMessages.APPLET_AUTH_NO_SSL);
                hresp.setStatus(status = 404);
                hresp.sendError(404, "Request must arrive over SSL.");
                return;
            }

            passed = true;

            // Note that the user is authenticated before this is run
            if (handleCustomAssertionClassRequest(hreq, hresp, auditor)) {
                return;
            }

            if (handleJarRequest(hreq, hresp)) {
                return;
            }

            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_FILTER_PASSED);
            final IOException[] ioeHolder = new IOException[1];
            final ServletException[] seHolder = new ServletException[1];
            RemoteUtils.runWithClientHost(hreq.getRemoteAddr(), new Runnable(){
                public void run() {
                    try {
                        filterChain.doFilter(req, resp);
                    } catch (IOException e) {
                        ioeHolder[0] = e;
                    } catch (ServletException e) {
                        seHolder[0] = e;
                    }
                }
            });
            if (ioeHolder[0] != null) throw ioeHolder[0];
            if (seHolder[0] != null) throw seHolder[0];
        } finally {
            try {
                Level level = Level.FINE;
                String message = "Admin applet request";
                if (!passed) {
                    message = "Applet applet request filter failed: status = " + status;
                    level = Level.WARNING;
                }
                User user = (User)hreq.getAttribute(PROP_USER);
                if (user == null) user = new UserBean();
                applicationContext.publishEvent(
                        new AdminAppletEvent(this,
                                             level,
                                             req.getRemoteAddr(),
                                             message,
                                             user.getProviderId(),
                                             getName(user),
                                             user.getId()));
                auditContext.flush();
            } finally {
                if (context != null) context.close();
            }
        }
    }

    private static String getName(User user) {
        return user.getName() == null ? user.getLogin() : user.getName();
    }

    public void destroy() {
        // No action required at this time
    }

    // If this method returns, an audit detail message has been added.
    private AuthResult authenticate(HttpServletRequest hreq, HttpServletResponse hresp, PolicyEnforcementContext context, Auditor auditor) throws IOException {
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
                        auditor.logAndAudit(ServiceMessages.APPLET_AUTH_COOKIE_SUCCESS, new String[] { getName(user) });
                        return AuthResult.OK;
                    }
                }
            }
        }

        final AssertionStatus result;
        try {
            result = dogfoodPolicy.checkRequest(context);
            if (result == AssertionStatus.NONE && context.getCredentials() != null) {
                final User user = context.getAuthenticatedUser();
                if (roleManager.getAssignedRoles(user).isEmpty()) {
                    auditor.logAndAudit(ServiceMessages.APPLET_AUTH_NO_ROLES, new String[] { getName(user) });
                    return AuthResult.FAIL;
                }
                hreq.setAttribute(PROP_CREDS, context.getCredentials());
                hreq.setAttribute(PROP_USER, user);
                auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_SUCCESS, new String[] { getName(user) });
                return AuthResult.OK;
            }

        } catch (PolicyAssertionException e) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_FAILED, new String[] {ExceptionUtils.getMessage(e)}, e);
            return AuthResult.FAIL;
        } catch (FindException e) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_DB_ERROR, new String[] {ExceptionUtils.getMessage(e)}, e);
            return AuthResult.FAIL;
        }

        // See if a challenge is indicated
        HttpServletResponseKnob hsrespKnob =
                (HttpServletResponseKnob)context.getResponse().getKnob(HttpServletResponseKnob.class);
        if (hsrespKnob != null && hsrespKnob.hasChallenge()) {
            hsrespKnob.beginChallenge();
            sendChallenge(hresp);
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_CHALLENGE);
            return AuthResult.CHALLENGED;
        }

        // Nope, it just done did failed
        auditor.logAndAudit(ServiceMessages.APPLET_AUTH_FAILED, new String[] { result.toString() });        
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
     * @param auditor The auditor to which a detail message will be added if a class is downloaded 
     * @return true if the request has been handled (so no further action should be taken)
     * @throws java.io.IOException if there is a problem loading or transmitting the class
     */
    private boolean handleCustomAssertionClassRequest(final HttpServletRequest hreq,
                                                      final HttpServletResponse hresp,
                                                      final Auditor auditor) throws IOException {
        boolean handled = false;
        String filePath = hreq.getRequestURI();
        String contextPath = hreq.getContextPath();
        if (isRequestForCustomAssertionClass(contextPath, filePath)) {
            String className = getCustomAssertionClassName(contextPath, filePath);
            if (className != null) {
                byte[] data = customAssertionsRegistrar.getAssertionClass(className);
                if (data != null) {
                    handled = true;
                    auditor.logAndAudit(ServiceMessages.APPLET_AUTH_CLASS_DOWNLOAD, new String[] {className});
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


    /**
     * Handle request for JAR files.
     *
     * <p>The user MUST be authenticated before calling this method.</p>
     *
     * <p>This will redirect to the Pack200 version of the file if one is
     * available</p>
     *
     * @param hreq The HttpServletRequest
     * @param hresp The HttpServletResponse
     * @return true if the request has been handled (so no further action should be taken)
     */
    private boolean handleJarRequest(final HttpServletRequest hreq,
                                     final HttpServletResponse hresp) throws IOException, ServletException {
        boolean handled = false;

        String filePath = hreq.getRequestURI();
        String contextPath = hreq.getContextPath();
        String encodingHeader = hreq.getHeader("Accept-Encoding");

        if (filterConfig != null && filePath != null && contextPath != null && encodingHeader != null &&
                filePath.endsWith(".jar") && encodingHeader.contains("pack200-gzip")) {
            ServletContext context = filterConfig.getServletContext();
            String resourceName = filePath.substring(contextPath.length());

            int dirIndex = resourceName.lastIndexOf('/');
            if (dirIndex > 0) {
                Set resources = context.getResourcePaths(resourceName.substring(0, dirIndex+1));
                String pack200Resource = resourceName + ".pack.gz";
                if (resources.contains(pack200Resource)) {
                    RequestDispatcher dispatcher = context.getRequestDispatcher(pack200Resource);
                    if (dispatcher != null) {
                        handled = true;
                        hresp.addHeader("Content-Encoding", "pack200-gzip");
                        dispatcher.forward(hreq, hresp);
                    }
                }
            }
        }

        return handled;
    }    
}
