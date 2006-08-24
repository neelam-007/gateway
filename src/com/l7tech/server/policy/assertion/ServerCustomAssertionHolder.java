/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.HttpServletResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.security.auth.Subject;
import javax.security.auth.login.FailedLoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.security.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>ServerCustomAssertionHolder</code> class represents the server side of the
 * custom assertion. It implemets the <code>ServerAssertion</code> interface, and it
 * prepares the environment for executing the custom assertion <code>ServiceInvocation<code>.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServerCustomAssertionHolder extends AbstractServerAssertion implements ServerAssertion {
    protected final Logger logger = Logger.getLogger(getClass().getName());

    private final CustomAssertionHolder data;
    final protected CustomAssertion customAssertion;
    final private boolean isAuthAssertion;
    private CustomAssertionDescriptor descriptor;
    private ServiceInvocation serviceInvocation;
    private final Auditor auditor;
    private CustomAssertionsRegistrar customAssertionRegistrar;
    private ApplicationContext applicationContext;

    public ServerCustomAssertionHolder(CustomAssertionHolder ca, ApplicationContext springContext) {
        super(ca);
        if (ca == null || ca.getCustomAssertion() == null) {
            throw new IllegalArgumentException();
        }
        this.data = ca;
        this.applicationContext = springContext;
        customAssertion = ca.getCustomAssertion(); // ignore hoder
        isAuthAssertion = Category.ACCESS_CONTROL.equals(ca.getCategory());
        // auditor
        auditor = new Auditor(this, springContext, logger);
    }

    private void initialize() throws PolicyAssertionException {
        descriptor = getCustomAssertionRegistrar().getDescriptor(customAssertion.getClass());

        if (!checkDescriptor(descriptor)) {
            logger.warning("Invalid custom assertion descriptor detected for '" + customAssertion.getClass() + "'\n" +
              " this policy element is misconfigured and will cause the policy to fail.");
            throw new PolicyAssertionException(data, "Custom assertion is misconfigured");
        }
        Class sa = descriptor.getServerAssertion();
        try {
            serviceInvocation = (ServiceInvocation)sa.newInstance();
            serviceInvocation.setCustomAssertion(customAssertion);
            new CustomAuditorImpl(auditor).register(serviceInvocation);
        } catch (InstantiationException e) {
            throw new PolicyAssertionException(data, "Custom assertion is misconfigured", e);
        } catch (IllegalAccessException e) {
            throw new PolicyAssertionException(data, "Custom assertion is misconfigured", e);
        }
    }

    private static void saveServletKnobs(PolicyEnforcementContext pec, Map context) {
        final HttpServletRequestKnob hsRequestKnob = (HttpServletRequestKnob)pec.getRequest().getKnob(HttpServletRequestKnob.class);
        if (hsRequestKnob != null) {
            String[] headerNames = hsRequestKnob.getHeaderNames();
            for (int i = 0; i < headerNames.length; i++) {
                String name = headerNames[i];
                context.put(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER_VALUES + "." + name.toLowerCase(), hsRequestKnob.getHeaderValues(name));
            }

            final HttpServletRequest httpServletRequest = hsRequestKnob.getHttpServletRequest();
            if (httpServletRequest != null)
                context.put("httpRequest", httpServletRequest);
        }
        final HttpServletResponseKnob hsResponseKnob = (HttpServletResponseKnob)pec.getResponse().getKnob(HttpServletResponseKnob.class);
        final HttpServletResponse httpServletResponse = hsResponseKnob == null ? null : hsResponseKnob.getHttpServletResponse();
        if (httpServletResponse != null)
            context.put("httpResponse", wrap(httpServletResponse, pec, context));
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Bugzilla #707 - removed the logger.entering()/exiting() as they are just for debugging purpose
        //logger.entering(ServerCustomAssertionHolder.class.getName(), "checkRequest");

        boolean contextClassLoaderReplaced = false;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class customAssertionClass = customAssertion.getClass();
        try {
            if (contextClassLoader != null && !customAssertionClass.getClassLoader().equals(contextClassLoader)) {
                contextClassLoaderReplaced = true;
                Thread.currentThread().setContextClassLoader(customAssertionClass.getClassLoader());
            }

            if(serviceInvocation == null) initialize();

            try {
                PublishedService service = context.getService();

                final CustomAssertionDescriptor descriptor = getCustomAssertionRegistrar().getDescriptor(customAssertionClass);

                if (!checkDescriptor(descriptor)) {
                    auditor.logAndAudit(AssertionMessages.CA_INVALID_CA_DESCRIPTOR, new String[] {customAssertion.getClass().getName()});
                    throw new PolicyAssertionException(data, "Custom assertion is misconfigured, service '" + service.getName() + "'");
                }
                Subject subject = new Subject();
                LoginCredentials principalCredentials = context.getCredentials();
                if (principalCredentials != null) {
                    String principalName = principalCredentials.getLogin();
                    auditor.logAndAudit(AssertionMessages.CA_CREDENTIAL_INFO,
                            new String[] {service.getName(), descriptor.getServerAssertion().getName(), principalName});

                    if (principalName != null) {
                        subject.getPrincipals().add(new CustomAssertionPrincipal(principalName));
                    }
                    final char[] credentials = principalCredentials.getCredentials();
                    if (credentials != null) {
                        subject.getPrivateCredentials().add(new String(credentials));
                    }
                }
                subject.setReadOnly();
                Subject.doAs(subject, new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        CustomService customService = null;
                        try {
                            if (isPostRouting(context)) {
                                CustomServiceResponse customServiceResponse = new CustomServiceResponse(context);
                                customService = customServiceResponse;
                                serviceInvocation.onResponse(customServiceResponse);
                            } else {
                                CustomServiceRequest customServiceRequest = new CustomServiceRequest(context);
                                customService = customServiceRequest;
                                serviceInvocation.onRequest(customServiceRequest);
                            }
                        }
                        finally {
                            if(customService!=null) customService.onCompletion();
                        }
                        return null;
                    }
                });
                if (isAuthAssertion && principalCredentials != null)
                    context.setAuthenticationResult(new AuthenticationResult(new UserBean(principalCredentials.getLogin()), null, false));
                return AssertionStatus.NONE;
            } catch (PrivilegedActionException e) {
                if (ExceptionUtils.causedBy(e.getException(), FailedLoginException.class)) {
                    auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] {
                            customAssertion.getName(),
                            "Authentication (login); detail '"+ExceptionUtils.getMessage(e.getException())+"'"});
                    return AssertionStatus.AUTH_FAILED;
                } else if (ExceptionUtils.causedBy(e.getException(), GeneralSecurityException.class)) {
                    if (isAuthAssertion) {
                        auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] {
                                customAssertion.getName(),
                                "Authorization (access control) failed; detail '"+ExceptionUtils.getMessage(e.getException())+"'"});
                        return AssertionStatus.UNAUTHORIZED;
                    }
                    else {
                        auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] {
                                customAssertion.getName(),
                                "Assertion failed; detail '"+ExceptionUtils.getMessage(e.getException())+"'"});
                        return AssertionStatus.FALSIFIED;
                    }
                }
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Failed to invoke the custom assertion"}, e);
                return AssertionStatus.FAILED;
            } catch (AccessControlException e) {
                auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] {
                        customAssertion.getName(),
                        "Authorization (access control) failed; detail '"+ExceptionUtils.getMessage(e)+"'"});
                return AssertionStatus.UNAUTHORIZED;
            }
        } finally {
            if (contextClassLoaderReplaced) Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private boolean isPostRouting(PolicyEnforcementContext context) {
        return RoutingStatus.ROUTED.equals(context.getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(context.getRoutingStatus());
    }


    private CustomAssertionsRegistrar getCustomAssertionRegistrar() {
        if (customAssertionRegistrar !=null) {
            return customAssertionRegistrar;
        }
        customAssertionRegistrar = (CustomAssertionsRegistrar)applicationContext.getBean("customAssertionRegistrar");

        return customAssertionRegistrar;
    }

    /**
     * check if descriptor is valid, log if invalid
     */
    private boolean checkDescriptor(CustomAssertionDescriptor descriptor) {
        if (descriptor == null || descriptor.getServerAssertion() == null) {
            return false;
        }
        return true;
    }

    private Object[][] extractParts(Message m) throws IOException {
        ArrayList contentTypes = new ArrayList();
        ArrayList bodies = new ArrayList();
        try {
            for (PartIterator i = m.getMimeKnob().getParts(); i.hasNext();) {
                PartInfo pi = i.next();
                String contentType = pi.getContentType().getFullValue();
                byte[] content = HexUtils.slurpStream(pi.getInputStream(false));
                if (contentType == null || contentType.length() < 1 || content == null || content.length < 1) {
                    logger.warning("empty partinfo (?)");
                } else {

                }
                contentTypes.add(contentType);
                bodies.add(content);
            }
        } catch (NoSuchPartException e) {
            String msg = "cannot iterate through message's parts";
            logger.log(Level.WARNING, msg, e);
            throw new RuntimeException(msg, e);
        }
        Object[][] messageParts = new Object[contentTypes.size()][2];
        int i = 0;
        for (Iterator iterator = contentTypes.iterator(); iterator.hasNext();) {
            messageParts[i][0] = iterator.next();
            i++;
        }
        i = 0;
        for (Iterator iterator = bodies.iterator(); iterator.hasNext();) {
            messageParts[i][1] = iterator.next();
            i++;
        }
        return messageParts;
    }

    /**
     * Use Vector since this is what the Custom Assertions API exposes ...
     */
    private Vector toServletCookies(Collection cookies) {
        Vector cookieVector = new Vector();

        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
            HttpCookie httpCookie = (HttpCookie) iterator.next();
            CookieUtils.toServletCookie(httpCookie);
        }

        return cookieVector;
    }

    /**
     * Wrap the HTTP Servlet Response to intercept any cookies and save them in the
     * context for later.
     */
    private static HttpServletResponse wrap(HttpServletResponse response, final PolicyEnforcementContext pec, final Map context) {
        return new HttpServletResponseWrapper(response){

            /**
             * Don't actually add to the response here, save it for later.
             */
            public void addCookie(Cookie cookie) {
                pec.addCookie(CookieUtils.fromServletCookie(cookie, true));
            }

            /**
             * @deprecated
             */
            public void setStatus(int i, String string) {
                super.setStatus(i, string);
            }

            /**
             * @deprecated
             */
            public String encodeRedirectUrl(String string) {
                return super.encodeRedirectUrl(string);
            }

            /**
             * @deprecated
             */
            public String encodeUrl(String string) {
                return super.encodeUrl(string);
            }
        };
    }

    private abstract class CustomService
    {
        protected void onCompletion() {
        }
    }

    private class CustomServiceResponse extends CustomService implements ServiceResponse {
        private final PolicyEnforcementContext pec;
        private final Map context = new HashMap();
        private final Document document;
        private final SecurityContext securityContext;

        public CustomServiceResponse(PolicyEnforcementContext pec) throws IOException, SAXException {
            this.pec = pec;
            this.document = (Document)pec.getResponse().getXmlKnob().getDocumentReadOnly().cloneNode(true);

            saveServletKnobs(pec, context);

            // plug in the message parts in here
            context.put("messageParts", extractParts(pec.getResponse()));

            securityContext = new SecurityContext() {
                public Subject getSubject() {
                    return Subject.getSubject(AccessController.getContext());
                }

                public boolean isAuthenticated() {
                    return CustomServiceResponse.this.pec.isAuthenticated();
                }

                public void setAuthenticated() throws GeneralSecurityException {
                    throw new GeneralSecurityException("Cannot authenticate in the response");
                }
            };
        }

        public Document getDocument() {
            return document;
        }

        public void setDocument(Document document) {
            XmlKnob respXml = (XmlKnob)pec.getResponse().getKnob(XmlKnob.class);
            if (respXml == null)
                pec.getResponse().initialize(document);
            else
                respXml.setDocument(document);
            try {
                final String docstring = XmlUtil.nodeToString(document);
                logger.fine("Set the response document to" + docstring);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing document", e);
            }
        }

        public SecurityContext getSecurityContext() {
            return securityContext;
        }

        public Map getContext() {
            return context;
        }

        /**
         * Access a context variable from the policy enforcement context
         */
        public Object getVariable(String name) {
            try {
                return pec.getVariable(name);
            } catch (NoSuchVariableException e) {
                logger.log(Level.INFO, "bad variable requested by custom assertion", e);
            }
            return null;
        }

        /**
         * Set a context variable from the policy enforcement context
         */
        public void setVariable(String name, Object value) {
            pec.setVariable(name, value);
        }
    }

    private class CustomServiceRequest extends CustomService implements ServiceRequest {
        private final PolicyEnforcementContext pec;
        private final Map context = new HashMap();
        private final Document document;
        private final SecurityContext securityContext;

        public CustomServiceRequest(PolicyEnforcementContext pec)
          throws IOException, SAXException {
            this.pec = pec;
            this.document = (Document) pec.getRequest().getXmlKnob().getDocumentReadOnly().cloneNode(true);
            Vector newCookies = toServletCookies(pec.getCookies());

            saveServletKnobs(pec, context);

            context.put("updatedCookies", newCookies);
            context.put("originalCookies", Collections.unmodifiableCollection(new ArrayList(newCookies)));
            // plug in the message parts in here
            context.put("messageParts", extractParts(pec.getRequest()));

            securityContext = new SecurityContext() {
                public Subject getSubject() {
                    return Subject.getSubject(AccessController.getContext());
                }

                public boolean isAuthenticated() {
                    return CustomServiceRequest.this.pec.isAuthenticated();
                }

                public void setAuthenticated() throws GeneralSecurityException {
                    if (CustomServiceRequest.this.pec.isAuthenticated()) {
                        throw new GeneralSecurityException("already authenticated");
                    }
                    CustomServiceRequest.this.pec.setAuthenticationResult(AuthenticationResult.AUTHENTICATED_UNKNOWN_USER);
                }
            };
        }

        public Document getDocument() {
            return document;
        }

        public void setDocument(Document document) {
            XmlKnob reqXml = (XmlKnob)pec.getRequest().getKnob(XmlKnob.class);
            if (reqXml == null)
                pec.getRequest().initialize(document);
            else
                reqXml.setDocument(document);
            try {
                final String docstring = XmlUtil.nodeToString(document);
                logger.fine("Set the request document to" + docstring);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing document", e);
            }
        }

        public SecurityContext getSecurityContext() {
            return securityContext;
        }

        public Map getContext() {
            return context;
        }

        protected void onCompletion() {
            Vector cookies = (Vector) context.get("updatedCookies");
            Collection originals = (Collection) context.get("originalCookies");
            for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
                Cookie cookie = (Cookie) iterator.next();
                if(!originals.contains(cookie)) {
                    // doesn't really matter if this has already been added
                    pec.addCookie(CookieUtils.fromServletCookie(cookie, true));
                }
            }
        }

        /**
         * Access a context variable from the policy enforcement context
         */
        public Object getVariable(String name) {
            try {
                return pec.getVariable(name);
            } catch (NoSuchVariableException e) {
                logger.log(Level.INFO, "bad variable requested by custom assertion", e);
            }
            return null;
        }

        /**
         * Set a context variable from the policy enforcement context
         */
        public void setVariable(String name, Object value) {
            pec.setVariable(name, value);
        }
    }
}
