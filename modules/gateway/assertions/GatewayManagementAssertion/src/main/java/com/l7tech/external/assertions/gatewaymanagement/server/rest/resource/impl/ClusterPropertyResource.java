package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ClusterPropertyRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The folder resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ClusterPropertyResource.CLUSTER_PROPERTIES_URI)
@Singleton
public class ClusterPropertyResource extends DependentRestEntityResource<ClusterPropertyMO, ClusterPropertyRestResourceFactory> {

    protected static final String CLUSTER_PROPERTIES_URI = "clusterProperties";

    @Override
    @SpringBean
    public void setFactory( ClusterPropertyRestResourceFactory factory) {
        super.factory = factory;
    }


    @Override
    protected Item<ClusterPropertyMO> toReference(ClusterPropertyMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
