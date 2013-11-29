package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.HttpConfigurationRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The Http Configuration resource
 */
@Provider
@Path(HttpConfigurationResource.httpConfiguration_URI)
public class HttpConfigurationResource extends RestEntityResource<HttpConfigurationMO, HttpConfigurationRestResourceFactory> {

    protected static final String httpConfiguration_URI = "httpConfigurations";

    @Override
    @SpringBean
    public void setFactory(HttpConfigurationRestResourceFactory factory) {
        super.factory = factory;
    }
}
