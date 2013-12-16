package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SecurePasswordRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The secure password resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(SecurePasswordResource.securePassword_URI)
public class SecurePasswordResource extends RestEntityResource<StoredPasswordMO, SecurePasswordRestResourceFactory> {

    protected static final String securePassword_URI = "passwords";

    @Override
    @SpringBean
    public void setFactory(SecurePasswordRestResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.SECURE_PASSWORD;
    }

    @Override
    protected Reference toReference(StoredPasswordMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
