package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SiteMinderConfigurationAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SiteMinderConfigurationTransformer;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.rest.SpringBean;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The active connector resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SiteMinderConfigurationResource.siteMinderConfigurations_URI)
@Singleton
public class SiteMinderConfigurationResource extends RestEntityResource<SiteMinderConfigurationMO, SiteMinderConfigurationAPIResourceFactory, SiteMinderConfigurationTransformer> {

    protected static final String siteMinderConfigurations_URI = "siteMinderConfigurations";

    @Override
    @SpringBean
    public void setFactory(SiteMinderConfigurationAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(SiteMinderConfigurationTransformer transformer) {
        super.transformer = transformer;
    }

    @NotNull
    @Override
    public String getUrl(@NotNull SiteMinderConfigurationMO siteMinderConfigurationMO) {
        return getUrlString(siteMinderConfigurationMO.getId());
    }
}
