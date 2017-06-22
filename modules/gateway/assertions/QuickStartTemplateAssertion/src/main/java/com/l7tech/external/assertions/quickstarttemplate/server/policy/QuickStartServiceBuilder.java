package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.google.common.collect.Sets;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartMapper;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.Service;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.resolution.NonUniqueServiceResolutionException;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.*;

/**
 * Dedicated class for building {@link PublishedService}.
 */
public class QuickStartServiceBuilder {

    @NotNull private final ServiceCache serviceCache;
    @NotNull private final FolderManager folderManager;
    @NotNull private final QuickStartPublishedServiceLocator serviceLocator;
    @NotNull private final QuickStartMapper mapper;

    public QuickStartServiceBuilder(
            @NotNull final ServiceCache serviceCache,
            @NotNull final FolderManager folderManager,
            @NotNull final QuickStartPublishedServiceLocator serviceLocator,
            @NotNull final QuickStartEncapsulatedAssertionLocator assertionLocator,
            @NotNull final ClusterPropertyManager clusterPropertyManager,
            @NotNull final Properties mapperProperties
            ) {
        this.serviceCache = serviceCache;
        this.folderManager = folderManager;
        this.serviceLocator = serviceLocator;
        this.mapper = new QuickStartMapper(assertionLocator, clusterPropertyManager, mapperProperties);
    }

    // TODO is there a better time in the assertion lifecycle to set assertion registry?
    public void setAssertionRegistry(AssertionRegistry assertionRegistry) {
        mapper.setAssertionRegistry(assertionRegistry);
    }

    /**
     * Creates a new {@link PublishedService service} using the specified {@link ServiceContainer service JSON object} as source.
     *
     * @param serviceContainer       the JSON {@link ServiceContainer} object.  Mandatory and cannot be {@code null}.
     * @return a new {@link PublishedService} instance, never {@code null}.
     * @throws FindException if an error happens communicating with the {@code Gateway}.
     * @throws QuickStartPolicyBuilderException if there is a service {@code URI} resolution conflict.
     *                                          TODO: perhaps reword this to cover generic rather then specific QST exceptions.
     */
    @NotNull
    public PublishedService createService(@NotNull final ServiceContainer serviceContainer) throws FindException, QuickStartPolicyBuilderException {
        final Service service = serviceContainer.service;
        final List<Assertion> assertions = mapper.getAssertions(service);

        final PublishedService publishedService = new PublishedService();
        publishedService.setName(service.name);
        publishedService.setFolder(getRootFolder());
        publishedService.setSoap(false);
        publishedService.setDisabled(false);

        // Set the service URI and fail if there is an existing service with the same URI (service URI conflict resolution)
        publishedService.setRoutingUri(service.gatewayUri);
        if (hasResolutionConflict(publishedService)) {
            throw new QuickStartPolicyBuilderException("Resolution parameters conflict for service '" + publishedService.getName() + "' " +
                    "because an existing service is already using the URI " + publishedService.getRoutingUri() + ". " +
                    "Try publishing this service using a different routing URI.");
        }

        publishedService.setHttpMethods(Sets.newHashSet(service.httpMethods));
        generatePolicy(publishedService, assertions);
        publishedService.setTracingEnabled(false);
        publishedService.parseWsdlStrategy(new ServiceDocumentWsdlStrategy(null));
        try {
            publishedService.setWsdlUrl(null);
        } catch (final MalformedURLException ignore) {
            // cannot happen when passing null
        }
        publishedService.setWsdlXml(null);

        return publishedService;

    }

