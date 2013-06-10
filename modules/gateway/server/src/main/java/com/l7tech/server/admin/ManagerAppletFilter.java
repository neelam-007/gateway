package com.l7tech.server.admin;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.admin.AdminLoginResult;
import com.l7tech.gateway.common.audit.ServiceMessages;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.*;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.system.AdminAppletEvent;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.AssertionModule;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.*;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.LoginException;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Authentication filter for the Manager applet servlet (and serving the manager applet jarfiles).
 */
public class ManagerAppletFilter implements Filter {
    private static final Logger logger = Logger.getLogger(ManagerAppletFilter.class.getName());
    public static final String RELOGIN = "Relogin due to incorrect username or password";
    public static final String INVALID_CERT = "Relogin due to incorrect certificate";
    public static final String CREDS_EXPIRED = "Relogin due to expired credentials";
    public static final String INVALID_PASSWORD = "Relogin due to invalid password.";
    public static final String REQUIRE_CERT_LOGIN = "REQUIRE.CERT.LOGIN";
    public static final String PROP_CREDS = "ManagerApplet.authenticatedCredentials";
    public static final String PROP_USER = "ManagerApplet.authenticatedUser";
    public static final String SESSION_ID_COOKIE_NAME = "sessionId";
    public static final String DEFAULT_CODEBASE_PREFIX = "/ssg/webadmin/applet/";
    public static final String USERNAME = "username";
    public static final String INVALID_PASSWORD_MESSAGE = "invalidPasswordMessage";

    private static final Map<String,String[]> RESOURCES = Collections.unmodifiableMap( new HashMap<String,String[]>(){
        {
            put( "/ssg/webadmin/favicon.ico", new String[]{"/com/l7tech/server/resources/favicon.ico", "image/png"} );
            put( "/ssg/webadmin/layer7_logo_small_32x32.png", new String[]{"/com/l7tech/server/resources/layer7_logo_small_32x32.png", "image/png"} );
         }
    } );

    private enum AuthResult { OK, CHALLENGED, FAIL }

    private FilterConfig filterConfig;
    private WebApplicationContext applicationContext;
    private CustomAssertionsRegistrar customAssertionsRegistrar;
    private ServerAssertionRegistry assertionRegistry;
    private AdminSessionManager adminSessionManager;
    private AdminLogin adminLogin;
    private AdminLoginHelper adminLoginHelper;

    private ServerAssertion dogfoodPolicy;
    private Document fakeDoc;
    private String codebasePrefix;

    private <T> T getBean(String name, Class<T> clazz) throws ServletException {
        try {
            final T o = applicationContext.getBean(name, clazz);
            if (o == null) throw new ServletException("Configuration error; could not find bean " + name);
            return o;
        } catch (BeansException beansException) {
            throw new ServletException("Configuration error; could not get bean " + name, beansException);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        customAssertionsRegistrar = getBean("customAssertionRegistrar", CustomAssertionsRegistrar.class);
        ServerPolicyFactory serverPolicyFactory = getBean("policyFactory", ServerPolicyFactory.class);
        assertionRegistry = getBean("assertionRegistry", ServerAssertionRegistry.class);
        adminSessionManager = getBean("adminSessionManager", AdminSessionManager.class);
        adminLogin = getBean("adminLogin", AdminLogin.class);
        adminLoginHelper = getBean("adminLoginHelper", AdminLoginHelper.class);

        //CompositeAssertion dogfood = new AllAssertion();
        CompositeAssertion dogfood = new OneOrMoreAssertion();
        dogfood.addChild(new SslAssertion(true));
        dogfood.addChild(new SslAssertion(false));
        fakeDoc = XmlUtil.createEmptyDocument("placeholder", "l7", "http://www.l7tech.com/ns/placeholder");

        try {
            Assertion.filterOutDisabledAssertions(dogfood);
            dogfoodPolicy = serverPolicyFactory.compilePolicy(dogfood, false);
        } catch (ServerPolicyException e) {
            throw new ServletException("Configuration error; could not compile dogfood policy", e);
        } catch (LicenseException e) {
            throw new RuntimeException(e); // can't happen: passed false for licenseEnforcement
        }

        String codebasePrefix = filterConfig.getInitParameter("codebaseprefix");
        if (codebasePrefix == null)
            codebasePrefix = DEFAULT_CODEBASE_PREFIX;
        if (!codebasePrefix.endsWith("/")) {
            codebasePrefix += "/";
        }
        this.codebasePrefix = codebasePrefix;
    }

    @Override
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
            // Check if there is a ADMIN_APPLET permission.
            SsgConnector connector = HttpTransportModule.getConnector(hreq);
            if (connector == null || !connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_APPLET)) {
                auditor.logAndAudit(ServiceMessages.APPLET_AUTH_PORT_NOT_ALLOWED);
                hresp.setStatus(status = 403);
                hresp.sendError(403, "Admin applet requests not permitted on this port.");
                return;
            }
            
