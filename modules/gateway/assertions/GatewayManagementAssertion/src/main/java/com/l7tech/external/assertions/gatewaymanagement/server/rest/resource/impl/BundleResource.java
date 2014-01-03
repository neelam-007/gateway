package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RestResourceLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This resource is used to export and import bundles for migration.
 * Do not make this a @Provider the will make allow @queryParam on fields. This will be added to the rest application using the application context. See /com/l7tech/external/assertions/gatewaymanagement/server/gatewayManagementContext.xml:restAgent
 */
@Path("bundle")
@RequestScoped
public class BundleResource {

    @SpringBean
    private DependencyAnalyzer dependencyAnalyzer;

    @SpringBean
    private RestResourceLocator restResourceLocator;

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

    public BundleResource() {
    }

    @GET
    public Reference exportBundle() throws FindException, ResourceFactory.ResourceNotFoundException, IOException {
        //TODO: need a way to export the entire gateway as a bundle
        return ManagedObjectFactory.<Bundle>createReference();
    }

    @GET
    @Path("{resourceType}/{id}")
    public Reference<Bundle> exportBundle(@PathParam("resourceType") String resourceType, @PathParam("id") String id) throws IOException, ResourceFactory.ResourceNotFoundException, FindException {
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
        Reference<Bundle> reference = ManagedObjectFactory.createReference();
        reference.setResource(createBundle(includeRequestFolder, defaultAction, defaultMapBy, header));
        reference.setType("BUNDLE");
        reference.setTitle("Bundle for " + resourceType + " " + id);
        reference.setLinks(CollectionUtils.<Link>list(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString())));
        return reference;
    }

    @POST
    public Reference<Bundle> exportBundle(References references) throws IOException, ResourceFactory.ResourceNotFoundException, FindException {
        List<EntityHeader> headers = new ArrayList<>(references.getReferences().size());
        for (Reference reference : references.getReferences()) {
            headers.add(new EntityHeader(reference.getId(), EntityType.valueOf(reference.getType()), null, null));
        }
        Reference<Bundle> reference = ManagedObjectFactory.createReference();
        reference.setResource(createBundle(includeRequestFolder, defaultAction, defaultMapBy, headers.toArray(new EntityHeader[headers.size()])));
        return reference;
    }

    @POST
    public Response importBundle() {
        return Response.ok().build();
    }

    private Bundle createBundle(boolean includeRequestFolder, Mapping.Action defaultAction, String defaultMapBy, EntityHeader... headers) throws ResourceFactory.ResourceNotFoundException, IOException, FindException {
        List<DependencySearchResults> dependencySearchResults = dependencyAnalyzer.getDependencies(Arrays.asList(headers));
        List<DependentObject> dependentObjects = dependencyAnalyzer.buildFlatDependencyList(dependencySearchResults);

        ArrayList<Reference> references = new ArrayList<>();
        ArrayList<Mapping> mappings = new ArrayList<>();
        for (final DependentObject dependentObject : dependentObjects) {
            if (dependentObject instanceof DependentEntity) {
                if (!includeRequestFolder && EntityType.FOLDER.equals(((DependentEntity) dependentObject).getEntityHeader().getType()) && Functions.exists(Arrays.asList(headers), new Functions.Unary<Boolean, EntityHeader>() {
                    @Override
                    public Boolean call(EntityHeader entityHeader) {
                        return Goid.equals(((DependentEntity) dependentObject).getEntityHeader().getGoid(), entityHeader.getGoid());
                    }
                })) {
                    continue;
                }
                RestEntityResource restResource = restResourceLocator.findByEntityType(dependentObject.getDependencyType().getEntityType());
                Reference resource = restResource.getResource(((DependentEntity) dependentObject).getEntityHeader().getStrId());
                references.add(resource);
                mappings.add(restResource.getFactory().buildMapping(resource.getResource(), defaultAction, defaultMapBy, buildProperties()));
            }
        }

        Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setReferences(ManagedObjectFactory.createReferences(references));
        bundle.setMappings(mappings);
        return bundle;
    }

    private Map<String, Object> buildProperties() {
        return Functions.toMap(containerRequest.getPropertyNames(), new Functions.Unary<Pair<String, Object>, String>() {
            @Override
            public Pair<String, Object> call(String s) {
                return new Pair<>(s, containerRequest.getProperty(s));
            }
        });
    }
}
