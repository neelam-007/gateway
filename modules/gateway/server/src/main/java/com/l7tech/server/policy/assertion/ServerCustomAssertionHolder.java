package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.custom.*;
import com.l7tech.gateway.common.custom.ContentTypeHeaderToCustomConverter;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory;
import com.l7tech.policy.assertion.ext.message.knob.CustomHttpHeadersKnob;
import com.l7tech.policy.assertion.ext.message.knob.CustomPartsKnob;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.custom.CustomMessageImpl;
import com.l7tech.gateway.common.custom.CustomToMessageTargetableConverter;
import com.l7tech.server.custom.format.CustomMessageFormatRegistry;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.AnonymousUserReference;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.message.*;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetableSupport;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.custom.knob.CustomHttpHeadersKnobImpl;
import com.l7tech.server.custom.knob.CustomPartsKnobImpl;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.store.KeyValueStoreServicesImpl;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.*;
import com.l7tech.server.policy.custom.CustomAuditorImpl;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;

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

import com.sun.istack.Nullable;
import org.jetbrains.annotations.NotNull;

import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * The <code>ServerCustomAssertionHolder</code> class represents the server side of the
 * custom assertion. It implements the <code>ServerAssertion</code> interface, and it
 * prepares the environment for executing the custom assertion <code>ServiceInvocation<code>.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServerCustomAssertionHolder extends AbstractServerAssertion implements ServerAssertion {
    private final CustomAssertionHolder data;
    final protected CustomAssertion customAssertion;
    final private boolean isAuthAssertion;
    private CustomAssertionDescriptor descriptor;
    private ServiceInvocation serviceInvocation;

    private CustomAssertionsRegistrar customAssertionRegistrar;
    private ApplicationContext applicationContext;

    public ServerCustomAssertionHolder(CustomAssertionHolder ca, ApplicationContext springContext) {
        super(ca);
        if (ca == null)
            throw new IllegalArgumentException();
        this.data = ca;
        this.applicationContext = springContext;
        customAssertion = ca.getCustomAssertion(); // ignore hoder
        isAuthAssertion = ca.isCustomCredentialSource();
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
            serviceInvocation = (ServiceInvocation) sa.newInstance();
            serviceInvocation.setCustomAssertion(customAssertion);
            new CustomAuditorImpl(getAudit()).register(serviceInvocation);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new PolicyAssertionException(data, "Custom assertion is misconfigured", e);
        }
    }

    private static void saveServletKnobs(PolicyEnforcementContext pec, Map context) {
        final HttpServletRequestKnob hsRequestKnob = pec.getRequest().getKnob(HttpServletRequestKnob.class);
        if (hsRequestKnob != null) {
            String[] headerNames = hsRequestKnob.getHeaderNames();
            for ( String name : headerNames ) {
                context.put( "request.http.headerValues" + "." + name.toLowerCase(), hsRequestKnob.getHeaderValues( name ) );
            }

            final HttpServletRequest httpServletRequest = hsRequestKnob.getHttpServletRequest();
            if (httpServletRequest != null)
                context.put("httpRequest", httpServletRequest);
        }
        final HttpServletResponseKnob hsResponseKnob = pec.getResponse().getKnob(HttpServletResponseKnob.class);
        final HttpServletResponse httpServletResponse = hsResponseKnob == null ? null : hsResponseKnob.getHttpServletResponse();
        if (httpServletResponse != null)
            context.put("httpResponse", wrap(httpServletResponse, pec));
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (customAssertion == null) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "CustomAssertionHolder contains no CustomAssertion" );
            return AssertionStatus.SERVER_ERROR;
        }
        // Bugzilla #707 - removed the logger.entering()/exiting() as they are just for debugging purpose
        //logger.entering(ServerCustomAssertionHolder.class.getName(), "checkRequest");

        boolean contextClassLoaderReplaced = false;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class customAssertionClass = customAssertion.getClass();
        try {
            if (contextClassLoader != null && !customAssertionClass.getClassLoader().equals(contextClassLoader)) {
                contextClassLoaderReplaced = true;

                getClass().getProtectionDomain().getCodeSource().getLocation();
                customAssertionClass.getProtectionDomain().getCodeSource().getLocation();

                Thread.currentThread().setContextClassLoader(customAssertionClass.getClassLoader());
            }

            if (serviceInvocation == null) initialize();

            try {
                PublishedService service = context.getService();

                final CustomAssertionDescriptor descriptor = getCustomAssertionRegistrar().getDescriptor(customAssertionClass);

                if (!checkDescriptor(descriptor)) {
                    logAndAudit(AssertionMessages.CA_INVALID_CA_DESCRIPTOR, customAssertion.getClass().getName() );
                    throw new PolicyAssertionException(data, "Custom assertion is misconfigured, service '" + service.getName() + "'");
                }
                Subject subject = new Subject();
                LoginCredentials principalCredentials = context.getDefaultAuthenticationContext().getLastCredentials();
                if (principalCredentials != null) {
                    String principalName = principalCredentials.getLogin();
                    logAndAudit(AssertionMessages.CA_CREDENTIAL_INFO,
                            service.getName(), descriptor.getServerAssertion().getName(), principalName );

                    if (principalName != null) {
                        subject.getPrincipals().add(new CustomAssertionPrincipal(principalName));
                    }
                    final char[] credentials = principalCredentials.getCredentials();
                    if (credentials != null) {
                        subject.getPrivateCredentials().add(new String(credentials));
                    }
                }
                subject.setReadOnly();
                Object retStatus = Subject.doAs(subject, new PrivilegedExceptionAction() {
                    @Override
                    public Object run() throws Exception {
                        CustomAssertionStatus status = null;
                        CustomPolicyContextImpl policyContext = null;
                        CustomServiceResponse customServiceResponse = null;
                        CustomServiceRequest customServiceRequest = null;

                        try {
                            Map defaultContextMap;
                            if (isPostRouting(context) && context.getResponse().isInitialized() && context.getResponse().getKnob(MimeKnob.class) != null) {
                                defaultContextMap = createDefaultContextMap(context, context.getResponse());
                                customServiceResponse = new CustomServiceResponse(context, defaultContextMap);
                            } else {
                                defaultContextMap = createDefaultContextMap(context, context.getRequest());
                                customServiceRequest = new CustomServiceRequest(context, defaultContextMap);
                            }
                            policyContext = new CustomPolicyContextImpl(context, defaultContextMap, customServiceRequest, customServiceResponse);
                            status = serviceInvocation.checkRequest(policyContext);
                        } catch (RuntimeException e) {
                            // ServiceInvocation.checkRequest(...) swallows IOException and GeneralSecurityException with RuntimeException
                            // so that those exceptions can be excluded from the function signature.
                            final Throwable throwable = e.getCause();
                            if (throwable instanceof IOException) {
                                throw (IOException)throwable;
                            } else if (throwable instanceof GeneralSecurityException){
                                throw (GeneralSecurityException)throwable;
                            }
                            throw e;
                        } finally {
                            if (policyContext != null) policyContext.onCompletion();
                        }
                        return status;
                    }
                });
                if (isAuthAssertion) {
                    AuthenticationContext authContext = context.getAuthenticationContext(context.getRequest());
                    if (principalCredentials != null) {
                        authContext.addAuthenticationResult(new AuthenticationResult(new UserBean(principalCredentials.getLogin()), principalCredentials.getSecurityTokens(),  null, false));
                    } else {
                        authContext.addAuthenticationResult(new AuthenticationResult(new AnonymousUserReference("", -1, "<unknown>"), new OpaqueSecurityToken()));
                    }
                }

                if (retStatus instanceof CustomAssertionStatus) {
                    return convertToAssertionStatus((CustomAssertionStatus)retStatus);
                }

                return AssertionStatus.FAILED;
            } catch (PrivilegedActionException e) {
                if (ExceptionUtils.causedBy(e.getException(), FailedLoginException.class)) {
                    logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, customAssertion.getName(),
                            "Authentication (login) failed: " + ExceptionUtils.toStringDeep(e, true) );
                    return AssertionStatus.AUTH_FAILED;
                } else if (ExceptionUtils.causedBy(e.getException(), GeneralSecurityException.class)) {
                    if (isAuthAssertion) {
                        logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, customAssertion.getName(),
                                "Authorization (access control) failed: " + ExceptionUtils.toStringDeep(e, true) );
                        return AssertionStatus.UNAUTHORIZED;
                    } else {
                        logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, customAssertion.getName(),
                                "Assertion failed: " + ExceptionUtils.toStringDeep(e, true) );
                        return AssertionStatus.FALSIFIED;
                    }
                }
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Failed to invoke the custom assertion"}, e);
                return AssertionStatus.FAILED;
            } catch (AccessControlException e) {
                logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, customAssertion.getName(),
                        "Authorization (access control) failed: " + ExceptionUtils.toStringDeep(e, true) );
                return AssertionStatus.UNAUTHORIZED;
            }
        } finally {
            if (contextClassLoaderReplaced) Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * Convert from CustomAssertionStatus into AssertionStatus
     * @return AssertionStatus object from
    */
    private static AssertionStatus convertToAssertionStatus(CustomAssertionStatus status) {
        switch (status) {
            case NONE: return AssertionStatus.NONE;
            case AUTH_FAILED: return AssertionStatus.AUTH_FAILED;
            case UNAUTHORIZED: return AssertionStatus.UNAUTHORIZED;
            case FALSIFIED: return AssertionStatus.FALSIFIED;
            case FAILED: return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    private boolean isPostRouting(PolicyEnforcementContext context) {
        return RoutingStatus.ROUTED.equals(context.getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(context.getRoutingStatus());
    }

    private CustomAssertionsRegistrar getCustomAssertionRegistrar() {
        if (customAssertionRegistrar != null) {
            return customAssertionRegistrar;
        }
        customAssertionRegistrar = (CustomAssertionsRegistrar) applicationContext.getBean("customAssertionRegistrar");

        return customAssertionRegistrar;
    }

    /**
     * check if descriptor is valid, log if invalid
     */
    private boolean checkDescriptor(CustomAssertionDescriptor descriptor) {
        return !(descriptor == null || descriptor.getServerAssertion() == null);
    }

    private Object[][] extractParts(Message m) throws IOException {
        ArrayList contentTypes = new ArrayList();
        ArrayList bodies = new ArrayList();
        try {
            for (PartIterator i = m.getMimeKnob().getParts(); i.hasNext();) {
                PartInfo pi = i.next();
                String contentType = pi.getContentType().getFullValue();
                byte[] content = IOUtils.slurpStream(pi.getInputStream(false));
                if (contentType == null || contentType.length() < 1 || content == null || content.length < 1) {
                    logger.fine("empty partinfo encountered");
                }
                contentTypes.add(contentType);
                bodies.add(content);
            }
        } catch (NoSuchPartException e) {
            String msg = "cannot iterate through message's parts";
            logger.log(Level.WARNING, msg, e);
            throw new RuntimeException(msg, e);
        } catch (IllegalStateException e) {
            logger.log(Level.FINE, "cannot get mime knob", e);
        }
        Object[][] messageParts = new Object[contentTypes.size()][2];
        int i = 0;
        for ( final Object contentType : contentTypes ) {
            messageParts[i][0] = contentType;
            i++;
        }
        i = 0;
        for ( final Object body : bodies ) {
            messageParts[i][1] = body;
            i++;
        }
        return messageParts;
    }

    /**
     * Use Vector since this is what the Custom Assertions API exposes ...
     */
    private Vector toServletCookies(Collection cookies) {
        Vector cookieVector = new Vector();

        for ( final Object cooky : cookies ) {
            HttpCookie httpCookie = (HttpCookie) cooky;
            //CookieUtils.toServletCookie(httpCookie);
            cookieVector.add( CookieUtils.toServletCookie( httpCookie ) );
        }

        return cookieVector;
    }

    /**
     * Wrap the HTTP Servlet Response to intercept any cookies and save them in the
     * context for later.
     */
    private static HttpServletResponse wrap(HttpServletResponse response, final PolicyEnforcementContext pec) {
        return new HttpServletResponseWrapper(response) {

            /**
             * Don't actually add to the response here, save it for later.
             */
            @Override
            public void addCookie(Cookie cookie) {
                pec.addCookie(CookieUtils.fromServletCookie(cookie, true));
            }

            /**
             * @deprecated
             */
            @Override
            @Deprecated
            public void setStatus(int i, String string) {
                super.setStatus(i, string);
            }

            /**
             * @deprecated
             */
            @Override
            @Deprecated
            public String encodeRedirectUrl(String string) {
                return super.encodeRedirectUrl(string);
            }

            /**
             * @deprecated
             */
            @Override
            @Deprecated
            public String encodeUrl(String string) {
                return super.encodeUrl(string);
            }
        };
    }

    private abstract class CustomService {
        protected void onCompletion() {
        }
    }

    /**
     * Use this function to initialize all common/shared context objects.
     *
     * @param pec    the policy enforcement context.
     * @return hash-map containing the default list of context objects.
     * @throws IOException if extractParts throws
     */
    private Map<String, Object> createDefaultContextMap(PolicyEnforcementContext pec, Message msg) throws IOException {
        final Map<String, Object> context = new HashMap<>();

        // add HttpServletRequest, HttpServletRequest header values and HttpServletResponse.
        saveServletKnobs(pec, context);

        // check if we should load all message parts into the context map
        if (serviceInvocation == null || serviceInvocation.loadsMessagePartsIntoMemory()) {
            // plug in the message parts in here (needed for legacy code)
            context.put("messageParts", extractParts(msg));
        }

        //add the ServiceFinder
        final ServiceFinderImpl serviceFinder = new ServiceFinderImpl();
        serviceFinder.setCertificateFinderImpl(new CertificateFinderImpl((TrustedCertManager) applicationContext.getBean("trustedCertManager")));
        serviceFinder.setVariableServicesImpl(new VariableServicesImpl(getAudit()));
        serviceFinder.setSecurePasswordServicesImpl(new SecurePasswordServicesImpl((SecurePasswordManager) applicationContext.getBean("securePasswordManager")));
        serviceFinder.setKeyValueStoreImpl(new KeyValueStoreServicesImpl((CustomKeyValueStoreManager) applicationContext.getBean("customKeyValueStoreManager")));

        context.put("serviceFinder", serviceFinder);

        return context;
    }

    private class CustomServiceResponse implements ServiceResponse {
        private final PolicyEnforcementContext pec;
        private final Map context;
        private Document document;
        private Document requestDocument;
        private final SecurityContext securityContext;

        public CustomServiceResponse(PolicyEnforcementContext pec, final Map defaultContextMap) throws IOException, SAXException {
            this.pec = pec;
            try {
                this.document = (Document) pec.getResponse().getXmlKnob().getDocumentReadOnly().cloneNode(true);
            } catch (Exception e) {
                this.document = null;
                logger.log(Level.FINE, "cannot get response xml", e);
            }
            try {
                this.requestDocument = (Document) pec.getRequest().getXmlKnob().getDocumentReadOnly().cloneNode(true);
            } catch (Exception e) {
                this.requestDocument = null;
                logger.log(Level.FINE, "cannot get request xml", e);
            }

            this.context = defaultContextMap;

            securityContext = new SecurityContext() {
                @Override
                public Subject getSubject() {
                    return Subject.getSubject(AccessController.getContext());
                }

                @Override
                public boolean isAuthenticated() {
                    return CustomServiceResponse.this.pec.getDefaultAuthenticationContext().isAuthenticated();
                }

                @Override
                public void setAuthenticated() throws GeneralSecurityException {
                    throw new GeneralSecurityException("Cannot authenticate in the response");
                }
            };
        }

        @Override
        public Document getDocument() {
            return document;
        }

        @Override
        public Document getRequestDocument() {
            return requestDocument;
        }

        @Override
        public void setDocument(Document document) {
            XmlKnob respXml = pec.getResponse().getKnob(XmlKnob.class);
            if (respXml == null)
                pec.getResponse().initialize(document);
            else
                respXml.setDocument(document);
            try {
                if (logger.isLoggable(Level.FINE)) {
                    final String docstring = XmlUtil.nodeToString(document);
                    logger.fine("Set the response document to" + docstring);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing document", e);
            }
        }

        @Override
        public SecurityContext getSecurityContext() {
            return securityContext;
        }

        @Override
        public Map getContext() {
            return context;
        }

        /**
         * Access a context variable from the policy enforcement context
         */
        @Override
        public Object getVariable(String name) {
            return ServerCustomAssertionHolder.this.getVariable(pec, name);
        }

        /**
         * Set a context variable from the policy enforcement context
         */
        @Override
        public void setVariable(String name, Object value) {
            ServerCustomAssertionHolder.this.setVariable(pec, name, value);
        }
    }

    private class CustomServiceRequest implements ServiceRequest {
        private final PolicyEnforcementContext pec;
        private final Map context;
        private Document document;
        private final SecurityContext securityContext;

        public CustomServiceRequest(PolicyEnforcementContext pec, Map defaultContextMap) throws IOException, SAXException {
            this.pec = pec;
            try {
                this.document = (Document) pec.getRequest().getXmlKnob().getDocumentReadOnly().cloneNode(true);
            } catch (Exception e) {
                logger.log(Level.FINE, "This request may not be XML", e);
                this.document = null;
            }

            // get the context map initialized with default objects.
            context = defaultContextMap;

            // add context cookies
            Vector newCookies = toServletCookies(pec.getCookies());
            context.put("updatedCookies", newCookies);
            context.put("originalCookies", Collections.unmodifiableCollection(new ArrayList(newCookies)));

            securityContext = new SecurityContext() {
                @Override
                public Subject getSubject() {
                    return Subject.getSubject(AccessController.getContext());
                }

                @Override
                public boolean isAuthenticated() {
                    return CustomServiceRequest.this.pec.getDefaultAuthenticationContext().isAuthenticated();
                }

                @Override
                public void setAuthenticated() throws GeneralSecurityException {
                    //not all existing custom assertions call this when authenticating
                    //authentication is determined by a lack of exception instead
                }
            };
        }

        @Override
        public Document getDocument() {
            return document;
        }

        @Override
        public void setDocument(Document document) {
            XmlKnob reqXml = pec.getRequest().getKnob(XmlKnob.class);
            if (reqXml == null)
                pec.getRequest().initialize(document);
            else
                reqXml.setDocument(document);
            try {
                if (logger.isLoggable(Level.FINE)) {
                    final String docString = XmlUtil.nodeToString(document);
                    logger.fine("Set the request document to" + docString);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing document", e);
            }
        }

        @Override
        public SecurityContext getSecurityContext() {
            return securityContext;
        }

        @Override
        public Map getContext() {
            return context;
        }

        /**
         * Access a context variable from the policy enforcement context
         */
        @Override
        public Object getVariable(String name) {
            return ServerCustomAssertionHolder.this.getVariable(pec, name);
        }

        /**
         * Set a context variable from the policy enforcement context
         */
        @Override
        public void setVariable(String name, Object value) {
            ServerCustomAssertionHolder.this.setVariable(pec, name, value);
        }
    }

    /**
     * Remove those cookies with the specified cookie names
     *
     * @param cookieNames The names of the cookie(s) to be removed
     * @param pec Policy Enforcement Context
     * @throws HttpCookie.IllegalFormatException thrown if cookies are not well format against HttpCookie format
     */
    private void removeCookies(final String[] cookieNames, final PolicyEnforcementContext pec) throws HttpCookie.IllegalFormatException {
        if (cookieNames == null || cookieNames.length == 0 || pec == null || pec.getRequest() == null) return;

        final Message message = pec.getRequest();
        HasOutboundHeaders oh = message.getKnob(OutboundHeadersKnob.class);
        if (oh == null) {
            oh = HttpOutboundRequestFacet.getOrCreateHttpOutboundRequestKnob(message);
        }

        HttpRequestKnob hrk = message.getKnob(HttpRequestKnob.class);
        if (hrk != null && !oh.containsHeader("Cookie")) {
            String[] oldValues = hrk.getHeaderValues("Cookie");
            for (String oldValue : oldValues) {
                oh.addHeader("Cookie", oldValue);
            }
        }

        for (String cookieName: cookieNames) {
            // Remove the cookies with the same cookie name from OutboundHeaders
            if (oh.containsHeader("Cookie")) {
                String[] values = oh.getHeaderValues("Cookie");

                for (String value: values) {
                    if (cookieName.equals(new HttpCookie(".", "/", value).getCookieName())) {
                        oh.removeHeader("Cookie", value);
                    }
                }
            }

            // Also remove the cookies with the same cookie name from pec context
            List<HttpCookie> toDelete = new ArrayList<>();
            Set<HttpCookie> contextCookies = pec.getCookies();
            for (HttpCookie cookie: contextCookies) {
                if (cookie.getCookieName().equals(cookieName)) {
                    toDelete.add(cookie);
                }
            }
            for (HttpCookie cookie: toDelete) {
                pec.deleteCookie(cookie);
            }
        }// for
    }

    /**
     * A helper function for setting a context variable from the policy enforcement context.
     *
     * @param policyContext    the policy enforcement context, required.
     * @param name             the name of the variable.
     * @param value            the value of the variable.
     */
    private void setVariable(@NotNull final PolicyEnforcementContext policyContext,
                             @Nullable final String name,
                             @Nullable final Object value) {
        policyContext.setVariable(name, value);
    }

    /**
     * A helper function for accessing a context variable from the policy enforcement context.
     *
     * @param policyContext    the policy enforcement context, required.
     * @param name             the name of the variable.
     * @return object representing the value of the variable, or null if the variable with the name cannot be found
     */
    private Object getVariable(@NotNull final PolicyEnforcementContext policyContext,
                               @Nullable final String name) {
        try {
            return policyContext.getVariable(name);
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, name );
        }
        return null;
    }

    /**
     * A helper function to go through all cookie updates and deletions from
     * the CustomAssertion context map and apply them into the policy enforcement context.
     *
     * @param policyContext the policy enforcement context.
     * @param contextMap    the CustomAssertion context map.
     */
    private void processUpdatedAndDeletedCookies(final PolicyEnforcementContext policyContext, final Map contextMap) {
        final Vector cookies = (Vector) contextMap.get("updatedCookies");
        final Collection originals = (Collection) contextMap.get("originalCookies");

        if (cookies != null && originals != null) {
            for ( final Object cooky : cookies ) {
                final Cookie cookie = (Cookie) cooky;
                if ( !originals.contains( cookie ) ) {
                    // doesn't really matter if this has already been added
                    policyContext.addCookie( CookieUtils.fromServletCookie( cookie, true ) );
                }
            }
        }

        try {
            final String[] cookieNames = (String[]) contextMap.get("customAssertionsCookiesToOmit");
            removeCookies(cookieNames, policyContext);
        } catch (HttpCookie.IllegalFormatException e) {
            throw new IllegalArgumentException("Invalid cookie format");
        }
    }

    /**
     * Attach New knobs here.
     *
     * @param customMessage    the message to attach the new knobs for.
     */
    public void doAttachKnobs(@NotNull final CustomMessageImpl customMessage) {
        // attach httpHeaders knob
        customMessage.attachKnob(CustomHttpHeadersKnob.class, new CustomHttpHeadersKnobImpl(customMessage.getMessage()));

        // attach mime multi-parts extractor knobs
        customMessage.attachKnob(CustomPartsKnob.class, new CustomPartsKnobImpl(customMessage.getMessage()));
    }

    /**
     * CustomPolicyContext implementation.
     */
    private class CustomPolicyContextImpl extends CustomService implements CustomPolicyContext
    {
        private final PolicyEnforcementContext policyContext;
        private final SecurityContext securityContext;

        private final boolean isPostRouting;
        private final Map contextMap;

        public CustomPolicyContextImpl(@NotNull final PolicyEnforcementContext policyContext,
                                       @NotNull final Map defaultContextMap,
                                       @Nullable final CustomServiceRequest customServiceRequest,
                                       @Nullable final CustomServiceResponse customServiceResponse
        ) throws IOException {
            this.policyContext = policyContext;

            this.isPostRouting = ServerCustomAssertionHolder.this.isPostRouting(policyContext);

            // get the default context map initialized with default objects.
            this.contextMap = defaultContextMap;

            // add context cookies
            Vector newCookies = toServletCookies(policyContext.getCookies());
            contextMap.put("updatedCookies", newCookies);
            contextMap.put("originalCookies", Collections.unmodifiableCollection(new ArrayList(newCookies)));

            // add default Request and Response
            if (customServiceRequest != null) {
                contextMap.put("defaultRequest", customServiceRequest);
            }
            if (customServiceResponse != null) {
                contextMap.put("defaultResponse", customServiceResponse);
            }

            securityContext = new SecurityContext() {
                @Override
                public Subject getSubject() {
                    return Subject.getSubject(AccessController.getContext());
                }

                @Override
                public boolean isAuthenticated() {
                    return policyContext.getDefaultAuthenticationContext().isAuthenticated();
                }

                @Override
                public void setAuthenticated() throws GeneralSecurityException {
                    if (isPostRouting) {
                        throw new GeneralSecurityException("Cannot authenticate in the response");
                    }

                    //not all existing custom assertions call this when authenticating
                    //authentication is determined by a lack of exception instead
                }
            };
        }

        @Override
        public void onCompletion() {
            processUpdatedAndDeletedCookies(policyContext, contextMap);
        }

        @Override
        public SecurityContext getSecurityContext() {
            return securityContext;
        }

        @Override
        public Map getContext() {
            return contextMap;
        }

        @Override
        public Object getVariable(final String name) {
            return ServerCustomAssertionHolder.this.getVariable(policyContext, name);
        }

        @Override
        public String expandVariable(String s) {
            Map<String, Object> vars = this.getVariableMap(Syntax.getReferencedNames(s));
            return this.expandVariable(s, vars);
        }

        @Override
        public String expandVariable(String s, Map<String, Object> vars) {
            if (s == null) {
                return null;
            }
            return ExpandVariables.process(s, vars, getAudit());
        }

        @Override
        public Map<String, Object> getVariableMap(String[] names) {
            return policyContext.getVariableMap(names, getAudit());
        }

        @Override
        public void setVariable(final String name, final Object value) {
            ServerCustomAssertionHolder.this.setVariable(policyContext, name, value);
        }

        @Override
        public CustomMessageFormatFactory getFormats() {
            return CustomMessageFormatRegistry.getInstance().getMessageFormatFactory();
        }

        @Override
        public CustomMessage getTargetMessage(final CustomMessageTargetable targetable) throws NoSuchVariableException, VariableNotSettableException {
            if (targetable == null) {
                throw new NoSuchVariableException("<NULL>", "Target name is null");
            }

            final CustomToMessageTargetableConverter converter = new CustomToMessageTargetableConverter(targetable);
            final Message targetMessage = policyContext.getOrCreateTargetMessage(converter, false);
            final CustomMessageImpl customMessage = new CustomMessageImpl(getFormats(), targetMessage);

            doAttachKnobs(customMessage);

            return customMessage;
        }

        @Override
        public CustomMessage getMessage(final String targetMessageVariable) throws NoSuchVariableException, VariableNotSettableException {
            return getTargetMessage(new CustomMessageTargetableSupport(targetMessageVariable));
        }

        @Override
        public boolean isPostRouting() {
            return this.isPostRouting;
        }

        @Override
        public CustomContentType createContentType(String contentTypeValue) throws IOException {
            return new ContentTypeHeaderToCustomConverter(ContentTypeHeader.parseValue(contentTypeValue));
        }
    }
}