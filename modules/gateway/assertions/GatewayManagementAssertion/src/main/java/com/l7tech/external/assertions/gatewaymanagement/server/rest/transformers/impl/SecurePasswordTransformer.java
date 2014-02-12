package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SecurePasswordResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.security.password.SecurePassword;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SecurePasswordTransformer extends APIResourceWsmanBaseTransformer<StoredPasswordMO, SecurePassword, SecurePasswordResourceFactory> {

    @Override
    @Inject
    protected void setFactory(SecurePasswordResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<StoredPasswordMO> convertToItem(StoredPasswordMO m) {
        return new ItemBuilder<StoredPasswordMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
