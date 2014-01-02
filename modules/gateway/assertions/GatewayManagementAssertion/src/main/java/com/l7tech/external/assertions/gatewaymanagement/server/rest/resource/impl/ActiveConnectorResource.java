package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ActiveConnectorRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.ActiveConnectorMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The active connector resource
 */
@Provider
@Path(ActiveConnectorResource.activeConnectors_URI)
@Singleton
public class ActiveConnectorResource extends RestEntityResource<ActiveConnectorMO, ActiveConnectorRestResourceFactory> {

    protected static final String activeConnectors_URI = "activeConnectors";

    @Override
    @SpringBean
    public void setFactory(ActiveConnectorRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Reference<ActiveConnectorMO> toReference(ActiveConnectorMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
