package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.HttpConfigurationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class HttpConfigurationTransformer extends APIResourceWsmanBaseTransformer<HttpConfigurationMO, HttpConfiguration, EntityHeader, HttpConfigurationResourceFactory> {

    @Override
    @Inject
    @Named("httpConfigurationResourceFactory")
    protected void setFactory(HttpConfigurationResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<HttpConfigurationMO> convertToItem(@NotNull HttpConfigurationMO m) {
        return new ItemBuilder<HttpConfigurationMO>(m.getHost(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
