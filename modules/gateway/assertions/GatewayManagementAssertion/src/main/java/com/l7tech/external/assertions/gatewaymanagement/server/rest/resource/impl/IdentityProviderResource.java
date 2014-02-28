package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.IdentityProviderAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.IdentityProviderTransformer;
import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 * The identity provider resource
 *
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + IdentityProviderResource.jdbcConnections_URI)
@Singleton
public class IdentityProviderResource extends RestEntityResource<IdentityProviderMO, IdentityProviderAPIResourceFactory, IdentityProviderTransformer> {

    protected static final String jdbcConnections_URI = "identityProviders";

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory( IdentityProviderAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(IdentityProviderTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Shows the users
     *
     * @param id The provider id
     * @return The user resource for handling user requests.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     *
     */
    @Path("{id}/users")
    public UserResource users(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new UserResource(id));
    }

    /**
     * Shows the groups
     *
     * @param id The provider id
     * @return The group resource for handling group requests.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     *
     */
    @Path("{id}/groups")
    public GroupResource groups(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new GroupResource(id));
    }
}
