package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleExporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleImporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.Functions;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/*
 * Do not make this a @Provider the will make allow
 * @queryParam on fields. This will be added to the rest application using the application context. See
 * /com/l7tech/external/assertions/gatewaymanagement/server/gatewayManagementContext.xml:restAgent
 */
/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * This resource is used to export and import bundles for migration. See <a href="migration.html">migration.html</a>
 * for
 * more documentation on migration and bundling.
 */
@Path(ServerRESTGatewayManagementAssertion.Version1_0_URI + "bundle")
@RequestScoped
public class BundleResource {
    @SpringBean
    private BundleImporter bundleImporter;

    @SpringBean
    private BundleExporter bundleExporter;

    @SpringBean
    private BundleTransformer transformer;

    @SpringBean
    private URLAccessibleLocator urlAccessibleLocator;

    @SpringBean
    private RbacAccessService rbacAccessService;

    @Context
    private UriInfo uriInfo;

    @Context
    private ContainerRequest containerRequest;

    public BundleResource() {
    }

    BundleResource(final BundleImporter bundleImporter, final BundleExporter bundleExporter,
                   final BundleTransformer transformer, final URLAccessibleLocator urlAccessibleLocator,
                   final RbacAccessService rbacAccessService, final UriInfo uriInfo,
                   final ContainerRequest containerRequest) {
        this.bundleImporter = bundleImporter;
        this.bundleExporter = bundleExporter;
        this.transformer = transformer;
        this.urlAccessibleLocator = urlAccessibleLocator;
        this.rbacAccessService = rbacAccessService;
        this.uriInfo = uriInfo;
        this.containerRequest = containerRequest;
    }

    /**
     * Returns the bundle for the given resources. This API call is capable of returning a bundle created from multiple resources.
     *
     * @param defaultAction                      Default bundling action. By default this is NewOrExisting
     * @param exportGatewayRestManagementService If true the gateway management service will be exported too. False by
     *                                           default.
     * @param folderIds                          Folders to export
     * @param serviceIds                         Services to export
     * @param policyIds                          Policies to export
     * @param fullGateway                        True to export the full gateway. False by default
     * @param includeDependencies                True to export with dependencies. False by default
     * @param encryptSecrets                     True to export with encrypted secrets. False by default.
     * @param encryptUsingClusterPassphrase      True to use the cluster passphrase if encrypting secrets. False by default.
     * @param encodedKeyPassphrase               The optional base-64 encoded passphrase to use for the encryption key when encrypting secrets.
     * @return The bundle for the resources
     * @throws IOException
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     */
    @GET
    public Item<Bundle> exportBundle(@QueryParam("defaultAction") @ChoiceParam({"NewOrExisting", "NewOrUpdate"}) @DefaultValue("NewOrExisting") String defaultAction,
                                     @QueryParam("exportGatewayRestManagementService") @DefaultValue("false") Boolean exportGatewayRestManagementService,
                                     @QueryParam("folder") List<String> folderIds,
                                     @QueryParam("service") List<String> serviceIds,
                                     @QueryParam("policy") List<String> policyIds,
                                     @QueryParam("all") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean fullGateway,
                                     @QueryParam("includeDependencies") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean includeDependencies,
                                     @QueryParam("encryptSecrets") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean encryptSecrets,
                                     @QueryParam("encryptUsingClusterPassphrase") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean encryptUsingClusterPassphrase,
                                     @HeaderParam("L7-key-passphrase") @Since(RestManVersion.VERSION_1_0_1) String encodedKeyPassphrase) throws IOException, ResourceFactory.ResourceNotFoundException, FindException, CannotRetrieveDependenciesException, GeneralSecurityException {
        rbacAccessService.validateFullAdministrator();
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("defaultAction", "exportGatewayRestManagementService", "folder", "service", "policy", "all", "includeDependencies", "encryptSecrets", "encryptUsingClusterPassphrase"));
        final String encodedPassphrase = getEncryptionPassphrase(encryptSecrets, encryptUsingClusterPassphrase, encodedKeyPassphrase);
        //validate that something is being exported
        if (folderIds.isEmpty() && serviceIds.isEmpty() && policyIds.isEmpty() && !fullGateway) {
            throw new InvalidArgumentException("Must specify at least one folder, service or policy to export");
        }

        List<EntityHeader> entityHeaders = new ArrayList<>(folderIds.size() + serviceIds.size() + policyIds.size());

