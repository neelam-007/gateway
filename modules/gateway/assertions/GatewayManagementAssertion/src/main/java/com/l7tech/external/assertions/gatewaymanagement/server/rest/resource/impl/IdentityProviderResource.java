package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.IdentityProviderRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The identity provider resource
 *
 */
@Provider
@Path(IdentityProviderResource.jdbcConnections_URI)
public class IdentityProviderResource extends RestEntityResource<IdentityProviderMO, IdentityProviderRestResourceFactory> {

    protected static final String jdbcConnections_URI = "identityProviders";

    @Override
    @SpringBean
    public void setFactory( IdentityProviderRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.ID_PROVIDER_CONFIG;
    }

    @Override
    protected Reference toReference(IdentityProviderMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
