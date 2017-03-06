package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
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
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.transform.dom.DOMResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private final static String ROOT_FOLDER_GOID = Folder.ROOT_FOLDER_ID.toString();

    private final @NotNull WspReader wspReader;
    private final @NotNull ServerPolicyFactory serverPolicyFactory;
    private final @NotNull ProtectedEntityTracker protectedEntityTracker;
    private final @NotNull QuickStartEncapsulatedAssertionTemplate quickStartEncapsulatedAssertionTemplate;

    private final @Nullable UserBean authenticatedUser;

    private ServerAssertion serverRestGatewayManagementAssertion = null;

    public QuickStartServiceBuilderRestmanImpl(
            @NotNull final WspReader wspReader,
            @NotNull final ServerPolicyFactory serverPolicyFactory,
            @NotNull final ProtectedEntityTracker protectedEntityTracker,
            @Nullable final UserBean authenticatedUser,
            @NotNull final QuickStartEncapsulatedAssertionTemplate quickStartEncapsulatedAssertionTemplate
    ) {
        this.wspReader = wspReader;
        this.serverPolicyFactory = serverPolicyFactory;
        this.protectedEntityTracker = protectedEntityTracker;
        this.authenticatedUser = authenticatedUser;
        this.quickStartEncapsulatedAssertionTemplate = quickStartEncapsulatedAssertionTemplate;
    }

    @Override
    public void createService() throws Exception {
        // TODO return import results to client?
        importBundle(createServiceBundle(String.class));
    }

    /**
     * Creates service bundle with the specified output type.
     * <br/>
     * Supported types:<br/>
     * <ul>
     *     <li>{@code ServiceMO}: RESTMan managed service object</li>
     *     <li>{@code String}: RESTMam bundle XML</li>
     *     <li>{@code Document}: RESTMam bundle XML</li>
     *     <li>{@code Message}: RESTMam bundle XML</li>
     * </ul>
     *
     * @param resType  required type of the service bundle
     * @throws Exception if an error happens while creating the bundle.
     * IllegalArgumentException is thrown when {@code resType} is unsupported.
     */
    @Override
    public <T> T createServiceBundle(@NotNull final Class<T> resType) throws Exception {
        final PublishedService publishedService = quickStartEncapsulatedAssertionTemplate.getPublishedService();

        final ServiceMO serviceMO = asResource(publishedService);
        if (ServiceMO.class.equals(resType)) {
            return resType.cast(serviceMO);
        }

        if (String.class.equals(resType)) {
            final DOMResult result = new DOMResult();
            MarshallingUtils.marshal(serviceMO, result, false);
            return resType.cast(XmlUtil.nodeToString(result.getNode()));
        }

        if (Document.class.equals(resType)) {
            final DOMResult result = new DOMResult();
            MarshallingUtils.marshal(serviceMO, result, false);
            return resType.cast(result.getNode());
        }

        if (Message.class.equals(resType)) {
            final DOMResult result = new DOMResult();
            MarshallingUtils.marshal(serviceMO, result, false);
            return resType.cast(new Message((Document) result.getNode()));
        }

        throw new IllegalArgumentException("Unsupported result class: " + resType.getName());
    }

    /**
     * Utility method to create a RESTMan managed object from the specified {@code publishedService}
     *
     * <p>
     * TODO: Consider refactoring to reuse {@code com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory#asResource()}
     * </p>
     *
     * @param publishedService    input service
     *
     * @return RESTMan managed service object {@link ServiceMO}, never {@code null}.
     */
    @NotNull
    private ServiceMO asResource(@NotNull final PublishedService publishedService) {
        final Policy policy = publishedService.getPolicy();

        final ServiceMO service = ManagedObjectFactory.createService();
        final ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();

        //service.setId( publishedService.getId() );
        service.setServiceDetail(serviceDetail);
        final List<ResourceSet> resourceSets = new ArrayList<>();
        resourceSets.add( buildPolicyResourceSet( policy ) );
        service.setResourceSets( resourceSets );

        //serviceDetail.setId( publishedService.getId() );
        //serviceDetail.setVersion( publishedService.getVersion() );
        serviceDetail.setFolderId( ROOT_FOLDER_GOID );
        serviceDetail.setName( publishedService.getName() );
        serviceDetail.setEnabled( !publishedService.isDisabled() );
        serviceDetail.setServiceMappings( buildServiceMappings(publishedService) );

        // TODO handle SecurityZone once we support it via json payload

        return service;
    }

    private static final String POLICY_TAG = "policy";
    private static final String POLICY_TYPE = "policy";

    @NotNull
    private ResourceSet buildPolicyResourceSet(@NotNull final Policy policy) {
        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag(POLICY_TAG);
        final Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Collections.singletonList(resource));
        resource.setType(POLICY_TYPE);
        resource.setContent(policy.getXml());
        if (policy.getVersion() != -1 ) {
            resource.setVersion( policy.getVersion() );
        }
        return resourceSet;
    }

    @NotNull
    private List<ServiceDetail.ServiceMapping> buildServiceMappings( final PublishedService publishedService ) {
        final List<ServiceDetail.ServiceMapping> mappings = new ArrayList<>();

        final ServiceDetail.HttpMapping httpMapping = ManagedObjectFactory.createHttpMapping();
        httpMapping.setUrlPattern( publishedService.getRoutingUri() );
        httpMapping.setVerbs( Functions.map( publishedService.getHttpMethodsReadOnly(), new Functions.Unary<String,HttpMethod>(){
            @Override
            public String call( final HttpMethod httpMethod ) {
                return httpMethod.name();
            }
        }) );
        mappings.add( httpMapping );

        if ( publishedService.isSoap() ) {
            ServiceDetail.SoapMapping soapMapping = ManagedObjectFactory.createSoapMapping();
            soapMapping.setLax( publishedService.isLaxResolution() );
            mappings.add( soapMapping );
        }

        return mappings;
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