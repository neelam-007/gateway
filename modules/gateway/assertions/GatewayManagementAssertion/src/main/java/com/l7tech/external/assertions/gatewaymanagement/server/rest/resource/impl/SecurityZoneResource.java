package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SecurityZoneRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.SecurityZoneMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The security zone resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SecurityZoneResource.securityZone_URI)
@Singleton
public class SecurityZoneResource extends RestEntityResource<SecurityZoneMO, SecurityZoneRestResourceFactory> {

    protected static final String securityZone_URI = "securityZones";

    @Override
    @SpringBean
    public void setFactory(SecurityZoneRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Item<SecurityZoneMO> toReference(SecurityZoneMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
