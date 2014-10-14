package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SampleMessageTransformer;
import com.l7tech.gateway.api.SampleMessageMO;
import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.service.SampleMessageManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
* The firewall rule rest resource factory.
*/
@Component
public class SampleMessageAPIResourceFactory extends EntityManagerAPIResourceFactory<SampleMessageMO, SampleMessage, EntityHeader> {

    @Inject
    private SampleMessageTransformer transformer;
    @Inject
    private SampleMessageManager sampleMessageManager;

    @NotNull
    @Override
    public EntityType getResourceEntityType() {
        return EntityType.SAMPLE_MESSAGE;
    }

    @Override
    protected SampleMessage convertFromMO(SampleMessageMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource).getEntity();
    }

    @Override
    protected SampleMessageMO convertToMO(SampleMessage entity) {
        return transformer.convertToMO(entity);
    }

    @Override
    protected SampleMessageManager getEntityManager() {
        return sampleMessageManager;
    }
}
