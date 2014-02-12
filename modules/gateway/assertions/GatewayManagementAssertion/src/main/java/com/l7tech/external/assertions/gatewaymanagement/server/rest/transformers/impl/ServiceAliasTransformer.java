package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ServiceAliasTransformer extends APIResourceWsmanBaseTransformer<ServiceAliasMO, PublishedServiceAlias, ServiceAliasResourceFactory> {

    @Override
    @Inject
    protected void setFactory(ServiceAliasResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<ServiceAliasMO> convertToItem(ServiceAliasMO m) {
        return new ItemBuilder<ServiceAliasMO>(m.getServiceReference().getId(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @Override
    public Item<ServiceAliasMO> convertToItem(EntityHeader header) {
        if (header instanceof AliasHeader) {
            return new ItemBuilder<ServiceAliasMO>(((AliasHeader) header).getAliasedEntityId().toString(), header.getStrId(), factory.getType().name())
                    .build();
        } else {
            return super.convertToItem(header);
        }
    }
}
