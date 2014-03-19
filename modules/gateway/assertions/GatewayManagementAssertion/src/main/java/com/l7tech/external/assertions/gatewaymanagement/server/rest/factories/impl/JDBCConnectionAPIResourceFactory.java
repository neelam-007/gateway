package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public JDBCConnectionMO getResourceTemplate() {
        JDBCConnectionMO jdbcConnectionMO = ManagedObjectFactory.createJDBCConnection();
        jdbcConnectionMO.setName("TemplateJDBCConnection");
        jdbcConnectionMO.setDriverClass("com.my.driver.class");
        jdbcConnectionMO.setEnabled(true);
        jdbcConnectionMO.setJdbcUrl("example.connection.url");
        jdbcConnectionMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("ConnectionProperty", "PropertyValue").map());
        return jdbcConnectionMO;
    }
}
