package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SiteMinderConfigurationRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The active connector resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SiteMinderConfigurationResource.siteMinderConfigurations_URI)
@Singleton
public class SiteMinderConfigurationResource extends RestEntityResource<SiteMinderConfigurationMO, SiteMinderConfigurationRestResourceFactory> {

    protected static final String siteMinderConfigurations_URI = "siteMinderConfigurations";

    @Override
    @SpringBean
    public void setFactory(SiteMinderConfigurationRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Reference<SiteMinderConfigurationMO> toReference(SiteMinderConfigurationMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
