/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */

package com.l7tech.server.admin.ws;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.admin.AdminLoginResult;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.*;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.event.system.AdminWebServiceEvent;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.util.DelegatingServletInputStream;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.security.auth.Subject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intercepts requests to admin web services (currently implemented with XFire, but should be completely transparent)
 * and does basic WSS processing and policy enforcement before passing them along
 */
public class AdminWebServiceFilter implements Filter {
    private WebApplicationContext applicationContext;
    private AuditContext auditContext;
    private ServerAssertion adminPolicy;
    private SecurityTokenResolver securityTokenResolver;
    private StashManagerFactory stashManagerFactory;
    private AdminLogin adminLogin;

    private static final Logger log = Logger.getLogger(AdminWebServiceFilter.class.getName());
    private static final String ERR_PREFIX = "Configuration error; could not get ";

    @SuppressWarnings({ "unchecked" })
    private <T> T getBean(ApplicationContext context, String name, String desc, Class<T> clazz) throws ServletException {
        try {
            T bean = (T) context.getBean(name, clazz);
            if (bean == null) throw new ServletException(ERR_PREFIX + desc);
            return bean;
        }
        catch(BeansException be) {
            throw new ServletException(ERR_PREFIX + desc, be);
        }
    }

    @Override
    public void init( final FilterConfig filterConfig ) throws ServletException {
        // Constructs a policy for all admin web services.
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (applicationContext == null) {
            throw new ServletException(ERR_PREFIX + "application context");
        }

        auditContext = getBean(applicationContext, "auditContext", "audit context", AuditContext.class);
        securityTokenResolver = getBean(applicationContext, "securityTokenResolver", "certificate resolver", SecurityTokenResolver.class);
        stashManagerFactory = getBean(applicationContext, "stashManagerFactory", "stashManagerFactory", StashManagerFactory.class);
        adminLogin = getBean(applicationContext, "adminLogin", "adminLogin", AdminLogin.class);

        final AllAssertion policy = new AllAssertion(Arrays.<Assertion>asList(
            new OneOrMoreAssertion(Arrays.<Assertion>asList(
                new AllAssertion(Arrays.<Assertion>asList(
                    new SslAssertion(false),
                    new HttpBasic())),
                new SslAssertion(true), // With client cert
                new AllAssertion(Arrays.<Assertion>asList(
                    new RequireWssX509Cert(),
                    new RequireWssSignedElement())),
                new SecureConversation()))));

        final ServerPolicyFactory policyFactory = (ServerPolicyFactory) applicationContext.getBean("policyFactory");
        if (policyFactory == null) {
            throw new ServletException(ERR_PREFIX + "ServerPolicyFactory");
        }
        try {
            // Bypass license enforcement for dogfood policies -- it can use any assertion it wants
            adminPolicy = policyFactory.compilePolicy(policy, false);
        } catch (ServerPolicyException e) {
            log.log(Level.SEVERE, "Unable to instantiate admin service policy: " + ExceptionUtils.getMessage(e), e);
            // fallthrough and complain
        } catch ( LicenseException e) {
            throw new RuntimeException(e); // can't happen, we said no license enforcement
        }
        if (adminPolicy == null) {
            throw new ServletException(ERR_PREFIX + "policy");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        final Message response = new Message();
        final Message request = new Message();

        final String rawct = servletRequest.getContentType();
        final ContentTypeHeader ctype = rawct != null && rawct.length() > 0
            ? ContentTypeHeader.parseValue(rawct)
            : ContentTypeHeader.XML_DEFAULT;

        servletResponse.setContentType(ContentTypeHeader.XML_DEFAULT.getFullValue());

        if (!(servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse)) {
            throw new ServletException("Only HTTP requests are supported");
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        if("get".equalsIgnoreCase(httpServletRequest.getMethod())) {
            if ("wsdl".equalsIgnoreCase(httpServletRequest.getQueryString())) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            else { // only allow GET for WSDL
                httpServletResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
        }

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(httpServletRequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(httpServletResponse);
        response.attachHttpResponseKnob(respKnob);

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        AssertionStatus status = null;
        try {
            request.initialize(stashManagerFactory.createStashManager(), ctype, servletRequest.getInputStream());

            trogdor(context, request);

            User authenticatedUser = null;
            // Get credentials from dogfood policy
            status = adminPolicy.checkRequest(context);
            if (status == AssertionStatus.NONE) {
                if (authContext.getLastCredentials() != null) {
                    try {
                        // Try to authenticate using same rules as SSM login
                        // (i.e. must be assigned to a Role with at least one RBAC permission)
                        final LoginCredentials credentials = authContext.getLastCredentials();
                        AdminLoginResult loginResult =
                                adminLogin.login(credentials.getLogin(), new String(credentials.getCredentials()));

                        authenticatedUser = loginResult.getUser();
                    } catch (Exception e) {
                        log.log(Level.INFO, "Authentication failed", e);
                        status = AssertionStatus.UNAUTHORIZED;
                    }
                }
            }

            context.setPolicyResult(status);
            if (status == AssertionStatus.NONE && authenticatedUser != null) {
                // Pass it along to XFire
                final HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpServletRequest) {
                    @Override
                    public ServletInputStream getInputStream() throws IOException {
                        try {
                            final InputStream is = request.getMimeKnob().getEntireMessageBodyAsInputStream();
                            return new DelegatingServletInputStream(is);
                        } catch (NoSuchPartException e) {
                            throw new IOException("Couldn't get InputStream"); // Very unlikely
                        }
                    }
                };

                Set<Principal> principals = new HashSet<Principal>();
                principals.add(authenticatedUser);
                Subject subject = new Subject(true, principals, Collections.emptySet(), Collections.emptySet());
                Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        filterChain.doFilter(wrapper, servletResponse);
                        return null;
                    }
                });
                respKnob.beginResponse();
            } else {
                respKnob.beginResponse();
                if (respKnob.hasChallenge()) {
                    // 401, not a fault
                    respKnob.beginChallenge();
                    httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    // 500, fault
                    throw new ServletException("Authentication Required");
                }
            }
        } catch (Exception e) {
            ServletOutputStream out = null;
            try {
                log.log(Level.WARNING, "Admin Web Service request failed", e);
                httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out = servletResponse.getOutputStream();
                Document fault = SoapFaultUtils.generateSoapFaultDocument(
                        SoapVersion.UNKNOWN,
                        SoapUtil.FC_CLIENT,
                        e.getMessage(),
                        null,
                        ((HttpServletRequest)servletRequest).getRequestURL().toString());
                XmlUtil.nodeToOutputStream(fault.getDocumentElement(), out, ContentTypeHeader.XML_DEFAULT.getEncoding().name());
            } catch (SAXException e1) {
                throw new ServletException(e1); // Can't happen really
            } finally {
                ResourceUtils.closeQuietly(out);
            }
        } finally {
            try {
                String message = "Administration Web Service";
                if (status != null && status != AssertionStatus.NONE) message += ": " + status.getMessage();
                User user = getUser(authContext);
                applicationContext.publishEvent(new AdminWebServiceEvent(this, Level.INFO, servletRequest.getRemoteAddr(), message, user.getProviderId(), getName(user), user.getId()));
            } catch(Exception se) {
                log.log(Level.WARNING, "Error dispatching event.", se);
            } finally {
                auditContext.flush();
                context.close();
            }
        }
    }

