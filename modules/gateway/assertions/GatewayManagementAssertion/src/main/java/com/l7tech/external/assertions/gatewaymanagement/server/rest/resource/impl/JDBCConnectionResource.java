package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.JDBCConnectionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The jdbc connection resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(JDBCConnectionResource.jdbcConnections_URI)
public class JDBCConnectionResource extends RestEntityResource<JDBCConnectionMO, JDBCConnectionRestResourceFactory> {

    protected static final String jdbcConnections_URI = "jdbcConnections";

    @Override
    @SpringBean
    public void setFactory( JDBCConnectionRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType() {
        return EntityType.JDBC_CONNECTION;
    }

    @Override
    protected Reference toReference(JDBCConnectionMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
