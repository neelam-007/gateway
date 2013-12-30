package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SecurityZoneRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.SecurityZoneMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The security zone resource
 */
@Provider
@Path(SecurityZoneResource.securityZone_URI)
public class SecurityZoneResource extends RestEntityResource<SecurityZoneMO, SecurityZoneRestResourceFactory> {

    protected static final String securityZone_URI = "securityZones";

    @Override
    @SpringBean
    public void setFactory(SecurityZoneRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.SECURITY_ZONE;
    }

    @Override
    protected Reference toReference(SecurityZoneMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
