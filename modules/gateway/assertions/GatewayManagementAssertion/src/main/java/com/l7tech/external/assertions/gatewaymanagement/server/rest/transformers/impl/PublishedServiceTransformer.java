package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.common.service.PublishedService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PublishedServiceTransformer extends APIResourceWsmanBaseTransformer<ServiceMO, PublishedService, ServiceResourceFactory> {

    @Override
    @Inject
    protected void setFactory(ServiceResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<ServiceMO> convertToItem(ServiceMO m) {
        return new ItemBuilder<ServiceMO>(m.getServiceDetail().getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