            // Note that the user is NOT authenticated for resource request
            if (handleResourceRequest(hreq, hresp)) {
                passed = true;
                return;
            }
            long maxBytes = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT,Message.getMaxBytes());

            Message request = new Message();
            request.initialize(fakeDoc,maxBytes);
            request.attachHttpRequestKnob(new HttpServletRequestKnob(hreq));

            Message response = new Message();
            HttpServletResponseKnob hsrespKnob = new HttpServletResponseKnob(hresp);
            response.attachHttpResponseKnob(hsrespKnob);

            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            AuthResult authResult = authenticate(hreq, hresp, context, auditor);
            if (authResult == AuthResult.CHALLENGED) {
                // Already audited a detail message and sent challenge
                passed = true;
                return;
            }

            if (authResult != AuthResult.OK) {
                if (isClasspathResourceRequest(hreq)) {
                    hresp.setStatus(403);
                    hresp.sendError(403);
                } else if ((hreq.getAttribute(CREDS_EXPIRED) != null || hreq.getAttribute(INVALID_PASSWORD) != null) && hreq.getParameter("Cancel") == null) {
                    //redirect to the login and password change page
                    filterConfig.getServletContext().getNamedDispatcher("ssgLoginAndPasswordUpdateFormServlet").include(hreq, hresp);
                } else {
                    filterConfig.getServletContext().getNamedDispatcher("ssgLoginFormServlet").include(hreq, hresp);
                }
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
            if (handleJarRequest(hreq, hresp))
                return;

            if (handleAssertionModuleClassRequest(hreq, hresp, auditor))
                return;

            if (handleCustomAssertionClassRequest(hreq, hresp, auditor))
                return;

            if (handleCustomAssertionResourceRequest(hreq, hresp)) {
                return;
            }

            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_FILTER_PASSED);
            final IOException[] ioeHolder = new IOException[1];
            final ServletException[] seHolder = new ServletException[1];
            RemoteUtils.runWithConnectionInfo(hreq.getRemoteAddr(), hreq, new Runnable(){
                @Override
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
                    message = "Applet request filter failed: status = " + status;
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
            } finally {
                if (context != null) context.close();
            }
        }
    }

    private static String getName(User user) {
        return user.getName() == null ? user.getLogin() : user.getName();
    }

    @Override
    public void destroy() {
        // No action required at this time
    }

    // If this method returns, an audit detail message has been added.
    private AuthResult authenticate(HttpServletRequest hreq, HttpServletResponse hresp, PolicyEnforcementContext context, Auditor auditor) throws ServletException, IOException {
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();

        // Check if already auth'd
        if (hreq.getAttribute(ManagerAppletFilter.PROP_USER) != null) {
            // we've already seen this request (dispatched)
            return AuthResult.OK;
        }

        // clear cookie and return to filter
        if ( hreq.getParameter("logout") != null || hreq.getParameter("Cancel") != null) {
            Cookie sessionCookie = new Cookie(ManagerAppletFilter.SESSION_ID_COOKIE_NAME, "");
            sessionCookie.setSecure(true);
            sessionCookie.setMaxAge(0);
            hresp.addCookie(sessionCookie);
            hresp.sendRedirect(hreq.getRequestURI());
            return AuthResult.CHALLENGED;
        }

        // Check for provided session ID and, if its valid and arrived over SSL, bypass authentication
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) {
            if (SESSION_ID_COOKIE_NAME.equalsIgnoreCase(cookie.getName())) {
                String sessionId = cookie.getValue();
                if (sessionId != null && sessionId.length() > 0 && hreq.isSecure()) {
                    Object sessionInfo = adminSessionManager.getSessionInfo(sessionId);
                    if ((sessionInfo instanceof AdditionalSessionInfo) &&
                            (hreq.getServerPort() == ((AdditionalSessionInfo)sessionInfo).port)) {
                        User user = null;
                        try {
                            user = adminSessionManager.resumeSession(sessionId);
                        } catch ( AuthenticationException ae ) {
                            logger.log( Level.WARNING, "Session resume failed, user has insufficient permissions.", ExceptionUtils.getDebugException(ae) );    
                        } catch ( ObjectModelException fe ) {
                            logger.log( Level.WARNING, "Error resuming session.", fe );
                        }

                        if ( user != null ) {
                            LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                                    new OpaqueSecurityToken(user.getLogin(), sessionId.toCharArray()), 
                                    CookieCredentialSourceAssertion.class);
                            authContext.addCredentials(creds);
                            hreq.setAttribute(PROP_CREDS, creds);
                            hreq.setAttribute(PROP_USER, user);
                            hreq.setAttribute(ManagerAppletFilter.SESSION_ID_COOKIE_NAME, sessionId);
                            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_COOKIE_SUCCESS, getName(user));
                            return AuthResult.OK;
                        }
                    }
                }
            }
        }

        final boolean isPost = "POST".equals( hreq.getMethod() );
        String username = isPost ? hreq.getParameter("username") : null;
        String password = isPost ? hreq.getParameter("password") : null;
        String newPassword = isPost ? hreq.getParameter("new_password") : null;
        String confirmPassword = isPost ? hreq.getParameter("confirm_password") : null;
        try {
            final AssertionStatus result = dogfoodPolicy.checkRequest(context);
            if (result == AssertionStatus.NONE) {
                if ( (username == null || password == null) && (authContext.isAuthenticationMissing() || authContext.getLastCredentials() == null) ){
                    if (isClasspathResourceRequest(hreq)) {
                        hresp.setStatus(403);
                        hresp.sendError(403);
                    } else if (newPassword != null || confirmPassword != null ) {
                        //redirect to the login and password change page
                        filterConfig.getServletContext().getNamedDispatcher("ssgLoginAndPasswordUpdateFormServlet").include(hreq, hresp);
                    } else {
                        filterConfig.getServletContext().getNamedDispatcher("ssgLoginFormServlet").include(hreq, hresp);
                    }
                    return AuthResult.CHALLENGED;
                }

                //new password and confirm password don't match, do not allow them to proceed
                if (username != null && (newPassword != null || confirmPassword != null) && (newPassword==null || !(newPassword.equals(confirmPassword))) ) {
                    throw new InvalidPasswordException("New password does not match with confirm password.  Please retry.");
                }

                // Check authentication
                AdminLoginResult loginResult;
                if ( newPassword != null || confirmPassword != null) {
                    //perform change password and login
                    loginResult = adminLogin.loginWithPasswordUpdate(username, password, newPassword);
                } else if ( username != null || password != null ){
                    loginResult = adminLogin.login(username, password);
                } else {
                    loginResult = adminLoginHelper.login(authContext.getLastCredentials().getClientCert());
                }

                final User user = loginResult.getUser();
                auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_SUCCESS, getName(user));

                // Establish a new admin session for the authenticated user
                AdditionalSessionInfo sessionInfo = new AdditionalSessionInfo();
                sessionInfo.port = hreq.getServerPort();
                String sessionId = adminSessionManager.createSession(user, sessionInfo);
                auditor.logAndAudit(ServiceMessages.APPLET_SESSION_CREATED, getName(user));

                Cookie sessionCookie = new Cookie(ManagerAppletFilter.SESSION_ID_COOKIE_NAME, sessionId);
                sessionCookie.setSecure(true);
                hresp.addCookie(sessionCookie);

                hresp.sendRedirect(hreq.getRequestURI());
                return AuthResult.CHALLENGED;
            }
        } catch (AccountLockedException lae) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_FAILED, ExceptionUtils.getMessage(lae));
            // Fall through and either challenge or send error message
            logger.log(Level.FINE, "Error authenticating administrator, " + ExceptionUtils.getMessage(lae), lae);
            hreq.setAttribute(RELOGIN, "NO");
            return AuthResult.FAIL;
        } catch (LoginException e) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_FAILED, ExceptionUtils.getMessage(e));
            // Fall through and either challenge or send error message
            logger.log(Level.FINE, "Error authenticating administrator, " + ExceptionUtils.getMessage(e), e);

            // For the case - incorrect username and password
            if (username != null || password != null) {
                if (ExceptionUtils.causedBy(e, CredentialExpiredPasswordDetailsException.class)) {
                    //credentials expired so we'll need to redirect to proper page to change password and login
                    hreq.setAttribute(CREDS_EXPIRED, "YES");
                    hreq.setAttribute(USERNAME, username);
                } else if (ExceptionUtils.causedBy(e, LoginRequireClientCertificateException.class)) {
                    hreq.setAttribute(REQUIRE_CERT_LOGIN, "YES");
                } else if (newPassword != null || confirmPassword != null) {
                    //need to redirect back to the change password and login page
                    hreq.setAttribute(INVALID_PASSWORD, "YES");
                    hreq.setAttribute(USERNAME, username);
                    hreq.setAttribute(INVALID_PASSWORD_MESSAGE, e.getMessage());
                } else {
                    hreq.setAttribute(RELOGIN, "YES");
                }
            } else {
                hreq.setAttribute(INVALID_CERT, "YES");
            }
            return AuthResult.FAIL;
        } catch (AccessControlException ace) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_FAILED, ExceptionUtils.getMessage(ace));
            // Fall through and either challenge or send error message
            logger.log(Level.FINE, "Error authenticating administrator, " + ExceptionUtils.getMessage(ace), ace);

            // For the case - incorrect username and password
            if (username != null || password != null) {
                hreq.setAttribute(RELOGIN, "YES");
            } else {
                hreq.setAttribute(INVALID_CERT, "YES");
            }
            return AuthResult.FAIL;
        } catch (InvalidPasswordException ipe) {
            //attempt to change password but was invalid (not compliant to standards), need to re-enter new password
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_FAILED, ExceptionUtils.getMessage(ipe));
            logger.log(Level.FINE, "Error changing password, " + ExceptionUtils.getMessage(ipe), ipe);
            hreq.setAttribute(INVALID_PASSWORD, "YES");
            hreq.setAttribute(USERNAME, username);
            hreq.setAttribute(INVALID_PASSWORD_MESSAGE, formatErrors(ipe));
            return AuthResult.FAIL;
        } catch (PolicyAssertionException e) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_POLICY_FAILED, ExceptionUtils.getMessage(e));
            // Fall through and either challenge or send error message
            logger.log(Level.FINE, "Error authenticating administrator, " + ExceptionUtils.getMessage(e), e);
        }

        // Request rejected (one of cases is using http rather than https):
        if (!hreq.isSecure()) {
            auditor.logAndAudit(ServiceMessages.APPLET_AUTH_NO_SSL);
            hresp.sendError(403, "Request must arrive using HTTPS.");
        } else {
            hresp.sendError(403);
        }
        return AuthResult.CHALLENGED;
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
    final protected boolean handleCustomAssertionClassRequest(final HttpServletRequest hreq,
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
     * Handle request for custom assertion resources.
     *
     * <p>The user MUST be authenticated before calling this method.</p>
     *
     * @param hreq  the HttpServletRequest
     * @param hresp the HttpServletResponse
     * @return true if the request has been handled (so no further action should be taken)
     * @throws IOException if there is a problem loading or transmitting the class
     */
    final protected boolean handleCustomAssertionResourceRequest (final HttpServletRequest hreq,
                                                                  final HttpServletResponse hresp) throws IOException {
        boolean handled = false;

        String filePath = hreq.getRequestURI();
        String contextPath = hreq.getContextPath();

        if (filePath != null && contextPath != null) {
            String resourceName = filePath.substring(contextPath.length());
            if (filePath.startsWith(contextPath) && !resourceName.endsWith(".class")) {
                if (resourceName.startsWith(DEFAULT_CODEBASE_PREFIX)) {
                    // Remove the codebase prefix
                    //
                    resourceName = resourceName.substring(DEFAULT_CODEBASE_PREFIX.length(), resourceName.length());
                }
                byte[] data = customAssertionsRegistrar.getAssertionResourceBytes(resourceName);
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
     * Handle request for static resource file.
     *
     * <p>The user is possibly NOT authenticated for this method.</p>
     *
     * @param hreq The HttpServletRequest
     * @param hresp The HttpServletResponse
     * @return true if the request has been handled (so no further action should be taken)
     * @throws java.io.IOException  if there's a problem reading the jar or sending the info
     * @throws javax.servlet.ServletException  if there is some other error
     */
    private boolean handleResourceRequest(final HttpServletRequest hreq,
                                          final HttpServletResponse hresp) throws IOException, ServletException {
        boolean handled = false;

        String filePath = hreq.getRequestURI();
        String contextPath = hreq.getContextPath();

        if ( filePath != null && contextPath != null ) {
            String resourceName = filePath.substring(contextPath.length());

            if ( "/ssg/webadmin".equals( resourceName ) ) {
                handled = true;
                hresp.sendRedirect( new URL( new URL( hreq.getRequestURL().toString() ), "webadmin/" ).toExternalForm() );
            } else if ( RESOURCES.containsKey( resourceName ) ) {
                String[] RESOURCE_PATH_AND_TYPE = RESOURCES.get( resourceName );
                InputStream resourceIn = ManagerAppletFilter.class.getResourceAsStream( RESOURCE_PATH_AND_TYPE[0] );
                if ( resourceIn != null ) {
                    try {
                        handled = true;
                        hresp.setContentType( RESOURCE_PATH_AND_TYPE[1] );
                        IOUtils.copyStream( resourceIn, hresp.getOutputStream() );
                    } finally {
                        ResourceUtils.closeQuietly( resourceIn );
                    }
                }
            }
        }

        return handled;
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
                if ( resources != null && resources.contains(pack200Resource) ) {
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

    private boolean isClasspathResourceRequest(final HttpServletRequest hreq) {
        String filePath = hreq.getRequestURI();
        String contextPath = hreq.getContextPath();
        return filePath != null && filePath.startsWith(contextPath) && filePath.contains(codebasePrefix);
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
            byte[] data = module.getResourceBytes(filePath, true);
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

    public static String formatErrors( final InvalidPasswordException e ) {
        if ( e.getPasswordErrors().isEmpty() ) {
            return "Invalid password '"+e.getMessage()+"'.";
        } else if ( e.getPasswordErrors().size() == 1 ) {
            return "Invalid password '"+e.getPasswordErrors().iterator().next()+"'.";
        } else {
            return "<div style=\"text-align: left\">Invalid password:<ul><li>" + TextUtils.join( "</li><li>", e.getPasswordErrors() ) + "</li></ul></div>";
        }
    }

    /**
     * The class stores additional session information such as port, etc.
     * If there are more additional info later on, you can add them as instance variables into the class.
     */
    private static class AdditionalSessionInfo {
        private int port;
    }
}
