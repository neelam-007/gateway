package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ListenPortRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The listen port resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ListenPortResource.listenPort_URI)
@Singleton
public class ListenPortResource extends RestEntityResource<ListenPortMO, ListenPortRestResourceFactory> {

    protected static final String listenPort_URI = "listenPorts";

    @Override
    @SpringBean
    public void setFactory(ListenPortRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Reference<ListenPortMO> toReference(ListenPortMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
