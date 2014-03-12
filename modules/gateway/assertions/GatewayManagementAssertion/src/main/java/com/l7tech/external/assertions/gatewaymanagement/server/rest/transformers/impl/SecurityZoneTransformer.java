package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.SecurityZoneMO;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SecurityZone;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SecurityZoneTransformer extends APIResourceWsmanBaseTransformer<SecurityZoneMO, SecurityZone, EntityHeader, SecurityZoneResourceFactory> {

    @Override
    @Inject
    protected void setFactory(SecurityZoneResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<SecurityZoneMO> convertToItem(SecurityZoneMO m) {
        return new ItemBuilder<SecurityZoneMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