    /**
     * Updates the content of the {@link PublishedService service}, specified by the {@code goid},
     * using the specified {@link ServiceContainer JSON object} as source or creates a new {@link PublishedService service}
     * if the specified {@code goid} doesn't exist.
     *
     * @param goid                           the {@link PublishedService service} {@link Goid goid}.  Mandatory and cannot be {@code null}.
     * @param serviceContainer               the JSON {@link ServiceContainer} object.  Mandatory and cannot be {@code null}.
     * @return updated {@link PublishedService} object or a new {@link PublishedService} instance in case the specified {@code goid} doesn't exist, never {@code null}.
     * @throws FindException if an error happens communicating with the {@code Gateway}.
     * @throws QuickStartPolicyBuilderException if there is a service {@code URI} resolution conflict.
     *                                          TODO: perhaps reword this to cover generic rather then specific QST exceptions.
     */
    @NotNull
    public PublishedService updateService(
            @NotNull final Goid goid,
            @NotNull final ServiceContainer serviceContainer
    ) throws FindException, QuickStartPolicyBuilderException {
        PublishedService publishedService = serviceLocator.findByGoid(goid);

        // DO NOT Fail -> if the serviceID of the service to be updated does not exist
        //if (publishedService == null) {
        //    throw new QuickStartPolicyBuilderException("Unable to find a service with ServiceID " + goid.toString());
        //}

        if (publishedService == null) {
            publishedService = createService(serviceContainer);
            // TODO: this is a PUT hence we should set the service goid to the specified goid, was this missed intentionally???
            publishedService.setGoid(goid);
        } else {
            final Service service = serviceContainer.service;
            final List<Assertion> assertions = mapper.getAssertions(service);
            publishedService.setName(service.name);
            publishedService.setRoutingUri(service.gatewayUri);
            publishedService.setHttpMethods(Sets.newHashSet(service.httpMethods));
            generatePolicy(publishedService, assertions);
        }

        return publishedService;
    }


    // TODO in the future, refactor out common code from com.l7tech.console.panels.PublishInternalServiceWizard.saveNonSoapServiceWithResolutionCheck()
    private boolean hasResolutionConflict(final PublishedService newService) throws QuickStartPolicyBuilderException {
        try {
            return ( (!newService.isDisabled()) &&
                    (Goid.isDefault(newService.getGoid()) &&
                            (!generateResolutionReportForNewService(newService).isSuccess())) );
        } catch ( FindException e ) {
            throw new QuickStartPolicyBuilderException("Error checking for service resolution conflict.", e);
        }
    }

    // TODO in the future, refactor out common code from com.l7tech.server.service.ServiceAdminImpl.generateResolutionReportForNewService()
    private ServiceAdmin.ResolutionReport generateResolutionReportForNewService(final PublishedService newService) throws FindException {
        if ( !Goid.isDefault(newService.getGoid())) {
            throw new FindException("Service must not be persistent.");
        }

        final Collection<ServiceAdmin.ConflictInfo> conflictInformation = new ArrayList<>();
        try {
            serviceCache.checkResolution( newService );
        } catch ( NonUniqueServiceResolutionException e ) {
            for ( final Goid serviceGoid : e.getConflictingServices() ) {
                for ( final Triple<String,String,String> parameters : e.getParameters( serviceGoid ) ) {
                    conflictInformation.add( new ServiceAdmin.ConflictInfo(
                            parameters.left,
                            e.getServiceName( serviceGoid, true ),
                            e.getServiceName( serviceGoid, false ),
                            serviceGoid,
                            parameters.right,
                            parameters.middle) );
                }
            }
        } catch ( ServiceResolutionException e ) {
            throw new FindException( ExceptionUtils.getMessage(e) );
        }

        return new ServiceAdmin.ResolutionReport( newService.getRoutingUri()!=null,
                conflictInformation.toArray(new ServiceAdmin.ConflictInfo[conflictInformation.size()]) );
    }

    /**
     * Utility method to get the {@code Gateway} root folder.
     *
     * TODO: Most likely the root folder will NOT change, hence perhaps deprecate this method and get the root-folder as a field in the constructor (or pass it as an argument).
     *
     * @return the {@code Gateway} root folder, never {@code null}.
     * @throws FindException if an error happens communicating with the {@code Gateway}.
     */
    @NotNull
    private Folder getRootFolder() throws FindException {
        final Folder rootFolder = folderManager.findByPrimaryKey(Folder.ROOT_FOLDER_ID);
        if (rootFolder == null) {
            throw new IllegalStateException("Cannot find Gateway Root folder!");
        }
        return rootFolder;
    }

    /**
     * Utility method to generate the policy (from the ordered {@code encapsulatedAssertions}) for the specified {@code service}.
     *
     * @param service       the {@link PublishedService} for which we are going to generate the policy
     * @param assertions    ordered list of {@link Assertion}'s to use as a source when generating the policy
     */
    private static void generatePolicy(@NotNull final PublishedService service, @NotNull final List<Assertion> assertions) {
        final Policy policy = service.getPolicy();
        assert policy != null;
        if (policy.getGuid() == null) {
            policy.setGuid(UUID.randomUUID().toString());
        }
        final AllAssertion allAss = new AllAssertion();
        assertions.forEach(allAss::addChild);
        policy.setXml(WspWriter.getPolicyXml(allAss));
    }
}
