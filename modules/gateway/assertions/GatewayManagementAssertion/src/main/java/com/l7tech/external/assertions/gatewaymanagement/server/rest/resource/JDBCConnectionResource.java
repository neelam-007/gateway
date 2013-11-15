package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The jdbc connection resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(JDBCConnectionResource.jdbcConnections_URI)
public class JDBCConnectionResource extends RestWsmanEntityResource<JDBCConnectionMO, JDBCConnectionResourceFactory> {

    protected static final String jdbcConnections_URI = "jdbcConnections";

    @Override
    @SpringBean
    public void setFactory( JDBCConnectionResourceFactory factory) {
        super.factory = factory;
    }
}
