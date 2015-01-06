package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EntityManagerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.bundling.PublishedServiceContainer;
import com.l7tech.util.MasterPasswordManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.UUID;

@Component
public class PublishedServiceTransformer extends APIResourceWsmanBaseTransformer<ServiceMO, PublishedService,ServiceHeader, ServiceResourceFactory> {

    @Override
    @Inject
    protected void setFactory(ServiceResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<ServiceMO> convertToItem(@NotNull ServiceMO m) {
        return new ItemBuilder<ServiceMO>(m.getServiceDetail().getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<PublishedService> convertFromMO(@NotNull ServiceMO serviceMO, boolean strict, MasterPasswordManager passwordManager) throws ResourceFactory.InvalidResourceException {
        final EntityManagerResourceFactory.EntityBag<PublishedService> entityBag =  factory.fromResourceAsBag(serviceMO, strict);
        if(!(entityBag instanceof ServiceResourceFactory.ServiceEntityBag)) {
            throw new IllegalStateException("Expected a ServiceEntityBag but got: " + entityBag.getClass() + ". This should not have happened!");
        }
        PublishedService service = entityBag.getEntity();

        if(serviceMO.getServiceDetail().getId()!=null){
            service.setGoid(Goid.parseGoid(serviceMO.getServiceDetail().getId()));
        }

        final Policy policy = service.getPolicy();
        service.setInternal(false);

        if (policy != null) {
            if (policy.getGuid() == null) {
                UUID guid = UUID.randomUUID();
                policy.setGuid(guid.toString());
            }

            if (policy.getName() == null) {
                policy.setName( service.generatePolicyName());
            }
        }
        return new PublishedServiceContainer(service,((ServiceResourceFactory.ServiceEntityBag)entityBag).getServiceDocuments());
    }
}
