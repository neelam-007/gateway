package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.SecurityZoneMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class SecurityZoneAPIResourceFactory extends WsmanBaseResourceFactory<SecurityZoneMO, SecurityZoneResourceFactory> {

    public SecurityZoneAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SECURITY_ZONE.toString();
    }

    @Override
    @Inject
    @Named("securityZoneResourceFactory")
    public void setFactory(SecurityZoneResourceFactory factory) {
        super.factory = factory;
    }
}