        for (String folderId : folderIds) {
            entityHeaders.add(new EntityHeader(folderId, EntityType.FOLDER, null, null));
        }
        for (String serviceId : serviceIds) {
            entityHeaders.add(new EntityHeader(serviceId, EntityType.SERVICE, null, null));
        }
        for (String policyId : policyIds) {
            entityHeaders.add(new EntityHeader(policyId, EntityType.POLICY, null, null));
        }

        if (fullGateway && !entityHeaders.isEmpty()) {
            throw new InvalidArgumentException("If specifying full gateway export (all=true) do not give any other entity id's");
        }

        final Bundle bundle = createBundle(true, Mapping.Action.valueOf(defaultAction), "id",
                exportGatewayRestManagementService, includeDependencies, encryptSecrets, encodedPassphrase,
                entityHeaders.toArray(new EntityHeader[entityHeaders.size()]));
        return new ItemBuilder<>(transformer.convertToItem(bundle))
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .build();
    }

    /**
     * Returns the bundle for the given resource type. The resource type is either a policy, service, or folder
     *
     * @param resourceType                       Resource type. Either folder, service or policy
     * @param id                                 ID of the resource to bundle
     * @param defaultAction                      Default bundling action. By default this is NewOrExisting
     * @param defaultMapBy                       Default map by action.
     * @param includeRequestFolder               For a folder export, specifies whether to include the folder in the
     *                                           bundle or just its contents.
     * @param exportGatewayRestManagementService If true the gateway management service will be exported too. False by
     *                                           default.
     * @param includeDependencies                True to export with dependencies. False by default
     * @param encryptSecrets                     True to export with encrypted secrets. False by default.
     * @param encryptUsingClusterPassphrase      True to use the cluster passphrase if encrypting secrets. False by default
     * @param encodedKeyPassphrase               The optional base-64 encoded passphrase to use for the encryption key when encrypting secrets.
     * @return The bundle for the resource
     * @throws IOException
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     * @title Folder, Service or Policy Export
     */
    @GET
    @Path("{resourceType}/{id}")
    public Item<Bundle> exportFolderServiceOrPolicyBundle(@PathParam("resourceType") @ChoiceParam({"folder", "policy", "service"}) String resourceType,
                                                          @PathParam("id") Goid id,
                                                          @QueryParam("defaultAction") @ChoiceParam({"NewOrExisting", "NewOrUpdate"}) @DefaultValue("NewOrExisting") String defaultAction,
                                                          @QueryParam("defaultMapBy") @DefaultValue("id") @ChoiceParam({"id", "name", "guid"}) String defaultMapBy,
                                                          @QueryParam("includeRequestFolder") @DefaultValue("false") Boolean includeRequestFolder,
                                                          @QueryParam("exportGatewayRestManagementService") @DefaultValue("false") Boolean exportGatewayRestManagementService,
                                                          @QueryParam("includeDependencies") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean includeDependencies,
                                                          @QueryParam("encryptSecrets") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean encryptSecrets,
                                                          @QueryParam("encryptUsingClusterPassphrase") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean encryptUsingClusterPassphrase,
                                                          @HeaderParam("L7-key-passphrase") @Since(RestManVersion.VERSION_1_0_1) String encodedKeyPassphrase) throws IOException, ResourceFactory.ResourceNotFoundException, FindException, CannotRetrieveDependenciesException, GeneralSecurityException {
        rbacAccessService.validateFullAdministrator();
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("defaultAction", "defaultMapBy", "includeRequestFolder", "exportGatewayRestManagementService", "includeDependencies", "encryptSecrets", "encryptUsingClusterPassphrase"));
        final String encodedPassphrase = getEncryptionPassphrase(encryptSecrets, encryptUsingClusterPassphrase, encodedKeyPassphrase);
        final EntityType entityType;
        switch (resourceType) {
            case "folder":
                entityType = EntityType.FOLDER;
                break;
            case "policy":
                entityType = EntityType.POLICY;
                break;
            case "service":
                entityType = EntityType.SERVICE;
                break;
            default:
                throw new IllegalArgumentException("Illegal resourceType. Can only generate bundles for folders, policies, or resources.");
        }

        EntityHeader header = new EntityHeader(id, entityType, null, null);
        final Bundle bundle = createBundle(includeRequestFolder, Mapping.Action.valueOf(defaultAction), defaultMapBy,
                exportGatewayRestManagementService, includeDependencies, encryptSecrets, encodedPassphrase, header);
        return new ItemBuilder<>(transformer.convertToItem(bundle))
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .build();
    }

    /**
     * This will import a bundle.
     *
     * @param test                 If true the bundle import will be tested no changes will be made to the gateway
     * @param activate             False to not activate the updated services and policies.
     * @param versionComment       The comment to set for updated/created services and policies
     * @param encodedKeyPassphrase The optional base-64 encoded passphrase to use for the encryption key when encrypting passwords.
     * @param bundle               The bundle to import
     * @return The mappings performed during the bundle import
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    public Response importBundle(@QueryParam("test") @DefaultValue("false") final boolean test,
                                 @QueryParam("activate") @DefaultValue("true") final boolean activate,
                                 @QueryParam("versionComment") final String versionComment,
                                 @HeaderParam("L7-key-passphrase") @Since(RestManVersion.VERSION_1_0_1) String encodedKeyPassphrase,
                                 final Bundle bundle) throws Exception {
        rbacAccessService.validateFullAdministrator();
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("test", "activate", "versionComment"));

        List<Mapping> mappings =
                bundleImporter.importBundle(bundle, test, activate, versionComment, encodedKeyPassphrase);

        Item<Mappings> item = new ItemBuilder<Mappings>("Bundle mappings", "BUNDLE MAPPINGS")
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .setContent(ManagedObjectFactory.createMappings(mappings))
                .build();
        return containsErrors(mappings) ? Response.status(Response.Status.CONFLICT).entity(item).build() : Response.ok(item).build();
    }

    /**
     * Checks if there are errors in the mappings list. If there is any error it will fail.
     *
     * @param mappings The list of mappings to check for errors
     * @return true if there is an error in the mappings list
     */
    private boolean containsErrors(List<Mapping> mappings) {
        return Functions.exists(mappings, new Functions.Unary<Boolean, Mapping>() {
            @Override
            public Boolean call(Mapping mapping) {
                return mapping.getErrorType() != null;
            }
        });
    }

    /**
     * Creates a bundle from the entity headers given
     *
     * @param includeRequestFolder true to include the request folder
     * @param defaultAction        The default mapping action to take
     * @param defaultMapBy         The default map by property
     * @param includeDependencies  true to include dependencies
     * @param headers              The header to bundle a bundle for
     * @return The bundle from the headers
     * @throws FindException
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private Bundle createBundle(boolean includeRequestFolder,
                                @NotNull final Mapping.Action defaultAction,
                                @NotNull final String defaultMapBy,
                                boolean exportGatewayRestManagementService,
                                boolean includeDependencies,
                                boolean encryptSecrets,
                                @Nullable final String encodedKeyPassphrase,
                                @NotNull final EntityHeader... headers) throws FindException, CannotRetrieveDependenciesException, FileNotFoundException, GeneralSecurityException {
        //build the bundling properties
        final Properties bundleOptionsBuilder = new Properties();
        bundleOptionsBuilder.setProperty(BundleExporter.IncludeRequestFolderOption, String.valueOf(includeRequestFolder));
        bundleOptionsBuilder.setProperty(BundleExporter.DefaultMappingActionOption, defaultAction.toString());
        bundleOptionsBuilder.setProperty(BundleExporter.DefaultMapByOption, defaultMapBy);
        bundleOptionsBuilder.setProperty(BundleExporter.EncryptSecrets, Boolean.toString(encryptSecrets));

        //ignore the rest man service so it is not exported
        if ( containerRequest.getProperty("ServiceId") != null && !exportGatewayRestManagementService) {
            bundleOptionsBuilder.setProperty(BundleExporter.IgnoredEntityIdsOption, containerRequest.getProperty("ServiceId").toString());
        }
        if(containerRequest.getProperty("ServiceId")!=null) {
            bundleOptionsBuilder.setProperty(BundleExporter.ServiceUsed, containerRequest.getProperty("ServiceId").toString());
        }
        //create the bundle export
        return bundleExporter.exportBundle(bundleOptionsBuilder, includeDependencies, encryptSecrets, encryptSecrets ? encodedKeyPassphrase : null, headers);
    }

    private String getEncryptionPassphrase(final Boolean encrypt, final Boolean useClusterPassphrase, final String customPassphrase) {
        String passphrase = null;
        if (encrypt) {
            if (!useClusterPassphrase && customPassphrase != null) {
                passphrase = customPassphrase;
            } else if (!useClusterPassphrase && customPassphrase == null) {
                throw new InvalidArgumentException("Passphrase is required for encryption");
            }
        }
        return passphrase;
    }
}
