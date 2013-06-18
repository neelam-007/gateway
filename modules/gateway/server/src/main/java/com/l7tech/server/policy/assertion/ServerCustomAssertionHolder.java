package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.custom.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.AnonymousUserReference;
import com.l7tech.identity.UserBean;
import com.l7tech.json.JSONData;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.message.*;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.assertion.ext.message.DataExtractor;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.CertificateFinderImpl;
import com.l7tech.server.policy.SecurePasswordServicesImpl;
import com.l7tech.server.policy.ServiceFinderImpl;
import com.l7tech.server.policy.VariableServicesImpl;
import com.l7tech.server.policy.custom.CustomAuditorImpl;
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
import java.io.InputStream;
import java.security.*;
import java.util.*;
import java.util.logging.Level;

import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * The <code>ServerCustomAssertionHolder</code> class represents the server side of the
 * custom assertion. It implemets the <code>ServerAssertion</code> interface, and it
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
        isAuthAssertion = Category.ACCESS_CONTROL.equals(ca.getCategory());
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
        } catch (InstantiationException e) {
            throw new PolicyAssertionException(data, "Custom assertion is misconfigured", e);
        } catch (IllegalAccessException e) {
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
                        CustomServiceResponse customServiceResponse = null;
                        CustomServiceRequest customServiceRequest = null;
                        CustomAssertionStatus status = null;
                        try {
                            customServiceRequest = new CustomServiceRequest(context);
                            if (context.getResponse().isInitialized() && context.getResponse().getKnob(MimeKnob.class) != null) {
                                customServiceResponse = new CustomServiceResponse(context);
                            }
                            serviceInvocation.setPostRouting(isPostRouting(context));
                            status = serviceInvocation.checkRequest(customServiceRequest, customServiceResponse);
                        } finally {
                            if (customServiceRequest != null) customServiceRequest.onCompletion();
                            if (customServiceResponse != null) customServiceResponse.onCompletion();
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

                if (retStatus != null && retStatus instanceof CustomAssertionStatus) {
                    return convertToAssertionStatus((CustomAssertionStatus)retStatus);
                }

                return AssertionStatus.NONE;
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
     * Implement DataExtractor interface
     */
    private abstract class CustomServiceDataExtractor extends CustomService implements DataExtractor
    {
        /**
         * Abstract method which should be implemented from {@link CustomServiceRequest} or {@link CustomServiceResponse}
         * Get the Message object associated with a targetable variable.
         *
         * @param targetable     The target message.
         *                       If null then target will default to Request or Response depending if the assertion is before or after routing.
         * @param getOrCreate    true if you want to create the variable if does not exist.
         * @return Message object associated with the targetable variable. Never NULL.
         * @throws NoSuchVariableException      If a variable with the name exists but is not a message variable.
         * @throws VariableNotSettableException If the variable does not exist and cannot be created.
         */
        protected abstract Message extractMessage(final CustomMessageTargetable targetable, boolean getOrCreate) throws NoSuchVariableException, VariableNotSettableException;

        @Override
        public CustomMessageData getMessageData(final CustomMessageTargetable targetable, final CustomMessageFormat format) throws NoSuchVariableException, IllegalArgumentException {
            if (format == null) {
                throw new IllegalArgumentException("format cannot be null");
            }

            final Message targetMessage = extractMessage(targetable, false);

            // if there is no data then return null
            if (!isDataOfType(targetMessage, format)) {
                return null;
            }

            switch (format)
            {
                case XML:
                    Document document;
                    try {
                        document = (Document) targetMessage.getXmlKnob().getDocumentReadOnly().cloneNode(true);
                    } catch (Exception e) {
                        logger.log(Level.FINE, "This target may not be XML", e);
                        document = null;
                    }

                    final Document retDoc = document;
                    return new CustomMessageData<Document>() {
                        @Override
                        public CustomContentHeader getContentType() {
                            ContentTypeHeader contentTypeHeader;
                            try {
                                contentTypeHeader = targetMessage.getMimeKnob().getFirstPart().getContentType();
                            } catch (IOException e) {
                                contentTypeHeader = targetMessage.getMimeKnob().getOuterContentType();
                            }
                            return contentTypeHeader != null ? new CustomToContentTypeHeaderConverter(contentTypeHeader) : null;
                        }
                        @Override
                        public Document getData() {
                            return retDoc;
                        }
                    };

                case JSON:
                    JSONData jsonData;
                    try {
                        jsonData = targetMessage.getJsonKnob().getJsonData();
                    } catch (Exception e) {
                        logger.log(Level.FINE, "This target may not be JSON", e);
                        jsonData = null;
                    }

                    final JSONData retJsonData = jsonData;
                    return new CustomMessageData<CustomJsonData>() {
                        @Override
                        public CustomContentHeader getContentType() {
                            ContentTypeHeader contentTypeHeader;
                            try {
                                contentTypeHeader = targetMessage.getMimeKnob().getFirstPart().getContentType();
                            } catch (IOException e) {
                                contentTypeHeader = targetMessage.getMimeKnob().getOuterContentType();
                            }
                            return contentTypeHeader != null ? new CustomToContentTypeHeaderConverter(contentTypeHeader) : null;
                        }
                        @Override
                        public CustomJsonData getData() {
                            return retJsonData != null ? new CustomToJsonData(retJsonData) : null;
                        }
                    };

                case BYTES:
                    final MimeKnob knobBytes = targetMessage.getKnob(MimeKnob.class);
                    // should always be true
                    if (knobBytes != null)
                    {
                        byte[] bytes = null;
                        try {
                            final InputStream inputStream = knobBytes.getEntireMessageBodyAsInputStream();
                            if (inputStream != null) {
                                bytes = IOUtils.slurpStream(inputStream);
                            }
                        } catch (IOException | NoSuchPartException e) {
                            logger.log(Level.FINE, "Failed to get Message entire body as stream", e);
                            bytes = null;
                        }

                        final byte[] retBytes = bytes;
                        return new CustomMessageData<byte[]>() {
                            @Override
                            public CustomContentHeader getContentType() {
                                return new CustomToContentTypeHeaderConverter(knobBytes.getOuterContentType());
                            }
                            @Override
                            public byte[] getData() {
                                return retBytes;
                            }
                        };
                    }
                    return null;

                case INPUT_STREAM:
                    final MimeKnob knobInput = targetMessage.getKnob(MimeKnob.class);
                    // should always be true
                    if (knobInput != null)
                    {
                        InputStream inputStream;
                        try {
                            inputStream = knobInput.getEntireMessageBodyAsInputStream();
                        } catch (IOException | NoSuchPartException e) {
                            logger.log(Level.FINE, "Failed to get Message entire body as stream", e);
                            inputStream = null;
                        }

                        final InputStream retInputStream = inputStream;
                        return new CustomMessageData<InputStream>() {
                            @Override
                            public CustomContentHeader getContentType() {
                                return new CustomToContentTypeHeaderConverter(knobInput.getOuterContentType());
                            }
                            @Override
                            public InputStream getData() {
                                return retInputStream;
                            }
                        };
                    }
                    return null;
            }

            return null;
        }

        @Override
        public CustomMessageData getMessageData(final CustomMessageFormat format) throws NoSuchVariableException, IllegalArgumentException {
            return getMessageData(null, format);
        }

        @Override
        public boolean isMessageDataOfType(final CustomMessageTargetable targetable, final CustomMessageFormat format) throws NoSuchVariableException, IllegalArgumentException {
            if (format == null) {
                throw new IllegalArgumentException("format cannot be null");
            }
            return (isDataOfType(extractMessage(targetable, false), format));
        }

        private boolean isDataOfType(final Message targetMessage, final CustomMessageFormat format) {
            try {
                switch (format) {
                    case XML:
                        return targetMessage.isInitialized() && targetMessage.isXml();
                    case JSON:
                        return targetMessage.isInitialized() && targetMessage.isJson();
                    case BYTES:
                        return targetMessage.isInitialized() && targetMessage.getKnob(MimeKnob.class) != null;
                    case INPUT_STREAM:
                        return targetMessage.isInitialized() && targetMessage.getKnob(MimeKnob.class) != null;
                }
            } catch (IOException ignored) { }
            return false;
        }

        @Override
        public boolean isMessageDataOfType(final CustomMessageFormat format) throws NoSuchVariableException, IllegalArgumentException {
            return isMessageDataOfType(null, format);
        }

        @Override
        public void setDOM(final CustomMessageTargetable targetable, final Document document) throws NoSuchVariableException, VariableNotSettableException {
            final Message targetMessage = extractMessage(targetable, true);
            final XmlKnob knob = targetMessage.getKnob(XmlKnob.class);
            if (knob == null) {
                targetMessage.initialize(document);
            } else {
                knob.setDocument(document);
            }

            try {
                if (logger.isLoggable(Level.FINE)) {
                    final String docString = XmlUtil.nodeToString(document);
                    logger.fine("Set the target message XML document to:\n" + docString);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing XML document", e);
            }
        }

        @Override
        public void setDOM(final Document document) throws NoSuchVariableException, VariableNotSettableException {
            setDOM(null, document);
        }

        @Override
        public void setJson(final CustomMessageTargetable targetable, final String jsonData) throws NoSuchVariableException, VariableNotSettableException, IOException {
            final Message targetMessage = extractMessage(targetable, true);
            targetMessage.initialize(ContentTypeHeader.APPLICATION_JSON, jsonData != null ? jsonData.getBytes() : new byte[0]);
            try {
                logger.fine("Set the target message JSON data to:\n" + jsonData);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error stringyfing Json document", e);
            }
        }

        @Override
        public void setJson(final String jsonData) throws NoSuchVariableException, VariableNotSettableException, IOException {
            setJson(null, jsonData);
        }

        @Override
        public void setBytes(final CustomMessageTargetable targetable, final CustomContentHeader contentType, final byte[] bytes) throws NoSuchVariableException, VariableNotSettableException, IOException, IllegalArgumentException {
            if (contentType == null) {
                throw new IllegalArgumentException("contentType cannot be null");
            }
            if (!(contentType instanceof CustomToContentTypeHeaderConverter)) {
                throw new IllegalArgumentException("contentType cannot be null");
            }

            final Message targetMessage = extractMessage(targetable, true);
            targetMessage.initialize(((CustomToContentTypeHeaderConverter)contentType).getInternalType(), bytes != null ? bytes : new byte[0]);

            if (logger.isLoggable(Level.FINE)) {
                try {
                    logger.fine("Set the target message document to:\n" +
                            ((bytes == null) ? "<null>" : Arrays.toString(bytes)));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error stringyfing byte array", e);
                }
            }
        }

        @Override
        public void setBytes(final CustomContentHeader contentType, final byte[] bytes) throws NoSuchVariableException, VariableNotSettableException, IOException, IllegalArgumentException {
            setBytes(null, contentType, bytes);
        }

        @Override
        public void setEntireMessageBodyFromInputStream(final CustomMessageTargetable targetable,
                                                        final CustomContentHeader contentType,
                                                        final InputStream body)
                throws NoSuchVariableException, VariableNotSettableException, IOException, IllegalArgumentException
        {
            if (contentType == null) {
                throw new IllegalArgumentException("contentType cannot be null");
            }
            if (!(contentType instanceof CustomToContentTypeHeaderConverter)) {
                throw new IllegalArgumentException("contentType cannot be null");
            }
            if (body == null) {
                throw new IllegalArgumentException("body cannot be null");
            }

            final Message targetMessage = extractMessage(targetable, true);
            targetMessage.initialize(new ByteArrayStashManager(),
                    ((CustomToContentTypeHeaderConverter)contentType).getInternalType(),
                    body);

            if (logger.isLoggable(Level.FINE)) {
                try {
                    logger.fine("Set the target message document to:\n" + Arrays.toString(IOUtils.slurpStream(body)));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error stringyfing InoutStream", e);
                }
            }
        }

        @Override
        public void setEntireMessageBodyFromInputStream(final CustomContentHeader contentType,
                                                        final InputStream body)
                throws NoSuchVariableException, VariableNotSettableException, IOException, IllegalArgumentException
        {
            setEntireMessageBodyFromInputStream(null, contentType, body);
        }

        @Override
        public CustomContentHeader parseContentTypeValue(final String contentTypeHeaderValue) throws IOException {
            final ContentTypeHeader header = ContentTypeHeader.parseValue(contentTypeHeaderValue);
            return header != null ? new CustomToContentTypeHeaderConverter(header) : null;
        }

        @Override
        public CustomContentHeader createContentType(final CustomContentHeader.Type type) throws IllegalArgumentException {
            if (type == null) {
                throw new IllegalArgumentException("type cannot be null");
            }

            final ContentTypeHeader header = (CustomToContentTypeHeaderConverter.toContentTypeHeader(type));
            if (header == null) {
                throw new IllegalArgumentException("type: " + type + " is not supported");
            }
            return (new CustomToContentTypeHeaderConverter(header));
        }
    }

    private class CustomServiceResponse extends CustomServiceDataExtractor implements ServiceResponse {
        private final PolicyEnforcementContext pec;
        private final Map context = new HashMap();
        private Document document;
        private Document requestDocument;
        private final SecurityContext securityContext;

        public CustomServiceResponse(PolicyEnforcementContext pec) throws IOException, SAXException {
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
            saveServletKnobs(pec, context);

            // plug in the message parts in here
            context.put("messageParts", extractParts(pec.getResponse()));
            //add the ServiceFinder
            context.put("serviceFinder", new ServiceFinderImpl(
                    new CertificateFinderImpl((TrustedCertManager) applicationContext.getBean("trustedCertManager")),
                    new VariableServicesImpl(getAudit()),
                    new SecurePasswordServicesImpl((SecurePasswordManager) applicationContext.getBean("securePasswordManager"))));

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
            try {
                return pec.getVariable(name);
            } catch (NoSuchVariableException e) {
                logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, name );
            }
            return null;
        }

        /**
         * Set a context variable from the policy enforcement context
         */
        @Override
        public void setVariable(String name, Object value) {
            pec.setVariable(name, value);
        }

        @Override
        protected Message extractMessage(CustomMessageTargetable targetable, boolean getOrCreate) throws NoSuchVariableException, VariableNotSettableException {
            if (targetable == null) {
                return pec.getResponse();
            }
            return getOrCreate ?
                    pec.getOrCreateTargetMessage(new CustomToMessageTargetableConverter(targetable), false) :
                    pec.getTargetMessage(new CustomToMessageTargetableConverter(targetable));
        }
    }

    private class CustomServiceRequest extends CustomServiceDataExtractor implements ServiceRequest {
        private final PolicyEnforcementContext pec;
        private final Map context = new HashMap();
        private Document document;
        private final SecurityContext securityContext;

        public CustomServiceRequest(PolicyEnforcementContext pec)
                throws IOException, SAXException {
            this.pec = pec;
            try {
                this.document = (Document) pec.getRequest().getXmlKnob().getDocumentReadOnly().cloneNode(true);
            } catch (Exception e) {
                logger.log(Level.FINE, "This request may not be XML", e);
                this.document = null;
            }
            Vector newCookies = toServletCookies(pec.getCookies());

            saveServletKnobs(pec, context);

            context.put("updatedCookies", newCookies);
            context.put("originalCookies", Collections.unmodifiableCollection(new ArrayList(newCookies)));
            // plug in the message parts in here
            context.put("messageParts", extractParts(pec.getRequest()));
            //add ServiceFinder
            context.put("serviceFinder", new ServiceFinderImpl(
                new CertificateFinderImpl((TrustedCertManager) applicationContext.getBean("trustedCertManager")),
                new VariableServicesImpl(getAudit()),
                new SecurePasswordServicesImpl((SecurePasswordManager) applicationContext.getBean("securePasswordManager"))));

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

        @Override
        protected void onCompletion() {
            Vector cookies = (Vector) context.get("updatedCookies");
            Collection originals = (Collection) context.get("originalCookies");
            for ( final Object cooky : cookies ) {
                Cookie cookie = (Cookie) cooky;
                if ( !originals.contains( cookie ) ) {
                    // doesn't really matter if this has already been added
                    pec.addCookie( CookieUtils.fromServletCookie( cookie, true ) );
                }
            }

            try {
                String[] cookieNames = (String[]) context.get("customAssertionsCookiesToOmit");
                removeCookies(cookieNames, pec);
            } catch (HttpCookie.IllegalFormatException e) {
                throw new IllegalArgumentException("Invalid cookie format");
            }
        }

        /**
         * Access a context variable from the policy enforcement context
         */
        @Override
        public Object getVariable(String name) {
            try {
                return pec.getVariable(name);
            } catch (NoSuchVariableException e) {
                logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, name );
            }
            return null;
        }

        /**
         * Set a context variable from the policy enforcement context
         */
        @Override
        public void setVariable(String name, Object value) {
            pec.setVariable(name, value);
        }

        protected final Message extractMessage(final CustomMessageTargetable targetable, boolean getOrCreate) throws NoSuchVariableException, VariableNotSettableException {
            if (targetable == null) {
                return pec.getRequest();
            }
            return getOrCreate ?
                    pec.getOrCreateTargetMessage(new CustomToMessageTargetableConverter(targetable), false) :
                    pec.getTargetMessage(new CustomToMessageTargetableConverter(targetable));
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
            Set<HttpCookie> contextCookies = pec.getCookies();
            for (HttpCookie cookie: contextCookies) {
                if (cookie.getCookieName().equals(cookieName)) {
                    pec.deleteCookie(cookie);
                }
            }
        }// for
    }
}