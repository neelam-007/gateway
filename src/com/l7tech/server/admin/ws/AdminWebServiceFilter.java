/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */

package com.l7tech.server.admin.ws;

import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.xml.CertificateResolver;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
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
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.StashManagerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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
    private ServerAssertion adminPolicy;
    private X509Certificate serverCertificate;
    private PrivateKey serverPrivateKey;
    private CertificateResolver certificateResolver;

    private static final Logger log = Logger.getLogger(AdminWebServiceFilter.class.getName());
    private static final String ERR_PREFIX = "Configuration error; could not get ";

    private Object getBean(ApplicationContext context, String name, String desc) throws ServletException {
        Object bean = context.getBean(name);
        if (bean == null) throw new ServletException(ERR_PREFIX + desc);
        return bean;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // Constructs a policy for all admin web services.
        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (applicationContext == null) {
            throw new ServletException(ERR_PREFIX + "application context");
        }

        serverPrivateKey = (PrivateKey)getBean(applicationContext, "sslKeystorePrivateKey", "server private key");
        serverCertificate = (X509Certificate)getBean(applicationContext, "sslKeystoreCertificate", "server certificate");
        certificateResolver = (CertificateResolver)getBean(applicationContext, "certificateResolver", "certificate resolver");

        IdentityProviderFactory ipf = (IdentityProviderFactory)getBean(applicationContext, "identityProviderFactory", "Identity Provider Factory");

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
        adminPolicy = policyFactory.makeServerPolicy(policy);
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

        if (!(servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse)) {
            throw new ServletException("Only HTTP requests are supported");
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String tmp = httpServletRequest.getQueryString();
        if ("get".equalsIgnoreCase(httpServletRequest.getMethod()) && "wsdl".equalsIgnoreCase(httpServletRequest.getQueryString())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(httpServletRequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob((HttpServletResponse) servletResponse);
        response.attachHttpResponseKnob(respKnob);

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setReplyExpected(true); // HTTP always expects to receive a reply

        try {
            request.initialize(StashManagerFactory.createStashManager(), ctype, servletRequest.getInputStream());

            trogdor(context, request);

            final AssertionStatus status = adminPolicy.checkRequest(context);
            if (status == AssertionStatus.NONE) {
                // TODO support admin services that requre Gateway Administrators membership
                // Pass it along to XFire
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                throw new ServletException("Authentication Required");
            }
        } catch (Exception e) {
            PrintWriter writer = null;
            try {
                log.log(Level.WARNING, "Admin Web Service request failed", e);
                writer = servletResponse.getWriter();
                String fault = SoapFaultUtils.generateSoapFaultXml(
                        SoapFaultUtils.FC_CLIENT,
                        e.getMessage(),
                        null,
                        ((HttpServletRequest)servletRequest).getRequestURL().toString());
                writer.print(fault);
            } catch (SAXException e1) {
                throw new ServletException(e1); // Can't happen really
            } finally {
                if (writer != null) writer.close();
            }
            throw new ServletException(e);
        } finally {
            context.close();
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
                final XmlKnob reqXml = request.getXmlKnob();
                ProcessorResult wssOutput = trogdor.undecorateMessage(request,
                                                      null, serverCertificate,
                                                      serverPrivateKey,
                                                      SecureConversationContextManager.getInstance(),
                                                      certificateResolver);
                reqXml.setProcessorResult(wssOutput);
            } catch (GeneralSecurityException e) {
                throw new ProcessorException(e);
            } catch (BadSecurityContextException e) {
                throw new ProcessorException(e);
            }
        }
    }

    public void destroy() {
    }
}
