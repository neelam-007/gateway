/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.admin;

import com.l7tech.admin.AdminLogin;
import com.l7tech.admin.AdminLoginResult;
import com.l7tech.common.LicenseException;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.audit.ServiceMessages;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.HttpServletResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ClassUtils;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.server.event.system.AdminAppletEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.AssertionModule;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.spring.remoting.RemoteUtils;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;

import javax.security.auth.login.LoginException;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication filter for the Manager applet servlet (and serving the manager applet jarfiles).
 */
public class ManagerAppletFilter implements Filter {
    private static final Logger logger = Logger.getLogger(ManagerAppletFilter.class.getName());
    public static final String RELOGIN = "Relogin due to incorrect username or password";
    public static final String PROP_CREDS = "ManagerApplet.authenticatedCredentials";
    public static final String PROP_USER = "ManagerApplet.authenticatedUser";
    public static final String SESSION_ID_COOKIE_NAME = "sessionId";
    public static final String DEFAULT_CODEBASE_PREFIX = "/ssg/webadmin/";

    private enum AuthResult { OK, CHALLENGED, FAIL }

    private FilterConfig filterConfig;
    private WebApplicationContext applicationContext;
    private CustomAssertionsRegistrar customAssertionsRegistrar;
    private ServerAssertionRegistry assertionRegistry;
    private AdminSessionManager adminSessionManager;
    private AdminLogin adminLogin;
    private AuditContext auditContext;
    private SoapFaultManager soapFaultManager;

    private ServerAssertion dogfoodPolicy;
    private Document fakeDoc;
    private String codebasePrefix;

