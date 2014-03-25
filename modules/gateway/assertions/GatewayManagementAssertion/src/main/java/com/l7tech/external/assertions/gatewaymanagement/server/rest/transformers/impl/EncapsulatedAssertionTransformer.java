package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EncapsulatedAssertionResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ZoneableGuidEntityHeader;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.server.bundling.PersistentEntityContainer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.UUID;

@Component
public class EncapsulatedAssertionTransformer extends APIResourceWsmanBaseTransformer<EncapsulatedAssertionMO, EncapsulatedAssertionConfig, ZoneableGuidEntityHeader,EncapsulatedAssertionResourceFactory> {

    @Override
    @Inject
    protected void setFactory(EncapsulatedAssertionResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<EncapsulatedAssertionMO> convertToItem(EncapsulatedAssertionMO m) {
        return new ItemBuilder<EncapsulatedAssertionMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @Override
    public PersistentEntityContainer<EncapsulatedAssertionConfig> convertFromMO(EncapsulatedAssertionMO m ,boolean strict) throws ResourceFactory.InvalidResourceException {

        PersistentEntityContainer<EncapsulatedAssertionConfig> container  =  super.convertFromMO(m, strict);

        if(container.getEntity()!=null){
            final Policy policy = container.getEntity().getPolicy();

            if (policy != null) {
                if (policy.getGoid() == Goid.DEFAULT_GOID) {
                    policy.setGoid(Goid.parseGoid(m.getPolicyReference().getId()));
                }

                if (policy.getGuid() == null) {
                    UUID guid = UUID.randomUUID();
                    policy.setGuid(guid.toString());
                }

            }
        }
        return container;
    }
}
