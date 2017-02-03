package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.Pair;
import com.l7tech.util.TooManyChildElementsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Build service using restman bundle.
 */
public class QuickStartServiceBuilderRestmanImpl implements QuickStartServiceBuilder {
    private static final Logger logger = Logger.getLogger(QuickStartServiceBuilderRestmanImpl.class.getName());

    private static final String REST_GATEWAY_MANAGEMENT_POLICY_XML =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                    "<wsp:All wsp:Usage=\"Required\">" +
                    "<L7p:RESTGatewayManagement>" +
                    "<L7p:OtherTargetMessageVariable stringValue=\"request\"/>" +
                    "<L7p:Target target=\"OTHER\"/>" +
                    "</L7p:RESTGatewayManagement>" +
                    "</wsp:All>" +
                    "</wsp:Policy>";

    private final @NotNull WspReader wspReader;
    private final @NotNull ServerPolicyFactory serverPolicyFactory;
    private final @NotNull ProtectedEntityTracker protectedEntityTracker;
    private final @NotNull QuickStartEncapsulatedAssertionTemplate quickStartEncapsulatedAssertionTemplate;

    private final @Nullable UserBean authenticatedUser;

    private ServerAssertion serverRestGatewayManagementAssertion = null;

    public QuickStartServiceBuilderRestmanImpl(@NotNull final WspReader wspReader, @NotNull final ServerPolicyFactory serverPolicyFactory, @NotNull final ProtectedEntityTracker protectedEntityTracker,
                                               @Nullable final UserBean authenticatedUser, @NotNull final QuickStartEncapsulatedAssertionTemplate quickStartEncapsulatedAssertionTemplate) {
        this.wspReader = wspReader;
        this.serverPolicyFactory = serverPolicyFactory;
        this.protectedEntityTracker = protectedEntityTracker;
        this.authenticatedUser = authenticatedUser;
        this.quickStartEncapsulatedAssertionTemplate = quickStartEncapsulatedAssertionTemplate;
    }

    @Override
    public void createService() throws Exception {
        final String encassSubResource =
                        "        &lt;L7p:Encapsulated&gt;\n" +
                        "            &lt;L7p:EncapsulatedAssertionConfigGuid stringValue=&quot;{0}&quot;/&gt;\n" +
                        "            &lt;L7p:EncapsulatedAssertionConfigName stringValue=&quot;{1}&quot;/&gt;\n" +
                        "            {2}" +
                        "        &lt;/L7p:Encapsulated&gt;\n";

        String encassSubResourceParameterEntry =
                        "                &lt;L7p:entry&gt;\n" +
                        "                    &lt;L7p:key stringValue=&quot;{0}&quot;/&gt;\n" +
                        "                    &lt;L7p:value stringValue=&quot;{1}&quot;/&gt;\n" +
                        "                &lt;/L7p:entry&gt;\n" ;

        StringBuilder encassResourceSb = new StringBuilder();
        for (EncapsulatedAssertion encapsulatedAssertion : quickStartEncapsulatedAssertionTemplate.getEncapsulatedAssertions()) {

            StringBuilder encassResourceParameterSb = new StringBuilder();
            if (encapsulatedAssertion.getParameterNames().size() > 0) {
                encassResourceParameterSb.append("            &lt;L7p:Parameters mapValue=&quot;included&quot;&gt;\n");
                for (String parameterName : encapsulatedAssertion.getParameterNames()) {
                    encassResourceParameterSb.append(MessageFormat.format(encassSubResourceParameterEntry, parameterName, encapsulatedAssertion.getParameter(parameterName)));
                }
                encassResourceParameterSb.append("            &lt;/L7p:Parameters&gt;");
            }

            encassResourceSb.append(MessageFormat.format(encassSubResource,
                    encapsulatedAssertion.getEncapsulatedAssertionConfigGuid(),
                    encapsulatedAssertion.getEncapsulatedAssertionConfigName(),
                    encassResourceParameterSb.toString()));
        }

        // TODO use reflection to add service attributes dynamically
        PublishedService publishedService = quickStartEncapsulatedAssertionTemplate.getPublishedService();

        // TODO hardcode for now
        final String httpMethods =
                "                            <l7:Verb>GET</l7:Verb>\n" +
                "                            <l7:Verb>POST</l7:Verb>\n" +
                "                            <l7:Verb>PUT</l7:Verb>\n";

        String serviceCreateRestmanReq =
                "        <l7:Service xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" version=\"0\">\n" +
                "            <l7:ServiceDetail folderId=\"0000000000000000ffffffffffffec76\">\n" +
                "                <l7:Name>{0}</l7:Name>\n" +
                "                <l7:Enabled>true</l7:Enabled>\n" +
                "                <l7:ServiceMappings>\n" +
                "                    <l7:HttpMapping>\n" +
                "                        <l7:UrlPattern>{1}</l7:UrlPattern>\n" +
                "                        <l7:Verbs>\n" +
                "                            {2}\n" +
                "                        </l7:Verbs>\n" +
                "                    </l7:HttpMapping>\n" +
                "                </l7:ServiceMappings>\n" +
                "            </l7:ServiceDetail>\n" +
                "            <l7:Resources>\n" +
                "                <l7:ResourceSet tag=\"policy\">\n" +
                "                    <l7:Resource type=\"policy\" version=\"0\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                "        {3}\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;\n" +
                "</l7:Resource>\n" +
                "                </l7:ResourceSet>\n" +
                "            </l7:Resources>\n" +
                "        </l7:Service>";

        // TODO return import results to client?
        importBundle(MessageFormat.format(serviceCreateRestmanReq, publishedService.getName(), publishedService.getRoutingUri(), httpMethods, encassResourceSb.toString()));
    }

