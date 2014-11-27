package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.DependencyResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.List;

/**
 * The base resource for resources that can have dependencies.
 *
 * @author Victor Kazakov
 */
public abstract class DependentRestEntityResource<R extends ManagedObject, F extends APIResourceFactory<R>, T extends APITransformer<R, ?>> extends RestEntityResource<R, F, T> {
    private static final String DEPENDENCIES_URI = "dependencies";
    @Context
    private ResourceContext resourceContext;

    /**
     * Returns the dependencies resource for the entity.
     *
     * @param id The id of the dependent entity
     * @return The dependency resource that will resolve dependencies
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @Path("{id}/" + DEPENDENCIES_URI)
    public DependencyResource dependencies(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(id);
        EntityHeader serviceHeader = new EntityHeader(resource.getId(), EntityType.valueOf(getResourceType()), null, null);
        return resourceContext.initResource(new DependencyResource(serviceHeader));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final R resource) {
        List<Link> links = super.getRelatedLinks(resource);
        if (resource != null) {
            links.add(ManagedObjectFactory.createLink("dependencies", getUrlString(resource.getId() + "/" + DEPENDENCIES_URI)));
        }
        return links;
    }
}
