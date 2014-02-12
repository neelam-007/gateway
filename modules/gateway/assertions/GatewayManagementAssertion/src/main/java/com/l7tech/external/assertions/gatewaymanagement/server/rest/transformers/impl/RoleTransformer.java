package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RbacRoleResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.RbacRoleMO;
import com.l7tech.gateway.common.security.rbac.Role;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class RoleTransformer extends APIResourceWsmanBaseTransformer<RbacRoleMO, Role, RbacRoleResourceFactory> {

    @Override
    @Inject
    protected void setFactory(RbacRoleResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<RbacRoleMO> convertToItem(RbacRoleMO m) {
        return new ItemBuilder<RbacRoleMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