    private Object getBean(String name, Class clazz) throws ServletException {
        try {
            final Object o = applicationContext.getBean(name, clazz);
            if (o == null) throw new ServletException("Configuration error; could not find bean " + name);
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
        assertionRegistry = (ServerAssertionRegistry)getBean("assertionRegistry", ServerAssertionRegistry.class);
        adminSessionManager = (AdminSessionManager)getBean("adminSessionManager", AdminSessionManager.class);
        adminLogin = (AdminLogin)getBean("adminLogin", AdminLogin.class);
        auditContext = (AuditContext)getBean("auditContext", AuditContext.class);
        soapFaultManager = (SoapFaultManager)getBean("soapFaultManager", SoapFaultManager.class);

        CompositeAssertion dogfood = new AllAssertion();
        dogfood.addChild(new SslAssertion(false));
        fakeDoc = XmlUtil.createEmptyDocument("placeholder", "l7", "http://www.l7tech.com/ns/placeholder");

        try {
            dogfoodPolicy = serverPolicyFactory.compilePolicy(dogfood, false);
        } catch (ServerPolicyException e) {
            throw new ServletException("Configuration error; could not compile dogfood policy", e);
        } catch (LicenseException e) {
            throw new RuntimeException(e); // can't happen: passed false for licenseEnforcement
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
                filterConfig.getServletContext().getNamedDispatcher("ssgLoginFormServlet").include(hreq, hresp);
                return;
            }

            if (!hreq.isSecure()) {
                auditor.logAndAudit(ServiceMessages.APPLET_AUTH_NO_SSL);
                hresp.setStatus(status = 404);
                hresp.sendError(404, "Request must arrive over SSL.");
                return;
            }

            SsgConnector connector = HttpTransportModule.getConnector(hreq);
            if (connector == null || !connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_APPLET)) {
                auditor.logAndAudit(ServiceMessages.APPLET_AUTH_NO_SSL);
                hresp.setStatus(status = 404);
                hresp.sendError(404, "Service not enabled on this port.");
                return;
            }

            passed = true;

            // Note that the user is authenticated before this is run
            if (handleJarRequest(hreq, hresp))
                return;

            if (handleAssertionModuleClassRequest(hreq, hresp, auditor))
                return;

            if (handleCustomAssertionClassRequest(hreq, hresp, auditor))
                return;

            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_FILTER_PASSED);
            final IOException[] ioeHolder = new IOException[1];
            final ServletException[] seHolder = new ServletException[1];
            RemoteUtils.runWithConnectionInfo(hreq.getRemoteAddr(), hreq, new Runnable(){
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
    private AuthResult authenticate(HttpServletRequest hreq, HttpServletResponse hresp, PolicyEnforcementContext context, Auditor auditor) throws ServletException, IOException {
        // Check if already auth'd
        if (hreq.getAttribute(ManagerAppletFilter.PROP_USER) != null) {
            // we've already seen this request (dispatched)
            return AuthResult.OK;
        }

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
                        context.addCredentials(creds);
                        hreq.setAttribute(PROP_CREDS, creds);
                        hreq.setAttribute(PROP_USER, user);
                        hreq.setAttribute(ManagerAppletFilter.SESSION_ID_COOKIE_NAME, sessionId);
                        auditor.logAndAudit(ServiceMessages.APPLET_AUTH_COOKIE_SUCCESS, getName(user));
                        return AuthResult.OK;
                    }
                }
            }
        }

        try {
            final AssertionStatus result = dogfoodPolicy.checkRequest(context);
            if (result == AssertionStatus.NONE) {
                String username = hreq.getParameter("username");
                String password = hreq.getParameter("password");
                if (username == null || password == null) {
                    filterConfig.getServletContext().getNamedDispatcher("ssgLoginFormServlet").include(hreq, hresp);
                    return AuthResult.CHALLENGED;
                }

                // Check authentication
                AdminLoginResult loginResult = adminLogin.login(username, password);
                final User user = loginResult.getUser();
                auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_SUCCESS, getName(user));

                // Establish a new admin session for the authenticated user
                String sessionId = adminSessionManager.createSession(user);
                auditor.logAndAudit(ServiceMessages.APPLET_SESSION_CREATED, getName(user));

                Cookie sessionCookie = new Cookie(ManagerAppletFilter.SESSION_ID_COOKIE_NAME, sessionId);
                sessionCookie.setSecure(true);
                hresp.addCookie(sessionCookie);

                hresp.sendRedirect(hreq.getRequestURI());
                return AuthResult.CHALLENGED;
            }
        } catch (LoginException e) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_FAILED, ExceptionUtils.getMessage(e));
            // Fall through and either challenge or send error message
            logger.log(Level.FINE, "Error authenticating administrator, " + ExceptionUtils.getMessage(e), e);

            // For the case - incorrect username and password
            hreq.setAttribute(RELOGIN, "YES");
            return AuthResult.FAIL;
        } catch (PolicyAssertionException e) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_FAILED, ExceptionUtils.getMessage(e));
            // Fall through and either challenge or send error message
            logger.log(Level.FINE, "Error authenticating administrator, " + ExceptionUtils.getMessage(e), e);
        }
        // Ensure that we send back a challenge withour 401
        HttpServletResponseKnob hsrespKnob =
                (HttpServletResponseKnob)context.getResponse().getKnob(HttpServletResponseKnob.class);

        hsrespKnob.beginChallenge();
        sendChallenge(hreq, hresp);
        auditor.logAndAudit(ServiceMessages.APPLET_AUTH_CHALLENGE);
        return AuthResult.CHALLENGED;
    }

    private void sendChallenge(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException
    {
        ServletOutputStream sos = null;
        try {
            // the challenge http header is supposed to already been appended at that point-ah
            hresp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            sos = hresp.getOutputStream();
            sos.print("Authentication Required");
            filterConfig.getServletContext().getNamedDispatcher("ssgLoginFormServlet").include(hreq, hresp);
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
            String className = pathToClassname(contextPath, filePath);
            if (className != null) {
                byte[] data = customAssertionsRegistrar.getAssertionClass(className);
                if (data != null) {
                    handled = true;
                    auditor.logAndAudit(ServiceMessages.APPLET_AUTH_CLASS_DOWNLOAD, className);
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
    private String pathToClassname(final String contextPath, final String filePath) {
        String name = null;

        int prefixLength = contextPath==null ? 0 : contextPath.length();
        prefixLength += codebasePrefix.length();
        if (prefixLength+6 < filePath.length()) {
            String className = filePath.substring(prefixLength);
            className = ClassUtils.stripSuffix(className, ".class");
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
     * @throws java.io.IOException  if there's a problem reading the jar or sending the info
     * @throws javax.servlet.ServletException  if there is some other error
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

    private static final Pattern STRIPCLASS = Pattern.compile("\\/[^/]+$");

    private boolean handleAssertionModuleClassRequest(final HttpServletRequest hreq,
                                                      final HttpServletResponse hresp,
                                                      final Auditor auditor)
            throws IOException, ServletException
    {

        String filePath = hreq.getRequestURI();
        String contextPath = hreq.getContextPath();

        if (filePath == null || !filePath.startsWith(contextPath))
            return false;

        boolean handled = false;

        int prefixLength = contextPath==null ? 0 : contextPath.length();
        prefixLength += codebasePrefix.length();
        if (prefixLength < filePath.length()) {
            filePath = filePath.substring(prefixLength);
        }

        String packageName = STRIPCLASS.matcher(filePath).replaceAll("").replace('/', '.');
        Set<AssertionModule> possibleModules = findAssertionModulesThatOfferPackage(packageName);

        if (possibleModules == null || possibleModules.isEmpty())
            return false;


        for (AssertionModule module : possibleModules) {
            byte[] data = module.getResourceBytes(filePath);
            if (data != null) {
                handled = true;
                auditor.logAndAudit(ServiceMessages.APPLET_AUTH_MODULE_CLASS_DL, filePath, module.getName());
                sendClass(hresp, data);
            }
        }

        return handled;
    }

    private Set<AssertionModule> findAssertionModulesThatOfferPackage(String packageName) {
        Set<AssertionModule> ret = new HashSet<AssertionModule>();
        Set<AssertionModule> mods = assertionRegistry.getLoadedModules();
        for (AssertionModule mod : mods)
            if (mod.offersPackage(packageName))
                ret.add(mod);
        return ret;
    }
}
