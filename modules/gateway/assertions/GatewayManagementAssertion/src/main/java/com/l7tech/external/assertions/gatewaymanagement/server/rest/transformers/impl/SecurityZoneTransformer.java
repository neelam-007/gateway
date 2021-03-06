package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.SecurityZoneMO;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SecurityZone;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class SecurityZoneTransformer extends APIResourceWsmanBaseTransformer<SecurityZoneMO, SecurityZone, EntityHeader, SecurityZoneResourceFactory> {

    @Override
    @Inject
    @Named("securityZoneResourceFactory")
    protected void setFactory(SecurityZoneResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<SecurityZoneMO> convertToItem(@NotNull SecurityZoneMO m) {
        return new ItemBuilder<SecurityZoneMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
