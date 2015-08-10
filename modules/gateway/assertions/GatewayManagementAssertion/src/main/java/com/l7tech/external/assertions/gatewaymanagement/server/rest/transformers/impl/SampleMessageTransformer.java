package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SampleMessageMO;
import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SampleMessageTransformer extends EntityManagerAPITransformer<SampleMessageMO, SampleMessage> implements EntityAPITransformer<SampleMessageMO, SampleMessage> {

    @Inject
    ServiceManager serviceManager;

    @Inject
    SecurityZoneManager securityZoneManager;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.SAMPLE_MESSAGE.toString();
    }

    @NotNull
    @Override
    public SampleMessageMO convertToMO(@NotNull EntityContainer<SampleMessage> userEntityContainer,  SecretsEncryptor secretsEncryptor) {
        return convertToMO(userEntityContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    public SampleMessageMO convertToMO(@NotNull SampleMessage sampleMessage) {
        return convertToMO(sampleMessage,null);
    }

    @NotNull
    @Override
    public SampleMessageMO convertToMO(@NotNull SampleMessage sampleMessage,  SecretsEncryptor secretsEncryptor) {
        SampleMessageMO sampleMessageMO = ManagedObjectFactory.createSampleMessageMO();
        sampleMessageMO.setId(sampleMessage.getId());
        sampleMessageMO.setVersion(sampleMessage.getVersion());
        sampleMessageMO.setName(sampleMessage.getName());
        sampleMessageMO.setXml(sampleMessage.getXml());
        sampleMessageMO.setOperation(sampleMessage.getOperationName());
        sampleMessageMO.setServiceId(sampleMessage.getServiceGoid()==null? null : sampleMessage.getServiceGoid().toString());
        doSecurityZoneToMO(sampleMessageMO,sampleMessage);

        return sampleMessageMO;
    }

    @NotNull
    @Override
    public EntityContainer<SampleMessage> convertFromMO(@NotNull SampleMessageMO sampleMessageMO, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(sampleMessageMO,true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<SampleMessage> convertFromMO(@NotNull SampleMessageMO sampleMessageMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        SampleMessage sampleMessage = new SampleMessage();
        sampleMessage.setId(sampleMessageMO.getId());
        if(sampleMessageMO.getVersion()!=null) {
            sampleMessage.setVersion(sampleMessageMO.getVersion());
        }
        sampleMessage.setName(sampleMessageMO.getName());
        sampleMessage.setXml(sampleMessageMO.getXml());
        sampleMessage.setOperationName(sampleMessageMO.getOperation());

        if(sampleMessageMO.getServiceId()!=null) {
            Goid serviceGoid = Goid.parseGoid(sampleMessageMO.getServiceId());
            try {
                serviceManager.findByPrimaryKey(serviceGoid);
            } catch (FindException e) {
                if (strict) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Cannot find published service with id: " + e.getMessage());
                }
            }
            sampleMessage.setServiceGoid(serviceGoid);
        }

        doSecurityZoneFromMO(sampleMessageMO,sampleMessage,strict);

        return new EntityContainer<SampleMessage>(sampleMessage);
    }

    @NotNull
    @Override
    public Item<SampleMessageMO> convertToItem(@NotNull SampleMessageMO m) {
        return new ItemBuilder<SampleMessageMO>(m.getName(), m.getId(), EntityType.SAMPLE_MESSAGE.name())
                .setContent(m)
                .build();
    }
}
