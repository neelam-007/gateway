package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.APIUtilityLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleExporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleImporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.*;

/**
 * This resource is used to export and import bundles for migration.
 * Do not make this a @Provider the will make allow @queryParam on fields. This will be added to the rest application using the application context. See /com/l7tech/external/assertions/gatewaymanagement/server/gatewayManagementContext.xml:restAgent
 */
@Path(BundleResource.Version_URI + "bundle")
@RequestScoped
public class BundleResource {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

    @SpringBean
    private DependencyAnalyzer dependencyAnalyzer;

    @SpringBean
    private APIUtilityLocator apiUtilityLocator;

    @SpringBean
    private URLAccessibleLocator URLAccessibleLocator;

    @SpringBean
    private BundleImporter bundleImporter;

    @SpringBean
    private BundleExporter bundleExporter;

    @Context
    private UriInfo uriInfo;

    @Context
    private ContainerRequest containerRequest;

    @QueryParam("defaultAction") @DefaultValue("NewOrExisting")
    private Mapping.Action defaultAction;
    @QueryParam("defaultMapBy") @DefaultValue("id")
    private String defaultMapBy;
    @QueryParam("includeRequestFolder") @DefaultValue("false")
    private boolean includeRequestFolder;
    @QueryParam("exportGatewayRestManagementService") @DefaultValue("false")
    private boolean exportGatewayRestManagementService;

    public BundleResource() {
    }

    @GET
    public Item exportBundle() throws FindException, ResourceFactory.ResourceNotFoundException, IOException {
        //TODO: need a way to export the entire gateway as a bundle
        return new ItemBuilder<Bundle>("Bundle", "BUNDLE").build();
    }

    @GET
    @Path("{resourceType}/{id}")
    public Item<Bundle> exportBundle(@PathParam("resourceType") String resourceType, @PathParam("id") String id) throws IOException, ResourceFactory.ResourceNotFoundException, FindException {
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

        EntityHeader header = new EntityHeader(Goid.parseGoid(id), entityType, null, null);
        return new ItemBuilder<Bundle>("Bundle for " + resourceType + " " + id, "BUNDLE")
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .setContent(createBundle(includeRequestFolder, defaultAction, defaultMapBy, header))
                .build();
    }

    @POST
    public Item<Bundle> exportBundle(List<Item> references) throws IOException, ResourceFactory.ResourceNotFoundException, FindException {
        List<EntityHeader> headers = new ArrayList<>(references.size());
        for (Item item : references) {
            headers.add(new EntityHeader(item.getId(), EntityType.valueOf(item.getType()), null, null));
        }
        return new ItemBuilder<Bundle>("Bundle", "BUNDLE")
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .setContent(createBundle(includeRequestFolder, defaultAction, defaultMapBy, headers.toArray(new EntityHeader[headers.size()])))
                .build();
    }

    @PUT
    public Response importBundle(@QueryParam("test") @DefaultValue("false") boolean test, Bundle bundle) {
        List<Mapping> mappings = bundleImporter.importBundle(bundle, test);
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

    @SuppressWarnings("unchecked")
    private Bundle createBundle(boolean includeRequestFolder, Mapping.Action defaultAction, String defaultMapBy, EntityHeader... headers) throws ResourceFactory.ResourceNotFoundException, IOException, FindException {
        CollectionUtils.MapBuilder<String, Object> bundleOptionsBuilder = CollectionUtils.MapBuilder.<String, Object>builder()
                .put(BundleExporter.IncludeRequestFolderOption, includeRequestFolder)
                .put(BundleExporter.DefaultMappingActionOption, defaultAction)
                .put(BundleExporter.DefaultMapByOption, defaultMapBy);
        if(containerRequest.getProperty("ServiceId") != null && !exportGatewayRestManagementService){
            bundleOptionsBuilder.put(BundleExporter.IgnoredEntityIdsOption, Arrays.asList(containerRequest.getProperty("ServiceId")));
        }
        return bundleExporter.exportBundle(bundleOptionsBuilder.map(), headers);
    }
}