    private void trogdor(PolicyEnforcementContext context, Message request) throws IOException, SAXException, InvalidDocumentFormatException, ProcessorException, NoSuchPartException {
        // WSS-Processing Step
        boolean isSoap = context.getRequest().isSoap();
        boolean hasSecurity = isSoap && context.getRequest().getSoapKnob().isSecurityHeaderPresent();

        if (isSoap && hasSecurity) {
            WssProcessor trogdor = new WssProcessorImpl(); // no need for locator
            try {
                final SecurityKnob reqSec = request.getSecurityKnob();
                ProcessorResult wssOutput = trogdor.undecorateMessage(request,
                                                      null,
                                                      SecureConversationContextManager.getInstance(),
                                                      securityTokenResolver);
                reqSec.setProcessorResult(wssOutput);
            } catch (GeneralSecurityException e) {
                throw new ProcessorException(e);
            } catch (BadSecurityContextException e) {
                throw new ProcessorException(e);
            }
        }
    }

    private User getUser(AuthenticationContext context) {
        User user = null;

        if(context.isAuthenticated()) {
            user = context.getLastAuthenticatedUser();
        }

        if(user==null) {
            user = new UserBean();
        }

        return user;
    }

    private String getName(User user) {
        return user.getName()!=null ? user.getName() : user.getLogin();
    }

    @Override
    public void destroy() {
    }
}
