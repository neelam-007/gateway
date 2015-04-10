package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.AssertionSecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.server.bundling.AssertionAccessContainer;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class AssertionSecurityZoneTransformer extends APIResourceWsmanBaseTransformer<AssertionSecurityZoneMO, AssertionAccess, EntityHeader, AssertionSecurityZoneResourceFactory> {

    @Override
    @Inject
    @Named("assertionSecurityZoneResourceFactory")
    protected void setFactory(AssertionSecurityZoneResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<AssertionSecurityZoneMO> convertToItem(@NotNull AssertionSecurityZoneMO m) {
        return new ItemBuilder<AssertionSecurityZoneMO>(m.getName(), m.getName(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<AssertionAccess> convertFromMO(@NotNull AssertionSecurityZoneMO m, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return new AssertionAccessContainer(super.convertFromMO(m, strict, secretsEncryptor).getEntity());
    }
}
