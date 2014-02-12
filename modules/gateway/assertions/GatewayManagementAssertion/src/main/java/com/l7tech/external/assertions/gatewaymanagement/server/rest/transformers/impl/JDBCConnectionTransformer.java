package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class JDBCConnectionTransformer extends APIResourceWsmanBaseTransformer<JDBCConnectionMO, JdbcConnection, JDBCConnectionResourceFactory> {

    @Override
    @Inject
    protected void setFactory(JDBCConnectionResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<JDBCConnectionMO> convertToItem(JDBCConnectionMO m) {
        return new ItemBuilder<JDBCConnectionMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
