package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.service.ServiceManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ServiceAliasTransformer extends APIResourceWsmanBaseTransformer<ServiceAliasMO, PublishedServiceAlias, AliasHeader<PublishedService>, ServiceAliasResourceFactory> {

    @Inject
    private ServiceManager serviceManager;

    @Override
    @Inject
    protected void setFactory(ServiceAliasResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<ServiceAliasMO> convertToItem(ServiceAliasMO m) {
        Item<ServiceAliasMO> item = new ItemBuilder<ServiceAliasMO>(m.getServiceReference().getId(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
        try {
            PublishedService service = serviceManager.findByPrimaryKey(Goid.parseGoid(m.getServiceReference().getId()));
            if(service != null) {
                item.setName(service.getName() + " alias");
            }
        } catch (Throwable t) {
            //do nothing.
        }
        return item;
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
