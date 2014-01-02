package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.TemplateFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.DependencyResource;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.objectmodel.EntityHeader;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * The base resource for resources that can have dependencies.
 *
 * @author Victor Kazakov
 */
public abstract class DependentRestEntityResource<R extends ManagedObject, F extends RestResourceFactory<R> & TemplateFactory<R>> extends RestEntityResource<R, F> {
    @Context
    private ResourceContext resourceContext;

    /**
     * Returns the dependencies resource for the entity.
     *
     * @param id The id of the dependent entity
     * @return The dependency resource that will resolve dependencies
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @Path("{id}/dependencies")
    public DependencyResource dependencies(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(id);
        EntityHeader serviceHeader = new EntityHeader(resource.getId(), getEntityType(), null, null);
        return resourceContext.initResource(new DependencyResource(serviceHeader));
    }
}
