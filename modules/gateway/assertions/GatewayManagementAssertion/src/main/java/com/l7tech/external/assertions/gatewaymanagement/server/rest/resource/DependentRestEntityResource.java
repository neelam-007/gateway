package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * The base resource for resources that can have dependencies.
 *
 * @author Victor Kazakov
 */
public abstract class DependentRestEntityResource<R extends ManagedObject, F extends ResourceFactory<R>> extends RestWsmanEntityResource<R,F> {
    @Context
    ResourceContext resourceContext;

    @Path("{id}/dependencies")
    public DependencyResource assignment(@PathParam("id") String id, @QueryParam("idType") @DefaultValue("id") RestWsmanEntityResource.IdType idType) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(buildSelectorMap(id, idType));
        EntityHeader serviceHeader = new EntityHeader(resource.getId(), getEntityType(), null, null);
        return resourceContext.initResource(new DependencyResource(serviceHeader));
    }

    protected abstract EntityType getEntityType();
}
