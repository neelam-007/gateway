package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RestResourceLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vkazakov on 12/9/13.
 */
@Provider
@Path("bundle")
public class BundleResource {

    @SpringBean
    private DependencyAnalyzer dependencyAnalyzer;

    @SpringBean
    private RestResourceLocator restResourceLocator;

    private final EntityHeader entityHeader;

    public BundleResource() {
        //TODO: need a way to specify all gateway dependencies.
        entityHeader = null;
    }

    public BundleResource(EntityHeader entityHeader) {
        this.entityHeader = entityHeader;
    }

    @GET
    public Response exportBundle() throws FindException, ResourceFactory.ResourceNotFoundException, IOException {
        DependencySearchResults dependencies = dependencyAnalyzer.getDependencies(entityHeader);

        ArrayList<Resource> resources = new ArrayList<>();
        buildResourceList(resources, dependencies.getDependent(), dependencies.getDependencies());
        Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setManagedObjects(resources);
        return Response.ok(bundle).build();
    }

    private void buildResourceList(ArrayList<Resource> resources, DependentObject dependent, List<Dependency> dependencies) throws ResourceFactory.ResourceNotFoundException, IOException {
        for (Dependency dependency : dependencies) {
            buildResourceList(resources, dependency.getDependent(), dependency.getDependencies());
        }
        if (dependent instanceof DependentEntity) {
            resources.add(getManagedObject((DependentEntity) dependent));
        }
    }

    private Resource getManagedObject(DependentEntity dependent) throws ResourceFactory.ResourceNotFoundException, IOException {
        RestEntityResource resource = restResourceLocator.findByEntityType(dependent.getDependencyType().getEntityType());
        Response entity = resource.getResource(dependent.getEntityHeader().getStrId());
        Resource resourceMO = ManagedObjectFactory.createResource();
        resourceMO.setId(dependent.getEntityHeader().getStrId());
        resourceMO.setType(dependent.getEntityHeader().getType().name());
        resourceMO.setContent(XmlUtil.nodeToString(ManagedObjectFactory.write((ManagedObject) entity.getEntity())));
        return resourceMO;
    }

    @GET
    @Path("mappings")
    public Response getMappings() {
        return Response.ok().build();
    }

    @POST
    public Response importBundle() {
        return Response.ok().build();
    }
}