    private String importBundle(@NotNull final String requestXml) throws Exception {
        final RestmanInvoker restmanInvoker = getRestmanInvoker();
        restmanInvoker.setAuthenticatedUser(authenticatedUser);

        try {
            final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);
            pec.setVariable(RestmanInvoker.VAR_RESTMAN_URI, "1.0/services");
            pec.setVariable(RestmanInvoker.VAR_RESTMAN_ACTION, "POST");

            // allow this code to "punch through" read-only entities
            Pair<AssertionStatus, RestmanMessage> result;
            try {
                result = protectedEntityTracker.doWithEntityProtectionDisabled(getProtectedEntityTrackerCallable(restmanInvoker, pec, requestXml));
            } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse |
                    GatewayManagementDocumentUtilities.UnexpectedManagementResponse |
                    InterruptedException e) {
                throw e;
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }

            if (AssertionStatus.NONE != result.left) {
                String msg = "Unable to install bundle. Failed to invoke REST Gateway Management assertion: " + result.left.getMessage();
                logger.log(Level.WARNING, msg);
                throw new QuickStartPolicyBuilderException(result.left.getMessage());
            }

            if (result.right.hasMappingError()) {
                String msg = "Unable to install bundle due to mapping errors:\n" + result.right.getAsString();
                logger.log(Level.WARNING, msg);
                throw new QuickStartPolicyBuilderException(result.right.getAsString());
            }
            return result.right.getAsString();
        } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse | IOException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new QuickStartPolicyBuilderException(ExceptionUtils.getMessage(e), e);
        } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw getRestmanErrorDetail(e);
        } catch (InterruptedException e) {
            // do nothing
        }

        return "";
    }

    private RestmanInvoker getRestmanInvoker() throws QuickStartPolicyBuilderException {
        // create RestmanInvoker
        if (serverRestGatewayManagementAssertion == null) {
            try {
                Assertion assertion = wspReader.parseStrictly(REST_GATEWAY_MANAGEMENT_POLICY_XML, WspReader.Visibility.omitDisabled);
                serverRestGatewayManagementAssertion = serverPolicyFactory.compilePolicy(assertion, false);
            } catch (IOException | ServerPolicyException | LicenseException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                throw new QuickStartPolicyBuilderException("Unable to initialize ServerRESTGatewayManagementAssertion.", e);
            }
        }

        GatewayManagementInvoker invoker = context -> serverRestGatewayManagementAssertion.checkRequest(context);

        return new RestmanInvoker(() -> {
            // nothing to do in cancelled callback.
            return true;
        }, invoker);
    }

    /**
     * This method will parse out the restman response and look for the <l7:Detail> tag to get the error message
     * details.  If that string contains the word "Exception", it will create a new Exception based on that string
     * value and throw it as a regular Exception (this is for unhandled exceptions).  Not the ideal way to
     * do this, but given the SK codebase as is stands now, there isn't much of a choice.
     *
     * @param ex Restman exception
     * @return The error message detail string
     * @throws Exception
     */
    private QuickStartPolicyBuilderException getRestmanErrorDetail(@NotNull GatewayManagementDocumentUtilities.UnexpectedManagementResponse ex) throws Exception {
        try {
            final Document doc = XmlUtil.parse(ExceptionUtils.getMessage(ex));
            // get error type
            final Element msgTypeNode = XmlUtil.findExactlyOneChildElementByName(doc.getDocumentElement(), doc.getNamespaceURI(), "Type");
            final String errorType = XmlUtil.getTextValue(msgTypeNode, true);
            // get error message
            final Element msgDetailsNode = XmlUtil.findExactlyOneChildElementByName(doc.getDocumentElement(), doc.getNamespaceURI(), "Detail");
            final String detailMsg = XmlUtil.getTextValue(msgDetailsNode, true);
            // BundleResource.importBundle fails with either CONFLICT or BAD_REQUEST (in case one of the entities in the bundle are invalid i.e. throws ResourceFactory.InvalidResourceException)
            // CONFLICT should be handled by test so it is of no interest here
            // BAD_REQUEST i.e. when ResourceFactory.InvalidResourceException is throw the error type is "InvalidResource", as per ExceptionMapper.handleOperationException()
            // if one of the above methods are changed this logic must be changed as well
            if ("InvalidResource".equalsIgnoreCase(errorType)) {
                return new QuickStartPolicyBuilderException(detailMsg);
            } else if (detailMsg.contains("Exception")) {
                throw new Exception(detailMsg);
            } else {
                return new QuickStartPolicyBuilderException(detailMsg);
            }
        } catch (final SAXException | MissingRequiredElementException | TooManyChildElementsException e) {
            throw ex;
        }
    }

    private Callable<Pair<AssertionStatus, RestmanMessage>> getProtectedEntityTrackerCallable(final RestmanInvoker restmanInvoker, final PolicyEnforcementContext pec, final String requestXml) {
        return () -> restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
    }
}