package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.JDBCConnectionAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.JDBCConnectionTransformer;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.rest.SpringBean;
import org.jetbrains.annotations.NotNull;

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
public class JDBCConnectionResource extends RestEntityResource<JDBCConnectionMO, JDBCConnectionAPIResourceFactory, JDBCConnectionTransformer> {

    protected static final String jdbcConnections_URI = "jdbcConnections";

    @Override
    @SpringBean
    public void setFactory( JDBCConnectionAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(JDBCConnectionTransformer transformer) {
        super.transformer = transformer;
    }

    @NotNull
    @Override
    public String getUrl(@NotNull JDBCConnectionMO jdbcConnectionMO) {
        return getUrlString(jdbcConnectionMO.getId());
    }
}
