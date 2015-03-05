package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SiteMinderConfigurationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class SiteMinderConfigurationTransformer extends APIResourceWsmanBaseTransformer<SiteMinderConfigurationMO, SiteMinderConfiguration,EntityHeader, SiteMinderConfigurationResourceFactory> {

    @Override
    @Inject
    @Named("siteMinderConfigurationResourceFactory")
    protected void setFactory(SiteMinderConfigurationResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<SiteMinderConfigurationMO> convertToItem(@NotNull SiteMinderConfigurationMO m) {
        return new ItemBuilder<SiteMinderConfigurationMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
