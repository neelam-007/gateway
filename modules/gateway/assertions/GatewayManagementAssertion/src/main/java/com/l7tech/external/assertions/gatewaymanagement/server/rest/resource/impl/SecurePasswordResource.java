package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SecurePasswordAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SecurePasswordTransformer;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.rest.SpringBean;
import org.jetbrains.annotations.NotNull;

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
public class SecurePasswordResource extends RestEntityResource<StoredPasswordMO, SecurePasswordAPIResourceFactory, SecurePasswordTransformer> {

    protected static final String securePassword_URI = "passwords";

    @Override
    @SpringBean
    public void setFactory(SecurePasswordAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(SecurePasswordTransformer transformer) {
        super.transformer = transformer;
    }

    @NotNull
    @Override
    public String getUrl(@NotNull StoredPasswordMO storedPasswordMO) {
        return getUrlString(storedPasswordMO.getId());
    }
}
