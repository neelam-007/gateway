/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */

package com.l7tech.server.admin.ws;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.event.system.AdminWebServiceEvent;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intercepts requests to admin web services (currently implemented with XFire, but should be completely transparent)
 * and does basic WSS processing and policy enforcement before passing them along
 */
public class AdminWebServiceFilter implements Filter {
    private WebApplicationContext applicationContext;
    private AuditContext auditContext;
    private SoapFaultManager soapFaultManager;
    private ServerAssertion adminPolicy;
    private X509Certificate serverCertificate;
    private PrivateKey serverPrivateKey;
    private SecurityTokenResolver securityTokenResolver;

    private static final Logger log = Logger.getLogger(AdminWebServiceFilter.class.getName());
    private static final String ERR_PREFIX = "Configuration error; could not get ";

    private Object getBean(ApplicationContext context, String name, String desc, Class clazz) throws ServletException {
        try {
            Object bean = context.getBean(name, clazz);
            if (bean == null) throw new ServletException(ERR_PREFIX + desc);
            return bean;
        }
        catch(BeansException be) {
            throw new ServletException(ERR_PREFIX + desc, be);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // Constructs a policy for all admin web services.
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (applicationContext == null) {
            throw new ServletException(ERR_PREFIX + "application context");
        }

        auditContext = (AuditContext)getBean(applicationContext, "auditContext", "audit context", AuditContext.class);
        soapFaultManager = (SoapFaultManager)getBean(applicationContext, "soapFaultManager", "soapFaultManager", SoapFaultManager.class);
        serverPrivateKey = (PrivateKey)getBean(applicationContext, "sslKeystorePrivateKey", "server private key", PrivateKey.class);
        serverCertificate = (X509Certificate)getBean(applicationContext, "sslKeystoreCertificate", "server certificate", X509Certificate.class);
        securityTokenResolver = (SecurityTokenResolver)getBean(applicationContext, "securityTokenResolver", "certificate resolver", SecurityTokenResolver.class);

        IdentityProviderFactory ipf = (IdentityProviderFactory)getBean(applicationContext, "identityProviderFactory", "Identity Provider Factory", IdentityProviderFactory.class);

        final Group adminGroup;
        final Group operatorGroup;
        try {
            IdentityProvider iip = ipf.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
            final GroupManager groupManager = iip.getGroupManager();
            adminGroup = groupManager.findByName(Group.ADMIN_GROUP_NAME);
            operatorGroup = groupManager.findByName(Group.OPERATOR_GROUP_NAME);
        } catch (FindException e) {
            throw new ServletException(ERR_PREFIX + "Internal Identity Provider or admin groups", e);
        }

        final AllAssertion policy = new AllAssertion(Arrays.asList(new Assertion[] {
                // TODO support configurable IP range assertions
                new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                        // HTTP Basic only permitted with SSL
                        new AllAssertion(Arrays.asList(new Assertion[] {
                                new SslAssertion(false),
                                new HttpBasic(),
                        })),
                        new SslAssertion(true), // With client cert
                        new RequestWssX509Cert(), // TODO do we care what part of the message is signed?
                        new SecureConversation(),
                })),
                new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                        new MemberOfGroup(adminGroup.getProviderId(),
                                adminGroup.getName(),
                                adminGroup.getUniqueIdentifier()),
                        new MemberOfGroup(operatorGroup.getProviderId(),
                                operatorGroup.getName(),
                                operatorGroup.getUniqueIdentifier())
                }))
        }));

        final ServerPolicyFactory policyFactory = (ServerPolicyFactory) applicationContext.getBean("policyFactory");
        if (policyFactory == null) {
            throw new ServletException(ERR_PREFIX + "ServerPolicyFactory");
        }
        try {
            adminPolicy = policyFactory.makeServerAssertion(policy);
        } catch (ServerPolicyException e) {
            log.log(Level.SEVERE, "Unable to instantiate admin service policy: " + ExceptionUtils.getMessage(e), e);
            // fallthrough and complain
        }
        if (adminPolicy == null) {
            throw new ServletException(ERR_PREFIX + "policy");
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
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

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setReplyExpected(true); // HTTP always expects to receive a reply
        context.setAuditContext(auditContext);
        context.setSoapFaultManager(soapFaultManager);

        AssertionStatus polStatus = null;
        try {
            request.initialize(StashManagerFactory.createStashManager(), ctype, servletRequest.getInputStream());

            trogdor(context, request);

            final AssertionStatus status = polStatus = adminPolicy.checkRequest(context);
            context.setPolicyResult(status);
            if (status == AssertionStatus.NONE) {
                // TODO support admin services that requre Gateway Administrators membership
                // Pass it along to XFire
                HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpServletRequest) {
                    public ServletInputStream getInputStream() throws IOException {
                        try {
                            final InputStream is = request.getMimeKnob().getEntireMessageBodyAsInputStream();
                            return new ServletInputStream() {
                                public int read() throws IOException {
                                    return is.read();
                                }

                                public int read(byte b[]) throws IOException {
                                    return is.read(b);
                                }

                                public int read(byte b[], int off, int len) throws IOException {
                                    return is.read(b, off, len);
                                }
                            };
                        } catch (NoSuchPartException e) {
                            throw new IOException("Couldn't get InputStream"); // Very unlikely
                        }
                    }

                    /**
                     * @deprecated
                     */
                    public boolean isRequestedSessionIdFromUrl() {
                        return super.isRequestedSessionIdFromUrl();
                    }

                    /**
                     * @deprecated
                     */
                    public String getRealPath(String string) {
                        return super.getRealPath(string);
                    }
                };

                filterChain.doFilter(wrapper, servletResponse);
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
                        SoapFaultUtils.FC_CLIENT,
                        e.getMessage(),
                        null,
                        ((HttpServletRequest)servletRequest).getRequestURL().toString());
                XmlUtil.nodeToOutputStream(fault.getDocumentElement(), out, ContentTypeHeader.XML_DEFAULT.getEncoding());
            } catch (SAXException e1) {
                throw new ServletException(e1); // Can't happen really
            } finally {
                if (out != null) try{ out.close(); }catch(Exception ex){}
            }
        } finally {
            try {
                String message = "Administration Web Service";
                if(polStatus!=null && polStatus!=AssertionStatus.NONE) message += ": " + polStatus.getMessage();
                User user = getUser(context);
                applicationContext.publishEvent(new AdminWebServiceEvent(this, Level.INFO, servletRequest.getRemoteAddr(), message, user.getProviderId(), getName(user), user.getUniqueIdentifier()));
            }
            catch(Exception se) {
                log.log(Level.WARNING, "Error dispatching event.", se);
            }
            finally {
                context.close();
            }
        }
    }

    private void trogdor(PolicyEnforcementContext context, Message request) throws IOException, SAXException, InvalidDocumentFormatException, ProcessorException {
        // WSS-Processing Step
        boolean isSoap = false;
        boolean hasSecurity = false;

        isSoap = context.getRequest().isSoap();
        hasSecurity = isSoap && context.getRequest().getSoapKnob().isSecurityHeaderPresent();

        if (isSoap && hasSecurity) {
            WssProcessor trogdor = new WssProcessorImpl(); // no need for locator
            try {
                final SecurityKnob reqSec = request.getSecurityKnob();
                ProcessorResult wssOutput = trogdor.undecorateMessage(request,
                                                      null, serverCertificate,
                                                      serverPrivateKey,
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

    private User getUser(PolicyEnforcementContext context) {
        User user = null;

        if(context.isAuthenticated()) {
            user = context.getAuthenticatedUser();
        }

        if(user==null) {
            user = new UserBean();
        }

        return user;
    }

    private String getName(User user) {
        return user.getName()!=null ? user.getName() : user.getLogin();
    }

    public void destroy() {
    }
}
