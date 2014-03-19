package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleExporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleImporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
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

/**
 * This resource is used to export and import bundles for migration. Do not make this a @Provider the will make allow
 * @queryParam on fields. This will be added to the rest application using the application context. See
 * /com/l7tech/external/assertions/gatewaymanagement/server/gatewayManagementContext.xml:restAgent
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

    @Context
    private UriInfo uriInfo;

    @Context
    private ContainerRequest containerRequest;

    @QueryParam("defaultAction")
    @DefaultValue("NewOrExisting")
    private Mapping.Action defaultAction;
    @QueryParam("defaultMapBy")
    @DefaultValue("id")
    private String defaultMapBy;
    @QueryParam("includeRequestFolder")
    @DefaultValue("false")
    private boolean includeRequestFolder;
    @QueryParam("exportGatewayRestManagementService")
    @DefaultValue("false")
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
        return new ItemBuilder<>(transformer.convertToItem(createBundle(includeRequestFolder, defaultAction, defaultMapBy, header)))
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
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
    public Response importBundle(@QueryParam("test") @DefaultValue("false") boolean test, Bundle bundle) throws ResourceFactory.InvalidResourceException {
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

    /**
     * Creates a bundle from the entity headers given
     * @param includeRequestFolder true to include the request folder
     * @param defaultAction The default mapping action to take
     * @param defaultMapBy The default map by property
     * @param headers The header to bundle a bundle for
     * @return The bundle from the headers
     * @throws FindException
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private Bundle createBundle(boolean includeRequestFolder, @NotNull final Mapping.Action defaultAction, @NotNull final String defaultMapBy, @NotNull final EntityHeader... headers) throws FindException {
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
        for(Mapping mapping : bundle.getMappings()){
            URLAccessible urlAccessible = urlAccessibleLocator.findByEntityType(mapping.getType());
            if(itemMap.containsKey(mapping.getSrcId())){
                mapping.setSrcUri(urlAccessible.getUrl(itemMap.get(mapping.getSrcId()).getContent()));
            }
        }
        return bundle;
    }
}
