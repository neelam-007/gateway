package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ClusterPropertyRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The folder resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(ClusterPropertyResource.CLUSTER_PROPERTIES_URI)
public class ClusterPropertyResource extends DependentRestEntityResource<ClusterPropertyMO, ClusterPropertyRestResourceFactory> {

    protected static final String CLUSTER_PROPERTIES_URI = "clusterProperties";

    @Override
    @SpringBean
    public void setFactory( ClusterPropertyRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.CLUSTER_PROPERTY;
    }

    @Override
    protected Reference toReference(ClusterPropertyMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
