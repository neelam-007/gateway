package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This was created: 11/18/13 as 11:58 AM
 *
 * @author Victor Kazakov
 */
@Component
public class JDBCConnectionAPIResourceFactory extends WsmanBaseResourceFactory<JDBCConnectionMO, JDBCConnectionResourceFactory> {

    public JDBCConnectionAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.JDBC_CONNECTION.toString();
    }

    @Override
    @Inject
    @Named("jdbcConnectionResourceFactory")
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory factory) {
        super.factory = factory;
    }
}
