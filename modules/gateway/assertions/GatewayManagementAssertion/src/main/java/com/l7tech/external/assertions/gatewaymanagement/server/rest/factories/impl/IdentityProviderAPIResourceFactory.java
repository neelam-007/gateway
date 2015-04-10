package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.IdentityProviderResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class IdentityProviderAPIResourceFactory extends WsmanBaseResourceFactory<IdentityProviderMO, IdentityProviderResourceFactory> {

    public IdentityProviderAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.ID_PROVIDER_CONFIG.toString();
    }

    @Override
    @Inject
    @Named("identityProviderResourceFactory")
    public void setFactory(IdentityProviderResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Mapping buildMapping(@NotNull IdentityProviderMO resource, @Nullable Mapping.Action defaultAction, @Nullable String defaultMapBy) {
        //The default mapping action for identity providers is to always map.
        Mapping mapping = super.buildMapping(resource, Mapping.Action.NewOrExisting, "id");
        mapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnNew", true).map());
        return mapping;
    }
}
