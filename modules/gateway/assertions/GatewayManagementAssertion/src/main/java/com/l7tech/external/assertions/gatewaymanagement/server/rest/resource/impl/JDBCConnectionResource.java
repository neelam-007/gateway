package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.JDBCConnectionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The jdbc connection resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + JDBCConnectionResource.jdbcConnections_URI)
@Singleton
public class JDBCConnectionResource extends RestEntityResource<JDBCConnectionMO, JDBCConnectionRestResourceFactory> {

    protected static final String jdbcConnections_URI = "jdbcConnections";

    @Override
    @SpringBean
    public void setFactory( JDBCConnectionRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Item<JDBCConnectionMO> toReference(JDBCConnectionMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
