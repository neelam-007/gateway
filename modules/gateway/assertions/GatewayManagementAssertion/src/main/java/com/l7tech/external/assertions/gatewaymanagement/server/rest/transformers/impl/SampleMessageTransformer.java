package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.objectmodel.*;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SampleMessageTransformer implements EntityAPITransformer<SampleMessageMO, SampleMessage> {

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
    public SampleMessageMO convertToMO(@NotNull EntityContainer<SampleMessage> userEntityContainer) {
        return convertToMO(userEntityContainer.getEntity());
    }

    @NotNull
    @Override
    public SampleMessageMO convertToMO(@NotNull SampleMessage sampleMessage) {
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
    public EntityContainer<SampleMessage> convertFromMO(@NotNull SampleMessageMO sampleMessageMO) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(sampleMessageMO,true);
    }

    @NotNull
    @Override
    public EntityContainer<SampleMessage> convertFromMO(@NotNull SampleMessageMO sampleMessageMO, boolean strict) throws ResourceFactory.InvalidResourceException {
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

    protected void doSecurityZoneToMO(SampleMessageMO resource, final SampleMessage entity) {
        if (entity instanceof ZoneableEntity) {
            ZoneableEntity zoneable = (ZoneableEntity) entity;

            if (zoneable.getSecurityZone() != null) {
                resource.setSecurityZoneId( zoneable.getSecurityZone().getId() );
                resource.setSecurityZone( zoneable.getSecurityZone().getName() );
            }
        }
    }

    protected void doSecurityZoneFromMO(SampleMessageMO resource, final SampleMessage entity, final boolean strict) throws ResourceFactory.InvalidResourceException {
        if (entity instanceof ZoneableEntity) {

            if (resource.getSecurityZoneId() != null && !resource.getSecurityZoneId().isEmpty()) {
                final Goid securityZoneId;
                try {
                    securityZoneId = GoidUpgradeMapper.mapId(EntityType.SECURITY_ZONE, resource.getSecurityZoneId());
                } catch (IllegalArgumentException nfe) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
                }
                SecurityZone zone = null;
                try {
                    zone = securityZoneManager.findByPrimaryKey(securityZoneId);
                } catch (FindException e) {
                    if(strict)
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
                }
                if (strict) {
                    if (zone == null)
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown security zone reference");
                    if (!zone.permitsEntityType(EntityType.findTypeByEntity(entity.getClass())))
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "entity type not permitted for referenced security zone");
                } else if (zone == null) {
                    zone = new SecurityZone();
                    zone.setGoid(securityZoneId);
                    zone.setName(resource.getSecurityZone());
                }
                ((ZoneableEntity) entity).setSecurityZone(zone);
            }
        }
    }

    @NotNull
    @Override
    public Item<SampleMessageMO> convertToItem(@NotNull SampleMessageMO m) {
        return new ItemBuilder<SampleMessageMO>(m.getName(), m.getId(), EntityType.SAMPLE_MESSAGE.name())
                .setContent(m)
                .build();
    }
}
