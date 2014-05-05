package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.Policy;
import com.l7tech.server.bundling.PersistentEntityContainer;
import com.l7tech.server.bundling.PublishedServiceContainer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.UUID;

@Component
public class PublishedServiceTransformer extends APIResourceWsmanBaseTransformer<ServiceMO, PublishedService,ServiceHeader, ServiceResourceFactory> {

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

    @Override
    public PersistentEntityContainer<PublishedService> convertFromMO(ServiceMO serviceMO,boolean strict) throws ResourceFactory.InvalidResourceException {
        Iterator<PersistentEntity> entities =  factory.fromResourceAsBag(serviceMO,strict).iterator();
        PublishedServiceContainer container = new PublishedServiceContainer((PublishedService)entities.next(),entities);

        if(container.getEntity()!=null && serviceMO.getServiceDetail().getId()!=null){
            container.getEntity().setGoid(Goid.parseGoid(serviceMO.getServiceDetail().getId()));
        }

        if(container.getEntity()!=null){
            final Policy policy = container.getEntity().getPolicy();
            container.getEntity().setInternal(false);

            if (policy != null) {
                if (policy.getGuid() == null) {
                    UUID guid = UUID.randomUUID();
                    policy.setGuid(guid.toString());
                }

                if (policy.getName() == null) {
                    policy.setName( container.getEntity().generatePolicyName());
                }
            }
            //need to update the service version as wsman does not set it. SSG-8476
            if(serviceMO.getVersion() != null) {
                container.getEntity().setVersion(serviceMO.getVersion());
            }
        }
        return container;
    }
}
