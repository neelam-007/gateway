package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleExporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleImporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/*
 * Do not make this a @Provider the will make allow
 * @queryParam on fields. This will be added to the rest application using the application context. See
 * /com/l7tech/external/assertions/gatewaymanagement/server/gatewayManagementContext.xml:restAgent
 */

/**
 * This resource is used to export and import bundles for migration.
 */
@Path(BundleResource.Version_URI + "bundle")
@RequestScoped
public class BundleResource {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

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

    /**
     * This method is not Implemented yet. This is meant to return a bundle of the full gateway.
     *
     * @return The bundle of the full gateway
     */
    //@GET
    public Item exportGateway() {
        rbacAccessService.validateFullAdministrator();
        //TODO: need a way to export the entire gateway as a bundle
        return new ItemBuilder<Bundle>("Bundle", "BUNDLE").build();
    }

    /**
     * Returns the bundle for the given resources.
     *
     * @param defaultAction                      The default bundling action. By default this is NewOrExisting
     * @param exportGatewayRestManagementService If true the gateway management service will be exported too. False by
     *                                           default.
     * @param folderIds                          The folders to export
     * @param serviceIds                         The services to export
     * @param policyIds                          The policies to export
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
                                     @QueryParam("policy") List<String> policyIds) throws IOException, ResourceFactory.ResourceNotFoundException, FindException, CannotRetrieveDependenciesException {
        rbacAccessService.validateFullAdministrator();

        //validate that something is being exported
        if (folderIds.isEmpty() && serviceIds.isEmpty() && policyIds.isEmpty()) {
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

        return new ItemBuilder<>(transformer.convertToItem(createBundle(true, Mapping.Action.valueOf(defaultAction), "id", exportGatewayRestManagementService, entityHeaders.toArray(new EntityHeader[entityHeaders.size()]))))
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    /**
     * Returns the for the given resource type. The resource type is either a policy, service, or folder
     *
     * @param resourceType                       The resource type. Either folder, service or policy
     * @param id                                 The id of the resource to bundle
     * @param defaultAction                      The default bundling action. By default this is NewOrExisting
     * @param defaultMapBy                       The default map by action.
     * @param includeRequestFolder               For a folder export, specifies whether to include the folder in the
     *                                           bundle or just its contents.
     * @param exportGatewayRestManagementService If true the gateway management service will be exported too. False by
     *                                           default.
     * @return The bundle for the resource
     * @throws IOException
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     */
    @GET
    @Path("{resourceType}/{id}")
    public Item<Bundle> exportFolderServicePolicyBundle(@PathParam("resourceType") @ChoiceParam({"folder", "policy", "service"}) String resourceType,
                                     @PathParam("id") Goid id,
                                     @QueryParam("defaultAction") @ChoiceParam({"NewOrExisting", "NewOrUpdate"}) @DefaultValue("NewOrExisting") String defaultAction,
                                     @QueryParam("defaultMapBy") @DefaultValue("id") @ChoiceParam({"id", "name", "guid"}) String defaultMapBy,
                                     @QueryParam("includeRequestFolder") @DefaultValue("false") Boolean includeRequestFolder,
                                     @QueryParam("exportGatewayRestManagementService") @DefaultValue("false") Boolean exportGatewayRestManagementService) throws IOException, ResourceFactory.ResourceNotFoundException, FindException, CannotRetrieveDependenciesException {
        rbacAccessService.validateFullAdministrator();
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
        return new ItemBuilder<>(transformer.convertToItem(createBundle(includeRequestFolder, Mapping.Action.valueOf(defaultAction), defaultMapBy, exportGatewayRestManagementService, header)))
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    /**
     * This will import the bundle.
     *
     * @param test   If true the bundle import will be tested no changes will be made to the gateway.,
     * @param bundle The bundle to import
     * @param activate False to not activate the updated services and policies.
     * @param versionComment The comment to set for updated/created services and policies
     * @return The mappings performed during the bundle import
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    public Response importBundle(@QueryParam("test") @DefaultValue("false") final boolean test,
                                 @QueryParam("activate") @DefaultValue("true") final boolean activate,
                                 @QueryParam("versionComment") final String versionComment,
                                 final Bundle bundle) throws Exception {
        rbacAccessService.validateFullAdministrator();

        AuditContextUtils.setSystem(true);
        List<Mapping> mappings = AuditContextFactory.doWithCustomAuditContext(AuditContextFactory.createLogOnlyAuditContext(), new Callable<List<Mapping>>() {
            @Override
            public List<Mapping> call() throws Exception {
                return bundleImporter.importBundle(bundle, test, activate, versionComment);
            }
        });
        AuditContextUtils.setSystem(false);

        Item<Mappings> item = new ItemBuilder<Mappings>("Bundle mappings", "BUNDLE MAPPINGS")
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
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
     * @param headers              The header to bundle a bundle for
     * @return The bundle from the headers
     * @throws FindException
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private Bundle createBundle(boolean includeRequestFolder, @NotNull final Mapping.Action defaultAction, @NotNull final String defaultMapBy, boolean exportGatewayRestManagementService, @NotNull final EntityHeader... headers) throws FindException, CannotRetrieveDependenciesException {
        //build the bundling properties
        final Properties bundleOptionsBuilder = new Properties();
        bundleOptionsBuilder.setProperty(BundleExporter.IncludeRequestFolderOption, String.valueOf(includeRequestFolder));
        bundleOptionsBuilder.setProperty(BundleExporter.DefaultMappingActionOption, defaultAction.toString());
        bundleOptionsBuilder.setProperty(BundleExporter.DefaultMapByOption, defaultMapBy);

        //ignore the rest man service so it is not exported
        if (containerRequest.getProperty("ServiceId") != null && !exportGatewayRestManagementService) {
            bundleOptionsBuilder.setProperty(BundleExporter.IgnoredEntityIdsOption, containerRequest.getProperty("ServiceId").toString());
        }
        //create the bundle export
        final Bundle bundle = bundleExporter.exportBundle(bundleOptionsBuilder, headers);
        //create a map of the items in the bundle so they are easy to reference and add the links to the items.
        final Map<String, Item> itemMap = Functions.toMap(bundle.getReferences(), new Functions.Unary<Pair<String, Item>, Item>() {
            @Override
            public Pair<String, Item> call(Item item) {
                URLAccessible urlAccessible = urlAccessibleLocator.findByEntityType(item.getType());
                List<Link> links = new ArrayList<>();
                links.add(urlAccessible.getLink(item.getContent()));
                links.addAll(urlAccessible.getRelatedLinks(item.getContent()));
                item.setLinks(links);
                return new Pair<>(item.getId(), item);
            }
        });
        //Add all the source uri's to the mappings
        for (Mapping mapping : bundle.getMappings()) {
            URLAccessible urlAccessible = urlAccessibleLocator.findByEntityType(mapping.getType());
            if (itemMap.containsKey(mapping.getSrcId())) {
                mapping.setSrcUri(urlAccessible.getUrl(itemMap.get(mapping.getSrcId()).getContent()));
            }
        }
        return bundle;
    }
}
