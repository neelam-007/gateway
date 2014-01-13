package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SecurePasswordRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The secure password resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SecurePasswordResource.securePassword_URI)
@Singleton
public class SecurePasswordResource extends RestEntityResource<StoredPasswordMO, SecurePasswordRestResourceFactory> {

    protected static final String securePassword_URI = "passwords";

    @Override
    @SpringBean
    public void setFactory(SecurePasswordRestResourceFactory factory) {
        super.factory = factory;
    }


    @Override
    protected Item<StoredPasswordMO> toReference(StoredPasswordMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
