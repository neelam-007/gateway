package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SiteMinderConfigurationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class SiteMinderConfigurationAPIResourceFactory extends WsmanBaseResourceFactory<SiteMinderConfigurationMO, SiteMinderConfigurationResourceFactory> {

    public SiteMinderConfigurationAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("enabled", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("enabled", RestResourceFactoryUtils.booleanConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public EntityType getEntityType(){
        return EntityType.SITEMINDER_CONFIGURATION;
    }

    @Override
    @Inject
    public void setFactory(SiteMinderConfigurationResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public SiteMinderConfigurationMO getResourceTemplate() {
        SiteMinderConfigurationMO siteMinderConfiguration = ManagedObjectFactory.createSiteMinderConfiguration();
        siteMinderConfiguration.setName("TemplateSiteMinderConfiguration");
        siteMinderConfiguration.setAddress("SFTP");
        siteMinderConfiguration.setEnabled(true);
        siteMinderConfiguration.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("ConnectorProperty", "PropertyValue").map());
        return siteMinderConfiguration;
    }
}
