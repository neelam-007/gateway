package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.util.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * This was created: 11/18/13 as 11:58 AM
 *
 * @author Victor Kazakov
 */
@Component
public class JDBCConnectionRestResourceFactory extends WsmanBaseResourceFactory<JDBCConnectionMO, com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory> {

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
