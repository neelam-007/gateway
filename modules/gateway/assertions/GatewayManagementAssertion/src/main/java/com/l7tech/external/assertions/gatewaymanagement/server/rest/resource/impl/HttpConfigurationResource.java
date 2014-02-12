package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.HttpConfigurationAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.HttpConfigurationTransformer;
import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.rest.SpringBean;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The Http Configuration resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + HttpConfigurationResource.httpConfiguration_URI)
@Singleton
public class HttpConfigurationResource extends RestEntityResource<HttpConfigurationMO, HttpConfigurationAPIResourceFactory, HttpConfigurationTransformer> {

    protected static final String httpConfiguration_URI = "httpConfigurations";

    @Override
    @SpringBean
    public void setFactory(HttpConfigurationAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(HttpConfigurationTransformer transformer) {
        super.transformer = transformer;
    }

    @NotNull
    @Override
    public String getUrl(@NotNull HttpConfigurationMO httpConfigurationMO) {
        return getUrlString(httpConfigurationMO.getId());
    }
}
